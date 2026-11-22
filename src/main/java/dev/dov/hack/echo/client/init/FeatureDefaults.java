package dev.dov.hack.echo.client.init;

import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.settings.*;

public final class FeatureDefaults {
    private FeatureDefaults() {}

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AimAssist Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedFovMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("FOV Mode"), CatalogText.literal("Single"), new java.lang.CharSequence[] {CatalogText.literal("Single"), CatalogText.literal("Split")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPitchFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRandom = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedMultipoint = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnClick = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Click"), false);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedTargetPriority = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedVerticalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedHorizontalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 5.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimMath = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimVector = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedFov = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedYawFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));

        dev.dov.hack.echo.client.modules.impl.combat.AimAssist.assignBootstrapSettings(Module, preparedFovMode, preparedRange, preparedPitchFOV, preparedItemWhitelist, preparedRandom, preparedSilentRotations, preparedMultipoint, preparedOnClick, preparedTargetPriority, preparedThroughWalls, preparedVerticalSpeed, preparedHotbarWhitelist, preparedHorizontalSpeed, preparedAimMath, preparedAimVector, preparedFov, preparedYawFOV);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AimAssist.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AimOptimizer Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedStrength = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Strength"), 35.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedSlowdown = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Slowdown"), 25.0f, 0.0f, 85.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedSmoothing = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Smoothing"), 30.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPrediction = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Prediction"), 60.0f, 0.0f, 150.0f, 5.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedMultipoint = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 100.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedGcd = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("GCD"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPlayersOnly = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Players Only"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedTargetPriority = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("FOV"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 4.5f, 1.0f, 16.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedFov = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("FOV"), 75.0f, 5.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnClick = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Click"), false);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));

        dev.dov.hack.echo.client.modules.impl.combat.AimOptimizer.assignBootstrapSettings(Module, preparedStrength, preparedSlowdown, preparedSmoothing, preparedPrediction, preparedMultipoint, preparedGcd, preparedThroughWalls, preparedPlayersOnly, preparedTargetPriority, preparedRange, preparedFov, preparedOnClick, preparedItemWhitelist, preparedHotbarWhitelist);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AimOptimizer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.AimVisualizer Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedSize = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Size"), 0.15f, 0.05f, 0.5f, 0.01f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedServerColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Server Color"), 255, 0, 0, 200);
        dev.dov.hack.echo.client.settings.ColorSetting preparedClientColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Client Color"), 0, 0, 255, 200);

        dev.dov.hack.echo.client.modules.impl.render.AimVisualizer.assignBootstrapSettings(Module, preparedSize, preparedServerColor, preparedClientColor);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.AimVisualizer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Ambience Module) {

        dev.dov.hack.echo.client.settings.ColorSetting preparedGradientColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Gradient color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapSecondaryColor(), false);
        dev.dov.hack.echo.client.settings.ColorSetting preparedFogColorColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Fog color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor(), false);
        dev.dov.hack.echo.client.settings.IntSetting preparedTime = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Time"), 18000, 0, 24000, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFogColorBoolean = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Fog color"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedGradient = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Gradient"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedWeather = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Weather"), CatalogText.literal("Clear"), new java.lang.CharSequence[] {CatalogText.literal("Clear"), CatalogText.literal("Rain"), CatalogText.literal("Thunder")});
        dev.dov.hack.echo.client.settings.IntSetting preparedFogDistance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Fog distance"), 16, 0, 48, CatalogText.literal(" chunks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCustomTime = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Custom time"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedModifyWeather = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Modify weather"), false);

        dev.dov.hack.echo.client.modules.impl.render.Ambience.assignBootstrapSettings(Module, preparedGradientColor, preparedFogColorColor, preparedTime, preparedFogColorBoolean, preparedGradient, preparedWeather, preparedFogDistance, preparedCustomTime, preparedModifyWeather);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Ambience.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.AntiTrap Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSigns = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Signs"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedHangingSigns = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Hanging Signs"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedGlowFrames = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Glow Frames"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPaintings = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Paintings"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedChestMinecarts = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Chest Minecarts"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedArmorStands = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Armor Stands"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoInteract = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Interact"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedItemFrames = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Item Frames"), true);

        dev.dov.hack.echo.client.modules.impl.utility.AntiTrap.assignBootstrapSettings(Module, preparedSigns, preparedHangingSigns, preparedGlowFrames, preparedPaintings, preparedChestMinecarts, preparedArmorStands, preparedNoInteract, preparedItemFrames);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.AntiTrap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.ArmorStatus Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.ArrayList Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedVisibilityFilter = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Visibility filter"), CatalogText.literal("Blacklist"), new java.lang.CharSequence[] {CatalogText.literal("Blacklist"), CatalogText.literal("Whitelist")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedLowercase = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Lowercase"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSuffixEnabled = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Suffix Enabled"), true);

        dev.dov.hack.echo.client.modules.impl.render.ArrayList.assignBootstrapSettings(Module, preparedVisibilityFilter, preparedLowercase, preparedSuffixEnabled);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.ArrayList.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.AttributeMine Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedMinDamage = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Min Damage"), 6.0f, 0.0f, 20.0f, 0.5f, CatalogText.literal(" dmg"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCanPlaceLegit = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Can Place Legit"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSafeBlockDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Safe block delay"), 0.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedOutlineColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        dev.dov.hack.echo.client.settings.IntSetting preparedSafetyTimeout = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Safety Timeout"), 200, 0, 1000, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapModeOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapModeOption2(), dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapModeOption()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedGlowstoneStart = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Glowstone Start"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedChargeDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Charge Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRenderPosition = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Render Position"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSafeAnchor = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Safe Anchor"), false);
        dev.dov.hack.echo.client.settings.IntSetting preparedExplodeSlot = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Explode Slot"), 1, 1, 9);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRepeat = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Repeat"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Style"), dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption(), dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.bootstrapStyleOption2()});
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCharges = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Charges"), 1.0f, 1.0f, 1.0f, 4.0f, 1.0f, CatalogText.literal(" charges"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedExplodeDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Explode Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotationSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Rotation Speed"), 6.0f, 1.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.ColorSetting preparedFillColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Fill Color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapForegroundColor().getRGB(), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), false);

        dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.assignBootstrapSettings(Module, preparedMinDamage, preparedCanPlaceLegit, preparedSafeBlockDelay, preparedOutlineColor, preparedSafetyTimeout, preparedMode, preparedGlowstoneStart, preparedChargeDelay, preparedRenderPosition, preparedSafeAnchor, preparedExplodeSlot, preparedRepeat, preparedStyle, preparedCharges, preparedExplodeDelay, preparedAutoAim, preparedRotationSpeed, preparedFillColor, preparedActivateKey, preparedSilentAim);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoAnchor.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoCart Module) {

        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapReturnToOption3(), dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapReturnToOption2(), dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapReturnToOption()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFreshArrowsOnly = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Fresh Arrows Only"), true);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedPlace = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapPlaceOption(), dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapPlaceOption2(), dev.dov.hack.echo.client.modules.impl.combat.AutoCart.bootstrapPlaceOption3()});

        dev.dov.hack.echo.client.modules.impl.combat.AutoCart.assignBootstrapSettings(Module, preparedReturnItems, preparedReturnTo, preparedSilentRotations, preparedFreshArrowsOnly, preparedReturnSlots, preparedPlace);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoCart.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoClicker Module) {

        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedBreakBlocks = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Break Blocks"), false);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRequireClick = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Require Click"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCps = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 9.0f, 13.0f, 1.0f, 20.0f, 0.5f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedRandomization = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Randomization"), dev.dov.hack.echo.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption2(), dev.dov.hack.echo.client.modules.impl.combat.AutoClicker.bootstrapRandomizationOption()});

        dev.dov.hack.echo.client.modules.impl.combat.AutoClicker.assignBootstrapSettings(Module, preparedHotbarWhitelist, preparedBreakBlocks, preparedItemWhitelist, preparedRequireClick, preparedCps, preparedRandomization);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoClicker.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedPlaceDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Place Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedPlaceObsidianDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Place Obsidian Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFakePunch = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Fake Punch"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedBreakDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Break Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwitchDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Switch Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPlaceObsidian = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Place Obsidian"), false);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.IntSetting preparedFailChance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Fail Chance"), 0, 0, 100, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption3(), dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption(), dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.bootstrapReturnToOption2()});
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.MOUSE_RIGHT);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStopOnKill = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stop on kill"), false);

        dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.assignBootstrapSettings(Module, preparedPlaceDelay, preparedPlaceObsidianDelay, preparedSwitchBack, preparedFakePunch, preparedBreakDelay, preparedSwitchDelay, preparedPlaceObsidian, preparedHotbarWhitelist, preparedItemWhitelist, preparedReturnSlots, preparedFailChance, preparedReturnTo, preparedActivateKey, preparedReturnItems, preparedStopOnKill);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoCrystal.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoMace Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCps = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 6.0f, 12.0f, 1.0f, 20.0f, 0.5f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedWorkWithMaceSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Work With Mace Swap"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedMultipoint = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPitchFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedTargetPriority = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedReEquipElytra = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Re-Equip Elytra"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedFallDistance = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Fall Distance"), 1.5f, 0.5f, 9.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutomaticallyUnequipElytra = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Automatically Unequip Elytra"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRandom = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedVerticalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedYawFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedUnequipTiming = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Unequip Timing"), CatalogText.literal("Late"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Late")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedHorizontalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedLateWindow = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Late Window"), 6.0f, 5.0f, 12.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimMath = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimVector = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});

        dev.dov.hack.echo.client.modules.impl.combat.AutoMace.assignBootstrapSettings(Module, preparedCps, preparedWorkWithMaceSwap, preparedMultipoint, preparedPitchFOV, preparedTargetPriority, preparedRange, preparedReEquipElytra, preparedSilentRotations, preparedFallDistance, preparedAutomaticallyUnequipElytra, preparedAutoAim, preparedRandom, preparedVerticalSpeed, preparedThroughWalls, preparedYawFOV, preparedUnequipTiming, preparedHorizontalSpeed, preparedLateWindow, preparedAimMath, preparedAimVector);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoMace.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoMaceSwap Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedFallDistance = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Fall Distance"), 1.5f, 0.5f, 9.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCps = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("CPS"), 6.0f, 12.0f, 1.0f, 20.0f, 0.5f);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStunSlam = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stun Slam"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAxeStun = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Axe Stun"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedStunReactionDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Stun Reaction Delay"), 0.0f, 35.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedStunSwitchDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Stun Switch Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSpearDTap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Spear D-tap"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedMinimumDamage = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Minimum Damage"), 10.0f, 0.0f, 35.0f, 1.0f, CatalogText.literal(" Dmg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimMath = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimVector = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedTargetPriority = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRandom = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedHorizontalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 5.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedVerticalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 4.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedYawFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPitchFOV = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Pitch FOV"), 180.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedMultipoint = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutomaticallyUnequipElytra = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Automatically Unequip Elytra"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedElytraRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Elytra Range"), 3.0f, 3.0f, 6.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedUnequipTiming = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Unequip Timing"), CatalogText.literal("Late"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Late")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedLateWindow = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Late Window"), 6.0f, 5.0f, 12.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedReEquipElytra = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Re-Equip Elytra"), false);

        dev.dov.hack.echo.client.modules.impl.combat.AutoMaceSwap.assignBootstrapSettings(Module, preparedFallDistance, preparedCps, preparedDelay, preparedItemWhitelist, preparedHotbarWhitelist, preparedStunSlam, preparedAutoAxeStun, preparedStunReactionDelay, preparedStunSwitchDelay, preparedSpearDTap, preparedMinimumDamage, preparedReturnTo, preparedReturnSlots, preparedReturnItems, preparedAutoAim, preparedAimMath, preparedAimVector, preparedTargetPriority, preparedRandom, preparedSilentRotations, preparedThroughWalls, preparedHorizontalSpeed, preparedVerticalSpeed, preparedRange, preparedYawFOV, preparedPitchFOV, preparedMultipoint, preparedAutomaticallyUnequipElytra, preparedElytraRange, preparedUnequipTiming, preparedLateWindow, preparedReEquipElytra);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoMaceSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.AutoTool Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedRequireClick = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Require click"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switch back"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRequireShift = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Require shift"), true);

        dev.dov.hack.echo.client.modules.impl.world.AutoTool.assignBootstrapSettings(Module, preparedRequireClick, preparedSwitchBack, preparedRequireShift);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.world.AutoTool.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.AutoTotem Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedHealthSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Health Swap"), false);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedRestoreItem = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Restore Item"), dev.dov.hack.echo.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption(), dev.dov.hack.echo.client.modules.impl.utility.AutoTotem.bootstrapRestoreItemOption2()});
        dev.dov.hack.echo.client.settings.IntSetting preparedSlotToRefillTotem = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Slot to Refill Totem"), 9, 1, 9);
        dev.dov.hack.echo.client.settings.IntSetting preparedRestoreAbove = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Restore Above"), 12, 1, 20, CatalogText.literal(" hp"));
        dev.dov.hack.echo.client.settings.IntSetting preparedEquipBelow = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Equip Below"), 8, 1, 19, CatalogText.literal(" hp"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRefillHotbar = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Refill Hotbar"), false);

        dev.dov.hack.echo.client.modules.impl.utility.AutoTotem.assignBootstrapSettings(Module, preparedHealthSwap, preparedRestoreItem, preparedSlotToRefillTotem, preparedRestoreAbove, preparedEquipBelow, preparedRefillHotbar);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.AutoTotem.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.AutoTrap Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        dev.dov.hack.echo.client.settings.IntSetting preparedPlaceDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Place Delay"), 1, 0, 4, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnlyStationary = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Only Stationary"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.utility.AutoTrap.bootstrapModeOption2(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.utility.AutoTrap.bootstrapModeOption2(), dev.dov.hack.echo.client.modules.impl.utility.AutoTrap.bootstrapModeOption()});

        dev.dov.hack.echo.client.modules.impl.utility.AutoTrap.assignBootstrapSettings(Module, preparedSilentRotations, preparedPlaceDelay, preparedOnlyStationary, preparedMode);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.AutoTrap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.AutoWalk Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.Backtrack Module) {

        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedOutlineColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        dev.dov.hack.echo.client.settings.IntSetting preparedMaximumHurtTime = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Maximum Hurt Time"), 10, 0, 10, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedHoldTime = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Hold Time"), 100.0f, 100.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedFillColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Fill Color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapForegroundColor().getRGB(), true);

        dev.dov.hack.echo.client.modules.impl.combat.Backtrack.assignBootstrapSettings(Module, preparedHotbarWhitelist, preparedOutlineColor, preparedMaximumHurtTime, preparedHoldTime, preparedItemWhitelist, preparedFillColor);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.Backtrack.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.BlockBruteforcer Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedRadius = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Radius"), 5, 1, 8, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedUp = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Up"), 3, 0, 64, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedDown = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Down"), 3, 0, 64, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedBlocksPerTick = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Blocks Per Tick"), 4, 1, 5, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Abort"), new java.lang.CharSequence[] {CatalogText.literal("Abort"), CatalogText.literal("Start")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFreezeMovement = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Freeze Movement"), true);

        dev.dov.hack.echo.client.modules.impl.world.BlockBruteforcer.assignBootstrapSettings(Module, preparedRadius, preparedUp, preparedDelay, preparedDown, preparedBlocksPerTick, preparedMode, preparedFreezeMovement);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.world.BlockBruteforcer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Chams Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 0.5f, 0.0f, 3.0f, 0.1f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor1 = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color 1"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor());
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRenderSelf = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Render Self"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedGradientScale = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Gradient Scale"), 1.0f, 0.25f, 4.0f, 0.25f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedGlowRadius = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Glow Radius"), 12.0f, 0.0f, 32.0f, 1.0f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedLineWidth = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Line Width"), 2.0f, 1.0f, 8.0f, 0.5f);
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor2 = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color 2"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapSecondaryColor());
        dev.dov.hack.echo.client.settings.FloatSetting preparedOutlineOpacity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Outline Opacity"), 1.0f, 0.0f, 1.0f, 0.05f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTeamColors = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Team Colors"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedFillOpacity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Fill Opacity"), 0.25f, 0.0f, 1.0f, 0.05f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedGlowIntensity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Glow Intensity"), 0.75f, 0.0f, 1.0f, 0.05f);

        dev.dov.hack.echo.client.modules.impl.render.Chams.assignBootstrapSettings(Module, preparedSpeed, preparedColor1, preparedRenderSelf, preparedGradientScale, preparedGlowRadius, preparedLineWidth, preparedColor2, preparedOutlineOpacity, preparedTeamColors, preparedFillOpacity, preparedGlowIntensity);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Chams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.ChestStealer Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedInitialDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Initial Delay"), 150, 1, 1000, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.IntSetting preparedTakeDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Take Delay"), 70, 1, 1000, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.IntSetting preparedCloseDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Close Delay"), 50, 1, 1000, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnlyImportant = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Only Important"), false);

        dev.dov.hack.echo.client.modules.impl.world.ChestStealer.initialDelay = preparedInitialDelay;
        dev.dov.hack.echo.client.modules.impl.world.ChestStealer.takeDelay = preparedTakeDelay;
        dev.dov.hack.echo.client.modules.impl.world.ChestStealer.closeDelay = preparedCloseDelay;
        dev.dov.hack.echo.client.modules.impl.world.ChestStealer.onlyImportant = preparedOnlyImportant;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.world.ChestStealer.initialDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.world.ChestStealer.takeDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.world.ChestStealer.closeDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.world.ChestStealer.onlyImportant);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.ChinaHat Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedHeight = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Height"), 0.31f, 0.1f, 0.8f, 0.01f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Rainbow"), new java.lang.CharSequence[] {CatalogText.literal("Static"), CatalogText.literal("Rainbow")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpinSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Spin Speed"), 0.25f, 0.0f, 2.0f, 0.05f);
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 185);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOutline = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Outline"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRadius = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Radius"), 0.55f, 0.3f, 1.2f, 0.05f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedOpacity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Opacity"), 0.9f, 0.0f, 1.0f, 0.05f);

        dev.dov.hack.echo.client.modules.impl.render.ChinaHat.assignBootstrapSettings(Module, preparedHeight, preparedMode, preparedSpinSpeed, preparedColor, preparedOutline, preparedRadius, preparedOpacity);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.ChinaHat.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.internals.ClickGui Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedScrollSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Scroll Speed"), 50.0f, 1.0f, 50.0f, 1.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStreamProof = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stream proof"), false);

        dev.dov.hack.echo.client.modules.internals.ClickGui.scrollSpeed = preparedScrollSpeed;
        dev.dov.hack.echo.client.modules.internals.ClickGui.streamProof = preparedStreamProof;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.internals.ClickGui.scrollSpeed);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.internals.ClickGui.streamProof);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Coordinates Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.Criticals Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Unsprint"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Unsprint"), CatalogText.literal("Release")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedResume = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Resume"), CatalogText.literal("Instant"), new java.lang.CharSequence[] {CatalogText.literal("Instant"), CatalogText.literal("Delay"), CatalogText.literal("Ground")});
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedResumeDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Resume Delay"), 1.0f, 3.0f, 1.0f, 10.0f, 1.0f, CatalogText.literal("t"));

        dev.dov.hack.echo.client.modules.impl.combat.Criticals.assignBootstrapSettings(Module, preparedMode, preparedResume, preparedResumeDelay);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.Criticals.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.CrystalOptimizer Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.Displace Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedYawOffset = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Yaw Offset"), 90.0f, 0.0f, 180.0f, 5.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedDirection = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Direction"), dev.dov.hack.echo.client.modules.impl.combat.Displace.bootstrapDirectionOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.Displace.bootstrapDirectionOption3(), dev.dov.hack.echo.client.modules.impl.combat.Displace.bootstrapDirectionOption2(), dev.dov.hack.echo.client.modules.impl.combat.Displace.bootstrapDirectionOption()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedDynamic = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Dynamic"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedKnockbackEnchant = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Knockback Enchant"), true);

        dev.dov.hack.echo.client.modules.impl.combat.Displace.assignBootstrapSettings(Module, preparedYawOffset, preparedDirection, preparedDynamic, preparedKnockbackEnchant);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.Displace.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.DiveBomb Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedTickDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Tick Delay"), 2, 0, 6, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoJump = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedInstantDiveBomb = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Instant Dive Bomb"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedDiveBomb = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Dive Bomb"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchback = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switchback"), true);

        dev.dov.hack.echo.client.modules.impl.movement.DiveBomb.assignBootstrapSettings(Module, preparedTickDelay, preparedAutoJump, preparedInstantDiveBomb, preparedSilentAim, preparedDiveBomb, preparedSwitchback);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.DiveBomb.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.ElytraSwap Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);

        dev.dov.hack.echo.client.modules.impl.utility.ElytraSwap.assignBootstrapSettings(Module, preparedSwitchBack, preparedActivateKey);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.ElytraSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.FastStairs Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.FastUse Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 0.0f, 0.0f, 0.0f, 4.0f, 1.0f, CatalogText.literal(" ticks"));

        dev.dov.hack.echo.client.modules.impl.utility.FastUse.delay = preparedDelay;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.utility.FastUse.delay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.utility.FastUse.items);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Freecam Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 1.0f, 5.0f, 0.1f);

        dev.dov.hack.echo.client.modules.impl.render.Freecam.assignBootstrapSettings(Module, preparedSpeed);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Freecam.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.FullBright Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.GUIWalk Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Grim")});

        dev.dov.hack.echo.client.modules.impl.movement.GUIWalk.assignBootstrapSettings(Module, preparedMode);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.GUIWalk.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.HandChams Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedLineWidth = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Line Width"), 2.0f, 1.0f, 8.0f, 0.5f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedGlowRadius = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Glow Radius"), 12.0f, 0.0f, 32.0f, 1.0f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedOutlineOpacity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Outline Opacity"), 1.0f, 0.0f, 1.0f, 0.05f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedFillOpacity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Fill Opacity"), 0.25f, 0.0f, 1.0f, 0.05f);
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor2 = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color 2"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapSecondaryColor());
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor1 = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color 1"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor());
        dev.dov.hack.echo.client.settings.FloatSetting preparedGradientScale = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Gradient Scale"), 1.0f, 0.25f, 4.0f, 0.25f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedGlowIntensity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Glow Intensity"), 0.75f, 0.0f, 1.0f, 0.05f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 0.5f, 0.0f, 3.0f, 0.1f, CatalogText.literal("x"));

        dev.dov.hack.echo.client.modules.impl.render.HandChams.assignBootstrapSettings(Module, preparedLineWidth, preparedGlowRadius, preparedOutlineOpacity, preparedFillOpacity, preparedColor2, preparedColor1, preparedGradientScale, preparedGlowIntensity, preparedSpeed);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.HandChams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.HighJump Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Normal"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Shulker")});
        dev.dov.hack.echo.client.settings.IntSetting preparedTicks = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Ticks"), 6, 1, 20, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedLift = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Lift"), 0.51f, 0.1f, 1.85f, 0.01f);

        dev.dov.hack.echo.client.modules.impl.movement.HighJump.assignBootstrapSettings(Module, preparedMode, preparedTicks, preparedLift);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.HighJump.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.Hitboxes Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedStaticHitbox = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Static Hitbox"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedExpandX = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Expand X"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedUniform = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Uniform"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedExpandYUp = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Expand Y Up"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedExpandZ = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Expand Z"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedExpandSize = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Expand Size"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedExpandYDown = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Expand Y Down"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));

        dev.dov.hack.echo.client.modules.impl.combat.Hitboxes.assignBootstrapSettings(Module, preparedStaticHitbox, preparedExpandX, preparedUniform, preparedExpandYUp, preparedExpandZ, preparedExpandSize, preparedExpandYDown);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.Hitboxes.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Hotbar Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.InfoBar Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowFrameTime = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Frame Time"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowFPS = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show FPS"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowPing = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Ping"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowUsername = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Username"), true);

        dev.dov.hack.echo.client.modules.impl.render.InfoBar.assignBootstrapSettings(Module, preparedShowFrameTime, preparedShowFPS, preparedShowPing, preparedShowUsername);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.InfoBar.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.ItemESP Module) {

        ItemListSetting preparedWhitelist = new ItemListSetting(CatalogText.literal("Whitelist"));
        BooleanSetting preparedFill = new BooleanSetting(CatalogText.literal("Fill"), false);
        MultiChoiceSetting preparedModes = new MultiChoiceSetting(CatalogText.literal("Modes"), new CharSequence[]{CatalogText.literal("Box"), CatalogText.literal("2D")});
        BooleanSetting preparedShowName = new BooleanSetting(CatalogText.literal("Show Name"), true);
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 2145955883);
        dev.dov.hack.echo.client.modules.impl.render.ItemESP.assignBootstrapSettings(Module, preparedWhitelist, preparedFill, preparedModes, preparedShowName, preparedTracers, preparedColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.ItemESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.JumpReset Module) {

        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedDamageSources = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Damage Sources"), new java.lang.CharSequence[] {CatalogText.literal("Player"), CatalogText.literal("Mob"), CatalogText.literal("Projectile"), CatalogText.literal("Explosion"), CatalogText.literal("Fall"), CatalogText.literal("Fire"), CatalogText.literal("Magic"), CatalogText.literal("Other")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedChance = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Chance"), 100.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedWebCheck = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Web Check"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedLiquidCheck = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Liquid Check"), true);

        dev.dov.hack.echo.client.modules.impl.movement.JumpReset.assignBootstrapSettings(Module, preparedDamageSources, preparedChance, preparedWebCheck, preparedLiquidCheck);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.JumpReset.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.KeepSprint Module) {

        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedSprintCondition = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Sprint Condition"), new java.lang.CharSequence[] {CatalogText.literal("Air"), CatalogText.literal("Ground")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeedKeptVelocity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed Kept Velocity"), 0.6f, 0.6f, 1.0f, 0.1f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShouldSprint = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Should Sprint"), true);

        dev.dov.hack.echo.client.modules.impl.movement.KeepSprint.assignBootstrapSettings(Module, preparedSprintCondition, preparedSpeedKeptVelocity, preparedShouldSprint);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.KeepSprint.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Keybinds Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.KeyCharge Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchback = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switchback"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Swap"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoJump = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);

        dev.dov.hack.echo.client.modules.impl.movement.KeyCharge.assignBootstrapSettings(Module, preparedSwitchback, preparedActivateKey, preparedSilentSwap, preparedAutoJump, preparedSilentAim, preparedAutoAim);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.KeyCharge.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.KeyPearl Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSyncToClient = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Sync to Client"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedThrowDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Throw Delay"), 0.0f, 0.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwapDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Swap Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));

        dev.dov.hack.echo.client.modules.impl.utility.KeyPearl.assignBootstrapSettings(Module, preparedSyncToClient, preparedThrowDelay, preparedActivateKey, preparedSwapBack, preparedSwapDelay);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.KeyPearl.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedGroundDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Ground Delay"), 50.0f, 150.0f, 0.0f, 5000.0f, 1.0f, dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay.bootstrapGroundDelayOption());
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedAirDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Air Delay"), 100.0f, 250.0f, 0.0f, 5000.0f, 1.0f, dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay.bootstrapGroundDelayOption());
        dev.dov.hack.echo.client.settings.IntSetting preparedChance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Chance"), 100, 0, 100, dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay.bootstrapChanceOption());

        dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay.assignBootstrapSettings(Module, preparedGroundDelay, preparedAirDelay, preparedChance);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.KnockbackDelay.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.LightESP Module) {

        IntSetting preparedMaxY = new IntSetting(CatalogText.literal("Max Y"), 0, -64, 319, CatalogText.literal(""));
        IntSetting preparedMinLight = new IntSetting(CatalogText.literal("Min Light"), 5, 0, 15, CatalogText.literal(""));
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        BooleanSetting preparedOutline = new BooleanSetting(CatalogText.literal("Outline"), true);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 255, 128, 0, 200, false);
        dev.dov.hack.echo.client.modules.impl.render.LightESP.assignBootstrapSettings(Module, preparedMaxY, preparedMinLight, preparedTracers, preparedOutline, preparedColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.LightESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.MaceSwap Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSpearDTap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Spear D-tap"), true);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedStunSwitchDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Stun Switch Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedMinimumDamage = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Minimum Damage"), 10.0f, 0.0f, 35.0f, 1.0f, CatalogText.literal(" Dmg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStunSlam = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stun Slam"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));

        dev.dov.hack.echo.client.modules.impl.combat.MaceSwap.assignBootstrapSettings(Module, preparedSpearDTap, preparedHotbarWhitelist, preparedStunSwitchDelay, preparedReturnItems, preparedMinimumDamage, preparedReturnTo, preparedReturnSlots, preparedItemWhitelist, preparedStunSlam, preparedDelay);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.MaceSwap.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.MLG Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedDistanceToGround = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Distance To Ground"), 24, 1, 24, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedItems = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Items"), dev.dov.hack.echo.client.modules.impl.utility.MLG.bootstrapItemsOptionSet());
        dev.dov.hack.echo.client.settings.IntSetting preparedFallDistanceThreshold = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Fall Distance Threshold"), 5, 0, 20, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilent = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent"), true);

        dev.dov.hack.echo.client.modules.impl.utility.MLG.assignBootstrapSettings(Module, preparedDistanceToGround, preparedItems, preparedFallDistanceThreshold, preparedAutoAim, preparedSilent);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.MLG.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.MultiUse Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.NoBreakDelay Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.NoJumpDelay Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.NoRender Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoSpyglassOverlay = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Spyglass Overlay"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoNausea = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Nausea"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoPortalOverlay = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Portal Overlay"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoFireOverlay = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Fire Overlay"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoFloatingItem = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Floating Item"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoVignette = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Vignette"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoCrystalBounce = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Crystal Bounce"), false);

        dev.dov.hack.echo.client.modules.impl.render.NoRender.noSpyglassOverlay = preparedNoSpyglassOverlay;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noNausea = preparedNoNausea;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noPortalOverlay = preparedNoPortalOverlay;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noFireOverlay = preparedNoFireOverlay;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noFloatingItem = preparedNoFloatingItem;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noVignette = preparedNoVignette;
        dev.dov.hack.echo.client.modules.impl.render.NoRender.noCrystalBounce = preparedNoCrystalBounce;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noFireOverlay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noNausea);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noVignette);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noFloatingItem);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noSpyglassOverlay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noPortalOverlay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.NoRender.noCrystalBounce);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.NoSlow Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedDelayTicks = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Delay Ticks"), 2, 0, 10);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("vanilla"), new java.lang.CharSequence[] {CatalogText.literal("vanilla"), CatalogText.literal("Grim")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedEnableSneak = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Enable Sneak"), false);

        dev.dov.hack.echo.client.modules.impl.movement.NoSlow.assignBootstrapSettings(Module, preparedDelayTicks, preparedMode, preparedEnableSneak);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.NoSlow.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Notifications Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedDuration = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Duration"), 1.5f, 0.5f, 8.0f, 0.1f, "s");

        dev.dov.hack.echo.client.modules.impl.render.Notifications.assignBootstrapSettings(Module, preparedDuration);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Notifications.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.NullMove Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Offhand Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Particles Module) {

        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), 255, 92, 20, 220);
        dev.dov.hack.echo.client.settings.IntSetting preparedRadius = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Radius"), 24, 8, 64, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.1f, 3.0f, 0.1f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.IntSetting preparedDensity = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Density"), 120, 10, 400, CatalogText.literal(" particles"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTwinkle = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Twinkle"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedSize = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Size"), 1.0f, 0.2f, 3.0f, 0.1f, CatalogText.literal("x"));

        dev.dov.hack.echo.client.modules.impl.render.Particles.assignBootstrapSettings(Module, preparedColor, preparedRadius, preparedSpeed, preparedDensity, preparedTwinkle, preparedSize);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Particles.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.PearlCatch Module) {

        dev.dov.hack.echo.client.settings.KeybindSetting preparedLowPearlCatch = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Low Pearl Catch"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedInstantPearlCatch = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Instant Pearl Catch"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedHighDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("High Delay"), 2.0f, 2.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRetry = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Retry"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedHighPearlCatch = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("High Pearl Catch"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);

        dev.dov.hack.echo.client.modules.impl.utility.PearlCatch.assignBootstrapSettings(Module, preparedLowPearlCatch, preparedAutoAim, preparedInstantPearlCatch, preparedHighDelay, preparedSwitchBack, preparedRetry, preparedHighPearlCatch, preparedSilentAim);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.PearlCatch.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.PearlGrapple Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedMaxPredictTicks = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Max Predict Ticks"), 60, 5, 80, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedDistance = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Distance"), 48.0f, 3.0f, 48.0f, 1.0f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        dev.dov.hack.echo.client.modules.impl.utility.PearlGrapple.assignBootstrapSettings(Module, preparedMaxPredictTicks, preparedDistance, preparedSilentRotations);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.PearlGrapple.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.Piercing Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedSortTargetsBy = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Sort Targets By"), CatalogText.literal("Hurt Time"), new java.lang.CharSequence[] {CatalogText.literal("Hurt Time"), CatalogText.literal("Health")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedBlocks = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Blocks"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedInteract = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Interact"), false);

        dev.dov.hack.echo.client.modules.impl.combat.Piercing.sortTargetsBy = preparedSortTargetsBy;
        dev.dov.hack.echo.client.modules.impl.combat.Piercing.blocks = preparedBlocks;
        dev.dov.hack.echo.client.modules.impl.combat.Piercing.interact = preparedInteract;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.combat.Piercing.blocks);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.combat.Piercing.interact);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.combat.Piercing.sortTargetsBy);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.combat.Piercing.holdingBlock);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.PlayerESP Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedHealthColor = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Health Color"), CatalogText.literal("Dynamic"), new java.lang.CharSequence[] {CatalogText.literal("Dynamic"), CatalogText.literal("Static")});
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), 2139881472);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTeamColor = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Team Color"), false);
        dev.dov.hack.echo.client.settings.ColorSetting preparedFullHealthColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Full Health Color"), -13899678);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRenderSelf = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Render Self"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowName = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Name"), false);
        dev.dov.hack.echo.client.settings.ColorSetting preparedHealthBarColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Health Bar Color"), -13899678);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowHeldItem = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Held Item"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFill = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Fill"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTracers = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Tracers"), true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedLowHealthColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Low Health Color"), -1692114);
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedModes = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Modes"), new java.lang.CharSequence[] {CatalogText.literal("Box"), CatalogText.literal("2D")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedHealthBar = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Health Bar"), CatalogText.literal("Left"), new java.lang.CharSequence[] {CatalogText.literal("None"), CatalogText.literal("Left"), CatalogText.literal("Right"), CatalogText.literal("Bottom")});

        dev.dov.hack.echo.client.modules.impl.render.PlayerESP.assignBootstrapSettings(Module, preparedHealthColor, preparedColor, preparedTeamColor, preparedFullHealthColor, preparedRenderSelf, preparedShowName, preparedHealthBarColor, preparedShowHeldItem, preparedFill, preparedTracers, preparedLowHealthColor, preparedModes, preparedHealthBar);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.PlayerESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.PotionEffects Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.PotRefill Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.utility.PotRefill.bootstrapModeOption3(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.utility.PotRefill.bootstrapModeOption3(), dev.dov.hack.echo.client.modules.impl.utility.PotRefill.bootstrapModeOption(), dev.dov.hack.echo.client.modules.impl.utility.PotRefill.bootstrapModeOption2()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRefillBuffPotions = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Refill Buff Potions"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 10.0f, 50.0f, 0.0f, 400.0f, 5.0f, CatalogText.literal("ms"));

        dev.dov.hack.echo.client.modules.impl.utility.PotRefill.assignBootstrapSettings(Module, preparedMode, preparedRefillBuffPotions, preparedDelay);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.PotRefill.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.internals.Prevent Module) {

        dev.dov.hack.echo.client.settings.ItemListSetting preparedPreventWithItemList = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Prevent With"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoAirHits = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Air Hits"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoShieldWithBlocks = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Shield With Blocks"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedInHotbar = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("In Hotbar"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedUntotem = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Untotem"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnObsidian = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Obsidian"), true);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedPreventWithItemList2 = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Prevent With"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoMining = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Mining"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedEChestOpen = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("EChest Open"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedInInventory = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("In Inventory"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAnchorCharge = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Anchor Charge"), true);

        dev.dov.hack.echo.client.modules.impl.internals.Prevent.assignBootstrapSettings(Module, preparedPreventWithItemList, preparedNoAirHits, preparedNoShieldWithBlocks, preparedInHotbar, preparedUntotem, preparedOnObsidian, preparedPreventWithItemList2, preparedNoMining, preparedEChestOpen, preparedInInventory, preparedAnchorCharge);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.internals.Prevent.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.internals.Protect Module) {

        dev.dov.hack.echo.client.settings.TextSetting preparedSkin = new dev.dov.hack.echo.client.settings.TextSetting(CatalogText.literal("Skin"), "Notch");
        dev.dov.hack.echo.client.settings.TextSetting preparedNickname = new dev.dov.hack.echo.client.settings.TextSetting(CatalogText.literal("Nickname"), "Player");

        dev.dov.hack.echo.client.modules.impl.internals.Protect.skin = preparedSkin;
        dev.dov.hack.echo.client.modules.impl.internals.Protect.nickname = preparedNickname;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.internals.Protect.nickname);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.internals.Protect.skin);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.Replenish Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedMinPercent = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Min %"), 25.0f, 0.0f, 100.0f, 1.0f);
        dev.dov.hack.echo.client.settings.IntSetting preparedCursorSpeed = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Cursor Speed"), 20, 1, 100);

        dev.dov.hack.echo.client.modules.impl.utility.Replenish.assignBootstrapSettings(Module, preparedMinPercent, preparedCursorSpeed);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.Replenish.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.Scaffold Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedPitchCheck = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Pitch Check"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.world.Scaffold.bootstrapModeOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.world.Scaffold.bootstrapModeOption()});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedListType = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("List Type"), dev.dov.hack.echo.client.modules.impl.world.Scaffold.bootstrapListTypeOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.world.Scaffold.bootstrapListTypeOption(), dev.dov.hack.echo.client.modules.impl.world.Scaffold.bootstrapListTypeOption2()});

        dev.dov.hack.echo.client.modules.impl.world.Scaffold.assignBootstrapSettings(Module, preparedPitchCheck, preparedMode, preparedListType);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.world.Scaffold.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.internals.SelfDestruct Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedReplaceMods = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Replace Mods"), false);
        dev.dov.hack.echo.client.settings.TextSetting preparedUrl = new dev.dov.hack.echo.client.settings.TextSetting(CatalogText.literal("URL"), "");

        dev.dov.hack.echo.client.modules.impl.internals.SelfDestruct.assignBootstrapSettings(Module, preparedReplaceMods, preparedUrl);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.internals.SelfDestruct.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedUnblockOwnShield = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Unblock Own Shield"), true);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), false);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption3(), dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption2(), dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapReturnToOption()});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption2(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption2(), dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.bootstrapModeOption()});
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedAttackDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Swap"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedReactionDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStun = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stun"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwapBackDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.assignBootstrapSettings(Module, preparedUnblockOwnShield, preparedHotbarWhitelist, preparedReturnSlots, preparedSwapBack, preparedReturnTo, preparedMode, preparedAttackDelay, preparedAutoSwap, preparedReactionDelay, preparedReturnItems, preparedStun, preparedSwapBackDelay, preparedItemWhitelist);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.ShieldBreaker.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.internals.SilentAim Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedRenderRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Render Rotations"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.internals.SilentAim.bootstrapSilentMode(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.internals.SilentAim.bootstrapNoneMode(), dev.dov.hack.echo.client.modules.internals.SilentAim.bootstrapSilentMode(), dev.dov.hack.echo.client.modules.internals.SilentAim.bootstrapStrictMode()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedServerSync = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Server Sync"), true);

        dev.dov.hack.echo.client.modules.internals.SilentAim.assignBootstrapSettings(Module, preparedRenderRotations, preparedMode, preparedServerSync);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.internals.SilentAim.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.SkyShaders Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedStars = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Stars"), 1.0f, 0.0f, 2.0f, 0.05f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Nebula"), new java.lang.CharSequence[] {CatalogText.literal("Nebula"), CatalogText.literal("Aurora"), CatalogText.literal("Twilight")});
        dev.dov.hack.echo.client.settings.ColorSetting preparedSecondaryColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Secondary color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapSecondaryColor(), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedEndDimension = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("End dimension"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedBrightness = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Brightness"), 1.0f, 0.1f, 2.0f, 0.05f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCustomColors = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Custom colors"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSunAndMoon = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Sun & moon"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.0f, 3.0f, 0.05f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedPrimaryColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Primary color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor(), false);

        dev.dov.hack.echo.client.modules.impl.render.SkyShaders.assignBootstrapSettings(Module, preparedStars, preparedMode, preparedSecondaryColor, preparedEndDimension, preparedBrightness, preparedCustomColors, preparedSunAndMoon, preparedSpeed, preparedPrimaryColor);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.SkyShaders.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.SmartPot Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoBuff = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Buff"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedThrowDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Throw Delay"), 150.0f, 250.0f, 0.0f, 450.0f, 5.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThrowPotion = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Throw Potion"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwapBackDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 50.0f, 100.0f, 0.0f, 500.0f, 5.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAngleCheck = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Angle Check"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedFov = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("FOV"), 0.0f, 20.0f, 0.0f, 90.0f, 1.0f);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedMinHealth = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Min Health"), 16.0f, 16.0f, 0.0f, 20.0f, 1.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), false);

        dev.dov.hack.echo.client.modules.impl.utility.SmartPot.assignBootstrapSettings(Module, preparedAutoBuff, preparedThrowDelay, preparedThrowPotion, preparedSwapBackDelay, preparedSwapBack, preparedSilentAim, preparedAngleCheck, preparedFov, preparedMinHealth, preparedAutoAim);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.SmartPot.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.SpearAssist Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimMath = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAimVector = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedMultipoint = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedVerticalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedTargetPriority = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 28.0f, 1.0f, 128.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedHorizontalSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedFov = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedRandom = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Random"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), false);

        dev.dov.hack.echo.client.modules.impl.combat.SpearAssist.assignBootstrapSettings(Module, preparedAimMath, preparedThroughWalls, preparedAimVector, preparedMultipoint, preparedVerticalSpeed, preparedTargetPriority, preparedRange, preparedHorizontalSpeed, preparedFov, preparedRandom, preparedSilentRotations);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.SpearAssist.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.SpearLunge Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedCooldown = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Cooldown"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Paper")});
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Attack"), new java.lang.CharSequence[] {CatalogText.literal("Attack"), CatalogText.literal("Keypress")});
        dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Return Items"));
        dev.dov.hack.echo.client.settings.IntSetting preparedCooldownPercent = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Cooldown %"), 85, 0, 100);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilent = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent"), false);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});

        dev.dov.hack.echo.client.modules.impl.combat.SpearLunge.assignBootstrapSettings(Module, preparedCooldown, preparedActivateKey, preparedMode, preparedReturnItems, preparedCooldownPercent, preparedHotbarWhitelist, preparedItemWhitelist, preparedSilent, preparedReturnSlots, preparedReturnTo);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.SpearLunge.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.SpearReach Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedPaperMCSupport = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("PaperMC support"), false);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        dev.dov.hack.echo.client.modules.impl.combat.SpearReach.assignBootstrapSettings(Module, preparedPaperMCSupport, preparedHotbarWhitelist, preparedItemWhitelist);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.SpearReach.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.Speed Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedStrafe = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Strafe"), 1.0f, 0.01f, 10.0f, 0.01f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedBoatBoost = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Boat Boost"), 0.5f, 0.01f, 0.75f, 0.01f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedMotion = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Motion"), 1.0f, 0.01f, 10.0f, 0.01f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedBoost = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Boost"), 0.1f, 0.01f, 0.5f, 0.01f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("MinemenClub"), new java.lang.CharSequence[] {CatalogText.literal("MinemenClub"), CatalogText.literal("Legit"), CatalogText.literal("Grim Entity"), CatalogText.literal("Motion"), CatalogText.literal("Strafe")});

        dev.dov.hack.echo.client.modules.impl.movement.Speed.assignBootstrapSettings(Module, preparedStrafe, preparedBoatBoost, preparedMotion, preparedBoost, preparedMode);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.Speed.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.Sprint Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedOmni = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Omni"), false);

        dev.dov.hack.echo.client.modules.impl.movement.Sprint.assignBootstrapSettings(Module, preparedOmni);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.Sprint.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.SprintReset Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedDurationBetweenReset = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Duration Between Reset"), 0, 0, 20, CatalogText.literal(" Ticks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedDuration = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Duration"), 3, 1, 20, CatalogText.literal(" Ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedGroundOnly = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Ground Only"), false);
        dev.dov.hack.echo.client.settings.IntSetting preparedReactionTime = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Reaction Time"), 0, 0, 20, CatalogText.literal(" Ticks"));

        dev.dov.hack.echo.client.modules.impl.movement.SprintReset.assignBootstrapSettings(Module, preparedDurationBetweenReset, preparedDuration, preparedGroundOnly, preparedReactionTime);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.SprintReset.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.Step Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedDisableOnSneak = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Disable on sneak"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedHeight = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Height"), 1.0f, 1.0f, 3.0f, 0.1f, CatalogText.literal(" blocks"));

        dev.dov.hack.echo.client.modules.impl.movement.Step.assignBootstrapSettings(Module, preparedDisableOnSneak, preparedHeight);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.Step.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.SwingSpeed Module) {

        dev.dov.hack.echo.client.settings.IntSetting preparedSwingSpeed = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Swing Speed"), 7, 1, 20, CatalogText.literal(" ticks"));

        dev.dov.hack.echo.client.modules.impl.render.SwingSpeed.swingSpeed = preparedSwingSpeed;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.SwingSpeed.swingSpeed);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Target Module) {

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.hud.HudElement.appendBootstrapSettings(Module);
        dev.dov.hack.echo.client.modules.HudModule.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.TargetESP Module) {

        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor().getRed(), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor().getGreen(), dev.dov.hack.echo.client.ui.ColorTheme.bootstrapPrimaryColor().getBlue(), 200, true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), dev.dov.hack.echo.client.modules.impl.render.TargetESP.bootstrapModeOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.render.TargetESP.bootstrapModeOption()});

        dev.dov.hack.echo.client.modules.impl.render.TargetESP.assignBootstrapSettings(Module, preparedColor, preparedMode);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.TargetESP.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.internals.Targets Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedAntiBot = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("AntiBot"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedKeepTargetRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Keep Target Range"), 16.0f, 1.0f, 50.0f, 0.5f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedIgnoreFriends = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Ignore Friends"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedLastAttacked = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Last Attacked"), false);
        dev.dov.hack.echo.client.settings.IntSetting preparedShieldTicks = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Shield Ticks"), 5, 1, 10, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedAntiBotChecks = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("AntiBot Checks"), new java.lang.CharSequence[] {CatalogText.literal("Name"), CatalogText.literal("Movement"), CatalogText.literal("Tab List"), CatalogText.literal("Scale"), CatalogText.literal("Bliss"), CatalogText.literal("Zeta / MMC")});
        dev.dov.hack.echo.client.settings.KeybindSetting preparedManualTargetSelect = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Manual Target Select"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedShieldDetection = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Shield Detection"), dev.dov.hack.echo.client.modules.internals.Targets.bootstrapShieldDetectionOption(), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.internals.Targets.bootstrapShieldDetectionOption(), dev.dov.hack.echo.client.modules.internals.Targets.bootstrapShieldDetectionOption2()});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedIgnoreTeams = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Ignore Teams"), false);
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedEntityTypes = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Entity Types"), new java.lang.CharSequence[] {CatalogText.literal("Players"), CatalogText.literal("Hostile"), CatalogText.literal("Passive"), CatalogText.literal("Neutral"), CatalogText.literal("Armor Stands"), CatalogText.literal("Invisible"), CatalogText.literal("Naked")});
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedTeamChecks = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Team Checks"), new java.lang.CharSequence[] {CatalogText.literal("Color"), CatalogText.literal("Name"), CatalogText.literal("Scoreboard")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedKeepTarget = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Keep Target"), true);
        dev.dov.hack.echo.client.settings.KeybindSetting preparedFriendBind = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Friend Bind"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPingCompensation = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Ping Compensation"), false);

        dev.dov.hack.echo.client.modules.internals.Targets.assignBootstrapSettings(Module, preparedAntiBot, preparedKeepTargetRange, preparedIgnoreFriends, preparedLastAttacked, preparedShieldTicks, preparedAntiBotChecks, preparedManualTargetSelect, preparedShieldDetection, preparedIgnoreTeams, preparedEntityTypes, preparedTeamChecks, preparedKeepTarget, preparedFriendBind, preparedPingCompensation);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.internals.Targets.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.Timer Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedTimer = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Timer"), 1.0f, 0.5f, 10.0f, 0.05f, CatalogText.literal("x"));

        dev.dov.hack.echo.client.modules.impl.movement.Timer.assignBootstrapSettings(Module, preparedTimer);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.Timer.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.utility.TotemHand Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBackOnClose = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap back on close"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAnchors = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Anchors"), true);
        dev.dov.hack.echo.client.settings.IntSetting preparedInventoryChance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Inventory chance"), 100, 0, 100, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPredictive = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Predictive"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedKillable = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Killable"), false);
        dev.dov.hack.echo.client.settings.IntSetting preparedMinDamage = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Min damage"), 10, 1, 30, CatalogText.literal(" dmg"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCrystals = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Crystals"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnPop = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On pop"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBackOnSafe = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap back on safe"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnInventoryOpen = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On inventory open"), false);

        dev.dov.hack.echo.client.modules.impl.utility.TotemHand.assignBootstrapSettings(Module, preparedSwapBackOnClose, preparedAnchors, preparedInventoryChance, preparedPredictive, preparedKillable, preparedMinDamage, preparedCrystals, preparedOnPop, preparedSwapBackOnSafe, preparedOnInventoryOpen);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.utility.TotemHand.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.TotemHit Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Swap"), false);

        dev.dov.hack.echo.client.modules.impl.combat.TotemHit.assignBootstrapSettings(Module, preparedDelay, preparedSilentSwap);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.TotemHit.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Trails Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedGradient = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Gradient"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedHeight = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Height"), 1.0f, 0.2f, 2.5f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedLifetime = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Lifetime"), 3.0f, 0.5f, 10.0f, 0.5f, CatalogText.literal("s"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 160);

        dev.dov.hack.echo.client.modules.impl.render.Trails.assignBootstrapSettings(Module, preparedGradient, preparedHeight, preparedLifetime, preparedColor);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Trails.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Trajectories Module) {

        dev.dov.hack.echo.client.settings.ColorSetting preparedArrowColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Arrow Color"), 255, 128, 128, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedPearlColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Pearl Color"), 102, 179, 255, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedWindChargeColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Wind Charge Color"), 255, 209, 102, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedPotionColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Potion Color"), 124, 192, 255, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedFireworkColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Firework Color"), 255, 196, 130, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedBowColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Bow Color"), 148, 114, 255, 200, true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedCrossbowColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Crossbow Color"), 114, 216, 180, 200, true);
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedTrajectories = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Trajectories"), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption2(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption5(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption3(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption4(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption7(), dev.dov.hack.echo.client.modules.impl.render.Trajectories.bootstrapTrajectoriesOption6()});

        dev.dov.hack.echo.client.modules.impl.render.Trajectories.assignBootstrapSettings(Module, preparedArrowColor, preparedPearlColor, preparedWindChargeColor, preparedPotionColor, preparedFireworkColor, preparedBowColor, preparedCrossbowColor, preparedTrajectories);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Trajectories.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.internals.Transactions Module) {

        Module.settings.clear();
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.TriggerBot Module) {

        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCooldown = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Cooldown"), 90.0f, 95.0f, 0.0f, 100.0f, 1.0f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedCritMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Crit Mode"), CatalogText.literal("pCrit"), new java.lang.CharSequence[] {CatalogText.literal("pCrit"), CatalogText.literal("Normal"), CatalogText.literal("None")});
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedReactionDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCritOnlyOnSpace = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Crit only on space"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedAttackDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 800.0f, 1.0f);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedAttackDistance = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Attack Distance"), 0.0f, 0.0f, 6.0f, 0.01f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedNoSweepOrPickHits = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("No Sweep or Pick Hits"), false);
        dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist = new dev.dov.hack.echo.client.settings.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedHurtTimeCheck = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("HurtTime Check"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAttackShields = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Attack Shields"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCritRange = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Crit Range"), 0.0f, 6.0f, 0.0f, 6.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCritCooldown = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Crit Cooldown"), 90.0f, 100.0f, 0.0f, 100.0f, 1.0f);

        dev.dov.hack.echo.client.modules.impl.combat.TriggerBot.assignBootstrapSettings(Module, preparedCooldown, preparedCritMode, preparedReactionDelay, preparedCritOnlyOnSpace, preparedAttackDelay, preparedItemWhitelist, preparedAttackDistance, preparedNoSweepOrPickHits, preparedHotbarWhitelist, preparedHurtTimeCheck, preparedAttackShields, preparedCritRange, preparedCritCooldown);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.TriggerBot.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.world.Tunneler Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilent = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("2x1"), new java.lang.CharSequence[] {CatalogText.literal("2x1"), CatalogText.literal("1x1")});

        dev.dov.hack.echo.client.modules.impl.world.Tunneler.assignBootstrapSettings(Module, preparedSilent, preparedMode);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.world.Tunneler.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.movement.Velocity Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedAmount = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Amount"), 0.0f, 0.0f, 1.0f, 0.01f);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Cancel"), new java.lang.CharSequence[] {CatalogText.literal("Cancel"), CatalogText.literal("MinemenClub")});
        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedSources = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Sources"), new java.lang.CharSequence[] {CatalogText.literal("Hits"), CatalogText.literal("Explosions")});

        dev.dov.hack.echo.client.modules.impl.movement.Velocity.assignBootstrapSettings(Module, preparedAmount, preparedMode, preparedSources);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.movement.Velocity.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.ViewModel Module) {

        dev.dov.hack.echo.client.settings.FloatSetting preparedPosXOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosX Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotXMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotX Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedEatingBob = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("EatingBob"), 1.0f, 0.0f, 1.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotYMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotY Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedStyles = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Styles"), CatalogText.literal("Default"), new java.lang.CharSequence[] {CatalogText.literal("Default"), CatalogText.literal("None"), CatalogText.literal("Custom"), CatalogText.literal("Smooth"), CatalogText.literal("Bounce"), CatalogText.literal("Slash"), CatalogText.literal("Punch"), CatalogText.literal("Swipe"), CatalogText.literal("Spin360"), CatalogText.literal("Scale"), CatalogText.literal("Spin"), CatalogText.literal("Katana"), CatalogText.literal("Sigma"), CatalogText.literal("Push"), CatalogText.literal("Stab"), CatalogText.literal("Slice"), CatalogText.literal("Exhibition")});
        dev.dov.hack.echo.client.settings.FloatSetting preparedStrengthOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Strength Off"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedStrengthMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Strength Main"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedScaleOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Scale Off"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotYOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotY Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPosXMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosX Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotZMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotZ Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedScaleMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Scale Main"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPosYOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosY Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotZOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotZ Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedEatingAnimation = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Eating Animation"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedPosZMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosZ Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedPosZOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosZ Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotXOff = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("RotX Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedPosYMain = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("PosY Main"), 0.0f, -3.0f, 3.0f, 0.2f);

        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posXOff = preparedPosXOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotXMain = preparedRotXMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.eatingBob = preparedEatingBob;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotYMain = preparedRotYMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.styles = preparedStyles;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.strengthOff = preparedStrengthOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.strengthMain = preparedStrengthMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.scaleOff = preparedScaleOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotYOff = preparedRotYOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posXMain = preparedPosXMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotZMain = preparedRotZMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.scaleMain = preparedScaleMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posYOff = preparedPosYOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotZOff = preparedRotZOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.eatingAnimation = preparedEatingAnimation;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posZMain = preparedPosZMain;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posZOff = preparedPosZOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotXOff = preparedRotXOff;
        dev.dov.hack.echo.client.modules.impl.render.ViewModel.posYMain = preparedPosYMain;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.eatingAnimation);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.eatingBob);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.styles);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.strengthMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.strengthOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.scaleMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.scaleOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posXMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posYMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posZMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posXOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posYOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.posZOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotXMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotYMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotZMain);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotXOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotYOff);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.ViewModel.rotZOff);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.XBow Module) {

        dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedPlace = new dev.dov.hack.echo.client.settings.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {dev.dov.hack.echo.client.modules.impl.combat.XBow.bootstrapPlaceOption2(), dev.dov.hack.echo.client.modules.impl.combat.XBow.bootstrapPlaceOption4(), dev.dov.hack.echo.client.modules.impl.combat.XBow.bootstrapPlaceOption3(), dev.dov.hack.echo.client.modules.impl.combat.XBow.bootstrapPlaceOption()});
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoAim = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        dev.dov.hack.echo.client.modules.impl.combat.XBow.assignBootstrapSettings(Module, preparedPlace, preparedActivateKey, preparedAutoAim, preparedSilentRotations);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.XBow.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Xray Module) {

        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughLiquids = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Liquids"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedExposedToAir = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Exposed to Air"), false);

        dev.dov.hack.echo.client.modules.impl.render.Xray.throughLiquids = preparedThroughLiquids;
        dev.dov.hack.echo.client.modules.impl.render.Xray.exposedToAir = preparedExposedToAir;

        Module.settings.clear();
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.Xray.throughLiquids);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.Xray.exposedToAir);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, dev.dov.hack.echo.client.modules.impl.render.Xray.blocks);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.DtapSetup Module) {

        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Crystal Bind"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedCheckPlace = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Check Place"), false);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwitchDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Switch Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.IntSetting preparedSwitchChance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Switch Chance"), 100, 0, 100, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedPlaceDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Place Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedBreakDelay = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Break Delay"), 0.0f, 30.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.IntSetting preparedPlaceChance = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Place Chance"), 100, 0, 100, CatalogText.literal("%"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoCrystalAction = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Crystal"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTickTiming = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Tick Timing"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedStopOnKill = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Stop on kill"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedFakePunch = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Fake Punch"), false);
        dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist = new dev.dov.hack.echo.client.settings.ItemListSetting(CatalogText.literal("Item Whitelist"));

        dev.dov.hack.echo.client.modules.impl.combat.DtapSetup.assignBootstrapSettings(Module, preparedActivateKey, preparedCheckPlace, preparedSwitchDelay, preparedSwitchChance, preparedPlaceDelay, preparedBreakDelay, preparedPlaceChance, preparedAutoCrystalAction, preparedTickTiming, preparedStopOnKill, preparedFakePunch, preparedItemWhitelist);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.DtapSetup.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.internals.InputSimulation Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Mode"),
                dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapModeReplay(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapModeOff(),
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapModeReplay(),
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapModeNative()
                });
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedOutputTarget = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Output Target"),
                dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapTargetOsAndGame(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapTargetOsAndGame(),
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapTargetGameOnly()
                });
        dev.dov.hack.echo.client.settings.BooleanSetting preparedMouseClicks = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Mouse Clicks"), true);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedHotbarSwaps = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Hotbar Swaps"),
                dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapHotbarKeys(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapHotbarOff(),
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapHotbarKeys(),
                        dev.dov.hack.echo.client.modules.internals.InputSimulation.bootstrapHotbarKeysScroll()
                });
        dev.dov.hack.echo.client.settings.BooleanSetting preparedMovementKeys = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Movement Keys"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOffhandSwap = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Offhand Swap"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedInventoryCursor = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Inventory Cursor"), true);
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedHoldDuration = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Hold Duration"), 25.0f, 65.0f, 10.0f, 150.0f, 5.0f, CatalogText.literal(" ms"));
        dev.dov.hack.echo.client.settings.FloatRangeSetting preparedCursorDuration = new dev.dov.hack.echo.client.settings.FloatRangeSetting(CatalogText.literal("Cursor Duration"), 60.0f, 180.0f, 20.0f, 350.0f, 10.0f, CatalogText.literal(" ms"));

        dev.dov.hack.echo.client.modules.internals.InputSimulation.assignBootstrapSettings(
                Module, preparedMode, preparedOutputTarget, preparedMouseClicks, preparedHotbarSwaps,
                preparedMovementKeys, preparedOffhandSwap, preparedInventoryCursor, preparedHoldDuration, preparedCursorDuration);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.internals.InputSimulation.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.combat.AutoWeb Module) {

        dev.dov.hack.echo.client.settings.ChoiceSetting preparedStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Style"), CatalogText.literal("Legit"), new java.lang.CharSequence[] {CatalogText.literal("Legit"), CatalogText.literal("Blatant")});
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("On Key"), new java.lang.CharSequence[] {CatalogText.literal("On Key"), CatalogText.literal("Always")});
        dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Activate Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedSwapMode = new dev.dov.hack.echo.client.settings.ChoiceSetting(CatalogText.literal("Swap Mode"), CatalogText.literal("Legit"), new java.lang.CharSequence[] {CatalogText.literal("Legit"), CatalogText.literal("Silent")});
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSwapBack = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Swap Back"), true);
        dev.dov.hack.echo.client.settings.IntSetting preparedSwapDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Swap Delay"), 1, 1, 6, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 4.2f, 1.5f, 5.5f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedFov = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("FOV"), 140.0f, 30.0f, 360.0f, 5.0f, CatalogText.literal(" deg"));
        dev.dov.hack.echo.client.settings.IntSetting preparedPredictTicks = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Predict Ticks"), 2, 0, 6, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.IntSetting preparedPlaceDelay = new dev.dov.hack.echo.client.settings.IntSetting(CatalogText.literal("Place Delay"), 4, 1, 20, CatalogText.literal(" ticks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedDoubleWeb = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Double Web"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAvoidSelf = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Avoid Self"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnlyGrounded = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Only Grounded"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSilentRotations = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRotationSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Rotation Speed"), 8.0f, 1.0f, 15.0f, 0.5f);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedAutoClearSelf = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Auto Clear Self"), true);

        dev.dov.hack.echo.client.modules.impl.combat.AutoWeb.assignBootstrapSettings(
                Module, preparedStyle, preparedMode, preparedActivateKey,
                preparedSwapMode, preparedSwapBack, preparedSwapDelay,
                preparedRange, preparedFov, preparedPredictTicks,
                preparedPlaceDelay, preparedDoubleWeb, preparedAvoidSelf, preparedOnlyGrounded,
                preparedSilentRotations, preparedRotationSpeed, preparedAutoClearSelf);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.combat.AutoWeb.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.AutoConfigHud Module) {

        dev.dov.hack.echo.client.settings.KeybindSetting preparedUnlockKey = new dev.dov.hack.echo.client.settings.KeybindSetting(CatalogText.literal("Unlock Key"), dev.dov.hack.echo.client.keyboard.InputKey.NONE);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedHideWhenIdle = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Hide When Idle"), false);

        dev.dov.hack.echo.client.modules.impl.render.AutoConfigHud.assignBootstrapSettings(Module, preparedUnlockKey, preparedHideWhenIdle);

        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.AutoConfigHud.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.PopChams Module) {
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnTotemPop = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Totem Pop"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnHit = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Hit"), false);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnDeath = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Death"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedDuration = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Duration"), 1.2f, 0.2f, 4.0f, 0.1f, CatalogText.literal(" s"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedRiseHeight = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Rise Height"), 1.2f, 0.0f, 4.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedFillColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Fill Color"), 255, 255, 255, 75);
        dev.dov.hack.echo.client.settings.ColorSetting preparedOutlineColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Outline Color"), 255, 255, 255, 220);

        dev.dov.hack.echo.client.modules.impl.render.PopChams.assignBootstrapSettings(
                Module, preparedOnTotemPop, preparedOnHit, preparedOnDeath, preparedThroughWalls,
                preparedDuration, preparedRiseHeight, preparedFillColor, preparedOutlineColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.PopChams.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.JumpCircles Module) {
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnJump = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Jump"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOnLand = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("On Land"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedOtherPlayers = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Other Players"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedMaxRadius = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Max Radius"), 1.3f, 0.4f, 3.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedDuration = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Duration"), 0.85f, 0.2f, 2.5f, 0.05f, CatalogText.literal(" s"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Color"), 255, 255, 255, 210);

        dev.dov.hack.echo.client.modules.impl.render.JumpCircles.assignBootstrapSettings(
                Module, preparedOnJump, preparedOnLand, preparedOtherPlayers, preparedMaxRadius, preparedDuration, preparedColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.JumpCircles.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.BlockHighlight Module) {
        dev.dov.hack.echo.client.settings.BooleanSetting preparedTargetHighlight = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Target Highlight"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedPlacementGhost = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Placement Ghost"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.dov.hack.echo.client.settings.FloatSetting preparedSmoothSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Smooth Speed"), 18.0f, 2.0f, 40.0f, 1.0f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedGhostDuration = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Ghost Duration"), 0.65f, 0.15f, 2.5f, 0.05f, CatalogText.literal(" s"));
        dev.dov.hack.echo.client.settings.ColorSetting preparedFillColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Fill Color"), 255, 255, 255, 42);
        dev.dov.hack.echo.client.settings.ColorSetting preparedOutlineColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Outline Color"), 255, 255, 255, 200);
        dev.dov.hack.echo.client.settings.ColorSetting preparedGhostColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Ghost Color"), 180, 220, 255, 85);

        dev.dov.hack.echo.client.modules.impl.render.BlockHighlight.assignBootstrapSettings(
                Module, preparedTargetHighlight, preparedPlacementGhost, preparedThroughWalls,
                preparedSmoothSpeed, preparedGhostDuration, preparedFillColor, preparedOutlineColor, preparedGhostColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.BlockHighlight.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.PearlMarkers Module) {
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowPearls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Ender Pearls"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowWindCharges = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Wind Charges"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowBeam = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Beacon Beam"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowEtaTag = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("ETA Tag"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedPearlColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Pearl Color"), 170, 120, 255, 220);
        dev.dov.hack.echo.client.settings.ColorSetting preparedWindColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Wind Color"), 190, 235, 255, 220);

        dev.dov.hack.echo.client.modules.impl.render.PearlMarkers.assignBootstrapSettings(
                Module, preparedShowPearls, preparedShowWindCharges, preparedShowBeam, preparedShowEtaTag,
                preparedThroughWalls, preparedPearlColor, preparedWindColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.PearlMarkers.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.TargetHalo Module) {
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Style"),
                dev.dov.hack.echo.client.modules.impl.render.TargetHalo.bootstrapOrbitStyle(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.impl.render.TargetHalo.bootstrapOrbitStyle(),
                        dev.dov.hack.echo.client.modules.impl.render.TargetHalo.bootstrapDoubleStyle(),
                        dev.dov.hack.echo.client.modules.impl.render.TargetHalo.bootstrapGroundStyle()
                });
        dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Speed"), 1.2f, 0.2f, 4.0f, 0.1f);
        dev.dov.hack.echo.client.settings.FloatSetting preparedRadius = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Extra Radius"), 0.22f, 0.0f, 1.0f, 0.02f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.FloatSetting preparedRange = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Range"), 12.0f, 3.0f, 40.0f, 0.5f, CatalogText.literal(" blocks"));
        dev.dov.hack.echo.client.settings.BooleanSetting preparedThroughWalls = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Through Walls"), true);
        dev.dov.hack.echo.client.settings.ColorSetting preparedPrimaryColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Primary Color"), 255, 255, 255, 230);
        dev.dov.hack.echo.client.settings.ColorSetting preparedSecondaryColor = new dev.dov.hack.echo.client.settings.ColorSetting(CatalogText.literal("Secondary Color"), 155, 195, 255, 210);

        dev.dov.hack.echo.client.modules.impl.render.TargetHalo.assignBootstrapSettings(
                Module, preparedStyle, preparedSpeed, preparedRadius, preparedRange,
                preparedThroughWalls, preparedPrimaryColor, preparedSecondaryColor);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.TargetHalo.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Keystrokes Module) {
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowCps = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show CPS"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowSpace = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Space"), true);
        dev.dov.hack.echo.client.settings.BooleanSetting preparedShowPing = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Show Ping"), true);

        dev.dov.hack.echo.client.modules.impl.render.Keystrokes.assignBootstrapSettings(
                Module, preparedShowCps, preparedShowSpace, preparedShowPing);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Keystrokes.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

    public static void initialize(dev.dov.hack.echo.client.modules.impl.render.Animations Module) {
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedAttackStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Attack Style"),
                dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackSlash(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapNone(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackSlash(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackBlockHit(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackArc(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackThrust(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapAttackFlick()
                });
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedUseStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Use Style"),
                dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapUseLowGuard(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapNone(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapUseSip(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapUseLowGuard(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapUsePulse()
                });
        dev.dov.hack.echo.client.settings.ChoiceSetting preparedPlaceStyle = new dev.dov.hack.echo.client.settings.ChoiceSetting(
                CatalogText.literal("Place Style"),
                dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapPlacePop(),
                new java.lang.CharSequence[] {
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapNone(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapPlacePop(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapPlaceSlide(),
                        dev.dov.hack.echo.client.modules.impl.render.Animations.bootstrapPlaceSnap()
                });
        dev.dov.hack.echo.client.settings.BooleanSetting preparedSmoothEquip = new dev.dov.hack.echo.client.settings.BooleanSetting(CatalogText.literal("Smooth Equip"), true);
        dev.dov.hack.echo.client.settings.FloatSetting preparedIntensity = new dev.dov.hack.echo.client.settings.FloatSetting(CatalogText.literal("Intensity"), 1.0f, 0.2f, 2.0f, 0.05f);

        dev.dov.hack.echo.client.modules.impl.render.Animations.assignBootstrapSettings(
                Module, preparedAttackStyle, preparedUseStyle, preparedPlaceStyle, preparedSmoothEquip, preparedIntensity);
        Module.settings.clear();
        dev.dov.hack.echo.client.modules.impl.render.Animations.appendBootstrapSettings(Module);
        Module.postInitialize();
    }

}