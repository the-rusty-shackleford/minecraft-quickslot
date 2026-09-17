/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import com.chunkworks.quickslot.domain.SlotRules;
import com.chunkworks.quickslot.compat.Driving;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Atomic mutations of the one stored stack. Runs only on the authoritative game thread. */
public final class Slot {
    private Slot() {}

    /**
     * requires: called on the server game thread with a server-owned stack.
     * effects: replaces the attachment with a defensive copy, syncing only a real change.
     * throws: IllegalArgumentException for an overfull or server-blocked stack;
     * IllegalStateException on a client. Empty is always allowed so removal remains possible.
     */
    public static boolean replace(Player player, ItemStack stack) {
        if (player.level().isClientSide) throw new IllegalStateException("Server-owned inventory");
        if (!valid(stack) || !ServerRules.permits(stack)) throw new IllegalArgumentException("Quick slot rejects this stack");
        if (ItemStack.matches(player.getData(SlotData.STACK), stack)) return false;
        store(player, stack);
        broadcast(player, false);
        return true;
    }

    /**
     * requires: server game thread. effects: swaps the two server-owned stacks once
     * if the request remains valid; otherwise leaves both untouched. No item data is
     * accepted from the requester. A packet may be considered at most once per tick.
     */
    public static boolean swap(Player player, int selected, long expectedRevision) {
        if (player.level().isClientSide) return false;
        long now = player.level().getGameTime();
        if (player.getData(SlotData.REQUEST_TICK) == now) return false;
        player.setData(SlotData.REQUEST_TICK, now);
        ItemStack held = player.getMainHandItem();
        ItemStack stored = player.getData(SlotData.STACK);
        boolean busy = player.isUsingItem() || player.containerMenu != player.inventoryMenu || Driving.isDriver(player);
        if (!SlotRules.maySwap(selected, player.getInventory().selected, expectedRevision, SlotData.revision(player),
                player.isAlive(), player.isSpectator(), busy, !ServerRules.permits(held)) || !valid(held) || !valid(stored)) {
            if (player instanceof ServerPlayer server) syncTo(server, server);
            return false;
        }
        if (ItemStack.matches(held, stored)) return false;
        ItemStack previous = stored.copy();
        store(player, held);
        player.getInventory().setItem(selected, previous);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        broadcast(player, true);
        return true;
    }

    private static boolean valid(ItemStack stack) {
        return stack.isEmpty() || stack.getCount() > 0 && stack.getCount() <= stack.getMaxStackSize();
    }

    private static void store(Player player, ItemStack stack) {
        player.setData(SlotData.STACK, stack.copy());
        player.setData(SlotData.REVISION, Math.incrementExact(SlotData.revision(player)));
    }

    /** effects: sends current state to the owner and existing observers; no mutation. */
    public static void broadcast(Player player, boolean swapped) {
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(server, Payloads.State.of(server, swapped));
        }
    }

    /** effects: sends one tracking snapshot to a newly interested observer. */
    public static void syncTo(ServerPlayer owner, ServerPlayer observer) {
        PacketDistributor.sendToPlayer(observer, Payloads.State.of(owner, false));
    }
}
