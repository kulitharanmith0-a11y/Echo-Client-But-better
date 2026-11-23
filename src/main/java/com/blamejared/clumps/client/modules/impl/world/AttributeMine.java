package com.blamejared.clumps.client.modules.impl.world;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public class AttributeMine extends Module {
    private BlockPos lastTarget;
    private int pendingToolSlot;
    private int switchDelay;

    public AttributeMine() {
        super(new ModuleInfo(CatalogText.literal("Attribute Mine"), CatalogText.literal("Technique to mine faster"), ModuleCategory.WORLD));
    }

    @Override
    public void postInitialize() {
        lastTarget = null;
        pendingToolSlot = -1;
        switchDelay = 0;
        super.postInitialize();
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (pendingToolSlot != -1) {
            --switchDelay;
            if (switchDelay <= 0) {
                if (actions().executeSlotSelect(this, pendingToolSlot).succeeded()) {
                    ((ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
                    pendingToolSlot = -1;
                }
            }
            return;
        }
        if (!client.options.attackKey.isPressed()) {
            lastTarget = null;
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            lastTarget = null;
            return;
        }
        BlockPos position = hit.getBlockPos();
        BlockState state = client.world.getBlockState(position);
        if (state.isAir()) {
            lastTarget = null;
            return;
        }
        if (position.equals(lastTarget)) return;
        lastTarget = position;
        int efficiencySlot = findHighestEfficiencySlot();
        int fastestSlot = findFastestToolSlot(state);
        if (efficiencySlot == -1) return;
        if (!actions().executeSlotSelect(this, efficiencySlot).succeeded()) return;
        ((ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
        if (fastestSlot != -1 && fastestSlot != efficiencySlot) {
            pendingToolSlot = fastestSlot;
            switchDelay = 2;
        }
    }

    private int findHighestEfficiencySlot() {
        PlayerInventory inventory = client.player.getInventory();
        int bestSlot = -1;
        int bestLevel = 0;
        var efficiency = client.world.getRegistryManager().getOptionalEntry(Enchantments.EFFICIENCY);
        if (efficiency.isEmpty()) return -1;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            int level = EnchantmentHelper.getLevel(efficiency.get(), stack);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private int findFastestToolSlot(BlockState state) {
        PlayerInventory inventory = client.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
