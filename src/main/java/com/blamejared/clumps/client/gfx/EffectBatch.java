package com.blamejared.clumps.client.gfx;

import com.blamejared.clumps.client.render.batch.RenderBatch;
import com.blamejared.clumps.client.gfx.texture.TextureView;

public interface EffectBatch extends RenderBatch {
    void draw(TextureView texture, float x, float y, float width, float height, float radius, float opacity, int tint, boolean mask, float u0, float v0, float u1, float v1, float depth);

    default void draw(TextureView texture, float x, float y, float width, float height, float radius, float opacity, int tint, boolean mask, float depth) {
        draw(texture, x, y, width, height, radius, opacity, tint, mask, 0.0F, 0.0F, 1.0F, 1.0F, depth);
    }

    default void draw(TextureView texture, float x, float y, float width, float height, float radius, float opacity, float depth) {
        draw(texture, x, y, width, height, radius, opacity, -1, false, depth);
    }

    
    default boolean isFull() {
        return false;
    }
}
