package dev.dov.hack.echo.client.handlers;

import net.minecraft.client.MinecraftClient;

public final class MouseSensitivity {
    private float horizontalRemainder;
    private float verticalRemainder;

    public void reset() {
        horizontalRemainder = 0.0F;
        verticalRemainder = 0.0F;
    }

    public MouseDelta quantize(float yawDelta, float pitchDelta, boolean fineScale, boolean invertHorizontal, boolean invertVertical) {
        float step = angularStep(fineScale);
        if (step <= 1.0E-5F) return MouseDelta.ZERO;
        horizontalRemainder += yawDelta / step * (invertHorizontal ? -1.0F : 1.0F);
        verticalRemainder += pitchDelta / step * (invertVertical ? -1.0F : 1.0F);
        int horizontal = (int) horizontalRemainder;
        int vertical = (int) verticalRemainder;
        horizontalRemainder -= horizontal;
        verticalRemainder -= vertical;
        return new MouseDelta(horizontal, vertical);
    }

    public static float angularStep(boolean fineScale) {
        double sensitivity = MinecraftClient.getInstance().options.getMouseSensitivity().getValue()
                * 0.6F + 0.2F;
        double scaled = sensitivity * sensitivity * sensitivity * 8.0;
        return (float) (scaled * (fineScale ? 0.01875 : 0.15));
    }
}
