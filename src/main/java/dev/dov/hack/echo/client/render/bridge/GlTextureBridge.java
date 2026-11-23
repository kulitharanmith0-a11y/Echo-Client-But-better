package dev.dov.hack.echo.client.render.bridge;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.opengl.OpenGlDevice;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureLayout;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import net.minecraft.client.texture.GlTexture;

public final class GlTextureBridge {
    private static final int COLOR_ATTACHMENT_STAGE = 2;
    private static final int DEPTH_ATTACHMENT_STAGE = 8;

    private GlTextureBridge() {}

    public static boolean isYFlipped() {
        return true;
    }

    public static TextureView importColor(RenderDevice device, GpuTextureView source) {
        return importView(device, source, TextureFormat.BGRA8Unorm);
    }

    private static TextureView importStorage(RenderDevice device, GpuTextureView source, TextureFormat format) {
        if (source == null || source.texture() == null) {
            return null;
        }
        int width = Math.max(1, source.getWidth(0));
        int height = Math.max(1, source.getHeight(0));
        if (device instanceof OpenGlDevice openGl && source.texture() instanceof GlTexture texture) {
            return openGl.importTextureView(texture.getGlId(), width, height, format, GlConst.toGlInternalId(texture.getFormat()));
        }
        return null;
    }

    public static TextureView importView(RenderDevice device, GpuTextureView source, TextureFormat format) {
        TextureView view = importStorage(device, source, format);
        if (view == null) {
            return null;
        }
        view.access = format.depth ? TextureAccess.DEPTH_ATTACHMENT : TextureAccess.COLOR_ATTACHMENT;
        view.layout = TextureLayout.GENERAL;
        view.stages = format.depth ? DEPTH_ATTACHMENT_STAGE : COLOR_ATTACHMENT_STAGE;
        view.yFlipped = isYFlipped();
        return view;
    }
}
