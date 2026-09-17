/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.menu;

import com.chunkworks.quickslot.ServerRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One real menu cell; admission is independent of later body-render classification. */
public final class QuickMenuSlot extends Slot {
    public static final int X = 77;
    public static final int Y = 42;
    private final Player owner;

    /** effects: creates the approved cell immediately above offhand. */
    public QuickMenuSlot(Player owner) {
        super(new QuickContainer(owner), 0, X, Y);
        this.owner = owner;
    }

    /** effects: validates admission against current server policy and player eligibility. */
    @Override public boolean mayPlace(ItemStack stack) {
        return container.stillValid(owner) && isActive() && ServerRules.permits(stack);
    }
    /** effects: lets the eligible owner withdraw even an item newly added to the blocklist. */
    @Override public boolean mayPickup(Player player) { return container.stillValid(player) && isActive(); }

    /**
     * effects: disables creative-menu editing, whose vanilla stack-creation protocol is
     * limited to indices 1..45. H still works in creative. This prevents phantom/duplicate
     * stacks from the creative screen's client-only wrapper of this appended cell.
     */
    @Override public boolean isActive() { return !owner.isCreative(); }
}
