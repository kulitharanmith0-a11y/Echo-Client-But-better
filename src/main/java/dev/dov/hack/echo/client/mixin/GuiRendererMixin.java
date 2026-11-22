package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.platform.Platform;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.render.HudRenderEvent;
import dev.dov.hack.echo.client.handlers.rotation.RotationPriority;
import dev.dov.hack.echo.client.render.bridge.DrawContextAdapter;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.render.FramebufferBlur;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.gui.ClientScreen;
import dev.dov.hack.echo.client.render.player.PlayerPreviewRenderer;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements Platform.GuiLoop {
    @Shadow @Final private GuiRenderState state;
    @Unique private boolean echo$mainGuiPass;
    @Unique private boolean echo$pendingHud;
    @Unique private boolean echo$drawHudBeforeBlur;
    @Unique private boolean echo$mirrorDraws;

    
    @Unique
    private static boolean echo$shouldDeferHud() {
        try {
            Class.forName(RotationPriority.class.getName(), true, RotationPriority.class.getClassLoader());
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException missingResolvedClass) {
            throw new IllegalStateException("Resolved class initialization failed", missingResolvedClass);
        }
        Screen screen = MinecraftRenderState.getScreen();
        if (screen instanceof ClientScreen) {
            return true;
        }
        return screen == null && !MinecraftRenderState.hasOverlay();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void prepareHudPass(CallbackInfo callback) {
        echo$pendingHud = false;
        echo$drawHudBeforeBlur = false;
        echo$mirrorDraws = false;
        if (echo$mainGuiPass && !EchoClient.eventsDisabled && EchoClient.uiRenderer != null && EchoClient.commandEncoder != null
                && MinecraftClient.getInstance().world != null) {
            echo$mirrorDraws = false;
            if (MinecraftRenderState.getScreen() instanceof ClientScreen) {
                EchoClient.uiRenderer.updateBackdrop(EchoClient.commandEncoder);
            }
            EchoClient.uiRenderer.renderPendingItems();
            PlayerPreviewRenderer.beginFrame();
            if (!echo$shouldDeferHud() && MinecraftRenderState.hasOverlay()) {
                echo$renderHud(false);
            } else {
                echo$pendingHud = true;
                if (!echo$shouldDeferHud()) {
                    echo$drawHudBeforeBlur = ((Platform.GuiState) state).prepareScreenBlurLayer();
                }
            }
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"))
    private void renderHudAtBlur(GameRenderer renderer, Operation<Void> original) {
        if (echo$drawHudBeforeBlur) {
            echo$drawHudBeforeBlur = false;
            echo$pendingHud = false;
            if (EchoClient.externalRendererActive && EchoClient.externalRenderer != null) {
                // In StreamProof mode, let vanilla screens (Esc / Settings / Inventory) render cleanly
                // onto Minecraft's main framebuffer without double-blurring or mirroring onto FramebufferBlur.
                echo$mirrorDraws = false;
            } else {
                echo$renderHud(false);
            }
        }
        if (!((Platform.GuiState) state).isScreenBlurLayerInstalled()) {
            original.call(renderer);
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 1))
    private void mirrorGuiDraw(GuiRenderer renderer, Supplier<String> label, Framebuffer target, GpuBufferSlice dynamicTransforms, GpuBufferSlice fog, GpuBuffer indices, VertexFormat.IndexType indexType, int start, int end, Operation<Void> original) {
        original.call(renderer, label, target, dynamicTransforms, fog, indices, indexType, start, end);
        if (echo$mirrorDraws && EchoClient.externalRenderer != null) {
            Framebuffer overlay = EchoClient.externalRenderer.framebufferBlur().framebuffer();
            if (overlay.getDepthAttachment() != null) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(overlay.getDepthAttachment(), 1.0);
            }
            original.call(renderer, label, overlay, dynamicTransforms, fog, indices, indexType, start, end);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void finishPendingHud(CallbackInfo callback) {
        if (echo$pendingHud) {
            echo$pendingHud = false;
            echo$drawHudBeforeBlur = false;
            Screen current = MinecraftRenderState.getScreen();
            boolean isClientScreen = current instanceof ClientScreen;
            if (!EchoClient.externalRendererActive || current == null || isClientScreen) {
                echo$renderHud(isClientScreen);
            }
        }
        echo$mirrorDraws = false;
    }

    @Unique
    private void echo$renderHud(boolean updateBackdrop) {
        UiRenderer renderer = EchoClient.uiRenderer;
        if (updateBackdrop) {
            renderer.updateBackdrop();
        }
        renderer.beginFrame();
        echo$postHudEvent();
        renderer.endFrame();
    }

    @Unique
    private void echo$renderHudToOverlay() {
        FramebufferBlur overlay = EchoClient.externalRenderer.framebufferBlur();
        overlay.captureMainFramebuffer();
        CommandEncoder encoder = EchoClient.commandEncoder;
        EchoClient.worldRenderer.compositeExternalOverlays(encoder, overlay.colorTexture(), OptionalInt.empty());
        EchoClient.worldRenderer.beginTarget(encoder, overlay.colorTexture(), OptionalInt.empty());
        EchoClient.uiRenderer.beginFrame(encoder, overlay.colorTexture(), overlay.depthTexture(), OptionalInt.empty());
        echo$postHudEvent();
        EchoClient.uiRenderer.endFrame();
    }

    @Unique
    private void echo$postHudEvent() {
        MinecraftClient client = MinecraftClient.getInstance();
        int mouseX = (int) client.mouse.getScaledX(client.getWindow());
        int mouseY = (int) client.mouse.getScaledY(client.getWindow());
        DrawContext drawContext = new DrawContext(client, state, mouseX, mouseY);
        UiRenderer renderer = EchoClient.uiRenderer;
        new HudRenderEvent(new MatrixStack(), client.getRenderTickCounter().getTickProgress(true), DrawContextAdapter.wrap(drawContext), renderer, renderer.backdropTexture).post();
    }

    @Override
    public void setMainGuiPass(boolean mainGuiPass) {
        echo$mainGuiPass = mainGuiPass;
    }
}
