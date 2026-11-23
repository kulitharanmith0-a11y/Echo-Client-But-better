package com.blamejared.clumps.client.runtime.api;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.runtime.ScriptSettingFactory;
import com.blamejared.clumps.client.runtime.SettingArguments;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class SettingsApi extends ScriptableObject {
    @Override
    public String getClassName() {
        return "settings";
    }

    @JSFunction("buildLayer")
    public static Object booleanSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.booleanSetting(arguments));
    }

    @JSFunction("loadStore")
    public static Object integerSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.integerSetting(arguments));
    }

    @JSFunction("processAdapter")
    public static Object floatSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.floatSetting(arguments));
    }

    @JSFunction("writeType")
    public static Object rangeSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.rangeSetting(arguments));
    }

    @JSFunction("decodeListener")
    public static Object choiceSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.choiceSetting(arguments));
    }

    @JSFunction("registerLayer")
    public static Object booleanGroupSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.booleanGroupSetting(arguments));
    }

    @JSFunction("resetTask")
    public static Object textSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.textSetting(arguments));
    }

    @JSFunction("getPath")
    public static Object colorSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.colorSetting(arguments));
    }

    @JSFunction("handleTemplate")
    public static Object keybindSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        String name = SettingArguments.string(arguments, 0, "setting");
        String keyName = SettingArguments.string(arguments, 1, "");
        return SettingApi.wrap(context, scope, new KeybindSetting(name, InputKey.parse(keyName)));
    }

    @JSFunction("handleFile")
    public static Object hotbarSlotsSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        return SettingApi.wrap(context, scope, ScriptSettingFactory.hotbarSlotsSetting(arguments));
    }

    @JSFunction("openAdapter")
    public static Object itemSelectionSetting(Context context, Scriptable scope, Object[] arguments, Function function) {
        String name = SettingArguments.string(arguments, 0, "setting");
        return SettingApi.wrap(context, scope, new ItemListSetting(name));
    }
}
