package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.input.KeybindingEvent;
import dev.dov.hack.echo.client.utils.targeting.AttackEvaluation;
import java.lang.invoke.MethodHandles;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class ItemUsageTickEvent extends Event {
    private final LivingEntity user;
    private final ItemStack stack;
    private final int remainingUseTicks;
    private final List<AttackEvaluation> attackEvaluations;

    
    public ItemUsageTickEvent(LivingEntity user, ItemStack stack, int remainingUseTicks, List<AttackEvaluation> attackEvaluations) {
        super();
        this.user = user;
        this.stack = stack;
        this.remainingUseTicks = remainingUseTicks;
        this.attackEvaluations = attackEvaluations;
    }

    
    public int getElapsedUseTicks() {
        initializePayloadType(ItemStack.class);
        ItemStack currentStack = stack;
        initializePayloadType(LivingEntity.class);
        LivingEntity currentUser = user;
        return currentStack.getMaxUseTime(currentUser) - remainingUseTicks;
    }

    
    public boolean hasAvailableAttack() {
        return attackEvaluations.stream().anyMatch(AttackEvaluation::isAvailable);
    }

    
    public boolean willDamage() {
        return attackEvaluations.stream().anyMatch(ItemUsageTickEvent::matchesDamage);
    }

    private static boolean matchesDamage(AttackEvaluation evaluation) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (sentinel == null && !evaluation.isAvailable()) {
            return false;
        }
        return evaluation.damage();
    }

    
    public LivingEntity getUser() {
        initializePayloadType(LivingEntity.class);
        return user;
    }

    
    public ItemStack getStack() {
        initializePayloadType(ItemStack.class);
        return stack;
    }

    
    public int getRemainingUseTicks() {
        return remainingUseTicks;
    }

    
    public List<AttackEvaluation> getAttackEvaluations() {
        return attackEvaluations;
    }

    private static void initializePayloadType(Class<?> type) {
        try {
            MethodHandles.lookup().ensureInitialized(type);
        } catch (IllegalAccessException inaccessibleType) {
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
    }
}
