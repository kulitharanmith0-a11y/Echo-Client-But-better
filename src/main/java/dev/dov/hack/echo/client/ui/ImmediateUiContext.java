package dev.dov.hack.echo.client.ui;

import dev.dov.hack.echo.client.keyboard.InputKey;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ImmediateUiContext {
    private static final int PRIMARY_BUTTON = InputKey.MOUSE_LEFT.code();
    private final InputState input = new InputState();
    private final Deque<UiId> ids = new ArrayDeque<>();
    private final Deque<UiRect> clips = new ArrayDeque<>();
    private LayoutMetrics canvas = LayoutMetrics.fit(1920.0f, 1080.0f, 1920.0f, 1080.0f);
    private UiId hoveredId = UiId.NONE;
    private UiId activeId = UiId.NONE;
    private UiId focusedId = UiId.NONE;
    private boolean enabled = true;
    private boolean pressConsumed;
    private boolean releaseConsumed;

    
    public void beginFrame(float screenWidth, float screenHeight, float logicalWidth, float logicalHeight) {
        canvas = LayoutMetrics.fit(screenWidth, screenHeight, logicalWidth, logicalHeight);
        hoveredId = UiId.NONE;
        pressConsumed = false;
        releaseConsumed = false;
        ids.clear();
        ids.push(UiId.ROOT);
        clips.clear();
        clips.push(new UiRect(0.0f, 0.0f, logicalWidth, logicalHeight));
    }

    
    public void endFrame() {
        if (input.wasReleased(PRIMARY_BUTTON) && !releaseConsumed) {
            activeId = UiId.NONE;
        }
        input.endFrame();
        ids.clear();
        clips.clear();
    }

    
    public Interaction interact(CharSequence label, UiRect bounds) {
        return interact(id(label), bounds);
    }

    
    public Interaction interact(UiId id, UiRect bounds) {
        if (!enabled || clips.isEmpty()) {
            return Interaction.NONE;
        }
        float mouseX = mouseX();
        float mouseY = mouseY();
        UiRect clipped = bounds.intersection(clips.peek());
        boolean hovered = clipped.width() > 0.0f && clipped.height() > 0.0f
                && clipped.contains(mouseX, mouseY);
        if (hovered) {
            hoveredId = id;
        }
        boolean pressed = hovered && input.wasPressed(PRIMARY_BUTTON) && !pressConsumed;
        if (pressed) {
            activeId = id;
            pressConsumed = true;
        }
        boolean held = activeId.equals(id) && input.isDown(PRIMARY_BUTTON);
        boolean released = activeId.equals(id) && input.wasReleased(PRIMARY_BUTTON) && !releaseConsumed;
        boolean clicked = released && hovered;
        if (released) {
            releaseConsumed = true;
            activeId = UiId.NONE;
        }
        return new Interaction(hovered, held, pressed, released, clicked);
    }

    
    public UiId id(CharSequence label) {
        return ids.peek().child(label);
    }

    
    public void pushId(CharSequence label) {
        ids.push(id(label));
    }

    
    public void pushId(int value) {
        ids.push(ids.peek().child(value));
    }

    
    public void popId() {
        if (ids.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root widget ID");
        }
        ids.pop();
    }

    
    public void pushClip(UiRect clip) {
        clips.push(clip.intersection(clips.peek()));
    }

    
    public void popClip() {
        if (clips.size() <= 1) {
            throw new IllegalStateException("Cannot pop the root clip");
        }
        clips.pop();
    }

    
    public void focus(UiId id) { focusedId = id; }

    
    public void clearFocus() { focusedId = UiId.NONE; }

    
    public boolean isFocused(UiId id) { return focusedId.equals(id); }

    
    public boolean isHovered(UiId id) { return hoveredId.equals(id); }

    
    public boolean isActive(UiId id) { return activeId.equals(id); }

    
    public UiId hoveredId() { return hoveredId; }

    
    public UiId activeId() { return activeId; }

    
    public LayoutMetrics canvas() { return canvas; }

    
    public InputState input() { return input; }

    
    public float mouseX() { return canvas.toLogicalX(input.mouseX()); }

    
    public float mouseY() { return canvas.toLogicalY(input.mouseY()); }

    
    public void setEnabled(boolean enabled) {
        if (this.enabled && !enabled) {
            resetInteraction();
        }
        this.enabled = enabled;
    }

    
    public void resetInteraction() {
        hoveredId = UiId.NONE;
        activeId = UiId.NONE;
        focusedId = UiId.NONE;
        pressConsumed = false;
        releaseConsumed = false;
    }

    
    public boolean enabled() { return enabled; }
}
