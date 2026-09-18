/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.chunkworks.quickslot.SlotData;

/** Partitions: absent/empty/populated/malformed legacy data; empty/occupied new slot;
 * named/damaged component preservation, unrelated attachments, idempotence and real compressed files;
 * existing output, symbolic-link aliases and unexpected input names refuse before writing output.
 * Uses native player load and actual NBT I/O, never substitutes a serializer or storage backend. */
@GameTestHolder("quickslot")
@PrefixGameTestTemplate(false)
public final class HolsterMigrationGameTests {
    public HolsterMigrationGameTests() {}

    @GameTest(template = "empty")
    public void nativePlayerLoadsMigratedCompleteItem(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.setDamageValue(54);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Legacy named sword"));
        CompoundTag source = player.saveWithoutId(new CompoundTag());
        CompoundTag attachments = source.getCompound("neoforge:attachments");
        attachments.put("stowed:holster", item.save(h.getLevel().registryAccess()));
        source.put("neoforge:attachments", attachments);
        CompoundTag converted = MigrateHolsters.convert(source);
        h.assertTrue(source.getCompound("neoforge:attachments").contains("stowed:holster"), "input remains unchanged");
        player.load(converted);
        h.assertTrue(ItemStack.matches(SlotData.copy(player), item), "native attachment loader retains full item");
        h.assertTrue(MigrateHolsters.convert(converted).equals(converted), "second conversion cannot duplicate item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void conflictingAndMalformedDataRefuseWithoutChanges(GameTestHelper h) {
        CompoundTag root = new CompoundTag(), attachments = new CompoundTag(), item = new CompoundTag();
        item.putString("id", "minecraft:diamond"); item.putInt("count", 7);
        attachments.put("stowed:holster", item.copy()); attachments.put("quickslot:stack", item.copy());
        root.put("neoforge:attachments", attachments);
        CompoundTag before = root.copy();
        boolean rejected = false;
        try { MigrateHolsters.convert(root); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected && root.equals(before), "occupied destination preserves both inputs");
        attachments.remove("quickslot:stack"); attachments.putString("stowed:holster", "bad");
        rejected = false;
        try { MigrateHolsters.convert(root); } catch (IllegalArgumentException expected) { rejected = true; }
        h.assertTrue(rejected, "wrong-type holster is not silently discarded");
        attachments.put("stowed:holster", new CompoundTag());
        h.assertTrue(!MigrateHolsters.convert(root).getCompound("neoforge:attachments").contains("stowed:holster"), "empty legacy attachment removed");
        h.assertTrue(MigrateHolsters.convert(new CompoundTag()).isEmpty(), "no legacy data creates no item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void actualFilesArePreparedWithoutOverwritingSource(GameTestHelper h) throws IOException {
        Path folder = Files.createTempDirectory(Path.of("."), "holster-test-");
        try {
            Path input = Files.createDirectory(folder.resolve("input")), output = folder.resolve("output");
            CompoundTag root = new CompoundTag(), attachments = new CompoundTag(), item = new CompoundTag();
            item.putString("id", "some_mod:custom_tool"); item.putInt("count", 1);
            CompoundTag components = new CompoundTag(); components.putString("some_mod:opaque_component", "preserve exactly");
            item.put("components", components);
            attachments.put("stowed:holster", item); attachments.putLong("another_mod:state", 19);
            root.put("neoforge:attachments", attachments); root.putString("unrelated", "retained");
            Path file = input.resolve(UUID.randomUUID() + ".dat");
            NbtIo.writeCompressed(root, file);
            byte[] before = Files.readAllBytes(file);
            h.assertValueEqual(MigrateHolsters.prepare(input, output), 1, "one legacy stack moved");
            h.assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(file)), "source gzip bytes unchanged");
            CompoundTag result = NbtIo.readCompressed(output.resolve(file.getFileName()), NbtAccounter.unlimitedHeap());
            h.assertTrue(result.getCompound("neoforge:attachments").getCompound("quickslot:stack").equals(item), "unknown mod components preserved without registry coercion");
            h.assertValueEqual(result.getString("unrelated"), "retained", "other player data preserved");
            h.assertTrue(Files.exists(output.resolve("COMPLETE")), "verified completion marker written");
            h.assertTrue(Files.readString(output.resolve("sha256.tsv")).contains(file.getFileName().toString()), "operator receives input and output checksums");
            boolean rejected = false;
            try { MigrateHolsters.prepare(input, output); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "existing output cannot be overwritten");
            h.assertValueEqual(MigrateHolsters.prepare(output, folder.resolve("again")), 0, "file-level rerun is idempotent");
            Path alias = folder.resolve("alias");
            Files.createSymbolicLink(alias, input.toAbsolutePath());
            rejected = false;
            try { MigrateHolsters.prepare(input, alias.resolve("unsafe")); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected && !Files.exists(input.resolve("unsafe")), "symbolic link cannot place output inside input");
            NbtIo.writeCompressed(root, input.resolve("unexpected.dat"));
            rejected = false;
            try { MigrateHolsters.prepare(input, folder.resolve("invalid")); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected && !Files.exists(folder.resolve("invalid")), "unrecognized .dat file cannot be silently skipped");
            h.succeed();
        } finally {
            try (var paths = Files.walk(folder)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
