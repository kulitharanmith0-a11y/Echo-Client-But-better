package com.blamejared.clumps.client.runtime;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public final class ScriptContextFactory extends ContextFactory {
    
    public ScriptContextFactory() {
    }

    
    @Override
    protected Context makeContext() {
        Context context = super.makeContext();
        context.setOptimizationLevel(9);
        context.setLanguageVersion(Context.VERSION_ES6);
        return context;
    }
}
