package dev.dov.hack.echo.client.runtime;

import java.util.function.BiConsumer;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class RhinoScriptRuntime {
    private final ContextFactory contextFactory;
    private final ScriptableObject sharedScope;
    private final BiConsumer<String, Exception> reportEvaluationFailure;

    
    public RhinoScriptRuntime(ScriptableObject sharedScope, BiConsumer<String, Exception> reportEvaluationFailure) {
        this(new ScriptContextFactory(), sharedScope, reportEvaluationFailure);
    }

    
    public RhinoScriptRuntime(ContextFactory contextFactory, ScriptableObject sharedScope, BiConsumer<String, Exception> reportEvaluationFailure) {
        this.contextFactory = contextFactory;
        this.sharedScope = sharedScope;
        this.reportEvaluationFailure = reportEvaluationFailure;
    }

    
    public Scriptable run(String source, String sourceName) {
        Script script = compile(source, sourceName);
        return contextFactory.call(context -> {
            Scriptable scope = RhinoScopes.createScriptScope(context, sharedScope);
            return RhinoOperations.executeInScope(context, script, scope, exception -> reportEvaluationFailure.accept(sourceName, exception));
        });
    }

    
    public Object call(Function function, Scriptable scope, Scriptable thisObject, Object[] arguments) {
        return contextFactory.call(context -> RhinoOperations.call(context, function, scope, thisObject, arguments));
    }

    private Script compile(String source, String sourceName) {
        return contextFactory.call(context -> RhinoOperations.compile(context, source, sourceName));
    }
}
