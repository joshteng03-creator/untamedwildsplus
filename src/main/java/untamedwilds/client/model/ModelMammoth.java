package untamedwilds.client.model;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.Mth;
import untamedwilds.entity.mammal.EntityMammoth;

public class ModelMammoth extends AdvancedEntityModel<EntityMammoth> {

    private final AdvancedModelBox body_hips;
    private final AdvancedModelBox body_rump;
    private final AdvancedModelBox tail;
    private final AdvancedModelBox tail_tuft;
    private final AdvancedModelBox fur_rump;
    private final AdvancedModelBox body_chest;
    private final AdvancedModelBox body_hump;
    private final AdvancedModelBox neck;
    private final AdvancedModelBox head;
    private final AdvancedModelBox head_dome;
    private final AdvancedModelBox ear_left;
    private final AdvancedModelBox ear_right;
    private final AdvancedModelBox eye_left;
    private final AdvancedModelBox eye_right;
    private final AdvancedModelBox trunk_1;
    private final AdvancedModelBox trunk_2;
    private final AdvancedModelBox trunk_3;
    private final AdvancedModelBox trunk_4;
    private final AdvancedModelBox trunk_5;
    private final AdvancedModelBox trunk_tip;
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
    private final AdvancedModelBox fur_mane;
    private final AdvancedModelBox fur_chest;
    private final AdvancedModelBox leg_back_left_1;
    private final AdvancedModelBox leg_back_left_2;
    private final AdvancedModelBox foot_back_left;
    private final AdvancedModelBox leg_back_right_1;
    private final AdvancedModelBox leg_back_right_2;
    private final AdvancedModelBox foot_back_right;
    private final AdvancedModelBox fur_skirt_left;
    private final AdvancedModelBox fur_skirt_right;

    private final ModelAnimator animator;

