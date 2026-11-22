package dev.dov.hack.echo.client.runtime.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class ChatApi extends ScriptableObject {
    
    @Override
    public String getClassName() { return "chat"; }

    
    @JSFunction("writeEvent")
    public static void print(Context context, Scriptable receiver, Object[] arguments, Function function) {
        if (arguments.length == 0) return;
        Text message = Text.literal(Context.toString(arguments[0]));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        client.inGameHud.getChatHud().addMessage(message);
    }
}
