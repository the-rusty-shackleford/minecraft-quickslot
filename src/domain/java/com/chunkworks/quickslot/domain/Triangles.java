/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.quickslot.domain;

/** Stateless triangle/box separating-axis test; no platform types or per-call allocations. */
public final class Triangles {
    private Triangles() {}
    /**
     * requires: a/b/c index finite xyz triples in vertices; positive finite half extents.
     * effects: returns whether the triangle touches/intersects the axis-aligned box centered at zero.
     * The input array is borrowed read-only for the duration of the call.
     */
    public static boolean intersects(float[] v, int a, int b, int c, float hx, float hy, float hz) {
        if (separates(v,a,b,c,1,0,0,hx,hy,hz) || separates(v,a,b,c,0,1,0,hx,hy,hz)
                || separates(v,a,b,c,0,0,1,hx,hy,hz)) return false;
        for (int i = 0; i < 3; i++) {
            int from = i == 0 ? a : i == 1 ? b : c;
            int to = i == 0 ? b : i == 1 ? c : a;
            float x = v[to]-v[from], y = v[to+1]-v[from+1], z = v[to+2]-v[from+2];
            if (separates(v,a,b,c,0,z,-y,hx,hy,hz) || separates(v,a,b,c,-z,0,x,hx,hy,hz)
                    || separates(v,a,b,c,y,-x,0,hx,hy,hz)) return false;
        }
        float x1=v[b]-v[a], y1=v[b+1]-v[a+1], z1=v[b+2]-v[a+2];
        float x2=v[c]-v[a], y2=v[c+1]-v[a+1], z2=v[c+2]-v[a+2];
        return !separates(v,a,b,c,y1*z2-z1*y2,z1*x2-x1*z2,x1*y2-y1*x2,hx,hy,hz);
    }
    private static boolean separates(float[] v,int a,int b,int c,float x,float y,float z,float hx,float hy,float hz) {
        float pa=v[a]*x+v[a+1]*y+v[a+2]*z, pb=v[b]*x+v[b+1]*y+v[b+2]*z, pc=v[c]*x+v[c+1]*y+v[c+2]*z;
        float radius=hx*Math.abs(x)+hy*Math.abs(y)+hz*Math.abs(z);
        return Math.min(pa,Math.min(pb,pc)) > radius || Math.max(pa,Math.max(pb,pc)) < -radius;
    }
}
