package dev.dov.hack.echo.client.gui;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.HudRenderEvent;
import dev.dov.hack.echo.client.event.render.InGameHudEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.HudModule;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.ui.GuiDimensions;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.render.font.FontAsset;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class HudEditorScreen extends ClientScreen {
    private static final int GRID_MAJOR_INTERVAL = 10;
    private static final int GRID_COLOR = 184549375;
    private static final int GRID_MAJOR_COLOR = 419430399;
    private static final int BACKGROUND_COLOR = -1476395008;
    private static final int OUTLINE_COLOR = 1509949439;
    private static final int ACTIVE_OUTLINE_COLOR = -1;
    private static final int GUIDE_COLOR = -11756801;
    private static final float SNAP_DISTANCE = 8.0F;
    private static final String HUD_TITLE = "Modify HUD";
    private static final String AUTOSAVE_PROFILE = "_autosave";

    private HudModule hoveredElement;
    private HudModule draggedElement;
    private HudModule selectedElement;
    private float snapGuideX;
    private float snapGuideY;
    private float dragOffsetX;
    private float dragOffsetY;
    private float mouseX;
    private float mouseY;

    
    public HudEditorScreen() {
        super(Text.literal(HUD_TITLE));
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
    }

    
    @Override
    protected void init() {
        super.init();
        EventBus.register(this);
    }

    
    @Override
    public void removed() {
        super.removed();
        EventBus.unregister(this);
        draggedElement = null;
        hoveredElement = null;
        selectedElement = null;
        if (EchoClient.ModuleConfig != null) {
            EchoClient.ModuleConfig.saveAsync(AUTOSAVE_PROFILE);
        }
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        this.mouseX = toUiX(mouseX);
        this.mouseY = toUiY(mouseY);
        if (draggedElement != null) {
            return;
        }
        hoveredElement = elementAt(this.mouseX, this.mouseY);
    }

    
    @Subscribe
    public void onInGameHudRender(InGameHudEvent event) {
        event.cancel();
    }

    
    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        UiRenderer renderer = event.getRenderer();
        float width = GuiDimensions.width();
        float height = GuiDimensions.height();
        renderer.drawBackdrop(renderer.halfResolutionBackdrop, 0.0F, 0.0F, width, height, 0.0F, 1.0F);
        renderer.drawRoundedRect(0.0F, 0.0F, width, height, 0.0F, BACKGROUND_COLOR);
        drawGrid(renderer, width, height);
        HudRenderer.INSTANCE.prepareHudFrame(renderer);
        for (HudModule Module : enabledElements()) {
            HudElement element = (HudElement) Module;
            HudElementLayout layout = HudRenderer.INSTANCE.createLayout(renderer, renderer.fullResolutionBackdrop, element.getScale(), element.getOpacity());
            element.renderAt(layout, Module.getX(), Module.getY());
        }
        for (HudModule Module : enabledElements()) {
            drawElementOutline(renderer, Module);
        }
        if (draggedElement != null) {
            drawSnapGuides(renderer, width, height);
        }
        HudModule active = draggedElement != null ? draggedElement
                : selectedElement != null ? selectedElement : hoveredElement;
        if (active != null) {
            drawAnchor(renderer, active);
        }
    }

    private void drawSnapGuides(UiRenderer renderer, float width, float height) {
        if (!Float.isNaN(snapGuideX)) {
            renderer.drawRoundedRect(MathHelper.clamp(snapGuideX - 0.5F, 0.0F, width - 1.0F), 0.0F, 1.0F, height, 0.0F, GUIDE_COLOR);
        }
        if (!Float.isNaN(snapGuideY)) {
            renderer.drawRoundedRect(0.0F, MathHelper.clamp(snapGuideY - 0.5F, 0.0F, height - 1.0F), width, 1.0F, 0.0F, GUIDE_COLOR);
        }
    }

    private void drawAnchor(UiRenderer renderer, HudModule Module) {
        float radius = Math.max(2.0F, 4.0F * HudRenderer.INSTANCE.getCanvasScale());
        float x = Module.getX() + Module.getWidth() * Module.getHorizontalAnchor();
        float y = Module.getY() + Module.getHeight() * Module.getVerticalAnchor();
        renderer.drawRoundedRect(x - radius, y - radius, radius * 2.0F, radius * 2.0F, radius, GUIDE_COLOR, 1.0F, ACTIVE_OUTLINE_COLOR);
    }

    private void drawGrid(UiRenderer renderer, float width, float height) {
        float spacing = 10.0F * HudRenderer.INSTANCE.getCanvasScale();
        if (spacing <= 0.0F) {
            return;
        }
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        for (int index = -MathHelper.floor(centerX / spacing);
                centerX + index * spacing <= width; index++) {
            renderer.drawRoundedRect(centerX + index * spacing - 0.5F, 0.0F, 1.0F, height, 0.0F, index % GRID_MAJOR_INTERVAL == 0 ? GRID_MAJOR_COLOR : GRID_COLOR);
        }
        for (int index = -MathHelper.floor(centerY / spacing);
                centerY + index * spacing <= height; index++) {
            renderer.drawRoundedRect(0.0F, centerY + index * spacing - 0.5F, width, 1.0F, 0.0F, index % GRID_MAJOR_INTERVAL == 0 ? GRID_MAJOR_COLOR : GRID_COLOR);
        }
    }

    private void drawElementOutline(UiRenderer renderer, HudModule Module) {
        boolean active = Module == draggedElement || Module == hoveredElement || Module == selectedElement;
        float x = Module.getX();
        float y = Module.getY();
        float width = Module.getWidth();
        float height = Module.getHeight();
        renderer.drawRoundedRect(x, y, width, height, 6.0F, 0, active ? 2.0F : 1.0F, active ? ACTIVE_OUTLINE_COLOR : OUTLINE_COLOR);
        if (!active) {
            return;
        }
        CharSequence name = Module.getName();
        float scale = HudRenderer.INSTANCE.getCanvasScale();
        float fontSize = Math.max(5.5F, 11.0F * scale);
        float textWidth = renderer.measureText(FontAsset.INTER_SEMI_BOLD, name, fontSize);
        float labelHeight = 18.0F * scale;
        float labelY = Math.max(3.0F, y - 20.0F * scale);
        renderer.drawRoundedRect(x, labelY, textWidth + 12.0F * scale, labelHeight, 4.0F * scale, -267119596);
        float fontHeight = renderer.measureFontHeight(FontAsset.INTER_SEMI_BOLD, fontSize);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, name, x + 6.0F * scale, labelY + (labelHeight - fontHeight) * 0.5F, fontSize, -1);
    }

    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!InputKey.MOUSE_LEFT.matches(click.button())) {
            return super.mouseClicked(click, doubled);
        }
        float x = toUiX((float) click.x());
        float y = toUiY((float) click.y());
        draggedElement = elementAt(x, y);
        selectedElement = draggedElement;
        if (draggedElement == null) {
            return super.mouseClicked(click, doubled);
        }
        dragOffsetX = x - draggedElement.getX();
        dragOffsetY = y - draggedElement.getY();
        return true;
    }

    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggedElement == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        float x = toUiX((float) click.x()) - dragOffsetX;
        float y = toUiY((float) click.y()) - dragOffsetY;
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        if (!InputKey.LEFT_ALT.isPressed() && !InputKey.RIGHT_ALT.isPressed()) {
            float distance = SNAP_DISTANCE * HudRenderer.INSTANCE.getCanvasScale();
            x = snapAxis(x, draggedElement.getWidth(), true, distance);
            y = snapAxis(y, draggedElement.getHeight(), false, distance);
        }
        moveWithinViewport(draggedElement, x, y);
        return true;
    }

    
    @Override
    public boolean keyPressed(KeyInput input) {
        HudModule Module = selectedElement != null ? selectedElement : hoveredElement;
        if (Module == null || draggedElement != null) {
            return super.keyPressed(input);
        }
        float step = (input.hasShift() ? 10.0F : 1.0F) / 2.0F;
        float dx = input.isLeft() ? -step : input.isRight() ? step : 0.0F;
        float dy = input.isUp() ? -step : input.isDown() ? step : 0.0F;
        if (dx == 0.0F && dy == 0.0F) {
            return super.keyPressed(input);
        }
        moveWithinViewport(Module, Module.getX() + dx, Module.getY() + dy);
        return true;
    }

    private static void moveWithinViewport(HudModule Module, float x, float y) {
        float maxX = Math.max(0.0F, GuiDimensions.width() - Module.getWidth());
        float maxY = Math.max(0.0F, GuiDimensions.height() - Module.getHeight());
        Module.setPosition(MathHelper.clamp(x, 0.0F, maxX), MathHelper.clamp(y, 0.0F, maxY));
    }

    private float snapAxis(float position, float size, boolean horizontal, float threshold) {
        List<HudModule> elements = enabledElements();
        float nearest = threshold;
        float offset = 0.0F;
        float guide = Float.NaN;
        for (int index = -1; index < elements.size(); index++) {
            float targetPosition = 0.0F;
            float targetSize = horizontal ? GuiDimensions.width() : GuiDimensions.height();
            if (index >= 0) {
                HudModule target = elements.get(index);
                if (target == draggedElement) {
                    continue;
                }
                targetPosition = horizontal ? target.getX() : target.getY();
                targetSize = horizontal ? target.getWidth() : target.getHeight();
            }
            for (int targetAnchor = 0; targetAnchor <= 2; targetAnchor++) {
                float targetCoordinate = targetPosition + targetSize * targetAnchor * 0.5F;
                for (int movingAnchor = 0; movingAnchor <= 2; movingAnchor++) {
                    float delta = targetCoordinate - (position + size * movingAnchor * 0.5F);
                    if (Math.abs(delta) < nearest) {
                        nearest = Math.abs(delta);
                        offset = delta;
                        guide = targetCoordinate;
                    }
                }
            }
        }
        if (horizontal) {
            snapGuideX = guide;
        } else {
            snapGuideY = guide;
        }
        return position + offset;
    }

    
    @Override
    public boolean mouseReleased(Click click) {
        if (draggedElement == null) {
            return super.mouseReleased(click);
        }
        draggedElement = null;
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        return true;
    }

    private HudModule elementAt(float x, float y) {
        List<HudModule> elements = enabledElements();
        for (int index = elements.size() - 1; index >= 0; index--) {
            HudModule element = elements.get(index);
            if (x < element.getX()) continue;
            if (!(x <= element.getX() + element.getWidth())) continue;
            if (y < element.getY()) continue;
            if (!(y <= element.getY() + element.getHeight())) continue;
            return element;
        }
        return null;
    }

    private static HudElementType elementType(HudModule Module) {
        return ((HudElement) Module).getElementType();
    }

    private static List<HudModule> enabledElements() {
        List<HudModule> result = new ArrayList<>();
        for (Module Module : ModuleManager.allFeatures()) {
            if (Module instanceof HudElement element && element.isEnabled()) {
                result.add(element);
            }
        }
        return result;
    }

    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
