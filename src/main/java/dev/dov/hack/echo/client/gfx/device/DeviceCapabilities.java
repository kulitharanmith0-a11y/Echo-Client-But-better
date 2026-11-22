package dev.dov.hack.echo.client.gfx.device;

public record DeviceCapabilities(boolean multiDrawIndirect, boolean computeShaders, boolean sampledImageArrays, boolean storageImages, boolean reversedDepth) {}
