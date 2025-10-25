package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.GhostlyCloneEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

/**
 * Event handler for Mist Breathing 7th Form aggro confusion.
 * When a player uses the 7th form, nearby mobs:
 * 1. Lose aggro on the player
 * 2. Pathfind toward random ghostly clones
 * 3. Make confused attack animations at the clones
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class MistBreathing7thFormHandler {

    private static final Random RANDOM = new Random();

    /**
     * Continuously confuse mobs near players using 7th form
     * This runs periodically for performance
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (mob.level().isClientSide) {
            return;
        }

        // Only run every 10 ticks (0.5 seconds) for performance
        if (mob.tickCount % 10 != 0) {
            return;
        }

        LivingEntity currentTarget = mob.getTarget();

        // Check if current target is using Mist Breathing 7th Form
        if (currentTarget != null && currentTarget.getPersistentData().getBoolean("mist_7th_teleport_active")) {
            // DE-AGGRO from the player
            mob.setTarget(null);

            // Find nearby ghostly clones
            Level level = mob.level();
            AABB searchArea = new AABB(mob.position(), mob.position()).inflate(25.0);
            List<GhostlyCloneEntity> clones = level.getEntitiesOfClass(GhostlyCloneEntity.class, searchArea,
                clone -> clone.isAlive() && clone.getOpacity() > 0.2f); // Only target visible clones

            if (!clones.isEmpty()) {
                // Pick a random clone to pathfind toward
                GhostlyCloneEntity targetClone = clones.get(RANDOM.nextInt(clones.size()));

                // Make mob pathfind to the clone's position
                Vec3 clonePos = targetClone.position();
                mob.getNavigation().moveTo(clonePos.x, clonePos.y, clonePos.z, 1.0); // Speed 1.0 (normal)

                // Store which clone we're confused by
                mob.getPersistentData().putInt("confused_by_clone", targetClone.getId());
                mob.getPersistentData().putInt("confusion_timer", 40); // Stay confused for 2 seconds
            }
        }

        // Handle confused state - make random attacks at air near clones
        if (mob.getPersistentData().contains("confusion_timer")) {
            int confusionTimer = mob.getPersistentData().getInt("confusion_timer");

            if (confusionTimer > 0) {
                mob.getPersistentData().putInt("confusion_timer", confusionTimer - 1);

                // Find the clone we're confused by
                int cloneId = mob.getPersistentData().getInt("confused_by_clone");
                GhostlyCloneEntity targetClone = null;

                for (GhostlyCloneEntity clone : mob.level().getEntitiesOfClass(GhostlyCloneEntity.class,
                    new AABB(mob.position(), mob.position()).inflate(25.0))) {
                    if (clone.getId() == cloneId) {
                        targetClone = clone;
                        break;
                    }
                }

                if (targetClone != null) {
                    // Continue pathfinding to clone
                    Vec3 clonePos = targetClone.position();

                    // If close enough, make confused attack animations
                    if (mob.distanceToSqr(targetClone) < 9.0) { // Within 3 blocks
                        // Make the mob "swing" at the air
                        if (RANDOM.nextFloat() < 0.1f) { // 10% chance per tick
                            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

                            // Play attack sound if available
                            mob.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
                        }

                        // Look at the clone
                        mob.getLookControl().setLookAt(clonePos.x, clonePos.y + targetClone.getEyeHeight(), clonePos.z);

                        // Occasionally change to a different clone (confusion effect)
                        if (RANDOM.nextFloat() < 0.05f) { // 5% chance
                            List<GhostlyCloneEntity> otherClones = mob.level().getEntitiesOfClass(GhostlyCloneEntity.class,
                                new AABB(mob.position(), mob.position()).inflate(25.0),
                                clone -> clone.isAlive() && clone.getOpacity() > 0.2f && clone.getId() != cloneId);

                            if (!otherClones.isEmpty()) {
                                GhostlyCloneEntity newTarget = otherClones.get(RANDOM.nextInt(otherClones.size()));
                                mob.getPersistentData().putInt("confused_by_clone", newTarget.getId());
                                mob.getNavigation().moveTo(newTarget.getX(), newTarget.getY(), newTarget.getZ(), 1.0);
                            }
                        }
                    } else {
                        // Still approaching - continue pathfinding
                        if (RANDOM.nextFloat() < 0.2f) { // Update path occasionally
                            mob.getNavigation().moveTo(clonePos.x, clonePos.y, clonePos.z, 1.0);
                        }
                    }
                } else {
                    // Clone disappeared (faded out or despawned) - find a new one
                    List<GhostlyCloneEntity> clones = mob.level().getEntitiesOfClass(GhostlyCloneEntity.class,
                        new AABB(mob.position(), mob.position()).inflate(25.0),
                        clone -> clone.isAlive() && clone.getOpacity() > 0.2f);

                    if (!clones.isEmpty()) {
                        GhostlyCloneEntity newTarget = clones.get(RANDOM.nextInt(clones.size()));
                        mob.getPersistentData().putInt("confused_by_clone", newTarget.getId());
                        mob.getNavigation().moveTo(newTarget.getX(), newTarget.getY(), newTarget.getZ(), 1.0);
                    }
                }
            } else {
                // Confusion ended - clean up
                mob.getPersistentData().remove("confusion_timer");
                mob.getPersistentData().remove("confused_by_clone");
                mob.getNavigation().stop();
            }
        }
    }
}
