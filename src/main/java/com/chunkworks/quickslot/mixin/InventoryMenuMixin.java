/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.mixin;

import com.chunkworks.quickslot.menu.QuickMenuSlot;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Append-only menu extension: vanilla slot indices and ordinary transfer branches survive. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    @Unique private QuickMenuSlot quickslot$cell;
    protected InventoryMenuMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void quickslot$append(Inventory inventory, boolean active, Player owner, CallbackInfo callback) {
        quickslot$cell = new QuickMenuSlot(owner);
        addSlot(quickslot$cell);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickslot$transfer(Player player, int index, CallbackInfoReturnable<ItemStack> result) {
        if (index < 0 || index >= slots.size()) { result.setReturnValue(ItemStack.EMPTY); return; }
        Slot source = slots.get(index);
        if (source == quickslot$cell) {
            if (!source.mayPickup(player)) { result.setReturnValue(ItemStack.EMPTY); return; }
            ItemStack remaining = source.getItem();
            ItemStack original = remaining.copy();
            if (!moveItemStackTo(remaining, 9, 45, false)) { result.setReturnValue(ItemStack.EMPTY); return; }
            source.setByPlayer(remaining, original);
            source.onTake(player, remaining);
            result.setReturnValue(original);
            return;
        }
        if (index < 9 || index >= 45 || !source.hasItem() || !source.mayPickup(player)
                || quickslot$cell.hasItem() || !quickslot$cell.mayPlace(source.getItem())) return;
        // Preserve vanilla auto-equipping before considering the new empty cell.
        EquipmentSlot equipment = player.getEquipmentSlotForItem(source.getItem());
        if (equipment.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                && !slots.get(8 - equipment.getIndex()).hasItem()) return;
        if (equipment == EquipmentSlot.OFFHAND && !slots.get(45).hasItem()) return;
        ItemStack remaining = source.getItem();
        ItemStack original = remaining.copy();
        if (!moveItemStackTo(remaining, quickslot$cell.index, quickslot$cell.index + 1, false)) return;
        source.setByPlayer(remaining, original);
        source.onTake(player, remaining);
        result.setReturnValue(original);
    }
}
