package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Glowing variant of Wisteria leaves block for Wisteria trees.
 * Features:
 * - Dim glow effect (light level 4)
 * - Drops purple petal particles when there's air below
 * - Repels monsters (demons) with weakness and slowness effects
 */
public class GlowingWisteriaLeavesBlock extends LeavesBlock {

    public GlowingWisteriaLeavesBlock(Properties properties) {
        super(properties);
    }

    /**
     * Called every tick for random display effects (client and server)
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // Only spawn particles on client side
        if (level.isClientSide()) {
            // Check if there's an empty block below
            BlockPos below = pos.below();
            if (level.isEmptyBlock(below)) {
                // 10% chance per tick to spawn a falling petal particle
                if (random.nextFloat() < 0.1f) {
                    double x = pos.getX() + random.nextDouble();
                    double y = pos.getY() - 0.05;
                    double z = pos.getZ() + random.nextDouble();

                    // Use cherry leaves particles for falling petals (purple-ish)
                    // If you want custom particles, you'll need to register them separately
                    level.addParticle(
                        ParticleTypes.CHERRY_LEAVES,
                        x, y, z,
                        0, -0.05, 0  // Velocity (falling slowly)
                    );
                }
            }
        }
    }

    /**
     * Called on server every random tick (slower than animateTick)
     * Used for gameplay effects like scaring monsters
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);

        // Scare monsters in a 6 block radius
        scareNearbyMonsters(level, pos);
    }

    /**
     * Apply debilitating effects to nearby demons (no damage)
     */
    private void scareNearbyMonsters(Level level, BlockPos pos) {
        // Define search area (6 blocks in all directions)
        AABB searchBox = new AABB(pos).inflate(6.0);

        // Find all living entities in range
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);

        // Get wisteria poison effect from KnY mod
        net.minecraft.world.effect.MobEffect wisteriaPoison =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.getWisteriaPoisonEffect();

        for (LivingEntity demon : entities) {
            // Only affect demons from kimetsunoyaiba mod (includes players turned into demons)
            if (!com.lerdorf.kimetsunoyaibamultiplayer.Damager.isDemon(demon)) {
                continue;
            }

            double dx = demon.getX() - pos.getX();
            double dz = demon.getZ() - pos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            // Apply effects based on distance (NO DAMAGE)
            if (distance < 3.0) {
                // Very close: Extreme slowness and poison
                demon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 9, false, false)); // Extreme slowness
                if (wisteriaPoison != null) {
                    demon.addEffect(new MobEffectInstance(wisteriaPoison, 100, 4, false, false)); // Strong poison
                }
            } else if (distance < 6.0) {
                // Medium range: Heavy slowness and moderate poison
                demon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, false)); // Heavy slowness
                if (wisteriaPoison != null) {
                    demon.addEffect(new MobEffectInstance(wisteriaPoison, 100, 2, false, false)); // Moderate poison
                }
            }
        }
    }

    /**
     * Called when an entity walks on/through the block
     * Provides immediate debilitating effects when demons touch leaves (no damage)
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        // If a demon touches the leaves directly, apply extreme debuffs (includes players turned into demons)
        if (entity instanceof LivingEntity living && com.lerdorf.kimetsunoyaibamultiplayer.Damager.isDemon(living)) {
            // Get wisteria poison effect from KnY mod
            net.minecraft.world.effect.MobEffect wisteriaPoison =
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.getWisteriaPoisonEffect();

            // Apply maximum slowness and strong poison (10 seconds) - NO DAMAGE
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 9, false, false)); // Maximum slowness
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, false)); // Nausea/confusion
            if (wisteriaPoison != null) {
                living.addEffect(new MobEffectInstance(wisteriaPoison, 200, 5, false, false)); // Very strong poison
            }
        }
    }
}