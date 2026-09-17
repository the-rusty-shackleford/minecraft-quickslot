/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Partitions: inside, face-touching, crossing with all vertices outside, and disjoint
 * despite overlapping coordinate projections. These pin geometry, not a renderer mock. */
final class TrianglesTest {
    private static boolean hit(float... vertices) { return Triangles.intersects(vertices,0,3,6,1,1,1); }
    @Test void interiorAndBoundaryCountAsContact() {
        assertTrue(hit(0,0,0, .5f,0,0, 0,.5f,0));
        assertTrue(hit(1,0,0, 1,.5f,0, 1,0,.5f));
    }
    @Test void faceCanCrossWithoutAnyVertexInside() {
        assertTrue(hit(-3,-3,0, 3,-3,0, 0,3,0));
    }
    @Test void separatedFacesAndObliqueCornerMissesAreRejected() {
        assertFalse(hit(-3,-3,2, 3,-3,2, 0,3,2));
        assertFalse(hit(2.2f,0,0, 0,2.2f,0, 2.2f,2.2f,2));
        assertFalse(hit(3,3,-1, 3,-1,3, -1,3,3));
    }
}
