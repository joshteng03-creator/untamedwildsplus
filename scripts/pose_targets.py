"""Per-model targets for the 2.8.0 EAT / DRINK / ALERT pose solver (scripts/pose_reach.py).

neck:   the first joint of the neck chain (its whole subtree is "the head and neck")
muzzle: the part(s) whose lowest point has to reach the grass / water (several = hidden families: the solve
        must work for every family, so the lowest one counts)
skip:   parts that may touch the ground or are moved by their own logic (ignored by the clearance test)
drink / eat / alert: False to skip a pose for this type (aquatic or special-cased)
"""

T = {
    # entity type      model                 neck root      muzzle parts
    "bison":        dict(model="ModelBison", neck="head_neck", muzzle=["head_main"], entity="EntityBison"),
    "deer":         dict(model="ModelDeer", neck="neck_lower", muzzle=["head_muzzle"], entity="EntityDeer"),
    "equid":        dict(model="ModelEquid", neck="neck", muzzle=["muzzle"], entity="EntityEquid"),
    "antelope":     dict(model="ModelAntelope", neck="neck_base", muzzle=["muzzle", "nose_bulb"], entity="EntityAntelope"),
    "camel":        dict(model="ModelCamel", neck="neck_1", muzzle=["head_nose", "head_nose_1"], entity="EntityCamel"),
    # DRINK the way giraffes really do: forelegs splayed wide, the chest lowered between them (pose_reach.solve_body),
    # THEN the neck solved on top -- instead of a neck folded 82 deg over straight legs
    "giraffid":     dict(model="ModelGiraffid", neck="neck_1", muzzle=["head_muzzle", "head_jaw"], entity="EntityGiraffid",
                         drink_body=dict(root="body_barrel", splay=30.0,
                                         fore=[("fore_left_shoulder", "fore_left_foot"), ("fore_right_shoulder", "fore_right_foot")],
                                         hind=[("hind_left_thigh", "hind_left_gaskin", "hind_left_cannon", "hind_left_foot"),
                                               ("hind_right_thigh", "hind_right_gaskin", "hind_right_cannon", "hind_right_foot")])),
    "rhino":        dict(model="ModelRhino", neck="head_neck", muzzle=["head_face"], entity="EntityRhino"),
    "tapir":        dict(model="ModelTapir", neck="neck", muzzle=["proboscis_2", "head_jaw"], entity="EntityTapir"),
    "toxodon":      dict(model="ModelToxodon", neck="neck", muzzle=["head_muzzle", "head_jaw"], entity="EntityToxodon"),
    "macrauchenia": dict(model="ModelMacrauchenia", neck="neck_base", muzzle=["proboscis_2", "head_jaw"], entity="EntityMacrauchenia"),
    "glyptodont":   dict(model="ModelGlyptodont", neck="head_neck", muzzle=["snout"], entity="EntityGlyptodont"),
    "ground_sloth": dict(model="ModelGroundSloth", neck="neck", muzzle=["lip_mega", "snout_mylo", "snout_short", "jaw_mega", "jaw_mylo", "jaw_short"],
                         entity="EntityGroundSloth"),
    "diprotodont":  dict(model="ModelDiprotodont", neck="neck", muzzle=["nose", "head_jaw"], entity="EntityDiprotodont"),
    "caprine":      dict(model="ModelCaprine", neck="neck", muzzle=["nose", "muzzle"], entity="EntityCaprine"),
    "wildebeest":   dict(model="ModelWildebeest", neck="neck_base", muzzle=["nose", "muzzle"], entity="EntityWildebeest", eat=False),
    "large_rodent": dict(model="ModelLargeRodent", neck="neck", muzzle=["nose", "muzzle"], entity="EntityLargeRodent", drink=False),
    "kangaroo":     dict(model="ModelKangaroo", neck="neck", muzzle=["nose", "muzzle"], entity="EntityKangaroo", eat=False, drink=False),
    # EAT is its own (hand-made, approved). DRINK = trunk style: the trunk tip SOLVED down into the water beyond the bank,
    # then coiled up to the mouth with the approved EAT coil (trunk_curl_java, copied verbatim from ModelMammoth's EAT,
    # Java degrees), twice. ALERT: head up to the head joint, ears FLARED sideways (yaw) -- a forward pitch swings an
    # elephant's huge ear over its face. Its legs are leg_front_* / foot_front_*, which FORE_WORDS does not match.
    "mammoth":      dict(model="ModelMammoth", neck="neck", muzzle=["trunk_6"], alert_muzzle=["head"], ear_flare=True,
                         entity="EntityMammoth", eat=False, fore=["foot_front_left", "foot_front_right"], drink_style="trunk",
                         smooth=["trunk_1", "trunk_2", "trunk_3", "trunk_4", "trunk_5", "trunk_6"],
                         drink_band=(-3.2, -2.2),
                         trunk_curl_java={"neck": -4.0, "trunk_1": -11.0, "trunk_2": 39.0, "trunk_3": 63.0, "trunk_4": 68.0,
                                          "trunk_5": 59.0, "trunk_6": 50.0}),
    "boar":         dict(model="ModelBoar", neck="head_main", muzzle=["head_snout", "head_mouth"], entity="EntityBoar", eat=False),
    # 2.9.0 giant ground birds: the long neck reaches down; the feet are the front of the stance (FORE_WORDS cannot
    # match a biped's legs)
    "ground_bird":  dict(model="ModelGroundBird", neck="neck_1", muzzle=["beak"], entity="EntityGroundBird",
                         fore=["foot_left", "foot_right"]),
    # 2.9.0 great apes: ALERT only. A knuckle-walker's neck cannot bring the mouth to the ground (the solve buried the
    # head 9u in the torso) -- apes feed with their HANDS, so EAT is a hand-made hand-to-mouth in ModelGreatApe and the
    # ape registers no DrinkGoal.
    "great_ape":    dict(model="ModelGreatApe", neck="neck", muzzle=["muzzle", "jaw", "nose"], entity="EntityGreatApe",
                         fore=["hand_left", "hand_right"], eat=False, drink=False),
    "hippo":        dict(model="ModelHippo", neck="head_neck", muzzle=["head_face"], entity="EntityHippo", eat=False, drink=False),
}

