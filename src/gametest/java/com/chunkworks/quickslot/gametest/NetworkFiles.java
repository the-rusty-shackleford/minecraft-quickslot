/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.compat.Driving;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Test-only control/evidence files. Production packets still carry every inventory change. */
final class NetworkFiles {
    private NetworkFiles() {}
    static final boolean ENABLED = Boolean.getBoolean("quickslot.networkTest");
    private static final Gson JSON = new Gson();
    private static Path file(String name) { return Path.of(System.getProperty("quickslot.testDir"), name + ".json"); }

    static JsonObject read(String name) throws IOException {
        Path path = file(name);
        return Files.exists(path) ? JsonParser.parseString(Files.readString(path)).getAsJsonObject() : new JsonObject();
    }
    static void write(String name, JsonObject value) throws IOException {
        Path path = file(name);
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, JSON.toJson(value));
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    static JsonObject stack(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());
        json.addProperty("damage", stack.getDamageValue());
        json.addProperty("name", stack.getHoverName().getString());
        json.addProperty("enchanted", stack.isEnchanted());
        return json;
    }
    static JsonObject player(Player player) {
        JsonObject json = new JsonObject();
        json.addProperty("id", player.getId());
        json.addProperty("uuid", player.getUUID().toString());
        json.addProperty("dimension", player.level().dimension().location().toString());
        json.addProperty("alive", player.isAlive());
        json.addProperty("selected", player.getInventory().selected);
        json.addProperty("revision", SlotData.revision(player));
        json.addProperty("driving", Driving.isDriver(player));
        json.addProperty("spectator", player.isSpectator());
        json.add("quick", stack(SlotData.copy(player)));
        json.add("held", stack(player.getMainHandItem()));
        json.add("cursor", stack(player.inventoryMenu.getCarried()));
        JsonArray inventory = new JsonArray();
        for (int i = 0; i < 36; i++) inventory.add(stack(player.getInventory().getItem(i)));
        json.add("inventory", inventory);
        for (var slot : player.inventoryMenu.slots) if (slot instanceof com.chunkworks.quickslot.menu.QuickMenuSlot) {
            json.addProperty("quickIndex", slot.index);
            json.add("menuQuick", stack(slot.getItem()));
        }
        if (ModList.get().isLoaded("vanillawheels")) WheelsFixture.describe(player, json);
        return json;
    }
    static int sequence(JsonObject command) { return command.has("seq") ? command.get("seq").getAsInt() : 0; }
}
