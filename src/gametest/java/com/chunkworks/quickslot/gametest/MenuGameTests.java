/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.Slot;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.menu.QuickMenuSlot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Partitions through real InventoryMenu.clicked: pickup/place/split, shift both directions,
 * number-key exchange, full inventory, armor priority, prediction ownership,
 * external mutation/clone refresh, survival/creative. No fake menu backend.
 */
@GameTestHolder("quickslot")
@PrefixGameTestTemplate(false)
public final class MenuGameTests {
    public MenuGameTests() {}
    private static int cell(Player p) {
        return p.inventoryMenu.slots.stream().filter(s -> s instanceof QuickMenuSlot).findFirst().orElseThrow().index;
    }
    private static Player player(GameTestHelper h) { return h.makeMockPlayer(GameType.SURVIVAL); }

    @GameTest(template = "empty")
    public void clickSplitAndPlaceConserveItems(GameTestHelper h) {
        Player p = player(h); int cell = cell(p);
        h.assertValueEqual(cell, 46, "appended after all vanilla slots");
        p.getInventory().setItem(0, new ItemStack(Items.APPLE, 17));
        p.inventoryMenu.clicked(36, 0, ClickType.PICKUP, p);
        p.inventoryMenu.clicked(cell, 0, ClickType.PICKUP, p);
        h.assertValueEqual(SlotData.copy(p).getCount(), 17, "placed all apples");
        h.assertTrue(p.inventoryMenu.getCarried().isEmpty(), "cursor empty after placement");
        p.inventoryMenu.clicked(cell, 1, ClickType.PICKUP, p);
        h.assertValueEqual(SlotData.copy(p).getCount(), 8, "right click leaves smaller half");
        h.assertValueEqual(p.inventoryMenu.getCarried().getCount(), 9, "cursor has larger half");
        p.inventoryMenu.clicked(cell, 1, ClickType.PICKUP, p);
        h.assertValueEqual(SlotData.copy(p).getCount(), 9, "right click returns exactly one");
        h.assertValueEqual(p.inventoryMenu.getCarried().getCount(), 8, "cursor retains eight");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void shiftBothDirections(GameTestHelper h) {
        Player p = player(h); int cell = cell(p);
        p.getInventory().setItem(9, new ItemStack(Items.ENDER_PEARL, 16));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);
        h.assertValueEqual(SlotData.copy(p).getCount(), 16, "shift into empty quick slot");
        h.assertTrue(p.getInventory().getItem(9).isEmpty(), "source consumed once");
        p.inventoryMenu.clicked(cell, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(SlotData.copy(p).isEmpty(), "shift out clears attachment");
        h.assertValueEqual(p.getInventory().getItem(9).getCount(), 16, "whole stack returns to inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void hotbarNumberKeyExchangesCompleteStacks(GameTestHelper h) {
        Player p = player(h);
        Slot.replace(p, new ItemStack(Items.DIAMOND, 4));
        p.getInventory().setItem(3, new ItemStack(Items.BREAD, 12));
        p.inventoryMenu.clicked(cell(p), 3, ClickType.SWAP, p);
        h.assertTrue(SlotData.copy(p).is(Items.BREAD), "number key stores hotbar item");
        h.assertValueEqual(SlotData.copy(p).getCount(), 12, "all bread retained");
        h.assertValueEqual(p.getInventory().getItem(3).getCount(), 4, "all diamonds returned");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void fullInventoryCannotLoseShiftedStack(GameTestHelper h) {
        Player p = player(h);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        Slot.replace(p, new ItemStack(Items.APPLE, 32));
        long revision = SlotData.revision(p);
        p.inventoryMenu.clicked(cell(p), 0, ClickType.QUICK_MOVE, p);
        h.assertValueEqual(SlotData.copy(p).getCount(), 32, "failed transfer leaves source intact");
        h.assertTrue(SlotData.revision(p) == revision, "no mutation or new snapshot on failed shift");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void armorAndOffhandRetainAutoEquipPriority(GameTestHelper h) {
        Player p = player(h);
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(p.inventoryMenu.getSlot(6).getItem().is(Items.DIAMOND_CHESTPLATE), "chestplate autoequips");
        p.getInventory().setItem(9, new ItemStack(Items.SHIELD));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);
        h.assertTrue(p.getOffhandItem().is(Items.SHIELD), "shield autoequips");
        h.assertTrue(SlotData.copy(p).isEmpty(), "quick slot did not steal equipment");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void workingStackDoesNotAliasAttachment(GameTestHelper h) {
        Player p = player(h);
        Slot.replace(p, new ItemStack(Items.APPLE, 15));
        var cell = p.inventoryMenu.getSlot(cell(p));
        cell.getItem().shrink(2);
        h.assertValueEqual(SlotData.copy(p).getCount(), 15, "working view is separate");
        cell.setChanged();
        h.assertValueEqual(SlotData.copy(p).getCount(), 13, "setChanged commits in-place menu mutation");
        Slot.replace(p, new ItemStack(Items.EMERALD, 3));
        h.assertTrue(cell.getItem().is(Items.EMERALD), "external H/API changes refresh view");
        p.setData(SlotData.STACK, new ItemStack(Items.DIAMOND, 2));
        h.assertTrue(cell.getItem().is(Items.DIAMOND), "clone/load replacement refreshes even at same revision");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void creativeCellIsNotAClientOnlyCreationSlot(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.CREATIVE);
        h.assertTrue(!p.inventoryMenu.getSlot(cell(p)).isActive(), "creative wrapper cannot edit an unsupported index");
        Slot.replace(p, new ItemStack(Items.APPLE, 5));
        h.assertTrue(Slot.swap(p, 0, SlotData.revision(p)), "creative players still use authoritative H swap");
        h.assertValueEqual(p.getMainHandItem().getCount(), 5, "H neither consumes nor duplicates creative stack");
        h.succeed();
    }
}
