package com.blamejared.clumps.client.handlers.rotation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationMath {
    private RotationMath() {
    }

    
    public static float smoothVelocity(float velocity, float angularError, float speed, float elapsedSeconds) {
        float speedLimit = speed * Math.min(1.0F + Math.abs(angularError) / 30.0F, 3.0F);
        float desiredVelocity = MathHelper.clamp(angularError * speed, -speedLimit, speedLimit);
        float blend = 1.0F - (float) Math.exp(-elapsedSeconds * 15.0F);
        return MathHelper.clamp(velocity + (desiredVelocity - velocity) * blend, -speedLimit, speedLimit);
    }

    
    public static Vec3d randomPoint(Box box) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Vec3d(
                box.minX + random.nextDouble() * (box.maxX - box.minX), box.minY + random.nextDouble() * (box.maxY - box.minY), box.minZ + random.nextDouble() * (box.maxZ - box.minZ));
    }
}
