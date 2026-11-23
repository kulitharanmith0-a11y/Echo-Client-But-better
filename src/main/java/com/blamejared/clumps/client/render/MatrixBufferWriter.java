package com.blamejared.clumps.client.render;

import org.lwjgl.system.MemoryUtil;

public final class MatrixBufferWriter {
    private static String[] memorySentinel;

    static {
        if (getMemorySentinel() == null) {
            setMemorySentinel(new String[5]);
        }
    }

    public static String[] getMemorySentinel() {
        return memorySentinel;
    }

    public static void setMemorySentinel(String[] sentinel) {
        memorySentinel = sentinel;
    }

    private MatrixBufferWriter() {}

    public static void putInt4(long address, int x, int y, int z, int w) {
        NativeMemoryWrites.putInt3(address, x, y, z);
        MemoryUtil.memPutInt(address + 12, w);
    }

    public static int readIntOrZero(long address) {
        return address == 0 ? 0 : MemoryUtil.memGetInt(address);
    }

    public static void putBytes(byte[] source, long address, int length) {
        for (int index = 0; index < length; index++) {
            MemoryUtil.memPutByte(address + index, source[index]);
        }
    }

    public static void putInts(int[] source, long address, int length) {
        for (int index = 0; index < length; index++) {
            MemoryUtil.memPutInt(address + index * 4L, source[index]);
        }
    }

    public static void putLongs(long[] source, long address, int length) {
        for (int index = 0; index < length; index++) {
            MemoryUtil.memPutLong(address + index * 8L, source[index]);
        }
    }
}
