/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

import static org.junit.jupiter.api.Assertions.*;
import static com.chunkworks.quickslot.domain.Presentation.*;
import org.junit.jupiter.api.Test;

/** Partitions: tag/fallback precedence, edible/named/ordinary blocks, armor/tools, hidden bowls and carried styles;
 * model bounds: tall, wide, deep, offset, invalid and oversized. Real renderer gate is separate. */
final class PresentationTest {
    private static Facts facts(Anchor tag, Style style, boolean food, boolean named, boolean block,
            boolean armor, boolean large, boolean hip, boolean drink, boolean bowl, boolean bucket) {
        return new Facts(tag, style, food, named, block, armor, large, hip, drink, bowl, bucket);
    }
    @Test void cropsAreCarriedButFeastOverrideWins() {
        assertEquals(new Kind(Anchor.LOWER_BACK, Style.POUCH), classify(facts(null,null,true,true,true,false,false,false,false,false,false)));
        assertEquals(Anchor.HIDDEN, classify(facts(Anchor.HIDDEN,null,true,false,true,false,false,false,false,false,false)).anchor());
    }
    @Test void namedBlocksAndOrdinaryBlocksDiffer() {
        assertEquals(Anchor.LOWER_BACK, classify(facts(null,null,false,true,true,false,false,false,false,false,false)).anchor());
        assertEquals(Anchor.HIDDEN, classify(facts(null,null,false,false,true,false,false,false,false,false,false)).anchor());
    }
    @Test void armorHiddenAndWeaponsBackUnlessTaggedBelt() {
        assertEquals(Anchor.HIDDEN, classify(facts(null,null,false,false,false,true,true,false,false,false,false)).anchor());
        assertEquals(Anchor.BACK, classify(facts(null,null,false,false,false,false,true,false,false,false,false)).anchor());
        assertEquals(Anchor.HIP, classify(facts(Anchor.LOWER_BACK,null,false,false,false,false,true,true,false,false,false)).anchor());
    }
    @Test void drinkBowlAndBucketResolveTheirOwnStyleAndAnchor() {
        assertEquals(new Kind(Anchor.HIP,Style.BOTTLE), classify(facts(null,null,true,false,false,false,false,false,true,false,false)));
        assertEquals(new Kind(Anchor.HIDDEN,Style.BOWL), classify(facts(null,null,true,false,false,false,false,false,false,true,false)));
        assertEquals(new Kind(Anchor.HIDDEN,Style.BOWL), classify(facts(Anchor.HIP,Style.BOWL,true,false,false,false,false,false,false,false,false)));
        assertEquals(new Kind(Anchor.HIDDEN,Style.BOWL), classify(facts(Anchor.HIP,Style.POUCH,true,false,false,false,false,false,false,true,false)));
        assertEquals(new Kind(Anchor.HIP,Style.HANG), classify(facts(null,null,true,false,false,false,false,false,true,false,true)));
        assertEquals(Style.POUCH, classify(facts(null,Style.POUCH,true,false,false,false,false,false,true,false,false)).style());
    }
    @Test void normalizationBoundsEveryAxisEvenForAnOversizedModel() {
        for (Bounds b : new Bounds[]{new Bounds(-2,0,0,2,80,1), new Bounds(-100,-5,-8,100,5,8),new Bounds(0,0,0,1,1,40)}) {
            double s = b.fit(0.98);
            assertTrue(Math.sqrt(b.width()*b.width()+b.height()*b.height()+b.depth()*b.depth())*s <= 0.98000001);
        }
        assertEquals(12, new Bounds(10,0,0,14,2,1).centerX());
    }
    @Test void invalidBoundsAndScaleAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0,0,0,0,0,0));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(2,0,0,1,1,1));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0,0,0,Double.NaN,1,1));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0,0,0,1,1,1).fit(0));
    }
    @Test void broadFacesAreAlignedRegardlessOfTheModelsAuthoredPlane() {
        assertEquals(Plane.XY, new Bounds(0,0,0,1,2,0.1).plane());
        assertEquals(Plane.YZ, new Bounds(0,0,0,0.1,2,1).plane());
        assertEquals(Plane.XZ, new Bounds(0,0,0,1,0.1,2).plane());
        assertEquals(0.1, new Bounds(0,0,0,1,0.1,2).flatDepth());
        assertEquals(Plane.XY, new Bounds(0,0,0,1,1,1).plane());
    }
}
