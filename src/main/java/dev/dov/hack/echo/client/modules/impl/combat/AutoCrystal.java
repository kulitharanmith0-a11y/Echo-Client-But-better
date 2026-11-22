package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.AttackKeyEvent;
import dev.dov.hack.echo.client.event.input.UseKeyEvent;
import dev.dov.hack.echo.client.event.interaction.BlockBreakingEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class AutoCrystal extends Module {
    private static final double NEARBY_DEATH_RADIUS = 6.0;
    private static final CharSequence returnToOption3 = CatalogText.literal("Original");
    private static final CharSequence returnToOption = CatalogText.literal("Slot");
    private static final CharSequence returnToOption2 = CatalogText.literal("Item");
    private static FloatRangeSetting placeDelay;
    private static FloatRangeSetting breakDelay;
    private static IntSetting failChance;
    private static BooleanSetting stopOnKill;
    private static BooleanSetting fakePunch;
    private static BooleanSetting placeObsidian;
    private static FloatRangeSetting switchDelay;
    private static FloatRangeSetting placeObsidianDelay;
    private static BooleanSetting switchBack;
    private static ChoiceSetting returnTo;
    private static HotbarSlotsSetting returnSlots;
    private static ItemListSetting returnItems;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static KeybindSetting activateKey;
    private CooldownTimer placeTimer;
    private CooldownTimer breakTimer;
    private CrystalPlacementStage stage;
    private long stageReadyAtTick;
    private BlockPos pendingObsidianPosition;
    private boolean obsidianPlacementPending;
    private boolean lastActionWasPlacement;
    private boolean actionSentThisTick;
    private static String placeDelayTooltip;
    private static String breakDelayTooltip;
    private static String placeObsidianTooltip;
    private static String obsidianOptionsTooltip;

    public AutoCrystal() {
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Auto Crystal"), dev.dov.hack.echo.client.init.CatalogText.literal("Automatically places and detonates crystals"), dev.dov.hack.echo.client.modules.ModuleCategory.COMBAT));
    }

    public boolean isActivationKeyActive() {
        return isEnabled() && activateKey != null && !activateKey.isListeningForKey() && activateKey.getKey().isPressed();
    }

    @Override
    public void postInitialize() {
        placeTimer = new CooldownTimer();
        breakTimer = new CooldownTimer();
        stage = CrystalPlacementStage.NONE;
        lastActionWasPlacement = true;
        itemWhitelist.setEntrySelected(Items.END_CRYSTAL, true);
        placeObsidian.description(CatalogText.literal("Hit Crystal, places obsidian and switches to crystal when you press the activate key"));
        fakePunch.description(CatalogText.literal("Spam attack and use clicks while holding a crystal, without checking for crystals"));
        itemWhitelist.description(CatalogText.literal("Items you can hold to start the Hit Crystal sequence"));
        switchDelay.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        placeObsidianDelay.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        switchBack.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        returnTo.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue());
        returnSlots.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue() && returnTo.isSelected(returnToOption));
        returnItems.setVisibilityPredicate(ignored -> placeObsidian.getValue() && switchBack.getValue() && returnTo.isSelected(returnToOption2));
        itemWhitelist.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        hotbarWhitelist.setVisibilityPredicate(ignored -> placeObsidian.getValue());
        if (placeDelayTooltip == null) placeDelayTooltip = "Timing";
        placeDelay.tooltip(placeDelayTooltip);
        if (breakDelayTooltip == null) breakDelayTooltip = "Timing";
        breakDelay.tooltip(breakDelayTooltip);
        if (placeObsidianTooltip == null) placeObsidianTooltip = "Hit Crystal";
        placeObsidian.tooltip(placeObsidianTooltip);
        if (obsidianOptionsTooltip == null) obsidianOptionsTooltip = "Hit Crystal";
        switchDelay.tooltip(obsidianOptionsTooltip);
        placeObsidianDelay.tooltip(obsidianOptionsTooltip);
        switchBack.tooltip(obsidianOptionsTooltip);
        returnTo.tooltip(obsidianOptionsTooltip);
        returnSlots.tooltip(obsidianOptionsTooltip);
        returnItems.tooltip(obsidianOptionsTooltip);
        itemWhitelist.tooltip(obsidianOptionsTooltip);
        hotbarWhitelist.tooltip(obsidianOptionsTooltip);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        placeTimer.reset();
        breakTimer.reset();
        lastActionWasPlacement = true;
        actionSentThisTick = false;
        resetSequence(false);
    }

    @Override
    public void onDisable() {
        resetSequence(false);
        lastActionWasPlacement = true;
        actionSentThisTick = false;
        super.onDisable();
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        actionSentThisTick = false;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed()) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            lastActionWasPlacement = true;
            return;
        }
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            return;
        }
        updateSequence();
        if (shouldFakePunch()) {
            if (!breakTimer.hasElapsedTicks(sampleDelayTicks(breakDelay))) return;
            if (!lastActionWasPlacement && placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
            if (!rollFailure() && !actions().requestAttackClick(this, event)) return;
            breakTimer.reset();
            lastActionWasPlacement = false;
            actionSentThisTick = true;
            return;
        }
        if (event.hasClicks() && shouldSuppressMining()) event.setClickCount(0);
        if (stage != CrystalPlacementStage.NONE && stage != CrystalPlacementStage.CRYSTAL) return;
        if (!InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EndCrystalEntity crystal)
                || crystal.isRemoved() || !crystal.isAlive()) return;
        if (!breakTimer.hasElapsedTicks(sampleDelayTicks(breakDelay))) return;
        if (!event.hasClicks() && !rollFailure() && !actions().requestAttackClick(this, event)) return;
        breakTimer.reset();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed()) return;
        if (stopOnKill.getValue() && hasNearbyDeadPlayer(NEARBY_DEATH_RADIUS)) return;
        switch (stage) {
            case SWITCH_OBSIDIAN, SWITCH_CRYSTAL -> event.setClickCount(0);
            case PLACE_OBSIDIAN -> handleObsidianUse(event);
            case NONE, CRYSTAL -> handleCrystalUse(event);
        }
    }

    @Subscribe
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || activateKey.isListeningForKey()) return;
        if (!activateKey.getKey().isPressed() || !shouldSuppressMining()) return;
        if (client.interactionManager.isBreakingBlock()) client.interactionManager.cancelBlockBreaking();
        event.cancel();
    }

    private void updateSequence() {
        BlockState pointedState = getCrosshairBlockState();
        if (pointedState != null && pointedState.isOf(Blocks.RESPAWN_ANCHOR)) {
            if (stage != CrystalPlacementStage.NONE) resetSequence(switchBack.getValue());
            return;
        }
        if (stage == CrystalPlacementStage.NONE) {
            tryStartSequence();
            if (stage == CrystalPlacementStage.NONE) return;
        }
        CrystalPlacementStage previousStage = null;
        while (stage != previousStage) {
            previousStage = stage;
            switch (stage) {
                case SWITCH_OBSIDIAN -> {
                    if (client.player.age < stageReadyAtTick) return;
                    int slot = findObsidianSlot();
                    if (slot == -1 || !DeferredActionQueue.ensureSelected(this, slot, -1, false)) {
                        resetSequence(false);
                        return;
                    }
                    enterStage(CrystalPlacementStage.PLACE_OBSIDIAN, sampleDelayTicks(placeObsidianDelay));
                }
                case SWITCH_CRYSTAL -> {
                    if (client.player.age < stageReadyAtTick) return;
                    int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.END_CRYSTAL);
                    if (slot == -1 || !DeferredActionQueue.ensureSelected(this, slot, -1, false)) {
                        resetSequence(false);
                        return;
                    }
                    stage = CrystalPlacementStage.CRYSTAL;
                    placeTimer.reset();
                }
                default -> { }
            }
        }
    }

    private void tryStartSequence() {
        if (!placeObsidian.getValue()) return;
        if (itemWhitelist.getSelectedCount() != 0
                && !itemWhitelist.isEntrySelected(client.player.getMainHandStack().getItem())) return;
        if (!hotbarWhitelist.permitsCurrentSlot()) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos position = hit.getBlockPos();
        BlockState state = client.world.getBlockState(position);
        boolean hasCrystal = InventoryItems.containsItemInHotbar(item -> item == Items.END_CRYSTAL);
        if (isCrystalBase(state)) {
            if (hasCrystal) enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            return;
        }
        if (state.isAir() || !hasCrystal || findObsidianSlot() == -1) return;
        BlockPos placement = state.isReplaceable() ? position : position.offset(hit.getSide());
        BlockState placementState = client.world.getBlockState(placement);
        if (!placementState.isAir() && !placementState.isReplaceable()) return;
        enterStage(CrystalPlacementStage.SWITCH_OBSIDIAN, sampleDelayTicks(switchDelay));
    }

    private void handleObsidianUse(UseKeyEvent event) {
        if (event.getClickCount() > 1) event.setClickCount(1);
        if (client.player.age < stageReadyAtTick) {
            event.setClickCount(0);
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            event.setClickCount(0);
            return;
        }
        if (isCrystalBase(client.world.getBlockState(hit.getBlockPos()))) {
            enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            event.setClickCount(0);
            return;
        }
        if (obsidianPlacementPending) {
            if (pendingObsidianPosition != null && isCrystalBase(client.world.getBlockState(pendingObsidianPosition))) {
                enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
                event.setClickCount(0);
                return;
            }
            obsidianPlacementPending = false;
            stageReadyAtTick = client.player.age + Math.max(1, sampleDelayTicks(placeObsidianDelay));
            event.setClickCount(0);
            return;
        }
        Item held = client.player.getMainHandStack().getItem();
        if (held != Items.OBSIDIAN && held != Items.BEDROCK) {
            event.setClickCount(0);
            return;
        }
        BlockPos position = new ItemPlacementContext(client.player, Hand.MAIN_HAND, client.player.getMainHandStack(), hit).getBlockPos();
        if (isCrystalBase(client.world.getBlockState(position))) {
            enterStage(CrystalPlacementStage.SWITCH_CRYSTAL, sampleDelayTicks(switchDelay));
            event.setClickCount(0);
            return;
        }
        if (!event.hasClicks() && !actions().requestUseClick(this, event)) {
            return;
        }
        pendingObsidianPosition = position;
        obsidianPlacementPending = true;
    }

    private void handleCrystalUse(UseKeyEvent event) {
        if (shouldFakePunch()) {
            if (actionSentThisTick) return;
            if (!placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
            if (!rollFailure() && !actions().requestUseClick(this, event)) return;
            placeTimer.reset();
            lastActionWasPlacement = true;
            actionSentThisTick = true;
            return;
        }
        if (!InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos position = hit.getBlockPos();
        if (!isCrystalBase(client.world.getBlockState(position)) || hasCrystalEntityCollision(position)) return;
        if (!placeTimer.hasElapsedTicks(sampleDelayTicks(placeDelay))) return;
        if (!event.hasClicks() && !rollFailure() && !actions().requestUseClick(this, event)) return;
        placeTimer.reset();
    }

    private void enterStage(CrystalPlacementStage next, int ticks) {
        stage = next;
        stageReadyAtTick = client.player.age + ticks;
        pendingObsidianPosition = null;
        obsidianPlacementPending = false;
    }

    private void resetSequence(boolean restore) {
        int slot = restore ? resolveReturnSlot() : -1;
        if (slot == -1) {
            DeferredActionQueue.finish(this, restore);
        } else {
            DeferredActionQueue.finish(this, false);
            InventoryItems.selectHotbarSlot(slot);
        }
        stage = CrystalPlacementStage.NONE;
        stageReadyAtTick = 0L;
        pendingObsidianPosition = null;
        obsidianPlacementPending = false;
    }

    private int resolveReturnSlot() {
        if (returnTo.isSelected(returnToOption)) return returnSlots.chooseSelectedSlot();
        if (returnTo.isSelected(returnToOption2)) {
            return InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
        }
        return -1;
    }

    private boolean shouldFakePunch() {
        return fakePunch.getValue() && InventoryItems.holdsInEitherHand(Items.END_CRYSTAL)
                && (stage == CrystalPlacementStage.NONE || stage == CrystalPlacementStage.CRYSTAL);
    }

    private boolean rollFailure() {
        if (failChance.getValue() <= 0) return false;
        return Math.random() * 100.0 < failChance.getValue();
    }

    private static int sampleDelayTicks(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private BlockState getCrosshairBlockState() {
        if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return client.world.getBlockState(hit.getBlockPos());
        }
        return null;
    }

    private boolean shouldSuppressMining() {
        BlockState state = getCrosshairBlockState();
        return state != null && (stage != CrystalPlacementStage.NONE || isCrystalBase(state));
    }

    private int findObsidianSlot() {
        int obsidian = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.OBSIDIAN);
        return obsidian != -1 ? obsidian : InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.BEDROCK);
    }

    private static boolean isCrystalBase(BlockState state) {
        return state != null && (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK));
    }

    private static boolean hasCrystalEntityCollision(BlockPos base) {
        if (client.world == null) return false;
        BlockPos above = base.up();
        if (!client.world.isAir(above)) return false;
        double x = above.getX(), y = above.getY(), z = above.getZ();
        return !client.world.getOtherEntities(null, new Box(x, y, z, x + 1.0, y + 2.0, z + 1.0)).isEmpty();
    }

    private static boolean hasNearbyDeadPlayer(double range) {
        if (client.player == null || client.world == null) return false;
        double squaredRange = range * range;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && player.isDead() && client.player.squaredDistanceTo(player) < squaredRange) return true;
        }
        return false;
    }

    
    public static void appendBootstrapSettings(AutoCrystal Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.placeDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.breakDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.failChance);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.stopOnKill);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.fakePunch);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.placeObsidian);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.switchDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.placeObsidianDelay);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.switchBack);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.returnTo);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.returnSlots);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.returnItems);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.itemWhitelist);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.hotbarWhitelist);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AutoCrystal.activateKey);
    }

    public static java.lang.CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static java.lang.CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static java.lang.CharSequence bootstrapReturnToOption2() { return returnToOption2; }

    
    public static void assignBootstrapSettings(AutoCrystal Module, dev.dov.hack.echo.client.settings.FloatRangeSetting preparedPlaceDelay, dev.dov.hack.echo.client.settings.FloatRangeSetting preparedPlaceObsidianDelay, dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack, dev.dov.hack.echo.client.settings.BooleanSetting preparedFakePunch, dev.dov.hack.echo.client.settings.FloatRangeSetting preparedBreakDelay, dev.dov.hack.echo.client.settings.FloatRangeSetting preparedSwitchDelay, dev.dov.hack.echo.client.settings.BooleanSetting preparedPlaceObsidian, dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist, dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist, dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots, dev.dov.hack.echo.client.settings.IntSetting preparedFailChance, dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo, dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey, dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems, dev.dov.hack.echo.client.settings.BooleanSetting preparedStopOnKill) {
        AutoCrystal.placeDelay = preparedPlaceDelay;
        AutoCrystal.placeObsidianDelay = preparedPlaceObsidianDelay;
        AutoCrystal.switchBack = preparedSwitchBack;
        AutoCrystal.fakePunch = preparedFakePunch;
        AutoCrystal.breakDelay = preparedBreakDelay;
        AutoCrystal.switchDelay = preparedSwitchDelay;
        AutoCrystal.placeObsidian = preparedPlaceObsidian;
        AutoCrystal.hotbarWhitelist = preparedHotbarWhitelist;
        AutoCrystal.itemWhitelist = preparedItemWhitelist;
        AutoCrystal.returnSlots = preparedReturnSlots;
        AutoCrystal.failChance = preparedFailChance;
        AutoCrystal.returnTo = preparedReturnTo;
        AutoCrystal.activateKey = preparedActivateKey;
        AutoCrystal.returnItems = preparedReturnItems;
        AutoCrystal.stopOnKill = preparedStopOnKill;
    }
}
