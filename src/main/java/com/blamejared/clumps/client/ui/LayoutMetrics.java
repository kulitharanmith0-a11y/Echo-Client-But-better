package com.blamejared.clumps.client.ui;

public record LayoutMetrics(float logicalWidth, float logicalHeight, float screenWidth, float screenHeight, float scale, float offsetX, float offsetY) {
    
    public static LayoutMetrics fit(float screenWidth, float screenHeight, float logicalWidth, float logicalHeight) {
        if (!(screenWidth > 0.0f) || !(screenHeight > 0.0f)
                || !(logicalWidth > 0.0f) || !(logicalHeight > 0.0f)) {
            throw new IllegalArgumentException("Canvas extents must be positive");
        }
        float scale = Math.min(screenWidth / logicalWidth, screenHeight / logicalHeight);
        return new LayoutMetrics(logicalWidth, logicalHeight, screenWidth, screenHeight, scale, (screenWidth - logicalWidth * scale) * 0.5f, (screenHeight - logicalHeight * scale) * 0.5f);
    }

    
    public float toScreenX(float x) {
        return offsetX + x * scale;
    }

    
    public float toScreenY(float y) {
        return offsetY + y * scale;
    }

    
    public float toScreenLength(float length) {
        return length * scale;
    }

    
    public float toLogicalX(float x) {
        return (x - offsetX) / scale;
    }

    
    public float toLogicalY(float y) {
        return (y - offsetY) / scale;
    }

    
    public UiRect toScreen(UiRect rectangle) {
        return new UiRect(toScreenX(rectangle.x()), toScreenY(rectangle.y()), toScreenLength(rectangle.width()), toScreenLength(rectangle.height()));
    }

    
    public boolean contains(float x, float y) {
        return x >= 0.0f && y >= 0.0f && x <= logicalWidth && y <= logicalHeight;
    }
}
