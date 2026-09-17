/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-owned admission and retention policy; existing blocked stacks can be withdrawn. */
public final class ServerRules {
    private ServerRules() {}
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKLIST;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        KEEP_ON_DEATH = builder.comment("Retain the quick slot on death, like keepInventory.").define("keepOnDeath", false);
        BLOCKLIST = builder.comment("Item IDs forbidden from entering the quick slot. Existing stacks remain retrievable.")
                .defineListAllowEmpty("itemBlocklist", List.of(), () -> "minecraft:barrier",
                        value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        SPEC = builder.build();
    }

    /** effects: returns whether this server currently permits the incoming stack. */
    public static boolean permits(ItemStack stack) {
        return stack.isEmpty() || !BLOCKLIST.get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
