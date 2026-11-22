package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.event.interaction.BlockInteractEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.lifecycle.ScreenChangeEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class HighJump extends Module {
    private static final CharSequence NORMAL_MODE = Concat.strings("Normal");
    private static final CharSequence SHULKER_MODE = Concat.strings("Shulker");
    private static final int PLACEMENT_TIMEOUT = 20;
    private static ChoiceSetting mode;
    private static FloatSetting lift;
    private static IntSetting ticks;
    private net.minecraft.util.math.BlockPos pendingPlacement;
    private int placementTimeoutTicks;
    private int remainingJumpTicks;

    public HighJump() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("High Jump"), dev.dov.hack.echo.client.init.CatalogText.literal("Boosts upward when jumping"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        ticks.tooltip("Timing");
    }

    @Override
    public CharSequence getSuffix() {
        return mode.getValue();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !mode.isSelected(NORMAL_MODE)) {
            return;
        }
        if (!event.jump || !client.player.isOnGround()) {
            return;
        }
        remainingJumpTicks = ticks.getValue();
    }

    @Subscribe
    private void onBlockInteract(BlockInteractEvent.Post event) {
        if (isOutsideWorld() || !mode.isSelected(SHULKER_MODE)) {
            return;
        }
        BlockPos position = event.getHit().getBlockPos();
        if (!isShulkerBox(position) || !isPlayerNear(position)) {
            return;
        }
        pendingPlacement = position.toImmutable();
        placementTimeoutTicks = PLACEMENT_TIMEOUT;
    }

    @Subscribe
    private void onScreenChange(ScreenChangeEvent event) {
        if (!mode.isSelected(SHULKER_MODE) || !(event.getScreen() instanceof ShulkerBoxScreen)) {
            return;
        }
        if (pendingPlacement == null || placementTimeoutTicks <= 0) {
            return;
        }
        if (!isShulkerBox(pendingPlacement) || !isPlayerNear(pendingPlacement)) {
            return;
        }
        remainingJumpTicks = ticks.getValue();
        pendingPlacement = null;
        placementTimeoutTicks = 0;
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            clearJumpState();
            return;
        }
        if (placementTimeoutTicks > 0 && --placementTimeoutTicks == 0) {
            pendingPlacement = null;
        }
        if (remainingJumpTicks <= 0) {
            return;
        }
        if (client.player.hasVehicle() || client.player.isTouchingWater() || client.player.isInLava()) {
            remainingJumpTicks = 0;
            return;
        }
        Vec3d velocity = client.player.getVelocity();
        client.player.setVelocity(velocity.x, Math.max(velocity.y, lift.getValue()), velocity.z);
        client.player.setOnGround(false);
        remainingJumpTicks--;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            Class.forName(net.minecraft.util.math.BlockPos.class.getName(), true, net.minecraft.util.math.BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        pendingPlacement = null;
        placementTimeoutTicks = 0;
        remainingJumpTicks = 0;
    }

    private boolean isShulkerBox(BlockPos position) {
        return client.world.getBlockState(position).getBlock() instanceof ShulkerBoxBlock;
    }

    private boolean isPlayerNear(BlockPos position) {
        return client.player.getBoundingBox().intersects(new Box(position).expand(0.25));
    }

    private void clearJumpState() {
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        pendingPlacement = null;
        placementTimeoutTicks = 0;
        remainingJumpTicks = 0;
    }

    
    public static void appendBootstrapSettings(HighJump Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HighJump.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HighJump.lift);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HighJump.ticks);
    }

    
    public static void assignBootstrapSettings(HighJump Module, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode, dev.dov.hack.echo.client.settings.IntSetting preparedTicks, dev.dov.hack.echo.client.settings.FloatSetting preparedLift) {
        HighJump.mode = preparedMode;
        HighJump.ticks = preparedTicks;
        HighJump.lift = preparedLift;
    }
}
