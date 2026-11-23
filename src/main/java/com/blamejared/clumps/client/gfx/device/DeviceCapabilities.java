package com.blamejared.clumps.client.gfx.device;

public record DeviceCapabilities(boolean multiDrawIndirect, boolean computeShaders, boolean sampledImageArrays, boolean storageImages, boolean reversedDepth) {}
