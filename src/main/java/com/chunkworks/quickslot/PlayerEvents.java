/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import com.chunkworks.quickslot.domain.SlotRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Lifecycle boundaries use normal drops and defensive attachment copies, never respawn grants. */
@EventBusSubscriber(modid = QuickSlot.ID)
public final class PlayerEvents {
    private PlayerEvents() {}

    /** effects: includes the quick slot in the normal, cancellable death-drop collection. */
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        ItemStack stack = SlotData.copy(player);
        if (stack.isEmpty()) return;
        var fate = SlotRules.onDeath(player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                ServerRules.KEEP_ON_DEATH.get(), EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP));
        if (fate == SlotRules.Death.KEEP) return;
        Slot.replace(player, ItemStack.EMPTY);
        if (fate == SlotRules.Death.DROP) {
            // Use the same constructor path, scatter and pickup delay as inventory.
            // Capture ServerPlayer.drop so it cannot bypass cancellation/grave consumers.
            var previousCapture = player.captureDrops(event.getDrops());
            try {
                ItemEntity dropped = player.drop(stack, true, false);
                if (dropped != null && !event.getDrops().contains(dropped)) event.getDrops().add(dropped);
            } finally {
                player.captureDrops(previousCapture);
            }
        }
    }

    /** effects: copies retained state once, also covering the End-return clone path. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player next = event.getEntity();
        if (next.level().isClientSide) return;
        Player previous = event.getOriginal();
        boolean retained = !event.isWasDeath() || next.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                || ServerRules.KEEP_ON_DEATH.get();
        next.setData(SlotData.STACK, retained ? SlotData.copy(previous) : ItemStack.EMPTY);
        next.setData(SlotData.REVISION, Math.incrementExact(SlotData.revision(previous)));
    }

    /** effects: makes the logged-in owner's persisted slot visible to client and observers. */
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { Slot.broadcast(event.getEntity(), false); }
    /** effects: refreshes the replacement player entity after respawn. */
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { Slot.broadcast(event.getEntity(), false); }
    /** effects: refreshes clients after the player's level changes. */
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { Slot.broadcast(event.getEntity(), false); }
    /** effects: sends the target's current stack when an observer enters tracking range. */
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer owner && event.getEntity() instanceof ServerPlayer observer) Slot.syncTo(owner, observer);
    }
}
