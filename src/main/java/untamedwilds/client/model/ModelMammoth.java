package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityMammoth;

/**
 * Proboscideans -- mammoths, mastodons, gomphotheres and elephants. FULL REBUILD (2026-11-15) of the July rig, which was
 * sculpted by eye before the remodelling SOP: its ears were flat slabs 2.5 cm proud of the skull, every species wore the
 * mammoth's domed head, hump and 185-degree spiral tusks, the body was 0.60 as wide as long and the legs were stacked planks.
 *
 * 57 boxes on 256x256, authored in scripts/proboscidean_spec.py, ported VERBATIM by scripts/gen_model_mammoth.py and proved
 * by verify_port.py. One mesh serves every species through FAMILIES read from the species flags (0 = the mammoth form, so a
 * species without the new flags is a mammoth):
 *   headShape 0 DOME / 1 TWIN (Asian) / 2 SLOPE (African) / 3 CREST (Palaeoloxodon) / 4 FLAT (mastodons, gomphotheres)
 *   backShape 0 SLOPE (mammoth hump) / 1 DIP (African) / 2 ARCH (Asian) / 3 LEVEL
 *   tuskShape 0 SPIRAL / 1 FORWARD / 2 STRAIGHT / 3 UPCURVE / 4 TWIST / 5 SMALL (first three segments only) / 6 STEGO
 *   earSize 0 / 1 / 2 (hinged flaps yawed off the skull), hasLargeTusks (x1.35 whole chain), hasWoollyCoat.
 * The model instance is SHARED by every proboscidean, so every family setting is written every frame, and before the
 * keyframe animations, which add on top of it.
 */
public class ModelMammoth extends AdvancedEntityModel<EntityMammoth> {

    private final AdvancedModelBox body_mid;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_withers;
    private final AdvancedModelBox body_hump;
    private final AdvancedModelBox fur_mane;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox head_dome;
    private final AdvancedModelBox head_dome_left;
    private final AdvancedModelBox head_dome_right;
    private final AdvancedModelBox head_crest;
    private final AdvancedModelBox jaw;
    private final AdvancedModelBox tusk_sheath_left;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox tusk_sheath_right;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox trunk_1;
    private final AdvancedModelBox trunk_2;
    private final AdvancedModelBox trunk_3;
    private final AdvancedModelBox trunk_4;
    private final AdvancedModelBox trunk_5;
    private final AdvancedModelBox trunk_6;
    private final AdvancedModelBox tusk_left_1;
    private final AdvancedModelBox tusk_left_2;
    private final AdvancedModelBox tusk_left_3;
    private final AdvancedModelBox tusk_left_4;
    private final AdvancedModelBox tusk_left_5;
    private final AdvancedModelBox tusk_right_1;
    private final AdvancedModelBox tusk_right_2;
    private final AdvancedModelBox tusk_right_3;
    private final AdvancedModelBox tusk_right_4;
    private final AdvancedModelBox tusk_right_5;
    private final AdvancedModelBox fur_neck;
    private final AdvancedModelBox leg_front_left_1;
    private final AdvancedModelBox leg_front_left_2;
    private final AdvancedModelBox foot_front_left;
    private final AdvancedModelBox leg_front_right_1;
    private final AdvancedModelBox leg_front_right_2;
    private final AdvancedModelBox foot_front_right;
    private final AdvancedModelBox fur_chest;
    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox tail_tuft;
    private final AdvancedModelBox fur_rump;
    private final AdvancedModelBox body_croup;
    private final AdvancedModelBox leg_back_left_1;
    private final AdvancedModelBox leg_back_left_2;
    private final AdvancedModelBox foot_back_left;
    private final AdvancedModelBox leg_back_right_1;
    private final AdvancedModelBox leg_back_right_2;
    private final AdvancedModelBox foot_back_right;
    private final AdvancedModelBox body_back;
    private final AdvancedModelBox fur_skirt_left;
    private final AdvancedModelBox fur_skirt_right;

    private final ModelAnimator animator;

