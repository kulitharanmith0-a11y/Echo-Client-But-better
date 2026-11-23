package com.blamejared.clumps.client.utils.geometry;

public final class TriangleBounds {
    private TriangleBounds() {
    }

    
    public static float maximumCoordinate(float[] triangle, int axis) {
        return Math.max(triangle[axis], Math.max(triangle[2 + axis], triangle[4 + axis]));
    }
}
