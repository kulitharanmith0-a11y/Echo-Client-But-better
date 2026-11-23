package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.movement.MovementVectors;

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
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Speed"), com.blamejared.clumps.client.init.CatalogText.literal("Increases movement speed"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT, false));
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
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Speed.mode);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Speed.boost);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Speed.boatBoost);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Speed.motion);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Speed.strafe);
    }

    
    public static void assignBootstrapSettings(Speed Module, com.blamejared.clumps.client.settings.FloatSetting preparedStrafe, com.blamejared.clumps.client.settings.FloatSetting preparedBoatBoost, com.blamejared.clumps.client.settings.FloatSetting preparedMotion, com.blamejared.clumps.client.settings.FloatSetting preparedBoost, com.blamejared.clumps.client.settings.ChoiceSetting preparedMode) {
        Speed.strafe = preparedStrafe;
        Speed.boatBoost = preparedBoatBoost;
        Speed.motion = preparedMotion;
        Speed.boost = preparedBoost;
        Speed.mode = preparedMode;
    }
}
