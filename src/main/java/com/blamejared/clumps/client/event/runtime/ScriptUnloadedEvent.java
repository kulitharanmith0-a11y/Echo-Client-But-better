package com.blamejared.clumps.client.event.runtime;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.runtime.ScriptFeature;

public class ScriptUnloadedEvent extends Event {
    private final ScriptFeature Module;
    private final String scriptName;

    public ScriptUnloadedEvent(ScriptFeature Module, String scriptName) {
        this.Module = Module;
        this.scriptName = scriptName;
    }

    public ScriptFeature getFeature() { return Module; }

    
    public String getScriptName() { return scriptName; }
}