    // ---- family tables (generated; Java axes) ----
    // TUSK_ROT[tuskShape][side 0 left / 1 right][segment][x, y, z] -- each family authored by world direction per segment
    private static final float[][][][] TUSK_ROT = {
        {   // SPIRAL
            {{-0.4117F, -0.0755F, -0.3577F}, {-0.5713F, -0.1200F, -0.1871F}, {-0.7182F, -0.2067F, 0.0665F}, {-0.8297F, -0.6569F, 1.0075F}, {1.2481F, -1.4719F, -0.5634F}},
            {{-0.4117F, 0.0755F, 0.3577F}, {-0.5713F, 0.1200F, 0.1871F}, {-0.7182F, 0.2067F, -0.0665F}, {-0.8297F, 0.6569F, -1.0075F}, {1.2481F, 1.4719F, 0.5634F}}
        },
        {   // FORWARD
            {{-0.4134F, -0.0318F, -0.1512F}, {-0.4584F, -0.0363F, -0.0496F}, {-0.4262F, -0.0458F, 0.0091F}, {-0.3616F, -0.0667F, 0.0607F}, {-0.2625F, -0.1003F, 0.1005F}},
            {{-0.4134F, 0.0318F, 0.1512F}, {-0.4584F, 0.0363F, 0.0496F}, {-0.4262F, 0.0458F, -0.0091F}, {-0.3616F, 0.0667F, -0.0607F}, {-0.2625F, 0.1003F, -0.1005F}}
        },
        {   // STRAIGHT
            {{-0.6703F, -0.0210F, -0.0602F}, {-0.1778F, -0.0103F, 0.0176F}, {-0.1370F, -0.0141F, 0.0293F}, {-0.1306F, -0.0151F, 0.0315F}, {-0.1483F, -0.0165F, 0.0351F}},
            {{-0.6703F, 0.0210F, 0.0602F}, {-0.1778F, 0.0103F, -0.0176F}, {-0.1370F, 0.0141F, -0.0293F}, {-0.1306F, 0.0151F, -0.0315F}, {-0.1483F, 0.0165F, -0.0351F}}
        },
        {   // UPCURVE
            {{-0.6569F, -0.0480F, -0.1406F}, {-0.4644F, -0.0320F, -0.0247F}, {-0.3742F, -0.0330F, -0.0007F}, {-0.3279F, -0.0485F, 0.0282F}, {-0.2396F, -0.0786F, 0.0604F}},
            {{-0.6569F, 0.0480F, 0.1406F}, {-0.4644F, 0.0320F, 0.0247F}, {-0.3742F, 0.0330F, 0.0007F}, {-0.3279F, 0.0485F, -0.0282F}, {-0.2396F, 0.0786F, -0.0604F}}
        },
        {   // TWIST
            {{-0.5594F, -0.0640F, -0.2221F}, {-0.5720F, -0.0674F, -0.3047F}, {-0.5396F, -0.1430F, -0.0248F}, {-0.3991F, -0.3878F, 0.3600F}, {-0.0472F, -0.4893F, 0.3095F}},
            {{-0.5594F, 0.0640F, 0.2221F}, {-0.5720F, 0.0674F, 0.3047F}, {-0.5396F, 0.1430F, 0.0248F}, {-0.3991F, 0.3878F, -0.3600F}, {-0.0472F, 0.4893F, -0.3095F}}
        },
        {   // SMALL
            {{-0.3888F, -0.0237F, -0.1200F}, {-0.5230F, -0.0342F, -0.0362F}, {-0.3942F, -0.0365F, 0.0177F}, {-0.3567F, -0.0361F, 0.0208F}, {-0.2625F, -0.1003F, 0.1005F}},
            {{-0.3888F, 0.0237F, 0.1200F}, {-0.5230F, 0.0342F, 0.0362F}, {-0.3942F, 0.0365F, -0.0177F}, {-0.3567F, 0.0361F, -0.0208F}, {-0.2625F, 0.1003F, -0.1005F}}
        },
        {   // STEGO (2.9.0, Stegodon: close-set, converging ahead of the trunk -- scripts/stegodon_tusk_design.py)
            {{-0.7356F, 0.0077F, 0.0200F}, {-0.2993F, -0.0275F, 0.1385F}, {-0.2708F, -0.0642F, 0.2168F}, {-0.2832F, 0.1353F, -0.1331F}, {-0.2013F, 0.1419F, -0.1419F}},
            {{-0.7356F, -0.0077F, -0.0200F}, {-0.2993F, 0.0275F, -0.1385F}, {-0.2708F, 0.0642F, -0.2168F}, {-0.2832F, -0.1353F, 0.1331F}, {-0.2013F, -0.1419F, 0.1419F}}
        },
    };
    // BACK_SCALE[backShape] = Y scale of {body_withers, body_back, body_croup} about each box's own pivot
    private static final float[][] BACK_SCALE = {{1.0F, 0.8000F, 0.5500F}, {1.0F, 0.8800F, 0.9600F}, {0.8600F, 1.5500F, 0.8000F}, {1.0F, 1.2200F, 1.0F}};   // SLOPE / DIP / ARCH / LEVEL
    // extra pitch per headShape (rad): head tilts the face, neck raises or lowers the carriage
    private static final float[] HEAD_PITCH = {0.0F, 0.0F, -0.2443F, 0.0F, -0.1396F};   // DOME / TWIN / SLOPE / CREST / FLAT
    private static final float[] NECK_PITCH = {0.0F, 0.0524F, -0.0349F, 0.0F, 0.1047F};
    // EAR[earSize] = {yaw off the skull (rad), Y scale, Z scale, pivot y, pivot z}; the hinge is the ear's top front corner
    private static final float[][] EAR = {{0.4189F, 0.2941F, 0.2778F, -4.0F, -3.5000F}, {0.4538F, 0.5882F, 0.5556F, -5.0F, -3.0F}, {0.5236F, 1.0F, 1.0F, -7.5000F, -3.0F}};
    // SOLVED rest-pose head lift per headShape (rad, split 60% neck / 40% head): the trunk and tusks clear the ground
    // resting, and rest on it asleep
    private static final float[] SIT_LIFT = {0.3927F, 0.4451F, 0.1222F, 0.3927F, 0.3665F};
    private static final float[] SLEEP_LIFT = {0.2793F, 0.3316F, 0.0087F, 0.3229F, 0.2531F};

