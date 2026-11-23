package com.blamejared.clumps.client.gui.animations;

public final class CubicBezier {
    private CubicBezier() {
    }

    
    public static float coordinate(float parameter, float firstControl, float secondControl) {
        float complement = 1.0F - parameter;
        return 3.0F * complement * complement * parameter * firstControl
                + 3.0F * complement * parameter * parameter * secondControl
                + parameter * parameter * parameter;
    }

    
    public static float derivative(float parameter, float firstControl, float secondControl) {
        float complement = 1.0F - parameter;
        return 3.0F * complement * complement * firstControl
                + 6.0F * complement * parameter * (secondControl - firstControl)
                + 3.0F * parameter * parameter * (1.0F - secondControl);
    }
}
