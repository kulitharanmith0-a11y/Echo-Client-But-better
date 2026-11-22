package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.KeyPressedQueryEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.SprintOverrideHandler;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.strings.Concat;
import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();
    private static final CharSequence OMNI_SUFFIX = Concat.strings("Omni");
    private static BooleanSetting omni;

    public Sprint() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Sprint"), dev.dov.hack.echo.client.init.CatalogText.literal("Auto sprint"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT));
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
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Sprint.omni);
    }

    
    public static void assignBootstrapSettings(Sprint Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedOmni) {
        Sprint.omni = preparedOmni;
    }
}
