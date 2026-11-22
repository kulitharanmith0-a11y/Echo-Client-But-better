package dev.dov.hack.echo.client.utils;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public record Rotation(float yaw, float pitch) {
    public Rotation {
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
    }

    
    public static Rotation lookingAt(Vec3d origin, Vec3d target) {
        Vec3d direction = target.subtract(origin);
        double horizontalDistance = Math.hypot(direction.x, direction.z);
        return new Rotation(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F), MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance))));
    }

    
    public Rotation relativeTo(Rotation reference) {
        float referenceYaw = MathHelper.wrapDegrees(reference.yaw);
        return new Rotation(referenceYaw + MathHelper.wrapDegrees(yaw - referenceYaw), pitch);
    }

    
    public float yawDifferenceTo(Rotation target) {
        return MathHelper.wrapDegrees(target.yaw - yaw);
    }

    
    public Rotation stepToward(Rotation target, float maxYawStep, float maxPitchStep) {
        float yawDifference = MathHelper.wrapDegrees(target.yaw - yaw);
        float nextYaw = yaw + MathHelper.clamp(yawDifference, -maxYawStep, maxYawStep);
        float pitchDifference = target.pitch - pitch;
        float nextPitch = pitch + MathHelper.clamp(pitchDifference, -maxPitchStep, maxPitchStep);
        return new Rotation(nextYaw, nextPitch);
    }
}
