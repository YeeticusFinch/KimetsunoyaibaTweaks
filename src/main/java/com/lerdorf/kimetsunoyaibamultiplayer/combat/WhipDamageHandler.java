package com.lerdorf.kimetsunoyaibamultiplayer.combat;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.client.WhipPhysics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Handles damage detection along Mitsuri's whip curve.
 *
 * Performs hit detection by sampling points along the whip and checking
 * for entities in range. Prevents multi-hit within the same attack.
 */
public class WhipDamageHandler {

    // Track which entities have been hit this attack (per player)
    private static final Map<UUID, Set<UUID>> hitEntitiesThisAttack = new HashMap<>();

    // Track when an attack started (for timeout)
    private static final Map<UUID, Long> attackStartTime = new HashMap<>();

    private static final long ATTACK_TIMEOUT_MS = 2000; // 2 seconds

    /**
     * Start a new attack for a player.
     * Clears the hit list so entities can be damaged again.
     */
    public static void startAttack(Player player) {
        hitEntitiesThisAttack.put(player.getUUID(), new HashSet<>());
        attackStartTime.put(player.getUUID(), System.currentTimeMillis());
    }

    /**
     * Get all entities currently touching the whip.
     */
    public static List<LivingEntity> getWhipTargets(Player player, double hitboxRadius) {
        Level level = player.level();
        List<Vec3> points = WhipPhysics.getPoints(player);

        Set<LivingEntity> targets = new HashSet<>();

        // Sample points along whip and check for entities
        for (Vec3 point : points) {
            AABB hitbox = new AABB(point, point).inflate(hitboxRadius);

            List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                hitbox,
                entity -> entity != player && entity.isAlive()
            );

            targets.addAll(nearby);
        }

        return new ArrayList<>(targets);
    }

    /**
     * Apply damage to all entities touching the whip.
     * Only damages each entity once per attack.
     *
     * @return Number of entities damaged
     */
    public static int applyWhipDamage(Player player, float damage, double hitboxRadius) {
        // Clean up old attacks
        cleanupOldAttacks();

        Set<UUID> hitThisAttack = hitEntitiesThisAttack.get(player.getUUID());
        if (hitThisAttack == null) {
            // No active attack, start one
            startAttack(player);
            hitThisAttack = hitEntitiesThisAttack.get(player.getUUID());
        }

        List<LivingEntity> targets = getWhipTargets(player, hitboxRadius);
        int damagedCount = 0;

        for (LivingEntity target : targets) {
            UUID targetId = target.getUUID();

            // Check if already hit this attack
            if (!hitThisAttack.contains(targetId)) {
                // Deal damage
                Damager.hurt(player, target, damage);
                hitThisAttack.add(targetId);
                damagedCount++;
            }
        }

        return damagedCount;
    }

    /**
     * Apply damage with custom source attribution (for entities).
     * For entities, uses a simpler radial sweep instead of whip physics.
     */
    public static int applyWhipDamage(LivingEntity attacker, float damage, double hitboxRadius) {
        if (attacker instanceof Player player) {
            return applyWhipDamage(player, damage, hitboxRadius);
        }

        // For non-player entities (like KanrojiEntity), use radial sweep
        return applyRadialWhipDamage(attacker, damage, hitboxRadius);
    }

    /**
     * Apply whip damage in a radial sweep for entities.
     * Uses a simple arc-based hitbox instead of complex whip physics.
     */
    private static int applyRadialWhipDamage(LivingEntity attacker, float damage, double range) {
        Level level = attacker.level();

        // Get entities in range
        AABB searchBox = attacker.getBoundingBox().inflate(range);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != attacker && entity.isAlive()
        );

        int damagedCount = 0;
        Vec3 attackerPos = attacker.position().add(0, attacker.getBbHeight() / 2, 0);
        Vec3 attackerLook = attacker.getLookAngle();

        for (LivingEntity target : targets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 toTarget = targetPos.subtract(attackerPos).normalize();

            // Check if target is in front (120 degree arc)
            double dotProduct = toTarget.dot(attackerLook);
            if (dotProduct > -0.5) { // cos(120°) ≈ -0.5
                // Deal damage
                Damager.hurt(attacker, target, damage);
                damagedCount++;
            }
        }

        return damagedCount;
    }

    /**
     * Check if a specific entity is touching the whip.
     */
    public static boolean isEntityTouchingWhip(Player player, LivingEntity target, double hitboxRadius) {
        List<Vec3> points = WhipPhysics.getPoints(player);

        for (Vec3 point : points) {
            if (target.position().distanceTo(point) <= hitboxRadius + target.getBbWidth() / 2) {
                return true;
            }
        }

        return false;
    }

    /**
     * End an attack for a player.
     * Clears the hit list after a delay to allow combo continuation.
     */
    public static void endAttack(Player player) {
        // Don't immediately clear - allow short window for combo continuation
        // The cleanup will handle it after timeout
    }

    /**
     * Clean up old attack data.
     */
    private static void cleanupOldAttacks() {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Long> entry : attackStartTime.entrySet()) {
            if (now - entry.getValue() > ATTACK_TIMEOUT_MS) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID uuid : toRemove) {
            hitEntitiesThisAttack.remove(uuid);
            attackStartTime.remove(uuid);
        }
    }

    /**
     * Clear all tracking data (for world reload).
     */
    public static void clearAll() {
        hitEntitiesThisAttack.clear();
        attackStartTime.clear();
    }

    /**
     * Clear tracking for a specific player (disconnect).
     */
    public static void cleanup(UUID playerUUID) {
        hitEntitiesThisAttack.remove(playerUUID);
        attackStartTime.remove(playerUUID);
    }
}
