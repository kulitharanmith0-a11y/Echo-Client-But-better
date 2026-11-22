package dev.dov.hack.echo.client.gfx.window;

public interface WindowSystem {
    
    long currentContextWindow();

    void makeContextCurrent(long window);

    void setSwapInterval(int interval);

    void swapBuffers(long window);

    int refreshRate(long window);
}
