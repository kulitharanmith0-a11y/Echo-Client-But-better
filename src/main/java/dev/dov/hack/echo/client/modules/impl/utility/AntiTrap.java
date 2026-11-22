package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.BlockBreakingEvent;
import dev.dov.hack.echo.client.event.interaction.BlockInteractEvent;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.modules.impl.utility.AntiTrapTargeting;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.RegistryListSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class AntiTrap extends Module {
    private static BooleanSetting noInteract;
    private static BooleanSetting armorStands;
    private static BooleanSetting itemFrames;
    private static BooleanSetting glowFrames;
    private static BooleanSetting paintings;
    private static BooleanSetting chestMinecarts;
    private static BooleanSetting signs;
    private static BooleanSetting hangingSigns;
    private static RegistryListSetting<Block> blockedBlocks =
            new AntiTrapBlockSelectionSetting(dev.dov.hack.echo.client.init.CatalogText.literal("Blocks"), () -> Registries.BLOCK);

    
    public AntiTrap() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Anti Trap"), dev.dov.hack.echo.client.init.CatalogText.literal("Helps you to not get trapped"), dev.dov.hack.echo.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        glowFrames.tooltip("Appearance");
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (!noInteract.getValue() || isOutsideWorld()) {
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            return;
        }
        if (!isBlockedBlock(client.world.getBlockState(hit.getBlockPos()))) {
            return;
        }
        HitResult replacement = AntiTrapTargeting.pickPastBlockedBlocks(client, this::isBlockedBlock, this::isBlockedEntityType);
        client.crosshairTarget = replacement;
        client.targetedEntity = replacement instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    @Subscribe
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (isCurrentBlockBlocked()) {
            event.cancel();
        }
    }

    @Subscribe
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isCurrentBlockBlocked()) {
            event.cancel();
        }
    }

    @Subscribe
    private void onBlockInteract(BlockInteractEvent.Pre event) {
        if (isBlockInteractionBlocked(event.getHit())) {
            event.cancel();
        }
    }

    @Subscribe
    private void onEntityAttack(EntityAttackEvent.Pre event) {
        if (isEntityInteractionBlocked(event.getTarget())) {
            event.cancel();
        }
    }

    private boolean isCurrentBlockBlocked() {
        if (!noInteract.getValue() || isOutsideWorld()) {
            return false;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            return false;
        }
        return isBlockInteractionBlocked(hit);
    }

    private boolean isBlockInteractionBlocked(BlockHitResult hit) {
        if (!noInteract.getValue() || isOutsideWorld() || hit == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return isBlockedBlock(client.world.getBlockState(hit.getBlockPos()));
    }

    private boolean isEntityInteractionBlocked(Entity entity) {
        if (!noInteract.getValue() || entity == null) {
            return false;
        }
        return isBlockedEntityType(entity.getType());
    }

    private boolean isBlockedEntityType(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        if (armorStands.getValue() && type == EntityType.ARMOR_STAND) {
            return true;
        }
        if (itemFrames.getValue() && type == EntityType.ITEM_FRAME) {
            return true;
        }
        if (glowFrames.getValue() && type == EntityType.GLOW_ITEM_FRAME) {
            return true;
        }
        if (paintings.getValue() && type == EntityType.PAINTING) {
            return true;
        }
        return chestMinecarts.getValue() && type == EntityType.CHEST_MINECART;
    }

    
    public static boolean shouldIgnoreEntity(Entity entity) {
        if (EchoClient.eventsDisabled || EchoClient.ModuleManager == null) {
            return false;
        }
        AntiTrap Module = (AntiTrap) EchoClient.ModuleManager.findFeature(AntiTrap.class);
        if (Module == null || !Module.isEnabled() || !noInteract.getValue()) {
            return false;
        }
        return entity != null && Module.isBlockedEntityType(entity.getType());
    }

    private boolean isBlockedBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (signs.getValue() && (state.isIn(BlockTags.STANDING_SIGNS) || state.isIn(BlockTags.WALL_SIGNS))) {
            return true;
        }
        if (hangingSigns.getValue()
                && (state.isIn(BlockTags.CEILING_HANGING_SIGNS) || state.isIn(BlockTags.WALL_HANGING_SIGNS))) {
            return true;
        }
        return blockedBlocks.isEntrySelected(state.getBlock());
    }

    
    public static void appendBootstrapSettings(AntiTrap Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.noInteract);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.armorStands);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.itemFrames);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.glowFrames);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.paintings);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.chestMinecarts);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.signs);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.hangingSigns);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AntiTrap.blockedBlocks);
    }

    
    public static void assignBootstrapSettings(AntiTrap Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedSigns, dev.dov.hack.echo.client.settings.BooleanSetting preparedHangingSigns, dev.dov.hack.echo.client.settings.BooleanSetting preparedGlowFrames, dev.dov.hack.echo.client.settings.BooleanSetting preparedPaintings, dev.dov.hack.echo.client.settings.BooleanSetting preparedChestMinecarts, dev.dov.hack.echo.client.settings.BooleanSetting preparedArmorStands, dev.dov.hack.echo.client.settings.BooleanSetting preparedNoInteract, dev.dov.hack.echo.client.settings.BooleanSetting preparedItemFrames) {
        AntiTrap.signs = preparedSigns;
        AntiTrap.hangingSigns = preparedHangingSigns;
        AntiTrap.glowFrames = preparedGlowFrames;
        AntiTrap.paintings = preparedPaintings;
        AntiTrap.chestMinecarts = preparedChestMinecarts;
        AntiTrap.armorStands = preparedArmorStands;
        AntiTrap.noInteract = preparedNoInteract;
        AntiTrap.itemFrames = preparedItemFrames;
    }
}
