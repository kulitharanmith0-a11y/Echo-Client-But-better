package com.blamejared.clumps.client.mixin;
import com.blamejared.clumps.client.platform.Platform;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.render.bridge.DrawContextAdapter;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.FramebufferBlur;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.ClientScreen;
import com.blamejared.clumps.client.render.player.PlayerPreviewRenderer;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
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
    @Unique private boolean clumps$mainGuiPass;
    @Unique private boolean clumps$pendingHud;
    @Unique private boolean clumps$drawHudBeforeBlur;
    @Unique private boolean clumps$mirrorDraws;

    
    @Unique
    private static boolean clumps$shouldDeferHud() {
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
        clumps$pendingHud = false;
        clumps$drawHudBeforeBlur = false;
        clumps$mirrorDraws = false;
        if (clumps$mainGuiPass && !ClumpsClient.eventsDisabled && ClumpsClient.uiRenderer != null && ClumpsClient.commandEncoder != null
                && MinecraftClient.getInstance().world != null) {
            clumps$mirrorDraws = false;
            if (MinecraftRenderState.getScreen() instanceof ClientScreen) {
                ClumpsClient.uiRenderer.updateBackdrop(ClumpsClient.commandEncoder);
            }
            try {
                ClumpsClient.uiRenderer.renderPendingItems();
            } catch (Throwable ignored) {
            }
            PlayerPreviewRenderer.beginFrame();
            if (!clumps$shouldDeferHud() && MinecraftRenderState.hasOverlay()) {
                clumps$renderHud(false);
            } else {
                clumps$pendingHud = true;
                if (!clumps$shouldDeferHud()) {
                    clumps$drawHudBeforeBlur = ((Platform.GuiState) state).prepareScreenBlurLayer();
                }
            }
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderBlur()V"))
    private void renderHudAtBlur(GameRenderer renderer, Operation<Void> original) {
        if (clumps$drawHudBeforeBlur) {
            clumps$drawHudBeforeBlur = false;
            clumps$pendingHud = false;
            if (ClumpsClient.externalRendererActive && ClumpsClient.externalRenderer != null) {
                // In StreamProof mode, let vanilla screens (Esc / Settings / Inventory) render cleanly
                // onto Minecraft's main framebuffer without double-blurring or mirroring onto FramebufferBlur.
                clumps$mirrorDraws = false;
            } else {
                clumps$renderHud(false);
            }
        }
        if (!((Platform.GuiState) state).isScreenBlurLayerInstalled()) {
            original.call(renderer);
        }
    }

    @WrapOperation(method = "renderPreparedDraws", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", ordinal = 1))
    private void mirrorGuiDraw(GuiRenderer renderer, Supplier<String> label, Framebuffer target, GpuBufferSlice dynamicTransforms, GpuBufferSlice fog, GpuBuffer indices, VertexFormat.IndexType indexType, int start, int end, Operation<Void> original) {
        original.call(renderer, label, target, dynamicTransforms, fog, indices, indexType, start, end);
        if (clumps$mirrorDraws && ClumpsClient.externalRenderer != null) {
            Framebuffer overlay = ClumpsClient.externalRenderer.framebufferBlur().framebuffer();
            if (overlay.getDepthAttachment() != null) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(overlay.getDepthAttachment(), 1.0);
            }
            original.call(renderer, label, overlay, dynamicTransforms, fog, indices, indexType, start, end);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void finishPendingHud(CallbackInfo callback) {
        if (clumps$pendingHud) {
            clumps$pendingHud = false;
            clumps$drawHudBeforeBlur = false;
            Screen current = MinecraftRenderState.getScreen();
            boolean isClientScreen = current instanceof ClientScreen;
            if (!ClumpsClient.externalRendererActive || current == null || isClientScreen) {
                clumps$renderHud(isClientScreen);
            }
        }
        clumps$mirrorDraws = false;
    }

    @Unique
    private void clumps$renderHud(boolean updateBackdrop) {
        UiRenderer renderer = ClumpsClient.uiRenderer;
        if (ClumpsClient.eventsDisabled || renderer == null || ClumpsClient.commandEncoder == null) {
            return;
        }
        if (updateBackdrop) {
            renderer.updateBackdrop();
        }
        renderer.beginFrame();
        clumps$postHudEvent();
        renderer.endFrame();
    }

    @Unique
    private void clumps$renderHudToOverlay() {
        FramebufferBlur overlay = ClumpsClient.externalRenderer.framebufferBlur();
        overlay.captureMainFramebuffer();
        CommandEncoder encoder = ClumpsClient.commandEncoder;
        ClumpsClient.worldRenderer.compositeExternalOverlays(encoder, overlay.colorTexture(), OptionalInt.empty());
        ClumpsClient.worldRenderer.beginTarget(encoder, overlay.colorTexture(), OptionalInt.empty());
        ClumpsClient.uiRenderer.beginFrame(encoder, overlay.colorTexture(), overlay.depthTexture(), OptionalInt.empty());
        clumps$postHudEvent();
        ClumpsClient.uiRenderer.endFrame();
    }

    @Unique
    private void clumps$postHudEvent() {
        MinecraftClient client = MinecraftClient.getInstance();
        int mouseX = (int) client.mouse.getScaledX(client.getWindow());
        int mouseY = (int) client.mouse.getScaledY(client.getWindow());
        DrawContext drawContext = new DrawContext(client, state, mouseX, mouseY);
        UiRenderer renderer = ClumpsClient.uiRenderer;
        new HudRenderEvent(new MatrixStack(), client.getRenderTickCounter().getTickProgress(true), DrawContextAdapter.wrap(drawContext), renderer, renderer.backdropTexture).post();
    }

    @Override
    public void setMainGuiPass(boolean mainGuiPass) {
        clumps$mainGuiPass = mainGuiPass;
    }
}
