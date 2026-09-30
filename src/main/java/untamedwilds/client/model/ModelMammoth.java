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

        // Rump depth cut 10 -> 6. At 10 the rear-bottom corner swung out to world z 22.0 -- 15 units
        // BEHIND the hip joint (z 7.98 equivalent: leg_back pivot at z 7), i.e. more than a third of
        // the shoulder height of overhang, which read as a shelf of butt sticking out past the hind
        // legs. A proboscidean croup falls away steeply and short: the rear now lands at z 18.0
        // (bottom) / 15.3 (top), ~11 behind the hip. Depth-only change, so the UV footprint SHRINKS
        // (64x21 -> 56x17) inside its existing slot -- no repack needed, the hide art just resamples.
        this.body_rump = new AdvancedModelBox(this, 168, 116);
        this.body_rump.setRotationPoint(0.0F, -5.0F, 6.0F);
        this.body_rump.addBox(-11.0F, -1.0F, 0.0F, 22.0F, 11.0F, 6.0F, 0.0F);
        this.setRotateAngle(body_rump, -0.2269F, 0.0F, 0.0F);

        // Pivot follows the shortened rump forward (8 -> 4) so the dock still sits 2 units inboard of
        // the new rear face instead of floating 4 units behind it.
        this.tail = new AdvancedModelBox(this, 212, 162);
        this.tail.setRotationPoint(0.0F, 3.0F, 4.0F);
        this.tail.addBox(-2.0F, 0.0F, -0.5F, 4.0F, 11.0F, 3.5F, 0.0F);
        this.setRotateAngle(tail, 0.2443F, 0.0F, 0.0F);

        this.tail_tuft = new AdvancedModelBox(this, 82, 182);
        this.tail_tuft.setRotationPoint(0.0F, 11.0F, 1.0F);
        this.tail_tuft.addBox(-2.75F, -0.5F, -2.25F, 5.5F, 7.0F, 5.0F, 0.0F);

        // The woolly skirt was the worst offender: 14 units tall stacked at -0.2269 ON TOP of the
        // rump's own -0.2269 meant a 26 deg total sweep, so its hem drifted 6 units rearward and the
        // plate ended at world z 26.6 -- a fur shelf hanging 4.5 past the body itself. Pivot follows
        // the shortened rump (8 -> 4.5, still straddling the new rear face) and the OWN tilt drops to
        // -0.1222 (20 deg total) so the skirt hangs nearer to plumb. Rear now z 22.2, hem y 4.3 --
        // the hem barely moves, so it still meets fur_skirt_left/right along the flanks. Height and
        // depth are untouched, so the UV footprint is unchanged.
        this.fur_rump = new AdvancedModelBox(this, 166, 162);
        this.fur_rump.setRotationPoint(0.0F, 6.0F, 4.5F);
        this.fur_rump.addBox(-10.0F, -2.0F, -0.5F, 20.0F, 14.0F, 3.0F, 0.0F);
        this.setRotateAngle(fur_rump, -0.1222F, 0.0F, 0.0F);

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
        // Flanks stepped in from +-9.0 to +-8.6. The neck and the head BOTH had their side faces at
        // exactly +-9.0, so wherever they overlapped in z the two cheeks were perfectly coplanar and
        // shimmered (22.7u2 of shared patch in scripts/zfight_sweep.py). Narrowing the neck rather
        // than the head keeps the painted face art on the head at its original UV scale, and it also
        // restores the eye plane's clearance from its WIDEST ancestor -- the equid bug, where an eye
        // cleared the skull but sat inside the neck.
        this.neck.addBox(-8.6F, -4.0F, -7.0F, 17.2F, 14.0F, 10.0F, 0.0F);
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
        // Pivot stepped in 9.0 -> 8.25. The ear's outer face sat at world x 10.48, z-fighting both
        // fur_neck (10.00) and the neck (9.00). Because the ear is tilted on two axes each of its
        // faces sweeps ~0.57u of x, so the pivot has to CENTRE that sweep in the 0.70u-wide gap
        // between the head (9.0) and fur_neck (10.0) -- nudging it to either end re-grazes one.
        // The yaw is now ZERO rather than a token 2 degrees, because x'' = cos(yaw)*x - sin(yaw)*z
        // means ANY yaw couples the ear's x extent to its z extent -- and z is exactly what the
        // earSize scaling stretches. At 2 degrees the large ear's inner face swung back onto the
        // neck's 8.6 plane. Flat, the ear sits at inner 8.29 / outer 9.39 at every ear size.
        this.ear_left.setRotationPoint(8.49F, 0.0F, -1.0F);
        this.ear_left.addBox(-0.2F, -1.5F, -2.0F, 1.1F, 7.0F, 6.5F, 0.0F);
        this.setRotateAngle(ear_left, 0.0F, 0.0F, 0.0F);

        this.ear_right = new AdvancedModelBox(this, 0, 182);
        this.ear_right.setRotationPoint(-8.49F, 0.0F, -1.0F);
        this.ear_right.addBox(-0.9F, -1.5F, -2.0F, 1.1F, 7.0F, 6.5F, 0.0F);
        this.setRotateAngle(ear_right, 0.0F, 0.0F, 0.0F);

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
        // Widened +-11.5 -> +-11.75. The mane and body_hips both sat at exactly +-11.5, so the
        // shoulder cape was perfectly coplanar with the flank over 34u2 -- the single biggest
        // shimmering patch on the woolly species. Still inboard of body_chest (12.0).
        this.fur_mane.addBox(-11.75F, -7.5F, -1.0F, 23.5F, 9.0F, 30.0F, 0.0F);
        this.setRotateAngle(fur_mane, -0.1571F, 0.0F, 0.0F);

        this.fur_chest = new AdvancedModelBox(this, 132, 141);
        this.fur_chest.setRotationPoint(0.0F, 3.0F, -19.0F);
        this.fur_chest.addBox(-11.0F, 0.0F, -1.5F, 22.0F, 12.0F, 7.5F, 0.0F);
        this.setRotateAngle(fur_chest, -0.1396F, 0.0F, 0.0F);

        this.leg_back_left_1 = new AdvancedModelBox(this, 96, 116);
        this.leg_back_left_1.setRotationPoint(7.5F, 1.0F, 3.0F);
        // Outer face stepped in 0.4 (world 10.6 -> 10.2) so it clears body_hump's 10.5. Width is
        // unchanged, so the UV footprint is untouched.
        this.leg_back_left_1.addBox(-3.9F, -1.0F, -5.0F, 6.6F, 12.5F, 11.0F, 0.0F);
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
        this.leg_back_right_1.addBox(-2.7F, -1.0F, -5.0F, 6.6F, 12.5F, 11.0F, 0.0F);
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

    /* Built once: Citadel asks for this every frame for every animal, and ImmutableList.of over
     * dozens of parts was a fresh allocation each time. Lazy because the parts only exist once
     * the constructor has built them. */
    private ImmutableList<AdvancedModelBox> allParts;

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        if (this.allParts == null) {
            this.allParts = ImmutableList.of(
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
        return this.allParts;
    }

    private void animate(IAnimatedEntity entityIn) {
        EntityMammoth mammoth = (EntityMammoth) entityIn;
        animator.update(mammoth);

        // Every keyframe below was checked numerically against the box geometry (2026-09-30): nothing goes
        // below ground, with the tusks at 1.0x and at the 1.35x large-tusk scale, and no trunk segment passes
        // through the skull.

        // Threat display: trumpet (head and trunk thrown up, ears flared, a forefoot raised), then stomp.
        animator.setAnimation(EntityMammoth.ATTACK_THREATEN);
        animator.startKeyframe(12);
        this.rotate(animator, neck, -22.0F, 0, 0);
        this.rotate(animator, head, -14.0F, 0, 0);
        this.rotate(animator, ear_left, 0, 40.0F, 0);
        this.rotate(animator, ear_right, 0, -40.0F, 0);
        this.rotate(animator, leg_front_right_1, -30.0F, 0, 0);
        this.rotate(animator, leg_front_right_2, 45.0F, 0, 0);
        this.rotate(animator, foot_front_right, -10.0F, 0, 0);
        this.rotate(animator, trunk_1, -25.0F, 0, 0);
        this.rotate(animator, trunk_2, -35.0F, 0, 0);
        this.rotate(animator, trunk_3, -40.0F, 0, 0);
        this.rotate(animator, trunk_4, -40.0F, 0, 0);
        this.rotate(animator, trunk_5, -35.0F, 0, 0);
        this.rotate(animator, trunk_tip, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.startKeyframe(7);
        this.rotate(animator, neck, 10.0F, 0, 6.0F);
        this.rotate(animator, head, 8.0F, 0, 4.0F);
        this.rotate(animator, ear_left, 0, 40.0F, 0);
        this.rotate(animator, ear_right, 0, -40.0F, 0);
        this.rotate(animator, trunk_1, -10.0F, 0, 0);
        this.rotate(animator, trunk_2, -8.0F, 0, 0);
        this.rotate(animator, trunk_3, 6.0F, 0, 0);
        this.rotate(animator, trunk_4, 10.0F, 0, 0);
        this.rotate(animator, trunk_5, 12.0F, 0, 0);
        this.rotate(animator, trunk_tip, 14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, neck, -22.0F, 0, 0);
        this.rotate(animator, head, -14.0F, 0, 0);
        this.rotate(animator, ear_left, 0, 40.0F, 0);
        this.rotate(animator, ear_right, 0, -40.0F, 0);
        this.rotate(animator, leg_front_right_1, -30.0F, 0, 0);
        this.rotate(animator, leg_front_right_2, 45.0F, 0, 0);
        this.rotate(animator, foot_front_right, -10.0F, 0, 0);
        this.rotate(animator, trunk_1, -25.0F, 0, 0);
        this.rotate(animator, trunk_2, -35.0F, 0, 0);
        this.rotate(animator, trunk_3, -40.0F, 0, 0);
        this.rotate(animator, trunk_4, -40.0F, 0, 0);
        this.rotate(animator, trunk_5, -35.0F, 0, 0);
        this.rotate(animator, trunk_tip, -30.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(7);
        this.rotate(animator, neck, 10.0F, 0, 6.0F);
        this.rotate(animator, head, 8.0F, 0, 4.0F);
        this.rotate(animator, ear_left, 0, 40.0F, 0);
        this.rotate(animator, ear_right, 0, -40.0F, 0);
        this.rotate(animator, trunk_1, -10.0F, 0, 0);
        this.rotate(animator, trunk_2, -8.0F, 0, 0);
        this.rotate(animator, trunk_3, 6.0F, 0, 0);
        this.rotate(animator, trunk_4, 10.0F, 0, 0);
        this.rotate(animator, trunk_5, 12.0F, 0, 0);
        this.rotate(animator, trunk_tip, 14.0F, 0, 0);
        animator.endKeyframe();
        animator.resetKeyframe(8);

        // Gore: head down with the trunk curled under out of harm's way, then a lunge that tosses the tusks
        // up and across.
        animator.setAnimation(EntityMammoth.ATTACK_GORE);
        animator.startKeyframe(6);
        this.rotate(animator, neck, 21.0F, 0, 13.0F);
        this.rotate(animator, head, 16.0F, 0, 10.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_1, 12.0F, 0, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_tip, 14.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(4);
        this.rotate(animator, neck, -21.0F, 0, -31.0F);
        this.rotate(animator, head, -18.0F, 0, -21.0F);
        this.rotate(animator, ear_left, 0, 25.0F, 0);
        this.rotate(animator, ear_right, 0, -25.0F, 0);
        this.rotate(animator, trunk_1, 14.0F, 0, 0);
        this.rotate(animator, trunk_2, 26.0F, 0, 0);
        this.rotate(animator, trunk_3, 32.0F, 0, 0);
        this.rotate(animator, trunk_4, 28.0F, 0, 0);
        this.rotate(animator, trunk_5, 20.0F, 0, 0);
        this.rotate(animator, trunk_tip, 14.0F, 0, 0);
        animator.move(body_hips, 0, 0, -2.0F);
        animator.endKeyframe();
        animator.resetKeyframe(4);

        // Graze: head drops and the trunk reaches the ground, the tip curls round a mouthful, then the whole
        // trunk coils back under the skull to the mouth.
        animator.setAnimation(EntityMammoth.EAT);
        animator.startKeyframe(10);
        this.rotate(animator, neck, 27.0F, 0, 0);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, trunk_1, -12.0F, 0, 0);
        this.rotate(animator, trunk_2, -4.0F, 0, 0);
        this.rotate(animator, trunk_3, 2.0F, 0, 0);
        this.rotate(animator, trunk_4, 4.0F, 0, 0);
        this.rotate(animator, trunk_5, 3.0F, 0, 0);
        this.rotate(animator, trunk_tip, 1.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(6);
        this.rotate(animator, neck, 27.0F, 0, 0);
        this.rotate(animator, head, 6.0F, 0, 0);
        this.rotate(animator, trunk_1, -12.0F, 0, 0);
        this.rotate(animator, trunk_2, -4.0F, 0, 0);
        this.rotate(animator, trunk_3, 2.0F, 0, 0);
        this.rotate(animator, trunk_4, 14.0F, 0, 0);
        this.rotate(animator, trunk_5, 28.0F, 0, 0);
        this.rotate(animator, trunk_tip, 40.0F, 0, 0);
        animator.endKeyframe();
        animator.startKeyframe(10);
        this.rotate(animator, neck, -4.0F, 0, 0);
        this.rotate(animator, head, 0, 0, 0);
        this.rotate(animator, trunk_1, -11.0F, 0, 0);
        this.rotate(animator, trunk_2, 39.0F, 0, 0);
        this.rotate(animator, trunk_3, 63.0F, 0, 0);
        this.rotate(animator, trunk_4, 68.0F, 0, 0);
        this.rotate(animator, trunk_5, 59.0F, 0, 0);
        this.rotate(animator, trunk_tip, 50.0F, 0, 0);
        animator.endKeyframe();
        animator.setStaticKeyframe(6);
        animator.resetKeyframe(8);
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

        // Ear size: 0 small (every extinct proboscidean), 1 medium (Elephas maximus), 2 the big fan
        // (Loxodonta africana). X is deliberately left at 1.0 -- the ear's lateral position is solved
        // against the head (9.0), neck (8.6) and fur_neck (10.0) face planes, and scaling it sideways
        // would walk it straight back into one of them.
        // The pivot also moves BACKWARD as the ear grows. setScale expands the box about its rotation
        // point, so a bigger ear grows forward as well as back, and the eye plane's rear edge is only
        // at head-local z = -4.5. Constraint held here: pivotZ - 2*earZ >= -4.0, i.e. the ear's
        // leading edge stays at least 0.5u behind the eye at every size.
        int earSize = mammoth.getEarSize();
        float earY = earSize == 2 ? 2.2F : earSize == 1 ? 1.5F : 1.0F;
        float earZ = earSize == 2 ? 1.9F : earSize == 1 ? 1.35F : 1.0F;
        float earPivotZ = earSize == 2 ? 0.5F : earSize == 1 ? -0.6F : -1.0F;
        this.ear_left.setScale(1.0F, earY, earZ);
        this.ear_right.setScale(1.0F, earY, earZ);
        this.ear_left.setRotationPoint(8.49F, 0.0F, earPivotZ);
        this.ear_right.setRotationPoint(-8.49F, 0.0F, earPivotZ);
        // Flap amplitude falls as the ear grows, so the tip travels a similar absolute distance
        // instead of a 12-unit fan swinging as far as a 7-unit one -- which both looks wrong and
        // sweeps the ear's outer face through the skull surface.
        float earFlap = earSize == 2 ? 0.045F : earSize == 1 ? 0.067F : 0.10F;

        // Mastodon/Cuvieronius: low flat skull and a much shallower shoulder hump.
        boolean flatBack = mammoth.hasFlatBack();
        this.head_dome.setScale(1.0F, flatBack ? 0.35F : 1.0F, 1.0F);
        this.body_hump.setScale(1.0F, flatBack ? 0.55F : 1.0F, 1.0F);

        // ---- Idle: trunk hangs and sways, amplitude growing toward the tip so the chain reads as a
        // heavy muscular hose rather than a stiff rod. Ears flick, tail swings. Lying down, the trunk rests
        // ON the ground, so its sway is cut to 30% -- at full amplitude the tip swings through the floor.
        float rest = Math.max(mammoth.sitProgress, mammoth.sleepProgress) / 40F;
        float sway = 1.0F - 0.7F * rest;
        this.walk(trunk_1, 0.10F, 0.035F * sway, false, 0.0F, 0F, ageInTicks, 1);
        this.walk(trunk_2, 0.10F, 0.05F * sway, false, 0.6F, 0F, ageInTicks, 1);
        this.walk(trunk_3, 0.10F, 0.07F * sway, false, 1.2F, 0F, ageInTicks, 1);
        this.walk(trunk_4, 0.10F, 0.09F * sway, false, 1.8F, 0F, ageInTicks, 1);
        this.walk(trunk_5, 0.10F, 0.11F * sway, false, 2.4F, 0F, ageInTicks, 1);
        this.walk(trunk_tip, 0.10F, 0.14F * sway, false, 3.0F, 0F, ageInTicks, 1);
        this.swing(trunk_2, 0.07F, 0.04F * sway, false, 1.0F, 0F, ageInTicks, 1);
        this.swing(trunk_4, 0.07F, 0.07F * sway, false, 2.0F, 0F, ageInTicks, 1);
        this.swing(trunk_tip, 0.07F, 0.10F * sway, false, 3.0F, 0F, ageInTicks, 1);
        this.flap(ear_left, 0.09F, earFlap, true, 0F, 0F, ageInTicks, 1);
        this.flap(ear_right, 0.09F, earFlap, false, 0F, 0F, ageInTicks, 1);
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

        // Blinking / asleep: squash the eye planes to 20% height about their centre, so the eye reads as a
        // shut dark slit instead of simply vanishing (the old trick buried the plane inside the skull).
        // Written every frame -- the model is shared, so a closed eye would otherwise leak to the next mammoth.
        float eyeOpen = mammoth.shouldRenderEyes() ? 1.0F : 0.2F;
        this.eye_left.setScale(1.0F, eyeOpen, 1.0F);
        this.eye_right.setScale(1.0F, eyeOpen, 1.0F);

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

        // ---- Walk: a lateral-sequence walk (LH, LF, RH, RF, a quarter-cycle apart) -- the elephant gait.
        // Upper leg, lower leg and foot per leg type were FITTED to the box geometry (2026-09-30) with the
        // exact Citadel walk() term at limbSwingAmount 0.4: each foot is planted for half the cycle and moves
        // backward while planted, lifts ~2u in swing, digs in at most 0.16u, stride ~12u. The inherited
        // rhino cycle it replaces dug in 0.9u and slid the planted hind feet FORWARD (a moonwalk).
        if (mammoth.canMove()) {
            bob(body_hips, 0.8f * globalSpeed, 0.3f * globalDegree, true, f, limbSwingAmount);
            flap(body_hips, 0.8f * globalSpeed, 0.08f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(neck, 0.8f * globalSpeed, 0.18f * globalDegree, false, 0, 0, f, limbSwingAmount);
            walk(head, 0.8f * globalSpeed, 0.12f * globalDegree, true, 0, 0, f, limbSwingAmount);

            walk(leg_back_left_1, 0.8f * globalSpeed, 0.745F, false, 0.000F, -0.229F, f, limbSwingAmount);
            walk(leg_back_left_2, 0.8f * globalSpeed, -0.596F, false, 0.895F, 0.944F, f, limbSwingAmount);
            walk(foot_back_left, 0.8f * globalSpeed, 0.248F, false, 0.443F, -0.606F, f, limbSwingAmount);
            walk(leg_front_left_1, 0.8f * globalSpeed, 0.769F, false, -1.571F, 0.280F, f, limbSwingAmount);
            walk(leg_front_left_2, 0.8f * globalSpeed, -0.717F, false, -2.348F, -1.199F, f, limbSwingAmount);
            walk(foot_front_left, 0.8f * globalSpeed, 0.334F, false, -2.151F, 0.819F, f, limbSwingAmount);
            walk(leg_back_right_1, 0.8f * globalSpeed, 0.745F, false, -3.142F, -0.229F, f, limbSwingAmount);
            walk(leg_back_right_2, 0.8f * globalSpeed, -0.596F, false, -2.247F, 0.944F, f, limbSwingAmount);
            walk(foot_back_right, 0.8f * globalSpeed, 0.248F, false, -2.699F, -0.606F, f, limbSwingAmount);
            walk(leg_front_right_1, 0.8f * globalSpeed, 0.769F, false, 1.571F, 0.280F, f, limbSwingAmount);
            walk(leg_front_right_2, 0.8f * globalSpeed, -0.717F, false, 0.794F, -1.199F, f, limbSwingAmount);
            walk(foot_front_right, 0.8f * globalSpeed, 0.334F, false, 0.991F, 0.819F, f, limbSwingAmount);

            // The trunk swings with the stride, lagging further down the chain.
            walk(trunk_1, 0.8f * globalSpeed, 0.22f * globalDegree, false, 0F, 0, f, limbSwingAmount);
            walk(trunk_2, 0.8f * globalSpeed, 0.26f * globalDegree, false, -0.6F, 0, f, limbSwingAmount);
            walk(trunk_3, 0.8f * globalSpeed, 0.30f * globalDegree, false, -1.2F, 0, f, limbSwingAmount);
            walk(trunk_4, 0.8f * globalSpeed, 0.34f * globalDegree, false, -1.8F, 0, f, limbSwingAmount);
            walk(trunk_5, 0.8f * globalSpeed, 0.38f * globalDegree, false, -2.4F, 0, f, limbSwingAmount);
            walk(trunk_tip, 0.8f * globalSpeed, 0.42f * globalDegree, false, -3.0F, 0, f, limbSwingAmount);
            swing(tail, 0.8f * globalSpeed, 0.35f * globalDegree, false, 0F, 0, f, limbSwingAmount);
        }

        // ---- Rest / sleep: sternal recumbency, solved numerically against the box geometry (2026-09-30).
        // The body sinks until the belly is 0.8u off the ground (headroom for the 6% breathing scale). At that
        // height a folded knee no longer fits, so the legs take the elephant's real lying posture: forelegs
        // stretched forward, hind legs straight back, all four feet within 0.1u of the ground. The tail lies
        // back along the ground, and the woolly coat flares and compresses instead of passing through it.
        // Checked with tusk scale 1.0 and 1.35: no box goes below ground in either pose.
        // Fur scales are written every frame (1.0 when standing) because the model instance is shared.
        float furRest = mammoth.sitProgress > 0 ? mammoth.sitProgress / 40F : mammoth.sleepProgress / 40F;
        this.fur_skirt_left.setScale(1.0F, 1.0F - 0.45F * furRest, 1.0F);
        this.fur_skirt_right.setScale(1.0F, 1.0F - 0.45F * furRest, 1.0F);
        this.fur_chest.setScale(1.0F, 1.0F - 0.40F * furRest, 1.0F);
        this.fur_rump.setScale(1.0F, 1.0F - 0.35F * furRest, 1.0F);
        if (mammoth.sitProgress > 0) {
            applyRestingPose(mammoth.sitProgress, false);
        }
        else if (mammoth.sleepProgress > 0) {
            applyRestingPose(mammoth.sleepProgress, true);
        }
    }

    private void applyRestingPose(float progress, boolean asleep) {
        this.progressPosition(body_hips, progress, 0.0F, 14.5F, 4.0F, 40);

        this.progressRotation(leg_front_left_1, progress, (float) Math.toRadians(-80F), 0, 0, 40);
        this.progressRotation(leg_front_left_2, progress, (float) Math.toRadians(20F), 0, 0, 40);
        this.progressRotation(foot_front_left, progress, (float) Math.toRadians(-20F), 0, 0, 40);
        this.progressRotation(leg_front_right_1, progress, (float) Math.toRadians(-80F), 0, 0, 40);
        this.progressRotation(leg_front_right_2, progress, (float) Math.toRadians(20F), 0, 0, 40);
        this.progressRotation(foot_front_right, progress, (float) Math.toRadians(-20F), 0, 0, 40);

        this.progressRotation(leg_back_left_1, progress, (float) Math.toRadians(80F), 0, 0, 40);
        this.progressRotation(leg_back_left_2, progress, 0, 0, 0, 40);
        this.progressRotation(foot_back_left, progress, (float) Math.toRadians(10F), 0, 0, 40);
        this.progressRotation(leg_back_right_1, progress, (float) Math.toRadians(80F), 0, 0, 40);
        this.progressRotation(leg_back_right_2, progress, 0, 0, 0, 40);
        this.progressRotation(foot_back_right, progress, (float) Math.toRadians(10F), 0, 0, 40);

        this.progressRotation(tail, progress, (float) Math.toRadians(65F), 0, 0, 40);
        this.progressRotation(tail_tuft, progress, (float) Math.toRadians(15F), 0, 0, 40);
        this.progressRotation(fur_rump, progress, (float) Math.toRadians(40F), 0, 0, 40);
        this.progressRotation(fur_skirt_left, progress, 0, 0, (float) Math.toRadians(-25F), 40);
        this.progressRotation(fur_skirt_right, progress, 0, 0, (float) Math.toRadians(25F), 40);

        if (asleep) {
            // Head down until the (large) tusks rest on the ground; trunk hangs, then lies forward.
            this.progressRotation(neck, progress, 0, 0, 0, 40);
            this.progressRotation(head, progress, (float) Math.toRadians(-4F), 0, 0, 40);
            this.progressRotation(trunk_1, progress, 0, 0, 0, 40);
            this.progressRotation(trunk_2, progress, (float) Math.toRadians(-40F), 0, 0, 40);
            this.progressRotation(trunk_3, progress, (float) Math.toRadians(-30F), 0, 0, 40);
            this.progressRotation(trunk_4, progress, 0, 0, 0, 40);
            this.progressRotation(trunk_5, progress, 0, 0, 0, 40);
            this.progressRotation(trunk_tip, progress, 0, 0, 0, 40);
        }
        else {
            // Resting awake: head held up, tusks well clear; trunk hangs to the ground and curls forward.
            this.progressRotation(neck, progress, (float) Math.toRadians(-4F), 0, 0, 40);
            this.progressRotation(head, progress, (float) Math.toRadians(-12F), 0, 0, 40);
            this.progressRotation(trunk_1, progress, 0, 0, 0, 40);
            this.progressRotation(trunk_2, progress, 0, 0, 0, 40);
            this.progressRotation(trunk_3, progress, (float) Math.toRadians(-40F), 0, 0, 40);
            this.progressRotation(trunk_4, progress, (float) Math.toRadians(-15F), 0, 0, 40);
            this.progressRotation(trunk_5, progress, (float) Math.toRadians(-15F), 0, 0, 40);
            this.progressRotation(trunk_tip, progress, (float) Math.toRadians(-15F), 0, 0, 40);
        }
    }
}
