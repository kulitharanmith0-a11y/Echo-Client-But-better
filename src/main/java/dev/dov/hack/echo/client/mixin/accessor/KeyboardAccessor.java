package dev.dov.hack.echo.client.mixin.accessor;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Keyboard.class)
public interface KeyboardAccessor {
    @Invoker("onKey")
    void invokeOnKey(long windowHandle, int action, KeyInput input);
}
