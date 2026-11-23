package com.blamejared.clumps.client.mixin.accessor;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Invoker("onMouseButton")
    void invokeOnMouseButton(long windowHandle, MouseInput input, int action);

    @Invoker("onMouseScroll")
    void invokeOnMouseScroll(long windowHandle, double horizontal, double vertical);
}
