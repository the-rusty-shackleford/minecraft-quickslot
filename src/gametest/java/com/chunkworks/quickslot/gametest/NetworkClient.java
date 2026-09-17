/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.client.ClientSetup;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Real client automation: normal connect, bound-key clicks, network requests and respawn. */
@EventBusSubscriber(modid = "quickslot_gametest", value = Dist.CLIENT)
public final class NetworkClient {
    private NetworkClient() {}
    private static int ticks;
    private static int completed;
    private static boolean connect = true;
    private static String error = "";

    /** effects: drives this test client without changing production slot state locally. */
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!NetworkFiles.ENABLED || ++ticks % 5 != 0) return;
        String role = System.getProperty("quickslot.testRole");
        Minecraft game = Minecraft.getInstance();
        try {
            if (ModList.get().isLoaded("stowed")) throw new IllegalStateException("Stowed must be absent from test profiles");
            JsonObject command = NetworkFiles.read(role + "-command");
            if (NetworkFiles.sequence(command) > completed) {
                completed = NetworkFiles.sequence(command);
                switch (command.get("op").getAsString()) {
                    case "key" -> KeyMapping.click(ClientSetup.SWAP.getKey());
                    case "request" -> PacketDistributor.sendToServer(new Payloads.Swap(command.get("selected").getAsInt(),
                            command.has("revision") ? command.get("revision").getAsLong() : SlotData.revision(game.player)));
                    case "disconnect" -> { connect = false; game.disconnect(new TitleScreen()); }
                    case "join" -> connect = true;
                    case "respawn" -> game.player.respawn();
                    case "quit" -> { connect = false; game.stop(); }
                    default -> throw new IllegalArgumentException("Unknown client operation");
                }
            }
            if (connect && game.screen instanceof TitleScreen && game.getOverlay() == null) {
                String address = "127.0.0.1:" + System.getProperty("quickslot.testPort");
                ConnectScreen.startConnecting(new TitleScreen(), game, ServerAddress.parseString(address),
                        new ServerData("Quick Slot isolated test", address, ServerData.Type.OTHER), false, null);
            }
        } catch (Exception failure) {
            error = failure.toString(); LogUtils.getLogger().error("Quick Slot client test failed", failure);
        }
        JsonObject state = new JsonObject();
        state.addProperty("seq", completed); state.addProperty("tick", ticks); state.addProperty("error", error);
        state.addProperty("stowed", ModList.get().isLoaded("stowed"));
        state.addProperty("connected", game.player != null && game.getConnection() != null);
        state.addProperty("screen", game.screen == null ? "none" : game.screen.getClass().getSimpleName());
        if (game.player != null) state.addProperty("self", game.player.getGameProfile().getName());
        JsonObject players = new JsonObject();
        if (game.level != null) for (var player : game.level.players()) players.add(player.getGameProfile().getName(), NetworkFiles.player(player));
        state.add("players", players);
        try { NetworkFiles.write(role + "-state", state); }
        catch (Exception failure) { LogUtils.getLogger().error("Cannot write client evidence", failure); }
    }
}
