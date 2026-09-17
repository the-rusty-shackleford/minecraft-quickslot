/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.SlotData;
import com.chunkworks.quickslot.domain.Presentation;
import com.chunkworks.quickslot.domain.Presentation.Anchor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AF: entries are fitted presentations of the synced quick-slot stack for each tracked player.
 * RI: weak keys do not retain departed players; values hold no player references; changes to
 * attachment identity or reload generation replace an entry. Render work allocates no stacks.
 * Client-only behavior is verified through third-person real-client captures, not a mock renderer.
 */
public final class BodyLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private record Entry(ItemStack source, ItemStack display, Presentation.Kind kind, RenderedBounds.Shape shape) {}
    private static final Map<AbstractClientPlayer, Entry> ENTRIES = new WeakHashMap<>();
    /** effects: creates a layer for either the wide or slim player renderer. */
    public BodyLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    /** effects: discards all resource/tag-derived fits and classifications. */
    public static void clear() { ENTRIES.clear(); ItemPresentation.clear(); }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        ItemStack source = player.getData(SlotData.STACK);
        if (source.isEmpty()) { ENTRIES.remove(player); return; }
        Entry entry = ENTRIES.get(player);
        if (entry == null || entry.source() != source) {
            ItemStack display = source.copyWithCount(1);
            var kind = ItemPresentation.of(display);
            entry = new Entry(source, display, kind, kind.anchor() == Anchor.HIDDEN ? null
                    : RenderedBounds.measure(player, display, light));
            ENTRIES.put(player, entry);
        }
        if (entry.kind().anchor() == Anchor.HIDDEN) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        place(pose, buffers, light, entry, player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1);
        Minecraft.getInstance().getItemRenderer().renderStatic(player, entry.display(), ItemDisplayContext.NONE,
                false, pose, buffers, player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }
    private static void place(PoseStack pose, MultiBufferSource buffers, int light, Entry entry, int side) {
        var b = entry.shape().bounds();
        Anchor anchor = entry.kind().anchor();
        boolean flatten = anchor == Anchor.BACK || entry.kind().style() == Presentation.Style.BOWL;
        Presentation.Plane plane = flatten ? b.plane() : Presentation.Plane.XY;
        double depth = flatten ? b.flatDepth() : b.depth();
        double depthScale = entry.display().is(Items.SHIELD) ? 0.35 : 1;
        double length = anchor == Anchor.BACK ? 0.98 : switch (entry.kind().style()) {
            case BOTTLE -> 0.27;
            case POUCH -> 0.25;
            case BOWL -> 0.23;
            case HANG -> 0.43;
            case TOOL -> 0.30;
        };
        double scale = b.fit(length);
        if (anchor == Anchor.BACK) {
            boolean shield = entry.display().is(Items.SHIELD);
            boolean trident = entry.display().is(Items.TRIDENT);
            if (shield) scale = b.fit(0.73);
            pose.translate(0, 0.32, 0.135 + depth * scale * depthScale / 2);
            float angle = shield ? 0 : trident ? -22 : entry.shape().sprite() ? -75 : -30;
            pose.mulPose(Axis.ZP.rotationDegrees(angle * side));
        } else if (anchor == Anchor.HIP) {
            BeltHolder.mount(pose, buffers, light, side);
            pose.translate(side * (0.26 + depth * scale / 2), 0.59, 0.23);
            pose.mulPose(Axis.YP.rotationDegrees(side * 90));
            pose.mulPose(Axis.ZP.rotationDegrees(180));
        } else {
            pose.translate(side * 0.14, 0.60, 0.14 + b.depth() * scale / 2);
            pose.mulPose(Axis.ZP.rotationDegrees(170 * side));
        }
        if (anchor != Anchor.BACK) BeltHolder.render(entry.kind().style(), pose, buffers, light);
        pose.scale((float) scale, (float) scale, (float) (scale * depthScale));
        if (plane == Presentation.Plane.YZ) pose.mulPose(Axis.YP.rotationDegrees(90));
        if (plane == Presentation.Plane.XZ) pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.translate(-b.centerX(), -b.centerY(), -b.centerZ());
    }
}
