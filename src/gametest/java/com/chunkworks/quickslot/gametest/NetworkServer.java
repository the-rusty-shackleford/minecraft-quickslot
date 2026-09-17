/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.QuickSlot;
import com.chunkworks.quickslot.ServerRules;
import com.chunkworks.quickslot.Slot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real dedicated-server fixtures controlled by the external integration test. */
@EventBusSubscriber(modid = "quickslot_gametest")
public final class NetworkServer {
    private NetworkServer() {}
    private static int tick;
    private static int completed;
    private static String error = "";
    private static final Set<UUID> POSITIONED = new HashSet<>();

    private static ItemStack item(JsonObject data) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.get("item").getAsString())),
                data.has("count") ? data.get("count").getAsInt() : 1);
        if (data.has("damage")) stack.setDamageValue(data.get("damage").getAsInt());
        if (data.has("name")) stack.set(DataComponents.CUSTOM_NAME, Component.literal(data.get("name").getAsString()));
        return stack;
    }

    private static void platform(ServerPlayer player, double x) {
        BlockPos origin = new BlockPos((int) x, 70, 0);
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++)
            player.serverLevel().setBlock(origin.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
        player.teleportTo(x, 71, 0);
        player.setRespawnPosition(player.level().dimension(), origin.above(), 0, true, false);
    }

    private static void execute(MinecraftServer server, JsonObject command) {
        String op = command.get("op").getAsString();
        ServerPlayer player = server.getPlayerList().getPlayerByName(command.has("target") ? command.get("target").getAsString() : "QuickDriver");
        if (op.equals("quit")) { server.halt(false); return; }
        if (player == null) throw new IllegalStateException("Target not connected");
        switch (op) {
            case "booth" -> {
                player.serverLevel().setDayTime(6000);
                player.serverLevel().setWeatherParameters(60000, 0, false, false);
                server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                platform(player, 0);
                player.teleportTo(player.serverLevel(), 0, 71, 0, 0, 0);
            }
            case "position" -> player.teleportTo(player.serverLevel(), command.get("x").getAsDouble(),
                    command.get("y").getAsDouble(), command.get("z").getAsDouble(),
                    command.get("yaw").getAsFloat(), command.get("pitch").getAsFloat());
            case "seed" -> {
                player.stopRiding();
                player.getInventory().clearContent();
                player.getInventory().selected = 0;
                player.setHealth(player.getMaxHealth());
                player.setGameMode(GameType.SURVIVAL);
                player.getFoodData().setFoodLevel(20);
                Slot.replace(player, item(command.getAsJsonObject("quick")));
                if (command.has("fireAspect") && command.get("fireAspect").getAsBoolean()) {
                    ItemStack burning = com.chunkworks.quickslot.SlotData.copy(player);
                    burning.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT), 1);
                    Slot.replace(player, burning);
                }
                if (command.has("vanishing") && command.get("vanishing").getAsBoolean()) {
                    ItemStack cursed = com.chunkworks.quickslot.SlotData.copy(player);
                    cursed.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
                    Slot.replace(player, cursed);
                }
                player.getInventory().setItem(0, item(command.getAsJsonObject("held")));
                player.inventoryMenu.broadcastChanges();
            }
            case "mode" -> player.setGameMode(GameType.byName(command.get("mode").getAsString()));
            case "keep" -> server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(command.get("value").getAsBoolean(), server);
            case "keepSlot" -> ServerRules.KEEP_ON_DEATH.set(command.get("value").getAsBoolean());
            case "kill" -> player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            case "teleport" -> {
                var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(command.get("dimension").getAsString())));
                if (level == null) throw new IllegalArgumentException("Unknown test dimension");
                double x = command.has("x") ? command.get("x").getAsDouble() : 0;
                player.teleportTo(level, x, 71, 0, 0, 0);
                platform(player, x);
            }
            case "mount" -> WheelsFixture.mount(player, command.has("passenger")
                    ? server.getPlayerList().getPlayerByName(command.get("passenger").getAsString()) : null);
            case "dismount" -> player.stopRiding();
            case "save" -> server.getPlayerList().saveAll();
            case "clearDrops" -> {
                for (var level : server.getAllLevels()) for (var entity : level.getAllEntities())
                    if (entity instanceof ItemEntity) entity.discard();
            }
            default -> throw new IllegalArgumentException("Unknown operation " + op);
        }
    }

    /** effects: applies isolated fixture commands and publishes server-side evidence. */
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!NetworkFiles.ENABLED || ++tick % 5 != 0) return;
        MinecraftServer server = event.getServer();
        try {
            if (ModList.get().isLoaded("stowed")) throw new IllegalStateException("Stowed must be absent from test profiles");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (POSITIONED.add(player.getUUID())) platform(player, player.getGameProfile().getName().equals("QuickDriver") ? 0 : 2);
            }
            JsonObject command = NetworkFiles.read("server-command");
            if (NetworkFiles.sequence(command) > completed) {
                completed = NetworkFiles.sequence(command);
                execute(server, command);
            }
        } catch (Exception failure) {
            error = failure.toString();
            LogUtils.getLogger().error("Quick Slot network fixture failed", failure);
        }
        JsonObject state = new JsonObject();
        state.addProperty("seq", completed); state.addProperty("tick", tick); state.addProperty("error", error);
        state.addProperty("stowed", ModList.get().isLoaded("stowed"));
        JsonObject players = new JsonObject();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) players.add(player.getGameProfile().getName(), NetworkFiles.player(player));
        state.add("players", players);
        JsonArray drops = new JsonArray();
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities())
            if (entity instanceof ItemEntity drop) drops.add(NetworkFiles.stack(drop.getItem()));
        state.add("drops", drops);
        try { NetworkFiles.write("server-state", state); }
        catch (Exception failure) { LogUtils.getLogger().error("Cannot write network test evidence", failure); }
    }
}
