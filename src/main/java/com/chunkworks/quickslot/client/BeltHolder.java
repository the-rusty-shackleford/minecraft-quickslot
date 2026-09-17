/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.domain.Presentation.Style;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Small native geometry gives consumables a visible pouch/loop rather than a floating sprite.
 * AF: immutable style-to-holder meshes in body-block units. RI: client render-thread only;
 * meshes are constructed once, and use the active pack's vanilla brown terracotta texture. */
final class BeltHolder {
    private static final RenderType MATERIAL = RenderType.entityCutoutNoCull(
            ResourceLocation.withDefaultNamespace("textures/block/brown_terracotta.png"));
    private static final Map<Style, ModelPart> PARTS = makeParts();
    private static final ModelPart MOUNT = makeMount();
    private BeltHolder() {}
    private static ModelPart makeMount() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("mount", CubeListBuilder.create()
                .addBox(0, 0, 0, 0.5f, 0.65f, 2.25f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }
    /** effects: bridges the rear hip anchor back to the belt surface. */
    static void mount(PoseStack pose, MultiBufferSource buffers, int light, int side) {
        pose.pushPose();
        pose.translate(side * 0.247, 0.49, 0.11);
        pose.scale(side, 1, 1);
        MOUNT.render(pose, buffers.getBuffer(MATERIAL), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    private static Map<Style, ModelPart> makeParts() {
        Map<Style, ModelPart> parts = new EnumMap<>(Style.class);
        for (Style style : Style.values()) {
            MeshDefinition mesh = new MeshDefinition();
            CubeListBuilder cubes = CubeListBuilder.create();
            switch (style) {
                case POUCH -> cubes.addBox(-1.6f, -1.55f, -0.5f, 3.2f, 1.45f, 1.05f)
                        .addBox(-1.6f, -0.1f, -0.5f, 0.35f, 0.75f, 1.05f)
                        .addBox(1.25f, -0.1f, -0.5f, 0.35f, 0.75f, 1.05f);
                case BOTTLE -> cubes.addBox(-0.25f, 0.8f, -0.3f, 0.5f, 1.15f, 0.3f)
                        .addBox(-0.55f, 0.65f, 0.05f, 1.1f, 0.25f, 0.25f);
                case HANG -> cubes.addBox(-0.25f, 1.25f, -0.25f, 0.5f, 1.1f, 0.4f);
                case TOOL, BOWL -> { continue; }
            }
            mesh.getRoot().addOrReplaceChild("holder", cubes, PartPose.ZERO);
            parts.put(style, LayerDefinition.create(mesh, 16, 16).bakeRoot());
        }
        return Map.copyOf(parts);
    }
    /** effects: draws the resolved carrying hardware at the already-oriented belt anchor. */
    static void render(Style style, PoseStack pose, MultiBufferSource buffers, int light) {
        ModelPart part = PARTS.get(style);
        if (part != null) part.render(pose, buffers.getBuffer(MATERIAL), light, OverlayTexture.NO_OVERLAY);
    }
}
