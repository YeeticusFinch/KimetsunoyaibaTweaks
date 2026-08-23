package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Helper class to access potion effects from the KimetsunoYaiba mod
 */
public class KnYEffects {

    // KimetsunoYaiba mod effect registry names
    private static final ResourceLocation COLD = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cold");
    private static final ResourceLocation COOL_TIME = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cool_time");
    private static final ResourceLocation COOLTIME_2 = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "cooltime_2");
    private static final ResourceLocation WISTERIA_POISON = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "wisteriapoison");
    private static final ResourceLocation REGENERATION_INHIBITION = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "regeneration_inhibition");
    private static final ResourceLocation IMMVABLE = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "immvable");

    /**
     * Gets the cold (Reiki) effect from KnY mod
     * @return The cold MobEffect, or null if not found
     */
    public static MobEffect getColdEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(COLD);
    }

    /**
     * Gets the cool_time effect from KnY mod
     * @return The cool_time MobEffect, or null if not found
     */
    public static MobEffect getCoolTimeEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(COOL_TIME);
    }

    /**
     * Gets the cooltime_2 effect from KnY mod
     * @return The cooltime_2 MobEffect, or null if not found
     */
    public static MobEffect getCoolTime2Effect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(COOLTIME_2);
    }

    /**
     * Checks if entity has the cool_time effect active
     * @param entity Entity to check
     * @return true if entity has cool_time effect
     */
    public static boolean hasCoolTime(LivingEntity entity) {
        MobEffect coolTime = getCoolTimeEffect();
        return coolTime != null && entity.hasEffect(coolTime);
    }

    /**
     * Checks if entity has the cooltime_2 effect active
     * @param entity Entity to check
     * @return true if entity has cooltime_2 effect
     */
    public static boolean hasCoolTime2(LivingEntity entity) {
        MobEffect coolTime2 = getCoolTime2Effect();
        return coolTime2 != null && entity.hasEffect(coolTime2);
    }

    /**
     * Gets the wisteriapoison effect from KnY mod
     * @return The wisteriapoison MobEffect, or null if not found
     */
    public static MobEffect getWisteriaPoisonEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(WISTERIA_POISON);
    }

    public static boolean addVisibleWisteriaPoisonEffect(LivingEntity entity, int durationTicks, int amplifier) {
        MobEffect wisteriaPoison = getWisteriaPoisonEffect();
        if (entity == null || wisteriaPoison == null) {
            return false;
        }

        entity.addEffect(new MobEffectInstance(wisteriaPoison, durationTicks, amplifier, false, true, true));
        return true;
    }

    public static MobEffect getRegenerationInhibitionEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(REGENERATION_INHIBITION);
    }

    public static MobEffect getImmvableEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(IMMVABLE);
    }
}
