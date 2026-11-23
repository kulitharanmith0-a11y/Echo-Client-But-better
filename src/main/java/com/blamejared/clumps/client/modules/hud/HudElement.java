package com.blamejared.clumps.client.modules.hud;

import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.HudModule;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.ui.UiSize;
import net.minecraft.client.MinecraftClient;

public abstract class HudElement extends HudModule {
    private final HudElementType elementType;
    private final FloatSetting scale;
    private final FloatSetting opacity;

    protected HudElement(ModuleInfo info, HudElementType elementType) {
        super(info);
        scale = new FloatSetting(CatalogText.literal("Scale"), 1.0f, 0.5f, 2.0f, 0.05f, CatalogText.literal("x"));
        opacity = new FloatSetting(CatalogText.literal("Opacity"), 1.0f, 0.1f, 1.0f, 0.05f);
        this.elementType = elementType;
        configurePosition(elementType.defaultX(), elementType.defaultY(), elementType.defaultWidth(), elementType.defaultHeight(), 1920.0f, 1080.0f, false);
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        scale.tooltip("Appearance");
        opacity.tooltip("Appearance");
    }

    public HudElementType getElementType() { return elementType; }
    public float getScale() { return scale.getValue(); }
    public float getOpacity() { return opacity.getValue(); }

    @Override
    public float getWidth() {
        return HudRenderer.INSTANCE.getElementSize(elementType).width() * getEffectiveScale();
    }

    @Override
    public float getHeight() {
        return HudRenderer.INSTANCE.getElementSize(elementType).height() * getEffectiveScale();
    }

    private float getEffectiveScale() {
        return HudRenderer.INSTANCE.getCanvasScale() * scale.getValue();
    }

    public UiSize measure(HudElementLayout layout) {
        return UiSize.fromDefaults(elementType);
    }

    public void render(HudElementLayout layout) {
        if (shouldRender()) {
            renderAt(layout, getX(), getY());
        }
    }

    public void renderAt(HudElementLayout layout, float x, float y) {
        UiSize size = HudRenderer.INSTANCE.getElementSize(elementType);
        layout.beginElement(elementType, x, y, size.width(), size.height());
        try {
            renderContents(layout);
        } finally {
            layout.endElement();
        }
    }

    protected abstract void renderContents(HudElementLayout layout);

    protected boolean shouldRender() {
        return true;
    }

    protected boolean isRightOfScreenCenter() {
        int viewportWidth = (int) Math.ceil(
                MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2.0);
        return getX() + getWidth() * 0.5f > viewportWidth * 0.5f;
    }

    
    public static void appendBootstrapSettings(HudElement Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.scale);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.opacity);
    }
}
