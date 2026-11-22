package dev.dov.hack.echo.client.render.hand;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.render.ViewModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public abstract class HandAnimation {
    private final String displayName;

    public HandAnimation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public abstract void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm);

    
    public static void applyConfiguredSwing(MatrixStack matrices, Arm arm, float swingProgress) {
        ViewModel Module = (ViewModel) EchoClient.ModuleManager.findFeature(ViewModel.class);
        float amplitude = 1.0F;
        if (Module != null && Module.isEnabled()) {
            if (MinecraftClient.getInstance().player != null) {
                amplitude = arm == MinecraftClient.getInstance().player.getMainArm()
                        ? ViewModel.strengthMain.getValue()
                        : ViewModel.strengthOff.getValue();
            } else {
                amplitude = ViewModel.strengthMain.getValue();
            }
        }
        int side = arm == Arm.RIGHT ? 1 : -1;
        float squaredWave = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * (-2.0F * amplitude)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * (-8.0F * amplitude)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }
}
