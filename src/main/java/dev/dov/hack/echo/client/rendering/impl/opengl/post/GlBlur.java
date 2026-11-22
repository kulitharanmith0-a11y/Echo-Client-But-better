package dev.dov.hack.echo.client.rendering.impl.opengl.post;

public final class GlBlur {
    private int passes = 2;
    private float radius = 4.0f;

    public void configure(int passes, float radius) {
        this.passes = Math.max(1, Math.min(6, passes));
        this.radius = Math.max(0.5f, radius);
    }

    public int passes() {
        return this.passes;
    }

    public float radius() {
        return this.radius;
    }

    
    public int downsampleWidth(int sourceWidth) {
        return Math.max(1, sourceWidth / 2);
    }

    public int downsampleHeight(int sourceHeight) {
        return Math.max(1, sourceHeight / 2);
    }

    
    public float[] taps() {
        float[] out = new float[this.passes * 2];
        for (int index = 0; index < this.passes; index++) {
            out[index * 2] = (index + 1) * this.radius / this.passes;
            out[index * 2 + 1] = 1.0f / (index + 1);
        }
        return out;
    }

    public int totalDraws() {
        return this.passes * 2 + 2;
    }
}
