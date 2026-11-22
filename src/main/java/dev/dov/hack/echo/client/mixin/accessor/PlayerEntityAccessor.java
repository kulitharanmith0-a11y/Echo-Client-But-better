package dev.dov.hack.echo.client.mixin.accessor;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Invoker("isSpaceAroundPlayerEmpty")
    boolean invokeIsSpaceAroundPlayerEmpty(double offsetX, double offsetY, double offsetZ);
}
