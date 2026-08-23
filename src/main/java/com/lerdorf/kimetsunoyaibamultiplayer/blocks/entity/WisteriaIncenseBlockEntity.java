package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.WisteriaIncenseBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.util.WisteriaRepellentHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.WisteriaResistanceHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WisteriaIncenseBlockEntity extends BlockEntity {
    private static final int MAX_INCENSE_RADIUS = 6;
    private static final float INCENSE_DAMAGE_PER_COUNT = 2.0F;
    private static final float MAX_INCENSE_DAMAGE_PER_SECOND = 12.0F;
    private static final Map<UUID, Long> LAST_INCENSE_DAMAGE_SECOND = new HashMap<>();

    private int burnTicks;

    public WisteriaIncenseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WISTERIA_INCENSE.get(), pos, state);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, WisteriaIncenseBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(state.getBlock() instanceof WisteriaIncenseBlock) || !state.getValue(WisteriaIncenseBlock.LIT)) {
            return;
        }

        blockEntity.burnTicks++;
        applyDemonRepellent(serverLevel, pos, state, blockEntity.burnTicks % 20 == 0);
        if (blockEntity.burnTicks >= 1000) {
            blockEntity.burnTicks = 0;
            int stage = state.getValue(WisteriaIncenseBlock.BURN_STAGE);
            if (stage >= 7) {
                WisteriaIncenseBlock.burnOut(serverLevel, pos, state);
            } else {
                serverLevel.setBlock(pos, state.setValue(WisteriaIncenseBlock.BURN_STAGE, stage + 1), 3);
            }
        }
        blockEntity.setChanged();
    }

    public void resetBurnTicks() {
        burnTicks = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTicks", burnTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTicks = tag.getInt("BurnTicks");
    }

    private static void applyDemonRepellent(ServerLevel level, BlockPos pos, BlockState state, boolean applyTimedEffects) {
        int count = state.getValue(WisteriaIncenseBlock.COUNT);
        double radius = 2.0D + count;
        AABB area = new AABB(pos).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, Damager::isDemon);
        Vec3 sourceCenter = Vec3.atCenterOf(pos);

        for (LivingEntity entity : entities) {
            boolean withinPushRange = entity.position().distanceToSqr(sourceCenter) <= (radius * 0.5D) * (radius * 0.5D);
            boolean pushed = false;
            if (withinPushRange && !WisteriaResistanceHelper.hasResistance(entity)) {
                WisteriaRepellentHelper.pushAwayFromSource(entity, sourceCenter, incensePushStrength(entity, sourceCenter, radius), 0.08D);
                pushed = true;
            }

            if (!applyTimedEffects && !pushed) {
                continue;
            }

            int incenseCount = sumLitIncenseCountsAffecting(level, entity);
            if (incenseCount <= 0) {
                continue;
            }

            int amplifier = Math.min(incenseCount, Math.max(0, Config.wisteriaIncenseMaxPoisonAmplifier));
            KnYEffects.addVisibleWisteriaPoisonEffect(entity, 160, amplifier);
            WisteriaRepellentHelper.addRegenerationInhibitionEffect(entity, 160);
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }

            applyIncenseDamage(level, entity, incenseCount);
        }
    }

    private static double incensePushStrength(LivingEntity entity, Vec3 sourceCenter, double radius) {
        double distance = entity.position().distanceTo(sourceCenter);
        return 0.35D * (1.0D - Math.min(distance / radius, 0.85D));
    }

    private static void applyIncenseDamage(ServerLevel level, LivingEntity entity, int incenseCount) {
        long gameSecond = level.getGameTime() / 20L;
        UUID entityId = entity.getUUID();
        if (LAST_INCENSE_DAMAGE_SECOND.getOrDefault(entityId, Long.MIN_VALUE) == gameSecond) {
            return;
        }

        float rawDamage = Math.min(MAX_INCENSE_DAMAGE_PER_SECOND, INCENSE_DAMAGE_PER_COUNT * incenseCount);
        float damage = WisteriaResistanceHelper.reduceWisteriaDamage(entity, rawDamage);
        LAST_INCENSE_DAMAGE_SECOND.put(entityId, gameSecond);
        if (damage > 0.0F) {
            entity.hurt(level.damageSources().magic(), damage);
        }
        pruneDamageTracker(gameSecond);
    }

    private static void pruneDamageTracker(long gameSecond) {
        if (gameSecond % 30L != 0L) {
            return;
        }

        Iterator<Map.Entry<UUID, Long>> iterator = LAST_INCENSE_DAMAGE_SECOND.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (gameSecond - entry.getValue() > 30L) {
                iterator.remove();
            }
        }
    }

    private static int sumLitIncenseCountsAffecting(ServerLevel level, LivingEntity entity) {
        int incenseCount = 0;
        BlockPos entityPos = entity.blockPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int x = -MAX_INCENSE_RADIUS; x <= MAX_INCENSE_RADIUS; x++) {
            for (int y = -MAX_INCENSE_RADIUS; y <= MAX_INCENSE_RADIUS; y++) {
                for (int z = -MAX_INCENSE_RADIUS; z <= MAX_INCENSE_RADIUS; z++) {
                    checkPos.set(entityPos.getX() + x, entityPos.getY() + y, entityPos.getZ() + z);
                    BlockState incenseState = level.getBlockState(checkPos);
                    if (!(incenseState.getBlock() instanceof WisteriaIncenseBlock) || !incenseState.getValue(WisteriaIncenseBlock.LIT)) {
                        continue;
                    }

                    int count = incenseState.getValue(WisteriaIncenseBlock.COUNT);
                    double radius = 2.0D + count;
                    if (entity.position().distanceToSqr(Vec3.atCenterOf(checkPos)) <= radius * radius) {
                        incenseCount += count;
                    }
                }
            }
        }
        return incenseCount;
    }
}
