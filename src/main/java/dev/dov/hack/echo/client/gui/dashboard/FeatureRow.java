package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;

public record FeatureRow(ModuleCategory moduleCategory, Module Module, float y, float height, float visibility) {
}