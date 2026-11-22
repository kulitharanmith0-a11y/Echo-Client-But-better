package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.settings.Setting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Snapshots {
    private Snapshots() { }

    
    private record DisplayResult(List<SnapshotDiff> arrayRows, List<SnapshotEntry> keybindRows) {}

    public static DisplayResult displayRows(List<BindSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots");
        List<SnapshotDiff> arrayRows = new ArrayList<>();
        List<SnapshotEntry> keybindRows = new ArrayList<>();
        for (BindSnapshot snapshot : snapshots) {
            ModuleCategory moduleCategory = snapshot.Module().getCategory();
            if (snapshot.enabled()) {
                Optional<String> keyLabel = snapshot.binding() == InputKey.NONE
                        ? Optional.empty() : Optional.of(snapshot.binding().displayLabel());
                arrayRows.add(new SnapshotDiff(snapshot.id(), snapshot.label(), moduleCategory, keyLabel, snapshot.pressed()));
            }
            List<SnapshotEntry> settings = snapshot.enabled()
                    ? settingRows(snapshot, moduleCategory) : List.of();
            if (snapshot.binding() == InputKey.NONE && settings.isEmpty()) {
                continue;
            }
            keybindRows.add(new SnapshotEntry(snapshot.id(), snapshot.label(), moduleCategory, snapshot.binding() == InputKey.NONE ? "-" : snapshot.binding().displayLabel(), snapshot.enabled(), snapshot.pressed()));
            keybindRows.addAll(settings);
        }
        return new DisplayResult(arrayRows, keybindRows);
    }

    
    private static List<SnapshotEntry> settingRows(BindSnapshot snapshot, ModuleCategory moduleCategory) {
        List<SnapshotEntry> result = new ArrayList<>();
        for (Setting setting : snapshot.Module().settings) {
            if (!(setting instanceof KeybindSetting binding) || binding.getKey() == InputKey.NONE) {
                continue;
            }
            result.add(new SnapshotEntry(snapshot.id() + "/" + Concat.shortHash(binding.getLabel()), binding.getLabel(), moduleCategory, binding.getKey().displayLabel(), true, binding.getKey().isPressed()));
        }
        return result;
    }

    
    public static List<SnapshotDiff> arrayRows(List<BindSnapshot> snapshots) {
        return displayRows(snapshots).arrayRows();
    }

    
    public static List<SnapshotEntry> keybindRows(List<BindSnapshot> snapshots) {
        return displayRows(snapshots).keybindRows();
    }
}
