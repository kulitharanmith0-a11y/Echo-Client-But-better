package dev.dov.hack.echo.client.runtime;

import dev.dov.hack.echo.client.runtime.api.ChatApi;
import dev.dov.hack.echo.client.runtime.api.ConsoleApi;
import dev.dov.hack.echo.client.runtime.api.MinecraftApi;
import dev.dov.hack.echo.client.runtime.api.SettingApi;
import dev.dov.hack.echo.client.runtime.api.SettingsApi;
import java.util.Set;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public class ScriptEngine {
    public static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final String[] LISTED_PACKAGE_PREFIXES = {
        "dev.dov.hack.echo.client.script.api.", "dev.dov.hack.echo.client.event.impl.", "dev.dov.hack.echo.client.draw2d.", "dev.dov.hack.echo.client.draw3d."
    };
    private static final Set<String> LISTED_CLASSES = Set.of(
        "java.lang.Object", "java.lang.String", "java.lang.CharSequence", "java.lang.Number", "java.lang.Boolean", "java.lang.Character", "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double"
    );

    private final RhinoScriptRuntime runtime;

    
    public ScriptEngine() {
        ScriptContextFactory factory = new ScriptContextFactory();
        ScriptableObject scope = factory.call(context -> {
            ScriptableObject shared = context.initStandardObjects();
            RhinoScopes.defineApi(context, shared, ConsoleApi.class, "console");
            RhinoScopes.defineApi(context, shared, SettingsApi.class, "settings");
            FontsRegistrationCompatibility.reportFailure();
            RhinoScopes.defineApi(context, shared, ChatApi.class, "chat");
            RhinoScopes.defineApi(context, shared, MinecraftApi.class, "minecraft");
            ScriptableObject.putProperty(shared, "mc", ScriptableObject.getProperty(shared, "minecraft"));
            SettingApi.register(shared);
            return shared;
        });
        runtime = new RhinoScriptRuntime(factory, scope, (sourceName, exception) -> ScriptErrorReporter.report(LOGGER, sourceName, "threw during load", exception));
    }

    
    public Scriptable run(String source, String sourceName) { return runtime.run(source, sourceName); }

    
    public Object call(Function function, Scriptable scope, Scriptable receiver, Object[] arguments) {
        return runtime.call(function, scope, receiver, arguments);
    }

    private static boolean isListedJavaClass(String className) {
        if (LISTED_CLASSES.contains(className)) return true;
        for (String prefix : LISTED_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }
}
