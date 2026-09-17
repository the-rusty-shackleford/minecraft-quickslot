/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;

/** Equipment visibility policy only; armor and head poses do not change item placement. */
final class EquipmentVisibility {
    private EquipmentVisibility() {}
    /** effects: reports worn elytra, which hides every quick-slot body display. */
    static boolean elytra(AbstractClientPlayer player) { return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA); }
    /** effects: reports a visible cape, respecting its skin-part toggle and elytra replacement. */
    static boolean cape(AbstractClientPlayer player) {
        return !elytra(player) && player.isModelPartShown(PlayerModelPart.CAPE) && player.getSkin().capeTexture() != null;
    }
}
