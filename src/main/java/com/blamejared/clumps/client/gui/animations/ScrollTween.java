package com.blamejared.clumps.client.gui.animations;

public class ScrollTween extends Tween {
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;

    
    public ScrollTween(Easing easing, float durationMillis) {
        super(easing, durationMillis);
    }

    
    public void start(float fromX, float fromY, float toX, float toY) {
        startX = fromX;
        startY = fromY;
        targetX = toX;
        targetY = toY;
        super.start(0.0, 1.0);
    }

    
    @Override
    public void setTarget(double target) {
    }

    
    public void scrollToX(float x) {
        if (isFinished()) {
            start(targetX, targetY, x, targetY);
        } else {
            targetX = x;
        }
    }

    
    public void scrollTo(float x, float y) {
        if (isFinished()) {
            start(targetX, targetY, x, y);
        } else {
            targetX = x;
            targetY = y;
        }
    }

    
    public float x() {
        return (float) (startX + (targetX - startX) * super.value());
    }

    
    public float y() {
        return (float) (startY + (targetY - startY) * super.value());
    }

    
    public void setTargetX(float x) { targetX = x; }

    
    public void setTargetY(float y) { targetY = y; }
}
