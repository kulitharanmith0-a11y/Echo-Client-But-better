package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.AimPointMode;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class SpearAssist extends Module {
    private static final CharSequence BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence NATURAL = CatalogText.literal("Natural");
    private static ChoiceSetting aimMath;
    private static ChoiceSetting aimVector;
    private static ChoiceSetting targetPriority;
    private static BooleanSetting random;
    private static BooleanSetting silentRotations;
    private static BooleanSetting throughWalls;
    private static FloatSetting horizontalSpeed;
    private static FloatSetting verticalSpeed;
    private static FloatSetting range;
    private static FloatSetting fov;
    private static FloatSetting multipoint;
    private boolean waitingForRotationRelease;

    public SpearAssist() {
        super(new ModuleInfo(CatalogText.literal("Spear Assist"), CatalogText.literal("Automatically aims at targets while holding a spear"), ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        waitingForRotationRelease = false;
        horizontalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(value -> !aimMath.isSelected(BLATANT));
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
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
        if (!isUsingSpear()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (DeferredActionQueue.contains(MaceSwap.class)
                            || DeferredActionQueue.contains(AutoMaceSwap.class)
                            || DeferredActionQueue.contains(SpearReach.class))) {
                return;
            }
            clearAim();
            return;
        }
        double distance = range.getValue();
        double distanceSquared = distance * distance;
        Vec3d eyes = client.player.getCameraPosVec(1.0F);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(distance), entity -> isCandidate(eyes, distanceSquared, entity));
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
        boolean aimed = rotations().aim(this, RotationPriority.HIGH, target, new AimPointSettings(aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT, multipoint.getValue(), random.getValue(), throughWalls.getValue()), new RotationSettings(silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, transition(), horizontalSpeed.getValue() * 20.0F, verticalSpeed.getValue() * 20.0F));
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
        return EntityRaycasts.isWithinFov(client.player, target, fov.getValue());
    }

    private boolean isUsingSpear() {
        if (isOutsideWorld() || !client.player.isUsingItem()) return false;
        ItemStack active = client.player.getActiveItem();
        if (active.isEmpty()) return false;
        return InventoryItems.isSpear(active.getItem());
    }

    private boolean isCandidate(Vec3d eyes, double distanceSquared, LivingEntity entity) {
        return entity != client.player && !entity.isRemoved() && entity.isAlive()
                && TargetSelection.isValidTarget(entity)
                && eyes.squaredDistanceTo(entity.getEntityPos()) <= distanceSquared && isWithinFov(entity);
    }

    public static void appendBootstrapSettings(SpearAssist Module) {
        CatalogSettings.append(Module, aimMath);
        CatalogSettings.append(Module, aimVector);
        CatalogSettings.append(Module, targetPriority);
        CatalogSettings.append(Module, random);
        CatalogSettings.append(Module, silentRotations);
        CatalogSettings.append(Module, throughWalls);
        CatalogSettings.append(Module, horizontalSpeed);
        CatalogSettings.append(Module, verticalSpeed);
        CatalogSettings.append(Module, range);
        CatalogSettings.append(Module, fov);
        CatalogSettings.append(Module, multipoint);
    }

    public static void assignBootstrapSettings(SpearAssist Module, ChoiceSetting preparedAimMath, BooleanSetting preparedThroughWalls, ChoiceSetting preparedAimVector, FloatSetting preparedMultipoint, FloatSetting preparedVerticalSpeed, ChoiceSetting preparedTargetPriority, FloatSetting preparedRange, FloatSetting preparedHorizontalSpeed, FloatSetting preparedFov, BooleanSetting preparedRandom, BooleanSetting preparedSilentRotations) {
        aimMath = preparedAimMath;
        throughWalls = preparedThroughWalls;
        aimVector = preparedAimVector;
        multipoint = preparedMultipoint;
        verticalSpeed = preparedVerticalSpeed;
        targetPriority = preparedTargetPriority;
        range = preparedRange;
        horizontalSpeed = preparedHorizontalSpeed;
        fov = preparedFov;
        random = preparedRandom;
        silentRotations = preparedSilentRotations;
    }
}