    public ModelMammoth() {
        this.texWidth = 256;
        this.texHeight = 256;

        // Full Blockbench rebuild (47 boxes). The silhouette is carried by ANGLED boxes in the
        // ModelRhino manner: body_chest is pitched up 8 deg, body_hump another 9 deg on top of it and
        // body_rump 13 deg down at the back, so the mammoth wedge (high shoulder hump falling away to a
        // low croup) is three stacked planes rather than one stepped box. The tusks and the trunk are
        // real rotation CHAINS -- each segment is a child of the previous one, so the ~185 deg tusk
        // spiral and the trunk curl accumulate smoothly and animate as a whip. Legs are three segments
        // with opposing rotations (front -13/+5, hind +12/-18/+6) exactly like the rhino, which bends
        // the limb while keeping the foot plumb and on the ground.
        // Every coordinate below is transcribed VERBATIM from the modded_entity export -- never
        // re-derived by hand (the Y-up/Y-down flip does not generalise; see CLAUDE.md lesson 1).
        this.body_hips = new AdvancedModelBox(this, 0, 86);
        this.body_hips.setRotationPoint(0.0F, -6.0F, 4.0F);
        this.body_hips.addBox(-11.5F, -5.0F, -8.0F, 23.0F, 13.0F, 17.0F, 0.0F);

        this.body_rump = new AdvancedModelBox(this, 168, 116);
        this.body_rump.setRotationPoint(0.0F, -5.0F, 6.0F);
        this.body_rump.addBox(-11.0F, -1.0F, 0.0F, 22.0F, 11.0F, 10.0F, 0.0F);
        this.setRotateAngle(body_rump, -0.2269F, 0.0F, 0.0F);

        this.tail = new AdvancedModelBox(this, 212, 162);
        this.tail.setRotationPoint(0.0F, 3.0F, 8.0F);
        this.tail.addBox(-2.0F, 0.0F, -0.5F, 4.0F, 11.0F, 3.5F, 0.0F);
        this.setRotateAngle(tail, 0.2443F, 0.0F, 0.0F);

        this.tail_tuft = new AdvancedModelBox(this, 82, 182);
        this.tail_tuft.setRotationPoint(0.0F, 11.0F, 1.0F);
        this.tail_tuft.addBox(-2.75F, -0.5F, -2.25F, 5.5F, 7.0F, 5.0F, 0.0F);

        this.fur_rump = new AdvancedModelBox(this, 166, 162);
        this.fur_rump.setRotationPoint(0.0F, 6.0F, 8.0F);
        this.fur_rump.addBox(-10.0F, -2.0F, -0.5F, 20.0F, 14.0F, 3.0F, 0.0F);
        this.setRotateAngle(fur_rump, -0.2269F, 0.0F, 0.0F);

        this.body_chest = new AdvancedModelBox(this, 102, 47);
        this.body_chest.setRotationPoint(0.0F, 0.0F, -8.0F);
        this.body_chest.addBox(-12.0F, -7.0F, -20.0F, 24.0F, 15.5F, 22.0F, 0.0F);
        this.setRotateAngle(body_chest, -0.1396F, 0.0F, 0.0F);

        this.body_hump = new AdvancedModelBox(this, 0, 47);
        this.body_hump.setRotationPoint(0.0F, -7.0F, -18.0F);
        this.body_hump.addBox(-10.5F, -7.0F, 0.0F, 21.0F, 8.5F, 30.0F, 0.0F);
        this.setRotateAngle(body_hump, -0.1571F, 0.0F, 0.0F);

        this.neck = new AdvancedModelBox(this, 40, 116);
        this.neck.setRotationPoint(0.0F, -3.0F, -18.0F);
        this.neck.addBox(-9.0F, -4.0F, -7.0F, 18.0F, 14.0F, 10.0F, 0.0F);
        this.setRotateAngle(neck, 0.0698F, 0.0F, 0.0F);

        this.head = new AdvancedModelBox(this, 80, 86);
        this.head.setRotationPoint(0.0F, 0.0F, -6.0F);
        this.head.addBox(-9.0F, -7.0F, -11.0F, 18.0F, 16.0F, 12.0F, 0.0F);
        this.setRotateAngle(head, 0.0873F, 0.0F, 0.0F);

        this.head_dome = new AdvancedModelBox(this, 116, 162);
        this.head_dome.setRotationPoint(0.0F, -6.0F, -6.0F);
        this.head_dome.addBox(-6.5F, -5.5F, -3.5F, 13.0F, 5.5F, 11.5F, 0.0F);
        this.setRotateAngle(head_dome, -0.0873F, 0.0F, 0.0F);

        this.ear_left = new AdvancedModelBox(this, 228, 162);
        this.ear_left.setRotationPoint(9.0F, 0.0F, -1.0F);
        this.ear_left.addBox(-0.2F, -1.5F, -2.0F, 1.1F, 7.0F, 6.5F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, -0.1047F, -0.0698F);

        this.ear_right = new AdvancedModelBox(this, 0, 182);
        this.ear_right.setRotationPoint(-9.0F, 0.0F, -1.0F);
        this.ear_right.addBox(-0.9F, -1.5F, -2.0F, 1.1F, 7.0F, 6.5F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.1047F, 0.0698F);

        // Eye planes sit 0.35 units clear of the skull face (which is at |x| = 9.0). The previous model
        // left only 0.0075 units of clearance, which is far below depth-buffer precision and made the
        // eyes shimmer/z-fight against the cheek every frame.
        // The planes are a LITERAL COPY of ModelGroundSloth's: 3x3 rather than 2.5x2.5, because a 2.5px
        // plane samples a fractional-pixel UV rect and left no room for a pupil to read against the hide.
        // At 3x3 the footprint is exactly 2*(0+3) x (3+3) = 6x6 px, which is what the sloth's eye art is
        // painted on, so the same 6x6 blocks drop straight into the mammoth atlas at these texOffs. A
        // full-model footprint sweep confirms 82-88 and 88-94 x 196-202 collide with nothing else.
        this.eye_left = new AdvancedModelBox(this, 82, 196);
        this.eye_left.setRotationPoint(9.35F, -1.5F, -6.0F);
        this.eye_left.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.eye_right = new AdvancedModelBox(this, 88, 196);
        this.eye_right.setRotationPoint(-9.35F, -1.5F, -6.0F);
        this.eye_right.addBox(0.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 0.0F);

        this.trunk_1 = new AdvancedModelBox(this, 18, 182);
        this.trunk_1.setRotationPoint(0.0F, 5.5F, -7.5F);
        this.trunk_1.addBox(-5.0F, 0.0F, -3.5F, 10.0F, 5.5F, 7.0F, 0.0F);
        this.setRotateAngle(trunk_1, -0.0873F, 0.0F, 0.0F);

        this.trunk_2 = new AdvancedModelBox(this, 52, 182);
        this.trunk_2.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.trunk_2.addBox(-4.5F, -1.0F, -3.0F, 9.0F, 6.0F, 6.0F, 0.0F);
        this.setRotateAngle(trunk_2, -0.1047F, 0.0F, 0.0F);

        this.trunk_3 = new AdvancedModelBox(this, 104, 182);
        this.trunk_3.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.trunk_3.addBox(-4.0F, -1.0F, -2.5F, 8.0F, 5.5F, 5.0F, 0.0F);
        this.setRotateAngle(trunk_3, -0.1222F, 0.0F, 0.0F);

        this.trunk_4 = new AdvancedModelBox(this, 226, 182);
        this.trunk_4.setRotationPoint(0.0F, 4.5F, 0.0F);
        this.trunk_4.addBox(-3.5F, -1.0F, -2.0F, 7.0F, 4.5F, 4.0F, 0.0F);
        this.setRotateAngle(trunk_4, -0.1571F, 0.0F, 0.0F);

        this.trunk_5 = new AdvancedModelBox(this, 48, 196);
        this.trunk_5.setRotationPoint(0.0F, 3.5F, 0.0F);
        this.trunk_5.addBox(-3.0F, -1.0F, -1.5F, 6.0F, 4.0F, 3.0F, 0.0F);
        this.setRotateAngle(trunk_5, -0.2094F, 0.0F, 0.0F);

        this.trunk_tip = new AdvancedModelBox(this, 66, 196);
        this.trunk_tip.setRotationPoint(0.0F, 3.0F, 0.0F);
        this.trunk_tip.addBox(-2.5F, -1.0F, -1.5F, 5.0F, 3.5F, 3.0F, 0.0F);
        this.setRotateAngle(trunk_tip, -0.2443F, 0.0F, 0.0F);

        this.tusk_left_1 = new AdvancedModelBox(this, 194, 182);
        this.tusk_left_1.setRotationPoint(7.0F, 6.0F, -7.0F);
        this.tusk_left_1.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.5F, 4.0F, 0.0F);
        this.setRotateAngle(tusk_left_1, -0.4363F, 0.0F, -0.1745F);

