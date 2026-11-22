package dev.dov.hack.echo.client.modules.impl.movement;

import java.util.function.BooleanSupplier;

public record DiveBombAction(Runnable action, BooleanSupplier ready) {
}
