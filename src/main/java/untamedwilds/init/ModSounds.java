package untamedwilds.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import untamedwilds.UntamedWilds;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = UntamedWilds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UntamedWilds.MOD_ID);

    public static final SoundEvent ENTITY_ATTACK_BITE = registerSound("entity.generic.bite");

    public static final SoundEvent ENTITY_HIPPO_AMBIENT = registerSound("entity.hippo.ambient");

    public static final SoundEvent ENTITY_BISON_AMBIENT = registerSound("entity.bison.ambient");

    public static final SoundEvent ENTITY_BEAR_AMBIENT = registerSound("entity.bear.ambient");
    public static final SoundEvent ENTITY_BEAR_WARNING = registerSound("entity.bear.warning");
    public static final SoundEvent ENTITY_BEAR_HURT = registerSound("entity.bear.hurt");
    public static final SoundEvent ENTITY_BEAR_DEATH = registerSound("entity.bear.death");
    public static final SoundEvent ENTITY_BEAR_BABY_AMBIENT = registerSound("entity.bear.baby.ambient");
    public static final SoundEvent ENTITY_BEAR_BABY_CRY = registerSound("entity.bear.baby.cry");

    public static final SoundEvent ENTITY_BIG_CAT_AMBIENT = registerSound("entity.big_cat.ambient");
    // 2.8.0: sounds.json and big_cat.json (cheetahs, small cats) always referenced this, but it was never
    // registered, so those species were silent.
    public static final SoundEvent ENTITY_BIG_CAT_AMBIENT_MEOW = registerSound("entity.big_cat.ambient_meow");
    public static final SoundEvent ENTITY_BIG_CAT_HURT = registerSound("entity.big_cat.hurt");
    public static final SoundEvent ENTITY_BIG_CAT_DEATH = registerSound("entity.big_cat.death");

    public static final SoundEvent ENTITY_AARDVARK_AMBIENT = registerSound("entity.aardvark.ambient");
    public static final SoundEvent ENTITY_AARDVARK_HURT = registerSound("entity.aardvark.hurt");
    public static final SoundEvent ENTITY_AARDVARK_DEATH = registerSound("entity.aardvark.death");

    public static final SoundEvent ENTITY_HYENA_LAUGHING = registerSound("entity.hyena.laugh");
    public static final SoundEvent ENTITY_HYENA_GROWL = registerSound("entity.hyena.growl");
    public static final SoundEvent ENTITY_HYENA_AMBIENT = registerSound("entity.hyena.ambient");
    public static final SoundEvent ENTITY_HYENA_HURT = registerSound("entity.hyena.hurt");
    public static final SoundEvent ENTITY_HYENA_DEATH = registerSound("entity.hyena.death");

    public static final SoundEvent ENTITY_BOAR_SQUEAL = registerSound("entity.boar.squeal");
    public static final SoundEvent ENTITY_BOAR_AMBIENT = registerSound("entity.boar.ambient");
    public static final SoundEvent ENTITY_BOAR_HURT = registerSound("entity.boar.hurt");
    public static final SoundEvent ENTITY_BOAR_DEATH = registerSound("entity.boar.death");

    public static final SoundEvent ENTITY_CAMEL_SPIT = registerSound("entity.camel.spit");
    public static final SoundEvent ENTITY_CAMEL_AMBIENT = registerSound("entity.camel.ambient");
    public static final SoundEvent ENTITY_CAMEL_HURT = registerSound("entity.camel.hurt");
    public static final SoundEvent ENTITY_CAMEL_DEATH = registerSound("entity.camel.death");

    public static final SoundEvent ENTITY_SPITTER_SPIT = registerSound("entity.spitter.spit");
    public static final SoundEvent ENTITY_SPITTER_AMBIENT = registerSound("entity.spitter.ambient");
    public static final SoundEvent ENTITY_SPITTER_HURT = registerSound("entity.spitter.hurt");
    public static final SoundEvent ENTITY_SPITTER_DEATH = registerSound("entity.spitter.death");

    public static final SoundEvent ENTITY_OPOSSUM_HISS = registerSound("entity.opossum.hiss");
    public static final SoundEvent ENTITY_OPOSSUM_AMBIENT = registerSound("entity.opossum.ambient");
    public static final SoundEvent ENTITY_OPOSSUM_HURT = registerSound("entity.opossum.hurt");
    public static final SoundEvent ENTITY_OPOSSUM_DEATH = registerSound("entity.opossum.death");

    public static final SoundEvent ENTITY_TARANTULA_AMBIENT = registerSound("entity.tarantula.ambient");

    public static final SoundEvent ENTITY_SNAKE_HISS = registerSound("entity.snake.warning");
    public static final SoundEvent ENTITY_SNAKE_RATTLE = registerSound("entity.snake.rattle");

    // <gen:voices> 2.8.0 CC0 / public-domain voices (scripts/audio/install_voices.py; credits in sounds/CREDITS.md)
    public static final SoundEvent ENTITY_GRAZER_EAT = registerSound("entity.grazer.eat");
    public static final SoundEvent ENTITY_ANIMAL_DRINK = registerSound("entity.animal.drink");
    public static final SoundEvent ENTITY_MEGAFAUNA_STEP = registerSound("entity.megafauna.step");
    public static final SoundEvent ENTITY_CARCASS_BUTCHER = registerSound("entity.carcass.butcher");
    public static final String[] VOICE_EVENTS = {
            "entity.antelope.alarm", "entity.antelope.ambient", "entity.antelope.death", "entity.antelope.hurt",
            "entity.baleen_whale.ambient", "entity.baleen_whale.call", "entity.baleen_whale.death", "entity.baleen_whale.hurt",
            "entity.bison.call", "entity.caprine.alarm", "entity.caprine.ambient", "entity.caprine.death",
            "entity.caprine.hurt", "entity.crocodilian.ambient", "entity.crocodilian.call", "entity.crocodilian.death",
            "entity.crocodilian.hurt", "entity.crocodilian.threat", "entity.deer.alarm", "entity.deer.ambient",
            "entity.deer.call", "entity.deer.death", "entity.deer.hurt", "entity.deer.threat",
            "entity.diprotodont.ambient", "entity.diprotodont.death", "entity.diprotodont.hurt", "entity.diprotodont.threat",
            "entity.dire_wolf.alarm", "entity.dire_wolf.ambient", "entity.dire_wolf.call", "entity.dire_wolf.death",
            "entity.dire_wolf.hurt", "entity.dire_wolf.threat", "entity.equid.alarm", "entity.equid.ambient",
            "entity.equid.call", "entity.equid.death", "entity.equid.hurt", "entity.giant_monitor.hurt",
            "entity.giant_monitor.threat", "entity.giraffid.alarm", "entity.giraffid.ambient", "entity.giraffid.death",
            "entity.giraffid.hurt", "entity.glyptodont.ambient", "entity.glyptodont.death", "entity.glyptodont.hurt",
            "entity.ground_sloth.ambient", "entity.ground_sloth.death", "entity.ground_sloth.hurt", "entity.ground_sloth.threat",
            "entity.kangaroo.alarm", "entity.kangaroo.ambient", "entity.kangaroo.death", "entity.kangaroo.hurt",
            "entity.kangaroo.threat", "entity.large_rodent.alarm", "entity.large_rodent.ambient", "entity.large_rodent.death",
            "entity.large_rodent.hurt", "entity.macrauchenia.alarm", "entity.macrauchenia.ambient", "entity.macrauchenia.death",
            "entity.macrauchenia.hurt", "entity.mammoth.alarm", "entity.mammoth.ambient", "entity.mammoth.call",
            "entity.mammoth.death", "entity.mammoth.hurt", "entity.mammoth.threat", "entity.monitor.threat",
            "entity.pinniped.ambient", "entity.pinniped.call", "entity.pinniped.death", "entity.pinniped.hurt",
            "entity.pinniped.threat", "entity.rhino.alarm", "entity.rhino.ambient", "entity.rhino.death",
            "entity.rhino.hurt", "entity.rhino.threat", "entity.softshell_turtle.threat", "entity.tapir.ambient",
            "entity.tapir.call", "entity.tapir.death", "entity.tapir.hurt", "entity.tortoise.ambient",
            "entity.tortoise.threat", "entity.toxodon.ambient", "entity.toxodon.death", "entity.toxodon.hurt",
            "entity.toxodon.threat", "entity.wildebeest.alarm", "entity.wildebeest.ambient", "entity.wildebeest.call",
            "entity.wildebeest.death", "entity.wildebeest.hurt"
    };
    static {
        for (String event : VOICE_EVENTS) {
            registerSound(event);
        }
    }
    // </gen:voices>

    private static SoundEvent registerSound(String soundName) {
        ResourceLocation location = new ResourceLocation(UntamedWilds.MOD_ID, soundName);
        SoundEvent event = new SoundEvent(location);
        ModSounds.SOUNDS.register(soundName, () -> event);
        return event;
    }
}
