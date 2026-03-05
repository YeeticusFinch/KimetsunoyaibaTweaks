package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BaseModBreathingExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import com.lerdorf.kimetsunoyaibamultiplayer.util.AttackDamageHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public class BreathingSwordSwingPacket {
    private final String animationName;

    public BreathingSwordSwingPacket() {
        this.animationName = "";
    }

    public BreathingSwordSwingPacket(String animationName) {
        this.animationName = animationName != null ? animationName : "";
    }

    public BreathingSwordSwingPacket(FriendlyByteBuf buf) {
        this.animationName = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.animationName);
    }

    private static final float DEFAULT_BOX_SIZE = 5f;
    private static final float KANROJI_BOX_SIZE = 10f; // Increased range for Kanroji's whip sword
    private static final float LOVE_BOX_SIZE = 8f;     // Love sword has slightly less range than Kanroji
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean isCustomSword = heldItem.getItem() instanceof BreathingSwordItem;
            boolean isBaseModNichirinSword = BaseModBreathingExecutor.isBaseModNichirinSword(heldItem.getItem());
            if (!isCustomSword && !isBaseModNichirinSword) return;

            // Base-mod training swords should only do normal melee damage, never form execution side effects.
            if (isBaseModNichirinSword && TrainingSwordHelper.isTrainingSword(heldItem)) {
                return;
            }

            // Check if player has cool_time effect from KnY mod (prevents attacks)
            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (Config.logDebug) {
                    Log.debug("Sword swing blocked by cool_time effect for player {}", player.getName().getString());
                }
                return;
            }

            // Broadcast slash rendering to nearby players so left-click slashes are visible in multiplayer.
            broadcastSlashToNearbyPlayers(player, animationName);

            // Base mod swords only need slash sync; AOE/combat state override is custom-sword behavior.
            if (!isCustomSword) {
                return;
            }

            // Set weak defensive power for basic sword swing (lasts 10 ticks)
            // This allows basic swings to clash with and mitigate enemy attacks
            double weakDefense = 3.0; // Weak defensive power (3 damage reduction)
            GuardStateHelper.setWeakAttackState(player, weakDefense);

            // Schedule clearing the weak attack state after 10 ticks (0.5 seconds)
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler.scheduleOnce(
                player,
                () -> GuardStateHelper.clearGuardState(player),
                10
            );

            // Determine box size based on sword type
            boolean isKanrojiSword = heldItem.getItem() instanceof NichirinSwordKanrojiAnimated;
            boolean isLoveSword = heldItem.getItem() instanceof NichirinSwordLoveAnimated;
            float boxSize = isKanrojiSword ? KANROJI_BOX_SIZE : isLoveSword ? LOVE_BOX_SIZE : DEFAULT_BOX_SIZE;

            // Perform AOE
            Vec3 attackerPos = player.position().add(0, player.getEyeHeight(), 0);
            Vec3 lookVec = player.getLookAngle().normalize();
            Vec3 frontPos = attackerPos.add(lookVec.scale(boxSize/1.5f));

            AABB attackBox = new AABB(frontPos.add(-boxSize/2, -boxSize/2, -boxSize/2), frontPos.add(boxSize/2, boxSize/2, boxSize/2));

            List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class, attackBox,
                e -> e != player && e.isAlive()
            );

            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            boolean isOffhandSwing = animationName.equals("left_sword_to_left")
                    || animationName.equals("left_sword_to_right")
                    || animationName.equals("left_sword_overhead");
            boolean isDoubleOverheadSwing = animationName.equals("double_sword_overhead");

            float damage;
            if (isDoubleOverheadSwing) {
                damage = AttackDamageHelper.getAverageDualWieldDamage(player);
            } else if (isOffhandSwing) {
                damage = AttackDamageHelper.getAttackDamageForHand(player, InteractionHand.OFF_HAND);
            } else {
                damage = AttackDamageHelper.getAttackDamageForHand(player, InteractionHand.MAIN_HAND);
            }
            for (LivingEntity target : targets) {
                // Use smart targeting system to prevent friendly fire
                if (!EnhancedLoveForms.isTargetable(player, target)) {
                    continue; // Skip non-targetable entities
                }

                Damager.hurt(player, target, damage);

                // Spawn love_slash particle on hit for Kanroji/Love sword
                if ((isKanrojiSword || isLoveSword) && player.level() instanceof ServerLevel serverLevel) {
                    double particleX = target.getX() + target.getBbHeight() * (Math.random() - 0.5);
                    double particleY = target.getY() + target.getBbHeight() * Math.random();
                    double particleZ = target.getZ() + target.getBbHeight() * (Math.random() - 0.5);
                    serverLevel.sendParticles(
                        ModParticles.LOVE_SLASH.get(),
                        particleX, particleY, particleZ,
                        1, 0, 0, 0, 0
                    );
                }
            }

            if (Config.logDebug && !targets.isEmpty()) {
                Log.debug("AOE attack hit {} entities (boxSize={}, isKanroji={})",
                    targets.size(), boxSize, isKanrojiSword);
            }

            // Spawn particle trail for Kanroji/Love sword swings (sword_to_left and sword_to_right animations)
            if ((isKanrojiSword || isLoveSword) && player.level() instanceof ServerLevel serverLevel) {
                spawnKanrojiSwordTrailParticles(player, serverLevel, animationName);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }

    private void broadcastSlashToNearbyPlayers(ServerPlayer attacker, String animName) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        String slashAnimation = normalizeSlashAnimation(animName);
        MobSwordSlashPacket packet = new MobSwordSlashPacket(attacker.getUUID(), slashAnimation, 0);

        for (ServerPlayer other : serverLevel.players()) {
            if (other.equals(attacker)) {
                continue;
            }
            if (other.distanceToSqr(attacker) <= (64.0 * 64.0)) {
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(packet, other);
            }
        }
    }

    private String normalizeSlashAnimation(String animName) {
        if (animName == null || animName.isEmpty()) {
            return "sword_to_left";
        }

        // Offhand dual-wield animations share the same slash path as their main-hand counterparts.
        if (animName.equals("left_sword_to_left")) {
            return "sword_to_left";
        }
        if (animName.equals("left_sword_to_right")) {
            return "sword_to_right";
        }
        if (animName.equals("left_sword_overhead") || animName.equals("double_sword_overhead")) {
            return "sword_overhead";
        }
        if (animName.equals("beast2")) {
            return "sword_to_left";
        }
        if (animName.equals("breath_beast2")) {
            return "sword_to_right";
        }
        return animName;
    }

    /**
     * Spawn particle trail for Kanroji sword swing animations (sword_to_left and sword_to_right)
     */
    private void spawnKanrojiSwordTrailParticles(ServerPlayer player, ServerLevel serverLevel, String animName) {
        
        if (com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.disableLoveM1TrailParticles)
            return;
        
        // Only spawn particles for sword_to_left and sword_to_right and sword_overhead and kanroji_sword_overhead animations
        if (!animName.equals("sword_to_left") && !animName.equals("sword_to_right") && !animName.equals("sword_overhead") && !animName.equals("kanroji_sword_overhead") && !animName.equals("sword_overhead_kanroji") && !animName.equals("sword_rotate")) {
            return;
        }

        // Get particle position data from ParticlePositions
        Map<String, float[][][]> particleMap = null;
        if (animName.equals("sword_to_left")) {
            particleMap = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_to_left;
        } else if (animName.equals("sword_to_right")) {
            particleMap = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_to_right;
        } else if (animName.equals("sword_rotate")) {
             particleMap = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_rotate;
         } else if (animName.equals("sword_overhead") || animName.equals("sword_overheaed")) {
             particleMap = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.sword_overhead;
         } else if (animName.equals("kanroji_sword_overhead") || animName.equals("sword_overhead_kanroji") || animName.equals("kanroji_sword_overheaed") || animName.equals("sword_overheaed_kanroji")) {
             particleMap = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticlePositions.kanroji_sword_overhead;
         }

        if (particleMap == null) {
            if (Config.logDebug) {
                Log.debug("No ParticlePositions map found for animation: {}", animName);
            }
            return;
        }

        float[][][] particlePoints = particleMap.get("point_a");
        if (particlePoints == null || particlePoints.length == 0) {
            if (Config.logDebug) {
                Log.debug("No particle points found in map for animation: {}", animName);
            }
            return;
        }

        // Track current tick for particle spawning
        final int[] currentTick = {0};
        final int totalTicks = particlePoints.length;

        // Get initial forward and right vectors using yaw (handles looking up/down)
        final Vec3[] vectors = new Vec3[2];
        float yawRad = (float) Math.toRadians(-player.getYRot());
        vectors[0] = new Vec3(Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        // Rotate 90 degrees counter-clockwise for RIGHT direction: (x, 0, z) -> (-z, 0, x)
        vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);

        // Schedule particle spawning for each tick of the animation
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler.scheduleRepeating(player, () -> {
            // Update vectors every 4 ticks to account for player rotation
            if (currentTick[0] % 4 == 0) {
                float yaw = (float) Math.toRadians(-player.getYRot());
                vectors[0] = new Vec3(Math.sin(yaw), 0, Math.cos(yaw)).normalize();
                vectors[1] = new Vec3(-vectors[0].z, 0, vectors[0].x);
            }

            // Spawn particles for current tick
            if (currentTick[0] < particlePoints.length && particlePoints[currentTick[0]].length > 0) {
                for (int i = 0; i < particlePoints[currentTick[0]].length; i++) {
                    float x = particlePoints[currentTick[0]][i][0];
                    float y = particlePoints[currentTick[0]][i][1];
                    float z = particlePoints[currentTick[0]][i][2];

                    Vec3 pos = player.position().add(vectors[0].scale(z).add(vectors[1].scale(x)).add(0, y, 0));

                    // Spawn pink dust particle at pos
                    serverLevel.sendParticles(
                        new com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions(
                            new org.joml.Vector3f(1.0f, 0.4f, 0.7f), // PINK
                            1.2f                                      // scale
                        ),
                        pos.x, pos.y, pos.z,
                        2, 0.05, 0.05, 0.05, 0 // count, velocity
                    );

                    // Spawn white dust particle at pos
                    serverLevel.sendParticles(
                        new com.lerdorf.kimetsunoyaibamultiplayer.particles.EnergyParticleOptions(
                            new org.joml.Vector3f(1.0f, 1.0f, 1.0f), // WHITE
                            1f                                        // scale
                        ),
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0 // count, velocity
                    );
                }
            }

            currentTick[0]++;
        }, 1, totalTicks); // Run for totalTicks iterations, once per tick

        if (Config.logDebug) {
            Log.debug("Started Kanroji sword trail particles for animation: {} ({} ticks)", animName, totalTicks);
        }
    }
}