    public ModelMammoth() {
        this.texWidth = 256;
        this.texHeight = 256;

        this.body_mid = new AdvancedModelBox(this, 0, 0);
        this.body_mid.setRotationPoint(0.0F, -10.0F, 0.0F);
        this.body_mid.addBox(-11.0F, -6.5000F, -13.0F, 22.0F, 19.0F, 25.0F, 0.0F);
        this.body_chest = new AdvancedModelBox(this, 150, 42);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -10.0F);
        this.body_chest.addBox(-10.0F, -7.5000F, -18.0F, 20.0F, 17.5000F, 20.0F, 0.0F);
        this.body_withers = new AdvancedModelBox(this, 0, 44);
        this.body_withers.setRotationPoint(0.0F, -5.0F, -7.0F);
        this.body_withers.addBox(-8.0F, -9.0F, -9.5000F, 16.0F, 14.0F, 19.0F, 0.0F);
        this.body_hump = new AdvancedModelBox(this, 175, 80);
        this.body_hump.setRotationPoint(0.0F, -6.0F, 0.0F);
        this.body_hump.addBox(-7.2000F, -7.5000F, -8.0F, 14.4000F, 11.5000F, 16.0F, 0.0F);
        this.setRotateAngle(body_hump, -0.2094F, 0.0F, 0.0F);
        this.fur_mane = new AdvancedModelBox(this, 94, 0);
        this.fur_mane.setRotationPoint(0.0F, -8.0F, -1.0F);
        this.fur_mane.addBox(-9.1000F, -26.0F, -6.5000F, 18.2000F, 34.0F, 9.5000F, 0.0F);
        this.setRotateAngle(fur_mane, -1.8151F, 0.0F, 0.0F);
        this.neck = new AdvancedModelBox(this, 32, 156);
        this.neck.setRotationPoint(0.0F, -4.0F, -16.0F);
        this.neck.addBox(-7.0F, -6.0F, -6.5000F, 14.0F, 14.0F, 7.5000F, 0.0F);
        this.head = new AdvancedModelBox(this, 70, 76);
        this.head.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.head.addBox(-7.5000F, -6.0F, -11.0F, 15.0F, 20.0F, 12.0F, 0.0F);
        this.head_dome = new AdvancedModelBox(this, 47, 178);
        this.head_dome.setRotationPoint(0.0F, -5.0F, -5.0F);
        this.head_dome.addBox(-6.0F, -6.0F, -5.0F, 12.0F, 8.5000F, 10.0F, 0.0F);
        this.setRotateAngle(head_dome, 0.1047F, 0.0F, 0.0F);
        this.head_dome_left = new AdvancedModelBox(this, 200, 193);
        this.head_dome_left.setRotationPoint(3.4000F, -5.0F, -5.0F);
        this.head_dome_left.addBox(-2.6000F, -4.5000F, -4.5000F, 5.6000F, 6.5000F, 8.5000F, 0.0F);
        this.head_dome_right = new AdvancedModelBox(this, 91, 195);
        this.head_dome_right.setRotationPoint(-3.4000F, -5.0F, -5.0F);
        this.head_dome_right.addBox(-3.0F, -4.5000F, -4.5000F, 5.6000F, 6.5000F, 8.5000F, 0.0F);
        this.head_crest = new AdvancedModelBox(this, 91, 178);
        this.head_crest.setRotationPoint(0.0F, -5.0F, -7.0F);
        this.head_crest.addBox(-6.8000F, -3.5000F, -7.0F, 13.6000F, 7.0F, 10.0F, 0.0F);
        this.setRotateAngle(head_crest, -0.2443F, 0.0F, 0.0F);
        this.jaw = new AdvancedModelBox(this, 0, 197);
        this.jaw.setRotationPoint(0.0F, 12.5000F, -3.0F);
        this.jaw.addBox(-3.4000F, -0.3000F, -6.5000F, 6.8000F, 4.3000F, 7.5000F, 0.0F);
        this.setRotateAngle(jaw, 0.2094F, 0.0F, 0.0F);
        this.tusk_sheath_left = new AdvancedModelBox(this, 52, 197);
        this.tusk_sheath_left.setRotationPoint(6.3000F, 11.0F, -9.0F);
        this.tusk_sheath_left.addBox(-2.2500F, -2.5000F, -3.5000F, 4.5000F, 6.1000F, 5.5000F, 0.0F);
        this.eye_left = new AdvancedModelBox(this, 223, 208);
        this.eye_left.setRotationPoint(7.8500F, 2.5000F, -7.0F);
        this.eye_left.addBox(0.0F, -1.0F, -1.5000F, 0.0F, 2.0F, 3.0F, 0.0F);
        this.ear_left = new AdvancedModelBox(this, 150, 0);
        this.ear_left.setRotationPoint(7.6000F, -7.5000F, -3.0F);
        this.ear_left.addBox(0.0F, 0.0F, 0.0F, 1.2000F, 23.8000F, 18.0F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, 0.5236F, 0.0F);
        this.tusk_sheath_right = new AdvancedModelBox(this, 229, 205);
        this.tusk_sheath_right.setRotationPoint(-6.3000F, 11.0F, -9.0F);
        this.tusk_sheath_right.addBox(-2.2500F, -2.5000F, -3.5000F, 4.5000F, 6.1000F, 5.5000F, 0.0F);
        this.eye_right = new AdvancedModelBox(this, 8, 209);
        this.eye_right.setRotationPoint(-7.8500F, 2.5000F, -7.0F);
        this.eye_right.addBox(0.0F, -1.0F, -1.5000F, 0.0F, 2.0F, 3.0F, 0.0F);
        this.ear_right = new AdvancedModelBox(this, 189, 0);
        this.ear_right.setRotationPoint(-7.6000F, -7.5000F, -3.0F);
        this.ear_right.addBox(-1.2000F, 0.0F, 0.0F, 1.2000F, 23.8000F, 18.0F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, -0.5236F, 0.0F);
        this.trunk_1 = new AdvancedModelBox(this, 170, 193);
        this.trunk_1.setRotationPoint(0.0F, 6.5000F, -12.0F);
        this.trunk_1.addBox(-4.6000F, -1.6000F, -2.8000F, 9.2000F, 8.6000F, 5.6000F, 0.0F);
        this.setRotateAngle(trunk_1, -0.5713F, 0.0F, 0.0F);
        this.trunk_2 = new AdvancedModelBox(this, 229, 193);
        this.trunk_2.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.trunk_2.addBox(-3.9000F, -1.2000F, -2.4000F, 7.8000F, 7.2000F, 4.8000F, 0.0F);
        this.setRotateAngle(trunk_2, 0.2655F, 0.0F, 0.0F);
        this.trunk_3 = new AdvancedModelBox(this, 29, 197);
        this.trunk_3.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.trunk_3.addBox(-3.6000F, -1.1000F, -2.1000F, 7.2000F, 7.1000F, 4.2000F, 0.0F);
        this.setRotateAngle(trunk_3, 0.2460F, 0.0F, 0.0F);
        this.trunk_4 = new AdvancedModelBox(this, 120, 208);
        this.trunk_4.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.trunk_4.addBox(-3.1000F, -1.0F, -1.8500F, 6.2000F, 6.5000F, 3.7000F, 0.0F);
        this.setRotateAngle(trunk_4, 0.1398F, 0.0F, 0.0F);
        this.trunk_5 = new AdvancedModelBox(this, 72, 197);
        this.trunk_5.setRotationPoint(0.0F, 5.5000F, 0.0F);
        this.trunk_5.addBox(-2.6000F, -1.0F, -1.6000F, 5.2000F, 6.0F, 3.2000F, 0.0F);
        this.setRotateAngle(trunk_5, 0.1627F, 0.0F, 0.0F);
        this.trunk_6 = new AdvancedModelBox(this, 190, 208);
        this.trunk_6.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.trunk_6.addBox(-2.2000F, -0.9000F, -1.4000F, 4.4000F, 4.9000F, 2.8000F, 0.0F);
        this.setRotateAngle(trunk_6, 0.2480F, 0.0F, 0.0F);
        this.tusk_left_1 = new AdvancedModelBox(this, 72, 207);
        this.tusk_left_1.setRotationPoint(6.3000F, 12.5000F, -11.0F);
        this.tusk_left_1.addBox(-1.4500F, -0.8000F, -1.4500F, 2.9000F, 6.8000F, 2.9000F, 0.0F);
        this.setRotateAngle(tusk_left_1, -0.4134F, -0.0318F, -0.1512F);
        this.tusk_left_2 = new AdvancedModelBox(this, 152, 208);
        this.tusk_left_2.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.tusk_left_2.addBox(-1.2500F, -0.9000F, -1.2500F, 2.5000F, 6.9000F, 2.5000F, 0.0F);
        this.setRotateAngle(tusk_left_2, -0.4584F, -0.0363F, -0.0496F);
        this.tusk_left_3 = new AdvancedModelBox(this, 172, 208);
        this.tusk_left_3.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.tusk_left_3.addBox(-1.0500F, -0.9000F, -1.0500F, 2.1000F, 6.4000F, 2.1000F, 0.0F);
        this.setRotateAngle(tusk_left_3, -0.4262F, -0.0458F, 0.0091F);
        this.tusk_left_4 = new AdvancedModelBox(this, 215, 208);
        this.tusk_left_4.setRotationPoint(0.0F, 5.5000F, 0.0F);
        this.tusk_left_4.addBox(-0.9000F, -0.9000F, -0.9000F, 1.8000F, 5.9000F, 1.8000F, 0.0F);
        this.setRotateAngle(tusk_left_4, -0.3616F, -0.0667F, 0.0607F);
        this.tusk_left_5 = new AdvancedModelBox(this, 249, 205);
        this.tusk_left_5.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.tusk_left_5.addBox(-0.7500F, -0.8000F, -0.7500F, 1.5000F, 5.3000F, 1.5000F, 0.0F);
        this.setRotateAngle(tusk_left_5, -0.2625F, -0.1003F, 0.1005F);
        this.tusk_right_1 = new AdvancedModelBox(this, 140, 208);
        this.tusk_right_1.setRotationPoint(-6.3000F, 12.5000F, -11.0F);
        this.tusk_right_1.addBox(-1.4500F, -0.8000F, -1.4500F, 2.9000F, 6.8000F, 2.9000F, 0.0F);
        this.setRotateAngle(tusk_right_1, -0.4134F, 0.0318F, 0.1512F);
        this.tusk_right_2 = new AdvancedModelBox(this, 162, 208);
        this.tusk_right_2.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.tusk_right_2.addBox(-1.2500F, -0.9000F, -1.2500F, 2.5000F, 6.9000F, 2.5000F, 0.0F);
        this.setRotateAngle(tusk_right_2, -0.4584F, 0.0363F, 0.0496F);
        this.tusk_right_3 = new AdvancedModelBox(this, 181, 208);
        this.tusk_right_3.setRotationPoint(0.0F, 6.0F, 0.0F);
        this.tusk_right_3.addBox(-1.0500F, -0.9000F, -1.0500F, 2.1000F, 6.4000F, 2.1000F, 0.0F);
        this.setRotateAngle(tusk_right_3, -0.4262F, 0.0458F, -0.0091F);
        this.tusk_right_4 = new AdvancedModelBox(this, 0, 209);
        this.tusk_right_4.setRotationPoint(0.0F, 5.5000F, 0.0F);
        this.tusk_right_4.addBox(-0.9000F, -0.9000F, -0.9000F, 1.8000F, 5.9000F, 1.8000F, 0.0F);
        this.setRotateAngle(tusk_right_4, -0.3616F, 0.0667F, -0.0607F);
        this.tusk_right_5 = new AdvancedModelBox(this, 84, 207);
        this.tusk_right_5.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.tusk_right_5.addBox(-0.7500F, -0.8000F, -0.7500F, 1.5000F, 5.3000F, 1.5000F, 0.0F);
        this.setRotateAngle(tusk_right_5, -0.2625F, 0.1003F, -0.1005F);
        this.fur_neck = new AdvancedModelBox(this, 124, 80);
        this.fur_neck.setRotationPoint(0.0F, 0.0F, -2.0F);
        this.fur_neck.addBox(-8.8000F, -9.5000F, -2.6000F, 17.6000F, 20.5000F, 7.6000F, 0.0F);
        this.leg_front_left_1 = new AdvancedModelBox(this, 120, 109);
        this.leg_front_left_1.setRotationPoint(5.8000F, 0.0F, -9.0F);
        this.leg_front_left_1.addBox(-3.8000F, -3.0F, -4.2000F, 7.6000F, 16.0F, 8.4000F, 0.0F);
        this.leg_front_left_2 = new AdvancedModelBox(this, 62, 108);
        this.leg_front_left_2.setRotationPoint(0.0F, 12.5000F, 0.0F);
        this.leg_front_left_2.addBox(-3.4000F, -1.0F, -3.8000F, 6.8000F, 17.5000F, 7.6000F, 0.0F);
        this.foot_front_left = new AdvancedModelBox(this, 139, 178);
        this.foot_front_left.setRotationPoint(0.0F, 16.0F, 0.0F);
        this.foot_front_left.addBox(-3.6000F, -0.5000F, -4.2000F, 7.2000F, 6.0F, 8.6000F, 0.0F);
        this.leg_front_right_1 = new AdvancedModelBox(this, 0, 132);
        this.leg_front_right_1.setRotationPoint(-5.8000F, 0.0F, -9.0F);
        this.leg_front_right_1.addBox(-3.8000F, -3.0F, -4.2000F, 7.6000F, 16.0F, 8.4000F, 0.0F);
        this.leg_front_right_2 = new AdvancedModelBox(this, 91, 108);
        this.leg_front_right_2.setRotationPoint(0.0F, 12.5000F, 0.0F);
        this.leg_front_right_2.addBox(-3.4000F, -1.0F, -3.8000F, 6.8000F, 17.5000F, 7.6000F, 0.0F);
        this.foot_front_right = new AdvancedModelBox(this, 171, 178);
        this.foot_front_right.setRotationPoint(0.0F, 16.0F, 0.0F);
        this.foot_front_right.addBox(-3.6000F, -0.5000F, -4.2000F, 7.2000F, 6.0F, 8.6000F, 0.0F);
        this.fur_chest = new AdvancedModelBox(this, 0, 178);
        this.fur_chest.setRotationPoint(0.0F, 8.0F, -14.0F);
        this.fur_chest.addBox(-7.2000F, -0.6000F, -4.8000F, 14.4000F, 9.6000F, 8.8000F, 0.0F);
        this.body_hips = new AdvancedModelBox(this, 0, 77);
        this.body_hips.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.body_hips.addBox(-10.4000F, -7.0F, -1.0F, 20.8000F, 16.0F, 13.0F, 0.0F);
        this.body_rump = new AdvancedModelBox(this, 152, 133);
        this.body_rump.setRotationPoint(0.0F, -9.0F, 9.0F);
        this.body_rump.addBox(-9.0F, -0.3000F, -1.0F, 18.0F, 16.3000F, 7.5000F, 0.0F);
        this.setRotateAngle(body_rump, -0.4189F, 0.0F, 0.0F);
        this.tail = new AdvancedModelBox(this, 247, 0);
        this.tail.setRotationPoint(0.0F, 2.5000F, 6.8000F);
        this.tail.addBox(-1.2000F, -0.5000F, -0.8000F, 2.4000F, 16.0F, 1.6000F, 0.0F);
        this.setRotateAngle(tail, 0.1396F, 0.0F, 0.0F);
        this.tail_tuft = new AdvancedModelBox(this, 205, 208);
        this.tail_tuft.setRotationPoint(0.0F, 15.0F, 0.0F);
        this.tail_tuft.addBox(-2.0F, -0.5000F, -0.5000F, 4.0F, 6.5000F, 1.0F, 0.0F);
        this.fur_rump = new AdvancedModelBox(this, 60, 134);
        this.fur_rump.setRotationPoint(0.0F, 7.0F, 4.0F);
        this.fur_rump.addBox(-9.9000F, -8.0F, -1.0F, 19.8000F, 16.0F, 5.8000F, 0.0F);
        this.body_croup = new AdvancedModelBox(this, 175, 108);
        this.body_croup.setRotationPoint(0.0F, -5.0F, 3.0F);
        this.body_croup.addBox(-8.2000F, -8.0F, -5.5000F, 16.4000F, 13.0F, 12.0F, 0.0F);
        this.setRotateAngle(body_croup, -0.1222F, 0.0F, 0.0F);
        this.leg_back_left_1 = new AdvancedModelBox(this, 0, 106);
        this.leg_back_left_1.setRotationPoint(6.0F, -1.0F, 5.0F);
        this.leg_back_left_1.addBox(-3.6000F, -3.0F, -4.0F, 7.2000F, 18.0F, 8.0F, 0.0F);
        this.leg_back_left_2 = new AdvancedModelBox(this, 32, 132);
        this.leg_back_left_2.setRotationPoint(0.0F, 14.5000F, 0.0F);
        this.leg_back_left_2.addBox(-3.2000F, -1.3000F, -3.6000F, 6.4000F, 16.8000F, 7.2000F, 0.0F);
        this.foot_back_left = new AdvancedModelBox(this, 203, 178);
        this.foot_back_left.setRotationPoint(0.0F, 15.0F, 0.0F);
        this.foot_back_left.addBox(-3.4000F, -0.5000F, -4.2000F, 6.8000F, 6.0F, 8.6000F, 0.0F);
        this.leg_back_right_1 = new AdvancedModelBox(this, 31, 106);
        this.leg_back_right_1.setRotationPoint(-6.0F, -1.0F, 5.0F);
        this.leg_back_right_1.addBox(-3.6000F, -3.0F, -4.0F, 7.2000F, 18.0F, 8.0F, 0.0F);
        this.leg_back_right_2 = new AdvancedModelBox(this, 203, 133);
        this.leg_back_right_2.setRotationPoint(0.0F, 14.5000F, 0.0F);
        this.leg_back_right_2.addBox(-3.2000F, -1.3000F, -3.6000F, 6.4000F, 16.8000F, 7.2000F, 0.0F);
        this.foot_back_right = new AdvancedModelBox(this, 139, 193);
        this.foot_back_right.setRotationPoint(0.0F, 15.0F, 0.0F);
        this.foot_back_right.addBox(-3.4000F, -0.5000F, -4.2000F, 6.8000F, 6.0F, 8.6000F, 0.0F);
        this.body_back = new AdvancedModelBox(this, 70, 44);
        this.body_back.setRotationPoint(0.0F, -5.0F, 0.0F);
        this.body_back.addBox(-8.5000F, -7.0F, -10.0F, 17.0F, 12.0F, 19.4000F, 0.0F);
        this.fur_skirt_left = new AdvancedModelBox(this, 75, 157);
        this.fur_skirt_left.setRotationPoint(11.8000F, 0.0F, -3.0F);
        this.fur_skirt_left.addBox(-21.0F, -2.0F, -0.4000F, 42.0F, 20.0F, 0.8000F, 0.0F);
        this.setRotateAngle(fur_skirt_left, 0.0F, -1.5708F, -0.0698F);
        this.fur_skirt_right = new AdvancedModelBox(this, 161, 157);
        this.fur_skirt_right.setRotationPoint(-11.8000F, 0.0F, -3.0F);
        this.fur_skirt_right.addBox(-21.0F, -2.0F, -0.4000F, 42.0F, 20.0F, 0.8000F, 0.0F);
        this.setRotateAngle(fur_skirt_right, 0.0F, -1.5708F, 0.0698F);

