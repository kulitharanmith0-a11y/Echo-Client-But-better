package dev.dov.hack.echo.client.gfx;

public final class VoxelAllocationSizes {
    private VoxelAllocationSizes() {
    }

    
    public static int sizeClassFor(int elementCount) {
        return Math.max(4, 32 - Integer.numberOfLeadingZeros(elementCount - 1));
    }
}
