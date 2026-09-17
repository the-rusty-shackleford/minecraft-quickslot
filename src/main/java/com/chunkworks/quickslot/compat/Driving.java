/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.compat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/** Optional driving context; the core class never imports a vehicle-mod type. */
public final class Driving {
    private Driving() {}
    /** effects: returns true only for the controlling driver of a Vanilla Wheels vehicle. */
    public static boolean isDriver(Player player) {
        return player != null && ModList.get().isLoaded("vanillawheels") && WheelsDriving.isDriver(player);
    }
}
