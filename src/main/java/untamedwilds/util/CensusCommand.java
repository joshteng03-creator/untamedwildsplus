package untamedwilds.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import untamedwilds.UntamedWilds;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.HerdEntity;
import untamedwilds.entity.INestingMob;
import untamedwilds.entity.ISpecies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Population census for tuning the ecosystem gamerules. The acceptance test for every ecology pass is
 * "predator and prey counts oscillate within bounds instead of one going to zero", which cannot be
 * judged from AnalyzerItem readouts of single animals.
 *
 *   /untamedwilds census               one snapshot: chat summary + rows appended to logs/uw_census.csv
 *   /untamedwilds census auto <min>   snapshot every <min> real minutes (0 stops) -- for AFK soak tests
 *   /untamedwilds census <species>    status report for one species (type:species), a whole type (type),
 *                                     or a bare species name if unique; Tab lists them, matching anywhere
 *
 * Counts only what is loaded, since MC only simulates loaded chunks; a soak test should therefore
 * keep the player parked in one place so the sampled area stays constant between rows.
 */
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID)
public class CensusCommand {

    private static final String HEADER = "game_time,day,dimension,type,species,diet,adults,juveniles,mean_condition,starving,senescent\n";
    private static int autoIntervalTicks = 0;
    private static int ticksUntilNext = 0;
    /* Last population seen per species query, so a report can show the trend since the previous look. */
    private static final Map<String, Integer> LAST_COUNT = new HashMap<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(UntamedWilds.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("census")
                        .executes(ctx -> {
                            List<String> summary = snapshot(ctx.getSource().getServer());
                            summary.forEach(line -> ctx.getSource().sendSuccess(new TextComponent(line), false));
                            return summary.size();
                        })
                        .then(Commands.literal("auto")
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(0, 120))
                                        .executes(ctx -> {
                                            int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
                                            autoIntervalTicks = minutes * 60 * 20;
                                            ticksUntilNext = 0;
                                            ctx.getSource().sendSuccess(new TextComponent(minutes == 0
                                                    ? "Census auto-logging stopped"
                                                    : "Census logging to logs/uw_census.csv every " + minutes + " min"), true);
                                            return minutes;
                                        })))
                        /* A sibling of the "auto" literal: Brigadier prefers a literal that matches the
                         * next word, so "census auto 5" still parses as the timer. greedyString rather
                         * than word() because word() rejects the ':' in type:species. */
                        .then(Commands.argument("species", StringArgumentType.greedyString())
                                .suggests(CensusCommand::suggestSpecies)
                                .executes(ctx -> report(ctx.getSource(), StringArgumentType.getString(ctx, "species"))))));
    }

    /** One entry per species ("type:species") plus one per type ("type", the whole genus). */
    private static List<String> speciesKeys() {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<EntityType<?>, EntityDataHolder> e : ComplexMob.ENTITY_DATA_HASH.entrySet()) {
            String type = EntityType.getKey(e.getKey()).getPath();
            keys.add(type);
            for (SpeciesDataHolder species : e.getValue().getSpeciesData()) {
                keys.add(type + ":" + species.getName().toLowerCase(Locale.ROOT));
            }
        }
        Collections.sort(keys);
        return keys;
    }

    /* Matches anywhere in the key rather than only as a prefix, so typing "gray" offers
     * dire_wolf:gray_wolf and "lion" offers every lion, not just types starting with "lion". Each
     * suggestion carries the in-game name as its tooltip. */
    private static CompletableFuture<Suggestions> suggestSpecies(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT).trim().replace(' ', '_');
        for (String key : speciesKeys()) {
            if (typed.isEmpty() || key.contains(typed)) {
                builder.suggest(key, displayName(key));
            }
        }
        return builder.buildFuture();
    }

    private static MutableComponent displayName(String key) {
        int colon = key.indexOf(':');
        return colon < 0
                ? new TranslatableComponent("entity.untamedwilds." + key)
                : new TranslatableComponent("entity.untamedwilds." + key.substring(0, colon) + "_" + key.substring(colon + 1));
    }

    /** Resolves a typed query to a canonical key: an exact key, or a bare species name that is unique. */
    private static String resolve(String query) {
        String q = query.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
        List<String> keys = speciesKeys();
        if (keys.contains(q)) {
            return q;
        }
        String match = null;
        for (String key : keys) {
            if (key.endsWith(":" + q)) {
                if (match != null) {
                    return null;   // ambiguous: the same species name is used by two types
                }
                match = key;
            }
        }
        return match;
    }

    private static int report(CommandSourceStack source, String query) {
        String key = resolve(query);
        if (key == null) {
            String q = query.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
            List<String> close = speciesKeys().stream().filter(k -> k.contains(q)).limit(6).toList();
            source.sendFailure(new TextComponent("Unknown or ambiguous species '" + query + "'"
                    + (close.isEmpty() ? "" : ". Did you mean: " + String.join(", ", close)) + " (press Tab for the list)"));
            return 0;
        }
        int colon = key.indexOf(':');
        String type = colon < 0 ? key : key.substring(0, colon);
        String species = colon < 0 ? null : key.substring(colon + 1);

        List<ComplexMob> found = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (ComplexMob mob : level.getEntities(EntityTypeTest.forClass(ComplexMob.class), ComplexMob::isAlive)) {
                if (!EntityType.getKey(mob.getType()).getPath().equals(type)) {
                    continue;
                }
                if (species != null && !(mob instanceof ISpecies s && s.getRawSpeciesName(mob.getVariant()).equals(species))) {
                    continue;
                }
                found.add(mob);
            }
        }

        source.sendSuccess(new TextComponent("== ").withStyle(ChatFormatting.GOLD)
                .append(displayName(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(new TextComponent(" (" + key + ") ==").withStyle(ChatFormatting.GOLD)), false);

        Integer previous = LAST_COUNT.put(key, found.size());
        if (found.isEmpty()) {
            source.sendSuccess(new TextComponent("None alive in loaded chunks"
                    + (previous != null && previous > 0 ? " (was " + previous + " at your last check)" : "")
                    + ". Only loaded chunks are simulated, so this count is local, not world-wide.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        int adults = 0, males = 0, females = 0, juveniles = 0, starving = 0, elderly = 0, pregnant = 0;
        float condition = 0F;
        long hunger = 0, ecoSum = 0, agePermille = 0;
        int ecoMin = Integer.MAX_VALUE, ecoMax = Integer.MIN_VALUE;
        Set<HerdEntity> herds = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ComplexMob mob : found) {
            if (mob.isBaby()) {
                juveniles++;
            } else {
                adults++;
                if (mob.isMale()) males++; else females++;
                // A mother's age runs positive while she carries; setAge's >0 to <=0 edge is the birth.
                if (!mob.isMale() && mob.getAge() > 0 && !(mob instanceof INestingMob nesting && nesting.isEggLayer())) {
                    pregnant++;
                }
            }
            if (mob.isStarving()) starving++;
            if (mob.isSenescent()) elderly++;
            condition += mob.condition;
            hunger += mob.getHunger();
            int eco = ComplexMob.getEcoLevel(mob);
            ecoSum += eco;
            ecoMin = Math.min(ecoMin, eco);
            ecoMax = Math.max(ecoMax, eco);
            agePermille += 1000L * mob.lifeTicks / Math.max(1, mob.getLifespan());
            if (mob.herd != null) {
                herds.add(mob.herd);
            }
        }
        int n = found.size();

        String trend = previous == null ? "" : n == previous ? "  (no change since last check)"
                : String.format(Locale.ROOT, "  (%+d since last check)", n - previous);
        source.sendSuccess(line("Population", adults + " adults (" + males + "M / " + females + "F), " + juveniles + " juveniles" + trend,
                found.get(0).isCarnivore() ? ChatFormatting.RED : ChatFormatting.GREEN), false);

        if (!herds.isEmpty()) {
            List<String> sizes = herds.stream().map(h -> h.creatureList.size()).sorted(Collections.reverseOrder()).map(String::valueOf).toList();
            long migrating = herds.stream().filter(HerdEntity::isMigrating).count();
            source.sendSuccess(line("Groups", herds.size() + " (sizes " + String.join(", ", sizes.subList(0, Math.min(8, sizes.size())))
                    + (sizes.size() > 8 ? ", ..." : "") + ")" + (migrating > 0 ? ", " + migrating + " migrating" : ""), ChatFormatting.WHITE), false);
        }
        source.sendSuccess(line("Health", String.format(Locale.ROOT, "condition %.2f avg, hunger %d avg, %d starving",
                condition / n, hunger / n, starving), starving > 0 ? ChatFormatting.RED : ChatFormatting.WHITE), false);
        source.sendSuccess(line("Breeding", pregnant + " pregnant, " + elderly + " elderly, mean age "
                + (agePermille / n / 10) + "% of lifespan", ChatFormatting.WHITE), false);
        source.sendSuccess(line("Eco level", String.format(Locale.ROOT, "%.1f avg (%d-%d)", (double) ecoSum / n, ecoMin, ecoMax), ChatFormatting.WHITE), false);

        if (source.getEntity() != null) {
            ComplexMob nearest = null;
            double best = Double.MAX_VALUE;
            for (ComplexMob mob : found) {
                if (mob.level == source.getEntity().level) {
                    double d = mob.distanceToSqr(source.getEntity());
                    if (d < best) {
                        best = d;
                        nearest = mob;
                    }
                }
            }
            if (nearest != null) {
                String tp = "/tp @s " + nearest.getBlockX() + " " + nearest.getBlockY() + " " + nearest.getBlockZ();
                MutableComponent where = new TextComponent(String.format(Locale.ROOT, "%d blocks away at %d %d %d",
                        (int) Math.sqrt(best), nearest.getBlockX(), nearest.getBlockY(), nearest.getBlockZ()))
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to fill in " + tp))));
                source.sendSuccess(new TextComponent("Nearest: ").withStyle(ChatFormatting.GRAY).append(where), false);
            }
        }
        return n;
    }

    private static MutableComponent line(String label, String value, ChatFormatting valueColour) {
        return new TextComponent(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(new TextComponent(value).withStyle(valueColour));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || autoIntervalTicks <= 0) {
            return;
        }
        if (--ticksUntilNext <= 0) {
            ticksUntilNext = autoIntervalTicks;
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                snapshot(server);
            }
        }
    }

    /** Counts every loaded ComplexMob, appends one CSV row per (dimension, type, species) and returns a chat summary. */
    private static List<String> snapshot(MinecraftServer server) {
        StringBuilder csv = new StringBuilder();
        Map<String, int[]> perType = new TreeMap<>();   // type -> {adults, juveniles}
        long gameTime = server.overworld().getGameTime();
        long day = server.overworld().getDayTime() / 24000L;

        for (ServerLevel level : server.getAllLevels()) {
            Map<String, Tally> groups = new TreeMap<>();
            for (ComplexMob mob : level.getEntities(EntityTypeTest.forClass(ComplexMob.class), ComplexMob::isAlive)) {
                String type = EntityType.getKey(mob.getType()).getPath();
                String species = mob instanceof ISpecies ispecies && ComplexMob.ENTITY_DATA_HASH.containsKey(mob.getType())
                        ? ispecies.getRawSpeciesName(mob.getVariant()) : "-";
                Tally t = groups.computeIfAbsent(type + "," + species, k -> new Tally(mob.isCarnivore() ? "carnivore" : "other"));
                t.add(mob);
                int[] sum = perType.computeIfAbsent(type, k -> new int[2]);
                sum[mob.isBaby() ? 1 : 0]++;
            }
            String dim = level.dimension().location().toString();
            groups.forEach((key, t) -> csv.append(gameTime).append(',').append(day).append(',').append(dim).append(',')
                    .append(key).append(',').append(t.diet).append(',').append(t.adults).append(',').append(t.juveniles).append(',')
                    .append(String.format(java.util.Locale.ROOT, "%.3f", t.conditionSum / Math.max(1, t.adults + t.juveniles))).append(',')
                    .append(t.starving).append(',').append(t.senescent).append('\n'));
        }

        write(csv.toString());

        List<String> summary = new ArrayList<>();
        summary.add("Census day " + day + " (loaded chunks only) - adults/juveniles:");
        StringBuilder line = new StringBuilder();
        for (Map.Entry<String, int[]> e : perType.entrySet()) {
            String part = e.getKey() + " " + e.getValue()[0] + "/" + e.getValue()[1];
            if (line.length() + part.length() > 90) {
                summary.add(line.toString());
                line.setLength(0);
            }
            line.append(line.length() == 0 ? "" : ", ").append(part);
        }
        if (line.length() > 0) {
            summary.add(line.toString());
        }
        summary.add("Rows appended to logs/uw_census.csv");
        return summary;
    }

    private static void write(String rows) {
        Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("uw_census.csv");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, HEADER, StandardCharsets.UTF_8);
            }
            Files.writeString(file, rows, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            UntamedWilds.LOGGER.error("Could not write census file " + file, e);
        }
    }

    private static final class Tally {
        final String diet;
        int adults, juveniles, starving, senescent;
        float conditionSum;

        Tally(String diet) {
            this.diet = diet;
        }

        void add(ComplexMob mob) {
            if (mob.isBaby()) this.juveniles++; else this.adults++;
            if (mob.isStarving()) this.starving++;
            if (mob.isSenescent()) this.senescent++;
            this.conditionSum += mob.condition;
        }
    }
}
