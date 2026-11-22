package dev.dov.hack.echo.client.platform;

public final class Platform {
    private Platform() {}

    public interface DisplayControl {
        void invalidateFullscreenState();
    }

    public interface GuiLoop {
        void setMainGuiPass(boolean mainGuiPass);
    }

    public interface GuiState {
        void markScreenLayerStart();
        boolean prepareScreenBlurLayer();
        boolean isScreenBlurLayerInstalled();
    }

    public interface RenderLoop {
        void renderGuiWithoutFog();
    }

    public interface SceneBuffer {
        void updateOutlineFramebufferCancellation();
    }

    public interface TickControl {
        void setTickSpeedMultiplier(float multiplier);
        float getTickSpeedMultiplier();
    }

    public static final class DialogBridge {
        private DialogBridge() {}

        private static final java.lang.reflect.Method MSG_BOX;

        static {
            java.lang.reflect.Method m = null;
            try {
                Class<?> cls = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                for (java.lang.reflect.Method candidate : cls.getMethods()) {
                    if (candidate.getName().equals("tinyfd_messageBox") && candidate.getParameterCount() == 5) {
                        m = candidate;
                        break;
                    }
                }
            } catch (Exception ignored) {}
            MSG_BOX = m;
        }

        public static boolean messageBox(CharSequence title, CharSequence message, CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
            if (MSG_BOX != null) {
                try {
                    Object result = MSG_BOX.invoke(null, title.toString(), message.toString(), dialogType.toString(), iconType.toString(), defaultButton);
                    return Boolean.TRUE.equals(result);
                } catch (Exception ignored) {}
            }
            return defaultButton;
        }
    }
}
