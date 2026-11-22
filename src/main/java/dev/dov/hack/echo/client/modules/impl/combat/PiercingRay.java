package dev.dov.hack.echo.client.modules.impl.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public record PiercingRay(ClientPlayerEntity player, Vec3d start, Vec3d end) {
}
