/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.Payloads;
import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.client.ClientSetup;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.ClickType;
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
                    case "inventory" -> KeyMapping.click(game.options.keyInventory.getKey());
                    case "attack" -> KeyMapping.click(game.options.keyAttack.getKey());
                    case "view" -> {
                        game.options.setCameraType(CameraType.valueOf(command.get("camera").getAsString()));
                        game.options.hideGui = command.has("hide") && command.get("hide").getAsBoolean();
                        if (command.has("fov")) game.options.fov().set(command.get("fov").getAsInt());
                        game.options.bobView().set(false);
                        if (command.has("yaw")) {
                            game.player.setYRot(command.get("yaw").getAsFloat());
                            game.player.setYHeadRot(command.get("yaw").getAsFloat());
                            game.player.setYBodyRot(command.get("yaw").getAsFloat());
                        }
                        if (command.has("pitch")) game.player.setXRot(command.get("pitch").getAsFloat());
                        if (command.has("width")) org.lwjgl.glfw.GLFW.glfwSetWindowSize(game.getWindow().getWindow(),
                                command.get("width").getAsInt(), command.get("height").getAsInt());
                    }
                    case "sneak" -> game.options.keyShift.setDown(command.get("value").getAsBoolean());
                    case "resourceReload" -> game.reloadResourcePacks();
                    case "packs" -> {
                        var packs = new java.util.ArrayList<String>();
                        packs.add("vanilla"); packs.add("mod_resources");
                        if (command.get("refined").getAsBoolean()) packs.add("file/Refined Tools 3.0");
                        game.getResourcePackRepository().setSelected(packs);
                        game.reloadResourcePacks();
                    }
                    case "walk" -> {
                        game.options.keyUp.setDown(command.get("value").getAsBoolean());
                        game.options.keySprint.setDown(command.has("sprint") && command.get("sprint").getAsBoolean());
                    }
                    case "mouse" -> {
                        if (!(game.screen instanceof AbstractContainerScreen<?> screen)) throw new IllegalStateException("No inventory screen");
                        double x = screen.getGuiLeft() + command.get("x").getAsDouble();
                        double y = screen.getGuiTop() + command.get("y").getAsDouble();
                        screen.mouseClicked(x, y, 0); screen.mouseReleased(x, y, 0);
                    }
                    case "closeGui" -> { if (game.screen != null) game.screen.onClose(); }
                    case "mouseSlot" -> {
                        if (!(game.screen instanceof AbstractContainerScreen<?> screen)) throw new IllegalStateException("No inventory screen");
                        var slot = screen.getMenu().getSlot(command.get("slot").getAsInt());
                        screen.mouseClicked(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8,
                                command.get("button").getAsInt());
                        screen.mouseReleased(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8,
                                command.get("button").getAsInt());
                    }
                    case "menuClick" -> game.gameMode.handleInventoryMouseClick(0, command.get("slot").getAsInt(),
                            command.get("button").getAsInt(), ClickType.valueOf(command.get("type").getAsString()), game.player);
                    case "hud" -> {
                        game.options.mainHand().set(HumanoidArm.valueOf(command.get("arm").getAsString()));
                        game.options.attackIndicator().set(AttackIndicatorStatus.valueOf(command.get("attack").getAsString()));
                        game.options.broadcastOptions();
                    }
                    case "capture" -> {
                        String name = command.get("name").getAsString();
                        if (!name.matches("[a-z0-9_-]+\\.png")) throw new IllegalArgumentException("Unsafe screenshot name");
                        Screenshot.grab(game.gameDirectory, name, game.getMainRenderTarget(), message -> {});
                    }
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
        state.addProperty("reloading", game.getOverlay() != null);
        if (game.screen instanceof AbstractContainerScreen<?> screen) {
            state.addProperty("guiLeft", screen.getGuiLeft()); state.addProperty("guiTop", screen.getGuiTop());
            state.addProperty("guiWidth", game.getWindow().getGuiScaledWidth());
            state.addProperty("guiHeight", game.getWindow().getGuiScaledHeight());
        }
        if (game.player != null) {
            state.addProperty("self", game.player.getGameProfile().getName());
            state.addProperty("skin", game.player.getSkin().model().toString());
        }
        JsonObject players = new JsonObject();
        if (game.level != null) for (var player : game.level.players()) players.add(player.getGameProfile().getName(), NetworkFiles.player(player));
        state.add("players", players);
        try { NetworkFiles.write(role + "-state", state); }
        catch (Exception failure) { LogUtils.getLogger().error("Cannot write client evidence", failure); }
    }
}
