package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.ui.LayoutMetrics;
import com.blamejared.clumps.client.ui.InputState;

public final class DashboardPointerState {
    
    public interface CaptureState {
        
        boolean overlayCapturesInput();
        
        boolean modalOpen();
    }

    private final InputState input;
    private final CaptureState capture;
    private LayoutMetrics canvas;
    private DashboardTransform transform;
    private float mouseX = -1.0f;
    private float mouseY = -1.0f;

    
    public DashboardPointerState(InputState input, LayoutMetrics canvas, DashboardTransform transform, CaptureState capture) {
        this.input = input;
        this.capture = capture;
        this.canvas = canvas;
        this.transform = transform;
    }

    
    public void updateCanvas(LayoutMetrics canvas, DashboardTransform transform) {
        this.canvas = canvas;
        this.transform = transform;
    }

    
    public void move(double x, double y) {
        mouseX = inputX((float) x);
        mouseY = inputY((float) y);
        float screenY = canvas.toScreenY(mouseY);
        float screenX = canvas.toScreenX(mouseX);
        input.setPointer(screenX, screenY);
    }

    
    public boolean press(double x, double y, int button) {
        move(x, y);
        input.setDown(button, true);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    
    public boolean release(double x, double y, int button) {
        move(x, y);
        input.setDown(button, false);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    
    public boolean drag(double x, double y, int button) {
        move(x, y);
        return input.isDown(button);
    }

    
    public boolean scroll(double x, double y, double amount) {
        move(x, y);
        input.addScroll(0.0, amount);
        return capture.overlayCapturesInput() || capture.modalOpen() || canvas.contains(mouseX, mouseY);
    }

    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        input.addKeyEvent(keyCode, scanCode, modifiers);
        return true;
    }

    
    public boolean characterTyped(int character) {
        input.addCharacter(character);
        return true;
    }

    
    public float mouseX() { return mouseX; }
    
    public float mouseY() { return mouseY; }

    private float inputX(float x) {
        float center = transform.viewportLeft() + 1920.0f * transform.viewportScale() * 0.5f;
        return 960.0f + (x - center) / Math.max(transform.viewportScale() * transform.zoom(), 1.0e-4f);
    }

    private float inputY(float y) {
        float center = transform.viewportTop() + 1080.0f * transform.viewportScale() * 0.5f
                + transform.verticalOffset() * transform.viewportScale();
        return 540.0f + (y - center) / Math.max(transform.viewportScale() * transform.zoom(), 1.0e-4f);
    }
}
