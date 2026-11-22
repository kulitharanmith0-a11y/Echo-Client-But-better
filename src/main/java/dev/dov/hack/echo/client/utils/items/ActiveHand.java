package dev.dov.hack.echo.client.utils.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public final class ActiveHand {
    private ActiveHand() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    
    public static void swingHand(LivingEntity entity, Hand hand) {
        entity.swingHand(hand);
    }
}
