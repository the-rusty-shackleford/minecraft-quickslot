/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.QuickSlot;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.compat.Driving;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * AF: PENDING holds recent snapshots whose player has not spawned locally yet.
 * RI: at most 256 pending players, each expiring after 100 client ticks; applied data
 * must match dimension, UUID and entity ID. All fields are client-game-thread confined.
 */
@EventBusSubscriber(modid = QuickSlot.ID, value = Dist.CLIENT)
public final class ClientSlot {
    private ClientSlot() {}
    private record Pending(Payloads.State state, long expires) {}
    private static final Map<UUID, Pending> PENDING = new LinkedHashMap<>();
    private static long ticks;
    private static long lastSwap = Long.MIN_VALUE;
    private static boolean checkedKeys;

    /** effects: applies a snapshot or retains it briefly until the tracked player appears. */
    public static void receive(Payloads.State state) {
        if (state.revision() < 0) return;
        if (apply(state)) { PENDING.remove(state.playerId()); return; }
        Pending previous = PENDING.get(state.playerId());
        if (previous == null || previous.state().revision() <= state.revision()) {
            PENDING.put(state.playerId(), new Pending(state, ticks + 100));
        }
        if (PENDING.size() > 256) PENDING.remove(PENDING.keySet().iterator().next());
    }

    private static boolean apply(Payloads.State state) {
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || !game.level.dimension().location().equals(state.dimension())) return false;
        Entity entity = game.level.getEntity(state.entityId());
        if (!(entity instanceof Player player) || !player.getUUID().equals(state.playerId())) return false;
        long previous = player.hasData(SlotData.STACK) ? SlotData.revision(player) : -1;
        if (previous > state.revision()) return true;
        player.setData(SlotData.STACK, state.stack());
        player.setData(SlotData.REVISION, state.revision());
        if (player == game.player && state.swapped() && previous < state.revision()) {
            lastSwap = ticks;
            // Local feedback only: intentionally very quiet, no world broadcast.
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.045f, 1.15f);
        }
        return true;
    }

    /** effects: returns whether a successful authoritative swap is within its short HUD pulse. */
    public static boolean recentlySwapped() { return lastSwap != Long.MIN_VALUE && ticks - lastSwap < 6; }

    /** effects: retries pending spawn snapshots and sends key intents, never predicts inventory locally. */
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        ticks++;
        Minecraft game = Minecraft.getInstance();
        if (!checkedKeys && game.player != null) {
            checkedKeys = true;
            for (KeyMapping key : game.options.keyMappings) {
                if (key != ClientSetup.SWAP && key.same(ClientSetup.SWAP)
                        && !key.getName().equals("key.vanillawheels.lights")) {
                    LogUtils.getLogger().warn("Quick Slot key conflicts with {}; rebind it in Controls", key.getName());
                }
            }
        }
        if (!PENDING.isEmpty()) {
            Iterator<Pending> it = PENDING.values().iterator();
            while (it.hasNext()) {
                Pending pending = it.next();
                if (pending.expires() < ticks || apply(pending.state())) it.remove();
            }
        }
        while (ClientSetup.SWAP.consumeClick()) {
            if (game.player != null && game.screen == null && game.player.isAlive()
                    && !game.player.isSpectator() && !game.player.isUsingItem() && !Driving.isDriver(game.player)) {
                PacketDistributor.sendToServer(new Payloads.Swap(game.player.getInventory().selected, SlotData.revision(game.player)));
            }
        }
    }

    /** effects: clears all session-specific state on disconnect. */
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PENDING.clear(); ticks = 0; lastSwap = Long.MIN_VALUE; checkedKeys = false;
    }
}
