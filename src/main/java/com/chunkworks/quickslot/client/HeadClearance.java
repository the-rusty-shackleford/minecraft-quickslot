/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.domain.Triangles;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * AF: allocation-free workspace for actual item faces versus the posed head/helmet volume.
 * RI: one workspace per cached player/stack presentation, render-thread confined; meshes
 * are read-only. Unchanged relative poses reuse the previous result. Item faces are
 * transformed to head-local coordinates, avoiding a diagonal model's empty bounding space.
 */
final class HeadClearance {
    private final Matrix4f relative = new Matrix4f(), part = new Matrix4f(), itemToHead = new Matrix4f();
    private final Matrix4f previous = new Matrix4f();
    private final Vector3f point = new Vector3f(), direction = new Vector3f();
    private final float[] vertices = new float[12];
    private float halfX, halfY;
    private float previousPad = -1, previousPush;
    private static Matrix4f transform(Matrix4f into, ModelPart part) {
        return into.identity().translate(part.x / 16, part.y / 16, part.z / 16)
                .rotateZYX(part.zRot, part.yRot, part.xRot).scale(part.xScale, part.yScale, part.zScale);
    }
    /** effects: returns up to one pixel of clearance, or infinity for a hidden-display fallback. */
    float push(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, RenderedBounds.Shape shape, Matrix4f item) {
        if (shape.quads().length == 0) return 0;
        relative.set(transform(part, model.head)).invert().mul(transform(part, model.body));
        relative.transformDirection(0,0,1,direction);
        itemToHead.set(relative).mul(item);
        float armor = (float) EquipmentClearance.armor(player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD);
        float pad = Math.max(0.03125f, armor) + 0.005f;
        if (pad == previousPad && previous.equals(itemToHead)) return previousPush;
        previous.set(itemToHead); previousPad = pad;
        halfX = 0.25f + pad; halfY = 0.25f + pad;
        if (!hits(shape.quads(),0)) return previousPush = 0;
        float low=0, high=0.0625f;
        if (hits(shape.quads(),high)) return previousPush = Float.POSITIVE_INFINITY;
        for (int i=0;i<7;i++) {
            float middle=(low+high)/2;
            if (hits(shape.quads(),middle)) low=middle; else high=middle;
        }
        return previousPush = high;
    }
    private boolean hits(float[][] quads,float shift) {
        for (float[] quad : quads) {
            for (int i=0;i<12;i+=3) {
                itemToHead.transformPosition(quad[i],quad[i+1],quad[i+2],point);
                vertices[i]=point.x+direction.x*shift;
                vertices[i+1]=point.y+direction.y*shift+0.25f;
                vertices[i+2]=point.z+direction.z*shift;
            }
            if (Triangles.intersects(vertices,0,3,6,halfX,halfY,halfX)
                    || Triangles.intersects(vertices,0,6,9,halfX,halfY,halfX)) return true;
        }
        return false;
    }
}
