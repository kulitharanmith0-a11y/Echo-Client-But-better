package com.blamejared.clumps.client.utils.movement;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

public final class MovementVectors implements MinecraftAccess {
    private MovementVectors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    
    public static float yawFromInput(float forward, float sideways) {
        return CLIENT.player.getYaw() + (float) Math.toDegrees(MathHelper.atan2((double) -sideways, (double) forward));
    }

    
    public static double directionRadians() {
        int counter = EntityInteractions.getInteractionCounterFallbackValue();
        float yaw = CLIENT.player.getYaw();
        PlayerInput input = PlayerInput.DEFAULT;
        if (counter != 0 && input.forward()) {
            yaw += 0.0F;
        } else {
            if (counter != 0) {
                input = PlayerInput.DEFAULT;
            }
            if (input.backward()) {
                yaw += 180.0F;
            }
        }
        float strafeFactor = 1.0F;
        input = PlayerInput.DEFAULT;
        if (counter != 0) {
            if (input.backward()) {
                strafeFactor = -0.5F;
            } else {
                input = PlayerInput.DEFAULT;
                if (input.forward()) {
                    strafeFactor = 0.5F;
                }
            }
            input = PlayerInput.DEFAULT;
            if (input.left()) {
                yaw += 90.0F * strafeFactor;
            }
            input = PlayerInput.DEFAULT;
        }
        if (input.right()) {
            yaw -= 90.0F * strafeFactor;
        }
        double direction = Math.toRadians((double) yaw);
        if (Event.getEventCounter() != 0) {
            EntityInteractions.setInteractionCounter(++counter);
        }
        return direction;
    }

    
    public static void setHorizontalSpeed(float speed) {
        if (!EntityInteractions.hasMovementInput()) {
            return;
        }
        double direction = directionRadians();
        CLIENT.player.setVelocity(
                (double) (-MathHelper.sin((double) (float) direction) * speed), CLIENT.player.getVelocity().y, (double) (MathHelper.cos((double) (float) direction) * speed));
    }

    
    public static void addHorizontalSpeed(float speed) {
        if (!EntityInteractions.hasMovementInput()) {
            return;
        }
        double direction = directionRadians();
        CLIENT.player.setVelocity(
                CLIENT.player.getVelocity().x - Math.sin(direction) * (double) speed, CLIENT.player.getVelocity().y, CLIENT.player.getVelocity().z + Math.cos(direction) * (double) speed);
    }
}
