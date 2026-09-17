/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.domain.Presentation.Anchor;
import com.chunkworks.quickslot.domain.Presentation.Style;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * AF: immutable overrides from assets/quickslot/quickslot_placement/namespace/item.json.
 * RI: finite validated block-space offsets, degree rotations and positive scales; swapped
 * atomically on the resource apply thread. Invalid entries are logged and omitted.
 */
public final class Placements extends SimpleJsonResourceReloadListener {
    public record Vector(double x, double y, double z) { static final Vector ZERO = new Vector(0, 0, 0); }
    public record Transform(Vector offset, Vector rotation, double scale) {
        static final Transform IDENTITY = new Transform(Vector.ZERO, Vector.ZERO, 1);
    }
    public record Override(Anchor anchor, Style style, Transform transform, Transform withBackpack) {}
    private static Map<ResourceLocation, Override> entries = Map.of();
    /** effects: registers a resource-only placement loader; no data-pack or network authority. */
    public Placements() { super(new Gson(), "quickslot_placement"); }
    /** effects: returns a per-item override, or null when fallback classification applies. */
    public static Override get(ResourceLocation item) { return entries.get(item); }

    @java.lang.Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Override> next = new HashMap<>();
        resources.forEach((id, json) -> {
            if (!id.getNamespace().equals("quickslot")) return;
            try {
                int slash = id.getPath().indexOf('/');
                if (slash < 1) throw new IllegalArgumentException("Expected namespace/item path");
                ResourceLocation item = ResourceLocation.fromNamespaceAndPath(id.getPath().substring(0, slash), id.getPath().substring(slash + 1));
                JsonObject o = json.getAsJsonObject();
                Anchor anchor = Anchor.valueOf(o.get("anchor").getAsString().toUpperCase(Locale.ROOT));
                Style style = o.has("style") ? Style.valueOf(o.get("style").getAsString().toUpperCase(Locale.ROOT)) : null;
                next.put(item, new Override(anchor, style, transform(o),
                        o.has("with_backpack") ? transform(o.getAsJsonObject("with_backpack")) : Transform.IDENTITY));
            } catch (RuntimeException bad) {
                LogUtils.getLogger().warn("Ignoring Quick Slot placement {}: {}", id, bad.toString());
            }
        });
        entries = Map.copyOf(next);
        BodyLayer.clear();
    }
    private static Transform transform(JsonObject o) {
        return new Transform(vector(o, "offset", 2), vector(o, "rotation", 360), number(o, "scale", 1, 0.05, 4));
    }
    private static Vector vector(JsonObject o, String name, double limit) {
        if (!o.has(name)) return Vector.ZERO;
        var a = o.getAsJsonArray(name);
        if (a.size() != 3) throw new IllegalArgumentException(name + " must contain xyz");
        double x = a.get(0).getAsDouble(), y = a.get(1).getAsDouble(), z = a.get(2).getAsDouble();
        if (!Double.isFinite(x + y + z) || Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) > limit)
            throw new IllegalArgumentException(name + " is out of range");
        return new Vector(x, y, z);
    }
    private static double number(JsonObject o, String name, double fallback, double min, double max) {
        double value = o.has(name) ? o.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException(name + " is out of range");
        return value;
    }
}
