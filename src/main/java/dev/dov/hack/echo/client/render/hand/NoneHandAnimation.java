package dev.dov.hack.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public class NoneHandAnimation extends HandAnimation {
    public NoneHandAnimation() {
        super("None");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        return;
    }
}
