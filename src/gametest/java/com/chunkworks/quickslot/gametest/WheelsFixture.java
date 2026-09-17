/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.vanillawheels.Vehicle;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Isolated optional vehicle fixture, loaded only in the explicit compatibility run. */
final class WheelsFixture {
    private WheelsFixture() {}
    static void mount(ServerPlayer driver, ServerPlayer passenger) {
        Vehicle car = Vehicle.create(driver.serverLevel(), ResourceLocation.parse("trailblazer:trailblazer"), new Vec3(4, 71, 4), 0);
        if (car == null) throw new IllegalStateException("Trailblazer profile missing");
        driver.serverLevel().addFreshEntity(car);
        if (!driver.startRiding(car, true)) throw new IllegalStateException("Driver could not mount");
        if (passenger != null && !passenger.startRiding(car, true)) throw new IllegalStateException("Passenger could not mount");
    }
    static void describe(Player player, JsonObject output) {
        if (player.getVehicle() instanceof Vehicle car) {
            output.addProperty("vehicle", car.getId());
            output.addProperty("lights", car.lights().name());
        }
    }
}