# Fixed geometry a model's setupAnim applies to every animal, mirrored here so fk_rig (and so the pose / carcass solvers)
# see what actually renders. KEEP IN SYNC with the Java constants named in each comment.
STATICS = {
    # ModelGroundSloth WIDE_* / JAW_MEGA_DEPTH (2.8.0 review: "far too thin", "the sag at the bottom of the face")
    "ModelGroundSloth": {
        "scale": {"jaw_mega": (1.0, 0.55, 1.0), "body_mid": (1.16, 1.0, 1.0), "body_hips": (1.18, 1.0, 1.0),
                  "body_rump": (1.18, 1.0, 1.0), "body_chest": (1.12, 1.0, 1.0), "body_back": (1.14, 1.0, 1.0)},
        "pivot_x": {"leg_left_thigh": 1.18, "leg_right_thigh": 1.18, "arm_left_1": 1.12, "arm_right_1": 1.12},
    },
}

# Leg-ish part names: excluded from the torso set used for the "neck through the body" test.
LIMB_WORDS = ("arm", "leg", "fore", "hind", "foot", "feet", "hand", "hoof", "toe", "paw", "claw", "thigh", "shin",
              "cannon", "gaskin", "upper", "lower", "forearm", "knee", "tail", "joey", "bird", "fur_", "club", "saddle")
# Hair and skin flaps: may pass through each other and the body, so they never count as "neck through the body".
SOFT_WORDS = ("beard", "mane", "fringe", "hair", "tuft", "ruff", "fur", "dewlap", "bell", "wattle", "crest")
# Leg parts used to find the front of the stance (the bank edge when drinking).
FORE_WORDS = ("fore", "arm", "hand")


# Parts NOT drawn on the approval sheets: the non-default families of a type, so a sheet shows one animal and not
# every horn / antler / snout family stacked on each other. (The solver still clears every family.)
HIDE = {
    "deer": ["palm_beam_left", "palm_lower_left", "palm_upper_left", "palm_digA_left", "palm_digB_left", "palm_digC_left",
             "palm_brow_left", "palm_beam_right", "palm_lower_right", "palm_upper_right", "palm_digA_right", "palm_digB_right",
             "palm_digC_right", "palm_brow_right", "throat_bell"],
    "antelope": ["nose_bulb", "horn_prong_rear_left", "horn_prong_rear_right"],
    "giraffid": ["oss_boss", "oss_palm_base_left", "oss_palm_left", "oss_palm_base_right", "oss_palm_right", "oss_spike_left",
                 "oss_spike_right", "oss_quad_left", "oss_quad_front_left", "oss_quad_right", "oss_quad_front_right"],
    "ground_sloth": ["snout_mylo", "jaw_mylo", "snout_short", "jaw_short"],
    "mammoth": ["fur_mane", "fur_neck", "fur_chest", "fur_rump", "fur_skirt_left", "fur_skirt_right", "head_dome_left",
                "head_dome_right", "head_crest"],
    "glyptodont": ["tail_club", "club_spike_left", "club_spike_right", "club_spike_top", "club_spike_rear"],
    "tapir": ["claw_left", "claw_right"],
    "diprotodont": ["nasal_boss", "joey_head", "joey_snout"],
    "kangaroo": ["claw_left", "claw_right", "joey_head", "joey_snout"],
    # 2.9.0 Meiolania's horns and club tail (shown on that one species): not in T (no eat/drink solve), read directly
    # by gen_carcass_pose so seven plain tortoises do not lie propped up on horns they do not have
    "tortoise": ["horn_left", "horn_right", "tail_1", "tail_2", "tail_club", "club_spike_left", "club_spike_right",
                 "club_horn_left", "club_horn_right"],
}
for _t, _h in HIDE.items():
    if _t in T:
        T[_t]["hide"] = _h
