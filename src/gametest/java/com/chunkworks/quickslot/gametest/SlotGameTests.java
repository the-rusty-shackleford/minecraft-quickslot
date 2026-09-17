/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.Slot;
import com.chunkworks.quickslot.SlotData;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Partitions: empty/full stacks, component-bearing equipment, defensive ownership;
 * save/load; ordinary/Vanishing death with retention off/on; clone death/End return;
 * invalid/stale intent and active use; empty/populated network codecs; overfull input.
 * These are real-server inventory/lifecycle tests with Minecraft's test-player fixture.
 * They do not substitute for the separate dedicated-server/two-real-client network gate.
 */
@GameTestHolder("quickslot")
@PrefixGameTestTemplate(false)
public final class SlotGameTests {
    public SlotGameTests() {}

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2, 2, 2));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static ItemStack sword() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(57);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Belongs to the player"));
        return sword;
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public void swapConservesBothCompleteStacks(GameTestHelper h) {
        Player p = player(h);
        ItemStack food = new ItemStack(Items.APPLE, 64);
        ItemStack tool = sword();
        Slot.replace(p, food);
        p.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
        h.assertTrue(Slot.swap(p, 0, SlotData.revision(p)), "valid swap succeeds");
        h.assertTrue(ItemStack.matches(SlotData.copy(p), tool), "named damaged sword stored intact");
        h.assertTrue(ItemStack.matches(p.getMainHandItem(), food), "all 64 apples moved into hand");
        h.runAfterDelay(1, () -> {
            h.assertTrue(Slot.swap(p, 0, SlotData.revision(p)), "second press swaps back");
            h.assertTrue(ItemStack.matches(p.getMainHandItem(), tool), "same sword returned");
            h.assertTrue(ItemStack.matches(SlotData.copy(p), food), "same food stack returned");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public void emptySlotAndDefensiveCopies(GameTestHelper h) {
        Player p = player(h);
        h.assertTrue(SlotData.copy(p).isEmpty(), "new player starts empty");
        ItemStack original = new ItemStack(Items.DIAMOND, 7);
        Slot.replace(p, original);
        original.shrink(6);
        ItemStack exposed = SlotData.copy(p);
        exposed.shrink(5);
        h.assertValueEqual(SlotData.copy(p).getCount(), 7, "caller mutations cannot consume stored items");
        h.assertTrue(Slot.swap(p, 0, SlotData.revision(p)), "empty hand withdraws the slot");
        h.assertTrue(SlotData.copy(p).isEmpty(), "slot cleared immediately");
        h.assertValueEqual(p.getMainHandItem().getCount(), 7, "exactly seven diamonds withdrawn");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void attachmentSurvivesPlayerSaveLoad(GameTestHelper h) {
        Player original = player(h);
        Slot.replace(original, sword());
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        Player loaded = player(h);
        loaded.load(saved);
        h.assertTrue(ItemStack.matches(SlotData.copy(original), SlotData.copy(loaded)), "real player NBT preserves the entire stack");
        Slot.replace(original, ItemStack.EMPTY);
        h.assertTrue(!SlotData.copy(loaded).isEmpty(), "loaded player owns its own stack");
        h.succeed();
    }

    private static void death(GameTestHelper h, boolean keep, boolean vanishing) {
        Player p = player(h);
        ItemStack item = sword();
        if (vanishing) item.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.VANISHING_CURSE), 1);
        Slot.replace(p, item);
        var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean old = rule.get();
        try {
            rule.set(keep, h.getLevel().getServer());
            p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
            h.assertTrue(!p.isAlive(), "actual death path ran");
            // Vanilla drops start near eye height; the dead player's box has already shrunk.
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(2),
                    drop -> drop.getItem().is(Items.DIAMOND_SWORD));
            h.assertValueEqual(drops.size(), keep || vanishing ? 0 : 1, "one ordinary drop, none retained/vanished");
            if (!drops.isEmpty()) {
                h.assertTrue(ItemStack.matches(drops.getFirst().getItem(), item), "drop retains all components");
                ItemEntity ordinaryInventoryDrop = p.drop(new ItemStack(Items.STICK), true, false);
                h.assertTrue(ordinaryInventoryDrop != null, "vanilla constructs an ordinary inventory drop");
                short expectedDelay = ordinaryInventoryDrop.saveWithoutId(new CompoundTag()).getShort("PickupDelay");
                short actualDelay = drops.getFirst().saveWithoutId(new CompoundTag()).getShort("PickupDelay");
                h.assertValueEqual(actualDelay, expectedDelay, "quick-slot pickup delay matches ordinary inventory");
            }
            Player clone = player(h);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, p, true));
            h.assertTrue(keep ? ItemStack.matches(SlotData.copy(clone), item) : SlotData.copy(clone).isEmpty(),
                    "death clone retains exactly the permitted stack");
            drops.forEach(ItemEntity::discard);
        } finally {
            rule.set(old, h.getLevel().getServer());
        }
        h.succeed();
    }

    @GameTest(template = "empty") public void ordinaryDeathDropsOnce(GameTestHelper h) { death(h, false, false); }
    @GameTest(template = "empty") public void vanishingDeathDropsNothing(GameTestHelper h) { death(h, false, true); }
    @GameTest(template = "empty") public void keepInventoryRetainsOrdinaryStack(GameTestHelper h) { death(h, true, false); }
    @GameTest(template = "empty") public void keepInventoryRetainsVanishingLikeVanilla(GameTestHelper h) { death(h, true, true); }

    @GameTest(template = "empty")
    public void endCloneDoesNotDuplicateOrAlias(GameTestHelper h) {
        Player original = player(h);
        Slot.replace(original, new ItemStack(Items.ENDER_PEARL, 16));
        Player next = player(h);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next, original, false));
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next, original, false));
        h.assertValueEqual(SlotData.copy(next).getCount(), 16, "copy replaces; it never adds stacks");
        Slot.replace(original, ItemStack.EMPTY);
        h.assertValueEqual(SlotData.copy(next).getCount(), 16, "clone does not alias original");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public void staleIntentCannotWithdrawChangedStack(GameTestHelper h) {
        Player p = player(h);
        Slot.replace(p, new ItemStack(Items.DIAMOND, 4));
        long old = SlotData.revision(p);
        Slot.replace(p, new ItemStack(Items.EMERALD, 9));
        h.assertTrue(!Slot.swap(p, 0, old), "stale revision rejected");
        h.assertTrue(p.getMainHandItem().isEmpty(), "rejection leaves hand alone");
        h.runAfterDelay(1, () -> {
            h.assertTrue(!Slot.swap(p, 9, SlotData.revision(p)), "out-of-range index rejected");
            h.assertTrue(SlotData.copy(p).is(Items.EMERALD) && SlotData.copy(p).getCount() == 9, "stored stack conserved");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public void drinkingCannotRaceASwap(GameTestHelper h) {
        Player p = player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        p.startUsingItem(InteractionHand.MAIN_HAND);
        h.assertTrue(p.isUsingItem(), "player is using the bucket");
        h.assertTrue(!Slot.swap(p, 0, 0), "using item blocks swap");
        h.assertTrue(p.getMainHandItem().is(Items.MILK_BUCKET) && SlotData.copy(p).isEmpty(), "no item duplicated or consumed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void codecPreservesEmptyAndComponentStacks(GameTestHelper h) {
        Player p = player(h);
        for (ItemStack stack : new ItemStack[] {ItemStack.EMPTY, sword(), new ItemStack(Items.APPLE, 64)}) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                var before = new Payloads.State(p.getId(), p.getUUID(), p.level().dimension().location(), 41, stack, true);
                Payloads.State.CODEC.encode(buffer, before);
                var after = Payloads.State.CODEC.decode(buffer);
                h.assertTrue(ItemStack.matches(before.stack(), after.stack()), "optional stack stream codec is lossless");
                h.assertTrue(after.playerId().equals(p.getUUID()) && after.revision() == 41 && after.swapped(), "identity and revision preserved");
                h.assertValueEqual(buffer.readableBytes(), 0, "codec consumes one complete payload");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void overfullStackCannotEnter(GameTestHelper h) {
        Player p = player(h);
        boolean refused = false;
        try { Slot.replace(p, new ItemStack(Items.DIAMOND_SWORD, 2)); }
        catch (IllegalArgumentException expected) { refused = true; }
        h.assertTrue(refused && SlotData.copy(p).isEmpty(), "rejects overfull equipment without changing inventory");
        h.succeed();
    }
}
