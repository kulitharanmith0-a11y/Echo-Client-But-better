package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.movement.MovementVectors;

public class Speed extends Module {
    private static final CharSequence GRIM_ENTITY = Concat.strings("Grim Entity");
    private static final CharSequence MOTION = Concat.strings("Motion");
    private static final CharSequence STRAFE = Concat.strings("Strafe");
    private static final CharSequence MINEMEN_CLUB = Concat.strings("MinemenClub");
    private static final CharSequence LEGIT = Concat.strings("Legit");
    private static ChoiceSetting mode;
    private static FloatSetting boost;
    private static FloatSetting boatBoost;
    private static FloatSetting motion;
    private static FloatSetting strafe;

    public Speed() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Speed"), dev.dov.hack.echo.client.init.CatalogText.literal("Increases movement speed"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        boost.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_ENTITY));
        boatBoost.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_ENTITY));
        motion.setVisibilityPredicate(ignored -> mode.isSelected(MOTION));
        strafe.setVisibilityPredicate(ignored -> mode.isSelected(STRAFE));
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !isEnabled() || !EntityInteractions.hasMovementInput()) {
            return;
        }
        if (mode.isSelected(MINEMEN_CLUB)) {
            if (client.player.isOnGround()) {
                MovementVectors.setHorizontalSpeed(0.26f);
                event.jump = true;
            }
        } else if (mode.isSelected(LEGIT)) {
            if (client.player.isOnGround()) {
                event.jump = true;
            }
        } else if (mode.isSelected(GRIM_ENTITY)) {
            if (EntityInteractions.hasNearbyBoat(client.player)) {
                MovementVectors.addHorizontalSpeed(boatBoost.getValue());
            } else if (EntityInteractions.hasNearbyEntity(client.player)) {
                MovementVectors.addHorizontalSpeed(boost.getValue());
            }
        } else if (mode.isSelected(MOTION)) {
            MovementVectors.addHorizontalSpeed(motion.getValue());
        } else if (mode.isSelected(STRAFE)) {
            MovementVectors.setHorizontalSpeed(strafe.getValue());
        }
    }

    
    public static void appendBootstrapSettings(Speed Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Speed.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Speed.boost);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Speed.boatBoost);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Speed.motion);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Speed.strafe);
    }

    
    public static void assignBootstrapSettings(Speed Module, dev.dov.hack.echo.client.settings.FloatSetting preparedStrafe, dev.dov.hack.echo.client.settings.FloatSetting preparedBoatBoost, dev.dov.hack.echo.client.settings.FloatSetting preparedMotion, dev.dov.hack.echo.client.settings.FloatSetting preparedBoost, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode) {
        Speed.strafe = preparedStrafe;
        Speed.boatBoost = preparedBoatBoost;
        Speed.motion = preparedMotion;
        Speed.boost = preparedBoost;
        Speed.mode = preparedMode;
    }
}
