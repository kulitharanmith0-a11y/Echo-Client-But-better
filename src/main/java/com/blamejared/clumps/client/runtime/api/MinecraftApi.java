package com.blamejared.clumps.client.runtime.api;

import com.blamejared.clumps.client.utils.WorldView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class MinecraftApi extends ScriptableObject {
    @Override public String getClassName() { return "minecraft"; }

    
    @JSFunction("getEvent")
    public static PlayerView getPlayer(Context context, Scriptable receiver, Object[] arguments, Function function) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? null : new PlayerView(player);
    }

    
    @JSFunction("isValue")
    public static WorldView getWorld(Context context, Scriptable receiver, Object[] arguments, Function function) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world == null ? null : new WorldView(world);
    }

    
    @JSFunction("removeChannel")
    public static HitResultView getCrosshairTarget(Context context, Scriptable receiver, Object[] arguments, Function function) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        return hit == null ? null : new HitResultView(hit);
    }
}
