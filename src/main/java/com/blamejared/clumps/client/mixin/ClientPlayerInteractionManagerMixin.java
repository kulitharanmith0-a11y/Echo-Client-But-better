package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.interaction.BlockInteractEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.interaction.HandInteractEvent;
import com.blamejared.clumps.client.event.movement.RotationContext;
import com.blamejared.clumps.client.event.movement.RotationEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.modules.impl.internals.Prevent;
import com.blamejared.clumps.client.modules.impl.world.NoBreakDelay;
import com.blamejared.clumps.client.utils.inventory.InventoryActionScheduler;
import com.blamejared.clumps.client.utils.inventory.SlotAction;
import com.blamejared.clumps.client.utils.inventory.SlotPackets;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = {"clickSlot", "clickSlot"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void protectOffhandTotem(int syncId, int slotId, int button, SlotActionType action, PlayerEntity player, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || ClumpsClient.ModuleManager == null) {
            return;
        }
        Prevent prevent = (Prevent) ClumpsClient.ModuleManager.findFeature(Prevent.class);
        if (prevent != null && prevent.isEnabled() && Prevent.untotem.getValue()
                && Prevent.inInventory.getValue() && !InventoryActionScheduler.isTransferring()) {
            boolean hasTotem = client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
            SlotAction slotAction = SlotPackets.fromEnum(action);
            if (slotId == 45 && hasTotem) {
                callback.cancel();
                return;
            } else if (slotAction == SlotAction.SWAP && button == 40 && hasTotem) {
                callback.cancel();
                return;
            }
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null && !coordinator.beforeClickSlot(syncId, slotId, button, action, player)) {
            callback.cancel();
        }
    }

    @Inject(method = {"clickSlot", "clickSlot"}, at = @At("RETURN"), require = 0)
    private void afterClickSlot(int syncId, int slotId, int button, SlotActionType action, PlayerEntity player, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.afterClickSlot(player);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void beforeItemInteraction(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (ClumpsClient.ModuleManager != null && !ClumpsClient.eventsDisabled) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null && !coordinator.beforeInteractItem(player, hand)) {
                callback.setReturnValue(ActionResult.PASS);
                return;
            }
            HandInteractEvent.Pre event = new HandInteractEvent.Pre(player, hand);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @Inject(method = "interactItem", at = @At("RETURN"))
    private void afterItemInteraction(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (!ClumpsClient.eventsDisabled) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null) {
                coordinator.afterInteractItem(player, callback.getReturnValue());
            }
        }
    }

    @ModifyArg(method = "interactItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V"), index = 1)
    private SequencedPacketCreator rotateItemInteractionPacket(SequencedPacketCreator original) {
        return sequence -> {
            var packet = original.predict(sequence);
            if (packet instanceof PlayerInteractItemC2SPacket interaction) {
                RotationEvent rotation = RotationEvent.dispatch(RotationContext.INTERACT, interaction.getYaw(), interaction.getPitch());
                return new PlayerInteractItemC2SPacket(interaction.getHand(), sequence, rotation.getYaw(), rotation.getPitch());
            }
            return packet;
        };
    }

    @Inject(method = "interactBlockInternal", at = @At("HEAD"), cancellable = true)
    private void beforeBlockInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> callback) {
        if (ClumpsClient.ModuleManager != null && !ClumpsClient.eventsDisabled) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null && !coordinator.beforeInteractBlock(player, hand, hit)) {
                callback.setReturnValue(ActionResult.PASS);
                return;
            }
            BlockInteractEvent.Pre event = new BlockInteractEvent.Pre(player, hand, hit);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @Inject(method = "interactBlockInternal", at = @At("RETURN"))
    private void afterBlockInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> callback) {
        if (ClumpsClient.ModuleManager != null && !ClumpsClient.eventsDisabled) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null) {
                coordinator.afterInteractBlock(player, callback.getReturnValue());
            }
            new BlockInteractEvent.Post(player, hand, hit).post();
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void beforeEntityAttack(PlayerEntity player, Entity target, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && ClumpsClient.ModuleManager != null) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null && !coordinator.beforeEntityAttack(player, target)) {
                callback.cancel();
                return;
            }
            EntityAttackEvent.Pre event = new EntityAttackEvent.Pre(player, target);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void afterEntityAttack(PlayerEntity player, Entity target, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.afterEntityAttack(player);
        }
        TargetSelection.recordAttack(target);
        if (ClumpsClient.ModuleManager != null) {
            new EntityAttackEvent.Post(player, target).post();
        }
    }

    @ModifyConstant(method = "updateBlockBreakingProgress", constant = @Constant(intValue = 5))
    public int removeBlockBreakingDelay(int original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        if (ClumpsClient.ModuleManager == null) {
            return original;
        }
        return ((NoBreakDelay) ClumpsClient.ModuleManager.findFeature(NoBreakDelay.class)).isEnabled() ? 0 : original;
    }
}
