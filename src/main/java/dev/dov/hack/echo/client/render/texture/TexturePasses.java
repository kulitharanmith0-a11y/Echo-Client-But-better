package dev.dov.hack.echo.client.render.texture;

import dev.dov.hack.echo.client.render.NativeMemoryWrites;

import dev.dov.hack.echo.client.render.world.DepthTestedWorldGeometry;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import dev.dov.hack.echo.client.render.player.EntityMaterialSettings;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.gfx.FramebufferOperations;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo.Filter;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureLayout;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import dev.dov.hack.echo.client.render.BackdropMode;
import dev.dov.hack.echo.client.render.texture.ItemTextureCache;
import dev.dov.hack.echo.client.render.world.SectionMeshUpdate;

public final class TexturePasses {
    private static final int UNIFORM_BYTES = 64;
    private static GraphicsPipeline gradientPipeline;
    private static GraphicsPipeline blurPipeline;
    private static GraphicsPipeline compositePipeline;
    private static Sampler sampler;
    private static Buffer uniforms;
    private static final BorrowedTextureViewCache framebufferImport = new BorrowedTextureViewCache();
    private static final BorrowedTextureViewCache previewImport = new BorrowedTextureViewCache();
    private static TexturePassResources framebufferTargets;
    private static TexturePassResources previewTargets;
    private static Texture previewOutput;

    private TexturePasses() {
        super();
    }

    
    public static void applyMaterial(EntityMaterialSettings material, Framebuffer target) {
        RenderDevice device = EchoClient.renderDevice;
        if (device == null || EchoClient.commandEncoder == null) {
            return;
        }
        TextureView color = framebufferImport.get(device, target.getColorAttachmentView());
        framebufferTargets = ensureTargets(device, framebufferTargets, target.textureWidth, target.textureHeight);
        renderMaterial(device, material, color, color, framebufferTargets, 0);
    }

    
    public static TextureView renderToTexture(EntityMaterialSettings material, GpuTextureView source, int width, int height, int overrideArgb) {
        RenderDevice device = EchoClient.renderDevice;
        if (device == null || EchoClient.commandEncoder == null) {
            return null;
        }
        TextureView input = previewImport.get(device, source);
        previewTargets = ensureTargets(device, previewTargets, width, height);
        if (previewOutput != null && (previewOutput.width != width || previewOutput.height != height)) {
            device.release(previewOutput);
            previewOutput = null;
        }
        if (previewOutput == null) {
            previewOutput = createTarget(device, width, height);
        }
        renderMaterial(device, material, input, previewOutput.views[0], previewTargets, overrideArgb);
        return previewOutput.views[0];
    }

