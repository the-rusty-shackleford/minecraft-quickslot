/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.client;

import com.chunkworks.quickslot.ClientRules;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * AF: cached one-item presentations of synced stacks, plus reusable fitting workspaces.
 * RI: weak keys retain no departed players; entries contain no player references. Stack identity
 * and tag/resource/config reloads invalidate fits. All rendering is client-thread confined.
 * Actual wide/slim third-person captures gate this client-only adapter.
 */
public final class BodyLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private record Entry(ItemStack source, ItemStack display, Presentation.Kind kind, RenderedBounds.Shape shape,
            Placements.Override override, Quaternionf rotation, Quaternionf backpackRotation) {}
    private static final Map<AbstractClientPlayer, Entry> ENTRIES = new WeakHashMap<>();
    private static java.util.function.Predicate<AbstractClientPlayer> visibleBackpack = player -> false;
    private static final double PACK_HIP_DROP = 0.18;
    private final PoseStack local = new PoseStack();
    private final Matrix4f holder = new Matrix4f();
    private Anchor fittedAnchor;
    /** effects: creates a body layer for a wide or slim player renderer. */
    public BodyLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    /** effects: invalidates reload-derived classification and model measurements. */
    public static void clear() { ENTRIES.clear(); ItemPresentation.clear(); }
    /**
     * requires: client setup thread; supplier reads an existing equipment snapshot without allocating.
     * effects: installs optional companion visibility. Only a visibly worn pack covers the back;
     * carried, inventory-only and cosmetically hidden packs must return false.
     * throws: NullPointerException for a null supplier.
     */
    public static void backpackVisibility(java.util.function.Predicate<AbstractClientPlayer> visible) {
        visibleBackpack = java.util.Objects.requireNonNull(visible);
    }

    private static Entry entry(AbstractClientPlayer player, ItemStack source, int light) {
        ItemStack display = source.copyWithCount(1);
        var kind = ItemPresentation.of(display);
        boolean bowlMeal = kind.style() == Presentation.Style.BOWL;
        var override = Placements.get(BuiltInRegistries.ITEM.getKey(display.getItem()));
        Quaternionf rotation = new Quaternionf();
        Quaternionf backpackRotation = new Quaternionf();
        if (override != null) {
            kind = new Presentation.Kind(override.anchor(), override.style() == null ? kind.style() : override.style());
            var r = override.transform().rotation();
            rotation.rotationXYZ((float) Math.toRadians(r.x()), (float) Math.toRadians(r.y()), (float) Math.toRadians(r.z()));
            var pack = override.withBackpack().rotation();
            backpackRotation.rotationXYZ((float) Math.toRadians(pack.x()), (float) Math.toRadians(pack.y()), (float) Math.toRadians(pack.z()));
        }
        if (bowlMeal || kind.style() == Presentation.Style.BOWL)
            kind = new Presentation.Kind(Anchor.HIDDEN, Presentation.Style.BOWL);
        return new Entry(source, display, kind, kind.anchor() == Anchor.HIDDEN ? null
                : RenderedBounds.measure(player, display, light), override, rotation, backpackRotation);
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        if (!ClientRules.RENDER.get() || player.isInvisible() || player.isSpectator()
                || EquipmentVisibility.elytra(player)) return;
        ItemStack source = player.getData(SlotData.STACK);
        if (source.isEmpty()) { ENTRIES.remove(player); return; }
        Entry entry = ENTRIES.get(player);
        if (entry == null || entry.source() != source) {
            entry = entry(player, source, light); ENTRIES.put(player, entry);
        }
        if (entry.kind().anchor() == Anchor.HIDDEN) return;
        boolean backpack = visibleBackpack.test(player);
        if ((backpack || EquipmentVisibility.cape(player)) && entry.kind().anchor() == Anchor.BACK) return;
        int side = player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1;
        fit(entry, player, side, backpack);
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        if (fittedAnchor != Anchor.BACK) {
            if (fittedAnchor == Anchor.HIP) {
                pose.pushPose();
                if (backpack) pose.translate(0, PACK_HIP_DROP, 0);
                BeltHolder.mount(pose, buffers, light, side);
                pose.popPose();
            }
            pose.pushPose(); pose.mulPose(holder);
            BeltHolder.render(entry.kind().style(), pose, buffers, light); pose.popPose();
        }
        pose.mulPose(local.last().pose());
        Minecraft.getInstance().getItemRenderer().renderStatic(player, entry.display(), ItemDisplayContext.NONE,
                false, pose, buffers, player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
        pose.popPose();
    }
    private void fit(Entry entry, AbstractClientPlayer player, int side, boolean backpack) {
        local.setIdentity();
        var b = entry.shape().bounds();
        Anchor anchor = entry.kind().anchor();
        if (anchor == Anchor.LOWER_BACK && (EquipmentVisibility.cape(player)
                || backpack && ClientRules.MOVE_BELT.get())) anchor = Anchor.HIP;
        fittedAnchor = anchor;
        boolean flatten = anchor == Anchor.BACK;
        Presentation.Plane plane = flatten ? b.plane() : Presentation.Plane.XY;
        double depth = flatten ? b.flatDepth() : b.depth();
        double depthScale = entry.display().is(Items.SHIELD) ? 0.35 : 1;
        double length = anchor == Anchor.BACK ? ClientRules.MAX_LENGTH.get() : switch (entry.kind().style()) {
            case BOTTLE -> ClientRules.BOTTLE_SCALE.get();
            case POUCH -> ClientRules.POUCH_SCALE.get();
            case BOWL -> throw new IllegalStateException("Bowl meals have no body placement");
            case HANG -> ClientRules.HANG_SCALE.get();
            case TOOL -> 0.30;
        };
        boolean shield = entry.display().is(Items.SHIELD);
        if (anchor == Anchor.BACK && shield) length = Math.min(length, 0.73);
        double multiplier = anchor == Anchor.BACK ? ClientRules.BACK_SCALE.get() : ClientRules.BELT_SCALE.get();
        if (entry.override() != null) {
            multiplier *= entry.override().transform().scale();
            if (backpack) multiplier *= entry.override().withBackpack().scale();
        }
        length *= multiplier;
        if (anchor == Anchor.BACK) length = Math.min(length, ClientRules.MAX_LENGTH.get());
        double scale = b.fit(length);
        if (anchor == Anchor.BACK) {
            int shoulder = switch (ClientRules.BACK_SIDE.get()) {
                case OPPOSITE_MAIN_ARM -> side;
                case MAIN_ARM -> -side;
                case LEFT -> -1;
                case RIGHT -> 1;
            };
            // Restore the approved Phase 3 fit. Small hair/helmet intersections are accepted;
            // head motion and armor never push the item away or make it disappear.
            local.translate(0, 0.32, 0.135 + depth * scale * depthScale / 2);
            float angle = shield ? 0 : entry.display().is(Items.TRIDENT) ? -22 : entry.shape().sprite() ? -75 : -30;
            local.mulPose(Axis.ZP.rotationDegrees(angle * shoulder));
        } else if (anchor == Anchor.HIP) {
            // Keep the approved body fit without a bag. With a visible pack, seat
            // small gear just below its lower edge instead of burying it in a pocket.
            local.translate(side * (0.26 + depth * scale / 2), 0.59 + (backpack ? PACK_HIP_DROP : 0), 0.23);
            local.mulPose(Axis.YP.rotationDegrees(side * 90));
            local.mulPose(Axis.ZP.rotationDegrees(180));
        } else {
            local.translate(side * 0.14, 0.60, 0.14 + b.depth() * scale / 2);
            local.mulPose(Axis.ZP.rotationDegrees(170 * side));
        }
        if (entry.override() != null) {
            var o = entry.override().transform().offset();
            local.translate(o.x(), o.y(), o.z()); local.mulPose(entry.rotation());
            if (backpack) {
                var pack = entry.override().withBackpack().offset();
                local.translate(pack.x(), pack.y(), pack.z()); local.mulPose(entry.backpackRotation());
            }
        }
        holder.set(local.last().pose());
        local.scale((float) scale, (float) scale, (float) (scale * depthScale));
        if (plane == Presentation.Plane.YZ) local.mulPose(Axis.YP.rotationDegrees(90));
        if (plane == Presentation.Plane.XZ) local.mulPose(Axis.XP.rotationDegrees(90));
        local.translate(-b.centerX(), -b.centerY(), -b.centerZ());
    }
}
