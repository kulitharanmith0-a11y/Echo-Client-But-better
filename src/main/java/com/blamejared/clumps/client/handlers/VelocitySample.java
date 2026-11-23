package com.blamejared.clumps.client.handlers;

import net.minecraft.util.math.Vec3d;

public record VelocitySample(int tick, Vec3d position, Vec3d velocity) {
}
