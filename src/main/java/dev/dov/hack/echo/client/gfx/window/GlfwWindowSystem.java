package dev.dov.hack.echo.client.gfx.window;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public final class GlfwWindowSystem implements WindowSystem {
    @Override
    public long currentContextWindow() {
        return GLFW.glfwGetCurrentContext();
    }

    @Override
    public void makeContextCurrent(long windowHandle) {
        GLFW.glfwMakeContextCurrent(windowHandle);
    }

    @Override
    public void setSwapInterval(int interval) {
        GLFW.glfwSwapInterval(interval);
    }

    @Override
    public void swapBuffers(long windowHandle) {
        GLFW.glfwSwapBuffers(windowHandle);
    }

    @Override
    public int refreshRate(long windowHandle) {
        long monitor = findMonitor(windowHandle);
        GLFWVidMode mode = monitor == 0L ? null : GLFW.glfwGetVideoMode(monitor);
        return mode == null || mode.refreshRate() <= 0 ? 60 : mode.refreshRate();
    }

    private static long findMonitor(long windowHandle) {
        long fullscreenMonitor = GLFW.glfwGetWindowMonitor(windowHandle);
        if (fullscreenMonitor != 0L) {
            return fullscreenMonitor;
        }
        int[] windowX = new int[1];
        int[] windowY = new int[1];
        GLFW.glfwGetWindowPos(windowHandle, windowX, windowY);
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors != null) {
            for (int index = 0; index < monitors.limit(); index++) {
                long monitor = monitors.get(index);
                GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
                if (mode == null) {
                    continue;
                }
                int[] monitorX = new int[1];
                int[] monitorY = new int[1];
                GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);
                if (windowX[0] >= monitorX[0] && windowX[0] < monitorX[0] + mode.width()
                        && windowY[0] >= monitorY[0] && windowY[0] < monitorY[0] + mode.height()) {
                    return monitor;
                }
            }
        }
        return GLFW.glfwGetPrimaryMonitor();
    }
}
