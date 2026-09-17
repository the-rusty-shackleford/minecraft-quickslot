/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.compat;

import com.chunkworks.vanillawheels.Vehicle;
import net.minecraft.world.entity.player.Player;

/** Loaded only after the Vanilla Wheels presence check. */
final class WheelsDriving {
    private WheelsDriving() {}
    static boolean isDriver(Player player) {
        return player.getVehicle() instanceof Vehicle vehicle && vehicle.getControllingPassenger() == player;
    }
}
