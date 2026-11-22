package dev.jadyen.hack.echo.natives;

public interface WindowBackend extends AutoCloseable {
    
    long windowHandle();

    
    void synchronizeWithMainWindow();

    
    void setVisible(boolean visible);

    
    @Override
    void close();

    int framebufferWidth();

    int framebufferHeight();
}
