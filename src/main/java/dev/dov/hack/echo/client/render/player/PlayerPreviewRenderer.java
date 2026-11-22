package dev.dov.hack.echo.client.render.player;

import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.render.bridge.GlTextureBridge;
import dev.dov.hack.echo.client.render.texture.TexturePasses;
import dev.dov.hack.echo.client.utils.Rotation;

import dev.dov.hack.echo.client.render.texture.ItemTextureCache;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.ui.UiBounds;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PlayerPreviewRenderer {
    private static Request pendingRequest;
    private static boolean resultAvailable;
    private static boolean materialRequested;
    private static final TextureCache baseTexture = new TextureCache();
    private static TextureView materialTexture;
    private static UiBounds bounds;
    private static UiBounds[] projectedFaces;
    private static float eyeY;

    private PlayerPreviewRenderer() {
        super();
    }

    private record Request(float width, float height, float pixelScale, float modelHeight, float modelBaseY, float mouseX, float mouseY, EntityMaterialSettings material, int teamColor) {
    }

    private static final class TextureCache {
        private GpuTextureView source;
        private TextureView texture;

        private void update(GpuTextureView nextSource) {
            if (source != nextSource) {
                source = nextSource;
                texture = GlTextureBridge.importColor(EchoClient.renderDevice, nextSource);
            }
        }
    }

    public static void requestRender(float width, float height, float pixelScale, float modelHeight, float modelBaseY, float mouseX, float mouseY, EntityMaterialSettings material, int teamColor) {
        pendingRequest = new Request(width, height, pixelScale, modelHeight, modelBaseY, mouseX, mouseY, material, teamColor);
    }

    public static TextureView getBaseTexture() {
        return resultAvailable ? baseTexture.texture : null;
    }

    public static TextureView getMaterialTexture() {
        return resultAvailable && materialRequested ? materialTexture : null;
    }

    public static UiBounds getBounds() {
        return resultAvailable ? bounds : null;
    }

    public static UiBounds[] getProjectedFaces() {
        return resultAvailable ? projectedFaces : null;
    }

    
    public static float getEyeY() {
        return eyeY;
    }

    
    public static void beginFrame() {
        Request request = pendingRequest;
        pendingRequest = null;
        if (request == null) {
            return;
        }
        resultAvailable = false;
        if (EchoClient.renderDevice == null) {
            return;
        }
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        int textureWidth = Math.min(2048, Math.round(request.width() * request.pixelScale()));
        int textureHeight = Math.min(2048, Math.round(request.height() * request.pixelScale()));
        if (textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        EntityRenderState state = EntityPreviewRenderer.prepare(player);
        float mouseDivisor = Math.max(1.0F, request.modelHeight() * 0.74F);
        float yaw = (float) Math.atan((request.width() * 0.5F - request.mouseX()) / mouseDivisor);
        float pitch = (float) Math.atan(
                (request.modelBaseY() - request.modelHeight() * 0.5F - request.mouseY()) / mouseDivisor);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = 180.0F + yaw * 20.0F;
            living.relativeHeadYaw = yaw * 20.0F;
            living.pitch = living.pose == EntityPose.GLIDING ? 0.0F : -pitch * 20.0F;
            living.width /= living.baseScale;
            living.height /= living.baseScale;
            living.baseScale = 1.0F;
        }
        float pitchRadians = pitch * 20.0F * 0.017453292F;
        float entityHeight = Math.max(state.height, 0.1F);
        float logicalScale = request.modelHeight() / entityHeight;
        float verticalOffset = (request.modelBaseY() - request.height() * 0.5F) / logicalScale
                - entityHeight * 0.5F;
        float pixelsPerUnit = logicalScale * request.pixelScale();
        Quaternionf rotation = new Quaternionf().rotateZ(3.1415927F)
                .mul(new Quaternionf().rotateX(pitchRadians));
        Vector3f translation = new Vector3f(0.0F, entityHeight / 2.0F + verticalOffset, 0.0F);

        GpuTextureView rendered = EntityPreviewRenderer.render(state, textureWidth, textureHeight, pixelsPerUnit, translation, rotation);
        baseTexture.update(rendered);
        materialRequested = request.material() != null;
        if (materialRequested) {
            materialTexture = TexturePasses.renderToTexture(request.material(), rendered, textureWidth, textureHeight, request.teamColor());
        }
        updateProjectedBounds(request, state, pitchRadians, entityHeight, logicalScale, verticalOffset);
        resultAvailable = true;
    }

    private static void updateProjectedBounds(Request request, EntityRenderState state, float pitchRadians, float entityHeight, float logicalScale, float verticalOffset) {
        float halfWidth = Math.max(state.width, 0.1F) * 0.5F;
        float sin = (float) Math.sin(pitchRadians);
        float cos = (float) Math.cos(pitchRadians);
        float center = entityHeight * 0.5F + verticalOffset;
        float left = request.width() * 0.5F - halfWidth * logicalScale;
        float projectedWidth = halfWidth * 2.0F * logicalScale;
        float projectedHeight = entityHeight * cos * logicalScale;
        float baseline = request.height() * 0.5F + center * logicalScale;
        float depthOffset = halfWidth * sin * logicalScale;
        UiBounds front = new UiBounds(left, baseline - depthOffset - projectedHeight, projectedWidth, projectedHeight);
        UiBounds back = new UiBounds(left, baseline + depthOffset - projectedHeight, projectedWidth, projectedHeight);
        projectedFaces = new UiBounds[]{front, back};
        float top = Math.min(front.y(), back.y());
        float bottom = Math.max(front.bottom(), back.bottom());
        bounds = new UiBounds(left, top, projectedWidth, bottom - top);
        eyeY = request.height() * 0.5F
                + (center - state.standingEyeHeight * cos) * logicalScale;
    }
}
