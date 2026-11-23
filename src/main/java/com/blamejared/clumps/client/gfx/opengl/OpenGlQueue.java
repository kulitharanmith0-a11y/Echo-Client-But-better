package com.blamejared.clumps.client.gfx.opengl;

import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.queue.CommandQueue;
import static org.lwjgl.opengl.GL11C.glFinish;

public class OpenGlQueue extends CommandQueue {
    private final OpenGlDevice device;
    private final int NativeCapabilities;

    OpenGlQueue(OpenGlDevice device, int NativeCapabilities) {
        this.device = device;
        this.NativeCapabilities = NativeCapabilities;
    }

    @Override public int NativeCapabilities() { return NativeCapabilities; }
    @Override public CommandEncoder beginCommands() { return device.commandEncoder(); }

    @Override
    public void submit(CommandEncoder commands) {
    }

    @Override public void waitIdle() { glFinish(); }

    @Override
    public void destroy() {
    }

    static int supportedCapabilities(OpenGlDevice device) {
        return 13 | (device.NativeCapabilities().computeShaders() ? 2 : 0);
    }
}
