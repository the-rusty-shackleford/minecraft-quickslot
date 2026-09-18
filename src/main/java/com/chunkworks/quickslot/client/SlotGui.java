/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.QuickSlot;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.menu.QuickMenuSlot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/** Client-only presentation; vanilla menu machinery owns inventory clicks and tooltips. */
@EventBusSubscriber(modid = QuickSlot.ID, value = Dist.CLIENT)
public final class SlotGui {
    private static final ResourceLocation INVENTORY = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation SELECTED = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private SlotGui() {}

    /** effects: renders one HUD cell opposite offhand with permanent hotbar-attack clearance. */
    public static void hud(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.options.hideGui || game.player.isSpectator()) return;
        boolean right = game.player.getMainArm() == HumanoidArm.RIGHT;
        int distance = game.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? 121 : 97;
        int x = graphics.guiWidth() / 2 + (right ? distance : -distance - 22);
        int y = graphics.guiHeight() - 22;
        frame(graphics, x, y, ClientSlot.recentlySwapped());
        // Borrow read-only for this frame; do not allocate a defensive stack copy each draw.
        ItemStack stack = game.player.getData(SlotData.STACK);
        graphics.renderItem(stack, x + 3, y + 3);
        graphics.renderItemDecorations(game.font, stack, x + 3, y + 3);
    }

    /** effects: draws the approved inventory cell behind vanilla's slot/item rendering. */
    @SubscribeEvent public static void background(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen screen)) return;
        event.getGuiGraphics().blit(INVENTORY, screen.getGuiLeft() + QuickMenuSlot.X - 1,
                screen.getGuiTop() + QuickMenuSlot.Y - 1, 76, 61, 18, 18);
    }

    private static void frame(GuiGraphics graphics, int x, int y, boolean active) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blitSprite(HOTBAR, 182, 22, 0, 0, x, y, 11, 22);
        graphics.blitSprite(HOTBAR, 182, 22, 171, 0, x + 11, y, 11, 22);
        if (active) graphics.blitSprite(SELECTED, x - 1, y - 1, 24, 23);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
}
