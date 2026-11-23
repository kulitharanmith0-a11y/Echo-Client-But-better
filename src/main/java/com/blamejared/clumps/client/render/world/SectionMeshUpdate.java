package com.blamejared.clumps.client.render.world;

public record SectionMeshUpdate(int operation, int sectionIndex, int[] packedFaces, int[] faceCounts, int sectionX, int sectionY, int sectionZ) {
    public static final int UPSERT = 0;
    public static final int REMOVE = 1;
    public static final int CLEAR = 2;
}
