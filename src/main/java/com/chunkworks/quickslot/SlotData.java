/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * AF: STACK is the player's one independent carried stack; REVISION identifies its
 * last server mutation in this session. REQUEST_TICK bounds packet processing.
 * RI: owned stacks are empty or within their item limit, never aliased with inventory;
 * all server changes go through Slot; revisions are nonnegative. Only STACK persists.
 */
public final class SlotData {
    private SlotData() {}
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, QuickSlot.ID);
    public static final Supplier<AttachmentType<ItemStack>> STACK = TYPES.register("stack",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());
    public static final Supplier<AttachmentType<Long>> REVISION = TYPES.register("revision", () -> AttachmentType.builder(() -> 0L).build());
    static final Supplier<AttachmentType<Long>> REQUEST_TICK = TYPES.register("request_tick", () -> AttachmentType.builder(() -> Long.MIN_VALUE).build());

    /** effects: returns an independent snapshot; mutating it cannot alter stored inventory. */
    public static ItemStack copy(Player player) { return player.getData(STACK).copy(); }

    /** effects: returns the transient version of this player's quick slot. */
    public static long revision(Player player) { return player.getData(REVISION); }
}
