package dev.dov.hack.echo.client.particle;

public abstract class Particle {
    public double x;
    public double y;
    public double z;
    public int color;
    private final int type;

    protected Particle(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public abstract boolean isDead();

    public abstract float getSize();

    public abstract void tick(float tickProgress);
}
