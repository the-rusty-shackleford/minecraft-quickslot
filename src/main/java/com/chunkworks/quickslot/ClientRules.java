/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client preferences expressed as data only: this class contains no rendering/client types. */
public final class ClientRules {
    private ClientRules() {}
    public enum BackSide { OPPOSITE_MAIN_ARM, MAIN_ARM, LEFT, RIGHT }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue RENDER, DYNAMIC_LIGHT, MOVE_BELT;
    public static final ModConfigSpec.EnumValue<BackSide> BACK_SIDE;
    public static final ModConfigSpec.DoubleValue BACK_SCALE, BELT_SCALE, MAX_LENGTH,
            BOTTLE_SCALE, POUCH_SCALE, HANG_SCALE;
    public static final ModConfigSpec.IntValue HUD_X, HUD_Y;
    static {
        var b = new ModConfigSpec.Builder();
        RENDER = b.define("render", true);
        BACK_SIDE = b.defineEnum("backSide", BackSide.OPPOSITE_MAIN_ARM);
        BACK_SCALE = b.defineInRange("backScale", 1.0, 0.1, 2.0);
        BELT_SCALE = b.defineInRange("beltScale", 1.0, 0.1, 2.0);
        MAX_LENGTH = b.comment("Maximum measured back-item diagonal in blocks, including model display transforms.")
                .defineInRange("maximumBackLength", 0.98, 0.2, 1.5);
        MOVE_BELT = b.comment("Move lower-back items to the hip while Backpacks+ is visibly worn. Back items remain hidden.")
                .define("moveLowerBackToHipWithBackpack", true);
        BOTTLE_SCALE = b.defineInRange("bottleLength", 0.27, 0.1, 0.5);
        POUCH_SCALE = b.defineInRange("pouchLength", 0.25, 0.1, 0.5);
        HANG_SCALE = b.defineInRange("hangingLength", 0.43, 0.1, 0.6);
        DYNAMIC_LIGHT = b.define("dynamicLight", true);
        HUD_X = b.comment("Horizontal distance adjustment; the same value mirrors with the main arm.")
                .defineInRange("hudOffsetX", 0, -64, 256);
        HUD_Y = b.defineInRange("hudOffsetY", 0, -256, 64);
        SPEC = b.build();
    }
}
