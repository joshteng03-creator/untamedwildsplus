package untamedwilds.entity.ai;

import com.mojang.math.Vector3d;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import untamedwilds.entity.ComplexMob;
import untamedwilds.entity.ComplexMobTerrestrial;
import untamedwilds.util.EcologyTags;
import untamedwilds.util.EntityUtils;

import java.util.EnumSet;
import java.util.List;

public class MeleeAttackCharger extends Goal {

    private final int executionChance;
    private final float speed;
    private final ComplexMobTerrestrial taskOwner;
    private double chargeX;
    private double chargeY;
    private double chargeZ;
    private int charge;

    public MeleeAttackCharger(ComplexMobTerrestrial entityIn, float speedIn, int chance) {
        this.taskOwner = entityIn;
        this.speed = speedIn;
        this.executionChance = chance;
        this.charge = 0;
        this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity chargeTarget = this.taskOwner.getTarget();

        if (this.taskOwner.isBaby() || chargeTarget == null || !this.taskOwner.isOnGround() || this.taskOwner.getRandom().nextInt(this.executionChance) != 0) {
            return false;
        } else {
            double distance = this.taskOwner.distanceTo(chargeTarget);
            if (distance < 2 || distance > 24 || !this.taskOwner.isOnGround()) {
                return false;
            } else {
                Vec3 chargePos = EntityUtils.getOvershootPath(taskOwner, chargeTarget, 10);
                boolean canSeeTargetFromDest = taskOwner.getSensing().hasLineOfSight(chargeTarget);
                if (canSeeTargetFromDest) {
                    chargeX = chargePos.x;
                    chargeY = chargePos.y;
                    chargeZ = chargePos.z;

                    return true;
                }
                return false;
            }
        }
    }

    /* The wind-up used to be set by two hardcoded instanceof checks written when the rhino and the bison
     * were the only chargers, so the MAMMOTH -- 10 attack and the highest knockback in the mod -- charged
     * with no telegraph at all. Every herbivore already declares ATTACK_THREATEN as Animation.create(50),
     * an exact match for this wind-up, so the hook costs nothing and covers every future charger. */
    public void start() {
        this.charge = 50;
        this.taskOwner.setAnimation(this.taskOwner.getChargeAnimation());
    }

    public boolean canContinueToUse() {
        return charge > 0 || !this.taskOwner.getNavigation().isDone();
    }

    public void tick() {
        this.taskOwner.getLookControl().setLookAt(chargeX, chargeY - 1, chargeZ);
        if (charge > 0) {
            if (--charge == 0) {
                this.taskOwner.getNavigation().moveTo(chargeX, chargeY, chargeZ, this.speed * 1.2F);
            } else {
                this.taskOwner.setSprinting(true);
            }
        } else { // AABB checking
            AABB offset_box = this.taskOwner.getBoundingBox().move(Math.cos(Math.toRadians(this.taskOwner.getYRot() + 90)) * 1.2, 0, Math.sin(Math.toRadians(this.taskOwner.getYRot() + 90)) * 1.2);
            //AxisAlignedBB offset_box = this.taskOwner.getBoundingBox().offset(Math.cos(this.taskOwner.getYRot() * ((float)Math.PI / 180F)), 0, Math.sin(this.taskOwner.getYRot() * ((float)Math.PI / 180F)));
            /*for (int i = 0; i < 4;  i++) {
                ((ServerWorld)this.taskOwner.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, offset_box.minX, offset_box.minY, offset_box.minZ, 1, 0, 0, 0, 0.05D);
                ((ServerWorld)this.taskOwner.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, offset_box.maxX, offset_box.maxY, offset_box.maxZ, 1, 0, 0, 0, 0.05D);
            }*/
            List<LivingEntity> entitiesHit = this.taskOwner.getLevel().getNearbyEntities(LivingEntity.class, TargetingConditions.forCombat(), this.taskOwner, offset_box);
            for (LivingEntity entityHit : entitiesHit) {
                if (canTrample(entityHit) && this.taskOwner.hasLineOfSight(entityHit)) {
                    /* herbivore_charge_multiplier, delivered by ComplexMob.doChargeHurtTarget so the
                     * transient modifier is torn down in a finally exactly as every predator bonus is.
                     * Per hit rather than once around the loop, matching the four predator call sites. */
                    this.taskOwner.doChargeHurtTarget(entityHit);
                }
            }
        }
    }

    /* The friendly-fire exemption used to be the single hardcoded test `!(entityHit instanceof
     * EntityRhino)`, written when the rhino was the only charger in the mod. Once the mammoth (and the
     * bison) inherited this goal, a charging bull gored its own herd -- calves included -- and every
     * bystander it clipped, and each victim's HurtByTargetGoal retaliated. One charge was enough to
     * start a brawl that ran until the clearing was empty. The rule is now general: never trample your
     * own kind, never trample a herd-mate, and never trample an uninvolved animal that just happens to
     * be standing in the lane. */
    private boolean canTrample(LivingEntity entityHit) {
        if (entityHit.equals(this.taskOwner) || entityHit.getType() == this.taskOwner.getType()) {
            return false;
        }
        if (this.taskOwner.herd != null && entityHit instanceof ComplexMob other && this.taskOwner.herd.containsCreature(other)) {
            return false;
        }
        // Anything that is not the animal we are actually charging has to earn the hit by being a
        // threat; a grazing herd standing in the way is not one.
        return entityHit.equals(this.taskOwner.getTarget()) || EcologyTags.isThreatTo(this.taskOwner, entityHit);
    }

    public void stop() {
        this.charge = 0;
        this.taskOwner.setSprinting(false);
    }
}
