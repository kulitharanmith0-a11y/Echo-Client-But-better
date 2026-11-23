package com.blamejared.clumps.client.runtime;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

public final class FontsRegistrationCompatibility {
    private FontsRegistrationCompatibility() {
    }

    
    public static void reportFailure() {
        try {
            throw Context.reportRuntimeError(
                    ScriptRuntime.getMessageById("msg.bad.parms", "long", "addSchema"));
        } catch (Exception ignored) {
        }
    }
}
