package com.blamejared.clumps.client.runtime;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class RhinoScopes {
    private RhinoScopes() {
    }

    
    public static Scriptable createScriptScope(Context context, Scriptable sharedScope) {
        Scriptable scope = context.newObject(sharedScope);
        scope.setPrototype(sharedScope);
        scope.setParentScope(null);
        return scope;
    }

    
    public static void defineApi(Context context, ScriptableObject sharedScope, Class<? extends Scriptable> apiClass, String className) {
        try {
            ScriptableObject.defineClass(sharedScope, apiClass);
            Scriptable instance = context.newObject(sharedScope, className);
            ScriptableObject.putProperty(sharedScope, className, instance);
        } catch (Exception ignored) {
        }
    }
}
