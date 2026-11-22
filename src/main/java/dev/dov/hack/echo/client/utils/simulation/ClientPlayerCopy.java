package dev.dov.hack.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public abstract class ClientPlayerCopy extends OtherClientPlayerEntity {
    protected ClientPlayerCopy(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    protected final void refreshWaterState() {
        this.updateWaterState();
    }
}
