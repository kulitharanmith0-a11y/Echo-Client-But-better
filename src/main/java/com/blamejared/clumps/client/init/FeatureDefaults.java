package com.blamejared.clumps.client.init;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.settings.*;

public final class FeatureDefaults {
    private FeatureDefaults() {}

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AimAssist Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedFovMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("FOV Mode"), CatalogText.literal("Single"), new java.lang.CharSequence[] {CatalogText.literal("Single"), CatalogText.literal("Split")});
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPitchFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedRandom = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedMultipoint = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnClick = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Hold Attack"), false);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetPriority = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedVerticalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.FloatSetting preparedHorizontalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 5.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimMath = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimVector = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        com.blamejared.clumps.client.settings.FloatSetting preparedFov = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedYawFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));

        com.blamejared.clumps.client.modules.impl.combat.AimAssist.assignBootstrapSettings(Module, preparedFovMode, preparedRange, preparedPitchFOV, preparedItemWhitelist, preparedRandom, preparedSilentRotations, preparedMultipoint, preparedOnClick, preparedTargetPriority, preparedThroughWalls, preparedVerticalSpeed, preparedHotbarWhitelist, preparedHorizontalSpeed, preparedAimMath, preparedAimVector, preparedFov, preparedYawFOV);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AimAssist.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AimOptimizer Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedStrength = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Strength"), 35.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedSlowdown = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Slowdown"), 25.0f, 0.0f, 85.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedSmoothing = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Smoothing"), 30.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPrediction = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Prediction"), 60.0f, 0.0f, 150.0f, 5.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedMultipoint = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 100.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedGcd = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("GCD"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPlayersOnly = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Players Only"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetPriority = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("FOV"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 4.5f, 1.0f, 16.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedFov = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("FOV"), 75.0f, 5.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnClick = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Click"), false);
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));

        com.blamejared.clumps.client.modules.impl.combat.AimOptimizer.assignBootstrapSettings(Module, preparedStrength, preparedSlowdown, preparedSmoothing, preparedPrediction, preparedMultipoint, preparedGcd, preparedThroughWalls, preparedPlayersOnly, preparedTargetPriority, preparedRange, preparedFov, preparedOnClick, preparedItemWhitelist, preparedHotbarWhitelist);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AimOptimizer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.AimVisualizer Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedSize = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Size"), 0.15f, 0.05f, 0.5f, 0.01f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.ColorSetting preparedServerColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Server Color"), 255, 0, 0, 200);
        com.blamejared.clumps.client.settings.ColorSetting preparedClientColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Client Color"), 0, 0, 255, 200);

        com.blamejared.clumps.client.modules.impl.render.AimVisualizer.assignBootstrapSettings(Module, preparedSize, preparedServerColor, preparedClientColor);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.AimVisualizer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Ambience Module) {

        com.blamejared.clumps.client.settings.ColorSetting preparedGradientColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Gradient color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapSecondaryColor(), false);
        com.blamejared.clumps.client.settings.ColorSetting preparedFogColorColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Fog color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor(), false);
        com.blamejared.clumps.client.settings.IntSetting preparedTime = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Time"), 18000, 0, 24000, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedFogColorBoolean = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Fog color"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedGradient = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Gradient"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedWeather = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Weather"), CatalogText.literal("Clear"), new java.lang.CharSequence[] {CatalogText.literal("Clear"), CatalogText.literal("Rain"), CatalogText.literal("Thunder")});
        com.blamejared.clumps.client.settings.IntSetting preparedFogDistance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Fog distance"), 16, 0, 48, CatalogText.literal(" chunks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedCustomTime = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Custom time"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedModifyWeather = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Modify weather"), false);

        com.blamejared.clumps.client.modules.impl.render.Ambience.assignBootstrapSettings(Module, preparedGradientColor, preparedFogColorColor, preparedTime, preparedFogColorBoolean, preparedGradient, preparedWeather, preparedFogDistance, preparedCustomTime, preparedModifyWeather);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Ambience.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.AntiTrap Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSigns = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Signs"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedHangingSigns = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Hanging Signs"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedGlowFrames = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Glow Frames"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPaintings = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Paintings"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedChestMinecarts = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Chest Minecarts"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedArmorStands = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Armor Stands"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoInteract = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Interact"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedItemFrames = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Item Frames"), true);

        com.blamejared.clumps.client.modules.impl.utility.AntiTrap.assignBootstrapSettings(Module, preparedSigns, preparedHangingSigns, preparedGlowFrames, preparedPaintings, preparedChestMinecarts, preparedArmorStands, preparedNoInteract, preparedItemFrames);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.AntiTrap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.ArmorStatus Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.ArrayList Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedVisibilityFilter = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Visibility filter"), CatalogText.literal("Blacklist"), new java.lang.CharSequence[] {CatalogText.literal("Blacklist"), CatalogText.literal("Whitelist")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedLowercase = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Lowercase"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSuffixEnabled = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Suffix Enabled"), true);

        com.blamejared.clumps.client.modules.impl.render.ArrayList.assignBootstrapSettings(Module, preparedVisibilityFilter, preparedLowercase, preparedSuffixEnabled);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.ArrayList.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.AttributeMine Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoAnchor Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedMinDamage = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Min Damage"), 6.0f, 0.0f, 20.0f, 0.5f, CatalogText.literal(" dmg"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedCanPlaceLegit = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Can Place Legit"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSafeBlockDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Safe block delay"), 0.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.ColorSetting preparedOutlineColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        com.blamejared.clumps.client.settings.IntSetting preparedSafetyTimeout = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Safety Timeout"), 200, 0, 1000, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapModeOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapModeOption2(), com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapModeOption()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedGlowstoneStart = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Glowstone Start"), false);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedChargeDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Charge Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedRenderPosition = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Render Position"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSafeAnchor = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Safe Anchor"), false);
        com.blamejared.clumps.client.settings.IntSetting preparedExplodeSlot = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Explode Slot"), 1, 1, 9);
        com.blamejared.clumps.client.settings.BooleanSetting preparedRepeat = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Repeat"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Style"), com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption(), com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption2()});
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCharges = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Charges"), 1.0f, 1.0f, 1.0f, 4.0f, 1.0f, CatalogText.literal(" charges"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedExplodeDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Explode Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotationSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Rotation Speed"), 6.0f, 1.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.ColorSetting preparedFillColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Fill Color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapForegroundColor().getRGB(), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), false);

        com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.assignBootstrapSettings(Module, preparedMinDamage, preparedCanPlaceLegit, preparedSafeBlockDelay, preparedOutlineColor, preparedSafetyTimeout, preparedMode, preparedGlowstoneStart, preparedChargeDelay, preparedRenderPosition, preparedSafeAnchor, preparedExplodeSlot, preparedRepeat, preparedStyle, preparedCharges, preparedExplodeDelay, preparedAutoAim, preparedRotationSpeed, preparedFillColor, preparedActivateKey, preparedSilentAim);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoAnchor.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoCart Module) {

        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapReturnToOption3(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapReturnToOption3(), com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapReturnToOption2(), com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapReturnToOption()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedFreshArrowsOnly = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Fresh Arrows Only"), true);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedPlace = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapPlaceOption(), com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapPlaceOption2(), com.blamejared.clumps.client.modules.impl.combat.AutoCart.bootstrapPlaceOption3()});

        com.blamejared.clumps.client.modules.impl.combat.AutoCart.assignBootstrapSettings(Module, preparedReturnItems, preparedReturnTo, preparedSilentRotations, preparedFreshArrowsOnly, preparedReturnSlots, preparedPlace);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoCart.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoClicker Module) {

        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedBreakBlocks = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Break Blocks"), false);
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedRequireClick = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Require Click"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCps = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 9.0f, 13.0f, 1.0f, 20.0f, 0.5f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedRandomization = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Randomization"), com.blamejared.clumps.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption2(), com.blamejared.clumps.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption()});

        com.blamejared.clumps.client.modules.impl.combat.AutoClicker.assignBootstrapSettings(Module, preparedHotbarWhitelist, preparedBreakBlocks, preparedItemWhitelist, preparedRequireClick, preparedCps, preparedRandomization);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoClicker.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoCrystal Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedPlaceDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Place Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedPlaceObsidianDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Place Obsidian Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedFakePunch = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Fake Punch"), false);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedBreakDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Break Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwitchDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Switch Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedPlaceObsidian = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Place Obsidian"), false);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.IntSetting preparedFailChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Fail Chance"), 0, 0, 100, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption3(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption3(), com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption(), com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption2()});
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.MOUSE_RIGHT);
        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedStopOnKill = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stop on kill"), false);

        com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.assignBootstrapSettings(Module, preparedPlaceDelay, preparedPlaceObsidianDelay, preparedSwitchBack, preparedFakePunch, preparedBreakDelay, preparedSwitchDelay, preparedPlaceObsidian, preparedHotbarWhitelist, preparedItemWhitelist, preparedReturnSlots, preparedFailChance, preparedReturnTo, preparedActivateKey, preparedReturnItems, preparedStopOnKill);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoCrystal.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoMace Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCps = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 6.0f, 12.0f, 1.0f, 20.0f, 0.5f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedWorkWithMaceSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Work With Mace Swap"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedMultipoint = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPitchFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetPriority = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedReEquipElytra = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Re-Equip Elytra"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedFallDistance = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Fall Distance"), 1.5f, 0.5f, 9.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutomaticallyUnequipElytra = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Automatically Unequip Elytra"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedRandom = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedVerticalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedYawFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedUnequipTiming = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Unequip Timing"), CatalogText.literal("Late"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Late")});
        com.blamejared.clumps.client.settings.FloatSetting preparedHorizontalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedLateWindow = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Late Window"), 6.0f, 5.0f, 12.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimMath = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimVector = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});

        com.blamejared.clumps.client.modules.impl.combat.AutoMace.assignBootstrapSettings(Module, preparedCps, preparedWorkWithMaceSwap, preparedMultipoint, preparedPitchFOV, preparedTargetPriority, preparedRange, preparedReEquipElytra, preparedSilentRotations, preparedFallDistance, preparedAutomaticallyUnequipElytra, preparedAutoAim, preparedRandom, preparedVerticalSpeed, preparedThroughWalls, preparedYawFOV, preparedUnequipTiming, preparedHorizontalSpeed, preparedLateWindow, preparedAimMath, preparedAimVector);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoMace.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoMaceSwap Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedFallDistance = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Fall Distance"), 1.5f, 0.5f, 9.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCps = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 6.0f, 12.0f, 1.0f, 20.0f, 0.5f);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedStunSlam = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stun Slam"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAxeStun = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Axe Stun"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedStunReactionDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Stun Reaction Delay"), 0.0f, 35.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedStunSwitchDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Stun Switch Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSpearDTap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Spear D-tap"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedMinimumDamage = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Minimum Damage"), 10.0f, 0.0f, 35.0f, 1.0f, CatalogText.literal(" Dmg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimMath = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimVector = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Closest"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetPriority = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedRandom = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedHorizontalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 6.5f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedVerticalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 6.5f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedYawFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 120.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPitchFOV = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 180.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedMultipoint = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutomaticallyUnequipElytra = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Automatically Unequip Elytra"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedElytraRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Elytra Range"), 3.0f, 3.0f, 6.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedUnequipTiming = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Unequip Timing"), CatalogText.literal("Late"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Late")});
        com.blamejared.clumps.client.settings.FloatSetting preparedLateWindow = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Late Window"), 6.0f, 5.0f, 12.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedReEquipElytra = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Re-Equip Elytra"), false);

        com.blamejared.clumps.client.modules.impl.combat.AutoMaceSwap.assignBootstrapSettings(Module, preparedFallDistance, preparedCps, preparedDelay, preparedItemWhitelist, preparedHotbarWhitelist, preparedStunSlam, preparedAutoAxeStun, preparedStunReactionDelay, preparedStunSwitchDelay, preparedSpearDTap, preparedMinimumDamage, preparedReturnTo, preparedReturnSlots, preparedReturnItems, preparedAutoAim, preparedAimMath, preparedAimVector, preparedTargetPriority, preparedRandom, preparedSilentRotations, preparedThroughWalls, preparedHorizontalSpeed, preparedVerticalSpeed, preparedRange, preparedYawFOV, preparedPitchFOV, preparedMultipoint, preparedAutomaticallyUnequipElytra, preparedElytraRange, preparedUnequipTiming, preparedLateWindow, preparedReEquipElytra);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoMaceSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.AutoTool Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedRequireClick = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Require click"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switch back"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedRequireShift = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Require shift"), true);

        com.blamejared.clumps.client.modules.impl.world.AutoTool.assignBootstrapSettings(Module, preparedRequireClick, preparedSwitchBack, preparedRequireShift);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.world.AutoTool.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.AutoTotem Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedHealthSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Health Swap"), false);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedRestoreItem = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Restore Item"), com.blamejared.clumps.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption(), com.blamejared.clumps.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption2()});
        com.blamejared.clumps.client.settings.IntSetting preparedSlotToRefillTotem = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Slot to Refill Totem"), 9, 1, 9);
        com.blamejared.clumps.client.settings.IntSetting preparedRestoreAbove = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Restore Above"), 12, 1, 20, CatalogText.literal(" hp"));
        com.blamejared.clumps.client.settings.IntSetting preparedEquipBelow = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Equip Below"), 8, 1, 19, CatalogText.literal(" hp"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedRefillHotbar = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Refill Hotbar"), false);

        com.blamejared.clumps.client.modules.impl.utility.AutoTotem.assignBootstrapSettings(Module, preparedHealthSwap, preparedRestoreItem, preparedSlotToRefillTotem, preparedRestoreAbove, preparedEquipBelow, preparedRefillHotbar);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.AutoTotem.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.AutoTrap Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        com.blamejared.clumps.client.settings.IntSetting preparedPlaceDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Place Delay"), 1, 0, 4, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnlyStationary = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Only Stationary"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.utility.AutoTrap.bootstrapModeOption2(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.utility.AutoTrap.bootstrapModeOption2(), com.blamejared.clumps.client.modules.impl.utility.AutoTrap.bootstrapModeOption()});

        com.blamejared.clumps.client.modules.impl.utility.AutoTrap.assignBootstrapSettings(Module, preparedSilentRotations, preparedPlaceDelay, preparedOnlyStationary, preparedMode);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.AutoTrap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.AutoWalk Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.Backtrack Module) {

        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.ColorSetting preparedOutlineColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        com.blamejared.clumps.client.settings.IntSetting preparedMaximumHurtTime = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Maximum Hurt Time"), 10, 0, 10, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedHoldTime = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Hold Time"), 100.0f, 100.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.ColorSetting preparedFillColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Fill Color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapForegroundColor().getRGB(), true);

        com.blamejared.clumps.client.modules.impl.combat.Backtrack.assignBootstrapSettings(Module, preparedHotbarWhitelist, preparedOutlineColor, preparedMaximumHurtTime, preparedHoldTime, preparedItemWhitelist, preparedFillColor);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.Backtrack.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.BlockBruteforcer Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedRadius = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Radius"), 5, 1, 8, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.IntSetting preparedUp = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Up"), 3, 0, 64, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.IntSetting preparedDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.IntSetting preparedDown = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Down"), 3, 0, 64, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.IntSetting preparedBlocksPerTick = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Blocks Per Tick"), 4, 1, 5, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Abort"), new java.lang.CharSequence[] {CatalogText.literal("Abort"), CatalogText.literal("Start")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedFreezeMovement = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Freeze Movement"), true);

        com.blamejared.clumps.client.modules.impl.world.BlockBruteforcer.assignBootstrapSettings(Module, preparedRadius, preparedUp, preparedDelay, preparedDown, preparedBlocksPerTick, preparedMode, preparedFreezeMovement);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.world.BlockBruteforcer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Chams Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 0.5f, 0.0f, 3.0f, 0.1f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.ColorSetting preparedColor1 = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color 1"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor());
        com.blamejared.clumps.client.settings.BooleanSetting preparedRenderSelf = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Render Self"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedGradientScale = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Gradient Scale"), 1.0f, 0.25f, 4.0f, 0.25f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedGlowRadius = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Glow Radius"), 12.0f, 0.0f, 32.0f, 1.0f);
        com.blamejared.clumps.client.settings.FloatSetting preparedLineWidth = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Line Width"), 2.0f, 1.0f, 8.0f, 0.5f);
        com.blamejared.clumps.client.settings.ColorSetting preparedColor2 = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color 2"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapSecondaryColor());
        com.blamejared.clumps.client.settings.FloatSetting preparedOutlineOpacity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Outline Opacity"), 1.0f, 0.0f, 1.0f, 0.05f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedTeamColors = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Team Colors"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedFillOpacity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Fill Opacity"), 0.25f, 0.0f, 1.0f, 0.05f);
        com.blamejared.clumps.client.settings.FloatSetting preparedGlowIntensity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Glow Intensity"), 0.75f, 0.0f, 1.0f, 0.05f);

        com.blamejared.clumps.client.modules.impl.render.Chams.assignBootstrapSettings(Module, preparedSpeed, preparedColor1, preparedRenderSelf, preparedGradientScale, preparedGlowRadius, preparedLineWidth, preparedColor2, preparedOutlineOpacity, preparedTeamColors, preparedFillOpacity, preparedGlowIntensity);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Chams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.ChestStealer Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedInitialDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Initial Delay"), 150, 1, 1000, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.IntSetting preparedTakeDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Take Delay"), 70, 1, 1000, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.IntSetting preparedCloseDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Close Delay"), 50, 1, 1000, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnlyImportant = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Only Important"), false);

        com.blamejared.clumps.client.modules.impl.world.ChestStealer.initialDelay = preparedInitialDelay;
        com.blamejared.clumps.client.modules.impl.world.ChestStealer.takeDelay = preparedTakeDelay;
        com.blamejared.clumps.client.modules.impl.world.ChestStealer.closeDelay = preparedCloseDelay;
        com.blamejared.clumps.client.modules.impl.world.ChestStealer.onlyImportant = preparedOnlyImportant;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.world.ChestStealer.initialDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.world.ChestStealer.takeDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.world.ChestStealer.closeDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.world.ChestStealer.onlyImportant);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.ChinaHat Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedHeight = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Height"), 0.31f, 0.1f, 0.8f, 0.01f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Rainbow"), new java.lang.CharSequence[] {CatalogText.literal("Static"), CatalogText.literal("Rainbow")});
        com.blamejared.clumps.client.settings.FloatSetting preparedSpinSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Spin Speed"), 0.25f, 0.0f, 2.0f, 0.05f);
        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 185);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOutline = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Outline"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedRadius = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Radius"), 0.55f, 0.3f, 1.2f, 0.05f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedOpacity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Opacity"), 0.9f, 0.0f, 1.0f, 0.05f);

        com.blamejared.clumps.client.modules.impl.render.ChinaHat.assignBootstrapSettings(Module, preparedHeight, preparedMode, preparedSpinSpeed, preparedColor, preparedOutline, preparedRadius, preparedOpacity);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.ChinaHat.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.internals.ClickGui Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedScrollSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Scroll Speed"), 50.0f, 1.0f, 50.0f, 1.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedStreamProof = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stream proof"), false);

        com.blamejared.clumps.client.modules.internals.ClickGui.scrollSpeed = preparedScrollSpeed;
        com.blamejared.clumps.client.modules.internals.ClickGui.streamProof = preparedStreamProof;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.internals.ClickGui.scrollSpeed);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.internals.ClickGui.streamProof);
        com.blamejared.clumps.client.modules.internals.ClickGui.appendAppearanceSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Coordinates Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.Criticals Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Unsprint"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Unsprint"), CatalogText.literal("Release")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedResume = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Resume"), CatalogText.literal("Instant"), new java.lang.CharSequence[] {CatalogText.literal("Instant"), CatalogText.literal("Delay"), CatalogText.literal("Ground")});
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedResumeDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Resume Delay"), 1.0f, 3.0f, 1.0f, 10.0f, 1.0f, CatalogText.literal("t"));

        com.blamejared.clumps.client.modules.impl.combat.Criticals.assignBootstrapSettings(Module, preparedMode, preparedResume, preparedResumeDelay);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.Criticals.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.CrystalOptimizer Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.Displace Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedYawOffset = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Yaw Offset"), 90.0f, 0.0f, 180.0f, 5.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedDirection = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Direction"), com.blamejared.clumps.client.modules.impl.combat.Displace.bootstrapDirectionOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.Displace.bootstrapDirectionOption3(), com.blamejared.clumps.client.modules.impl.combat.Displace.bootstrapDirectionOption2(), com.blamejared.clumps.client.modules.impl.combat.Displace.bootstrapDirectionOption()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedDynamic = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Dynamic"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedKnockbackEnchant = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Knockback Enchant"), true);

        com.blamejared.clumps.client.modules.impl.combat.Displace.assignBootstrapSettings(Module, preparedYawOffset, preparedDirection, preparedDynamic, preparedKnockbackEnchant);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.Displace.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.DiveBomb Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedTickDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Tick Delay"), 2, 0, 6, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoJump = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedInstantDiveBomb = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Instant Dive Bomb"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedDiveBomb = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Dive Bomb"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchback = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switchback"), true);

        com.blamejared.clumps.client.modules.impl.movement.DiveBomb.assignBootstrapSettings(Module, preparedTickDelay, preparedAutoJump, preparedInstantDiveBomb, preparedSilentAim, preparedDiveBomb, preparedSwitchback);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.DiveBomb.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.ElytraSwap Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);

        com.blamejared.clumps.client.modules.impl.utility.ElytraSwap.assignBootstrapSettings(Module, preparedSwitchBack, preparedActivateKey);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.ElytraSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.FastStairs Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.FastUse Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 0.0f, 0.0f, 0.0f, 4.0f, 1.0f, CatalogText.literal(" ticks"));

        com.blamejared.clumps.client.modules.impl.utility.FastUse.delay = preparedDelay;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.utility.FastUse.delay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.utility.FastUse.items);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Freecam Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 1.0f, 5.0f, 0.1f);

        com.blamejared.clumps.client.modules.impl.render.Freecam.assignBootstrapSettings(Module, preparedSpeed);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Freecam.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.FullBright Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.GUIWalk Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Grim")});

        com.blamejared.clumps.client.modules.impl.movement.GUIWalk.assignBootstrapSettings(Module, preparedMode);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.GUIWalk.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.HandChams Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedLineWidth = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Line Width"), 2.0f, 1.0f, 8.0f, 0.5f);
        com.blamejared.clumps.client.settings.FloatSetting preparedGlowRadius = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Glow Radius"), 12.0f, 0.0f, 32.0f, 1.0f);
        com.blamejared.clumps.client.settings.FloatSetting preparedOutlineOpacity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Outline Opacity"), 1.0f, 0.0f, 1.0f, 0.05f);
        com.blamejared.clumps.client.settings.FloatSetting preparedFillOpacity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Fill Opacity"), 0.25f, 0.0f, 1.0f, 0.05f);
        com.blamejared.clumps.client.settings.ColorSetting preparedColor2 = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color 2"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapSecondaryColor());
        com.blamejared.clumps.client.settings.ColorSetting preparedColor1 = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color 1"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor());
        com.blamejared.clumps.client.settings.FloatSetting preparedGradientScale = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Gradient Scale"), 1.0f, 0.25f, 4.0f, 0.25f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedGlowIntensity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Glow Intensity"), 0.75f, 0.0f, 1.0f, 0.05f);
        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 0.5f, 0.0f, 3.0f, 0.1f, CatalogText.literal("x"));

        com.blamejared.clumps.client.modules.impl.render.HandChams.assignBootstrapSettings(Module, preparedLineWidth, preparedGlowRadius, preparedOutlineOpacity, preparedFillOpacity, preparedColor2, preparedColor1, preparedGradientScale, preparedGlowIntensity, preparedSpeed);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.HandChams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.HighJump Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Normal"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Shulker")});
        com.blamejared.clumps.client.settings.IntSetting preparedTicks = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Ticks"), 6, 1, 20, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedLift = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Lift"), 0.51f, 0.1f, 1.85f, 0.01f);

        com.blamejared.clumps.client.modules.impl.movement.HighJump.assignBootstrapSettings(Module, preparedMode, preparedTicks, preparedLift);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.HighJump.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.Hitboxes Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedStaticHitbox = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Static Hitbox"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedExpandX = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Expand X"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedUniform = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Uniform"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedExpandYUp = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Expand Y Up"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedExpandZ = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Expand Z"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedExpandSize = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Expand Size"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedExpandYDown = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Expand Y Down"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));

        com.blamejared.clumps.client.modules.impl.combat.Hitboxes.assignBootstrapSettings(Module, preparedStaticHitbox, preparedExpandX, preparedUniform, preparedExpandYUp, preparedExpandZ, preparedExpandSize, preparedExpandYDown);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.Hitboxes.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Hotbar Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.InfoBar Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedShowFrameTime = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Frame Time"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowFPS = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show FPS"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowPing = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Ping"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowUsername = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Username"), true);

        com.blamejared.clumps.client.modules.impl.render.InfoBar.assignBootstrapSettings(Module, preparedShowFrameTime, preparedShowFPS, preparedShowPing, preparedShowUsername);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.InfoBar.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.ItemESP Module) {

        ItemListSetting preparedWhitelist = new ItemListSetting(CatalogText.literal("Whitelist"));
        BooleanSetting preparedFill = new BooleanSetting(CatalogText.literal("Fill"), false);
        MultiChoiceSetting preparedModes = new MultiChoiceSetting(CatalogText.literal("Modes"), new CharSequence[]{CatalogText.literal("Box"), CatalogText.literal("2D")});
        BooleanSetting preparedShowName = new BooleanSetting(CatalogText.literal("Show Name"), true);
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 2145955883);
        com.blamejared.clumps.client.modules.impl.render.ItemESP.assignBootstrapSettings(Module, preparedWhitelist, preparedFill, preparedModes, preparedShowName, preparedTracers, preparedColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.ItemESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.JumpReset Module) {

        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedDamageSources = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Damage Sources"), new java.lang.CharSequence[] {CatalogText.literal("Player"), CatalogText.literal("Mob"), CatalogText.literal("Projectile"), CatalogText.literal("Explosion"), CatalogText.literal("Fall"), CatalogText.literal("Fire"), CatalogText.literal("Magic"), CatalogText.literal("Other")});
        com.blamejared.clumps.client.settings.FloatSetting preparedChance = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Chance"), 100.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedWebCheck = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Web Check"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedLiquidCheck = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Liquid Check"), true);

        com.blamejared.clumps.client.modules.impl.movement.JumpReset.assignBootstrapSettings(Module, preparedDamageSources, preparedChance, preparedWebCheck, preparedLiquidCheck);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.JumpReset.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.KeepSprint Module) {

        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedSprintCondition = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Sprint Condition"), new java.lang.CharSequence[] {CatalogText.literal("Air"), CatalogText.literal("Ground")});
        com.blamejared.clumps.client.settings.FloatSetting preparedSpeedKeptVelocity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed Kept Velocity"), 0.6f, 0.6f, 1.0f, 0.1f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShouldSprint = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Should Sprint"), true);

        com.blamejared.clumps.client.modules.impl.movement.KeepSprint.assignBootstrapSettings(Module, preparedSprintCondition, preparedSpeedKeptVelocity, preparedShouldSprint);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.KeepSprint.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Keybinds Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.KeyCharge Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchback = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switchback"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Swap"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoJump = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);

        com.blamejared.clumps.client.modules.impl.movement.KeyCharge.assignBootstrapSettings(Module, preparedSwitchback, preparedActivateKey, preparedSilentSwap, preparedAutoJump, preparedSilentAim, preparedAutoAim);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.KeyCharge.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.KeyPearl Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSyncToClient = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Sync to Client"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedThrowDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Throw Delay"), 0.0f, 0.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwapDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Swap Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));

        com.blamejared.clumps.client.modules.impl.utility.KeyPearl.assignBootstrapSettings(Module, preparedSyncToClient, preparedThrowDelay, preparedActivateKey, preparedSwapBack, preparedSwapDelay);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.KeyPearl.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedGroundDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Ground Delay"), 50.0f, 150.0f, 0.0f, 5000.0f, 1.0f, com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay.bootstrapGroundDelayOption());
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedAirDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Air Delay"), 100.0f, 250.0f, 0.0f, 5000.0f, 1.0f, com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay.bootstrapGroundDelayOption());
        com.blamejared.clumps.client.settings.IntSetting preparedChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Chance"), 100, 0, 100, com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay.bootstrapChanceOption());

        com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay.assignBootstrapSettings(Module, preparedGroundDelay, preparedAirDelay, preparedChance);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.KnockbackDelay.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.LightESP Module) {

        IntSetting preparedMaxY = new IntSetting(CatalogText.literal("Max Y"), 0, -64, 319, CatalogText.literal(""));
        IntSetting preparedMinLight = new IntSetting(CatalogText.literal("Min Light"), 5, 0, 15, CatalogText.literal(""));
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        BooleanSetting preparedOutline = new BooleanSetting(CatalogText.literal("Outline"), true);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 255, 128, 0, 200, false);
        com.blamejared.clumps.client.modules.impl.render.LightESP.assignBootstrapSettings(Module, preparedMaxY, preparedMinLight, preparedTracers, preparedOutline, preparedColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.LightESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.MaceSwap Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSpearDTap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Spear D-tap"), true);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedStunSwitchDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Stun Switch Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.FloatSetting preparedMinimumDamage = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Minimum Damage"), 10.0f, 0.0f, 35.0f, 1.0f, CatalogText.literal(" Dmg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedStunSlam = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stun Slam"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));

        com.blamejared.clumps.client.modules.impl.combat.MaceSwap.assignBootstrapSettings(Module, preparedSpearDTap, preparedHotbarWhitelist, preparedStunSwitchDelay, preparedReturnItems, preparedMinimumDamage, preparedReturnTo, preparedReturnSlots, preparedItemWhitelist, preparedStunSlam, preparedDelay);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.MaceSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.MLG Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedDistanceToGround = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Distance To Ground"), 24, 1, 24, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedItems = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Items"), com.blamejared.clumps.client.modules.impl.utility.MLG.bootstrapItemsOptionSet());
        com.blamejared.clumps.client.settings.IntSetting preparedFallDistanceThreshold = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Fall Distance Threshold"), 5, 0, 20, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilent = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent"), true);

        com.blamejared.clumps.client.modules.impl.utility.MLG.assignBootstrapSettings(Module, preparedDistanceToGround, preparedItems, preparedFallDistanceThreshold, preparedAutoAim, preparedSilent);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.MLG.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.MultiUse Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.NoBreakDelay Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.NoJumpDelay Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.NoRender Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedNoSpyglassOverlay = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Spyglass Overlay"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoNausea = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Nausea"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoPortalOverlay = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Portal Overlay"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoFireOverlay = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Fire Overlay"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoFloatingItem = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Floating Item"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoVignette = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Vignette"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoCrystalBounce = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Crystal Bounce"), false);

        com.blamejared.clumps.client.modules.impl.render.NoRender.noSpyglassOverlay = preparedNoSpyglassOverlay;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noNausea = preparedNoNausea;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noPortalOverlay = preparedNoPortalOverlay;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noFireOverlay = preparedNoFireOverlay;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noFloatingItem = preparedNoFloatingItem;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noVignette = preparedNoVignette;
        com.blamejared.clumps.client.modules.impl.render.NoRender.noCrystalBounce = preparedNoCrystalBounce;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noFireOverlay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noNausea);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noVignette);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noFloatingItem);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noSpyglassOverlay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noPortalOverlay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.NoRender.noCrystalBounce);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.NoSlow Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedDelayTicks = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Delay Ticks"), 2, 0, 10);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("vanilla"), new java.lang.CharSequence[] {CatalogText.literal("vanilla"), CatalogText.literal("Grim")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedEnableSneak = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Enable Sneak"), false);

        com.blamejared.clumps.client.modules.impl.movement.NoSlow.assignBootstrapSettings(Module, preparedDelayTicks, preparedMode, preparedEnableSneak);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.NoSlow.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Notifications Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedDuration = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Duration"), 1.5f, 0.5f, 8.0f, 0.1f, "s");

        com.blamejared.clumps.client.modules.impl.render.Notifications.assignBootstrapSettings(Module, preparedDuration);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Notifications.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.NullMove Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Offhand Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Particles Module) {

        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), 255, 92, 20, 220);
        com.blamejared.clumps.client.settings.IntSetting preparedRadius = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Radius"), 24, 8, 64, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.1f, 3.0f, 0.1f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.IntSetting preparedDensity = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Density"), 120, 10, 400, CatalogText.literal(" particles"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedTwinkle = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Twinkle"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedSize = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Size"), 1.0f, 0.2f, 3.0f, 0.1f, CatalogText.literal("x"));

        com.blamejared.clumps.client.modules.impl.render.Particles.assignBootstrapSettings(Module, preparedColor, preparedRadius, preparedSpeed, preparedDensity, preparedTwinkle, preparedSize);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Particles.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.PearlCatch Module) {

        com.blamejared.clumps.client.settings.KeybindSetting preparedLowPearlCatch = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Low Pearl Catch"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedInstantPearlCatch = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Instant Pearl Catch"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedHighDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("High Delay"), 2.0f, 2.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedRetry = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Retry"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedHighPearlCatch = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("High Pearl Catch"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);

        com.blamejared.clumps.client.modules.impl.utility.PearlCatch.assignBootstrapSettings(Module, preparedLowPearlCatch, preparedAutoAim, preparedInstantPearlCatch, preparedHighDelay, preparedSwitchBack, preparedRetry, preparedHighPearlCatch, preparedSilentAim);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.PearlCatch.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.PearlGrapple Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedMaxPredictTicks = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Max Predict Ticks"), 60, 5, 80, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedDistance = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Distance"), 48.0f, 3.0f, 48.0f, 1.0f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        com.blamejared.clumps.client.modules.impl.utility.PearlGrapple.assignBootstrapSettings(Module, preparedMaxPredictTicks, preparedDistance, preparedSilentRotations);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.PearlGrapple.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.Piercing Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedSortTargetsBy = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Sort Targets By"), CatalogText.literal("Hurt Time"), new java.lang.CharSequence[] {CatalogText.literal("Hurt Time"), CatalogText.literal("Health")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedBlocks = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Blocks"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedInteract = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Interact"), false);

        com.blamejared.clumps.client.modules.impl.combat.Piercing.sortTargetsBy = preparedSortTargetsBy;
        com.blamejared.clumps.client.modules.impl.combat.Piercing.blocks = preparedBlocks;
        com.blamejared.clumps.client.modules.impl.combat.Piercing.interact = preparedInteract;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.combat.Piercing.blocks);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.combat.Piercing.interact);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.combat.Piercing.sortTargetsBy);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.combat.Piercing.holdingBlock);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.PlayerESP Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedHealthColor = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Health Color"), CatalogText.literal("Dynamic"), new java.lang.CharSequence[] {CatalogText.literal("Dynamic"), CatalogText.literal("Static")});
        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), 2139881472);
        com.blamejared.clumps.client.settings.BooleanSetting preparedTeamColor = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Team Color"), false);
        com.blamejared.clumps.client.settings.ColorSetting preparedFullHealthColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Full Health Color"), -13899678);
        com.blamejared.clumps.client.settings.BooleanSetting preparedRenderSelf = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Render Self"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowName = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Name"), false);
        com.blamejared.clumps.client.settings.ColorSetting preparedHealthBarColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Health Bar Color"), -13899678);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowHeldItem = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Held Item"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedFill = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Fill"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedTracers = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Tracers"), true);
        com.blamejared.clumps.client.settings.ColorSetting preparedLowHealthColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Low Health Color"), -1692114);
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedModes = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Modes"), new java.lang.CharSequence[] {CatalogText.literal("Box"), CatalogText.literal("2D")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedHealthBar = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Health Bar"), CatalogText.literal("Left"), new java.lang.CharSequence[] {CatalogText.literal("None"), CatalogText.literal("Left"), CatalogText.literal("Right"), CatalogText.literal("Bottom")});

        com.blamejared.clumps.client.modules.impl.render.PlayerESP.assignBootstrapSettings(Module, preparedHealthColor, preparedColor, preparedTeamColor, preparedFullHealthColor, preparedRenderSelf, preparedShowName, preparedHealthBarColor, preparedShowHeldItem, preparedFill, preparedTracers, preparedLowHealthColor, preparedModes, preparedHealthBar);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.PlayerESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.PotionEffects Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.PotRefill Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.utility.PotRefill.bootstrapModeOption3(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.utility.PotRefill.bootstrapModeOption3(), com.blamejared.clumps.client.modules.impl.utility.PotRefill.bootstrapModeOption(), com.blamejared.clumps.client.modules.impl.utility.PotRefill.bootstrapModeOption2()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedRefillBuffPotions = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Refill Buff Potions"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 10.0f, 50.0f, 0.0f, 400.0f, 5.0f, CatalogText.literal("ms"));

        com.blamejared.clumps.client.modules.impl.utility.PotRefill.assignBootstrapSettings(Module, preparedMode, preparedRefillBuffPotions, preparedDelay);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.PotRefill.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.internals.Prevent Module) {

        com.blamejared.clumps.client.settings.ItemListSetting preparedPreventWithItemList = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Prevent With"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoAirHits = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Air Hits"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoShieldWithBlocks = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Shield With Blocks"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedInHotbar = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("In Hotbar"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedUntotem = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Untotem"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnObsidian = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Obsidian"), true);
        com.blamejared.clumps.client.settings.ItemListSetting preparedPreventWithItemList2 = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Prevent With"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoMining = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Mining"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedEChestOpen = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("EChest Open"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedInInventory = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("In Inventory"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAnchorCharge = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Anchor Charge"), true);

        com.blamejared.clumps.client.modules.impl.internals.Prevent.assignBootstrapSettings(Module, preparedPreventWithItemList, preparedNoAirHits, preparedNoShieldWithBlocks, preparedInHotbar, preparedUntotem, preparedOnObsidian, preparedPreventWithItemList2, preparedNoMining, preparedEChestOpen, preparedInInventory, preparedAnchorCharge);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.internals.Prevent.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.internals.Protect Module) {

        com.blamejared.clumps.client.settings.TextSetting preparedSkin = new com.blamejared.clumps.client.settings.TextSetting(CatalogText.literal("Skin"), "Notch");
        com.blamejared.clumps.client.settings.TextSetting preparedNickname = new com.blamejared.clumps.client.settings.TextSetting(CatalogText.literal("Nickname"), "Player");

        com.blamejared.clumps.client.modules.impl.internals.Protect.skin = preparedSkin;
        com.blamejared.clumps.client.modules.impl.internals.Protect.nickname = preparedNickname;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.internals.Protect.nickname);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.internals.Protect.skin);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.Replenish Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedMinPercent = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Min %"), 25.0f, 0.0f, 100.0f, 1.0f);
        com.blamejared.clumps.client.settings.IntSetting preparedCursorSpeed = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Cursor Speed"), 20, 1, 100);

        com.blamejared.clumps.client.modules.impl.utility.Replenish.assignBootstrapSettings(Module, preparedMinPercent, preparedCursorSpeed);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.Replenish.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.Scaffold Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedPitchCheck = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Pitch Check"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.world.Scaffold.bootstrapModeOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.world.Scaffold.bootstrapModeOption()});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedListType = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("List Type"), com.blamejared.clumps.client.modules.impl.world.Scaffold.bootstrapListTypeOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.world.Scaffold.bootstrapListTypeOption(), com.blamejared.clumps.client.modules.impl.world.Scaffold.bootstrapListTypeOption2()});

        com.blamejared.clumps.client.modules.impl.world.Scaffold.assignBootstrapSettings(Module, preparedPitchCheck, preparedMode, preparedListType);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.world.Scaffold.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.internals.SelfDestruct Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedReplaceMods = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Replace Mods"), false);
        com.blamejared.clumps.client.settings.TextSetting preparedUrl = new com.blamejared.clumps.client.settings.TextSetting(CatalogText.literal("URL"), "");

        com.blamejared.clumps.client.modules.impl.internals.SelfDestruct.assignBootstrapSettings(Module, preparedReplaceMods, preparedUrl);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.internals.SelfDestruct.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedUnblockOwnShield = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Unblock Own Shield"), true);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), false);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption3(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption3(), com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption2(), com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption()});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption2(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption2(), com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption()});
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedAttackDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Swap"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedReactionDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedStun = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stun"), false);
        com.blamejared.clumps.client.settings.IntSetting preparedStunChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Stun Chance"), 100, 0, 100, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwapBackDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.assignBootstrapSettings(Module, preparedUnblockOwnShield, preparedHotbarWhitelist, preparedReturnSlots, preparedSwapBack, preparedReturnTo, preparedMode, preparedAttackDelay, preparedAutoSwap, preparedReactionDelay, preparedReturnItems, preparedStun, preparedStunChance, preparedSwapBackDelay, preparedItemWhitelist);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.ShieldBreaker.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.internals.SilentAim Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedRenderRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Render Rotations"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.internals.SilentAim.bootstrapSilentMode(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.internals.SilentAim.bootstrapNoneMode(), com.blamejared.clumps.client.modules.internals.SilentAim.bootstrapSilentMode(), com.blamejared.clumps.client.modules.internals.SilentAim.bootstrapStrictMode()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedServerSync = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Server Sync"), true);

        com.blamejared.clumps.client.modules.internals.SilentAim.assignBootstrapSettings(Module, preparedRenderRotations, preparedMode, preparedServerSync);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.internals.SilentAim.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.SkyShaders Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedStars = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Stars"), 1.0f, 0.0f, 2.0f, 0.05f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Nebula"), new java.lang.CharSequence[] {CatalogText.literal("Nebula"), CatalogText.literal("Aurora"), CatalogText.literal("Twilight")});
        com.blamejared.clumps.client.settings.ColorSetting preparedSecondaryColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Secondary color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapSecondaryColor(), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedEndDimension = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("End dimension"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedBrightness = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Brightness"), 1.0f, 0.1f, 2.0f, 0.05f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedCustomColors = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Custom colors"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSunAndMoon = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Sun & moon"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.0f, 3.0f, 0.05f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.ColorSetting preparedPrimaryColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Primary color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor(), false);

        com.blamejared.clumps.client.modules.impl.render.SkyShaders.assignBootstrapSettings(Module, preparedStars, preparedMode, preparedSecondaryColor, preparedEndDimension, preparedBrightness, preparedCustomColors, preparedSunAndMoon, preparedSpeed, preparedPrimaryColor);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.SkyShaders.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.SmartPot Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoBuff = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Buff"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedThrowDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Throw Delay"), 150.0f, 250.0f, 0.0f, 450.0f, 5.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThrowPotion = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Throw Potion"), false);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwapBackDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 50.0f, 100.0f, 0.0f, 500.0f, 5.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAngleCheck = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Angle Check"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedFov = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("FOV"), 0.0f, 20.0f, 0.0f, 90.0f, 1.0f);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedMinHealth = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Min Health"), 16.0f, 16.0f, 0.0f, 20.0f, 1.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);

        com.blamejared.clumps.client.modules.impl.utility.SmartPot.assignBootstrapSettings(Module, preparedAutoBuff, preparedThrowDelay, preparedThrowPotion, preparedSwapBackDelay, preparedSwapBack, preparedSilentAim, preparedAngleCheck, preparedFov, preparedMinHealth, preparedAutoAim);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.SmartPot.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.SpearAssist Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimMath = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAimVector = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        com.blamejared.clumps.client.settings.FloatSetting preparedMultipoint = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatSetting preparedVerticalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetPriority = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 28.0f, 1.0f, 128.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedHorizontalSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedFov = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedRandom = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);

        com.blamejared.clumps.client.modules.impl.combat.SpearAssist.assignBootstrapSettings(Module, preparedAimMath, preparedThroughWalls, preparedAimVector, preparedMultipoint, preparedVerticalSpeed, preparedTargetPriority, preparedRange, preparedHorizontalSpeed, preparedFov, preparedRandom, preparedSilentRotations);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.SpearAssist.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.SpearLunge Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedCooldown = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Cooldown"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Paper")});
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Attack"), new java.lang.CharSequence[] {CatalogText.literal("Attack"), CatalogText.literal("Keypress")});
        com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        com.blamejared.clumps.client.settings.IntSetting preparedCooldownPercent = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Cooldown %"), 85, 0, 100);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilent = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent"), false);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});

        com.blamejared.clumps.client.modules.impl.combat.SpearLunge.assignBootstrapSettings(Module, preparedCooldown, preparedActivateKey, preparedMode, preparedReturnItems, preparedCooldownPercent, preparedHotbarWhitelist, preparedItemWhitelist, preparedSilent, preparedReturnSlots, preparedReturnTo);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.SpearLunge.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.SpearReach Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedPaperMCSupport = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("PaperMC support"), false);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        com.blamejared.clumps.client.modules.impl.combat.SpearReach.assignBootstrapSettings(Module, preparedPaperMCSupport, preparedHotbarWhitelist, preparedItemWhitelist);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.SpearReach.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Speed Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedStrafe = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Strafe"), 1.0f, 0.01f, 10.0f, 0.01f);
        com.blamejared.clumps.client.settings.FloatSetting preparedBoatBoost = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Boat Boost"), 0.5f, 0.01f, 0.75f, 0.01f);
        com.blamejared.clumps.client.settings.FloatSetting preparedMotion = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Motion"), 1.0f, 0.01f, 10.0f, 0.01f);
        com.blamejared.clumps.client.settings.FloatSetting preparedBoost = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Boost"), 0.1f, 0.01f, 0.5f, 0.01f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("MinemenClub"), new java.lang.CharSequence[] {CatalogText.literal("MinemenClub"), CatalogText.literal("Legit"), CatalogText.literal("Grim Entity"), CatalogText.literal("Motion"), CatalogText.literal("Strafe")});

        com.blamejared.clumps.client.modules.impl.movement.Speed.assignBootstrapSettings(Module, preparedStrafe, preparedBoatBoost, preparedMotion, preparedBoost, preparedMode);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Speed.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Sprint Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedOmni = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Omni"), false);

        com.blamejared.clumps.client.modules.impl.movement.Sprint.assignBootstrapSettings(Module, preparedOmni);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Sprint.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.SprintReset Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedDurationBetweenReset = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Duration Between Reset"), 0, 0, 20, CatalogText.literal(" Ticks"));
        com.blamejared.clumps.client.settings.IntSetting preparedDuration = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Duration"), 3, 1, 20, CatalogText.literal(" Ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedGroundOnly = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Ground Only"), false);
        com.blamejared.clumps.client.settings.IntSetting preparedReactionTime = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Reaction Time"), 0, 0, 20, CatalogText.literal(" Ticks"));

        com.blamejared.clumps.client.modules.impl.movement.SprintReset.assignBootstrapSettings(Module, preparedDurationBetweenReset, preparedDuration, preparedGroundOnly, preparedReactionTime);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.SprintReset.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Step Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedDisableOnSneak = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Disable on sneak"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedHeight = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Height"), 1.0f, 1.0f, 3.0f, 0.1f, CatalogText.literal(" blocks"));

        com.blamejared.clumps.client.modules.impl.movement.Step.assignBootstrapSettings(Module, preparedDisableOnSneak, preparedHeight);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Step.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.SwingSpeed Module) {

        com.blamejared.clumps.client.settings.IntSetting preparedSwingSpeed = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Swing Speed"), 7, 1, 20, CatalogText.literal(" ticks"));

        com.blamejared.clumps.client.modules.impl.render.SwingSpeed.swingSpeed = preparedSwingSpeed;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.SwingSpeed.swingSpeed);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Target Module) {

        Module.settings.clear();
        com.blamejared.clumps.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        com.blamejared.clumps.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.TargetESP Module) {

        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor().getRed(), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor().getGreen(), com.blamejared.clumps.client.ui.ColorTheme.bootstrapPrimaryColor().getBlue(), 200, true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), com.blamejared.clumps.client.modules.impl.render.TargetESP.bootstrapModeOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.render.TargetESP.bootstrapModeOption()});

        com.blamejared.clumps.client.modules.impl.render.TargetESP.assignBootstrapSettings(Module, preparedColor, preparedMode);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.TargetESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.internals.Targets Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedAntiBot = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("AntiBot"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedKeepTargetRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Keep Target Range"), 16.0f, 1.0f, 50.0f, 0.5f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedIgnoreFriends = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Ignore Friends"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedLastAttacked = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Last Attacked"), false);
        com.blamejared.clumps.client.settings.IntSetting preparedShieldTicks = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Shield Ticks"), 5, 1, 10, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedAntiBotChecks = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("AntiBot Checks"), new java.lang.CharSequence[] {CatalogText.literal("Name"), CatalogText.literal("Movement"), CatalogText.literal("Tab List"), CatalogText.literal("Scale"), CatalogText.literal("Bliss"), CatalogText.literal("Zeta / MMC")});
        com.blamejared.clumps.client.settings.KeybindSetting preparedManualTargetSelect = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Manual Target Select"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedShieldDetection = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Shield Detection"), com.blamejared.clumps.client.modules.internals.Targets.bootstrapShieldDetectionOption(), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.internals.Targets.bootstrapShieldDetectionOption(), com.blamejared.clumps.client.modules.internals.Targets.bootstrapShieldDetectionOption2()});
        com.blamejared.clumps.client.settings.BooleanSetting preparedIgnoreTeams = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Ignore Teams"), false);
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedEntityTypes = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Entity Types"), new java.lang.CharSequence[] {CatalogText.literal("Players"), CatalogText.literal("Hostile"), CatalogText.literal("Passive"), CatalogText.literal("Neutral"), CatalogText.literal("Armor Stands"), CatalogText.literal("Invisible"), CatalogText.literal("Naked")});
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedTeamChecks = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Team Checks"), new java.lang.CharSequence[] {CatalogText.literal("Color"), CatalogText.literal("Name"), CatalogText.literal("Scoreboard")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedKeepTarget = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Keep Target"), true);
        com.blamejared.clumps.client.settings.KeybindSetting preparedFriendBind = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Friend Bind"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPingCompensation = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Ping Compensation"), false);

        com.blamejared.clumps.client.modules.internals.Targets.assignBootstrapSettings(Module, preparedAntiBot, preparedKeepTargetRange, preparedIgnoreFriends, preparedLastAttacked, preparedShieldTicks, preparedAntiBotChecks, preparedManualTargetSelect, preparedShieldDetection, preparedIgnoreTeams, preparedEntityTypes, preparedTeamChecks, preparedKeepTarget, preparedFriendBind, preparedPingCompensation);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.internals.Targets.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Timer Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedTimer = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Timer"), 1.0f, 0.5f, 10.0f, 0.05f, CatalogText.literal("x"));

        com.blamejared.clumps.client.modules.impl.movement.Timer.assignBootstrapSettings(Module, preparedTimer);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Timer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.TotemHand Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBackOnClose = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap back on close"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAnchors = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Anchors"), true);
        com.blamejared.clumps.client.settings.IntSetting preparedInventoryChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Inventory chance"), 100, 0, 100, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedPredictive = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Predictive"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedKillable = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Killable"), false);
        com.blamejared.clumps.client.settings.IntSetting preparedMinDamage = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Min damage"), 10, 1, 30, CatalogText.literal(" dmg"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedCrystals = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Crystals"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnPop = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On pop"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBackOnSafe = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap back on safe"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnInventoryOpen = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On inventory open"), false);

        com.blamejared.clumps.client.modules.impl.utility.TotemHand.assignBootstrapSettings(Module, preparedSwapBackOnClose, preparedAnchors, preparedInventoryChance, preparedPredictive, preparedKillable, preparedMinDamage, preparedCrystals, preparedOnPop, preparedSwapBackOnSafe, preparedOnInventoryOpen);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.TotemHand.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.TotemHit Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Swap"), false);

        com.blamejared.clumps.client.modules.impl.combat.TotemHit.assignBootstrapSettings(Module, preparedDelay, preparedSilentSwap);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.TotemHit.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Trails Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedGradient = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Gradient"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedHeight = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Height"), 1.0f, 0.2f, 2.5f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedLifetime = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Lifetime"), 3.0f, 0.5f, 10.0f, 0.5f, CatalogText.literal("s"));
        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 160);

        com.blamejared.clumps.client.modules.impl.render.Trails.assignBootstrapSettings(Module, preparedGradient, preparedHeight, preparedLifetime, preparedColor);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Trails.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Trajectories Module) {

        com.blamejared.clumps.client.settings.ColorSetting preparedArrowColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Arrow Color"), 255, 128, 128, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedPearlColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Pearl Color"), 102, 179, 255, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedWindChargeColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Wind Charge Color"), 255, 209, 102, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedPotionColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Potion Color"), 124, 192, 255, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedFireworkColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Firework Color"), 255, 196, 130, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedBowColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Bow Color"), 148, 114, 255, 200, true);
        com.blamejared.clumps.client.settings.ColorSetting preparedCrossbowColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Crossbow Color"), 114, 216, 180, 200, true);
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedTrajectories = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Trajectories"), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption2(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption5(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption3(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption4(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption7(), com.blamejared.clumps.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption6()});

        com.blamejared.clumps.client.modules.impl.render.Trajectories.assignBootstrapSettings(Module, preparedArrowColor, preparedPearlColor, preparedWindChargeColor, preparedPotionColor, preparedFireworkColor, preparedBowColor, preparedCrossbowColor, preparedTrajectories);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Trajectories.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.internals.Transactions Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.TriggerBot Module) {

        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCooldown = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Cooldown"), 90.0f, 95.0f, 0.0f, 100.0f, 1.0f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedCritMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Crit Mode"), CatalogText.literal("pCrit"), new java.lang.CharSequence[] {CatalogText.literal("pCrit"), CatalogText.literal("Normal"), CatalogText.literal("None")});
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedReactionDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedCritOnlyOnSpace = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Crit only on space"), false);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedAttackDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 800.0f, 1.0f);
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        com.blamejared.clumps.client.settings.FloatSetting preparedAttackDistance = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Attack Distance"), 0.0f, 0.0f, 6.0f, 0.01f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedNoSweepOrPickHits = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("No Sweep or Pick Hits"), false);
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedHurtTimeCheck = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("HurtTime Check"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAttackShields = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Attack Shields"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCritRange = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Crit Range"), 0.0f, 6.0f, 0.0f, 6.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCritCooldown = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Crit Cooldown"), 90.0f, 100.0f, 0.0f, 100.0f, 1.0f);

        com.blamejared.clumps.client.modules.impl.combat.TriggerBot.assignBootstrapSettings(Module, preparedCooldown, preparedCritMode, preparedReactionDelay, preparedCritOnlyOnSpace, preparedAttackDelay, preparedItemWhitelist, preparedAttackDistance, preparedNoSweepOrPickHits, preparedHotbarWhitelist, preparedHurtTimeCheck, preparedAttackShields, preparedCritRange, preparedCritCooldown);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.TriggerBot.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.Tunneler Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedSilent = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("2x1"), new java.lang.CharSequence[] {CatalogText.literal("2x1"), CatalogText.literal("1x1")});

        com.blamejared.clumps.client.modules.impl.world.Tunneler.assignBootstrapSettings(Module, preparedSilent, preparedMode);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.world.Tunneler.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Velocity Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedAmount = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Amount"), 0.0f, 0.0f, 1.0f, 0.01f);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Cancel"), new java.lang.CharSequence[] {CatalogText.literal("Cancel"), CatalogText.literal("MinemenClub")});
        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedSources = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Sources"), new java.lang.CharSequence[] {CatalogText.literal("Hits"), CatalogText.literal("Explosions")});

        com.blamejared.clumps.client.modules.impl.movement.Velocity.assignBootstrapSettings(Module, preparedAmount, preparedMode, preparedSources);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Velocity.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.ViewModel Module) {

        com.blamejared.clumps.client.settings.FloatSetting preparedPosXOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosX Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotXMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotX Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedEatingBob = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("EatingBob"), 1.0f, 0.0f, 1.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotYMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotY Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedStyles = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Styles"), CatalogText.literal("Default"), new java.lang.CharSequence[] {CatalogText.literal("Default"), CatalogText.literal("None"), CatalogText.literal("Custom"), CatalogText.literal("Smooth"), CatalogText.literal("Bounce"), CatalogText.literal("Slash"), CatalogText.literal("Punch"), CatalogText.literal("Swipe"), CatalogText.literal("Spin360"), CatalogText.literal("Scale"), CatalogText.literal("Spin"), CatalogText.literal("Katana"), CatalogText.literal("Sigma"), CatalogText.literal("Push"), CatalogText.literal("Stab"), CatalogText.literal("Slice"), CatalogText.literal("Exhibition")});
        com.blamejared.clumps.client.settings.FloatSetting preparedStrengthOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Strength Off"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedStrengthMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Strength Main"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedScaleOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Scale Off"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRotYOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotY Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPosXMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosX Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotZMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotZ Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedScaleMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Scale Main"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPosYOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosY Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotZOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotZ Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedEatingAnimation = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Eating Animation"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedPosZMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosZ Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        com.blamejared.clumps.client.settings.FloatSetting preparedPosZOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosZ Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotXOff = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("RotX Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.FloatSetting preparedPosYMain = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("PosY Main"), 0.0f, -3.0f, 3.0f, 0.2f);

        com.blamejared.clumps.client.modules.impl.render.ViewModel.posXOff = preparedPosXOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotXMain = preparedRotXMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.eatingBob = preparedEatingBob;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotYMain = preparedRotYMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.styles = preparedStyles;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.strengthOff = preparedStrengthOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.strengthMain = preparedStrengthMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.scaleOff = preparedScaleOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotYOff = preparedRotYOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.posXMain = preparedPosXMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotZMain = preparedRotZMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.scaleMain = preparedScaleMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.posYOff = preparedPosYOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotZOff = preparedRotZOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.eatingAnimation = preparedEatingAnimation;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.posZMain = preparedPosZMain;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.posZOff = preparedPosZOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.rotXOff = preparedRotXOff;
        com.blamejared.clumps.client.modules.impl.render.ViewModel.posYMain = preparedPosYMain;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.eatingAnimation);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.eatingBob);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.styles);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.strengthMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.strengthOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.scaleMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.scaleOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posXMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posYMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posZMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posXOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posYOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.posZOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotXMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotYMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotZMain);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotXOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotYOff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.ViewModel.rotZOff);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.XBow Module) {

        com.blamejared.clumps.client.settings.MultiChoiceSetting preparedPlace = new com.blamejared.clumps.client.settings.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {com.blamejared.clumps.client.modules.impl.combat.XBow.bootstrapPlaceOption2(), com.blamejared.clumps.client.modules.impl.combat.XBow.bootstrapPlaceOption4(), com.blamejared.clumps.client.modules.impl.combat.XBow.bootstrapPlaceOption3(), com.blamejared.clumps.client.modules.impl.combat.XBow.bootstrapPlaceOption()});
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        com.blamejared.clumps.client.modules.impl.combat.XBow.assignBootstrapSettings(Module, preparedPlace, preparedActivateKey, preparedAutoAim, preparedSilentRotations);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.XBow.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Xray Module) {

        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughLiquids = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Liquids"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedExposedToAir = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Exposed to Air"), false);

        com.blamejared.clumps.client.modules.impl.render.Xray.throughLiquids = preparedThroughLiquids;
        com.blamejared.clumps.client.modules.impl.render.Xray.exposedToAir = preparedExposedToAir;

        Module.settings.clear();
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.Xray.throughLiquids);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.Xray.exposedToAir);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, com.blamejared.clumps.client.modules.impl.render.Xray.blocks);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.DtapSetup Module) {

        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Crystal Bind"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedCheckPlace = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Check Place"), false);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwitchDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Switch Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.IntSetting preparedSwitchChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Switch Chance"), 100, 0, 100, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedPlaceDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Place Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedBreakDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Break Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.IntSetting preparedPlaceChance = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Place Chance"), 100, 0, 100, CatalogText.literal("%"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoCrystalAction = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Crystal"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedTickTiming = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Tick Timing"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedStopOnKill = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Stop on kill"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedFakePunch = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Fake Punch"), false);
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        com.blamejared.clumps.client.modules.impl.combat.DtapSetup.assignBootstrapSettings(Module, preparedActivateKey, preparedCheckPlace, preparedSwitchDelay, preparedSwitchChance, preparedPlaceDelay, preparedBreakDelay, preparedPlaceChance, preparedAutoCrystalAction, preparedTickTiming, preparedStopOnKill, preparedFakePunch, preparedItemWhitelist);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.DtapSetup.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.internals.InputSimulation Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Mode"),
                com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapModeReplay(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapModeOff(),
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapModeReplay(),
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapModeNative()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedOutputTarget = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Output Target"),
                com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapTargetOsAndGame(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapTargetOsAndGame(),
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapTargetGameOnly()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedMouseClicks = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Mouse Clicks"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedHotbarSwaps = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Hotbar Swaps"),
                com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapHotbarKeys(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapHotbarOff(),
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapHotbarKeys(),
                        com.blamejared.clumps.client.modules.internals.InputSimulation.bootstrapHotbarKeysScroll()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedMovementKeys = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Movement Keys"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOffhandSwap = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Offhand Swap"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedInventoryCursor = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Inventory Cursor"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedHoldDuration = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Hold Duration"), 25.0f, 65.0f, 10.0f, 150.0f, 5.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCursorDuration = new com.blamejared.clumps.client.settings.FloatRangeSetting(CatalogText.literal("Cursor Duration"), 60.0f, 180.0f, 20.0f, 350.0f, 10.0f, CatalogText.literal(" ms"));

        com.blamejared.clumps.client.modules.internals.InputSimulation.assignBootstrapSettings(
                Module, preparedMode, preparedOutputTarget, preparedMouseClicks, preparedHotbarSwaps,
                preparedMovementKeys, preparedOffhandSwap, preparedInventoryCursor, preparedHoldDuration, preparedCursorDuration);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.internals.InputSimulation.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoWeb Module) {

        com.blamejared.clumps.client.settings.ChoiceSetting preparedStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Style"), CatalogText.literal("Legit"), new java.lang.CharSequence[] {CatalogText.literal("Legit"), CatalogText.literal("Blatant")});
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("On Key"), new java.lang.CharSequence[] {CatalogText.literal("On Key"), CatalogText.literal("Always")});
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedSwapMode = new com.blamejared.clumps.client.settings.ChoiceSetting(CatalogText.literal("Swap Mode"), CatalogText.literal("Legit"), new java.lang.CharSequence[] {CatalogText.literal("Legit"), CatalogText.literal("Silent")});
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), true);
        com.blamejared.clumps.client.settings.IntSetting preparedSwapDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Swap Delay"), 1, 1, 6, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 4.2f, 1.5f, 5.5f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedFov = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("FOV"), 140.0f, 30.0f, 360.0f, 5.0f, CatalogText.literal(" deg"));
        com.blamejared.clumps.client.settings.IntSetting preparedPredictTicks = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Predict Ticks"), 2, 0, 6, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.IntSetting preparedPlaceDelay = new com.blamejared.clumps.client.settings.IntSetting(CatalogText.literal("Place Delay"), 4, 1, 20, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedDoubleWeb = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Double Web"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAvoidSelf = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Avoid Self"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnlyGrounded = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Only Grounded"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotationSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Rotation Speed"), 8.0f, 1.0f, 15.0f, 0.5f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoClearSelf = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Auto Clear Self"), true);

        com.blamejared.clumps.client.modules.impl.combat.AutoWeb.assignBootstrapSettings(
                Module, preparedStyle, preparedMode, preparedActivateKey,
                preparedSwapMode, preparedSwapBack, preparedSwapDelay,
                preparedRange, preparedFov, preparedPredictTicks,
                preparedPlaceDelay, preparedDoubleWeb, preparedAvoidSelf, preparedOnlyGrounded,
                preparedSilentRotations, preparedRotationSpeed, preparedAutoClearSelf);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoWeb.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.AutoConfigHud Module) {

        com.blamejared.clumps.client.settings.KeybindSetting preparedUnlockKey = new com.blamejared.clumps.client.settings.KeybindSetting(CatalogText.literal("Unlock Key"), com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedHideWhenIdle = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Hide When Idle"), false);

        com.blamejared.clumps.client.modules.impl.render.AutoConfigHud.assignBootstrapSettings(Module, preparedUnlockKey, preparedHideWhenIdle);

        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.AutoConfigHud.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.PopChams Module) {
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnTotemPop = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Totem Pop"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnHit = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Hit"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnDeath = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Death"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedDuration = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Duration"), 1.2f, 0.2f, 4.0f, 0.1f, CatalogText.literal(" s"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRiseHeight = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Rise Height"), 1.2f, 0.0f, 4.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.ColorSetting preparedFillColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Fill Color"), 255, 255, 255, 75);
        com.blamejared.clumps.client.settings.ColorSetting preparedOutlineColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Outline Color"), 255, 255, 255, 220);

        com.blamejared.clumps.client.modules.impl.render.PopChams.assignBootstrapSettings(
                Module, preparedOnTotemPop, preparedOnHit, preparedOnDeath, preparedThroughWalls,
                preparedDuration, preparedRiseHeight, preparedFillColor, preparedOutlineColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.PopChams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.JumpCircles Module) {
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnJump = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Jump"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnLand = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("On Land"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedOtherPlayers = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Other Players"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedMaxRadius = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Max Radius"), 1.3f, 0.4f, 3.0f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedDuration = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Duration"), 0.85f, 0.2f, 2.5f, 0.05f, CatalogText.literal(" s"));
        com.blamejared.clumps.client.settings.ColorSetting preparedColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Color"), 255, 255, 255, 210);

        com.blamejared.clumps.client.modules.impl.render.JumpCircles.assignBootstrapSettings(
                Module, preparedOnJump, preparedOnLand, preparedOtherPlayers, preparedMaxRadius, preparedDuration, preparedColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.JumpCircles.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.BlockHighlight Module) {
        com.blamejared.clumps.client.settings.BooleanSetting preparedTargetHighlight = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Target Highlight"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPlacementGhost = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Placement Ghost"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        com.blamejared.clumps.client.settings.FloatSetting preparedSmoothSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Smooth Speed"), 18.0f, 2.0f, 40.0f, 1.0f);
        com.blamejared.clumps.client.settings.FloatSetting preparedGhostDuration = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Ghost Duration"), 0.65f, 0.15f, 2.5f, 0.05f, CatalogText.literal(" s"));
        com.blamejared.clumps.client.settings.ColorSetting preparedFillColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Fill Color"), 255, 255, 255, 42);
        com.blamejared.clumps.client.settings.ColorSetting preparedOutlineColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Outline Color"), 255, 255, 255, 200);
        com.blamejared.clumps.client.settings.ColorSetting preparedGhostColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Ghost Color"), 180, 220, 255, 85);

        com.blamejared.clumps.client.modules.impl.render.BlockHighlight.assignBootstrapSettings(
                Module, preparedTargetHighlight, preparedPlacementGhost, preparedThroughWalls,
                preparedSmoothSpeed, preparedGhostDuration, preparedFillColor, preparedOutlineColor, preparedGhostColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.BlockHighlight.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.PearlMarkers Module) {
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowPearls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Ender Pearls"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowWindCharges = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Wind Charges"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowBeam = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Beacon Beam"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowEtaTag = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("ETA Tag"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        com.blamejared.clumps.client.settings.ColorSetting preparedPearlColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Pearl Color"), 170, 120, 255, 220);
        com.blamejared.clumps.client.settings.ColorSetting preparedWindColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Wind Color"), 190, 235, 255, 220);

        com.blamejared.clumps.client.modules.impl.render.PearlMarkers.assignBootstrapSettings(
                Module, preparedShowPearls, preparedShowWindCharges, preparedShowBeam, preparedShowEtaTag,
                preparedThroughWalls, preparedPearlColor, preparedWindColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.PearlMarkers.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.TargetHalo Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Style"),
                com.blamejared.clumps.client.modules.impl.render.TargetHalo.bootstrapOrbitStyle(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.render.TargetHalo.bootstrapOrbitStyle(),
                        com.blamejared.clumps.client.modules.impl.render.TargetHalo.bootstrapDoubleStyle(),
                        com.blamejared.clumps.client.modules.impl.render.TargetHalo.bootstrapGroundStyle()
                });
        com.blamejared.clumps.client.settings.FloatSetting preparedSpeed = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.2f, 0.2f, 4.0f, 0.1f);
        com.blamejared.clumps.client.settings.FloatSetting preparedRadius = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Extra Radius"), 0.22f, 0.0f, 1.0f, 0.02f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Range"), 12.0f, 3.0f, 40.0f, 0.5f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedThroughWalls = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        com.blamejared.clumps.client.settings.ColorSetting preparedPrimaryColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Primary Color"), 255, 255, 255, 230);
        com.blamejared.clumps.client.settings.ColorSetting preparedSecondaryColor = new com.blamejared.clumps.client.settings.ColorSetting(CatalogText.literal("Secondary Color"), 155, 195, 255, 210);

        com.blamejared.clumps.client.modules.impl.render.TargetHalo.assignBootstrapSettings(
                Module, preparedStyle, preparedSpeed, preparedRadius, preparedRange,
                preparedThroughWalls, preparedPrimaryColor, preparedSecondaryColor);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.TargetHalo.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Keystrokes Module) {
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowCps = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show CPS"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowSpace = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Space"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedShowPing = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Show Ping"), true);

        com.blamejared.clumps.client.modules.impl.render.Keystrokes.assignBootstrapSettings(
                Module, preparedShowCps, preparedShowSpace, preparedShowPing);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Keystrokes.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.render.Animations Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedAttackStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Attack Style"),
                com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackSlash(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapNone(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackSlash(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackBlockHit(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackExhibition(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackSigma(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackKatana(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackArc(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackThrust(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackFlick(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackSpin(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapAttackBreeze()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedUseStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Use Style"),
                com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUseLowGuard(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapNone(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUseSip(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUseLowGuard(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUsePulse(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUseCleanTilt(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapUseFloating()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedPlaceStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Place Style"),
                com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlacePop(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapNone(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlacePop(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlaceSlide(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlaceSnap(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlaceFlip(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapPlaceBounce()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedIdleStyle = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Idle Style"),
                com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapNone(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapNone(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapIdleBreathe(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapIdleFloat(),
                        com.blamejared.clumps.client.modules.impl.render.Animations.bootstrapIdleSway()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedSmoothEquip = new com.blamejared.clumps.client.settings.BooleanSetting(CatalogText.literal("Smooth Equip"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedIntensity = new com.blamejared.clumps.client.settings.FloatSetting(CatalogText.literal("Intensity"), 1.0f, 0.2f, 2.0f, 0.05f);

        com.blamejared.clumps.client.modules.impl.render.Animations.assignBootstrapSettings(
                Module, preparedAttackStyle, preparedUseStyle, preparedPlaceStyle, preparedIdleStyle, preparedSmoothEquip, preparedIntensity);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.render.Animations.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.utility.CartRefill Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Mode"),
                com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapModeAutoOpen(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapModeAutoOpen(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapModeInInventory(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapModeSilent()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedCartType = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Cart Type"),
                com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapCartTnt(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapCartTnt(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapCartAny()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTrigger = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Trigger"),
                com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapTriggerFillSlots(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapTriggerFillSlots(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapTriggerNoCart()
                });
        com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarSlots = new com.blamejared.clumps.client.settings.HotbarSlotsSetting(
                CatalogText.literal("Selected Slots"));
        com.blamejared.clumps.client.settings.ChoiceSetting preparedPickOrder = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Pick From Inv"),
                com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapPickClosest(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapPickClosest(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapPickRandom(),
                        com.blamejared.clumps.client.modules.impl.utility.CartRefill.bootstrapPickFirst()
                });
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(
                CatalogText.literal("Delay"), 50.0f, 120.0f, 0.0f, 500.0f, 10.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedAutoClose = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Auto Close"), true);
        com.blamejared.clumps.client.settings.FloatRangeSetting preparedCloseDelay = new com.blamejared.clumps.client.settings.FloatRangeSetting(
                CatalogText.literal("Close Delay"), 40.0f, 100.0f, 0.0f, 400.0f, 10.0f, CatalogText.literal(" ms"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedOnlyEmptySlot = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Only Empty Slot"), false);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPauseOnUse = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Pause On Use"), true);

        com.blamejared.clumps.client.modules.impl.utility.CartRefill.assignBootstrapSettings(
                Module, preparedMode, preparedCartType, preparedTrigger, preparedHotbarSlots,
                preparedPickOrder, preparedDelay, preparedAutoClose, preparedCloseDelay,
                preparedOnlyEmptySlot, preparedPauseOnUse);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.utility.CartRefill.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.movement.Snaptap Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Priority"),
                com.blamejared.clumps.client.modules.impl.movement.Snaptap.bootstrapModeLastInput(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.movement.Snaptap.bootstrapModeLastInput(),
                        com.blamejared.clumps.client.modules.impl.movement.Snaptap.bootstrapModeFirstInput()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedHorizontal = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Strafe (A/D)"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedVertical = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Forward/Back (W/S)"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPauseWhileSneaking = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Pause While Sneaking"), false);

        com.blamejared.clumps.client.modules.impl.movement.Snaptap.assignBootstrapSettings(
                Module, preparedMode, preparedHorizontal, preparedVertical, preparedPauseWhileSneaking);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.movement.Snaptap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.PlateWeb Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTrigger = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Trigger"),
                com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapTriggerSelf(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapTriggerSelf(),
                        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapTriggerAny()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedSwapMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Swap Mode"),
                com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapSwapLegit(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapSwapLegit(),
                        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.bootstrapSwapSilent()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Swap Back"), true);
        com.blamejared.clumps.client.settings.IntSetting preparedSwapDelay = new com.blamejared.clumps.client.settings.IntSetting(
                CatalogText.literal("Swap Delay"), 1, 0, 5, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.IntSetting preparedPlaceDelay = new com.blamejared.clumps.client.settings.IntSetting(
                CatalogText.literal("Place Delay"), 1, 0, 10, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(
                CatalogText.literal("Range"), 4.5f, 2.0f, 6.0f, 0.1f);
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Silent Rotations"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotationSpeed = new com.blamejared.clumps.client.settings.FloatSetting(
                CatalogText.literal("Rotation Speed"), 18.0f, 5.0f, 40.0f, 1.0f);

        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.assignBootstrapSettings(
                Module, preparedTrigger, preparedSwapMode, preparedSwapBack, preparedSwapDelay,
                preparedPlaceDelay, preparedRange, preparedSilentRotations, preparedRotationSpeed);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.PlateWeb.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.world.GhostHand Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Mode"),
                com.blamejared.clumps.client.modules.impl.world.GhostHand.bootstrapModeAlways(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.world.GhostHand.bootstrapModeAlways(),
                        com.blamejared.clumps.client.modules.impl.world.GhostHand.bootstrapModeHoldKey()
                });
        com.blamejared.clumps.client.settings.KeybindSetting preparedHoldKey = new com.blamejared.clumps.client.settings.KeybindSetting(
                CatalogText.literal("Hold Key"),
                com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.BooleanSetting preparedCobwebs = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Cobwebs"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedPlayersOnly = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Players Only"), true);
        com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist = new com.blamejared.clumps.client.settings.ItemListSetting(
                CatalogText.literal("Item Whitelist"));

        com.blamejared.clumps.client.modules.impl.world.GhostHand.assignBootstrapSettings(
                Module, preparedMode, preparedHoldKey, preparedCobwebs, preparedPlayersOnly, preparedItemWhitelist);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.world.GhostHand.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(com.blamejared.clumps.client.modules.impl.combat.AutoDrain Module) {
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMethod = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Method"),
                com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapMethodAuto(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapMethodAuto(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapMethodBucket(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapMethodWebs(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapMethodBlocks()
                });
        com.blamejared.clumps.client.settings.ChoiceSetting preparedMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Mode"),
                com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapModeAlways(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapModeAlways(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapModeOnKey()
                });
        com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey = new com.blamejared.clumps.client.settings.KeybindSetting(
                CatalogText.literal("Activate Key"),
                com.blamejared.clumps.client.keyboard.InputKey.NONE);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedTargetFilter = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Target"),
                com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapTargetEnemy(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapTargetEnemy(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapTargetSelfEnemy(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapTargetAny()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedDrainWater = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Water"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedDrainLava = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Lava"), true);
        com.blamejared.clumps.client.settings.BooleanSetting preparedIgnoreLakes = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Ignore Lakes"), true);
        com.blamejared.clumps.client.settings.ChoiceSetting preparedSwapMode = new com.blamejared.clumps.client.settings.ChoiceSetting(
                CatalogText.literal("Swap Mode"),
                com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapSwapLegit(),
                new java.lang.CharSequence[] {
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapSwapLegit(),
                        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.bootstrapSwapSilent()
                });
        com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Swap Back"), true);
        com.blamejared.clumps.client.settings.IntSetting preparedSwapDelay = new com.blamejared.clumps.client.settings.IntSetting(
                CatalogText.literal("Swap Delay"), 1, 0, 5, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.IntSetting preparedDrainDelay = new com.blamejared.clumps.client.settings.IntSetting(
                CatalogText.literal("Drain Delay"), 2, 0, 10, CatalogText.literal(" ticks"));
        com.blamejared.clumps.client.settings.FloatSetting preparedRange = new com.blamejared.clumps.client.settings.FloatSetting(
                CatalogText.literal("Range"), 4.5f, 2.0f, 5.5f, 0.1f, CatalogText.literal(" blocks"));
        com.blamejared.clumps.client.settings.BooleanSetting preparedSilentRotations = new com.blamejared.clumps.client.settings.BooleanSetting(
                CatalogText.literal("Silent Rotations"), true);
        com.blamejared.clumps.client.settings.FloatSetting preparedRotationSpeed = new com.blamejared.clumps.client.settings.FloatSetting(
                CatalogText.literal("Rotation Speed"), 18.0f, 5.0f, 40.0f, 1.0f);

        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.assignBootstrapSettings(
                Module, preparedMethod, preparedMode, preparedActivateKey, preparedTargetFilter,
                preparedDrainWater, preparedDrainLava, preparedIgnoreLakes, preparedSwapMode,
                preparedSwapBack, preparedSwapDelay, preparedDrainDelay, preparedRange,
                preparedSilentRotations, preparedRotationSpeed);
        Module.settings.clear();
        com.blamejared.clumps.client.modules.impl.combat.AutoDrain.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

}