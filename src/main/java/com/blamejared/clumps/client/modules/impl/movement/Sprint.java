package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.KeyPressedQueryEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.SprintOverrideHandler;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.strings.Concat;
import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();
    private static final CharSequence OMNI_SUFFIX = Concat.strings("Omni");
    private static BooleanSetting omni;

    public Sprint() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Sprint"), com.blamejared.clumps.client.init.CatalogText.literal("Auto sprint"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
    }

    public boolean isOmniEnabled() {
        return isEnabled() && omni.getValue();
    }

    @Override
    public CharSequence getSuffix() {
        return isOmniEnabled() ? OMNI_SUFFIX : EMPTY_SUFFIX;
    }

    @Override
    public void onDisable() {
        int controlState = DiveBomb.readSharedControlState();
        MinecraftClient observedClient = client;
        if (controlState == 0 && observedClient.player != null) {
            SprintOverrideHandler.resetOverrides();
            observedClient.player.setSprinting(false);
        } else if (controlState != 0) {
            observedClient.player.setSprinting(false);
        }
        super.onDisable();
    }

    @Subscribe
    public void onSprintKeyQuery(KeyPressedQueryEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (SprintOverrideHandler.isSuppressSprint()) {
            event.setPressed(false);
            return;
        }
        if (EntityInteractions.hasMovementInput()) {
            event.setPressed(true);
        }
    }

    
    public static void appendBootstrapSettings(Sprint Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Sprint.omni);
    }

    
    public static void assignBootstrapSettings(Sprint Module, com.blamejared.clumps.client.settings.BooleanSetting preparedOmni) {
        Sprint.omni = preparedOmni;
    }
}
