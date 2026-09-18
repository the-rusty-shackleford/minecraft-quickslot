/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.ClientRules;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.luminance.LuminanceConfig;
import com.chunkworks.luminance.api.Luminance;
import com.chunkworks.luminance.client.Providers;
import com.chunkworks.luminance.domain.Point;
import com.chunkworks.luminance.domain.Source;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * AF: the authoritative synced quick slot contributes its item's normal held light.
 * RI: optional client-only adapter; dry/wet luminance is cached per player/stack/revision,
 * invalidated on resource reload and logout. Never changes stacks or real world light.
 * Luminance combines co-located held/mount/quick-slot sources by maximum, never addition.
 * Real-client provider, terrain, shader and absence checks are required acceptance gates.
 */
final class LuminanceCompat {
    private LuminanceCompat() {}
    private record Levels(long revision, ItemStack stack, int dry, int wet) {}
    private static final Map<Player, Levels> CACHE = new WeakHashMap<>();

    /** requires: client setup with Luminance 1.1+. effects: registers one player provider. */
    static void register() {
        Luminance.forEntityInterpolated(EntityType.PLAYER, LuminanceCompat::sources);
    }

    /** effects: discards luminance derived from previous item definitions or a prior session. */
    static void clear() { CACHE.clear(); }

    private static int luminance(Player player, boolean wet) {
        if (!player.hasData(SlotData.STACK)) return 0;
        long revision = SlotData.revision(player);
        var stack = player.getData(SlotData.STACK);
        Levels levels = CACHE.get(player);
        // Initial login sync can replace the default EMPTY stack at revision zero.
        // The authoritative snapshot identity must invalidate that early empty cache.
        if (levels == null || levels.revision() != revision || levels.stack() != stack) {
            levels = new Levels(revision, stack, Providers.luminanceOf(stack, false),
                    Providers.luminanceOf(stack, true));
            CACHE.put(player, levels);
        }
        return wet ? levels.wet() : levels.dry();
    }

    private static List<? extends Source> sources(Player player, Float partial) {
        if (player.isSpectator() || !ClientRules.DYNAMIC_LIGHT.get()
                || !LuminanceConfig.HELD_ITEMS.get()) return List.of();
        boolean wet = player.isUnderWater();
        int light = luminance(player, wet);
        int held = Math.max(Providers.luminanceOf(player.getMainHandItem(), wet),
                Providers.luminanceOf(player.getOffhandItem(), wet));
        if (light <= held) return List.of();
        return List.of(new Point(Mth.lerp(partial, player.xOld, player.getX()),
                Mth.lerp(partial, player.yOld, player.getY()) + player.getEyeHeight() - 0.2,
                Mth.lerp(partial, player.zOld, player.getZ()), light));
    }
}
