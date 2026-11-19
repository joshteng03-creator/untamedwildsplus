package untamedwilds.init;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import untamedwilds.UntamedWilds;

public class ModAdvancementTriggers {
    public static UntamedTriggers<?> NO_PATCHOULI_LOADED = new UntamedTriggers<>(new ResourceLocation(UntamedWilds.MOD_ID, "guidebook_alt"));
    public static UntamedTriggers<?> BAIT_BASIC = new UntamedTriggers<>(new ResourceLocation(UntamedWilds.MOD_ID, "used_bait"));
    public static UntamedTriggers<?> MASTER_BAIT = new UntamedTriggers<>(new ResourceLocation(UntamedWilds.MOD_ID, "master_bait"));

    public static UntamedTriggers<?> DISCOVERED_SPITTER = new UntamedTriggers<>(new ResourceLocation(UntamedWilds.MOD_ID, "discovered_spitter"));
    // Any of the mod's animals observed (spyglass or Field Guide); an advancement may narrow it to one type.
    public static DiscoveryTrigger DISCOVERED_ANIMAL = new DiscoveryTrigger();

    public static void register() {
        CriteriaTriggers.register(NO_PATCHOULI_LOADED);
        CriteriaTriggers.register(BAIT_BASIC);
        CriteriaTriggers.register(MASTER_BAIT);

        CriteriaTriggers.register(DISCOVERED_SPITTER);
        CriteriaTriggers.register(DISCOVERED_ANIMAL);
    }

    /**
     * {@code untamedwilds:discovered_animal}, optional condition {@code "entity_type": "untamedwilds:bear"}.
     * Without a type it matches any of the mod's animals (First Sighting); with one, only that type (one
     * criterion per type in the Zoologist challenge).
     */
    public static class DiscoveryTrigger extends SimpleCriterionTrigger<DiscoveryTrigger.Instance> {
        private static final ResourceLocation ID = new ResourceLocation(UntamedWilds.MOD_ID, "discovered_animal");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        protected Instance createInstance(JsonObject json, EntityPredicate.Composite player, DeserializationContext context) {
            ResourceLocation type = json.has("entity_type") ? new ResourceLocation(GsonHelper.getAsString(json, "entity_type")) : null;
            return new Instance(player, type);
        }

        public void trigger(ServerPlayer player, EntityType<?> type) {
            ResourceLocation key = EntityType.getKey(type);
            this.trigger(player, instance -> instance.type == null || instance.type.equals(key));
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            private final ResourceLocation type;

            public Instance(EntityPredicate.Composite player, ResourceLocation type) {
                super(ID, player);
                this.type = type;
            }

            @Override
            public JsonObject serializeToJson(SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                if (this.type != null) {
                    json.addProperty("entity_type", this.type.toString());
                }
                return json;
            }
        }
    }

    public static class UntamedTriggers<T extends CriterionTriggerInstance> extends SimpleCriterionTrigger<UntamedTriggers.Instance> {
        private final ResourceLocation id;

        public UntamedTriggers(ResourceLocation resourceLocation) {
            this.id = resourceLocation;
        }

        public Instance createInstance(JsonObject objectIn, EntityPredicate.Composite predicateIn, DeserializationContext p_230241_3_) {
            return new UntamedTriggers.Instance(predicateIn, id);
        }

        public void trigger(ServerPlayer entityIn) {
            this.trigger(entityIn, (input) -> true);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        public static class Instance extends AbstractCriterionTriggerInstance  {

            public Instance(EntityPredicate.Composite p_i231507_1_, ResourceLocation res) {
                super(res, p_i231507_1_);
            }

            public JsonObject serializeToJson(SerializationContext p_230240_1_) {
                return super.serializeToJson(p_230240_1_);
            }
        }
    }
}