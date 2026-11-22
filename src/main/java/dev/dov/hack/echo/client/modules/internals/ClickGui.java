package dev.dov.hack.echo.client.modules.internals;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import org.lwjgl.system.Platform;

public class ClickGui extends Module {
    public static FloatSetting scrollSpeed;
    public static BooleanSetting streamProof;
    public static final boolean SUPPORTS_STREAM_PROOF;
    private static final String SCROLL_SPEED_TOOLTIP;
    private static boolean syncingStreamProof;

    static {
        SCROLL_SPEED_TOOLTIP = "Timing";
        SUPPORTS_STREAM_PROOF =
                Platform.get() == Platform.WINDOWS || Platform.get() == Platform.MACOSX;
    }

    
    public ClickGui() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Click GUI"), dev.dov.hack.echo.client.init.CatalogText.literal("Click-based GUI"), dev.dov.hack.echo.client.modules.ModuleCategory.INTERNALS, 344));
    }

    @Override
    public void postInitialize() {
        streamProof.onChange(ClickGui::applyStreamProofSetting);
        streamProof.setVisibilityPredicate(ClickGui::isStreamProofSettingVisible);
        scrollSpeed.tooltip(SCROLL_SPEED_TOOLTIP);
    }

    @Override
    public void toggle() {
        if (isEnabled()) {
            if (EchoClient.screenManager != null) {
                EchoClient.screenManager.close();
            }
            return;
        }
        setEnabled(true);
    }

    
    @Override
    public void onEnable() {
        try {
            try {
                initializeScreenManagerType();
                if (EchoClient.screenManager != null) {
                    EchoClient.screenManager.open();
                }
            } catch (Exception ignored) {
            }
        } finally {
            setEnabled(false);
        }
    }

    
    @Override
    public boolean isEnabled() {
        initializeScreenManagerType();
        return EchoClient.screenManager != null && EchoClient.screenManager.isOpen();
    }

    @Override
    public boolean recordsStateChanges() {
        return false;
    }

    @Override
    public boolean persistsEnabledState() {
        return false;
    }

    public float getScrollSpeed() {
        return scrollSpeed.getValue();
    }

    private static boolean isStreamProofSettingVisible(Object context) {
        return SUPPORTS_STREAM_PROOF;
    }

    private static void applyStreamProofSetting() {
        if (syncingStreamProof) {
            return;
        }
        EchoClient.setExternalRendererEnabled(streamProof.getValue() && SUPPORTS_STREAM_PROOF);
    }

    public static void syncStreamProofFailure() {
        if (streamProof == null || !streamProof.getValue()) {
            return;
        }
        syncingStreamProof = true;
        try {
            streamProof.setValue(false);
        } finally {
            syncingStreamProof = false;
        }
    }

    private static void initializeScreenManagerType() {
        try {
            Class.forName(ScreenManager.class.getName(), true, ScreenManager.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

}
