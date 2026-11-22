package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.handlers.rotation.AimTransition;
import dev.dov.hack.echo.client.handlers.rotation.RotationPriority;
import dev.dov.hack.echo.client.handlers.rotation.RotationSettings;
import dev.dov.hack.echo.client.handlers.rotation.RotationVisibility;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.Rotation;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.placement.BlockPlacement;
import dev.dov.hack.echo.client.utils.placement.PlacementTarget;
import dev.dov.hack.echo.client.utils.targeting.EntityRaycasts;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoWeb extends Module {
    private static final CharSequence MODE_KEY = CatalogText.literal("On Key");
    private static final CharSequence MODE_ALWAYS = CatalogText.literal("Always");
    private static final CharSequence SECTION_PLACEMENT = CatalogText.literal("Placement");
    private static final CharSequence SECTION_ROTATION = CatalogText.literal("Rotation");
    private static final CharSequence SECTION_ESCAPE = CatalogText.literal("Escape");

    private static final float AIM_TOLERANCE_DEGREES = 10.0F;
    private static final int WATER_PICKUP_WINDOW_TICKS = 6;

    public static ChoiceSetting mode;
    public static KeybindSetting activateKey;
    public static FloatSetting range;
    public static FloatSetting fov;
    public static IntSetting predictTicks;
    public static IntSetting placeDelay;
    public static BooleanSetting doubleWeb;
    public static BooleanSetting avoidSelf;
    public static BooleanSetting onlyGrounded;
    public static BooleanSetting silentRotations;
    public static FloatSetting rotationSpeed;
    public static BooleanSetting autoClearSelf;

    private PlacementTarget activePlacement;
    private BlockPos placedWaterPos;
    private long placedWaterTick;
    private int cooldownTicks;
    private boolean disablePending;

    public AutoWeb() {
        super(new ModuleInfo(
                CatalogText.literal("Auto Web"),
                CatalogText.literal("Places cobwebs at predicted enemy feet and clears self-webs"),
                ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        resetState();
        activateKey.setVisibilityPredicate(ignored -> mode.isSelected(MODE_KEY));
        activateKey.description(CatalogText.literal("Hold to place cobwebs when Mode is On Key"));
        range.description(CatalogText.literal("Maximum distance to web targets"));
        fov.description(CatalogText.literal("Maximum angle from crosshair to target"));
        predictTicks.description(CatalogText.literal("Ticks ahead to predict moving targets"));
        placeDelay.description(CatalogText.literal("Minimum ticks between cobweb placements"));
        doubleWeb.description(CatalogText.literal("Also places a cobweb at target head height after feet are webbed"));
        avoidSelf.description(CatalogText.literal("Prevents placing cobwebs on blocks intersecting yourself"));
        onlyGrounded.description(CatalogText.literal("Only places cobwebs on targets standing on the ground"));
        autoClearSelf.description(CatalogText.literal("Uses a hotbar water bucket to quickly break a cobweb trapping your own feet"));

        range.tooltip(SECTION_PLACEMENT);
        fov.tooltip(SECTION_PLACEMENT);
        predictTicks.tooltip(SECTION_PLACEMENT);
        placeDelay.tooltip(SECTION_PLACEMENT);
        doubleWeb.tooltip(SECTION_PLACEMENT);
        avoidSelf.tooltip(SECTION_PLACEMENT);
        onlyGrounded.tooltip(SECTION_PLACEMENT);
        silentRotations.tooltip(SECTION_ROTATION);
        rotationSpeed.tooltip(SECTION_ROTATION);
        autoClearSelf.tooltip(SECTION_ESCAPE);
    }

    @Override
    public CharSequence getSuffix() {
        return mode.getValue();
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        activePlacement = null;
        placedWaterPos = null;
        placedWaterTick = -1L;
        DeferredActionQueue.finish(this, true);
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            if (!rotations().beginReturn(this)) {
                disablePending = false;
                super.onDisable();
            }
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activePlacement == null) {
            releaseRotation();
            return;
        }
        Vec3d aimPoint = activePlacement.hitResult().getPos();
        float speed = rotationSpeed.getValue() * 20.0F;
        RotationSettings settings = new RotationSettings(
                silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED,
                speed,
                speed);
        boolean accepted = rotations().aim(this, RotationPriority.HIGH, aimPoint, settings);
        if (!accepted) {
            releaseRotation();
        }
    }

    @Subscribe(priority = EventPriority.NORMAL)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (disablePending) {
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            clearActivePlacement();
            return;
        }
        if (!client.player.isAlive() || client.player.isUsingItem()) {
            clearActivePlacement();
            return;
        }
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) {
            clearActivePlacement();
            return;
        }

        // Priority 1: Pick back up water placed for self-web clear
        if (placedWaterPos != null) {
            if (handleWaterPickup()) {
                return;
            }
        }

        // Priority 2: Auto-clear cobweb at own feet using water bucket
        if (autoClearSelf.getValue() && handleSelfWebClear()) {
            return;
        }

        // Priority 3: Place cobweb on enemy target
        if (!isTriggerActive()) {
            clearActivePlacement();
            return;
        }
        if (cooldownTicks > 0) {
            return;
        }
        int webSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.COBWEB));
        if (webSlot == -1) {
            clearActivePlacement();
            return;
        }

        PlacementTarget targetPlacement = findBestWebPlacement();
        if (targetPlacement == null) {
            clearActivePlacement();
            return;
        }

        activePlacement = targetPlacement;
        if (!isAimedAt(targetPlacement.hitResult().getPos())) {
            return;
        }
        if (!selectHotbarSlot(webSlot)) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
            return;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, targetPlacement.hitResult(), true);
        if (result.isAccepted()) {
            cooldownTicks = Math.max(1, placeDelay.getValue());
            activePlacement = null;
            DeferredActionQueue.finish(this, true);
        }
    }

    private boolean handleSelfWebClear() {
        BlockPos selfWeb = findSelfCobwebPos();
        if (selfWeb == null) {
            return false;
        }
        int bucketSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.WATER_BUCKET));
        if (bucketSlot == -1) {
            return false;
        }
        PlacementTarget placement = BlockPlacement.find(selfWeb, true, true, true);
        if (placement == null) {
            return false;
        }
        activePlacement = placement;
        if (!isAimedAt(placement.hitResult().getPos())) {
            return true;
        }
        if (!selectHotbarSlot(bucketSlot)) {
            return true;
        }
        if (!client.player.getMainHandStack().isOf(Items.WATER_BUCKET)) {
            return true;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, placement.hitResult(), true);
        if (result.isAccepted()) {
            placedWaterPos = selfWeb;
            placedWaterTick = client.player.age;
            activePlacement = null;
        }
        return true;
    }

    private boolean handleWaterPickup() {
        long elapsed = client.player.age - placedWaterTick;
        if (elapsed > WATER_PICKUP_WINDOW_TICKS) {
            placedWaterPos = null;
            placedWaterTick = -1L;
            DeferredActionQueue.finish(this, true);
            return false;
        }
        if (elapsed < 1L) {
            return true;
        }
        BlockState state = client.world.getBlockState(placedWaterPos);
        if (!state.isOf(Blocks.WATER)) {
            placedWaterPos = null;
            placedWaterTick = -1L;
            DeferredActionQueue.finish(this, true);
            return false;
        }
        int emptyBucketSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.BUCKET));
        if (emptyBucketSlot == -1) {
            placedWaterPos = null;
            placedWaterTick = -1L;
            DeferredActionQueue.finish(this, true);
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(placedWaterPos), net.minecraft.util.math.Direction.UP, placedWaterPos, false);
        activePlacement = new PlacementTarget(placedWaterPos, hit);
        if (!isAimedAt(hit.getPos())) {
            return true;
        }
        if (!selectHotbarSlot(emptyBucketSlot)) {
            return true;
        }
        if (!client.player.getMainHandStack().isOf(Items.BUCKET)) {
            return true;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, hit, true);
        if (result.isAccepted()) {
            placedWaterPos = null;
            placedWaterTick = -1L;
            activePlacement = null;
            cooldownTicks = Math.max(1, placeDelay.getValue());
            DeferredActionQueue.finish(this, true);
        }
        return true;
    }

    private BlockPos findSelfCobwebPos() {
        Box box = client.player.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private PlacementTarget findBestWebPlacement() {
        double maxRange = range.getValue();
        double maxRangeSq = maxRange * maxRange;
        Vec3d eye = client.player.getEyePos();
        float maxFov = fov.getValue();

        List<PlayerEntity> targets = client.world.getEntitiesByClass(
                PlayerEntity.class,
                client.player.getBoundingBox().expand(maxRange + 1.5),
                player -> isValidWebTarget(player, eye, maxRangeSq, maxFov));
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)));

        int lead = Math.max(0, predictTicks.getValue());
        for (PlayerEntity target : targets) {
            BlockPos feet = predictTargetFeet(target, lead);
            PlacementTarget feetPlacement = tryBuildWebPlacement(feet, eye, maxRangeSq);
            if (feetPlacement != null) {
                return feetPlacement;
            }
            if (doubleWeb.getValue() && isCobweb(feet)) {
                PlacementTarget headPlacement = tryBuildWebPlacement(feet.up(), eye, maxRangeSq);
                if (headPlacement != null) {
                    return headPlacement;
                }
            }
        }
        return null;
    }

    private PlacementTarget tryBuildWebPlacement(BlockPos pos, Vec3d eye, double maxRangeSq) {
        if (pos == null || isCobweb(pos)) {
            return null;
        }
        BlockState state = client.world.getBlockState(pos);
        if (!state.isReplaceable()) {
            return null;
        }
        if (avoidSelf.getValue() && wouldWebSelf(pos)) {
            return null;
        }
        if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > maxRangeSq) {
            return null;
        }
        PlacementTarget target = BlockPlacement.find(pos, true, true, true);
        if (target == null) {
            return null;
        }
        if (eye.squaredDistanceTo(target.hitResult().getPos()) > maxRangeSq) {
            return null;
        }
        return target;
    }

    private BlockPos predictTargetFeet(PlayerEntity target, int leadTicks) {
        if (leadTicks <= 0) {
            return target.getBlockPos();
        }
        double vx = target.getX() - target.lastX;
        double vz = target.getZ() - target.lastZ;
        double predX = target.getX() + vx * leadTicks;
        double predZ = target.getZ() + vz * leadTicks;
        BlockPos predicted = BlockPos.ofFloored(predX, target.getY() + 0.05, predZ);
        if (client.world.getBlockState(predicted).isReplaceable()) {
            return predicted;
        }
        return target.getBlockPos();
    }

    private boolean isValidWebTarget(PlayerEntity player, Vec3d eye, double maxRangeSq, float maxFov) {
        if (player == null || player == client.player || !player.isAlive() || player.isRemoved()
                || player.isSpectator() || !TargetSelection.isValidTarget(player)) {
            return false;
        }
        if (onlyGrounded.getValue() && !player.isOnGround()) {
            return false;
        }
        if (player.getBoundingBox().squaredMagnitude(eye) > maxRangeSq) {
            return false;
        }
        if (maxFov < 360.0F && !EntityRaycasts.isWithinFov(client.player, player, maxFov, maxFov)) {
            return false;
        }
        BlockPos feet = player.getBlockPos();
        if (isCobweb(feet) && (!doubleWeb.getValue() || isCobweb(feet.up()))) {
            return false;
        }
        return true;
    }

    private boolean wouldWebSelf(BlockPos pos) {
        Box selfExpanded = client.player.getBoundingBox().expand(0.15, 0.0, 0.15);
        return selfExpanded.intersects(new Box(pos));
    }

    private boolean isCobweb(BlockPos pos) {
        return client.world.getBlockState(pos).isOf(Blocks.COBWEB);
    }

    private boolean isTriggerActive() {
        if (mode.isSelected(MODE_ALWAYS)) {
            return true;
        }
        InputKey key = activateKey.getKey();
        return key != null && key != InputKey.NONE && key.isPressed();
    }

    private boolean selectHotbarSlot(int slot) {
        if (client.player.getInventory().getSelectedSlot() == slot) {
            return true;
        }
        return DeferredActionQueue.select(this, slot, 3, true, null);
    }

    private boolean isAimedAt(Vec3d point) {
        Rotation current = rotations().effectiveRotation();
        if (current == null) {
            return false;
        }
        Rotation desired = Rotation.lookingAt(client.player.getEyePos(), point);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desired.yaw() - current.yaw()));
        float pitchDiff = Math.abs(desired.pitch() - current.pitch());
        return yawDiff <= AIM_TOLERANCE_DEGREES && pitchDiff <= AIM_TOLERANCE_DEGREES;
    }

    private void clearActivePlacement() {
        activePlacement = null;
        releaseRotation();
    }

    private void releaseRotation() {
        if (!rotations().isOwnedBy(this)) {
            return;
        }
        if (silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void resetState() {
        activePlacement = null;
        placedWaterPos = null;
        placedWaterTick = -1L;
        cooldownTicks = 0;
        disablePending = false;
    }

    public static void appendBootstrapSettings(AutoWeb module) {
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, activateKey);
        CatalogSettings.append(module, range);
        CatalogSettings.append(module, fov);
        CatalogSettings.append(module, predictTicks);
        CatalogSettings.append(module, placeDelay);
        CatalogSettings.append(module, doubleWeb);
        CatalogSettings.append(module, avoidSelf);
        CatalogSettings.append(module, onlyGrounded);
        CatalogSettings.append(module, silentRotations);
        CatalogSettings.append(module, rotationSpeed);
        CatalogSettings.append(module, autoClearSelf);
    }

    public static void assignBootstrapSettings(
            AutoWeb module,
            ChoiceSetting preparedMode,
            KeybindSetting preparedActivateKey,
            FloatSetting preparedRange,
            FloatSetting preparedFov,
            IntSetting preparedPredictTicks,
            IntSetting preparedPlaceDelay,
            BooleanSetting preparedDoubleWeb,
            BooleanSetting preparedAvoidSelf,
            BooleanSetting preparedOnlyGrounded,
            BooleanSetting preparedSilentRotations,
            FloatSetting preparedRotationSpeed,
            BooleanSetting preparedAutoClearSelf) {
        mode = preparedMode;
        activateKey = preparedActivateKey;
        range = preparedRange;
        fov = preparedFov;
        predictTicks = preparedPredictTicks;
        placeDelay = preparedPlaceDelay;
        doubleWeb = preparedDoubleWeb;
        avoidSelf = preparedAvoidSelf;
        onlyGrounded = preparedOnlyGrounded;
        silentRotations = preparedSilentRotations;
        rotationSpeed = preparedRotationSpeed;
        autoClearSelf = preparedAutoClearSelf;
    }
}
