package dev.dov.hack.echo.client.utils.targeting;

import static dev.dov.hack.echo.client.MinecraftAccess.CLIENT;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.modules.internals.Targets;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class TargetSelection implements MinecraftAccess {
    private static LivingEntity currentTarget;
    private static LivingEntity lastAttackedTarget;
    private static int lastAttackTick;

    private TargetSelection() {
    }

    
    public static void recordAttack(Entity entity) {
        if (CLIENT.player == null || !(entity instanceof LivingEntity living)) {
            return;
        }
        lastAttackedTarget = living;
        lastAttackTick = CLIENT.player.age;
        if (!isValidTarget(living)) {
            return;
        }
        currentTarget = living;
    }

    
    public static boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved()
                || entity.isDead() || entity.isSpectator() || CLIENT.player == null) {
            return false;
        }
        Targets targets = targets();
        if (targets == null) {
            return true;
        }
        MultiChoiceSetting types = Targets.entityTypes;
        boolean hasSelection = false;
        for (Boolean selected : types.getOptions().values()) {
            if (selected) {
                hasSelection = true;
                break;
            }
        }
        if (hasSelection && EntityPredicates.isUnarmored(entity) && !types.isSelected(Targets.NAKED)) {
            return false;
        }
        if (hasSelection && entity.isInvisible() && !types.isSelected(Targets.INVISIBLE)) {
            return false;
        }
        if (EntityPredicates.isPlayer(entity)) {
            return isPlayerAllowed(entity, targets, hasSelection);
        }
        if (entity instanceof ArmorStandEntity) {
            return !hasSelection || types.isSelected(Targets.ARMOR_STANDS);
        }
        if (EntityPredicates.isHostile(entity)) {
            return !hasSelection || types.isSelected(Targets.HOSTILE);
        }
        if (EntityPredicates.isPassive(entity)) {
            return !hasSelection || types.isSelected(Targets.PASSIVE);
        }
        if (EntityPredicates.isNeutral(entity)) {
            return !hasSelection || types.isSelected(Targets.NEUTRAL);
        }
        return !hasSelection;
    }

    
    public static LivingEntity resolveTarget(LivingEntity suggested) {
        if (!isKeepTargetEnabled()) {
            return suggested;
        }
        if (prefersLastAttacked()) {
            return resolveLastAttacked();
        }
        if (canKeepTarget(currentTarget)) {
            return currentTarget;
        }
        if (!canKeepTarget(suggested)) {
            currentTarget = null;
            return null;
        }
        currentTarget = suggested;
        return currentTarget;
    }

    
    public static LivingEntity findNearestTarget(Vec3d origin, float tickDelta) {
        if (origin == null || CLIENT.world == null || CLIENT.player == null) {
            return null;
        }
        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Entity entity : CLIENT.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == CLIENT.player || !isValidTarget(living)) {
                continue;
            }
            LivingEntity resolved = resolveTarget(living);
            if (resolved == null) {
                continue;
            }
            double distanceSquared = origin.squaredDistanceTo(resolved.getLerpedPos(tickDelta));
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = resolved;
            }
        }
        return nearest;
    }

    
    public static void selectTarget(LivingEntity entity) {
        if (canKeepTarget(entity)) {
            currentTarget = entity;
        }
    }

    public static void clearTarget() {
        currentTarget = null;
    }

    public static void clearTargets() {
        currentTarget = null;
        lastAttackedTarget = null;
        lastAttackTick = 0;
    }

    
    public static LivingEntity recentlyAttacked(int maximumAgeTicks) {
        if (CLIENT.player == null || !isLastAttackedValid()) {
            return null;
        }
        if (CLIENT.player.age - lastAttackTick > maximumAgeTicks) {
            return null;
        }
        return lastAttackedTarget;
    }

    public static boolean isBot(LivingEntity entity) {
        Targets targets = targets();
        return targets != null && isBot(entity, targets);
    }

    
    public static boolean hasDeadPlayerWithin(double distance) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return false;
        }
        double distanceSquared = distance * distance;
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player != CLIENT.player && player.isDead()
                    && CLIENT.player.squaredDistanceTo(player) < distanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity resolveLastAttacked() {
        LivingEntity attacked = validLastAttacked();
        if (attacked != null) {
            currentTarget = attacked;
        }
        if (canKeepTarget(currentTarget)) {
            return currentTarget;
        }
        currentTarget = null;
        return null;
    }

    private static LivingEntity validLastAttacked() {
        return isLastAttackedValid() ? lastAttackedTarget : null;
    }

    private static boolean isLastAttackedValid() {
        if (lastAttackedTarget == null || CLIENT.player == null
                || lastAttackedTarget.getEntityWorld() != CLIENT.world
                || !lastAttackedTarget.isAlive() || lastAttackedTarget.isRemoved()) {
            return false;
        }
        return isValidTarget(lastAttackedTarget);
    }

    private static boolean canKeepTarget(LivingEntity entity) {
        if (entity == null || CLIENT.player == null || !entity.isAlive() || entity.isRemoved()
                || entity.getEntityWorld() != CLIENT.world) {
            return false;
        }
        double range = keepTargetRange();
        if (CLIENT.player.squaredDistanceTo(entity) > range * range) {
            return false;
        }
        return isValidTarget(entity);
    }

    private static double keepTargetRange() {
        return targets() == null ? 16.0 : Targets.keepTargetRange.getValue();
    }

    private static boolean isKeepTargetEnabled() {
        return targets() != null && Targets.keepTarget.getValue();
    }

    private static boolean prefersLastAttacked() {
        return targets() != null && Targets.keepTarget.getValue() && Targets.lastAttacked.getValue();
    }

    private static boolean isPlayerAllowed(LivingEntity entity, Targets targets, boolean hasSelection) {
        if (hasSelection && !Targets.entityTypes.isSelected(Targets.PLAYERS)) {
            return false;
        }
        if (Targets.antiBot.getValue() && isBot(entity, targets)) {
            return false;
        }
        if (Targets.ignoreTeams.getValue() && targets.isSameTeam(entity)) {
            return false;
        }
        if (Targets.ignoreFriends.getValue()) {
            String name = entity.getName().getString();
            if (EchoClient.friendManager != null && EchoClient.friendManager.contains(name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBot(LivingEntity entity, Targets targets) {
        return entity instanceof PlayerEntity player && CachedPlayerTargets.isBot(player, targets);
    }

    private static Targets targets() {
        if (EchoClient.ModuleManager == null) {
            return null;
        }
        return (Targets) EchoClient.ModuleManager.findFeature(Targets.class);
    }
}
