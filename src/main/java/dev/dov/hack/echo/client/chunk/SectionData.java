package dev.dov.hack.echo.client.chunk;

import java.util.Arrays;

public final class SectionData {
    public final int sectionX;
    public final int sectionY;
    public final int sectionZ;
    private final int[] faces;
    private final int[] faceCounts;

    public SectionData(int sectionX, int sectionY, int sectionZ, int[] faces, int[] faceCounts) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        this.faces = faces != null ? faces : new int[0];
        this.faceCounts = faceCounts != null ? faceCounts : new int[0];
    }

    public SectionData(int sectionX, int sectionY, int sectionZ) {
        this(sectionX, sectionY, sectionZ, new int[0], new int[0]);
    }

    public FaceList faces() {
        return new FaceList(faces);
    }

    public int[] faceCounts() {
        return faceCounts;
    }

    public static final class FaceList {
        private final int[] data;

        FaceList(int[] data) {
            this.data = data;
        }

        public int[] toIntArray() {
            return Arrays.copyOf(data, data.length);
        }
    }
}
