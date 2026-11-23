package com.blamejared.clumps.client.modules.internals;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
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
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Click GUI"), com.blamejared.clumps.client.init.CatalogText.literal("Click-based GUI"), com.blamejared.clumps.client.modules.ModuleCategory.INTERNALS, 344));
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
            if (ClumpsClient.screenManager != null) {
                ClumpsClient.screenManager.close();
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
                if (ClumpsClient.screenManager != null) {
                    ClumpsClient.screenManager.open();
                }
            } catch (Throwable ignored) {
            }
        } finally {
            setEnabled(false);
        }
    }

    
    @Override
    public boolean isEnabled() {
        initializeScreenManagerType();
        return ClumpsClient.screenManager != null && ClumpsClient.screenManager.isOpen();
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
        ClumpsClient.setExternalRendererEnabled(streamProof.getValue() && SUPPORTS_STREAM_PROOF);
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
