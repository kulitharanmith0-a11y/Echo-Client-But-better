package com.blamejared.clumps.client.modules.impl.movement;

import java.util.function.BooleanSupplier;

public record DiveBombAction(Runnable action, BooleanSupplier ready) {
}
