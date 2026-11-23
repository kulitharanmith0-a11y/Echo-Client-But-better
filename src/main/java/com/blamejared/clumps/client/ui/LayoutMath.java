package com.blamejared.clumps.client.ui;

public final class LayoutMath {
    private LayoutMath() {
    }

    
    public static float snapAndClamp(float value, float minimum, float maximum, float step) {
        float clamped = Math.max(minimum, Math.min(maximum, value));
        if (!(step > 0.0f)) {
            return clamped;
        }
        return Math.max(minimum, Math.min(maximum, minimum + Math.round((clamped - minimum) / step) * step));
    }

    
    public static UiRect placePopup(UiRect anchor, float width, float height, UiRect viewport, float gap) {
        float x = Math.max(viewport.x(), Math.min(anchor.x(), viewport.right() - width));
        float below = anchor.bottom() + gap;
        float above = anchor.y() - gap - height;
        float y = below + height <= viewport.bottom() ? below : Math.max(viewport.y(), above);
        return new UiRect(x, y, Math.min(width, viewport.width()), Math.min(height, viewport.height()));
    }

    
    public static float quadraticBezier(float start, float control, float end, float t) {
        float remaining = 1.0f - t;
        return remaining * remaining * start + 2.0f * remaining * t * control + t * t * end;
    }

    
    public static AxisSnapResult updateSnap(float distance, boolean wasSnapped, float entryDistance, float releaseDistance) {
        boolean snapped = distance <= (wasSnapped ? releaseDistance : entryDistance);
        return new AxisSnapResult(snapped, snapped && !wasSnapped, !snapped && wasSnapped);
    }

    
    public static int premultiplyArgb(int argb) {
        int alpha = argb >>> 24 & 0xff;
        int red = (argb >>> 16 & 0xff) * alpha + 127;
        int green = (argb >>> 8 & 0xff) * alpha + 127;
        int blue = (argb & 0xff) * alpha + 127;
        return alpha << 24 | red / 255 << 16 | green / 255 << 8 | blue / 255;
    }
}
