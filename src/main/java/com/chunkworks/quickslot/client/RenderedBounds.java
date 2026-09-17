/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.domain.Presentation.Bounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * AF: extrema of vertices emitted by the real item renderer, including custom models.
 * RI: one instance per measurement, used on the render thread, never submitted to the GPU.
 * The normal render path is measured in NONE context, so Modefite/Refined Tools choose
 * their body model rather than the deliberately flat GUI/fixed/ground model.
 */
final class RenderedBounds implements VertexConsumer {
    record Shape(Bounds bounds, boolean sprite, float[][] quads) {}
    private final List<float[]> quads = new ArrayList<>();
    private float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
    private float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
    private boolean rotatedFaces;

    /** effects: measures actual emitted geometry once; logs unsupported renderers and uses a bounded fallback. */
    static Shape measure(AbstractClientPlayer player, ItemStack stack, int light) {
        RenderedBounds vertices = new RenderedBounds();
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(player, stack, ItemDisplayContext.NONE,
                    false, new PoseStack(), type -> vertices.consumer(), player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
            Bounds bounds = new Bounds(vertices.minX, vertices.minY, vertices.minZ, vertices.maxX, vertices.maxY, vertices.maxZ);
            return new Shape(bounds, bounds.squareSprite() && !vertices.rotatedFaces, vertices.quads.toArray(float[][]::new));
        } catch (RuntimeException unsupported) {
            LogUtils.getLogger().warn("Quick Slot could not measure {}: {}. Placement override may be needed.",
                    stack.getItem(), unsupported.toString());
            return new Shape(new Bounds(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5), false, new float[0][]);
        }
    }
    private VertexConsumer consumer() {
        // Glint creates a VertexMultiConsumer, which requires distinct delegates for
        // its base and foil passes. Each stream shares the same extrema accumulator.
        return new VertexConsumer() {
            private float[] quad = new float[12];
            private int next;
            @Override public VertexConsumer addVertex(float x, float y, float z) {
                RenderedBounds.this.addVertex(x, y, z);
                quad[next++] = x; quad[next++] = y; quad[next++] = z;
                if (next == 12) { quads.add(quad); quad = new float[12]; next = 0; }
                return this;
            }
            @Override public VertexConsumer setNormal(float x, float y, float z) { RenderedBounds.this.setNormal(x, y, z); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer setUv(float u, float v) { return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
        };
    }
    @Override public VertexConsumer addVertex(float x, float y, float z) {
        minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v) { return this; }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) {
        int axes = (Math.abs(x) > 0.01 ? 1 : 0) + (Math.abs(y) > 0.01 ? 1 : 0) + (Math.abs(z) > 0.01 ? 1 : 0);
        rotatedFaces |= axes > 1;
        return this;
    }
}