        this.body_mid.addChild(this.body_chest);
        this.body_chest.addChild(this.body_withers);
        this.body_withers.addChild(this.body_hump);
        this.body_withers.addChild(this.fur_mane);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.head_dome);
        this.head.addChild(this.head_dome_left);
        this.head.addChild(this.head_dome_right);
        this.head.addChild(this.head_crest);
        this.head.addChild(this.jaw);
        this.head.addChild(this.tusk_sheath_left);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.tusk_sheath_right);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.trunk_1);
        this.trunk_1.addChild(this.trunk_2);
        this.trunk_2.addChild(this.trunk_3);
        this.trunk_3.addChild(this.trunk_4);
        this.trunk_4.addChild(this.trunk_5);
        this.trunk_5.addChild(this.trunk_6);
        this.head.addChild(this.tusk_left_1);
        this.tusk_left_1.addChild(this.tusk_left_2);
        this.tusk_left_2.addChild(this.tusk_left_3);
        this.tusk_left_3.addChild(this.tusk_left_4);
        this.tusk_left_4.addChild(this.tusk_left_5);
        this.head.addChild(this.tusk_right_1);
        this.tusk_right_1.addChild(this.tusk_right_2);
        this.tusk_right_2.addChild(this.tusk_right_3);
        this.tusk_right_3.addChild(this.tusk_right_4);
        this.tusk_right_4.addChild(this.tusk_right_5);
        this.neck.addChild(this.fur_neck);
        this.body_chest.addChild(this.leg_front_left_1);
        this.leg_front_left_1.addChild(this.leg_front_left_2);
        this.leg_front_left_2.addChild(this.foot_front_left);
        this.body_chest.addChild(this.leg_front_right_1);
        this.leg_front_right_1.addChild(this.leg_front_right_2);
        this.leg_front_right_2.addChild(this.foot_front_right);
        this.body_chest.addChild(this.fur_chest);
        this.body_mid.addChild(this.body_hips);
        this.body_hips.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.tail.addChild(this.tail_tuft);
        this.body_rump.addChild(this.fur_rump);
        this.body_hips.addChild(this.body_croup);
        this.body_hips.addChild(this.leg_back_left_1);
        this.leg_back_left_1.addChild(this.leg_back_left_2);
        this.leg_back_left_2.addChild(this.foot_back_left);
        this.body_hips.addChild(this.leg_back_right_1);
        this.leg_back_right_1.addChild(this.leg_back_right_2);
        this.leg_back_right_2.addChild(this.foot_back_right);
        this.body_mid.addChild(this.body_back);
        this.body_mid.addChild(this.fur_skirt_left);
        this.body_mid.addChild(this.fur_skirt_right);

        // hasLargeTusks scales a whole 5-box chain about its root, so the scale must reach the children
        this.tusk_left_1.setShouldScaleChildren(true);
        this.tusk_right_1.setShouldScaleChildren(true);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_mid);
    }

    /* Built once: Citadel asks for this every frame for every animal. */
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
                body_mid,
                body_chest,
                body_withers,
                body_hump,
                fur_mane,
                neck,
                head,
                head_dome,
                head_dome_left,
                head_dome_right,
                head_crest,
                jaw,
                tusk_sheath_left,
                eye_left,
                ear_left,
                tusk_sheath_right,
                eye_right,
                ear_right,
                trunk_1,
                trunk_2,
                trunk_3,
                trunk_4,
                trunk_5,
                trunk_6,
                tusk_left_1,
                tusk_left_2,
                tusk_left_3,
                tusk_left_4,
                tusk_left_5,
                tusk_right_1,
                tusk_right_2,
                tusk_right_3,
                tusk_right_4,
                tusk_right_5,
                fur_neck,
                leg_front_left_1,
                leg_front_left_2,
                foot_front_left,
                leg_front_right_1,
                leg_front_right_2,
                foot_front_right,
                fur_chest,
                body_hips,
                body_rump,
                tail,
                tail_tuft,
                fur_rump,
                body_croup,
                leg_back_left_1,
                leg_back_left_2,
                foot_back_left,
                leg_back_right_1,
                leg_back_right_2,
                foot_back_right,
                body_back,
                fur_skirt_left,
                fur_skirt_right
            );
        }
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityMammoth mammoth = (EntityMammoth) entityIn;
        animator.update(mammoth);
        // <gen:idle-poses> 2.8.0: SOLVED by scripts/pose_reach.py against this model's own boxes (muzzle to the grass / the
        // water line beyond the bank, every head part and horn family clear of the ground, nothing through the torso).
        // Regenerate, never hand-edit: python scripts/gen_keyframes.py mammoth
        // DRINK (100t, DrinkGoal), the way elephants drink: the trunk tip down into the water beyond the bank (SOLVED,
        // one smooth curve), a hold while it draws water, then the trunk coiled up to the mouth (the approved EAT
        // coil, pose_targets trunk_curl_java), a hold while it pours -- twice
        animator.setAnimation(EntityMammoth.DRINK);
        animator.startKeyframe(15);
        this.rotate(animator, neck, 5.3F, 0.0F, 0.0F);
        this.rotate(animator, head, 3.9F, 0.0F, 0.0F);
        this.rotate(animator, trunk_1, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, trunk_2, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, trunk_3, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_4, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_5, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_6, -0.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(15);
        animator.startKeyframe(12);
        this.rotate(animator, neck, -4.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_1, -11.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_2, 39.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_3, 63.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_4, 68.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_5, 59.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_6, 50.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(10);
        animator.startKeyframe(12);
        this.rotate(animator, neck, 5.3F, 0.0F, 0.0F);
        this.rotate(animator, head, 3.9F, 0.0F, 0.0F);
        this.rotate(animator, trunk_1, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, trunk_2, -0.5F, 0.0F, 0.0F);
        this.rotate(animator, trunk_3, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_4, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_5, -0.4F, 0.0F, 0.0F);
        this.rotate(animator, trunk_6, -0.4F, 0.0F, 0.0F);
        animator.move(neck, 0.0F, 1.0F, 0.0F);
        animator.endKeyframe();
        animator.setStaticKeyframe(15);
        animator.startKeyframe(12);
        this.rotate(animator, neck, -4.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_1, -11.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_2, 39.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_3, 63.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_4, 68.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_5, 59.0F, 0.0F, 0.0F);
        this.rotate(animator, trunk_6, 50.0F, 0.0F, 0.0F);
        animator.endKeyframe();
        animator.resetKeyframe(9);
        // </gen:idle-poses>

        // Threat display: trumpet (head and trunk thrown up, ears flared, a forefoot raised), then stomp. Twice.
        animator.setAnimation(EntityMammoth.ATTACK_THREATEN);
        for (int beat = 0; beat < 2; beat++) {
            animator.startKeyframe(beat == 0 ? 12 : 10);
            this.rotate(animator, neck, -18.0F, 0, 0);
            this.rotate(animator, head, -12.0F, 0, 0);
            this.rotate(animator, ear_left, 0, 35.0F, 0);
            this.rotate(animator, ear_right, 0, -35.0F, 0);
            this.rotate(animator, leg_front_right_1, -30.0F, 0, 0);
            this.rotate(animator, leg_front_right_2, 40.0F, 0, 0);
            this.rotate(animator, foot_front_right, -10.0F, 0, 0);
            this.rotate(animator, trunk_1, -20.0F, 0, 0);
            this.rotate(animator, trunk_2, -30.0F, 0, 0);
            this.rotate(animator, trunk_3, -35.0F, 0, 0);
            this.rotate(animator, trunk_4, -35.0F, 0, 0);
            this.rotate(animator, trunk_5, -30.0F, 0, 0);
            this.rotate(animator, trunk_6, -25.0F, 0, 0);
            animator.endKeyframe();
            if (beat == 0) {
                animator.setStaticKeyframe(6);
            }
            animator.startKeyframe(7);
            this.rotate(animator, neck, 8.0F, 0, 6.0F);
            this.rotate(animator, head, 6.0F, 0, 4.0F);
            this.rotate(animator, ear_left, 0, 35.0F, 0);
            this.rotate(animator, ear_right, 0, -35.0F, 0);
            this.rotate(animator, trunk_1, -8.0F, 0, 0);
            this.rotate(animator, trunk_2, -6.0F, 0, 0);
            this.rotate(animator, trunk_3, 6.0F, 0, 0);
            this.rotate(animator, trunk_4, 10.0F, 0, 0);
            this.rotate(animator, trunk_5, 12.0F, 0, 0);
            this.rotate(animator, trunk_6, 14.0F, 0, 0);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: head down with the trunk curled under out of harm's way, then a lunge that tosses the tusks up and
        // across. Shallower than the July rig's: the long straight and upcurved tusks reach far lower than its spirals.
        animator.setAnimation(EntityMammoth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 12.0F, 0, 10.0F);
        this.rotate(animator, head, 8.0F, 0, 8.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_6, 14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -21.0F, 0, -26.0F);
        this.rotate(animator, head, -16.0F, 0, -18.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_6, 14.0F, 0, 0);
        animator.move(body_mid, 0, 0, -2.0F);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // Graze the way elephants do: the head dips only a little, the trunk reaches forward along the ground, the tip
        // curls round a mouthful, then the whole trunk coils back under the skull to the mouth.
        animator.setAnimation(EntityMammoth.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, trunk_1, -6.0F, 0, 0);
        this.rotate(animator, trunk_2, -10.0F, 0, 0);
        this.rotate(animator, trunk_3, -6.0F, 0, 0);
        this.rotate(animator, trunk_4, 10.0F, 0, 0);
        this.rotate(animator, trunk_5, 22.0F, 0, 0);
        this.rotate(animator, trunk_6, 30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, neck, 8.0F, 0, 0);
        this.rotate(animator, head, 4.0F, 0, 0);
        this.rotate(animator, trunk_1, -6.0F, 0, 0);
        this.rotate(animator, trunk_2, -10.0F, 0, 0);
        this.rotate(animator, trunk_3, -6.0F, 0, 0);
        this.rotate(animator, trunk_4, 22.0F, 0, 0);
        this.rotate(animator, trunk_5, 38.0F, 0, 0);
        this.rotate(animator, trunk_6, 50.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, neck, -4.0F, 0, 0);
        this.rotate(animator, trunk_1, -11.0F, 0, 0);
        this.rotate(animator, trunk_2, 39.0F, 0, 0);
        this.rotate(animator, trunk_3, 63.0F, 0, 0);
        this.rotate(animator, trunk_4, 68.0F, 0, 0);
        this.rotate(animator, trunk_5, 59.0F, 0, 0);
        this.rotate(animator, trunk_6, 50.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.resetKeyframe(8);
    }

    public void setupAnim(EntityMammoth mammoth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();

        // ---- FAMILIES (every frame: one model instance serves every proboscidean) ----
        // (locals are *F: a local named `head` would shadow the head part)
        int headF = Mth.clamp(mammoth.getHeadShape(), 0, 4);
        int backF = Mth.clamp(mammoth.getBackShape(), 0, 3);
        int tuskF = Mth.clamp(mammoth.getTuskShape(), 0, TUSK_ROT.length - 1);
        int earF = Mth.clamp(mammoth.getEarSize(), 0, 2);
        boolean coat = mammoth.hasWoollyCoat();
        this.head_dome.showModel = headF == 0;
        this.head_dome_left.showModel = headF == 1;
        this.head_dome_right.showModel = headF == 1;
        this.head_crest.showModel = headF == 3;
        this.body_hump.showModel = backF == 0;
        this.fur_neck.showModel = coat;
        this.fur_mane.showModel = coat;
        this.fur_chest.showModel = coat;
        this.fur_rump.showModel = coat;
        this.fur_skirt_left.showModel = coat;
        this.fur_skirt_right.showModel = coat;
        this.body_withers.setScale(1.0F, BACK_SCALE[backF][0], 1.0F);
        this.body_back.setScale(1.0F, BACK_SCALE[backF][1], 1.0F);
        this.body_croup.setScale(1.0F, BACK_SCALE[backF][2], 1.0F);
        this.head.rotateAngleX += HEAD_PITCH[headF];
        this.neck.rotateAngleX += NECK_PITCH[headF];
        float[] e = EAR[earF];
        this.ear_left.setRotationPoint(7.6000F, e[3], e[4]);
        this.ear_right.setRotationPoint(-7.6000F, e[3], e[4]);
        this.setRotateAngle(ear_left, 0.0F, e[0], 0.0F);
        this.setRotateAngle(ear_right, 0.0F, -e[0], 0.0F);
        this.ear_left.setScale(1.0F, e[1], e[2]);
        this.ear_right.setScale(1.0F, e[1], e[2]);
        AdvancedModelBox[] tl = {tusk_left_1, tusk_left_2, tusk_left_3, tusk_left_4, tusk_left_5};
        AdvancedModelBox[] tr = {tusk_right_1, tusk_right_2, tusk_right_3, tusk_right_4, tusk_right_5};
        for (int k = 0; k < 5; k++) {
            float[] l = TUSK_ROT[tuskF][0][k], r = TUSK_ROT[tuskF][1][k];
            this.setRotateAngle(tl[k], l[0], l[1], l[2]);
            this.setRotateAngle(tr[k], r[0], r[1], r[2]);
        }
        boolean longTusk = tuskF != 5;   // SMALL: only the first three segments
        this.tusk_left_4.showModel = longTusk;
        this.tusk_right_4.showModel = longTusk;
        float tuskScale = mammoth.hasLargeTusks() ? 1.35F : 1.0F;
        this.tusk_left_1.setScale(tuskScale, tuskScale, tuskScale);
        this.tusk_right_1.setScale(tuskScale, tuskScale, tuskScale);

        animate(mammoth);
        float globalSpeed = 1.1F;
        float globalDegree = 1.0F;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- idle: the trunk hangs and sways, amplitude growing toward the tip (cut to 30% lying down, where it rests
        // on the ground); the ears fan (a swing about the hinge -- how an elephant actually flaps them); tail swings ----
        float rest = Math.max(mammoth.sitProgress, mammoth.sleepProgress) / 40F;
        float sway = 1.0F - 0.7F * rest;
        this.walk(trunk_1, 0.10F, 0.035F * sway, false, 0.0F, 0F, ageInTicks, 1);
        this.walk(trunk_2, 0.10F, 0.05F * sway, false, 0.6F, 0F, ageInTicks, 1);
        this.walk(trunk_3, 0.10F, 0.07F * sway, false, 1.2F, 0F, ageInTicks, 1);
        this.walk(trunk_4, 0.10F, 0.09F * sway, false, 1.8F, 0F, ageInTicks, 1);
        this.walk(trunk_5, 0.10F, 0.11F * sway, false, 2.4F, 0F, ageInTicks, 1);
        this.walk(trunk_6, 0.10F, 0.14F * sway, false, 3.0F, 0F, ageInTicks, 1);
        this.swing(trunk_2, 0.07F, 0.04F * sway, false, 1.0F, 0F, ageInTicks, 1);
        this.swing(trunk_4, 0.07F, 0.07F * sway, false, 2.0F, 0F, ageInTicks, 1);
        this.swing(trunk_6, 0.07F, 0.10F * sway, false, 3.0F, 0F, ageInTicks, 1);
        float earFan = earF == 2 ? 0.10F : earF == 1 ? 0.08F : 0.05F;
        this.swing(ear_left, 0.09F, earFan, false, 0F, 0F, ageInTicks, 1);
        this.swing(ear_right, 0.09F, earFan, true, 0F, 0F, ageInTicks, 1);
        this.swing(tail, 0.10F, 0.16F, false, 0F, 0F, ageInTicks, 1);
        this.walk(tail_tuft, 0.10F, 0.10F, false, 1F, 0F, ageInTicks, 1);

        // breathing
        float bx = (float) (1.0F + Math.sin(ageInTicks / 20) * 0.04F), by = (float) (1.0F + Math.sin(ageInTicks / 16) * 0.04F);
        this.body_mid.setScale(bx, by, 1.0F);
        this.body_chest.setScale(bx, by, 1.0F);
        this.body_hips.setScale(bx, by, 1.0F);

        // Blinking / asleep: squash the eye planes to 20% height about their centre -- a shut dark slit
        float eyeOpen = mammoth.shouldRenderEyes() ? 1.0F : 0.2F;
        this.eye_left.setScale(1.0F, eyeOpen, 1.0F);
        this.eye_right.setScale(1.0F, eyeOpen, 1.0F);

        if (!mammoth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        if (mammoth.isInWater()) {
            this.head.rotateAngleX -= 0.18F;   // trunk up as a snorkel
            if (!mammoth.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5F;
                float pitch = Mth.clamp(mammoth.getXRot() - 10, -25F, 25.0F);
                this.body_mid.rotateAngleX += (float) (pitch * Math.PI / 180F);
            }
        }

        // ---- walk: lateral-sequence (LH, LF, RH, RF a quarter-cycle apart), the elephant gait ----
        if (mammoth.canMove()) {
            bob(body_mid, 0.8F * globalSpeed, 0.3F * globalDegree, true, f, limbSwingAmount);
            flap(body_mid, 0.8F * globalSpeed, 0.06F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck, 0.8F * globalSpeed, 0.12F * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8F * globalSpeed, 0.08F * globalDegree, true, 0, 0, f, limbSwingAmount);
            // (generated by the walk fit in proboscidean_spec.py --walk)
            walk(leg_back_left_1, 0.8F * globalSpeed, 0.390F, false, 0.000F, 0.000F, f, limbSwingAmount);
            walk(leg_back_left_2, 0.8F * globalSpeed, 0.656F, false, -1.562F, 0.534F, f, limbSwingAmount);
            walk(foot_back_left, 0.8F * globalSpeed, -0.390F, false, 0.000F, 0.000F, f, limbSwingAmount);
            walk(foot_back_left, 0.8F * globalSpeed, -0.656F, false, -1.562F, -0.534F, f, limbSwingAmount);
            walk(leg_back_right_1, 0.8F * globalSpeed, 0.390F, false, 3.142F, 0.000F, f, limbSwingAmount);
            walk(leg_back_right_2, 0.8F * globalSpeed, 0.656F, false, 1.579F, 0.534F, f, limbSwingAmount);
            walk(foot_back_right, 0.8F * globalSpeed, -0.390F, false, 3.142F, 0.000F, f, limbSwingAmount);
            walk(foot_back_right, 0.8F * globalSpeed, -0.656F, false, 1.579F, -0.534F, f, limbSwingAmount);
            walk(leg_front_left_1, 0.8F * globalSpeed, 0.512F, false, -1.571F, 0.000F, f, limbSwingAmount);
            walk(leg_front_left_2, 0.8F * globalSpeed, 0.775F, false, -3.457F, 0.758F, f, limbSwingAmount);
            walk(foot_front_left, 0.8F * globalSpeed, -0.512F, false, -1.571F, 0.000F, f, limbSwingAmount);
            walk(foot_front_left, 0.8F * globalSpeed, -0.775F, false, -3.457F, -0.758F, f, limbSwingAmount);
            walk(leg_front_right_1, 0.8F * globalSpeed, 0.512F, false, 1.571F, 0.000F, f, limbSwingAmount);
            walk(leg_front_right_2, 0.8F * globalSpeed, 0.775F, false, -0.315F, 0.758F, f, limbSwingAmount);
            walk(foot_front_right, 0.8F * globalSpeed, -0.512F, false, 1.571F, 0.000F, f, limbSwingAmount);
            walk(foot_front_right, 0.8F * globalSpeed, -0.775F, false, -0.315F, -0.758F, f, limbSwingAmount);
            walk(trunk_1, 0.8F * globalSpeed, 0.18F * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(trunk_2, 0.8F * globalSpeed, 0.22F * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
            walk(trunk_3, 0.8F * globalSpeed, 0.26F * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(trunk_4, 0.8F * globalSpeed, 0.30F * globalDegree, false, -1.8F, 0, f, limbSwingAmount);
            walk(trunk_5, 0.8F * globalSpeed, 0.34F * globalDegree, false, -2.4F, 0, f, limbSwingAmount);
            walk(trunk_6, 0.8F * globalSpeed, 0.38F * globalDegree, false, -3.0F, 0, f, limbSwingAmount);
            swing(tail, 0.8F * globalSpeed, 0.35F * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }

        // ---- rest / sleep: SOLVED sternal recumbency (proboscidean_spec.py --pose). The coat squashes instead of
        // passing through the ground; written every frame (1.0 standing) because the model is shared.
        float furRest = mammoth.sitProgress > 0 ? mammoth.sitProgress / 40F : mammoth.sleepProgress / 40F;
        this.fur_skirt_left.setScale(1.0F, 1.0F - 0.2700F * furRest, 1.0F);
        this.fur_skirt_right.setScale(1.0F, 1.0F - 0.2700F * furRest, 1.0F);
        this.fur_chest.setScale(1.0F, 1.0F - 0.4300F * furRest, 1.0F);
        this.fur_rump.setScale(1.0F, 1.0F - 0.0F * furRest, 1.0F);
        this.fur_neck.setScale(1.0F, 1.0F - 0.0F * furRest, 1.0F);
        if (mammoth.sitProgress > 0) {
            applyRestingPose(mammoth.sitProgress, SIT_LIFT[headF], false);
        }
        else if (mammoth.sleepProgress > 0) {
            applyRestingPose(mammoth.sleepProgress, SLEEP_LIFT[headF], true);
        }
    }

    private void applyRestingPose(float progress, float lift, boolean asleep) {
        // the belly sinks to 0.8u off the ground; forelegs stretched forward, hind legs straight back
        this.progressPosition(body_mid, progress, 0.0F, 10.7000F, 0.0F, 40);
        this.progressRotation(leg_front_left_1, progress, -1.4875F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_front_left_2, progress, 0.4100F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_front_left, progress, -0.4508F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_back_left_1, progress, 1.2455F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_back_left_2, progress, -0.0132F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_back_left, progress, 0.2599F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_front_right_1, progress, -1.4875F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_front_right_2, progress, 0.4100F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_front_right, progress, -0.4508F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_back_right_1, progress, 1.2455F, 0.0F, 0.0F, 40);
        this.progressRotation(leg_back_right_2, progress, -0.0132F, 0.0F, 0.0F, 40);
        this.progressRotation(foot_back_right, progress, 0.2599F, 0.0F, 0.0F, 40);
        this.progressRotation(tail, progress, 1.0646F, 0.0F, 0.0F, 40);   // lies back along the ground
        // head lift per head shape keeps the trunk and tusks clear (rest) / resting on the ground (sleep); added on
        // top of the family pitch, which progressRotation leaves in place (it moves toward target - default)
        this.progressRotation(neck, progress, 0.0F - lift * 0.6F, 0.0F, 0.0F, 40);
        this.progressRotation(head, progress, 0.0F - lift * 0.4F, 0.0F, 0.0F, 40);
        if (!asleep) {
            this.progressRotation(trunk_1, progress, -0.5713F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_2, progress, 0.2655F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_3, progress, -0.3649F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_4, progress, -0.1744F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_5, progress, -0.0991F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_6, progress, -0.0138F, 0.0F, 0.0F, 40);
        }
        else {
            this.progressRotation(trunk_1, progress, -0.5713F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_2, progress, -0.2581F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_3, progress, -0.1903F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_4, progress, 0.0525F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_5, progress, 0.1627F, 0.0F, 0.0F, 40);
            this.progressRotation(trunk_6, progress, 0.2480F, 0.0F, 0.0F, 40);
        }
    }
}
