package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.WisteriaPetalsBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WisteriaRepellentHelper {
    public static final double FOREST_RANGE = 8.0D;
    public static final double OUTSIDE_RANGE = 6.0D;
    public static final int FOREST_SLOWNESS_AMPLIFIER = 5;
    public static final int FOREST_POISON_AMPLIFIER = 7;
    public static final int OUTSIDE_SLOWNESS_AMPLIFIER = 1;
    public static final int OUTSIDE_POISON_AMPLIFIER = 1;
    public static final int AURA_DURATION_TICKS = 160;
    private static final float FOREST_PETAL_DAMAGE = 9.0F;
    private static final float OUTSIDE_PETAL_DAMAGE = 2.0F;
    private static final Map<PetalDamageKey, Long> LAST_PETAL_DAMAGE_SECOND = new HashMap<>();

    private static final ResourceLocation WISTERIA_FOREST_CYAN =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cyan");
    private static final ResourceLocation WISTERIA_FOREST_CREAM =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cream");
    private static final ResourceLocation WISTERIA_FOREST =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest");

    private WisteriaRepellentHelper() {
    }

    public static boolean isInWisteriaForest(Level level, BlockPos pos) {
        ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);
        if (biomeKey == null) {
            return false;
        }

        ResourceLocation biomeLoc = biomeKey.location();
        return biomeLoc.equals(WISTERIA_FOREST_CYAN)
            || biomeLoc.equals(WISTERIA_FOREST_CREAM)
            || biomeLoc.equals(WISTERIA_FOREST);
    }

    public static double effectRange(Level level, BlockPos sourcePos) {
        return isInWisteriaForest(level, sourcePos) ? FOREST_RANGE : OUTSIDE_RANGE;
    }

    public static double pushRange(Level level, BlockPos sourcePos) {
        return effectRange(level, sourcePos) * 0.5D;
    }

    public static void applyBlockAura(Level level, BlockPos sourcePos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double range = effectRange(serverLevel, sourcePos);
        AABB searchBox = new AABB(sourcePos).inflate(range);
        List<LivingEntity> demons = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, Damager::isDemon);
        for (LivingEntity demon : demons) {
            if (isEntityWithinSourceRange(serverLevel, sourcePos, demon)) {
                applyEffectsFromSource(serverLevel, sourcePos, demon, AURA_DURATION_TICKS);
            }
        }
    }

    public static void applyEffectsFromSource(Level level, BlockPos sourcePos, LivingEntity demon, int durationTicks) {
        if (!Damager.isDemon(demon)) {
            return;
        }

        boolean sourceInForest = isInWisteriaForest(level, sourcePos);
        int slownessAmplifier = sourceInForest ? FOREST_SLOWNESS_AMPLIFIER : OUTSIDE_SLOWNESS_AMPLIFIER;
        int poisonAmplifier = sourceInForest ? FOREST_POISON_AMPLIFIER : OUTSIDE_POISON_AMPLIFIER;
        WisteriaResistanceHelper.addMovementSlowdownUnlessResistant(demon, durationTicks, slownessAmplifier, false, false);
        WisteriaResistanceHelper.addWisteriaPoisonEffect(demon, durationTicks, poisonAmplifier, false, true, true);
        if (sourceInForest) {
            addRegenerationInhibitionEffect(demon, durationTicks);
        }
    }

    public static boolean addRegenerationInhibitionEffect(LivingEntity entity, int durationTicks) {
        MobEffect regenerationInhibition = KnYEffects.getRegenerationInhibitionEffect();
        if (entity == null || regenerationInhibition == null) {
            return false;
        }

        entity.addEffect(new MobEffectInstance(regenerationInhibition, durationTicks, 0, false, true, true));
        return true;
    }

    public static void applyNearestRepellentAura(ServerLevel level, LivingEntity demon) {
        RepellentSource source = findNearestRepellentSource(level, demon);
        if (source == null) {
            return;
        }

        applyEffectsFromSource(level, source.pos(), demon, AURA_DURATION_TICKS);
        if (source.isPetal() && source.isWithinPushRange()) {
            applyPetalDamage(level, source.pos(), demon);
        }
        if (source.inWisteriaForest() && source.isWithinPushRange() && !WisteriaResistanceHelper.hasResistance(demon)) {
            pushAwayFromSource(demon, Vec3.atCenterOf(source.pos()), 0.35D, 0.08D);
        }
    }

    public static void applyPetalDamage(Level level, BlockPos sourcePos, LivingEntity demon) {
        if (!(level instanceof ServerLevel serverLevel) || !Damager.isDemon(demon)) {
            return;
        }

        long gameSecond = serverLevel.getGameTime() / 20L;
        PetalDamageKey key = new PetalDamageKey(demon.getUUID(), sourcePos.immutable());
        if (LAST_PETAL_DAMAGE_SECOND.getOrDefault(key, Long.MIN_VALUE) == gameSecond) {
            return;
        }

        float baseDamage = isInWisteriaForest(serverLevel, sourcePos) ? FOREST_PETAL_DAMAGE : OUTSIDE_PETAL_DAMAGE;
        float damage = WisteriaResistanceHelper.reduceWisteriaDamage(demon, baseDamage);
        LAST_PETAL_DAMAGE_SECOND.put(key, gameSecond);
        if (damage > 0.0F) {
            demon.hurt(serverLevel.damageSources().magic(), damage);
        }
        prunePetalDamageTracker(gameSecond);
    }

    public static void pushAwayFromSource(LivingEntity entity, Vec3 sourceCenter, double strength, double yBoost) {
        Vec3 away = new Vec3(entity.getX() - sourceCenter.x, 0.0D, entity.getZ() - sourceCenter.z);
        if (away.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 push = away.normalize().scale(strength).add(0.0D, yBoost, 0.0D);
        MovementHelper.addVelocity(entity, push);
    }

    public static boolean isWisteriaRepellentBlock(BlockState state) {
        return isWisteriaLeafBlock(state) || state.getBlock() instanceof WisteriaPetalsBlock;
    }

    private static boolean isWisteriaLeafBlock(BlockState state) {
        return state.is(ModBlocks.WISTERIA_LEAVES.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_PINK.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_CYAN.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_CREAM.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get());
    }

    private static boolean isEntityWithinSourceRange(Level level, BlockPos sourcePos, LivingEntity entity) {
        double range = effectRange(level, sourcePos);
        return Vec3.atCenterOf(sourcePos).distanceToSqr(entity.position()) <= range * range;
    }

    @Nullable
    private static RepellentSource findNearestRepellentSource(ServerLevel level, LivingEntity entity) {
        BlockPos entityPos = entity.blockPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        RepellentSource nearestForestSource = null;
        RepellentSource nearestOutsideSource = null;
        double nearestForestDistance = Double.MAX_VALUE;
        double nearestOutsideDistance = Double.MAX_VALUE;

        int searchRadius = (int) Math.ceil(FOREST_RANGE);
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    checkPos.set(entityPos.getX() + x, entityPos.getY() + y, entityPos.getZ() + z);
                    BlockState state = level.getBlockState(checkPos);
                    if (!isWisteriaRepellentBlock(state)) {
                        continue;
                    }

                    boolean inForest = isInWisteriaForest(level, checkPos);
                    double range = inForest ? FOREST_RANGE : OUTSIDE_RANGE;
                    double distanceSqr = Vec3.atCenterOf(checkPos).distanceToSqr(entity.position());
                    if (distanceSqr > range * range) {
                        continue;
                    }

                    BlockPos sourcePos = checkPos.immutable();
                    boolean isPetal = state.getBlock() instanceof WisteriaPetalsBlock;
                    if (inForest && distanceSqr < nearestForestDistance) {
                        nearestForestDistance = distanceSqr;
                        nearestForestSource = new RepellentSource(sourcePos, true, isPetal, distanceSqr, range);
                    } else if (!inForest && distanceSqr < nearestOutsideDistance) {
                        nearestOutsideDistance = distanceSqr;
                        nearestOutsideSource = new RepellentSource(sourcePos, false, isPetal, distanceSqr, range);
                    }
                }
            }
        }

        return nearestForestSource != null ? nearestForestSource : nearestOutsideSource;
    }

    private static void prunePetalDamageTracker(long gameSecond) {
        if (gameSecond % 30L != 0L) {
            return;
        }

        Iterator<Map.Entry<PetalDamageKey, Long>> iterator = LAST_PETAL_DAMAGE_SECOND.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PetalDamageKey, Long> entry = iterator.next();
            if (gameSecond - entry.getValue() > 30L) {
                iterator.remove();
            }
        }
    }

    private record RepellentSource(BlockPos pos, boolean inWisteriaForest, boolean isPetal, double distanceSqr, double effectRange) {
        private boolean isWithinPushRange() {
            double pushRange = effectRange * 0.5D;
            return distanceSqr <= pushRange * pushRange;
        }
    }

    private record PetalDamageKey(UUID entityId, BlockPos sourcePos) {
    }
}
