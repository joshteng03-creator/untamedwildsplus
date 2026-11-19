package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityCaprine;

/**
 * Caprines -- Alpine + Siberian ibex, argali, bighorn, chamois, Harrington's mountain goat (Phase 4.5, 2026-10-10): 53
 * boxes on 256x128, authored in scripts/caprine_spec.py, ported VERBATIM by scripts/gen_model_caprine.py, proved by
 * verify_port.py. A stocky, deep-chested, short-legged climber -- deliberately NOT the slender antelope frame.
 *
 * FOUR HORN FAMILIES share the skull, one shown per species (showModel on each chain root hides its whole subtree):
 * ibex scimitars, ram curls (authored by world orientation so the curl lies in a vertical plane outside the skull),
 * chamois hooks and mountain-goat daggers; plus a beard (ibex + mountain goat).
 * ATTACK_BUTT is the head-down charge-and-butt every caprine fights with; ATTACK_CLASH is the cosmetic rut clash between
 * males -- rear up on the hind legs, then drop and smash horns.
 */
public class ModelCaprine extends AdvancedEntityModel<EntityCaprine> {

    private final AdvancedModelBox body_barrel;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox muzzle;
    private final AdvancedModelBox nose;
    private final AdvancedModelBox beard;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox ibex_left_1;
    private final AdvancedModelBox ibex_left_2;
    private final AdvancedModelBox ibex_left_3;
    private final AdvancedModelBox ibex_left_4;
    private final AdvancedModelBox ibex_right_1;
    private final AdvancedModelBox ibex_right_2;
    private final AdvancedModelBox ibex_right_3;
    private final AdvancedModelBox ibex_right_4;
    private final AdvancedModelBox ram_left_1;
    private final AdvancedModelBox ram_left_2;
    private final AdvancedModelBox ram_left_3;
    private final AdvancedModelBox ram_left_4;
    private final AdvancedModelBox ram_right_1;
    private final AdvancedModelBox ram_right_2;
    private final AdvancedModelBox ram_right_3;
    private final AdvancedModelBox ram_right_4;
    private final AdvancedModelBox chamois_left_1;
    private final AdvancedModelBox chamois_left_2;
    private final AdvancedModelBox chamois_right_1;
    private final AdvancedModelBox chamois_right_2;
    private final AdvancedModelBox dagger_left_1;
    private final AdvancedModelBox dagger_left_2;
    private final AdvancedModelBox dagger_right_1;
    private final AdvancedModelBox dagger_right_2;
    private final AdvancedModelBox fore_left_upper;
    private final AdvancedModelBox fore_left_forearm;
    private final AdvancedModelBox fore_left_cannon;
    private final AdvancedModelBox fore_left_hoof;
    private final AdvancedModelBox fore_right_upper;
    private final AdvancedModelBox fore_right_forearm;
    private final AdvancedModelBox fore_right_cannon;
    private final AdvancedModelBox fore_right_hoof;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox hind_left_thigh;
    private final AdvancedModelBox hind_left_gaskin;
    private final AdvancedModelBox hind_left_cannon;
    private final AdvancedModelBox hind_left_hoof;
    private final AdvancedModelBox hind_right_thigh;
    private final AdvancedModelBox hind_right_gaskin;
    private final AdvancedModelBox hind_right_cannon;
    private final AdvancedModelBox hind_right_hoof;

    private final ModelAnimator animator;

