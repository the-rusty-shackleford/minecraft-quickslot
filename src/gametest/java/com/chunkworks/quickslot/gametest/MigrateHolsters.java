/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * AF: a prepared copy of player saves with the legacy holster attachment renamed.
 * RI: source files are never written; an occupied destination or malformed attachment
 * aborts preparation. Item tags/components and every unrelated player field are preserved.
 * This operator tool belongs to the development source set, never the production jar.
 */
public final class MigrateHolsters {
    private MigrateHolsters() {}
    private static final String ATTACHMENTS = "neoforge:attachments";
    private static final String OLD = "stowed:holster";
    private static final String NEXT = "quickslot:stack";
    private record Prepared(Path source, String digest, CompoundTag original, CompoundTag converted) {}

    /**
     * requires: raw player NBT from Minecraft 1.21.1.
     * effects: returns an independent migrated copy; running again is a no-op.
     * throws: IllegalArgumentException for malformed attachments or an occupied Quick Slot.
     */
    public static CompoundTag convert(CompoundTag source) {
        CompoundTag result = source.copy();
        if (!result.contains(ATTACHMENTS)) return result;
        if (!result.contains(ATTACHMENTS, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Malformed player attachments");
        CompoundTag attachments = result.getCompound(ATTACHMENTS);
        if (!attachments.contains(OLD)) return result;
        if (!attachments.contains(OLD, Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Malformed legacy holster");
        CompoundTag old = attachments.getCompound(OLD);
        if (!empty(old)) {
            if (attachments.contains(NEXT) && (!attachments.contains(NEXT, Tag.TAG_COMPOUND)
                    || !empty(attachments.getCompound(NEXT))))
                throw new IllegalArgumentException("Quick Slot is occupied; neither item was changed");
            attachments.put(NEXT, old.copy());
        }
        attachments.remove(OLD);
        return result;
    }

    private static boolean empty(CompoundTag stack) {
        if (stack.isEmpty()) return true;
        if (!stack.contains("id", Tag.TAG_STRING))
            throw new IllegalArgumentException("Item stack has no string ID");
        if (stack.getString("id").equals("minecraft:air")) return true;
        if (stack.getString("id").isBlank()) throw new IllegalArgumentException("Blank item ID");
        if (stack.contains("count") && (!stack.contains("count", Tag.TAG_ANY_NUMERIC) || stack.getInt("count") <= 0))
            throw new IllegalArgumentException("Invalid item count");
        return false;
    }

    /**
     * requires: an offline copied playerdata directory; output's parent must exist;
     * output must not exist or be inside input, including through a symbolic link.
     * effects: validates every current .dat file before creating independent converted files;
     * writes COMPLETE only after all output files round-trip and source files remain unchanged.
     * throws: IOException or IllegalArgumentException on unsafe paths, conflict, corruption or I/O failure.
     */
    public static int prepare(Path input, Path output) throws IOException {
        Path source = input.toRealPath();
        Path requested = output.toAbsolutePath().normalize();
        Path target = requested.getParent().toRealPath().resolve(requested.getFileName());
        if (!Files.isDirectory(source) || target.startsWith(source) || Files.exists(target))
            throw new IllegalArgumentException("Use a copied input directory and a new separate output directory");
        List<Prepared> prepared = new ArrayList<>();
        try (var paths = Files.list(source)) {
            for (Path file : paths.filter(p -> p.getFileName().toString().endsWith(".dat")).sorted().toList()) {
                if (!file.getFileName().toString().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.dat"))
                    throw new IllegalArgumentException("Unexpected player save filename: " + file.getFileName());
                if (!Files.isRegularFile(file) || Files.isSymbolicLink(file))
                    throw new IllegalArgumentException("Player save is not a regular file: " + file.getFileName());
                String digest = digest(file);
                CompoundTag before = NbtIo.readCompressed(file, NbtAccounter.create(32L * 1024 * 1024));
                prepared.add(new Prepared(file, digest, before, convert(before)));
            }
        }
        if (prepared.isEmpty()) throw new IllegalArgumentException("No player saves found");
        Files.createDirectory(target);
        int changed = 0;
        StringBuilder manifest = new StringBuilder("filename\tsource_sha256\tprepared_sha256\n");
        for (Prepared player : prepared) {
            Path file = target.resolve(player.source().getFileName());
            NbtIo.writeCompressed(player.converted(), file);
            if (!player.converted().equals(NbtIo.readCompressed(file, NbtAccounter.create(32L * 1024 * 1024))))
                throw new IOException("Output verification failed: " + file.getFileName());
            manifest.append(file.getFileName()).append('\t').append(player.digest()).append('\t').append(digest(file)).append('\n');
            if (!player.original().equals(player.converted())) changed++;
        }
        for (Prepared player : prepared) {
            if (!player.digest().equals(digest(player.source())))
                throw new IOException("Input changed during preparation: " + player.source().getFileName());
        }
        Files.writeString(target.resolve("sha256.tsv"), manifest);
        Files.writeString(target.resolve("COMPLETE"), "Verified " + prepared.size() + " player saves; migrated " + changed + ".\n");
        return changed;
    }

    private static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("Java requires SHA-256", impossible);
        }
    }

    /** effects: prepares copied saves; throws: on any refusal so an operator cannot mistake failure for success. */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("Usage: MigrateHolsters INPUT_COPY OUTPUT_NEW");
        int changed = prepare(Path.of(args[0]), Path.of(args[1]));
        System.out.println("Verified holster migration for " + changed + " player saves. Source untouched; no deployment performed.");
    }
}
