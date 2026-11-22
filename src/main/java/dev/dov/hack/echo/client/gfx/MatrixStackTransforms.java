package dev.dov.hack.echo.client.gfx;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public final class MatrixStackTransforms {
    private static final String UTILITY_CLASS_MESSAGE =
            "This is a utility class and cannot be instantiated";

    private MatrixStackTransforms() {
        throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
    }

    
    public static void rotate(MatrixStack matrices, Quaternionf rotation) {
        matrices.multiply(rotation);
    }
}
