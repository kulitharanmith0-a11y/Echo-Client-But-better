package com.blamejared.clumps.client.modules.impl.world;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.RegistryListSetting;
import com.blamejared.clumps.client.settings.list.RegistryEntryGroup;
import com.blamejared.clumps.client.utils.blocks.BlockGroups;
import com.blamejared.clumps.client.utils.strings.Concat;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class GhostHand extends Module {
    private static final CharSequence MODE_ALWAYS = CatalogText.literal("Always");
    private static final CharSequence MODE_HOLD_KEY = CatalogText.literal("Hold Key");
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();

    private static ChoiceSetting mode;
    private static KeybindSetting holdKey;
    private static BooleanSetting cobwebs;
    private static BooleanSetting playersOnly;
    private static ItemListSetting itemWhitelist;
    private static final RegistryListSetting<Block> targetBlocks =
            new GhostHandBlockListSetting(CatalogText.literal("Blocks"), () -> Registries.BLOCK);

    public GhostHand() {
        super(new ModuleInfo(
                CatalogText.literal("Ghost Hand"),
                CatalogText.literal("Break or interact with cobwebs and selected blocks through players"),
                ModuleCategory.WORLD));
    }

    @Override
    public void postInitialize() {
        mode.description(CatalogText.literal("Always active when aiming at selected blocks, or only while holding Hold Key"));
        holdKey.description(CatalogText.literal("Hold this key to target selected blocks through players"));
        holdKey.setVisibilityPredicate(ignored -> mode.isSelected(MODE_HOLD_KEY));
        cobwebs.description(CatalogText.literal("Always include Cobwebs without needing to add them to Blocks"));
        targetBlocks.description(CatalogText.literal("Additional blocks you can break or interact with through players"));
        playersOnly.description(CatalogText.literal("Only phase through players (turn off to pass through all entities)"));
        itemWhitelist.description(CatalogText.literal("Only active while holding a whitelisted item (leave empty for any item)"));

        mode.tooltip("Activation");
        holdKey.tooltip("Activation");
        cobwebs.tooltip("Blocks");
        targetBlocks.tooltip("Blocks");
        playersOnly.tooltip("Targeting");
        itemWhitelist.tooltip("Conditions");
    }

    @Override
    public CharSequence getSuffix() {
        return mode != null && mode.isSelected(MODE_HOLD_KEY) ? MODE_HOLD_KEY : EMPTY_SUFFIX;
    }

    public static boolean shouldIgnoreEntity(Entity entity) {
        GhostHand module = findActiveInstance();
        if (module == null || entity == null || entity == module.client.player) {
            return false;
        }
        if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) {
            return false;
        }
        return module.hasMatchingBlockInSight();
    }

    public static boolean isTargetingGhostBlock() {
        GhostHand module = findActiveInstance();
        if (module == null) {
            return false;
        }
        if (!(module.client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return module.isTargetBlock(module.client.world.getBlockState(hit.getBlockPos()));
    }

    private static GhostHand findActiveInstance() {
        if (ClumpsClient.eventsDisabled || ClumpsClient.ModuleManager == null) {
            return null;
        }
        Module found = ClumpsClient.ModuleManager.findFeature(GhostHand.class);
        if (!(found instanceof GhostHand module) || !module.isEnabled()) {
            return null;
        }
        if (module.isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return null;
        }
        if (mode == null || cobwebs == null || playersOnly == null || itemWhitelist == null) {
            return null;
        }
        if (mode.isSelected(MODE_HOLD_KEY)) {
            InputKey key = holdKey != null ? holdKey.getKey() : InputKey.NONE;
            if (key == null || key == InputKey.NONE || !key.isPressed()) {
                return null;
            }
        }
        if (!itemWhitelist.acceptsHeldItem()) {
            return null;
        }
        return module;
    }

    private boolean hasMatchingBlockInSight() {
        if (client.player == null || client.world == null) {
            return false;
        }
        double reach = Math.max(0.0, client.player.getBlockInteractionRange());
        if (reach <= 0.0) {
            return false;
        }
        float tickProgress = client.getRenderTickCounter() != null
                ? client.getRenderTickCounter().getTickProgress(false)
                : 1.0F;
        Vec3d start = client.player.getCameraPosVec(tickProgress);
        Vec3d end = start.add(client.player.getRotationVec(tickProgress).multiply(reach));
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                client.player));
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return isTargetBlock(client.world.getBlockState(hit.getBlockPos()));
    }

    private boolean isTargetBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (cobwebs.getValue() && block == Blocks.COBWEB) {
            return true;
        }
        return targetBlocks.isEntrySelected(block);
    }

    public static CharSequence bootstrapModeAlways() {
        return MODE_ALWAYS;
    }

    public static CharSequence bootstrapModeHoldKey() {
        return MODE_HOLD_KEY;
    }

    public static void appendBootstrapSettings(GhostHand module) {
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, holdKey);
        CatalogSettings.append(module, cobwebs);
        CatalogSettings.append(module, targetBlocks);
        CatalogSettings.append(module, playersOnly);
        CatalogSettings.append(module, itemWhitelist);
    }

    public static void assignBootstrapSettings(
            GhostHand module,
            ChoiceSetting preparedMode,
            KeybindSetting preparedHoldKey,
            BooleanSetting preparedCobwebs,
            BooleanSetting preparedPlayersOnly,
            ItemListSetting preparedItemWhitelist) {
        mode = preparedMode;
        holdKey = preparedHoldKey;
        cobwebs = preparedCobwebs;
        playersOnly = preparedPlayersOnly;
        itemWhitelist = preparedItemWhitelist;
    }

    private static final class GhostHandBlockListSetting extends RegistryListSetting<Block> {
        GhostHandBlockListSetting(CharSequence label, Supplier<Registry<Block>> registry) {
            super(label, registry);
        }

        @Override
        public List<RegistryEntryGroup<Block>> getGroups() {
            return BlockGroups.copyGroups();
        }

        @Override
        public Function<Block, String> getEntryLabel() {
            return block -> new ItemStack(block.asItem()).getName().getString();
        }
    }
}
