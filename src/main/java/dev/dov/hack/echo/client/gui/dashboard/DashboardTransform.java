package dev.dov.hack.echo.client.gui.dashboard;

public record DashboardTransform(
        float viewportLeft, float viewportTop, float viewportScale, float zoom, float animationScale, float pivotX, float pivotY, float localOffsetX, float localOffsetY, float verticalOffset) {

    
    public float renderX(float logicalX) {
        float center = viewportLeft + 1920.0f * viewportScale * 0.5f;
        float animated = pivotX + (logicalX - pivotX) * animationScale;
        return center + (animated - 960.0f + localOffsetX) * viewportScale * zoom;
    }

    
    public float renderY(float logicalY) {
        float center = viewportTop + 1080.0f * viewportScale * 0.5f;
        float animated = pivotY + (logicalY - pivotY) * animationScale + localOffsetY;
        return center + (animated - 540.0f) * viewportScale * zoom + verticalOffset * viewportScale;
    }

    
    public float renderSize(float logicalSize) {
        return logicalSize * viewportScale * zoom * animationScale;
    }
}
