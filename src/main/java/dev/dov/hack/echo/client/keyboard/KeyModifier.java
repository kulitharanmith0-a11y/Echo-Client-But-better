package dev.dov.hack.echo.client.keyboard;

public enum KeyModifier {
    SHIFT(1, 3), CONTROL(2, 192), ALT(4, 768), SUPER(8, 3072);

    private final int glfwMask;
    private final int nativeMask;

    KeyModifier(int glfwMask, int nativeMask) {
        this.glfwMask = glfwMask;
        this.nativeMask = nativeMask;
    }

    
    public int mask() {
        return glfwMask;
    }

    
    public boolean isSet(int modifiers) {
        return (modifiers & mask()) != 0;
    }
}
