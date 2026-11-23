package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;

public record FeatureRow(ModuleCategory moduleCategory, Module Module, float y, float height, float visibility) {
}