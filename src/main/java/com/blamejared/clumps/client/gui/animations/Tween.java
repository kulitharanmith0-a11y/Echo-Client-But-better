package com.blamejared.clumps.client.gui.animations;

public class Tween {
    private Easing easing;
    private double durationMillis;
    private long startTimeMillis;
    private double startValue;
    private double targetValue;

    
    public Tween(Easing easing, double durationMillis) {
        this.easing = easing;
        startTimeMillis = System.currentTimeMillis();
        this.durationMillis = durationMillis;
    }

    
    public boolean isFinished() {
        return System.currentTimeMillis() - startTimeMillis >= durationMillis;
    }

    
    public void setTarget(double target) {
        if (!isFinished()) {
            targetValue = target;
        } else {
            start(targetValue, target);
        }
    }

    
    public void start(double from, double to) {
        startValue = from;
        targetValue = to;
        startTimeMillis = System.currentTimeMillis();
    }

    
    public double value() {
        if (isFinished()) {
            return targetValue;
        }
        long now = System.currentTimeMillis();
        float elapsedMillis = now - startTimeMillis;
        double progress = elapsedMillis / durationMillis;
        double eased = Math.clamp(easing.function().apply(progress), 0.0, 1.0);
        return startValue + (targetValue - startValue) * eased;
    }

    
    public void setEasing(Easing easing) { this.easing = easing; }

    
    public void setDurationMillis(double durationMillis) { this.durationMillis = durationMillis; }

    
    public double startValue() { return startValue; }

    
    public double targetValue() { return targetValue; }
}