    private static void renderMaterial(RenderDevice device, EntityMaterialSettings material, TextureView input, TextureView output, TexturePassResources targets, int overrideArgb) {
        ensureResources(device);
        writeUniforms(material, overrideArgb);
        CommandEncoder commands = EchoClient.commandEncoder;
        TextureView outline = targets.outline().views[0];
        TextureView horizontal = targets.horizontalBlur().views[0];
        TextureView vertical = targets.verticalBlur().views[0];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long push = MemoryUtil.memAddress(stack.callocFloat(2));
            sampled(commands, input);
            colorAttachment(commands, outline);
            RenderPass pass = commands.beginRenderPass(outline, OptionalInt.of(0), null, OptionalDouble.empty());
            device.setViewportToTexture(pass, outline);
            pass.setPipeline(gradientPipeline);
            pass.setBuffer(0, uniforms.wholeSlice());
            pass.setTexture(1, input);
            pass.setSampler(2, sampler);
            NativeMemoryWrites.putFloat(push, input.yFlipped ? 1.0F : 0.0F);
            pass.setPushConstants(push, 4);
            pass.draw(4, 1, 0);

            blur(device, pass, commands, outline, horizontal, push, 1.0F, 0.0F);
            blur(device, pass, commands, horizontal, vertical, push, 0.0F, 1.0F);
            sampled(commands, outline);
            sampled(commands, vertical);
            colorAttachment(commands, output);
            pass.setAttachments(output, null);
            device.setViewportToTexture(pass, output);
            pass.setPipeline(compositePipeline);
            pass.setBuffer(0, uniforms.wholeSlice());
            pass.setTexture(1, outline);
            pass.setTexture(2, vertical);
            pass.setSampler(3, sampler);
            pass.draw(4, 1, 0);
            pass.end();
        }
    }

    private static void blur(RenderDevice device, RenderPass pass, CommandEncoder commands, TextureView input, TextureView output, long push, float x, float y) {
        sampled(commands, input);
        colorAttachment(commands, output);
        pass.setAttachments(output, null);
        device.setViewportToTexture(pass, output);
        pass.setPipeline(blurPipeline);
        pass.setBuffer(0, uniforms.wholeSlice());
        pass.setTexture(1, input);
        pass.setSampler(2, sampler);
        NativeMemoryWrites.putFloat2(push, x, y);
        pass.setPushConstants(push, 8);
        pass.draw(4, 1, 0);
    }

    private static void sampled(CommandEncoder commands, TextureView view) {
        commands.transitionTexture(view, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
    }

    private static void colorAttachment(CommandEncoder commands, TextureView view) {
        commands.transitionTexture(view, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
    }

    public static Framebuffer createFramebuffer(String name, int width, int height, boolean useDepth) {
        return FramebufferOperations.create(name, width, height, useDepth);
    }

    
    public static void blitToMainFramebuffer(Framebuffer source) {
        FramebufferOperations.blitColor(source, MinecraftClient.getInstance().getFramebuffer());
    }

    public static void releaseResources() {
        RenderDevice device = EchoClient.renderDevice;
        if (device == null) {
            return;
        }
        if (framebufferTargets != null) {
            framebufferTargets.release(device);
            framebufferTargets = null;
        }
        if (previewTargets != null) {
            previewTargets.release(device);
            previewTargets = null;
        }
        if (previewOutput != null) {
            device.release(previewOutput);
            previewOutput = null;
        }
        if (uniforms != null) {
            device.release(uniforms);
            device.release(sampler);
            device.release(gradientPipeline);
            device.release(blurPipeline);
            device.release(compositePipeline);
            uniforms = null;
            sampler = null;
            gradientPipeline = null;
            blurPipeline = null;
            compositePipeline = null;
        }
    }

    private static void ensureResources(RenderDevice device) {
        if (uniforms != null) {
            return;
        }
        gradientPipeline = device.createGraphicsPipeline(pipelineInfo(device, "gradient", 4));
        blurPipeline = device.createGraphicsPipeline(pipelineInfo(device, "blur", 8));
        compositePipeline = device.createGraphicsPipeline(pipelineInfo(device, "composite", 0));
        sampler = device.createSampler(SamplerInfo.builder().filter(Filter.NEAREST).build());
        uniforms = device.createBuffer(BufferInfo.builder().usage(2).size(UNIFORM_BYTES)
                .mapped(true).dynamic(true).build());
    }

    private static GraphicsPipelineInfo pipelineInfo(RenderDevice device, String name, int pushBytes) {
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/chams/" + name + ".wgsl");
        info.blendEnabled = false;
        info.cullEnabled = false;
        info.depthTestEnabled = false;
        info.depthWriteEnabled = false;
        info.pushConstantBytes = pushBytes;
        return info;
    }

    private static TexturePassResources ensureTargets(RenderDevice device, TexturePassResources current, int width, int height) {
        if (current != null && current.outline().width == width && current.outline().height == height) {
            return current;
        }
        if (current != null) {
            current.release(device);
        }
        return new TexturePassResources(createTarget(device, width, height), createTarget(device, width, height), createTarget(device, width, height));
    }

    private static Texture createTarget(RenderDevice device, int width, int height) {
        return device.createTexture(TextureInfo.builder().width(width).height(height)
                .format(TextureFormat.BGRA8Unorm).usage(9).build());
    }

    private static void writeUniforms(EntityMaterialSettings material, int overrideArgb) {
        boolean override = overrideArgb != 0 && material.usesTeamColors();
        long address = uniforms.mappedAddress();
        NativeMemoryWrites.putFloat(address, material.getLineWidthSetting().getValue());
        NativeMemoryWrites.putFloat(address + 4L, material.getGradientScaleSetting().getValue());
        NativeMemoryWrites.putFloat(address + 8L, material.getFillOpacitySetting().getValue());
        NativeMemoryWrites.putFloat(address + 12L, material.getOutlineOpacitySetting().getValue());
        NativeMemoryWrites.putFloat(address + 16L, material.usesTeamColors() && !override ? 1.0F : 0.0F);
        NativeMemoryWrites.putFloat(address + 20L, material.getAnimationPhase());
        NativeMemoryWrites.putFloat(address + 24L, material.getGlowRadiusSetting().getValue());
        NativeMemoryWrites.putFloat(address + 28L, material.getGlowIntensitySetting().getValue());
        if (override) {
            writeArgb(address + 32L, overrideArgb);
            writeArgb(address + 48L, overrideArgb);
        } else {
            writeColor(address + 32L, material.getColor1Setting());
            writeColor(address + 48L, material.getColor2Setting());
        }
    }

    private static void writeColor(long address, ColorSetting color) {
        NativeMemoryWrites.putFloat4(address, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }

    private static void writeArgb(long address, int argb) {
        NativeMemoryWrites.putFloat4(address, ColorHelper.getRed(argb) / 255.0F, ColorHelper.getGreen(argb) / 255.0F, ColorHelper.getBlue(argb) / 255.0F, ColorHelper.getAlpha(argb) / 255.0F);
    }
}
