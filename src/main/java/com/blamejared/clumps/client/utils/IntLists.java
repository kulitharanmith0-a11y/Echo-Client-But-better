package com.blamejared.clumps.client.utils;

import com.blamejared.clumps.client.utils.geometry.TriangleBounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;

public final class IntLists {
    private static final int BIN_COUNT = 16;

    private IntLists() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    
    public static int appendBins(List<float[]> triangles, int triangleIndexOffset, int axis, List<int[]> ranges, IntArrayList indices) {
        int sortAxis = 1 - axis;
        int firstRange = ranges.size();
        for (int bin = 0; bin < BIN_COUNT; bin++) {
            IntArrayList members = intersectingTriangles(triangles, axis, bin);
            members.sort((int first, int second) -> Float.compare(
                    TriangleBounds.maximumCoordinate(triangles.get(second), sortAxis), TriangleBounds.maximumCoordinate(triangles.get(first), sortAxis)));
            int start = indices.size();
            for (int triangle : members) indices.add(triangleIndexOffset + triangle);
            ranges.add(new int[]{start, members.size()});
        }
        return firstRange;
    }

    private static IntArrayList intersectingTriangles(List<float[]> triangles, int axis, int bin) {
        float lower = (float) bin / BIN_COUNT;
        float upper = (float) (bin + 1) / BIN_COUNT;
        IntArrayList result = new IntArrayList();
        for (int index = 0; index < triangles.size(); index++) {
            float[] triangle = triangles.get(index);
            float minimum = Math.min(triangle[axis], Math.min(triangle[2 + axis], triangle[4 + axis]));
            float maximum = Math.max(triangle[axis], Math.max(triangle[2 + axis], triangle[4 + axis]));
            if (maximum >= lower && minimum <= upper) result.add(index);
        }
        return result;
    }
}
