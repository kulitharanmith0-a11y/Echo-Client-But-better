package dev.dov.hack.echo.client.runtime.api;

import dev.dov.hack.echo.client.runtime.SettingCallbacks;
import dev.dov.hack.echo.client.runtime.SettingValueCodec;
import dev.dov.hack.echo.client.settings.Setting;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

public class SettingApi extends ScriptableObject {
    private Setting setting;

    @Override
    public String getClassName() {
        return "ScriptSetting";
    }

    public static void register(ScriptableObject scope) {
        try {
            ScriptableObject.defineClass(scope, SettingApi.class);
        } catch (Exception ignored) {
        }
    }

    public static SettingApi wrap(Context context, Scriptable scope, Setting setting) {
        SettingApi wrapper = (SettingApi) context.newObject(scope, "ScriptSetting");
        wrapper.setting = setting;
        return wrapper;
    }

    @JSFunction("encodeQuery")
    public static Object get(Context context, Scriptable self, Object[] arguments, Function function) {
        return SettingValueCodec.toJs(((SettingApi) self).setting);
    }

    @JSFunction("startFeature")
    public static void set(Context context, Scriptable self, Object[] arguments, Function function) {
        if (arguments.length == 0) {
            return;
        }
        SettingValueCodec.fromJs(((SettingApi) self).setting, arguments[0]);
    }

    @JSFunction("updateQuery")
    public static SettingApi onChange(Context context, Scriptable self, Object[] arguments, Function function) {
        SettingApi wrapper = (SettingApi) self;
        if (arguments.length > 0 && arguments[0] instanceof Function callback) {
            wrapper.setting.onChange(() -> SettingCallbacks.callOnChange(callback, context, self, wrapper.setting));
        }
        return wrapper;
    }

    @JSFunction("readHeader")
    public static SettingApi visibleWhen(Context context, Scriptable self, Object[] arguments, Function function) {
        SettingApi wrapper = (SettingApi) self;
        if (arguments.length > 0 && arguments[0] instanceof Function callback) {
            wrapper.setting.setVisibilityPredicate(ignored -> SettingCallbacks.testVisibility(callback, context, self));
        }
        return wrapper;
    }

    public Setting getSetting() {
        return setting;
    }
}
