package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyMedicineHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class SunlightImmunityHelper {
    private static final TagKey<EntityType<?>> ENCLOSED_VEHICLES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("forge", "enclosed")
    );

    public static final ResourceLocation BASE_OVERCOME_SUNLIGHT_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "overcome_sunlight");
    public static final ResourceLocation SUNLIGHT_IMMUNITY_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "conquering_the_sun");

    private SunlightImmunityHelper() {
    }

    public static boolean hasSunlightImmunity(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getPersistentData().getBoolean(AlchemyMedicineHandler.SUNLIGHT_IMMUNITY_KEY)) {
            return true;
        }
        return entity instanceof ServerPlayer player
            && !isBaseSunBreathingSunlightImmunityDisabled()
            && hasBaseOvercomeSunlightAdvancement(player);
    }

    public static boolean isBaseSunBreathingSunlightImmunityDisabled() {
        return CustomProgressionConfig.disableSunBreathingSunlightImmunity != null
            && CustomProgressionConfig.disableSunBreathingSunlightImmunity.get();
    }

    /**
     * Whether the Fire Resistance potion effect should grant demons sunlight
     * immunity (base mod behavior). Defaults to true until the config loads.
     */
    public static boolean isFireResistanceSunlightImmunityEnabled() {
        return CustomProgressionConfig.fireResistanceGivesSunlightImmunity == null
            || CustomProgressionConfig.fireResistanceGivesSunlightImmunity.get();
    }

    /**
     * Applies sunlight burn damage. When Fire Resistance sunlight immunity is
     * suppressed (config off), a generic damage source is used so the vanilla
     * Fire Resistance effect cannot nullify the burn.
     */
    public static boolean hurtSunlightBurn(LivingEntity entity, float amount) {
        if (entity == null || entity.level().isClientSide || entity.isRemoved()) {
            return false;
        }
        DamageSource source = isFireResistanceSunlightImmunityEnabled()
            ? entity.damageSources().onFire()
            : entity.damageSources().generic();
        return entity.hurt(source, amount);
    }

    public static boolean isOvercomeSunlightAdvancement(Advancement advancement) {
        return advancement != null && BASE_OVERCOME_SUNLIGHT_ADVANCEMENT.equals(advancement.getId());
    }

    public static boolean hasBaseOvercomeSunlightAdvancement(ServerPlayer player) {
        if (player == null || player.server == null) {
            return false;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(BASE_OVERCOME_SUNLIGHT_ADVANCEMENT);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void revokeBaseOvercomeSunlightAdvancement(ServerPlayer player) {
        revokeAdvancement(player, BASE_OVERCOME_SUNLIGHT_ADVANCEMENT);
    }

    public static void revokeSunlightImmunityAdvancement(ServerPlayer player) {
        revokeAdvancement(player, SUNLIGHT_IMMUNITY_ADVANCEMENT);
    }

    public static void playSunlightImmunityGrantedEffects(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        level.playSound(
            null,
            entity.getX(),
            entity.getY(0.5D),
            entity.getZ(),
            SoundEvents.WITHER_SPAWN,
            SoundSource.PLAYERS,
            1.5F,
            0.75F
        );
        spawnBloodFlameSpiral(level, entity);

        if (entity instanceof ServerPlayer player) {
            awardAdvancement(player, SUNLIGHT_IMMUNITY_ADVANCEMENT);
        }
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || player.server == null) {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static void revokeAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || player.server == null) {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria()) {
            player.getAdvancements().revoke(advancement, criterion);
        }
    }

    private static void spawnBloodFlameSpiral(ServerLevel level, LivingEntity entity) {
        double baseY = entity.getY() + 0.1D;
        double height = Math.max(1.8D, entity.getBbHeight() + 0.5D);
        double radius = Math.max(0.8D, entity.getBbWidth() * 0.8D + 0.35D);

        for (int i = 0; i < 96; i++) {
            double progress = i / 96.0D;
            double angle = progress * Math.PI * 8.0D;
            double y = baseY + progress * height;
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            double driftX = Math.cos(angle) * 0.02D;
            double driftZ = Math.sin(angle) * 0.02D;

            level.sendParticles(
                ModParticles.BLOOD_FLAME.get(),
                x,
                y,
                z,
                1,
                driftX,
                0.035D,
                driftZ,
                0.0D
            );
        }
    }

    public static boolean isSunlightFireDamage(DamageSource source) {
        return source != null
            && (source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.IN_FIRE));
    }

    public static boolean isBaseSunlightGenericDamage(DamageSource source) {
        return source != null
            && source.is(DamageTypes.GENERIC)
            && source.getEntity() == null
            && source.getDirectEntity() == null;
    }

    public static boolean isInBurningSunlight(LivingEntity entity) {
        if (!isInSunlightExposure(entity)) {
            return false;
        }
        return !entity.isInWaterRainOrBubble() && !entity.isUnderWater();
    }

    public static boolean isInSunlightExposure(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel) || !serverLevel.isDay()) {
            return false;
        }
        return isInSunlightExposure(serverLevel, entity, false);
    }

    public static boolean isInSunlightExposureIgnoringRain(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel serverLevel) || !serverLevel.isDay()) {
            return false;
        }
        return isInSunlightExposure(serverLevel, entity, true);
    }

    public static boolean isEnclosedVehicle(Entity entity) {
        return entity != null && entity.getType().is(ENCLOSED_VEHICLES);
    }

    private static boolean isInSunlightExposure(ServerLevel level, LivingEntity entity, boolean ignoringRain) {
        Entity vehicle = entity.getVehicle();
        if (isEnclosedVehicle(vehicle)) {
            return false;
        }

        if (isEntitySkyExposed(level, entity, ignoringRain)) {
            return true;
        }
        return vehicle != null && isEntitySkyExposed(level, vehicle, ignoringRain);
    }

    private static boolean isEntitySkyExposed(ServerLevel level, Entity entity, boolean ignoringRain) {
        return isSkyExposed(level, entity.blockPosition(), ignoringRain)
            || isSkyExposed(level, BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()), ignoringRain)
            || isSkyExposed(level, BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ()), ignoringRain);
    }

    private static boolean isSkyExposed(ServerLevel level, BlockPos pos) {
        return isSkyExposed(level, pos, false);
    }

    private static boolean isSkyExposed(ServerLevel level, BlockPos pos, boolean ignoringRain) {
        if (level.canSeeSky(pos)) {
            return ignoringRain || !level.isRainingAt(pos);
        }

        BlockPos exposedFluidSurface = findSkyExposedFluidSurface(level, pos);
        return exposedFluidSurface != null && (ignoringRain || !level.isRainingAt(exposedFluidSurface));
    }

    private static boolean isSkyExposedIgnoringRain(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos) || findSkyExposedFluidSurface(level, pos) != null;
    }

    private static BlockPos findSkyExposedFluidSurface(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return null;
        }

        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int y = pos.getY(); y < level.getMaxBuildHeight(); y++) {
            mutable.set(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(mutable);
            FluidState fluid = state.getFluidState();
            if (fluid.isEmpty() && !state.isAir()) {
                return null;
            }
            if (level.canSeeSky(mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }
}
