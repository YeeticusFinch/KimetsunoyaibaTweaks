package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class DemonTargetingHelper {
    private static final int PLAYER_RETARGET_INTERVAL = 10;
    private static final double DEFAULT_TARGET_RANGE = 32.0D;

    private DemonTargetingHelper() {
    }

    public static boolean retargetToCloserNonDemonPlayer(Mob demon, Predicate<LivingEntity> targetPredicate) {
        if (demon == null || demon.level().isClientSide() || demon.tickCount % PLAYER_RETARGET_INTERVAL != 0) {
            return false;
        }

        Player nearestPlayer = findNearestNonDemonPlayer(demon, getTargetSearchRange(demon), targetPredicate);
        if (nearestPlayer == null || demon.getTarget() == nearestPlayer) {
            return false;
        }

        LivingEntity currentTarget = demon.getTarget();
        double playerDistance = demon.distanceToSqr(nearestPlayer);
        if (!isValidNonDemonTarget(demon, currentTarget, targetPredicate)
            || playerDistance < demon.distanceToSqr(currentTarget)) {
            demon.setTarget(nearestPlayer);
            demon.setLastHurtByMob(nearestPlayer);
            return true;
        }

        return false;
    }

    public static boolean shouldKeepCloserExistingNonDemonTarget(
        Mob demon,
        LivingEntity currentTarget,
        LivingEntity candidateTarget,
        Predicate<LivingEntity> targetPredicate
    ) {
        if (!isValidNonDemonTarget(demon, currentTarget, targetPredicate) || candidateTarget == null || !candidateTarget.isAlive()) {
            return false;
        }
        return demon.distanceToSqr(currentTarget) <= demon.distanceToSqr(candidateTarget);
    }

    public static boolean isTargetableNonDemonPlayer(Mob demon, Player player, Predicate<LivingEntity> targetPredicate) {
        return player != null
            && player.isAlive()
            && !player.isSpectator()
            && !player.isCreative()
            && !Damager.isDemon(player)
            && isValidNonDemonTarget(demon, player, targetPredicate);
    }

    public static boolean isValidNonDemonTarget(Mob demon, LivingEntity target, Predicate<LivingEntity> targetPredicate) {
        return target != null
            && target != demon
            && target.isAlive()
            && !Damager.isDemon(target)
            && (targetPredicate == null || targetPredicate.test(target));
    }

    public static double getTargetSearchRange(Mob demon) {
        return Math.max(DEFAULT_TARGET_RANGE, demon.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    private static Player findNearestNonDemonPlayer(Mob demon, double range, Predicate<LivingEntity> targetPredicate) {
        AABB searchBox = demon.getBoundingBox().inflate(range);
        List<Player> players = demon.level().getEntitiesOfClass(
            Player.class,
            searchBox,
            player -> isTargetableNonDemonPlayer(demon, player, targetPredicate)
                && demon.distanceToSqr(player) <= range * range
        );

        return players.stream()
            .min(Comparator.comparingDouble(demon::distanceToSqr))
            .orElse(null);
    }
}
