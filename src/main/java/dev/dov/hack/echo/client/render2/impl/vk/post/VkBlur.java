package dev.dov.hack.echo.client.rendering.impl.vk.post;

public final class VkBlur {
    private int passes = 2;
    private int groupSize = 8;
    private float sigma = 4.0f;

    public void configure(int passes, float sigma) {
        this.passes = Math.max(1, Math.min(6, passes));
        this.sigma = Math.max(0.5f, sigma);
    }

    public int dispatches(int width, int height) {
        int groupsX = (Math.max(1, width / 2) + this.groupSize - 1) / this.groupSize;
        int groupsY = (Math.max(1, height / 2) + this.groupSize - 1) / this.groupSize;
        return groupsX * groupsY * this.passes;
    }

    public int passes() {
        return this.passes;
    }

    public float sigma() {
        return this.sigma;
    }

    public int groupSize() {
        return this.groupSize;
    }
}
