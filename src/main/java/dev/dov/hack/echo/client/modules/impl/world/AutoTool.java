package dev.dov.hack.echo.client.modules.impl.world;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {
    private static BooleanSetting switchBack;
    private static BooleanSetting requireClick;
    private static BooleanSetting requireShift;
    private int previousSlot;

    public AutoTool() {
        super(new ModuleInfo(CatalogText.literal("Auto Tool"), CatalogText.literal("Automatically selects best tool"), ModuleCategory.WORLD));
    }

    @Override
    public void postInitialize() {
        previousSlot = -1;
        super.postInitialize();
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.crosshairTarget instanceof BlockHitResult hit
                && !client.world.getBlockState(hit.getBlockPos()).isAir()
                && (!requireClick.getValue() || client.options.attackKey.isPressed())
                && (!requireShift.getValue() || client.options.sneakKey.isPressed())) {
            int bestSlot = findBestToolSlot(hit.getBlockPos());
            if (bestSlot != -1) {
                if (previousSlot == -1) {
                    previousSlot = client.player.getInventory().getSelectedSlot();
                }
                actions().executeSlotSelect(this, bestSlot);
            } else if (switchBack.getValue() && previousSlot != -1) {
                if (actions().executeSlotSelect(this, previousSlot).succeeded()) {
                    previousSlot = -1;
                }
            }
        } else if (switchBack.getValue() && previousSlot != -1) {
            if (actions().executeSlotSelect(this, previousSlot).succeeded()) {
                previousSlot = -1;
            }
        }
    }

    private int findBestToolSlot(BlockPos position) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = miningSpeed(stack, position);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private float miningSpeed(ItemStack stack, BlockPos position) {
        if (stack.isEmpty()) {
            return 0.0f;
        }
        float speed = stack.getMiningSpeedMultiplier(client.world.getBlockState(position));
        if (speed > 1.0f) {
            var efficiency = client.world.getRegistryManager().getOptionalEntry(Enchantments.EFFICIENCY);
            if (efficiency.isPresent()) {
                int level = EnchantmentHelper.getLevel(efficiency.get(), stack);
                if (level > 0) {
                    speed += level * level + 1;
                }
            }
        }
        return speed;
    }

    public static void appendBootstrapSettings(AutoTool Module) {
        CatalogSettings.append(Module, switchBack);
        CatalogSettings.append(Module, requireClick);
        CatalogSettings.append(Module, requireShift);
    }

    public static void assignBootstrapSettings(AutoTool Module, BooleanSetting preparedRequireClick, BooleanSetting preparedSwitchBack, BooleanSetting preparedRequireShift) {
        requireClick = preparedRequireClick;
        switchBack = preparedSwitchBack;
        requireShift = preparedRequireShift;
    }
}
