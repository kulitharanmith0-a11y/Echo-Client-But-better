package com.blamejared.clumps.client.runtime;

import java.util.function.Consumer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

public final class RhinoOperations {
    private RhinoOperations() {
    }

    
    public static Script compile(Context context, String source, String sourceName) {
        return context.compileString(source, sourceName, 1, null);
    }

    
    public static Object call(Context context, Function function, Scriptable scope, Scriptable thisObject, Object[] arguments) {
        return function.call(context, scope, thisObject, arguments);
    }

    
    public static Scriptable executeInScope(Context context, Script script, Scriptable scope, Consumer<Exception> reportFailure) {
        try {
            script.exec(context, scope);
            return scope;
        } catch (Exception exception) {
            reportFailure.accept(exception);
            return null;
        }
    }
}
