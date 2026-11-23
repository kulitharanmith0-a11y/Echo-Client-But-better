package com.blamejared.clumps.client.modules.impl.world;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.interaction.BlockBreakingEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Tunneler extends Module {
    private static final CharSequence SINGLE_BLOCK = CatalogText.literal("1x1");
    private static ChoiceSetting mode;
    private static BooleanSetting silent;
    private boolean breakingBlock;
    private boolean targetUpperBlock;
    private boolean aimingAtBlock;

    public Tunneler() {
        super(new ModuleInfo(CatalogText.literal("Tunneler"), CatalogText.literal("Automatically mines tunnels while walking forward"), ModuleCategory.WORLD, false));
    }

    public static void appendBootstrapSettings(Tunneler Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, silent);
    }

    public static void assignBootstrapSettings(Tunneler Module, BooleanSetting preparedSilent, ChoiceSetting preparedMode) {
        silent = preparedSilent;
        mode = preparedMode;
    }

    @Override
    public void postInitialize() {
        breakingBlock = false;
        targetUpperBlock = true;
        aimingAtBlock = false;
        super.postInitialize();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) return;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos position = hit.getBlockPos();
            var side = hit.getSide();
            if (client.world.getBlockState(position).isAir()) {
                breakingBlock = false;
                return;
            }
            if (client.interactionManager != null) {
                if (client.interactionManager.updateBlockBreakingProgress(position, side)) {
                    client.player.swingHand(Hand.MAIN_HAND);
                    breakingBlock = true;
                } else {
                    breakingBlock = false;
                }
            }
        }
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld()) return;
        event.forward = true;
        event.backward = false;
        event.left = false;
        event.right = false;
        event.jump = false;
        event.sneak = false;
        event.allowMovementInFreecam = true;
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld()) return;
        Vec3d target = findTarget();
        if (target == null) {
            if (aimingAtBlock && rotations().isAiming() && rotations().isOwnedBy(this)) {
                if (!rotations().beginReturn(this)) aimingAtBlock = false;
            } else if (rotations().isOwnedBy(this)) {
                rotations().cancel(this);
                aimingAtBlock = false;
            }
            return;
        }
        var manager = rotations();
        RotationSettings settings = RotationSettings.uniform(
                silent.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, 100.0F);
        if (manager.aim(this, RotationPriority.HIGH, target, settings)) aimingAtBlock = true;
    }

    private Vec3d findTarget() {
        if (client.player == null || client.world == null) return null;
        float yaw = client.player.getYaw();
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        Vec3d playerPosition = client.player.getEntityPos();
        int y = (int) Math.floor(playerPosition.y);
        double x = playerPosition.x + forwardX;
        double z = playerPosition.z + forwardZ;
        if (mode.isSelected(SINGLE_BLOCK)) {
            BlockPos block = new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
            return client.world.getBlockState(block).isAir() ? null : Vec3d.ofCenter(block);
        }
        BlockPos lower = new BlockPos((int) Math.floor(x), y, (int) Math.floor(z));
        BlockPos upper = new BlockPos((int) Math.floor(x), y + 1, (int) Math.floor(z));
        boolean upperSolid = !client.world.getBlockState(upper).isAir();
        boolean lowerSolid = !client.world.getBlockState(lower).isAir();
        if (!upperSolid && !lowerSolid) return null;
        if (upperSolid && !lowerSolid) {
            targetUpperBlock = true;
        } else if (!upperSolid && lowerSolid) {
            targetUpperBlock = false;
        } else if (!breakingBlock) {
            targetUpperBlock = !targetUpperBlock;
        }
        return Vec3d.ofCenter(targetUpperBlock ? upper : lower);
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld()) return;
        event.cancel();
    }

    @Subscribe
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        event.cancel();
    }

    @Override
    public void onDisable() {
        if (rotations().isOwnedBy(this)) rotations().cancel(this);
        aimingAtBlock = false;
        breakingBlock = false;
        targetUpperBlock = true;
        super.onDisable();
    }
}
