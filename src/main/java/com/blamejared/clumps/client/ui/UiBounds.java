package com.blamejared.clumps.client.ui;

public record UiBounds(float x, float y, float width, float height) {
    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public UiBounds expand(float amount) {
        return new UiBounds(x - amount, y - amount, width + amount * 2.0f, height + amount * 2.0f);
    }
}
