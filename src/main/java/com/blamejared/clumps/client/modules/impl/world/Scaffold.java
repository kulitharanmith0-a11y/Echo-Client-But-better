package com.blamejared.clumps.client.modules.impl.world;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.event.interaction.BlockInteractEvent;
import com.blamejared.clumps.client.event.movement.MovementPacketEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.RegistryListSetting;
import com.blamejared.clumps.client.settings.specialized.ScaffoldBlockListSetting;
import com.blamejared.clumps.client.utils.placement.ScaffoldController;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

public class Scaffold extends Module {
    private static final CharSequence modeOption = CatalogText.literal("Legit");
    private static final CharSequence listTypeOption = CatalogText.literal("Whitelist");
    private static final CharSequence listTypeOption2 = CatalogText.literal("Blacklist");
    private static ChoiceSetting mode;
    private static BooleanSetting pitchCheck;
    private static ChoiceSetting listType;
    private static RegistryListSetting<Block> blocks =
        new ScaffoldBlockListSetting(CatalogText.literal("Blocks Selection"), () -> Registries.BLOCK);
    private ScaffoldController scaffoldController;
    private static String conditionsSection;
    private static String listTypeSection;
    private static String blocksSection;

    public Scaffold() {
        super(new ModuleInfo(CatalogText.literal("Scaffold"), CatalogText.literal("Module to assist with bridging"), ModuleCategory.WORLD));
    }

    @Override
    public void postInitialize() {
        scaffoldController = new ScaffoldController(this, new Predicate<Object>() {
            @Override public boolean test(Object ignored) { return mode.isSelected(modeOption); }
        });
        pitchCheck.description(CatalogText.literal("Only assist while looking down"));
        blocks.description(CatalogText.literal("Blocks the list applies to. An empty whitelist allows every block"));
        if (conditionsSection == null) conditionsSection = "Conditions";
        pitchCheck.tooltip(conditionsSection);
        if (listTypeSection == null) listTypeSection = "Blocks";
        listType.tooltip(listTypeSection);
        if (blocksSection == null) blocksSection = "Blocks";
        blocks.tooltip(blocksSection);
        settings.addAll(scaffoldController.getSettings());
    }

    @Override
    public boolean requiresMovementCorrection() {
        return true;
    }

    @Override
    public CharSequence getSuffix() {
        return mode.getValue();
    }

    public static boolean passesPitchCheck() {
        return !pitchCheck.getValue() || client.player.getPitch() > 0.0f;
    }

    public static boolean isAllowedStack(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!isSuitableBlock(blockItem.getBlock())) {
            return false;
        }
        boolean selected = blocks.isEntrySelected(blockItem.getBlock());
        if (listType.isSelected(listTypeOption2)) {
            return !selected;
        }
        return blocks.getSelectedCount() == 0 || selected;
    }

    public static boolean isSuitableBlock(Block block) {
        if (block instanceof FallingBlock) {
            return false;
        }
        BlockState state = block.getDefaultState();
        if (state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty()) {
            return false;
        }
        VoxelShape outline = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
        return Block.isFaceFullSquare(outline, Direction.DOWN)
            && outline.getMax(Direction.Axis.Y) >= 0.75;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName(ScaffoldController.class.getName(), true, ScaffoldController.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        ScaffoldController controller = scaffoldController;
        try {
            Class.forName(ScaffoldController.class.getName(), true, ScaffoldController.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        controller.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, false);
        try {
            Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        RotationManager rotations = rotations();
        try {
            Class.forName(RotationManager.class.getName(), true, RotationManager.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        rotations.release(this);
        super.onDisable();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (mode.isSelected(modeOption)) scaffoldController.onPlayerInput(event);
    }

    @Subscribe
    private void onMovementPacket(MovementPacketEvent.Post event) {
        if (mode.isSelected(modeOption)) scaffoldController.onMovementPacket(event);
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (mode.isSelected(modeOption)) scaffoldController.onMouseMovement(event);
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (mode.isSelected(modeOption)) scaffoldController.onHotbarKey(event);
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (mode.isSelected(modeOption)) scaffoldController.onUseKey(event);
    }

    @Subscribe
    private void onBlockInteract(BlockInteractEvent.Post event) {
        if (mode.isSelected(modeOption)) scaffoldController.onBlockInteract(event);
    }

    public static void appendBootstrapSettings(Scaffold Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, pitchCheck);
        CatalogSettings.append(Module, listType);
        CatalogSettings.append(Module, blocks);
    }

    public static CharSequence bootstrapModeOption() { return modeOption; }
    public static CharSequence bootstrapListTypeOption() { return listTypeOption; }
    public static CharSequence bootstrapListTypeOption2() { return listTypeOption2; }

    public static void assignBootstrapSettings(Scaffold Module, BooleanSetting preparedPitchCheck, ChoiceSetting preparedMode, ChoiceSetting preparedListType) {
        pitchCheck = preparedPitchCheck;
        mode = preparedMode;
        listType = preparedListType;
    }
}
