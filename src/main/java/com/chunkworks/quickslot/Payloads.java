/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Required protocol 1. Only server-to-client messages carry item data. */
public final class Payloads {
    private Payloads() {}
    private static Consumer<State> clientReceiver = state -> {};

    /** Client intent; the selected index and expected revision are untrusted. */
    public record Swap(int selected, long revision) implements CustomPacketPayload {
        public static final Type<Swap> TYPE = new Type<>(QuickSlot.id("swap"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Swap> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Swap::selected, ByteBufCodecs.VAR_LONG, Swap::revision, Swap::new);
        @Override public Type<Swap> type() { return TYPE; }
    }

    /**
     * AF: an authoritative stack snapshot for one player entity in one dimension.
     * RI: owns its stack copy; UUID and dimension disambiguate reused numeric IDs.
     */
    public record State(int entityId, UUID playerId, ResourceLocation dimension, long revision,
            ItemStack stack, boolean swapped) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(QuickSlot.id("state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::entityId, UUIDUtil.STREAM_CODEC, State::playerId,
                ResourceLocation.STREAM_CODEC, State::dimension, ByteBufCodecs.VAR_LONG, State::revision,
                ItemStack.OPTIONAL_STREAM_CODEC, State::stack, ByteBufCodecs.BOOL, State::swapped, State::new);
        public State { stack = stack.copy(); }
        @Override public ItemStack stack() { return stack.copy(); }
        @Override public Type<State> type() { return TYPE; }

        /** effects: captures the server's current attachment and entity identity. */
        public static State of(ServerPlayer player, boolean swapped) {
            return new State(player.getId(), player.getUUID(), player.level().dimension().location(),
                    SlotData.revision(player), player.getData(SlotData.STACK), swapped);
        }
    }

    /** requires: client setup; effects: installs the client adapter without a common-side client-class reference. */
    public static void receiveOnClient(Consumer<State> receiver) { clientReceiver = receiver; }

    /** effects: registers required, main-thread payload handlers on both physical sides. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Swap.TYPE, Swap.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) Slot.swap(player, packet.selected(), packet.revision());
        });
        registrar.playToClient(State.TYPE, State.CODEC, (packet, context) -> clientReceiver.accept(packet));
    }
}
