/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.QuickSlot;
import com.chunkworks.quickslot.compat.Driving;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** H belongs to Quick Slot on foot and to vehicle lights while driving. */
@EventBusSubscriber(modid = QuickSlot.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}
    private static final IKeyConflictContext ON_FOOT = new IKeyConflictContext() {
        @Override public boolean isActive() {
            return KeyConflictContext.IN_GAME.isActive() && !Driving.isDriver(Minecraft.getInstance().player);
        }
        @Override public boolean conflicts(IKeyConflictContext other) {
            return other == this || other == KeyConflictContext.IN_GAME;
        }
    };
    public static final KeyMapping SWAP = new KeyMapping("key.quickslot.swap", ON_FOOT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.quickslot");

    /** effects: exposes the configurable swap key in Controls. */
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(SWAP); }
    /** effects: draws Quick Slot immediately above the ordinary hotbar layer. */
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, QuickSlot.id("slot"), SlotGui::hud);
    }
    /** effects: installs the state receiver once the client mod is initialized. */
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> Payloads.receiveOnClient(ClientSlot::receive));
    }
}
