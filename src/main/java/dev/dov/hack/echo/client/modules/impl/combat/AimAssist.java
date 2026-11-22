package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.rotation.AimTransition;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.handlers.rotation.RotationPriority;
import dev.dov.hack.echo.client.handlers.rotation.RotationSettings;
import dev.dov.hack.echo.client.handlers.rotation.RotationVisibility;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.utils.targeting.AimPointMode;
import dev.dov.hack.echo.client.utils.targeting.AimPointSettings;
import dev.dov.hack.echo.client.utils.targeting.EntityRaycasts;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private static final CharSequence BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence NATURAL = CatalogText.literal("Natural");
    private static final CharSequence SINGLE_FOV = CatalogText.literal("Single");
    private static final CharSequence SPLIT_FOV = CatalogText.literal("Split");
    private boolean waitingForRotationRelease;
    private static ChoiceSetting aimMath;
    private static ChoiceSetting aimVector;
    private static ChoiceSetting targetPriority;
    private static BooleanSetting random;
    private static BooleanSetting silentRotations;
    private static BooleanSetting throughWalls;
    private static FloatSetting horizontalSpeed;
    private static FloatSetting verticalSpeed;
    private static BooleanSetting onClick;
    private static FloatSetting range;
    private static ChoiceSetting fovMode;
    private static FloatSetting fov;
    private static FloatSetting yawFOV;
    private static FloatSetting pitchFOV;
    private static FloatSetting multipoint;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;

    public AimAssist() {
        super(new ModuleInfo(CatalogText.literal("Aim Assist"), CatalogText.literal("Automatically aims at targets"), ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        waitingForRotationRelease = false;
        horizontalSpeed.setAllowOutOfRange(true);
        verticalSpeed.setAllowOutOfRange(true);
        horizontalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        fov.setVisibilityPredicate(value -> fovMode.isSelected(SINGLE_FOV));
        yawFOV.setVisibilityPredicate(value -> fovMode.isSelected(SPLIT_FOV));
        pitchFOV.setVisibilityPredicate(value -> fovMode.isSelected(SPLIT_FOV));
        aimMath.tooltip("Aim");
        aimVector.tooltip("Aim");
        targetPriority.tooltip("Targeting");
        random.tooltip("Aim");
        silentRotations.tooltip("Aim");
        throughWalls.tooltip("Aim");
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
        onClick.tooltip("Conditions");
        range.tooltip("Targeting");
        fovMode.tooltip("Targeting");
        fov.tooltip("Targeting");
        yawFOV.tooltip("Targeting");
        pitchFOV.tooltip("Targeting");
        multipoint.tooltip("Aim");
        itemWhitelist.tooltip("Conditions");
        hotbarWhitelist.tooltip("Conditions");
    }

    @Override
    public CharSequence getSuffix() {
        return aimMath.getValue();
    }

    @Override
    public void onEnable() {
        waitingForRotationRelease = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            waitingForRotationRelease = true;
            return;
        }
        waitingForRotationRelease = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (waitingForRotationRelease) {
            finishDisable();
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            rotations().beginReturn(this);
            return;
        }
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (DeferredActionQueue.contains(MaceSwap.class)
                            || AutoMaceSwap.hasPendingSwap()
                            || DeferredActionQueue.contains(SpearReach.class))) {
                return;
            }
            clearAim();
            return;
        }
        if (onClick.getValue() && !KeybindingCache.isPressed(client.options.attackKey)) {
            clearAim();
            return;
        }
        double distance = range.getValue();
        double distanceSquared = distance * distance;
        Vec3d eyes = client.player.getCameraPosVec(1.0F);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(distance), AimAssist::isLivingCandidate)
                .stream().filter(TargetSelection::isValidTarget)
                .filter(entity -> eyes.squaredDistanceTo(entity.getEntityPos()) <= distanceSquared)
                .filter(this::isWithinFov).toList();
        LivingEntity selected = EntityRaycasts.selectTarget(client.player, candidates, eyes, targetPriority);
        LivingEntity target = TargetSelection.resolveTarget(selected);
        if (target == null || eyes.squaredDistanceTo(target.getEntityPos()) > distanceSquared) {
            clearAim();
            return;
        }
        if (!isWithinFov(target)) {
            clearAim();
            return;
        }
        boolean aimed = rotations().aim(this, RotationPriority.NORMAL, target, new AimPointSettings(aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT, multipoint.getValue(), random.getValue(), throughWalls.getValue()), new RotationSettings(silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, transition(), horizontalSpeed.getValue() * 20.0F, verticalSpeed.getValue() * 20.0F));
        if (!aimed) clearAim();
    }

    private void clearAim() {
        if (silentRotations.getValue()) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private void finishDisable() {
        if (rotations().beginReturn(this)) return;
        waitingForRotationRelease = false;
        super.onDisable();
    }

    private AimTransition transition() {
        if (aimMath.isSelected(NATURAL)) return AimTransition.NATURAL;
        return aimMath.isSelected(BLATANT) ? AimTransition.SNAP : AimTransition.EASED;
    }

    private boolean isWithinFov(LivingEntity target) {
        if (fovMode.isSelected(SPLIT_FOV)) {
            return EntityRaycasts.isWithinFov(client.player, target, yawFOV.getValue(), pitchFOV.getValue());
        }
        return EntityRaycasts.isWithinFov(client.player, target, fov.getValue());
    }

    private static boolean isLivingCandidate(LivingEntity entity) {
        return entity != client.player && !entity.isRemoved() && entity.isAlive();
    }

    
    public static void appendBootstrapSettings(AimAssist Module) {
        CatalogSettings.append(Module, aimMath);
        CatalogSettings.append(Module, aimVector);
        CatalogSettings.append(Module, targetPriority);
        CatalogSettings.append(Module, random);
        CatalogSettings.append(Module, silentRotations);
        CatalogSettings.append(Module, throughWalls);
        CatalogSettings.append(Module, horizontalSpeed);
        CatalogSettings.append(Module, verticalSpeed);
        CatalogSettings.append(Module, onClick);
        CatalogSettings.append(Module, range);
        CatalogSettings.append(Module, fovMode);
        CatalogSettings.append(Module, fov);
        CatalogSettings.append(Module, yawFOV);
        CatalogSettings.append(Module, pitchFOV);
        CatalogSettings.append(Module, multipoint);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
    }

    
    public static void assignBootstrapSettings(AimAssist Module, ChoiceSetting preparedFovMode, FloatSetting preparedRange, FloatSetting preparedPitchFOV, ItemListSetting preparedItemWhitelist, BooleanSetting preparedRandom, BooleanSetting preparedSilentRotations, FloatSetting preparedMultipoint, BooleanSetting preparedOnClick, ChoiceSetting preparedTargetPriority, BooleanSetting preparedThroughWalls, FloatSetting preparedVerticalSpeed, HotbarSlotsSetting preparedHotbarWhitelist, FloatSetting preparedHorizontalSpeed, ChoiceSetting preparedAimMath, ChoiceSetting preparedAimVector, FloatSetting preparedFov, FloatSetting preparedYawFOV) {
        fovMode = preparedFovMode;
        range = preparedRange;
        pitchFOV = preparedPitchFOV;
        itemWhitelist = preparedItemWhitelist;
        random = preparedRandom;
        silentRotations = preparedSilentRotations;
        multipoint = preparedMultipoint;
        onClick = preparedOnClick;
        targetPriority = preparedTargetPriority;
        throughWalls = preparedThroughWalls;
        verticalSpeed = preparedVerticalSpeed;
        hotbarWhitelist = preparedHotbarWhitelist;
        horizontalSpeed = preparedHorizontalSpeed;
        aimMath = preparedAimMath;
        aimVector = preparedAimVector;
        fov = preparedFov;
        yawFOV = preparedYawFOV;
    }
}
