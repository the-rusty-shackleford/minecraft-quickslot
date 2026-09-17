/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.menu;

import com.chunkworks.quickslot.Slot;
import com.chunkworks.quickslot.SlotData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * AF: a one-cell vanilla menu view of the owner's authoritative attachment.
 * RI: the menu owns a separate mutable working stack, never the attachment's stack.
 * A revision change refreshes it; vanilla setChanged commits in-place menu mutations.
 * Client writes predict only this view; only server snapshots change client attachments.
 * Confined to the owning game thread. All indices must be zero.
 */
public final class QuickContainer implements Container {
    private final Player owner;
    private ItemStack working = ItemStack.EMPTY;
    private ItemStack observed;
    private long revision = -1;

    /** effects: creates an initially lazy menu view for owner. */
    public QuickContainer(Player owner) { this.owner = owner; }

    /** effects: returns the fixed cell count. */
    @Override public int getContainerSize() { return 1; }
    /** effects: reports whether the current view is empty. */
    @Override public boolean isEmpty() { return getItem(0).isEmpty(); }

    /** requires: index is zero. effects: returns the mutable menu working stack. */
    @Override public ItemStack getItem(int index) {
        checkIndex(index);
        long current = SlotData.revision(owner);
        ItemStack authoritative = owner.getData(SlotData.STACK);
        if (revision != current || observed != authoritative) {
            working = authoritative.copy(); revision = current; observed = authoritative;
        }
        return working;
    }

    /** requires: index is zero, amount nonnegative. effects: removes and returns up to amount. */
    @Override public ItemStack removeItem(int index, int amount) {
        ItemStack removed = getItem(index).split(Math.max(0, amount));
        setChanged();
        return removed;
    }

    /** requires: index is zero. effects: removes the whole stack; persistence/sync still occur. */
    @Override public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = getItem(index);
        setItem(index, ItemStack.EMPTY);
        return removed;
    }

    /** requires: index zero, legal menu-owned stack. effects: takes an independent copy and commits. */
    @Override public void setItem(int index, ItemStack stack) {
        checkIndex(index);
        working = stack.copy();
        revision = SlotData.revision(owner);
        observed = owner.getData(SlotData.STACK);
        setChanged();
    }

    /** effects: commits a vanilla menu mutation on the server; prediction stays client-local. */
    @Override public void setChanged() {
        if (!owner.level().isClientSide) {
            Slot.replaceFromMenu(owner, working);
            revision = SlotData.revision(owner);
            observed = owner.getData(SlotData.STACK);
        }
    }

    /** effects: permits only the living, nonspectating owner to interact. */
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator();
    }
    /** effects: empties the menu and its server attachment. */
    @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }

    private static void checkIndex(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
    }
}
