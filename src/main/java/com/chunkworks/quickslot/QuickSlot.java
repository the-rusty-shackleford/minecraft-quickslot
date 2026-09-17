/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** Composition root for the common-side storage and required network protocol. */
@Mod(QuickSlot.ID)
public final class QuickSlot {
    public static final String ID = "quickslot";

    /** effects: registers common types and server rules without loading client classes. */
    public QuickSlot(IEventBus bus, ModContainer container) {
        SlotData.TYPES.register(bus);
        bus.addListener(Payloads::register);
        container.registerConfig(ModConfig.Type.SERVER, ServerRules.SPEC);
    }

    /** effects: returns a resource name in this mod's namespace. */
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
