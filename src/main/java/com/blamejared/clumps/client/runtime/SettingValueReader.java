package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.settings.Setting;
import com.blamejared.clumps.client.settings.TextSetting;
import java.util.Objects;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

public final class SettingValueReader {
    private SettingValueReader() {
    }

    public static Object toJs(Setting setting) {
        Objects.requireNonNull(setting);
        return switch (setting) {
            case BooleanSetting value -> value.getValue();
            case IntSetting value -> value.getValue();
            case FloatSetting value -> value.getValue();
            case ChoiceSetting value -> value.getValue().toString();
            case TextSetting value -> value.getText();
            case ColorSetting value -> value.getArgb();
            case KeybindSetting value -> value.getKey();
            case FloatRangeSetting value -> {
                NativeObject range = new NativeObject();
                range.defineProperty("min", value.getLower(), 0);
                range.defineProperty("max", value.getUpper(), 0);
                yield range;
            }
            case MultiChoiceSetting value -> {
                NativeObject group = new NativeObject();
                value.toStringMap().forEach((name, enabled) -> group.defineProperty(name, enabled, 0));
                yield group;
            }
            case HotbarSlotsSetting value -> {
                NativeArray slots = new NativeArray(9L);
                for (int index = 0; index < 9; index++) {
                    slots.put(index, slots, value.isSelected(index));
                }
                yield slots;
            }
            default -> Undefined.instance;
        };
    }
}
