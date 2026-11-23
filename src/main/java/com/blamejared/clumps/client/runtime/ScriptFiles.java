package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.gui.DashboardScreen;
import java.util.ArrayList;
import java.util.List;

public final class ScriptFiles {
    public static final boolean SCRIPTING_ENABLED = false;
    private static String disabledMessage;
    private static String failedPrefix;
    private static String parseErrorMessage;
    private static String loadedPrefix;
    private static String nameSeparator;
    private static String enabledMessage;
    private static String registeredMessage;

    private ScriptFiles() {}

    
    public static ScriptManager unavailableManager() {
        return null;
    }

    public static List<String> listFilenames() {
        boolean stateClear = DashboardScreen.isSharedControlStateClear();
        ScriptManager manager = unavailableManager();
        if (!stateClear && manager == null) {
            return List.of();
        }
        return manager.listFilenames();
    }

    public static ScriptFeature loadedScript(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ScriptManager manager = unavailableManager();
        if (!state) {
            return (ScriptFeature) (Object) manager;
        }
        return manager == null ? null : manager.loadedScripts().get(filename);
    }

    
    public static String readSource(String filename) {
        return "";
    }

    
    public static boolean writeSource(String filename, String source) {
        return false;
    }

    public static boolean deleteScript(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ScriptManager manager = unavailableManager();
        if (manager == null) {
            return false;
        }
        if (state) {
            manager.unload(filename);
        }
        return true;
    }

    
    public static String describeFile(String filename) {
        return "";
    }

    public static List<String> reloadWithDiagnostics(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ArrayList<String> diagnostics = new ArrayList<>();
        ScriptManager manager = unavailableManager();
        if (state) {
            if (manager == null) {
                diagnostics.add(disabledMessage());
                return diagnostics;
            }
            manager.unload(filename);
            manager.reload(filename);
        }
        ScriptFeature Module = manager.loadedScripts().get(filename);
        if (state) {
            if (Module == null) {
                diagnostics.add(failedPrefix() + filename);
                diagnostics.add(parseErrorMessage());
                return diagnostics;
            }
            String name = String.valueOf(Module.getName());
            diagnostics.add(loadedPrefix() + filename + nameSeparator() + name);
        }
        diagnostics.add(Module.isEnabled() ? enabledMessage() : registeredMessage());
        return diagnostics;
    }

    private static String disabledMessage() {
        String value = disabledMessage;
        if (value == null) disabledMessage = value = "scripts are disabled";
        return value;
    }

    private static String failedPrefix() {
        String value = failedPrefix;
        if (value == null) failedPrefix = value = "failed to load ";
        return value;
    }

    private static String parseErrorMessage() {
        String value = parseErrorMessage;
        if (value == null) parseErrorMessage = value = "see the log for the parse error";
        return value;
    }

    private static String loadedPrefix() {
        String value = loadedPrefix;
        if (value == null) loadedPrefix = value = "loaded ";
        return value;
    }

    private static String nameSeparator() {
        String value = nameSeparator;
        if (value == null) nameSeparator = value = " as ";
        return value;
    }

    private static String enabledMessage() {
        String value = enabledMessage;
        if (value == null) enabledMessage = value = "Module enabled";
        return value;
    }

    private static String registeredMessage() {
        String value = registeredMessage;
        if (value == null) registeredMessage = value = "Module registered, currently off";
        return value;
    }
}
