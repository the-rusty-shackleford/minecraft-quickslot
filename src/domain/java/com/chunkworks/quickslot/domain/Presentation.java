/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

/** Pure presentation policy. Platform facts are immutable inputs; storage is unaffected. */
public final class Presentation {
    private Presentation() {}
    public enum Anchor { HIDDEN, BACK, HIP, LOWER_BACK }
    public enum Style { TOOL, BOTTLE, POUCH, BOWL, HANG }
    public enum Plane { XY, YZ, XZ }
    /** AF: one resolved body location and carrying style. RI: both members are non-null. */
    public record Kind(Anchor anchor, Style style) {}
    /** AF: classification facts about a stack. RI: optional tagged choices may be null. */
    public record Facts(Anchor taggedAnchor, Style taggedStyle, boolean consumable,
            boolean namedBlock, boolean block, boolean armor, boolean large,
            boolean hipTool, boolean drink, boolean bowl, boolean bucket) {}

    /** effects: resolves tags first, then consumables before placeable crops and ordinary blocks. */
    public static Kind classify(Facts f) {
        Style style = f.taggedStyle() != null ? f.taggedStyle()
                : f.bucket() ? Style.HANG : f.drink() ? Style.BOTTLE
                : f.bowl() ? Style.BOWL : f.consumable() ? Style.POUCH : Style.TOOL;
        Anchor anchor = f.taggedAnchor();
        if (anchor == null) {
            if (f.consumable()) anchor = Anchor.LOWER_BACK;
            else if (f.namedBlock()) anchor = Anchor.LOWER_BACK;
            else if (f.block() || f.armor()) anchor = Anchor.HIDDEN;
            else if (f.large()) anchor = Anchor.BACK;
            else anchor = Anchor.LOWER_BACK;
        }
        if (anchor == Anchor.LOWER_BACK && (f.hipTool() || style == Style.BOTTLE
                || style == Style.BOWL || style == Style.HANG)) anchor = Anchor.HIP;
        return new Kind(anchor, style);
    }

    /**
     * AF: measured rendered extents in blocks. RI: finite, ordered, non-degenerate bounds.
     * Invalid/no-vertex custom renderers must supply an explicit fallback before construction.
     */
    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Bounds {
            if (!Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ)
                    || minX > maxX || minY > maxY || minZ > maxZ
                    || Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) < 0.00001)
                throw new IllegalArgumentException("Empty or invalid rendered bounds");
        }
        public double width() { return maxX - minX; }
        public double height() { return maxY - minY; }
        public double depth() { return maxZ - minZ; }
        public double centerX() { return (minX + maxX) / 2; }
        public double centerY() { return (minY + maxY) / 2; }
        public double centerZ() { return (minZ + maxZ) / 2; }
        /** effects: identifies a strongly thinner axis so a flat model can face the body surface. */
        public Plane plane() {
            if (width() < depth() * 0.6 && width() < height() * 0.6) return Plane.YZ;
            if (height() < depth() * 0.6 && height() < width() * 0.6) return Plane.XZ;
            return Plane.XY;
        }
        /** effects: returns thickness after aligning the broad face with the body surface. */
        public double flatDepth() {
            return switch (plane()) { case XY -> depth(); case YZ -> width(); case XZ -> height(); };
        }
        /** requires: positive finite limit; effects: fits even a rotated model within that diagonal. */
        public double fit(double limit) {
            if (!Double.isFinite(limit) || limit <= 0) throw new IllegalArgumentException("Invalid length");
            return limit / Math.sqrt(width() * width() + height() * height() + depth() * depth());
        }
        /** effects: identifies nearly square, shallow sprites for their diagonal tool orientation. */
        public boolean squareSprite() {
            return depth() < Math.max(width(), height()) * 0.15 && width() > height() * 0.75;
        }
    }
}
