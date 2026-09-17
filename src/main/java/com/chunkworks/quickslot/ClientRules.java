/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client preferences expressed as data only: this class contains no rendering/client types. */
public final class ClientRules {
    private ClientRules() {}
    public enum BackSide { OPPOSITE_MAIN_ARM, MAIN_ARM, LEFT, RIGHT }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RENDER, DYNAMIC_LIGHT, BACKPACK_PUSHOUT, MOVE_BELT;
    public static final ModConfigSpec.EnumValue<BackSide> BACK_SIDE;
    public static final ModConfigSpec.DoubleValue BACK_SCALE, BELT_SCALE, MAX_LENGTH, CHEST_OFFSET,
            LEGS_OFFSET, BULKY_OFFSET, PACK_ANGLE, PACK_X, PACK_Y, PACK_Z, PACK_DEPTH,
            BOTTLE_SCALE, POUCH_SCALE, BOWL_SCALE, HANG_SCALE;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ARMOR_OFFSETS;
    static {
        var b = new ModConfigSpec.Builder();
        RENDER = b.define("render", true);
        BACK_SIDE = b.defineEnum("backSide", BackSide.OPPOSITE_MAIN_ARM);
        BACK_SCALE = b.defineInRange("backScale", 1.0, 0.1, 2.0);
        BELT_SCALE = b.defineInRange("beltScale", 1.0, 0.1, 2.0);
        MAX_LENGTH = b.comment("Maximum measured back-item diagonal in blocks, including model display transforms.")
                .defineInRange("maximumBackLength", 0.98, 0.2, 1.5);
        CHEST_OFFSET = b.comment("Vanilla outer armor inflates by one pixel; lower values are not allowed.")
                .defineInRange("chestArmorOffset", 0.0625, 0.0625, 0.5);
        LEGS_OFFSET = b.defineInRange("leggingsOffset", 0.03125, 0.03125, 0.5);
        BULKY_OFFSET = b.defineInRange("bulkyArmorExtraOffset", 0.125, 0, 1);
        ARMOR_OFFSETS = b.comment("Extra block-space clearance per armor item: namespace:item=0.125.")
                .defineListAllowEmpty("armorItemOffsets", List.of(), () -> "minecraft:diamond_chestplate=0.0",
                        value -> value instanceof String s && s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=[0-9]+(\\.[0-9]+)?"));
        PACK_ANGLE = b.defineInRange("backpackSlingAngle", 32.0, -80, 80);
        PACK_X = b.defineInRange("backpackOffsetX", 0.07, -0.3, 0.3);
        PACK_Y = b.defineInRange("backpackOffsetY", -0.02, -0.3, 0.3);
        PACK_Z = b.defineInRange("backpackOffsetZ", 0.0, -0.1, 0.5);
        PACK_DEPTH = b.defineInRange("backpackDepthScale", 0.35, 0.05, 1.0);
        BACKPACK_PUSHOUT = b.define("backpackPushout", true);
        MOVE_BELT = b.define("moveLowerBackToHipWithBackpack", true);
        BOTTLE_SCALE = b.defineInRange("bottleLength", 0.27, 0.1, 0.5);
        POUCH_SCALE = b.defineInRange("pouchLength", 0.25, 0.1, 0.5);
        BOWL_SCALE = b.defineInRange("bowlLength", 0.23, 0.1, 0.5);
        HANG_SCALE = b.defineInRange("hangingLength", 0.43, 0.1, 0.6);
        DYNAMIC_LIGHT = b.define("dynamicLight", true);
        HUD_X = b.comment("Horizontal distance adjustment; the same value mirrors with the main arm.")
                .defineInRange("hudOffsetX", 0, -64, 256);
        HUD_Y = b.defineInRange("hudOffsetY", 0, -256, 64);
        SPEC = b.build();
    }
}
