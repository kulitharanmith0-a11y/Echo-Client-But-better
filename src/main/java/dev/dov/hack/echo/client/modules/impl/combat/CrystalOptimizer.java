package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.utils.strings.SegmentedText;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class CrystalOptimizer extends Module {
    private EndCrystalEntity trackedCrystal;

    public CrystalOptimizer() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Crystal Optimizer"), dev.dov.hack.echo.client.init.CatalogText.literal("Optimizes crystal speeds"), dev.dov.hack.echo.client.modules.ModuleCategory.COMBAT));
    }

    @Subscribe
    public void onAttackAttempt(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EndCrystalEntity crystal)) {
            return;
        }
        StatusEffectInstance weakness = client.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = client.player.getStatusEffect(StatusEffects.STRENGTH);
        boolean canBreakCrystal = weakness == null
                || strength != null && strength.getAmplifier() > weakness.getAmplifier()
                || InventoryItems.holdsPickaxe() || InventoryItems.holdsSword();
        if (canBreakCrystal) {
            trackedCrystal = crystal;
        }
    }

    @Subscribe
    private void onInputHandled(HandleInputEvent.Post event) {
        if (trackedCrystal == null) {
            return;
        }
        if (!trackedCrystal.isRemoved()) {
            trackedCrystal.setRemoved(Entity.RemovalReason.KILLED);
            trackedCrystal.onRemoved();
        }
        trackedCrystal = null;
    }

    @Override
    public void onDisable() {
        trackedCrystal = null;
        super.onDisable();
    }

}
