package com.blamejared.clumps.client.gfx.command;

import com.blamejared.clumps.client.gfx.buffer.BufferAccess;
import com.blamejared.clumps.client.gfx.buffer.BufferSlice;
import com.blamejared.clumps.client.gfx.surface.RenderTargetLease;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureAccess;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureLayout;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

public abstract class CommandEncoder {
    public abstract TransferPass beginTransfer();
    public abstract ComputePass beginComputePass();
    public abstract RenderPass beginExternalRenderPass(Supplier<RenderTargetLease> target);
    public abstract RenderPass beginRenderPass(TextureView color, OptionalInt clearColor, TextureView depth, OptionalDouble clearDepth);
    public abstract void transitionBuffer(BufferSlice slice, int stages, BufferAccess access);
    public abstract void transitionTexture(TextureView view, int stages, TextureAccess access, TextureLayout layout);
}
