/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.QuickSlot;
import com.chunkworks.quickslot.domain.Presentation;
import com.chunkworks.quickslot.domain.Presentation.Anchor;
import com.chunkworks.quickslot.domain.Presentation.Style;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.Tags;

/**
 * AF: CACHE maps item/component combinations to their tag-derived body classification.
 * RI: bounded to 2048 entries, game-thread confined, cleared when tags/resources change.
 * Client only: item model/style correctness is gated by the real-client booth.
 */
public final class ItemPresentation {
    private ItemPresentation() {}
    private record Key(Item item, DataComponentPatch components) {}
    private static final Map<Key, Presentation.Kind> CACHE = new HashMap<>();
    private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, QuickSlot.id(name)); }
    private static final TagKey<Item> BACK = tag("render/back"), BELT = tag("render/belt"), HIDDEN = tag("render/hidden");
    private static final TagKey<Item> HIP = tag("belt_anchor/hip");
    private static final TagKey<Item> BOTTLE = tag("belt_style/bottle"), POUCH = tag("belt_style/pouch"),
            BOWL = tag("belt_style/bowl"), HANG = tag("belt_style/hang");

    /** effects: forgets reload-derived classifications. */
    public static void clear() { CACHE.clear(); }
    /** effects: returns cached classification; components can change food, use and remainder behavior. */
    public static Presentation.Kind of(ItemStack stack) {
        Key key = new Key(stack.getItem(), stack.getComponentsPatch());
        if (CACHE.size() >= 2048) CACHE.clear();
        return CACHE.computeIfAbsent(key, ignored -> resolve(stack));
    }
    private static Presentation.Kind resolve(ItemStack stack) {
        Anchor anchor = stack.is(HIDDEN) ? Anchor.HIDDEN : stack.is(BACK) ? Anchor.BACK
                : stack.is(BELT) ? Anchor.LOWER_BACK : null;
        Style style = stack.is(BOTTLE) ? Style.BOTTLE : stack.is(POUCH) ? Style.POUCH
                : stack.is(BOWL) ? Style.BOWL : stack.is(HANG) ? Style.HANG : null;
        UseAnim animation = stack.getUseAnimation();
        boolean consumable = stack.has(DataComponents.FOOD) || stack.getItem() instanceof PotionItem
                || animation == UseAnim.EAT || animation == UseAnim.DRINK
                || stack.is(Tags.Items.FOODS) || stack.is(Tags.Items.DRINKS);
        var food = stack.get(DataComponents.FOOD);
        boolean bowl = stack.getCraftingRemainingItem().is(Items.BOWL)
                || food != null && food.usingConvertsTo().filter(s -> s.is(Items.BOWL)).isPresent();
        Equipable equippable = Equipable.get(stack);
        boolean armor = stack.is(Tags.Items.ARMORS) || equippable != null && equippable.getEquipmentSlot().isArmor()
                || stack.getItem() instanceof AnimalArmorItem || stack.is(Items.ELYTRA);
        boolean large = stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(Tags.Items.TOOLS_MACE)
                || stack.is(Tags.Items.TOOLS_SHIELD) || stack.is(Tags.Items.TOOLS_BOW)
                || stack.is(Tags.Items.TOOLS_CROSSBOW) || stack.is(Tags.Items.TOOLS_SPEAR)
                || stack.is(Tags.Items.MELEE_WEAPON_TOOLS) || stack.is(Tags.Items.RANGED_WEAPON_TOOLS)
                || stack.is(Tags.Items.MINING_TOOL_TOOLS);
        boolean hip = stack.is(HIP) || stack.is(Tags.Items.TOOLS_SHEAR) || stack.is(Tags.Items.TOOLS_BRUSH)
                || stack.is(Tags.Items.TOOLS_IGNITER) || stack.is(Tags.Items.TOOLS_WRENCH);
        return Presentation.classify(new Presentation.Facts(anchor, style, consumable,
                stack.getItem() instanceof ItemNameBlockItem, stack.getItem() instanceof BlockItem,
                armor, large, hip, animation == UseAnim.DRINK || stack.getItem() instanceof PotionItem,
                bowl, stack.is(Tags.Items.BUCKETS) || stack.getItem() instanceof BucketItem));
    }
}
