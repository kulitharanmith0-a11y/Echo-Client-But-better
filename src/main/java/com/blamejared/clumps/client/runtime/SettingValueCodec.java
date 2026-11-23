package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.Setting;
import com.blamejared.clumps.client.settings.TextSetting;
import java.util.Objects;
import org.mozilla.javascript.NativeObject;

public class SettingValueCodec {
    public static Object toJs(Setting setting) {
        return SettingValueReader.toJs(setting);
    }

    public static void fromJs(Setting setting, Object value) {
        Objects.requireNonNull(setting);
        switch (setting) {
            case BooleanSetting target -> target.setValue(ScriptValues.toBoolean(value));
            case IntSetting target -> {
                if (value instanceof Number number) {
                    target.setValue(number.intValue());
                }
            }
            case FloatSetting target -> {
                if (value instanceof Number number) {
                    target.setValue(number.floatValue());
                }
            }
            case ChoiceSetting target -> {
                if (value instanceof String text) {
                    target.setValue(text);
                }
            }
            case TextSetting target -> {
                if (value instanceof String text) {
                    target.setText(text);
                }
            }
            case ColorSetting target -> {
                if (value instanceof Number number) {
                    target.setRgb(number.intValue());
                }
            }
            case KeybindSetting target -> {
                if (value instanceof CharSequence text) {
                    target.setKey(InputKey.parse(text.toString()));
                }
            }
            case FloatRangeSetting target -> {
                if (value instanceof NativeObject range) {
                    Object minimum = range.get("min", range);
                    Object maximum = range.get("max", range);
                    if (minimum instanceof Number number) {
                        target.setLower(number.floatValue());
                    }
                    if (maximum instanceof Number number) {
                        target.setUpper(number.floatValue());
                    }
                }
            }
            default -> {
            }
        }
    }
}