        this.tusk_left_2 = new AdvancedModelBox(this, 130, 182);
        this.tusk_left_2.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_left_2.addBox(-1.85F, -1.0F, -1.85F, 3.7F, 6.5F, 3.7F, 0.0F);
        this.setRotateAngle(tusk_left_2, -0.6109F, 0.0F, -0.1047F);

        this.tusk_left_3 = new AdvancedModelBox(this, 146, 182);
        this.tusk_left_3.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_left_3.addBox(-1.7F, -1.0F, -1.7F, 3.4F, 6.5F, 3.4F, 0.0F);
        this.setRotateAngle(tusk_left_3, -0.7854F, 0.0F, 0.0698F);

        this.tusk_left_4 = new AdvancedModelBox(this, 0, 196);
        this.tusk_left_4.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_left_4.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F, 0.0F);
        this.setRotateAngle(tusk_left_4, -0.7854F, 0.0F, 0.1745F);

        this.tusk_left_5 = new AdvancedModelBox(this, 12, 196);
        this.tusk_left_5.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.tusk_left_5.addBox(-1.25F, -1.0F, -1.25F, 2.5F, 5.5F, 2.5F, 0.0F);
        this.setRotateAngle(tusk_left_5, -0.6109F, 0.0F, 0.2094F);

        this.tusk_right_1 = new AdvancedModelBox(this, 210, 182);
        this.tusk_right_1.setRotationPoint(-7.0F, 6.0F, -7.0F);
        this.tusk_right_1.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.5F, 4.0F, 0.0F);
        this.setRotateAngle(tusk_right_1, -0.4363F, 0.0F, 0.1745F);

        this.tusk_right_2 = new AdvancedModelBox(this, 162, 182);
        this.tusk_right_2.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_right_2.addBox(-1.85F, -1.0F, -1.85F, 3.7F, 6.5F, 3.7F, 0.0F);
        this.setRotateAngle(tusk_right_2, -0.6109F, 0.0F, 0.1047F);

        this.tusk_right_3 = new AdvancedModelBox(this, 178, 182);
        this.tusk_right_3.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_right_3.addBox(-1.7F, -1.0F, -1.7F, 3.4F, 6.5F, 3.4F, 0.0F);
        this.setRotateAngle(tusk_right_3, -0.7854F, 0.0F, -0.0698F);

        this.tusk_right_4 = new AdvancedModelBox(this, 24, 196);
        this.tusk_right_4.setRotationPoint(0.0F, 5.5F, 0.0F);
        this.tusk_right_4.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F, 0.0F);
        this.setRotateAngle(tusk_right_4, -0.7854F, 0.0F, -0.1745F);

        this.tusk_right_5 = new AdvancedModelBox(this, 36, 196);
        this.tusk_right_5.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.tusk_right_5.addBox(-1.25F, -1.0F, -1.25F, 2.5F, 5.5F, 2.5F, 0.0F);
        this.setRotateAngle(tusk_right_5, -0.6109F, 0.0F, -0.2094F);

        this.fur_neck = new AdvancedModelBox(this, 140, 86);
        this.fur_neck.setRotationPoint(0.0F, -1.0F, -2.0F);
        this.fur_neck.addBox(-10.0F, -4.0F, -6.0F, 20.0F, 14.0F, 11.0F, 0.0F);
        this.setRotateAngle(fur_neck, 0.0698F, 0.0F, 0.0F);

        this.leg_front_left_1 = new AdvancedModelBox(this, 202, 86);
        this.leg_front_left_1.setRotationPoint(8.0F, 1.0F, -12.0F);
        this.leg_front_left_1.addBox(-4.0F, -0.5F, -6.0F, 7.6F, 12.5F, 12.0F, 0.0F);
        this.setRotateAngle(leg_front_left_1, 0.2269F, 0.0F, 0.0F);

        this.leg_front_left_2 = new AdvancedModelBox(this, 0, 141);
        this.leg_front_left_2.setRotationPoint(0.0F, 12.0F, 0.0F);
        this.leg_front_left_2.addBox(-3.5F, -0.5F, -5.0F, 7.0F, 11.0F, 10.0F, 0.0F);
        this.setRotateAngle(leg_front_left_2, -0.0873F, 0.0F, 0.0F);

        this.foot_front_left = new AdvancedModelBox(this, 192, 141);
        this.foot_front_left.setRotationPoint(0.0F, 10.5F, 0.0F);
        this.foot_front_left.addBox(-4.25F, -0.5F, -5.5F, 8.5F, 8.75F, 11.0F, 0.0F);

        this.leg_front_right_1 = new AdvancedModelBox(this, 0, 116);
        this.leg_front_right_1.setRotationPoint(-8.0F, 1.0F, -12.0F);
        this.leg_front_right_1.addBox(-3.6F, -0.5F, -6.0F, 7.6F, 12.5F, 12.0F, 0.0F);
        this.setRotateAngle(leg_front_right_1, 0.2269F, 0.0F, 0.0F);

        this.leg_front_right_2 = new AdvancedModelBox(this, 34, 141);
        this.leg_front_right_2.setRotationPoint(0.0F, 12.0F, 0.0F);
        this.leg_front_right_2.addBox(-3.5F, -0.5F, -5.0F, 7.0F, 11.0F, 10.0F, 0.0F);
        this.setRotateAngle(leg_front_right_2, -0.0873F, 0.0F, 0.0F);

        this.foot_front_right = new AdvancedModelBox(this, 0, 162);
        this.foot_front_right.setRotationPoint(0.0F, 10.5F, 0.0F);
        this.foot_front_right.addBox(-4.25F, -0.5F, -5.5F, 8.5F, 8.75F, 11.0F, 0.0F);

        this.fur_mane = new AdvancedModelBox(this, 148, 0);
        this.fur_mane.setRotationPoint(0.0F, -8.0F, -18.0F);
        this.fur_mane.addBox(-11.5F, -7.5F, -1.0F, 23.0F, 9.0F, 30.0F, 0.0F);
        this.setRotateAngle(fur_mane, -0.1571F, 0.0F, 0.0F);

        this.fur_chest = new AdvancedModelBox(this, 132, 141);
        this.fur_chest.setRotationPoint(0.0F, 3.0F, -19.0F);
        this.fur_chest.addBox(-11.0F, 0.0F, -1.5F, 22.0F, 12.0F, 7.5F, 0.0F);
        this.setRotateAngle(fur_chest, -0.1396F, 0.0F, 0.0F);

        this.leg_back_left_1 = new AdvancedModelBox(this, 96, 116);
        this.leg_back_left_1.setRotationPoint(7.5F, 1.0F, 3.0F);
        this.leg_back_left_1.addBox(-3.5F, -1.0F, -5.0F, 6.6F, 12.5F, 11.0F, 0.0F);
        this.setRotateAngle(leg_back_left_1, -0.2094F, 0.0F, 0.0F);

        this.leg_back_left_2 = new AdvancedModelBox(this, 68, 141);
        this.leg_back_left_2.setRotationPoint(0.0F, 11.5F, 0.0F);
        this.leg_back_left_2.addBox(-3.0F, -1.0F, -4.75F, 6.0F, 10.5F, 10.0F, 0.0F);
        this.setRotateAngle(leg_back_left_2, 0.3142F, 0.0F, 0.0F);

        this.foot_back_left = new AdvancedModelBox(this, 40, 162);
        this.foot_back_left.setRotationPoint(0.0F, 9.5F, 0.0F);
        this.foot_back_left.addBox(-3.75F, -0.5F, -4.75F, 7.5F, 9.0F, 10.5F, 0.0F);
        this.setRotateAngle(foot_back_left, -0.1047F, 0.0F, 0.0F);

        this.leg_back_right_1 = new AdvancedModelBox(this, 132, 116);
        this.leg_back_right_1.setRotationPoint(-7.5F, 1.0F, 3.0F);
        this.leg_back_right_1.addBox(-3.1F, -1.0F, -5.0F, 6.6F, 12.5F, 11.0F, 0.0F);
        this.setRotateAngle(leg_back_right_1, -0.2094F, 0.0F, 0.0F);

        this.leg_back_right_2 = new AdvancedModelBox(this, 100, 141);
        this.leg_back_right_2.setRotationPoint(0.0F, 11.5F, 0.0F);
        this.leg_back_right_2.addBox(-3.0F, -1.0F, -4.75F, 6.0F, 10.5F, 10.0F, 0.0F);
        this.setRotateAngle(leg_back_right_2, 0.3142F, 0.0F, 0.0F);

        this.foot_back_right = new AdvancedModelBox(this, 78, 162);
        this.foot_back_right.setRotationPoint(0.0F, 9.5F, 0.0F);
        this.foot_back_right.addBox(-3.75F, -0.5F, -4.75F, 7.5F, 9.0F, 10.5F, 0.0F);
        this.setRotateAngle(foot_back_right, -0.1047F, 0.0F, 0.0F);

        this.fur_skirt_left = new AdvancedModelBox(this, 0, 0);
        this.fur_skirt_left.setRotationPoint(11.5F, 3.0F, 0.0F);
        this.fur_skirt_left.addBox(-0.5F, 0.0F, -26.0F, 2.0F, 12.0F, 35.0F, 0.0F);
        this.setRotateAngle(fur_skirt_left, 0.0F, 0.0F, -0.1047F);

        this.fur_skirt_right = new AdvancedModelBox(this, 74, 0);
        this.fur_skirt_right.setRotationPoint(-11.5F, 3.0F, 0.0F);
        this.fur_skirt_right.addBox(-1.5F, 0.0F, -26.0F, 2.0F, 12.0F, 35.0F, 0.0F);
        this.setRotateAngle(fur_skirt_right, 0.0F, 0.0F, 0.1047F);

        this.body_hips.addChild(this.body_rump);
        this.body_rump.addChild(this.tail);
        this.tail.addChild(this.tail_tuft);
        this.body_rump.addChild(this.fur_rump);
        this.body_hips.addChild(this.body_chest);
        this.body_chest.addChild(this.body_hump);
        this.body_chest.addChild(this.neck);
        this.neck.addChild(this.head);
        this.head.addChild(this.head_dome);
        this.head.addChild(this.ear_left);
        this.head.addChild(this.ear_right);
        this.head.addChild(this.eye_left);
        this.head.addChild(this.eye_right);
        this.head.addChild(this.trunk_1);
        this.trunk_1.addChild(this.trunk_2);
        this.trunk_2.addChild(this.trunk_3);
        this.trunk_3.addChild(this.trunk_4);
        this.trunk_4.addChild(this.trunk_5);
        this.trunk_5.addChild(this.trunk_tip);
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
        this.body_chest.addChild(this.fur_mane);
        this.body_chest.addChild(this.fur_chest);
        this.body_hips.addChild(this.leg_back_left_1);
        this.leg_back_left_1.addChild(this.leg_back_left_2);
        this.leg_back_left_2.addChild(this.foot_back_left);
        this.body_hips.addChild(this.leg_back_right_1);
        this.leg_back_right_1.addChild(this.leg_back_right_2);
        this.leg_back_right_2.addChild(this.foot_back_right);
        this.body_hips.addChild(this.fur_skirt_left);
        this.body_hips.addChild(this.fur_skirt_right);

        // The tusks are a 5-box chain, so scaling only the root box would fatten one segment and leave
        // the rest behind. setShouldScaleChildren makes AdvancedModelBox apply its scale to the whole
        // subtree (translateAndRotate pushes the scale onto the PoseStack instead of cancelling it out
        // before the children render), so hasLargeTusks grows the entire tusk about its base pivot.
        this.tusk_left_1.setShouldScaleChildren(true);
        this.tusk_right_1.setShouldScaleChildren(true);

        animator = ModelAnimator.create();
        updateDefaultPose();
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(body_hips);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(
            body_hips, body_rump, tail, tail_tuft,
            body_chest, body_hump, neck, head, head_dome,
            ear_left, ear_right, eye_left, eye_right,
            trunk_1, trunk_2, trunk_3, trunk_4, trunk_5, trunk_tip,
            tusk_left_1, tusk_left_2, tusk_left_3, tusk_left_4, tusk_left_5,
            tusk_right_1, tusk_right_2, tusk_right_3, tusk_right_4, tusk_right_5,
            fur_neck, fur_mane, fur_chest, fur_skirt_left, fur_skirt_right, fur_rump,
            leg_front_left_1, leg_front_left_2, foot_front_left,
            leg_front_right_1, leg_front_right_2, foot_front_right,
            leg_back_left_1, leg_back_left_2, foot_back_left,
            leg_back_right_1, leg_back_right_2, foot_back_right
        );
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityMammoth mammoth = (EntityMammoth) entityIn;
        animator.update(mammoth);

        // Threat display: rock the shoulders, swing the head so the tusks sweep, and paw the ground.
        animator.setAnimation(EntityMammoth.ATTACK_THREATEN);
        for (int i = 0; i < 2; i++) {
            animator.startKeyframe(12);
            this.rotate(animator, body_chest, 0, 0, 6.5F);
            this.rotate(animator, neck, 5.2F, 0, -10.4F);
            this.rotate(animator, head, 5.2F, 0, -7.8F);
            this.rotate(animator, trunk_1, -18F, 0, 0);
            this.rotate(animator, trunk_2, -14F, 0, 0);
            animator.move(leg_front_left_1, 0, -0.6F, 0);
            this.rotate(animator, leg_front_left_1, 0, 0, -6.5F);
            animator.move(leg_front_right_1, 0, -0.5F, 0);
            this.rotate(animator, leg_front_right_1, -18F, 0, -5.2F);
            animator.endKeyframe();
            animator.startKeyframe(9);
            this.rotate(animator, body_chest, 0, 0, -10.4F);
            this.rotate(animator, neck, 5.2F, 0, 20.9F);
            this.rotate(animator, head, 5.2F, 0, 13.0F);
            this.rotate(animator, trunk_1, -10F, 0, 0);
            this.rotate(animator, trunk_2, -8F, 0, 0);
            animator.move(leg_front_left_1, 0, 0.5F, 0);
            this.rotate(animator, leg_front_left_1, 0, 0, 10.4F);
            animator.move(leg_front_right_1, 0, 0.5F, 0);
            this.rotate(animator, leg_front_right_1, 22F, 0, 10.4F);
            animator.endKeyframe();
        }
        animator.resetKeyframe(8);

        // Gore: rear the head back, then drive the tusks forward and up.
        animator.setAnimation(EntityMammoth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 20.9F, 0, 13.0F);
        this.rotate(animator, head, 15.6F, 0, 10.4F);
        this.rotate(animator, trunk_1, -26F, 0, 0);
        this.rotate(animator, trunk_2, -20F, 0, 0);
        this.rotate(animator, trunk_3, -16F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -20.9F, 0, -31.3F);
        this.rotate(animator, head, -18.2F, 0, -20.9F);
        this.rotate(animator, trunk_1, 10F, 0, 0);
        this.rotate(animator, trunk_2, 14F, 0, 0);
        this.rotate(animator, trunk_3, 18F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(4);
    }

    public void setupAnim(EntityMammoth mammoth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.resetToDefaultPose();
        animate(mammoth);
        float globalSpeed = 1.1f;
        float globalDegree = 1f;
        float f = limbSwing / 2;
        limbSwingAmount = Math.min(0.4F, limbSwingAmount);

        // ---- Per-species toggles (Part B). Entity-flag driven, so they only run in a live client;
        // Blockbench never calls setupAnim, which is why every species looks identical there. The model
        // instance is shared across all mammoths, so BOTH branches must be written every frame or one
        // species' state leaks into the next entity rendered.
        boolean woollyCoat = mammoth.hasWoollyCoat();
        this.fur_mane.showModel = woollyCoat;
        this.fur_neck.showModel = woollyCoat;
        this.fur_chest.showModel = woollyCoat;
        this.fur_rump.showModel = woollyCoat;
        this.fur_skirt_left.showModel = woollyCoat;
        this.fur_skirt_right.showModel = woollyCoat;

        float tuskScale = mammoth.hasLargeTusks() ? 1.35F : 1.0F;
        this.tusk_left_1.setScale(tuskScale, tuskScale, tuskScale);
        this.tusk_right_1.setScale(tuskScale, tuskScale, tuskScale);

        // Mastodon/Cuvieronius: low flat skull and a much shallower shoulder hump.
        boolean flatBack = mammoth.hasFlatBack();
        this.head_dome.setScale(1.0F, flatBack ? 0.35F : 1.0F, 1.0F);
        this.body_hump.setScale(1.0F, flatBack ? 0.55F : 1.0F, 1.0F);

        // ---- Idle: trunk hangs and sways, amplitude growing toward the tip so the chain reads as a
        // heavy muscular hose rather than a stiff rod. Ears flick, tail swings.
        this.walk(trunk_1, 0.10F, 0.035F, false, 0.0F, 0F, ageInTicks, 1);
        this.walk(trunk_2, 0.10F, 0.05F, false, 0.6F, 0F, ageInTicks, 1);
        this.walk(trunk_3, 0.10F, 0.07F, false, 1.2F, 0F, ageInTicks, 1);
        this.walk(trunk_4, 0.10F, 0.09F, false, 1.8F, 0F, ageInTicks, 1);
        this.walk(trunk_5, 0.10F, 0.11F, false, 2.4F, 0F, ageInTicks, 1);
        this.walk(trunk_tip, 0.10F, 0.14F, false, 3.0F, 0F, ageInTicks, 1);
        this.swing(trunk_2, 0.07F, 0.04F, false, 1.0F, 0F, ageInTicks, 1);
        this.swing(trunk_4, 0.07F, 0.07F, false, 2.0F, 0F, ageInTicks, 1);
        this.swing(trunk_tip, 0.07F, 0.10F, false, 3.0F, 0F, ageInTicks, 1);
        this.flap(ear_left, 0.09F, 0.10F, true, 0F, 0F, ageInTicks, 1);
        this.flap(ear_right, 0.09F, 0.10F, false, 0F, 0F, ageInTicks, 1);
        this.swing(tail, 0.10F, 0.16F, false, 0F, 0F, ageInTicks, 1);
        this.walk(tail_tuft, 0.10F, 0.10F, false, 1F, 0F, ageInTicks, 1);

        // Breathing
        this.body_hips.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.06F), (float) (1.0F + Math.sin(ageInTicks / 16) * 0.06F), 1.0F);
        this.body_chest.setScale((float) (1.0F + Math.sin(ageInTicks / 20) * 0.06F), (float) (1.0F + Math.sin(ageInTicks / 16) * 0.06F), 1.0F);
        bob(body_hips, 0.4F * globalSpeed, 0.1F, false, ageInTicks / 20, 2);
        bob(leg_front_left_1, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_front_right_1, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_back_left_1, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        bob(leg_back_right_1, 0.4F * globalSpeed, 0.1F, false, -ageInTicks / 20, 2);
        walk(neck, 0.4f * globalSpeed, 0.03f, false, 2.8F, 0.06F, ageInTicks / 20, 2);

        // Blinking: bury the eye planes inside the skull (|x| 9.35 -> 8.0) so they stop rendering.
        if (!mammoth.shouldRenderEyes()) {
            this.eye_left.setRotationPoint(8.0F, -1.5F, -6.0F);
            this.eye_right.setRotationPoint(-8.0F, -1.5F, -6.0F);
        }

        // Head tracking
        if (!mammoth.isSleeping()) {
            this.faceTarget(netHeadYaw, headPitch, 3, neck);
            this.faceTarget(netHeadYaw, headPitch, 3, head);
        }

        // Pitch/Yaw handler
        if (mammoth.isInWater()) {
            this.setRotateAngle(head, -0.18203784098300857F, 0.0F, 0.0F);
            if (!mammoth.isOnGround()) {
                f = ageInTicks / 6;
                limbSwingAmount = 0.5f;
                float pitch = Mth.clamp(mammoth.getXRot() - 10, -25F, 25.0F);
                this.setRotateAngle(body_hips, (float) (pitch * Math.PI / 180F), 0, 0);
            }
        }

        // ---- Walk. Two-segment legs driven exactly like ModelRhino: the upper segment leads and the
        // lower segment follows a step out of phase (and slightly hotter) so the knee/hock flexes
        // through the stride instead of the whole leg swinging as one rigid post. Front and hind run at
        // opposite sign for a diagonal gait; the feet counter-rotate so the sole stays flat at plant.
        // Speeds are lower than the rhino's -- a mammoth strides slowly and heavily.
        if (mammoth.canMove()) {
            bob(body_hips, 0.8f * globalSpeed, 0.5f * globalDegree, true, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.18f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);

            walk(leg_front_right_1, -0.8f * globalSpeed, 1.0f * globalDegree, true, 0F, 1.0f, f, limbSwingAmount);
            walk(leg_front_right_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, -1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(foot_front_right, -0.8f * globalSpeed, 0.5f * globalDegree, true, -2F, 0, f, limbSwingAmount);
            walk(leg_front_left_1, -0.8f * globalSpeed, 1.0f * globalDegree, true, 2F, 1.0f, f, limbSwingAmount);
            walk(leg_front_left_2, -0.8f * globalSpeed, 1.0f * globalDegree, false, 1F, 1.0f, f, limbSwingAmount * 1.2f);
            walk(foot_front_left, -0.8f * globalSpeed, 0.5f * globalDegree, true, 0F, 0, f, limbSwingAmount);

            walk(leg_back_right_1, 0.8f * globalSpeed, 1.0f * globalDegree, false, 2.8F, 0, f, limbSwingAmount);
            walk(leg_back_right_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, 1.8F, 0, f, limbSwingAmount);
            walk(foot_back_right, 0.8f * globalSpeed, 0.5f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_back_left_1, 0.8f * globalSpeed, 1.0f * globalDegree, false, 0.8F, 0, f, limbSwingAmount);
            walk(leg_back_left_2, 0.8f * globalSpeed, 1.0f * globalDegree, true, -0.2F, 0, f, limbSwingAmount);
            walk(foot_back_left, 0.8f * globalSpeed, 0.5f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);

            // The trunk swings with the stride, lagging further down the chain.
            walk(trunk_1, 0.8f * globalSpeed, 0.22f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(trunk_2, 0.8f * globalSpeed, 0.26f * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
            walk(trunk_3, 0.8f * globalSpeed, 0.30f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(trunk_4, 0.8f * globalSpeed, 0.34f * globalDegree, false, -1.8F, 0, f, limbSwingAmount);
            walk(trunk_5, 0.8f * globalSpeed, 0.38f * globalDegree, false, -2.4F, 0, f, limbSwingAmount);
            walk(trunk_tip, 0.8f * globalSpeed, 0.42f * globalDegree, false, -3.0F, 0, f, limbSwingAmount);
            swing(tail, 0.8f * globalSpeed, 0.35f * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }

        // ---- Rest / sleep. Both use one validated pose: the body sinks 16 units so the belly settles
        // just off the ground, each leg folds against itself (front knee forward, hind hock back) and
        // the trunk lies stretched out in front. The fold angles were solved against the actual box
        // geometry so all four feet land AT ground level -- the previous model dropped the body but left
        // the legs unfolded, so they hung in the air underneath it.
        if (mammoth.sitProgress > 0) {
            applyRestingPose(mammoth.sitProgress);
        }
        else if (mammoth.sleepProgress > 0) {
            applyRestingPose(mammoth.sleepProgress);
        }
    }

    private void applyRestingPose(float progress) {
        this.progressPosition(body_hips, progress, 0.0F, 10.0F, 4.0F, 40);

        this.progressRotation(leg_front_left_1, progress, (float) Math.toRadians(-30F), 0, 0, 40);
        this.progressRotation(leg_front_left_2, progress, (float) Math.toRadians(130F), 0, 0, 40);
        this.progressRotation(foot_front_left, progress, 0, 0, 0, 40);
        this.progressRotation(leg_front_right_1, progress, (float) Math.toRadians(-30F), 0, 0, 40);
        this.progressRotation(leg_front_right_2, progress, (float) Math.toRadians(130F), 0, 0, 40);
        this.progressRotation(foot_front_right, progress, 0, 0, 0, 40);

        this.progressRotation(leg_back_left_1, progress, (float) Math.toRadians(45F), 0, 0, 40);
        this.progressRotation(leg_back_left_2, progress, (float) Math.toRadians(-140F), 0, 0, 40);
        this.progressRotation(foot_back_left, progress, 0, 0, 0, 40);
        this.progressRotation(leg_back_right_1, progress, (float) Math.toRadians(45F), 0, 0, 40);
        this.progressRotation(leg_back_right_2, progress, (float) Math.toRadians(-140F), 0, 0, 40);
        this.progressRotation(foot_back_right, progress, 0, 0, 0, 40);

        this.progressRotation(neck, progress, (float) Math.toRadians(2F), 0, 0, 40);
        this.progressRotation(head, progress, (float) Math.toRadians(8F), 0, 0, 40);

        this.progressRotation(trunk_1, progress, (float) Math.toRadians(-40F), 0, 0, 40);
        this.progressRotation(trunk_2, progress, (float) Math.toRadians(-30F), 0, 0, 40);
        this.progressRotation(trunk_3, progress, (float) Math.toRadians(-20F), 0, 0, 40);
        this.progressRotation(trunk_4, progress, (float) Math.toRadians(-4F), 0, 0, 40);
        this.progressRotation(trunk_5, progress, (float) Math.toRadians(-2F), 0, 0, 40);
        this.progressRotation(trunk_tip, progress, (float) Math.toRadians(-2F), 0, 0, 40);
    }
}
