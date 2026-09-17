/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.ClientRules;
import com.chunkworks.quickslot.QuickSlot;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reload-derived armor offsets. Actual ArmorItem identity prevents chest-slot packs acting as plate. */
final class EquipmentClearance {
    private static final TagKey<Item> BULKY = TagKey.create(Registries.ITEM, QuickSlot.id("bulky_armor"));
    private static Map<ResourceLocation, Double> overrides = Map.of();
    private EquipmentClearance() {}
    static void reload() {
        Map<ResourceLocation, Double> next = new HashMap<>();
        for (String line : ClientRules.ARMOR_OFFSETS.get()) {
            String[] parts = line.split("=", 2);
            next.put(ResourceLocation.parse(parts[0]), Math.min(1, Double.parseDouble(parts[1])));
        }
        overrides = Map.copyOf(next);
    }
    static double armor(ItemStack stack, EquipmentSlot slot) {
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) return 0;
        double vanilla = slot == EquipmentSlot.LEGS ? ClientRules.LEGS_OFFSET.get() : ClientRules.CHEST_OFFSET.get();
        return vanilla + overrides.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()),
                stack.is(BULKY) ? ClientRules.BULKY_OFFSET.get() : 0);
    }
    static boolean elytra(AbstractClientPlayer player) { return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA); }
    static boolean cape(AbstractClientPlayer player) {
        return !elytra(player) && player.isModelPartShown(PlayerModelPart.CAPE) && player.getSkin().capeTexture() != null;
    }
}
