package dev.dov.hack.echo.client.particle;

import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.util.ArrayList;
import java.util.List;

public final class ParticleManager {
    private final List<Particle> particles = new ArrayList<>();
    private TextureView texture;

    public void initialize() {}

    public void add(Particle particle) {
        particles.add(particle);
    }

    public void tick(float tickDelta) {
        particles.removeIf(p -> { p.tick(tickDelta); return p.isDead(); });
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public TextureView getTexture() {
        return texture;
    }

    public void setTexture(TextureView texture) {
        this.texture = texture;
    }

    public void clear() {
        particles.clear();
    }
}
