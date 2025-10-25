package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles wisteria biome protection - prevents demons from entering wisteria forests
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class WisteriaBiomeHandler {

    private static final ResourceLocation WISTERIA_FOREST_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest");

    /**
     * Check if the entity is in a wisteria forest biome
     */
    private static boolean isInWisteriaForest(LivingEntity entity) {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);

        if (biomeKey == null) {
            return false;
        }

        return biomeKey.location().equals(WISTERIA_FOREST_ID);
    }

    /**
     * Prevent demons from entering wisteria forest biome
     */
    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // Only check on server side
        if (entity.level().isClientSide()) {
            return;
        }

        // Only check demons (includes players turned into demons)
        if (!Damager.isDemon(entity)) {
            return;
        }

        // Check every 10 ticks to reduce performance impact
        if (entity.tickCount % 10 != 0) {
            return;
        }

        // Check if demon is in wisteria forest
        if (!isInWisteriaForest(entity)) {
            return;
        }

        // Get wisteria poison effect from KnY mod
        net.minecraft.world.effect.MobEffect wisteriaPoison =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.getWisteriaPoisonEffect();

        // Apply severe damage and effects
        entity.hurt(entity.level().damageSources().magic(), 5.0f); // 2.5 hearts damage

        // Apply strong debuffs
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 2, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, false));

        if (wisteriaPoison != null) {
            entity.addEffect(new MobEffectInstance(wisteriaPoison, 100, 3, false, false));
        }

        // Calculate push direction away from biome center
        // Find nearest non-wisteria block by checking surrounding positions
        double pushX = 0;
        double pushZ = 0;
        int samples = 0;

        // Sample 8 directions around entity
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                net.minecraft.core.BlockPos checkPos = entity.blockPosition().offset(dx * 16, 0, dz * 16);
                Holder<Biome> checkBiome = entity.level().getBiome(checkPos);
                ResourceKey<Biome> checkBiomeKey = checkBiome.unwrapKey().orElse(null);

                if (checkBiomeKey != null && !checkBiomeKey.location().equals(WISTERIA_FOREST_ID)) {
                    // Found non-wisteria biome, push towards it
                    pushX += dx;
                    pushZ += dz;
                    samples++;
                }
            }
        }

        // If we found a direction to push, apply strong knockback
        if (samples > 0) {
            pushX /= samples;
            pushZ /= samples;
            double distance = Math.sqrt(pushX * pushX + pushZ * pushZ);

            if (distance > 0) {
                double pushStrength = 0.5; // Very strong push
                entity.push(
                    (pushX / distance) * pushStrength,
                    0.3, // Strong upward push to help escape
                    (pushZ / distance) * pushStrength
                );
            }
        } else {
            // No clear direction found, push in random direction away from center
            double angle = entity.level().random.nextDouble() * 2 * Math.PI;
            entity.push(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
        }
    }
}
