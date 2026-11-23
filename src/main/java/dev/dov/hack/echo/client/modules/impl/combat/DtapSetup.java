package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.AttackKeyEvent;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.input.UseKeyEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.ItemUseEvent;
import dev.dov.hack.echo.client.handlers.LatencyTracker;
import dev.dov.hack.echo.client.handlers.action.ActionCoordinator;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.MathRng;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.placement.BlockPlacement;
import dev.dov.hack.echo.client.utils.placement.PlacementWorldQueries;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class DtapSetup extends Module {
    private static final long RECENT_OBSIDIAN_WINDOW_MS = 500L;
    private static final int DAMAGE_IFRAME_TICKS = 10;
    private static final int MAX_TICK_WAIT_TICKS = 12;
    private static final double NEARBY_DEATH_RADIUS = 6.0;
    private static final double CRYSTAL_TARGET_RADIUS_SQ = 8.0 * 8.0;

    private static KeybindSetting activateKey;
    private static BooleanSetting checkPlace;
    private static FloatRangeSetting switchDelay;
    private static IntSetting switchChance;
    private static FloatRangeSetting placeDelay;
    private static FloatRangeSetting breakDelay;
    private static IntSetting placeChance;
    private static BooleanSetting autoCrystalAction;
    private static BooleanSetting tickTiming;
    private static BooleanSetting stopOnKill;
    private static BooleanSetting fakePunch;
    private static ItemListSetting itemWhitelist;

    private final CooldownTimer switchTimer = new CooldownTimer();
    private final CooldownTimer placeTimer = new CooldownTimer();
    private final CooldownTimer breakTimer = new CooldownTimer();
    private final CooldownTimer fakeBreakTimer = new CooldownTimer();
    private final CooldownTimer fakePlaceTimer = new CooldownTimer();

    private long sampledSwitchDelayMs;
    private long sampledPlaceDelayMs;
    private long sampledBreakDelayMs;
    private long lastObsidianPlaceTime;
    private BlockPos lastObsidianSupportPos;
    private BlockPos lastObsidianPlacedPos;
    private boolean obsidianPlacementPending;

    // Tick-timing state (tracks the 10-tick damage immunity cycle across crystal breaks)
    private long lastCrystalBreakTick = -100L;
    private int tickTimingTargetId = -1;
    private boolean sawHurtAfterBreak;

    private boolean activateKeyWasPressed;
    private boolean active;
    private boolean crystalling;
    private boolean lastActionWasPlacement = true;
    private boolean actionSentThisTick;

    public DtapSetup() {
        super(new ModuleInfo(
                CatalogText.literal("Dtap Setup"),
                CatalogText.literal("Automatically places obsidian and swaps to end crystals when pressing your crystal bind"),
                ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        itemWhitelist.setEntrySelected(Items.END_CRYSTAL, true);
        itemWhitelist.setEntrySelected(Items.OBSIDIAN, true);
        itemWhitelist.setEntrySelected(Items.NETHERITE_SWORD, true);
        itemWhitelist.setEntrySelected(Items.DIAMOND_SWORD, true);
        itemWhitelist.setEntrySelected(Items.NETHERITE_AXE, true);
        itemWhitelist.setEntrySelected(Items.DIAMOND_AXE, true);
        itemWhitelist.setEntrySelected(Items.MACE, true);
        activateKey.description(CatalogText.literal("Your crystal keybind to start obsidian placement and crystal swap"));
        checkPlace.description(CatalogText.literal("Checks if the target block has open air above it with no blocking entities before switching"));
        switchDelay.description(CatalogText.literal("Delay in milliseconds before switching hotbar slots"));
        placeDelay.description(CatalogText.literal("Delay in milliseconds between placing obsidian or crystals"));
        breakDelay.description(CatalogText.literal("Delay in milliseconds before breaking a placed crystal"));
        placeChance.description(CatalogText.literal("Randomization chance for placing obsidian or crystals"));
        autoCrystalAction.description(CatalogText.literal("Place and break crystals automatically if Auto Crystal is not already active on this keybind"));
        tickTiming.description(CatalogText.literal("Places the next crystal immediately after a break, then waits for the enemy's 10-tick hurt-time to clear before detonating"));
        stopOnKill.description(CatalogText.literal("Stops placing and detonating crystals when a nearby player dies"));
        fakePunch.description(CatalogText.literal("Spam attack and use clicks while holding a crystal, without checking for crystals"));
        itemWhitelist.description(CatalogText.literal("Items you can hold to start the Dtap Setup sequence"));
        resampleDelays();
    }

    @Override
    public void onEnable() {
        resetState();
        clearObsidianTracking();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetState();
        clearObsidianTracking();
        super.onDisable();
    }

    @Subscribe
    private void onInputPre(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || client.player.isUsingItem()
                || activateKey.isListeningForKey()) {
            return;
        }

        updateObsidianPendingState();
        updateTickTimingObservation();

        boolean switchReady = switchTimer.hasElapsedMillis((double) sampledSwitchDelayMs);
        boolean placeReady = placeTimer.hasElapsedMillis((double) sampledPlaceDelayMs);
        boolean breakReady = breakTimer.hasElapsedMillis((double) sampledBreakDelayMs);

        boolean isActivatePressed = activateKey.getKey() != InputKey.NONE && activateKey.getKey().isPressed();
        boolean isRmbPressed = InputKey.MOUSE_RIGHT.isPressed();
        boolean isHoldingCrystal = client.player.isHolding(Items.END_CRYSTAL);
        boolean mainHandCrystal = client.player.getMainHandStack().isOf(Items.END_CRYSTAL);
        boolean mainHandObsidian = client.player.getMainHandStack().isOf(Items.OBSIDIAN);

        boolean justPressed = isActivatePressed && !activateKeyWasPressed;
        activateKeyWasPressed = isActivatePressed;

        if (justPressed && crystalling && !mainHandCrystal
                && client.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hit.getBlockPos();
            BlockPos placementPos = resolveObsidianPlacementPos(hit);
            boolean isCrystalBase = BlockPlacement.isEndCrystalBase(client.world.getBlockState(targetPos));
            boolean isSameRecentObi = isRecentOrPendingObsidian(targetPos, placementPos);
            if (!isCrystalBase && !isSameRecentObi) {
                crystalling = false;
                active = false;
                switchTimer.reset();
                placeTimer.reset();
                resampleDelays();
            }
        }

        boolean shouldRun = isActivatePressed
                || (crystalling && isRmbPressed && (isHoldingCrystal || mainHandObsidian));
        if (!shouldRun) {
            resetState();
            return;
        }
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) {
            resetState();
            return;
        }

        // System Client Check Place: verify space above the targeted block is air and free of non-item entities
        if (!mainHandCrystal
                && client.crosshairTarget instanceof BlockHitResult hitResult
                && hitResult.getType() == HitResult.Type.BLOCK
                && !active && checkPlace.getValue()
                && !canPlaceBlockClient(hitResult.getBlockPos())) {
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        if (!isAllowedTriggerItem(mainHand)) {
            resetState();
            return;
        }

        active = true;

        // Never switch to or place obsidian if already holding an end crystal in hand.
        if (mainHandCrystal) {
            crystalling = true;
        }

        if (!crystalling) {
            if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                BlockPos placementPos = resolveObsidianPlacementPos(hit);
                boolean targetIsBase = BlockPlacement.isEndCrystalBase(client.world.getBlockState(hitPos));
                boolean placementIsBase = placementPos != null
                        && BlockPlacement.isEndCrystalBase(client.world.getBlockState(placementPos));

                if (!targetIsBase && !placementIsBase && !isRecentOrPendingObsidian(hitPos, placementPos)) {
                    if (PlacementWorldQueries.isChargedRespawnAnchor(hitPos)) {
                        return;
                    }

                    resetKeys();

                    boolean switchedThisTick = false;
                    if (!client.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
                        if (!switchReady || isCoordinatorBusy()) {
                            return;
                        }
                        if (rollPercent(switchChance.getValue())) {
                            int obiSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.OBSIDIAN));
                            if (obiSlot == -1) {
                                return;
                            }
                            if (actions().executeSlotSelect(this, obiSlot).succeeded()) {
                                switchTimer.reset();
                                sampledSwitchDelayMs = sampleDelayMs(switchDelay);
                                switchedThisTick = true;
                            }
                        }
                    }

                    if (switchedThisTick && sampledPlaceDelayMs > 0L) {
                        return;
                    }

                    if (client.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
                        if (!placeReady || isCoordinatorBusy()) {
                            return;
                        }
                        if (placementPos != null && (BlockPlacement.intersectsPlayer(placementPos)
                                || BlockPlacement.hasBlockingEntity(placementPos))) {
                            return;
                        }
                        if (rollPercent(placeChance.getValue())) {
                            if (actions().executeBlockUse(this, Hand.MAIN_HAND, hit, true).succeeded()) {
                                lastObsidianPlaceTime = System.currentTimeMillis();
                                lastObsidianSupportPos = hitPos.toImmutable();
                                lastObsidianPlacedPos = placementPos != null ? placementPos.toImmutable() : hitPos.toImmutable();
                                obsidianPlacementPending = true;
                                placeTimer.reset();
                                sampledPlaceDelayMs = sampleDelayMs(placeDelay);
                                crystalling = true;
                                resetKeys();
                                switchTimer.reset();
                                sampledSwitchDelayMs = sampleDelayMs(switchDelay);
                                return;
                            }
                        }
                    }
                } else {
                    resetKeys();
                    crystalling = true;
                }
            }
        }

        if (crystalling) {
            if (!client.player.isHolding(Items.END_CRYSTAL)) {
                if (client.player.getMainHandStack().isOf(Items.OBSIDIAN)) {
                    resetKeys();
                }
                if (!switchReady || isCoordinatorBusy()) {
                    return;
                }
                if (rollPercent(switchChance.getValue())) {
                    int crystalSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.END_CRYSTAL));
                    if (crystalSlot == -1) {
                        return;
                    }
                    if (actions().executeSlotSelect(this, crystalSlot).succeeded()) {
                        switchTimer.reset();
                        sampledSwitchDelayMs = sampleDelayMs(switchDelay);
                    }
                }
            }

            if (client.player.isHolding(Items.END_CRYSTAL) && autoCrystalAction.getValue()
                    && !isEchoAutoCrystalActive() && !shouldFakePunch()) {
                handleCrystalFallback(placeReady, breakReady);
            }
        }
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        actionSentThisTick = false;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) {
            return;
        }
        if (!isSessionPressed()) {
            lastActionWasPlacement = true;
            return;
        }
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) {
            resetState();
            return;
        }
        if (!shouldFakePunch()) {
            return;
        }
        if (!fakeBreakTimer.hasElapsedMillis((double) sampledBreakDelayMs)) {
            return;
        }
        if (!lastActionWasPlacement && fakePlaceTimer.hasElapsedMillis((double) sampledPlaceDelayMs)) {
            return;
        }
        if (tickTiming.getValue() && shouldWaitForTickTiming(client.player.getEntityPos())) {
            return;
        }
        if (rollPercent(placeChance.getValue()) && !actions().requestAttackClick(this, event)) {
            return;
        }
        recordCrystalBreak(client.player.getEntityPos());
        fakeBreakTimer.reset();
        sampledBreakDelayMs = sampleDelayMs(breakDelay);
        lastActionWasPlacement = false;
        actionSentThisTick = true;
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) {
            return;
        }
        if (!isSessionPressed()) {
            return;
        }
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) {
            return;
        }
        if (!shouldFakePunch()) {
            return;
        }
        if (actionSentThisTick) {
            return;
        }
        if (!fakePlaceTimer.hasElapsedMillis((double) sampledPlaceDelayMs)) {
            return;
        }
        if (rollPercent(placeChance.getValue()) && !actions().requestUseClick(this, event)) {
            return;
        }
        fakePlaceTimer.reset();
        sampledPlaceDelayMs = sampleDelayMs(placeDelay);
        lastActionWasPlacement = true;
        actionSentThisTick = true;
    }

    private void handleCrystalFallback(boolean placeReady, boolean breakReady) {
        if (isCoordinatorBusy()) {
            return;
        }
        if (client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof EndCrystalEntity crystal
                && !crystal.isRemoved() && crystal.isAlive()) {
            if (!breakReady) {
                return;
            }
            if (tickTiming.getValue() && shouldWaitForTickTiming(crystal.getEntityPos())) {
                return;
            }
            if (actions().executeEntityAttack(this, crystal, true).succeeded()) {
                recordCrystalBreak(crystal.getEntityPos());
                breakTimer.reset();
                sampledBreakDelayMs = sampleDelayMs(breakDelay);
            }
            return;
        }
        if (!placeReady || !(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!BlockPlacement.isEndCrystalBase(client.world.getBlockState(pos))
                || BlockPlacement.hasEntityInAirAbove(pos)) {
            return;
        }
        if (rollPercent(placeChance.getValue())
                && actions().executeBlockUse(this, Hand.MAIN_HAND, hit, true).succeeded()) {
            placeTimer.reset();
            sampledPlaceDelayMs = sampleDelayMs(placeDelay);
        }
    }

    @Subscribe
    private void onItemUse(ItemUseEvent.Pre event) {
        if (client.player == null || event.getPlayer() != client.player) {
            return;
        }
        ActionCoordinator coordinator = actions();
        if (coordinator != null && (coordinator.isAutomatedActionActive() || coordinator.syntheticUseOwner() == this)) {
            return;
        }
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.isOf(Items.OBSIDIAN)) {
            if (active || crystalling || obsidianPlacementPending
                    || (System.currentTimeMillis() - lastObsidianPlaceTime < RECENT_OBSIDIAN_WINDOW_MS)) {
                event.cancel();
            }
        } else if (mainHand.isOf(Items.END_CRYSTAL)) {
            if ((active || crystalling) && !isEchoAutoCrystalActive()) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (client.player == null) {
            return;
        }
        ActionCoordinator coordinator = actions();
        if (coordinator != null && (coordinator.isAutomatedActionActive() || coordinator.syntheticAttackOwner() == this)) {
            return;
        }
        if (client.player.getMainHandStack().isOf(Items.END_CRYSTAL)
                && !InputKey.MOUSE_LEFT.isPressed()
                && !isEchoAutoCrystalActive()) {
            event.cancel();
        }
    }

    /**
     * Exact System Client check-place predicate: verifies {@code block.up()} is air
     * and contains no entities other than dropped {@link ItemEntity}.
     */
    private boolean canPlaceBlockClient(BlockPos block) {
        if (client.world == null || block == null) {
            return false;
        }
        BlockPos up = block.up();
        if (!client.world.isAir(up)) {
            return false;
        }
        List<Entity> entities = client.world.getOtherEntities(null, new Box(up));
        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity)) {
                return false;
            }
        }
        return true;
    }

    private void updateTickTimingObservation() {
        if (tickTimingTargetId < 0 || client.world == null || client.player == null) {
            return;
        }
        long ticksSinceBreak = client.player.age - lastCrystalBreakTick;
        if (ticksSinceBreak > MAX_TICK_WAIT_TICKS || ticksSinceBreak < 0L) {
            tickTimingTargetId = -1;
            sawHurtAfterBreak = false;
            return;
        }
        if (client.world.getEntityById(tickTimingTargetId) instanceof PlayerEntity enemy
                && enemy.isAlive() && enemy.hurtTime > 0) {
            sawHurtAfterBreak = true;
        }
    }

    private void recordCrystalBreak(Vec3d center) {
        if (client.player == null) {
            return;
        }
        lastCrystalBreakTick = client.player.age;
        PlayerEntity target = findNearestEnemy(center);
        tickTimingTargetId = target == null ? -1 : target.getId();
        sawHurtAfterBreak = target != null && target.hurtTime > 0;
    }

    private boolean shouldWaitForTickTiming(Vec3d center) {
        if (client.player == null || client.world == null) {
            return false;
        }
        PlayerEntity target = null;
        if (tickTimingTargetId >= 0
                && client.world.getEntityById(tickTimingTargetId) instanceof PlayerEntity tracked
                && tracked.isAlive() && !tracked.isRemoved()
                && tracked.squaredDistanceTo(center) <= CRYSTAL_TARGET_RADIUS_SQ) {
            target = tracked;
        }
        if (target == null) {
            target = findNearestEnemy(center);
        }
        if (target == null) {
            return false;
        }
        if (target.hurtTime > 0 && target.getId() == tickTimingTargetId) {
            sawHurtAfterBreak = true;
        }
        long ticksSinceBreak = client.player.age - lastCrystalBreakTick;
        return shouldHoldBreakForTickTiming(
                target.hurtTime,
                ticksSinceBreak,
                sawHurtAfterBreak,
                LatencyTracker.milliseconds());
    }

    /**
     * Pure damage-tick timing decision for D-tap crystal detonation:
     * waits through the server's 10-tick damage immunity window (including ping arrival
     * of the first crystal's hurtTime update) so the second crystal detonates on the exact
     * tick the enemy becomes vulnerable again.
     */
    public static boolean shouldHoldBreakForTickTiming(
            int enemyHurtTime,
            long ticksSinceLastBreak,
            boolean sawHurtAfterBreak,
            int pingMillis) {
        int rttTicks = Math.max(0, Math.min(4, Math.round(Math.max(0, pingMillis) / 50.0f)));
        int halfRttLead = Math.max(0, Math.min(2, Math.round((Math.max(0, pingMillis) * 0.5f) / 50.0f)));

        // If enemy is visibly in hurtTime, wait until hurtTime drops into our half-RTT send lead.
        if (enemyHurtTime > halfRttLead) {
            return true;
        }

        // Right after breaking Crystal #1, wait for the server's hurtTime packet to arrive
        // (or through the 10-tick i-frame if hurtTime was already confirmed).
        if (ticksSinceLastBreak >= 0L && ticksSinceLastBreak < MAX_TICK_WAIT_TICKS) {
            int hurtArrivalWindow = Math.min(4, rttTicks + 2);
            if (!sawHurtAfterBreak && ticksSinceLastBreak < hurtArrivalWindow) {
                return true;
            }
            if (sawHurtAfterBreak && ticksSinceLastBreak < DAMAGE_IFRAME_TICKS - halfRttLead) {
                return true;
            }
        }
        return false;
    }

    private boolean isSessionPressed() {
        boolean isActivatePressed = activateKey.getKey() != InputKey.NONE && activateKey.getKey().isPressed();
        if (isActivatePressed) {
            return true;
        }
        boolean isRmbPressed = InputKey.MOUSE_RIGHT.isPressed();
        return crystalling && isRmbPressed
                && (client.player.isHolding(Items.END_CRYSTAL) || client.player.isHolding(Items.OBSIDIAN));
    }

    private boolean shouldFakePunch() {
        return fakePunch.getValue()
                && (crystalling || active)
                && InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)
                && !isEchoAutoCrystalActive();
    }

    private static boolean hasNearbyDeadPlayer(double range) {
        if (client.player == null || client.world == null) {
            return false;
        }
        double squaredRange = range * range;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && player.isDead() && client.player.squaredDistanceTo(player) < squaredRange) {
                return true;
            }
        }
        return false;
    }

    private PlayerEntity findNearestEnemy(Vec3d center) {
        if (client.player == null || client.world == null || center == null) {
            return null;
        }
        PlayerEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (EntityInteractions.isFriend(player.getName().getString())) {
                continue;
            }
            double distSq = player.squaredDistanceTo(center);
            if (distSq <= CRYSTAL_TARGET_RADIUS_SQ && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = player;
            }
        }
        return best;
    }

    private boolean isCoordinatorBusy() {
        ActionCoordinator coordinator = actions();
        return coordinator != null && (coordinator.isActionSent() || coordinator.isSlotPending() || coordinator.isMovementSent());
    }

    private boolean isEchoAutoCrystalActive() {
        if (EchoClient.ModuleManager == null) {
            return false;
        }
        AutoCrystal autoCrystal = (AutoCrystal) EchoClient.ModuleManager.findFeature(AutoCrystal.class);
        return autoCrystal != null && autoCrystal.isActivationKeyActive();
    }

    private boolean isAllowedTriggerItem(ItemStack stack) {
        if ((active || crystalling) && (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.OBSIDIAN))) {
            return true;
        }
        if (itemWhitelist != null && itemWhitelist.getSelectedCount() > 0) {
            return itemWhitelist.isEntrySelected(stack.getItem());
        }
        if (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.OBSIDIAN) || stack.getItem() instanceof MaceItem) {
            return true;
        }
        return InventoryItems.isLegacySword(stack.getItem())
                || stack.isOf(Items.COPPER_SWORD)
                || InventoryItems.isAxe(stack)
                || InventoryItems.isSpear(stack.getItem());
    }

    private void resampleDelays() {
        sampledSwitchDelayMs = sampleDelayMs(switchDelay);
        sampledPlaceDelayMs = sampleDelayMs(placeDelay);
        sampledBreakDelayMs = sampleDelayMs(breakDelay);
    }

    private static long sampleDelayMs(FloatRangeSetting setting) {
        if (setting == null) {
            return 0L;
        }
        return Math.max(0L, Math.round(setting.sampleGaussian()));
    }

    private void updateObsidianPendingState() {
        if (!obsidianPlacementPending) {
            return;
        }
        if (lastObsidianPlacedPos != null && client.world != null
                && BlockPlacement.isEndCrystalBase(client.world.getBlockState(lastObsidianPlacedPos))) {
            obsidianPlacementPending = false;
            return;
        }
        if (System.currentTimeMillis() - lastObsidianPlaceTime >= RECENT_OBSIDIAN_WINDOW_MS) {
            obsidianPlacementPending = false;
        }
    }

    private BlockPos resolveObsidianPlacementPos(BlockHitResult hit) {
        if (hit == null || client.world == null) {
            return null;
        }
        BlockPos hitPos = hit.getBlockPos();
        if (client.world.getBlockState(hitPos).isReplaceable()) {
            return hitPos;
        }
        return hitPos.offset(hit.getSide());
    }

    private boolean isRecentOrPendingObsidian(BlockPos hitPos, BlockPos placementPos) {
        boolean withinWindow = obsidianPlacementPending
                || (System.currentTimeMillis() - lastObsidianPlaceTime < RECENT_OBSIDIAN_WINDOW_MS);
        if (!withinWindow) {
            return false;
        }
        if (lastObsidianPlacedPos != null
                && (lastObsidianPlacedPos.equals(hitPos) || lastObsidianPlacedPos.equals(placementPos))) {
            return true;
        }
        return lastObsidianSupportPos != null
                && (lastObsidianSupportPos.equals(hitPos) || lastObsidianSupportPos.equals(placementPos));
    }

    private void clearObsidianTracking() {
        lastObsidianPlaceTime = 0L;
        lastObsidianSupportPos = null;
        lastObsidianPlacedPos = null;
        obsidianPlacementPending = false;
    }

    private void resetState() {
        switchTimer.reset();
        placeTimer.reset();
        breakTimer.reset();
        fakeBreakTimer.reset();
        fakePlaceTimer.reset();
        resampleDelays();
        active = false;
        crystalling = false;
        activateKeyWasPressed = false;
        lastActionWasPlacement = true;
        actionSentThisTick = false;
        lastCrystalBreakTick = -100L;
        tickTimingTargetId = -1;
        sawHurtAfterBreak = false;
    }

    private void resetKeys() {
        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            client.interactionManager.cancelBlockBreaking();
        }
        if (client.options == null) {
            return;
        }
        if (client.options.hotbarKeys != null) {
            for (KeyBinding hotbarKey : client.options.hotbarKeys) {
                while (hotbarKey.wasPressed()) { }
            }
        }
        if (client.options.useKey != null) {
            while (client.options.useKey.wasPressed()) { }
            client.options.useKey.setPressed(false);
        }
    }

    private static boolean rollPercent(int chance) {
        if (chance >= 100) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return MathRng.randomInt(1, 101) <= chance;
    }

    public static void assignBootstrapSettings(
            DtapSetup module,
            KeybindSetting preparedActivateKey,
            BooleanSetting preparedCheckPlace,
            FloatRangeSetting preparedSwitchDelay,
            IntSetting preparedSwitchChance,
            FloatRangeSetting preparedPlaceDelay,
            FloatRangeSetting preparedBreakDelay,
            IntSetting preparedPlaceChance,
            BooleanSetting preparedAutoCrystalAction,
            BooleanSetting preparedTickTiming,
            BooleanSetting preparedStopOnKill,
            BooleanSetting preparedFakePunch,
            ItemListSetting preparedItemWhitelist) {
        activateKey = preparedActivateKey;
        checkPlace = preparedCheckPlace;
        switchDelay = preparedSwitchDelay;
        switchChance = preparedSwitchChance;
        placeDelay = preparedPlaceDelay;
        breakDelay = preparedBreakDelay;
        placeChance = preparedPlaceChance;
        autoCrystalAction = preparedAutoCrystalAction;
        tickTiming = preparedTickTiming;
        stopOnKill = preparedStopOnKill;
        fakePunch = preparedFakePunch;
        itemWhitelist = preparedItemWhitelist;
    }

    public static void appendBootstrapSettings(DtapSetup module) {
        CatalogSettings.append(module, activateKey);
        CatalogSettings.append(module, checkPlace);
        CatalogSettings.append(module, switchDelay);
        CatalogSettings.append(module, switchChance);
        CatalogSettings.append(module, placeDelay);
        CatalogSettings.append(module, breakDelay);
        CatalogSettings.append(module, placeChance);
        CatalogSettings.append(module, autoCrystalAction);
        CatalogSettings.append(module, tickTiming);
        CatalogSettings.append(module, stopOnKill);
        CatalogSettings.append(module, fakePunch);
        CatalogSettings.append(module, itemWhitelist);
    }
}
