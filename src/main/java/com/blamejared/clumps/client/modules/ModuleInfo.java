package com.blamejared.clumps.client.modules;

import com.blamejared.clumps.client.keyboard.InputKey;

public record ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory, InputKey key, boolean visible) {
    public ModuleInfo {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        moduleCategory = moduleCategory == null ? ModuleCategory.INTERNALS : moduleCategory;
        key = key == null ? InputKey.NONE : key;
    }

    public ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory, InputKey key) {
        this(name, description, moduleCategory, key, true);
    }

    public ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory, int keyCode, boolean visible) {
        this(name, description, moduleCategory, InputKey.fromCode(keyCode), visible);
    }

    public ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory, int keyCode) {
        this(name, description, moduleCategory, keyCode, true);
    }

    public ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory, boolean visible) {
        this(name, description, moduleCategory, InputKey.NONE, visible);
    }

    public ModuleInfo(CharSequence name, CharSequence description, ModuleCategory moduleCategory) {
        this(name, description, moduleCategory, InputKey.NONE, true);
    }
}