    public ModelCaprine() {
        this.texWidth = 256;
        this.texHeight = 128;

        this.body_barrel = new AdvancedModelBox(this, 0, 0);
        this.body_barrel.setRotationPoint(0.0F, 12.6000F, 2.0F);
        this.body_barrel.addBox(-4.2000F, -3.6000F, -8.0F, 8.4000F, 7.2000F, 15.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 47, 0);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.body_chest.addBox(-3.8000F, -4.4000F, -6.0F, 7.6000F, 8.6000F, 7.0F, 0.0F);
        this.setRotateAngle(body_chest, -0.1047F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 104, 0);
        this.neck.setRotationPoint(0.0F, -2.6000F, -5.0F);
        this.neck.addBox(-2.3000F, -2.1000F, -4.2000F, 4.6000F, 4.6000F, 6.4000F, 0.0F);
        this.setRotateAngle(neck, -0.5585F, 0.0F, 0.0F);
        this.head = new AdvancedModelBox(this, 154, 0);
        this.head.setRotationPoint(0.0F, 0.0F, -4.2000F);
        this.head.addBox(-2.0F, -1.9000F, -3.8000F, 4.0F, 3.6000F, 5.2000F, 0.0F);
        this.setRotateAngle(head, 1.1519F, 0.0F, 0.0F);
        this.muzzle = new AdvancedModelBox(this, 199, 0);
        this.muzzle.setRotationPoint(0.0F, 0.0F, -3.8000F);
        this.muzzle.addBox(-1.4000F, -1.2000F, -2.6000F, 2.8000F, 2.5000F, 3.6000F, 0.0F);
        this.setRotateAngle(muzzle, 0.2094F, 0.0F, 0.0F);
        this.nose = new AdvancedModelBox(this, 169, 23);
        this.nose.setRotationPoint(0.0F, 0.0F, -2.6000F);
        this.nose.addBox(-1.0F, -0.9000F, -0.4000F, 2.0F, 1.5000F, 0.7000F, 0.0F);
        this.beard = new AdvancedModelBox(this, 125, 23);
        this.beard.setRotationPoint(0.0F, 1.2000F, -1.0F);
        this.beard.addBox(-0.7000F, -0.2000F, -0.6000F, 1.4000F, 2.6000F, 1.2000F, 0.0F);
        this.setRotateAngle(beard, 0.2618F, 0.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 185, 23);
        this.ear_left.setRotationPoint(1.9000F, -1.2000F, -0.8000F);
        this.ear_left.addBox(0.0F, -0.4000F, -0.6000F, 2.4000F, 0.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, -0.1745F, 1.0472F);
        this.eye_left = new AdvancedModelBox(this, 131, 23);
        this.eye_left.setRotationPoint(2.3500F, -0.6000F, -2.0F);
        this.eye_left.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 193, 23);
        this.ear_right.setRotationPoint(-1.9000F, -1.2000F, -0.8000F);
        this.ear_right.addBox(-2.4000F, -0.4000F, -0.6000F, 2.4000F, 0.8000F, 1.2000F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.1745F, -1.0472F);
        this.eye_right = new AdvancedModelBox(this, 135, 23);
        this.eye_right.setRotationPoint(-2.3500F, -0.6000F, -2.0F);
        this.eye_right.addBox(0.0F, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F);
        this.ibex_left_1 = new AdvancedModelBox(this, 8, 23);
        this.ibex_left_1.setRotationPoint(0.9000F, -1.5000F, -1.6000F);
        this.ibex_left_1.addBox(-0.6500F, -3.2000F, -0.8000F, 1.3000F, 3.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(ibex_left_1, -0.3142F, 0.0F, 0.0349F);
        this.ibex_left_2 = new AdvancedModelBox(this, 67, 23);
        this.ibex_left_2.setRotationPoint(0.0F, -2.8000F, 0.0F);
        this.ibex_left_2.addBox(-0.5500F, -3.0F, -0.7000F, 1.1000F, 3.4000F, 1.4000F, 0.0F);
        this.setRotateAngle(ibex_left_2, -0.4363F, 0.0F, 0.1571F);
        this.ibex_left_3 = new AdvancedModelBox(this, 72, 23);
        this.ibex_left_3.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.ibex_left_3.addBox(-0.4500F, -2.8000F, -0.6000F, 0.9000F, 3.2000F, 1.2000F, 0.0F);
        this.setRotateAngle(ibex_left_3, -0.4363F, 0.0F, 0.1571F);
        this.ibex_left_4 = new AdvancedModelBox(this, 139, 23);
        this.ibex_left_4.setRotationPoint(0.0F, -2.4000F, 0.0F);
        this.ibex_left_4.addBox(-0.3500F, -2.4000F, -0.4500F, 0.7000F, 2.8000F, 0.9000F, 0.0F);
        this.setRotateAngle(ibex_left_4, -0.3840F, 0.0F, 0.1571F);
        this.ibex_right_1 = new AdvancedModelBox(this, 14, 23);
        this.ibex_right_1.setRotationPoint(-0.9000F, -1.5000F, -1.6000F);
        this.ibex_right_1.addBox(-0.6500F, -3.2000F, -0.8000F, 1.3000F, 3.6000F, 1.6000F, 0.0F);
        this.setRotateAngle(ibex_right_1, -0.3142F, 0.0F, -0.0349F);
        this.ibex_right_2 = new AdvancedModelBox(this, 77, 23);
        this.ibex_right_2.setRotationPoint(0.0F, -2.8000F, 0.0F);
        this.ibex_right_2.addBox(-0.5500F, -3.0F, -0.7000F, 1.1000F, 3.4000F, 1.4000F, 0.0F);
        this.setRotateAngle(ibex_right_2, -0.4363F, 0.0F, -0.1571F);
        this.ibex_right_3 = new AdvancedModelBox(this, 82, 23);
        this.ibex_right_3.setRotationPoint(0.0F, -2.6000F, 0.0F);
        this.ibex_right_3.addBox(-0.4500F, -2.8000F, -0.6000F, 0.9000F, 3.2000F, 1.2000F, 0.0F);
        this.setRotateAngle(ibex_right_3, -0.4363F, 0.0F, -0.1571F);
        this.ibex_right_4 = new AdvancedModelBox(this, 143, 23);
        this.ibex_right_4.setRotationPoint(0.0F, -2.4000F, 0.0F);
        this.ibex_right_4.addBox(-0.3500F, -2.4000F, -0.4500F, 0.7000F, 2.8000F, 0.9000F, 0.0F);
        this.setRotateAngle(ibex_right_4, -0.3840F, 0.0F, -0.1571F);
        this.ram_left_1 = new AdvancedModelBox(this, 246, 0);
        this.ram_left_1.setRotationPoint(1.9000F, -1.5000F, -1.6000F);
        this.ram_left_1.addBox(-1.0F, -2.6000F, -1.0F, 2.0F, 3.0F, 2.0F, 0.0F);
        this.setRotateAngle(ram_left_1, -0.8231F, 0.3065F, 0.6376F);
        this.ram_left_2 = new AdvancedModelBox(this, 53, 23);
        this.ram_left_2.setRotationPoint(0.0F, -2.2000F, 0.0F);
        this.ram_left_2.addBox(-0.7750F, -2.6000F, -0.8500F, 1.5500F, 3.0F, 1.7000F, 0.0F);
        this.setRotateAngle(ram_left_2, -1.1404F, 0.2751F, -0.6502F);
        this.ram_left_3 = new AdvancedModelBox(this, 87, 23);
        this.ram_left_3.setRotationPoint(0.0F, -2.2000F, 0.0F);
        this.ram_left_3.addBox(-0.5500F, -2.4000F, -0.7000F, 1.1000F, 2.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(ram_left_3, -1.0472F, 0.0F, 0.0F);
        this.ram_left_4 = new AdvancedModelBox(this, 147, 23);
        this.ram_left_4.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.ram_left_4.addBox(-0.3250F, -2.2000F, -0.5500F, 0.6500F, 2.6000F, 1.1000F, 0.0F);
        this.setRotateAngle(ram_left_4, -1.0472F, 0.0F, 0.0F);
        this.ram_right_1 = new AdvancedModelBox(this, 0, 23);
        this.ram_right_1.setRotationPoint(-1.9000F, -1.5000F, -1.6000F);
        this.ram_right_1.addBox(-1.0F, -2.6000F, -1.0F, 2.0F, 3.0F, 2.0F, 0.0F);
        this.setRotateAngle(ram_right_1, -0.8231F, -0.3065F, -0.6376F);
        this.ram_right_2 = new AdvancedModelBox(this, 60, 23);
        this.ram_right_2.setRotationPoint(0.0F, -2.2000F, 0.0F);
        this.ram_right_2.addBox(-0.7750F, -2.6000F, -0.8500F, 1.5500F, 3.0F, 1.7000F, 0.0F);
        this.setRotateAngle(ram_right_2, -1.1404F, -0.2751F, 0.6502F);
        this.ram_right_3 = new AdvancedModelBox(this, 92, 23);
        this.ram_right_3.setRotationPoint(0.0F, -2.2000F, 0.0F);
        this.ram_right_3.addBox(-0.5500F, -2.4000F, -0.7000F, 1.1000F, 2.8000F, 1.4000F, 0.0F);
        this.setRotateAngle(ram_right_3, -1.0472F, 0.0F, 0.0F);
        this.ram_right_4 = new AdvancedModelBox(this, 151, 23);
        this.ram_right_4.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.ram_right_4.addBox(-0.3250F, -2.2000F, -0.5500F, 0.6500F, 2.6000F, 1.1000F, 0.0F);
        this.setRotateAngle(ram_right_4, -1.0472F, 0.0F, 0.0F);
        this.chamois_left_1 = new AdvancedModelBox(this, 163, 23);
        this.chamois_left_1.setRotationPoint(0.9000F, -1.5000F, -1.6000F);
        this.chamois_left_1.addBox(-0.3000F, -2.4000F, -0.3500F, 0.6000F, 2.8000F, 0.7000F, 0.0F);
        this.setRotateAngle(chamois_left_1, -0.1396F, 0.0F, 0.0698F);
        this.chamois_left_2 = new AdvancedModelBox(this, 181, 23);
        this.chamois_left_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.chamois_left_2.addBox(-0.2500F, -1.4000F, -0.2500F, 0.5000F, 1.8000F, 0.5000F, 0.0F);
        this.setRotateAngle(chamois_left_2, -2.2689F, 0.0F, 0.0F);
        this.chamois_right_1 = new AdvancedModelBox(this, 166, 23);
        this.chamois_right_1.setRotationPoint(-0.9000F, -1.5000F, -1.6000F);
        this.chamois_right_1.addBox(-0.3000F, -2.4000F, -0.3500F, 0.6000F, 2.8000F, 0.7000F, 0.0F);
        this.setRotateAngle(chamois_right_1, -0.1396F, 0.0F, -0.0698F);
        this.chamois_right_2 = new AdvancedModelBox(this, 183, 23);
        this.chamois_right_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.chamois_right_2.addBox(-0.2500F, -1.4000F, -0.2500F, 0.5000F, 1.8000F, 0.5000F, 0.0F);
        this.setRotateAngle(chamois_right_2, -2.2689F, 0.0F, 0.0F);
        this.dagger_left_1 = new AdvancedModelBox(this, 155, 23);
        this.dagger_left_1.setRotationPoint(0.9000F, -1.5000F, -1.6000F);
        this.dagger_left_1.addBox(-0.4000F, -2.4000F, -0.4000F, 0.8000F, 2.8000F, 0.8000F, 0.0F);
        this.setRotateAngle(dagger_left_1, -0.5760F, 0.0F, 0.1047F);
        this.dagger_left_2 = new AdvancedModelBox(this, 175, 23);
        this.dagger_left_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.dagger_left_2.addBox(-0.3000F, -1.8000F, -0.3000F, 0.6000F, 2.2000F, 0.6000F, 0.0F);
        this.setRotateAngle(dagger_left_2, -0.2967F, 0.0F, 0.0F);
        this.dagger_right_1 = new AdvancedModelBox(this, 159, 23);
        this.dagger_right_1.setRotationPoint(-0.9000F, -1.5000F, -1.6000F);
        this.dagger_right_1.addBox(-0.4000F, -2.4000F, -0.4000F, 0.8000F, 2.8000F, 0.8000F, 0.0F);
        this.setRotateAngle(dagger_right_1, -0.5760F, 0.0F, -0.1047F);
        this.dagger_right_2 = new AdvancedModelBox(this, 178, 23);
        this.dagger_right_2.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.dagger_right_2.addBox(-0.3000F, -1.8000F, -0.3000F, 0.6000F, 2.2000F, 0.6000F, 0.0F);
        this.setRotateAngle(dagger_right_2, -0.2967F, 0.0F, 0.0F);
        this.fore_left_upper = new AdvancedModelBox(this, 173, 0);
        this.fore_left_upper.setRotationPoint(3.0F, 1.3519F, -2.6000F);
        this.fore_left_upper.addBox(-1.4000F, -1.6000F, -1.8000F, 2.8000F, 5.2000F, 3.5000F, 0.0F);
        this.setRotateAngle(fore_left_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_left_forearm = new AdvancedModelBox(this, 230, 0);
        this.fore_left_forearm.setRotationPoint(-0.2500F, 3.6000F, 0.0F);
        this.fore_left_forearm.addBox(-0.7500F, -0.9000F, -1.3000F, 1.5000F, 3.9000F, 2.5000F, 0.0F);
        this.setRotateAngle(fore_left_forearm, 0.2793F, 0.0F, 0.0F);
        this.fore_left_cannon = new AdvancedModelBox(this, 20, 23);
        this.fore_left_cannon.setRotationPoint(-0.0500F, 3.0F, 0.0F);
        this.fore_left_cannon.addBox(-0.4000F, -0.8000F, -0.9000F, 0.8000F, 3.4000F, 1.8000F, 0.0F);
        this.setRotateAngle(fore_left_cannon, -0.1396F, 0.0F, 0.0F);
        this.fore_left_hoof = new AdvancedModelBox(this, 97, 23);
        this.fore_left_hoof.setRotationPoint(0.0250F, 2.6000F, 0.0F);
        this.fore_left_hoof.addBox(-0.6250F, -0.5000F, -1.3000F, 1.2500F, 1.7000F, 1.8000F, 0.0F);
        this.setRotateAngle(fore_left_hoof, 0.0349F, 0.0F, 0.0F);
        this.fore_right_upper = new AdvancedModelBox(this, 186, 0);
        this.fore_right_upper.setRotationPoint(-3.0F, 1.3519F, -2.6000F);
        this.fore_right_upper.addBox(-1.4000F, -1.6000F, -1.8000F, 2.8000F, 5.2000F, 3.5000F, 0.0F);
        this.setRotateAngle(fore_right_upper, -0.0698F, 0.0F, 0.0F);
        this.fore_right_forearm = new AdvancedModelBox(this, 238, 0);
        this.fore_right_forearm.setRotationPoint(0.2500F, 3.6000F, 0.0F);
        this.fore_right_forearm.addBox(-0.7500F, -0.9000F, -1.3000F, 1.5000F, 3.9000F, 2.5000F, 0.0F);
        this.setRotateAngle(fore_right_forearm, 0.2793F, 0.0F, 0.0F);
        this.fore_right_cannon = new AdvancedModelBox(this, 26, 23);
        this.fore_right_cannon.setRotationPoint(0.0500F, 3.0F, 0.0F);
        this.fore_right_cannon.addBox(-0.4000F, -0.8000F, -0.9000F, 0.8000F, 3.4000F, 1.8000F, 0.0F);
        this.setRotateAngle(fore_right_cannon, -0.1396F, 0.0F, 0.0F);
        this.fore_right_hoof = new AdvancedModelBox(this, 104, 23);
        this.fore_right_hoof.setRotationPoint(-0.0250F, 2.6000F, 0.0F);
        this.fore_right_hoof.addBox(-0.6250F, -0.5000F, -1.3000F, 1.2500F, 1.7000F, 1.8000F, 0.0F);
        this.setRotateAngle(fore_right_hoof, 0.0349F, 0.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 77, 0);
        this.body_rump.setRotationPoint(0.0F, 0.0F, 5.0F);
        this.body_rump.addBox(-3.9000F, -3.0F, 0.0F, 7.8000F, 5.8000F, 5.6000F, 0.0F);
        this.setRotateAngle(body_rump, -0.1396F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 44, 23);
        this.tail.setRotationPoint(0.0F, -2.6000F, 5.4000F);
        this.tail.addBox(-0.9000F, -0.4000F, -0.5000F, 1.8000F, 2.2000F, 2.3000F, 0.0F);
        this.setRotateAngle(tail, 0.6109F, 0.0F, 0.0F);
        this.hind_left_thigh = new AdvancedModelBox(this, 126, 0);
        this.hind_left_thigh.setRotationPoint(3.0F, 0.5787F, 3.4000F);
        this.hind_left_thigh.addBox(-1.5000F, -1.8000F, -2.0F, 3.0F, 5.6000F, 4.0F, 0.0F);
        this.setRotateAngle(hind_left_thigh, -0.2094F, 0.0F, 0.0F);
        this.hind_left_gaskin = new AdvancedModelBox(this, 212, 0);
        this.hind_left_gaskin.setRotationPoint(-0.2500F, 3.8000F, 0.0F);
        this.hind_left_gaskin.addBox(-0.8500F, -1.0F, -1.3000F, 1.7000F, 4.2000F, 2.6000F, 0.0F);
        this.setRotateAngle(hind_left_gaskin, 0.8378F, 0.0F, 0.0F);
        this.hind_left_cannon = new AdvancedModelBox(this, 32, 23);
        this.hind_left_cannon.setRotationPoint(-0.0500F, 3.2000F, 0.0F);
        this.hind_left_cannon.addBox(-0.5000F, -0.8000F, -0.9000F, 1.0F, 3.6000F, 1.8000F, 0.0F);
        this.setRotateAngle(hind_left_cannon, -0.6632F, 0.0F, 0.0F);
        this.hind_left_hoof = new AdvancedModelBox(this, 111, 23);
        this.hind_left_hoof.setRotationPoint(0.0250F, 2.8000F, 0.0F);
        this.hind_left_hoof.addBox(-0.7250F, -0.5000F, -1.3000F, 1.4500F, 1.7000F, 1.8000F, 0.0F);
        this.setRotateAngle(hind_left_hoof, 0.1745F, 0.0F, 0.0F);
        this.hind_right_thigh = new AdvancedModelBox(this, 140, 0);
        this.hind_right_thigh.setRotationPoint(-3.0F, 0.5787F, 3.4000F);
        this.hind_right_thigh.addBox(-1.5000F, -1.8000F, -2.0F, 3.0F, 5.6000F, 4.0F, 0.0F);
        this.setRotateAngle(hind_right_thigh, -0.2094F, 0.0F, 0.0F);
        this.hind_right_gaskin = new AdvancedModelBox(this, 221, 0);
        this.hind_right_gaskin.setRotationPoint(0.2500F, 3.8000F, 0.0F);
        this.hind_right_gaskin.addBox(-0.8500F, -1.0F, -1.3000F, 1.7000F, 4.2000F, 2.6000F, 0.0F);
        this.setRotateAngle(hind_right_gaskin, 0.8378F, 0.0F, 0.0F);
        this.hind_right_cannon = new AdvancedModelBox(this, 38, 23);
        this.hind_right_cannon.setRotationPoint(0.0500F, 3.2000F, 0.0F);
        this.hind_right_cannon.addBox(-0.5000F, -0.8000F, -0.9000F, 1.0F, 3.6000F, 1.8000F, 0.0F);
        this.setRotateAngle(hind_right_cannon, -0.6632F, 0.0F, 0.0F);
        this.hind_right_hoof = new AdvancedModelBox(this, 118, 23);
        this.hind_right_hoof.setRotationPoint(-0.0250F, 2.8000F, 0.0F);
        this.hind_right_hoof.addBox(-0.7250F, -0.5000F, -1.3000F, 1.4500F, 1.7000F, 1.8000F, 0.0F);
        this.setRotateAngle(hind_right_hoof, 0.1745F, 0.0F, 0.0F);

        this.body_barrel.addChild(this.body_chest);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.muzzle);
        this.muzzle.addChild(this.nose);
        this.muzzle.addChild(this.beard);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.ibex_left_1);
        this.ibex_left_1.addChild(this.ibex_left_2);
        this.ibex_left_2.addChild(this.ibex_left_3);
        this.ibex_left_3.addChild(this.ibex_left_4);
        this.head.addChild(this.ibex_right_1);
        this.ibex_right_1.addChild(this.ibex_right_2);
        this.ibex_right_2.addChild(this.ibex_right_3);
        this.ibex_right_3.addChild(this.ibex_right_4);
        this.head.addChild(this.ram_left_1);
        this.ram_left_1.addChild(this.ram_left_2);
        this.ram_left_2.addChild(this.ram_left_3);
        this.ram_left_3.addChild(this.ram_left_4);
        this.head.addChild(this.ram_right_1);
        this.ram_right_1.addChild(this.ram_right_2);
        this.ram_right_2.addChild(this.ram_right_3);
        this.ram_right_3.addChild(this.ram_right_4);
        this.head.addChild(this.chamois_left_1);
        this.chamois_left_1.addChild(this.chamois_left_2);
        this.head.addChild(this.chamois_right_1);
        this.chamois_right_1.addChild(this.chamois_right_2);
        this.head.addChild(this.dagger_left_1);
        this.dagger_left_1.addChild(this.dagger_left_2);
        this.head.addChild(this.dagger_right_1);
        this.dagger_right_1.addChild(this.dagger_right_2);
        this.body_chest.addChild(this.fore_left_upper);
        this.fore_left_upper.addChild(this.fore_left_forearm);
        this.fore_left_forearm.addChild(this.fore_left_cannon);
        this.fore_left_cannon.addChild(this.fore_left_hoof);
        this.body_chest.addChild(this.fore_right_upper);
        this.fore_right_upper.addChild(this.fore_right_forearm);
        this.fore_right_forearm.addChild(this.fore_right_cannon);
        this.fore_right_cannon.addChild(this.fore_right_hoof);
        this.body_barrel.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.body_rump.addChild(this.hind_left_thigh);
        this.hind_left_thigh.addChild(this.hind_left_gaskin);
        this.hind_left_gaskin.addChild(this.hind_left_cannon);
        this.hind_left_cannon.addChild(this.hind_left_hoof);
        this.body_rump.addChild(this.hind_right_thigh);
        this.hind_right_thigh.addChild(this.hind_right_gaskin);
        this.hind_right_gaskin.addChild(this.hind_right_cannon);
        this.hind_right_cannon.addChild(this.hind_right_hoof);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_barrel);
    }

    // Built once: getAllParts() is called every frame, and a fresh ImmutableList per animal per frame
    // was pure garbage (2.8.0, same as ModelBison).
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_barrel,
                body_chest,
                neck,
                head,
                muzzle,
                nose,
                beard,
                ear_left,
                eye_left,
                ear_right,
                eye_right,
                ibex_left_1,
                ibex_left_2,
                ibex_left_3,
                ibex_left_4,
                ibex_right_1,
                ibex_right_2,
                ibex_right_3,
                ibex_right_4,
                ram_left_1,
                ram_left_2,
                ram_left_3,
                ram_left_4,
                ram_right_1,
                ram_right_2,
                ram_right_3,
                ram_right_4,
                chamois_left_1,
                chamois_left_2,
                chamois_right_1,
                chamois_right_2,
                dagger_left_1,
                dagger_left_2,
                dagger_right_1,
                dagger_right_2,
                fore_left_upper,
                fore_left_forearm,
                fore_left_cannon,
                fore_left_hoof,
                fore_right_upper,
                fore_right_forearm,
                fore_right_cannon,
                fore_right_hoof,
                body_rump,
                tail,
                hind_left_thigh,
                hind_left_gaskin,
                hind_left_cannon,
                hind_left_hoof,
                hind_right_thigh,
                hind_right_gaskin,
                hind_right_cannon,
                hind_right_hoof
        );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityCaprine goat = (EntityCaprine) entityIn;
        animator.update(goat);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py caprine
        // EAT (40t, GrazeGoal's mouthful): head down to the grass, a cropping lift, back down
        animator.setAnimation(EntityCaprine.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 50.8F, 0.0F, 0.0F);
        this.rotate(animator, head, 5.5F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 2.8F, 0.0F, 0.0F);
        this.rotate(animator, nose, 2.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.1F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 50.8F, 0.0F, 0.0F);
        this.rotate(animator, head, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 2.8F, 0.0F, 0.0F);
        this.rotate(animator, nose, 2.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.1F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 50.8F, 0.0F, 0.0F);
        this.rotate(animator, head, 5.5F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 2.8F, 0.0F, 0.0F);
        this.rotate(animator, nose, 2.7F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.1F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(10);
        // DRINK (100t, DrinkGoal): muzzle to the water past the bank edge, two swallows lifting the head
        animator.setAnimation(EntityCaprine.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck, 66.4F, 0.0F, 0.0F);
        this.rotate(animator, head, -1.8F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 0.7F, 0.0F, 0.0F);
        this.rotate(animator, nose, 4.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 66.4F, 0.0F, 0.0F);
        this.rotate(animator, head, -6.8F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 0.7F, 0.0F, 0.0F);
        this.rotate(animator, nose, 4.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 66.4F, 0.0F, 0.0F);
        this.rotate(animator, head, -1.8F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 0.7F, 0.0F, 0.0F);
        this.rotate(animator, nose, 4.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(20);
        animator.startKeyframe(5);
        this.rotate(animator, neck, 66.4F, 0.0F, 0.0F);
        this.rotate(animator, head, -6.8F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 0.7F, 0.0F, 0.0F);
        this.rotate(animator, nose, 4.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.startKeyframe(5);
        this.rotate(animator, neck, 66.4F, 0.0F, 0.0F);
        this.rotate(animator, head, -1.8F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 0.7F, 0.0F, 0.0F);
        this.rotate(animator, nose, 4.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 6.0F, -0.4F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.resetKeyframe(15);
        // ALERT (40t, AlertGoal): head raised, ears up, staring at the threat; repeats while the alert lasts
        animator.setAnimation(EntityCaprine.ALERT);
        animator.startKeyframe(8);
        this.rotate(animator, neck, -35.0F, 0.0F, 0.0F);
        this.rotate(animator, head, -9.0F, 0.0F, 0.0F);
        this.rotate(animator, muzzle, 12.0F, 0.0F, 0.0F);
        this.rotate(animator, nose, 12.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(24);
        animator.resetKeyframe(8);
        // </gen:idle-poses>

        // ATTACK_BUTT (14t): head drops, horns forward, then the butt drives up and through
        animator.setAnimation(EntityCaprine.ATTACK_BUTT);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 30.0F, 0, 0);
        this.rotate(animator, head, 20.0F, 0, 0);
        this.rotate(animator, body_chest, 6.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        animator.move(body_barrel, 0, 0, -1.5F);
        this.rotate(animator, neck, -10.0F, 0, 0);
        this.rotate(animator, head, -12.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // ATTACK_CLASH (36t): the rut clash -- rear up on the hind legs, hold, drop and smash horns forward
        animator.setAnimation(EntityCaprine.ATTACK_CLASH);
        animator.startKeyframe(10);
        this.rotate(animator, body_barrel, -32.0F, 0, 0);
        animator.move(body_barrel, 0, -3.0F, 2.0F);
        this.rotate(animator, hind_left_thigh, 30.0F, 0, 0);
        this.rotate(animator, hind_right_thigh, 30.0F, 0, 0);
        this.rotate(animator, fore_left_upper, -35.0F, 0, 0);
        this.rotate(animator, fore_right_upper, -35.0F, 0, 0);
        this.rotate(animator, neck, 10.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.startKeyframe(5);
        animator.move(body_barrel, 0, 0, -2.0F);
        this.rotate(animator, neck, 34.0F, 0, 0);
        this.rotate(animator, head, 22.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(5);
        animator.resetKeyframe(10);
    }

    public void setupAnim(EntityCaprine goat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(goat);
        limbSwingAmount = Math.min(0.5F, limbSwingAmount);
        float gait = limbSwing * 0.7F / Math.max(0.3F, goat.getMobSize() * goat.getScale());

        // ---- per-species horns + beard ----
        int variant = goat.getVariant();
        boolean ibex = variant == 0 || variant == 1;
        this.ibex_left_1.showModel = ibex;
        this.ibex_right_1.showModel = ibex;
        boolean ram = variant == 2 || variant == 3;
        this.ram_left_1.showModel = ram;
        this.ram_right_1.showModel = ram;
        boolean chamois = variant == 4;
        this.chamois_left_1.showModel = chamois;
        this.chamois_right_1.showModel = chamois;
        boolean dagger = variant == 5;
        this.dagger_left_1.showModel = dagger;
        this.dagger_right_1.showModel = dagger;
        this.beard.showModel = variant == 0 || variant == 1 || variant == 5;

        // ---- idle ----
        float breath = (float) (Math.sin(ageInTicks / 20) * 0.03F);
        this.body_barrel.setScale(1.0F + breath, 1.0F + breath, 1.0F);
        this.swing(ear_left, 0.2F, 0.18F, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.2F, 0.18F, false, 1F, 0F, ageInTicks, 1);
        this.swing(tail, 0.25F, 0.2F, false, 0F, 0F, ageInTicks, 1);
        this.walk(beard, 0.12F, 0.08F, false, 0F, 0F, ageInTicks, 1);

        if (!goat.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(1.2925F, -0.6000F, -2.0F);
            this.eye_right.setRotationPoint(-1.2925F, -0.6000F, -2.0F);
        }

        if (!goat.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 2, head);
        }

        if (goat.isInWater() && !goat.isOnGround()) {
            gait = ageInTicks / 5;
            limbSwingAmount = 0.4F;
            this.setRotateAngle(neck, -0.3F, 0, 0);
        }

        // ---- gait: a quick, short-strided diagonal walk/trot, the body bobbing over stiff sure-footed legs ----
        if (goat.canMove()) {
            float spd = 1.0F;
            bob(body_barrel, 2.0F * spd, 0.5F, true, gait, limbSwingAmount);
            walk(neck, spd, 0.08F, false, 0, 0, gait, limbSwingAmount);
            walk(fore_left_upper, spd, 0.75F, false, 0F, 0F, gait, limbSwingAmount);
            walk(fore_left_forearm, spd, 0.45F, false, -1.3F, 0.15F, gait, limbSwingAmount);
            walk(fore_left_cannon, spd, 0.30F, false, -1.3F, 0F, gait, limbSwingAmount);
            walk(fore_right_upper, spd, 0.75F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(fore_right_forearm, spd, 0.45F, false, Mth.PI - 1.3F, 0.15F, gait, limbSwingAmount);
            walk(fore_right_cannon, spd, 0.30F, false, Mth.PI - 1.3F, 0F, gait, limbSwingAmount);
            walk(hind_left_thigh, spd, 0.70F, false, Mth.PI, 0F, gait, limbSwingAmount);
            walk(hind_left_gaskin, spd, 0.50F, true, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_left_cannon, spd, 0.30F, false, Mth.PI - 1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_thigh, spd, 0.70F, false, 0F, 0F, gait, limbSwingAmount);
            walk(hind_right_gaskin, spd, 0.50F, true, -1.2F, 0F, gait, limbSwingAmount);
            walk(hind_right_cannon, spd, 0.30F, false, -1.2F, 0F, gait, limbSwingAmount);
        }

        if (goat.sitProgress > 0) {
            // SOLVED (caprine_spec.py --pose): sternal recumbency, every leg box above the floor
            this.progressPosition(body_barrel, goat.sitProgress, 0.0F, 19.0F, 0.0F, 40);
            this.progressRotation(fore_left_upper, goat.sitProgress, -1.4245F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, goat.sitProgress, 1.8437F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, goat.sitProgress, -1.8754F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, goat.sitProgress, 0.1159F, 0, 0, 40);
            this.progressRotation(fore_right_upper, goat.sitProgress, -1.4245F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, goat.sitProgress, 1.8437F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, goat.sitProgress, -1.8754F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, goat.sitProgress, 0.1159F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, goat.sitProgress, -1.2253F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, goat.sitProgress, 2.1637F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, goat.sitProgress, -2.3514F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, goat.sitProgress, 0.1025F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, goat.sitProgress, -1.2253F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, goat.sitProgress, 2.1637F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, goat.sitProgress, -2.3514F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, goat.sitProgress, 0.1025F, 0, 0, 40);
            this.progressRotation(neck, goat.sitProgress, -0.6981F, 0, 0, 40);
        }
        else if (goat.sleepProgress > 0) {
            // SOLVED (caprine_spec.py --pose): sternal recumbency, every leg box above the floor
            this.progressPosition(body_barrel, goat.sleepProgress, 0.0F, 19.8000F, 0.0F, 40);
            this.progressRotation(fore_left_upper, goat.sleepProgress, -1.6169F, 0, 0, 40);
            this.progressRotation(fore_left_forearm, goat.sleepProgress, 2.1338F, 0, 0, 40);
            this.progressRotation(fore_left_cannon, goat.sleepProgress, -1.9743F, 0, 0, 40);
            this.progressRotation(fore_left_hoof, goat.sleepProgress, 0.1055F, 0, 0, 40);
            this.progressRotation(fore_right_upper, goat.sleepProgress, -1.6169F, 0, 0, 40);
            this.progressRotation(fore_right_forearm, goat.sleepProgress, 2.1338F, 0, 0, 40);
            this.progressRotation(fore_right_cannon, goat.sleepProgress, -1.9743F, 0, 0, 40);
            this.progressRotation(fore_right_hoof, goat.sleepProgress, 0.1055F, 0, 0, 40);
            this.progressRotation(hind_left_thigh, goat.sleepProgress, -1.6012F, 0, 0, 40);
            this.progressRotation(hind_left_gaskin, goat.sleepProgress, 2.1843F, 0, 0, 40);
            this.progressRotation(hind_left_cannon, goat.sleepProgress, -2.0107F, 0, 0, 40);
            this.progressRotation(hind_left_hoof, goat.sleepProgress, 0.1209F, 0, 0, 40);
            this.progressRotation(hind_right_thigh, goat.sleepProgress, -1.6012F, 0, 0, 40);
            this.progressRotation(hind_right_gaskin, goat.sleepProgress, 2.1843F, 0, 0, 40);
            this.progressRotation(hind_right_cannon, goat.sleepProgress, -2.0107F, 0, 0, 40);
            this.progressRotation(hind_right_hoof, goat.sleepProgress, 0.1209F, 0, 0, 40);
            this.progressRotation(neck, goat.sleepProgress, -0.4538F, 0, 0, 40);
        }
    }
}
