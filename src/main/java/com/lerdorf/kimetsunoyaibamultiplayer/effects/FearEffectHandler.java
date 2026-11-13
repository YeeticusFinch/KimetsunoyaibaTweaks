package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class FearEffectHandler {
    private static final int HOTBAR_COOLDOWN_TICKS = 6;
    private static final int PRESENCE_CHECK_TICKS = 200;
    private static final int KIZUKI_FEAR_COOLDOWN_TICKS = 60 * 60 * 20;
    private static final double KIZUKI_FEAR_RANGE = 40.0D;
    private static final int MAX_MOVEMENT_CURVE_LEVEL = 9;
    private static final int FREEZE_START_LEVEL = 5;
    private static final int MAX_FREEZE_TICKS = 40;
    private static final int MIN_SLOW_TICKS = 5;
    private static final int MAX_SLOW_TICKS = 100;
    private static final double MIN_SLOW_MOVEMENT_MULTIPLIER = 0.2D;
    private static final String KNY_NAMESPACE = "kimetsunoyaiba";

    private static final String FEAR_PHASE_ACTIVE_KEY = "knymp_fear_phase_active";
    private static final String FEAR_PHASE_START_TICK_KEY = "knymp_fear_phase_start_tick";
    private static final String FEAR_PHASE_LEVEL_KEY = "knymp_fear_phase_level";
    private static final String PLAYER_LOCKED_KEY = "knymp_fear_locked";
    private static final String PLAYER_LOCK_X_KEY = "knymp_fear_lock_x";
    private static final String PLAYER_LOCK_Z_KEY = "knymp_fear_lock_z";

    private static final String MOB_NO_AI_STORED_KEY = "knymp_fear_no_ai_stored";
    private static final String MOB_ORIGINAL_NO_AI_KEY = "knymp_fear_original_no_ai";
    private static final UUID FEAR_SLOWDOWN_MODIFIER_UUID = UUID.fromString("d7adca67-5b84-4c5c-9bb8-000000000001");

    private record KizukiFearProfile(int level, int durationTicks) {
    }

    public static int getFearLevel(LivingEntity entity) {
        if (entity == null || !entity.hasEffect(ModEffects.FEAR.get())) {
            return 0;
        }
        MobEffectInstance instance = entity.getEffect(ModEffects.FEAR.get());
        if (instance == null) {
            return 0;
        }
        int fearLevel = Math.max(1, Math.min(10, instance.getAmplifier() + 1));
        MobEffectInstance courage = entity.getEffect(ModEffects.COURAGE.get());
        if (courage != null) {
            fearLevel -= Math.max(1, courage.getAmplifier() + 1);
        }
        return Math.max(0, fearLevel);
    }

    public static boolean grantFearWithCooldown(LivingEntity target, int displayedFearLevel, int durationTicks) {
        if (target == null || !target.isAlive() || target.level().isClientSide() || durationTicks <= 0) {
            return false;
        }

        int level = Math.max(1, Math.min(10, displayedFearLevel));
        int amplifier = level - 1;
        if (hasCourageAgainst(target, amplifier)) {
            return false;
        }

        MobEffectInstance cooldown = target.getEffect(ModEffects.FEAR_COOLDOWN.get());
        if (cooldown != null && cooldown.getAmplifier() >= amplifier) {
            return false;
        }

        target.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), durationTicks, amplifier, false, false, true));
        target.addEffect(new MobEffectInstance(ModEffects.FEAR_COOLDOWN.get(), KIZUKI_FEAR_COOLDOWN_TICKS, amplifier, false, false, true));
        return true;
    }

    public static boolean hasCourageAgainst(LivingEntity target, int fearAmplifier) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        MobEffectInstance courage = target.getEffect(ModEffects.COURAGE.get());
        return courage != null && courage.getAmplifier() >= fearAmplifier;
    }

    public static boolean isParalyzed(LivingEntity entity) {
        int level = getFearLevel(entity);
        if (level <= 0) {
            return false;
        }
        return getFearPhaseElapsed(entity, level) < getFreezeTicks(level);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.isAlive()) {
            return;
        }

        if (!entity.level().isClientSide()) {
            applyKizukiPresenceFear(entity);
        }

        int fearLevel = getFearLevel(entity);
        if (fearLevel <= 0) {
            clearFearMovementState(entity);
            return;
        }

        ensureFearPhase(entity, fearLevel);
        int phaseElapsed = getFearPhaseElapsed(entity, fearLevel);
        if (phaseElapsed >= getFreezeTicks(fearLevel)) {
            clearParalysis(entity);
            applyFearSlowdown(entity, phaseElapsed, fearLevel);
        } else {
            removeFearSlowdown(entity);
            if (entity instanceof Player player) {
                enforcePlayerParalysis(player);
            } else if (entity instanceof Mob mob) {
                enforceMobParalysis(mob);
            } else {
                stopHorizontalMotionAndJump(entity);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity source = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (!EntityConfig.kizukiFearEnabled || source == null || target == null || source.level().isClientSide()) {
            return;
        }

        KizukiFearProfile profile = getKizukiFearProfile(source);
        if (profile == null || !canReceiveKizukiAggroFear(target)) {
            return;
        }

        grantFearWithCooldown(target, profile.level(), profile.durationTicks());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null || event.player.level().isClientSide()) {
            return;
        }
        if (isParalyzed(event.player)) {
            keepHotbarCoolingDown(event.player);
            applyHiddenBaseCoolTime(event.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide() && isParalyzed(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteractionIfParalyzed(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteractionIfParalyzed(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelInteractionIfParalyzed(event);
    }

    private static void cancelInteractionIfParalyzed(PlayerInteractEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide() || !isParalyzed(player)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static void applyKizukiPresenceFear(LivingEntity source) {
        if (!EntityConfig.kizukiFearEnabled || source.tickCount % PRESENCE_CHECK_TICKS != 0
                || !(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        KizukiFearProfile profile = getKizukiFearProfile(source);
        if (profile == null) {
            return;
        }

        double rangeSqr = KIZUKI_FEAR_RANGE * KIZUKI_FEAR_RANGE;
        AABB bounds = source.getBoundingBox().inflate(KIZUKI_FEAR_RANGE);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != source && canReceiveKizukiPresenceFear(target))) {
            if (source.distanceToSqr(target) <= rangeSqr) {
                grantFearWithCooldown(target, profile.level(), profile.durationTicks());
            }
        }
    }

    private static KizukiFearProfile getKizukiFearProfile(LivingEntity entity) {
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        if (entityId == null || !KNY_NAMESPACE.equals(entityId.getNamespace())) {
            return null;
        }

        return switch (entityId.getPath()) {
            case "muzan", "muzan_2", "tanjiro_demon" -> new KizukiFearProfile(9, 15 * 20);
            case "kokushibo" -> new KizukiFearProfile(8, 9 * 20);
            case "doma" -> new KizukiFearProfile(7, 8 * 20);
            case "akaza" -> new KizukiFearProfile(6, 7 * 20);
            case "nakime", "zohakuten" -> new KizukiFearProfile(5, 5 * 20);
            case "gyokko", "gyokko_2" -> new KizukiFearProfile(4, 4 * 20);
            case "daki" -> new KizukiFearProfile(3, 3 * 20);
            case "gyutaro" -> new KizukiFearProfile(4, 3 * 20);
            case "enmu" -> new KizukiFearProfile(2, 2 * 20);
            case "rokuro", "rui", "mukago", "kamanue", "kyogai" -> new KizukiFearProfile(1, 2 * 20);
            default -> null;
        };
    }

    private static boolean canReceiveKizukiPresenceFear(LivingEntity target) {
        return canReceiveKizukiFearBase(target) && !Damager.isDemon(target);
    }

    private static boolean canReceiveKizukiAggroFear(LivingEntity target) {
        return canReceiveKizukiFearBase(target);
    }

    private static boolean canReceiveKizukiFearBase(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof ServerPlayer serverPlayer) {
            return serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    private static void enforcePlayerParalysis(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PLAYER_LOCKED_KEY)) {
            data.putBoolean(PLAYER_LOCKED_KEY, true);
            data.putDouble(PLAYER_LOCK_X_KEY, player.getX());
            data.putDouble(PLAYER_LOCK_Z_KEY, player.getZ());
        }

        double lockX = data.getDouble(PLAYER_LOCK_X_KEY);
        double lockZ = data.getDouble(PLAYER_LOCK_Z_KEY);

        stopHorizontalMotionAndJump(player);
        player.setSprinting(false);

        if (player.level().isClientSide()) {
            player.setPos(lockX, player.getY(), lockZ);
            return;
        }

        double horizontalDistanceSqr = player.distanceToSqr(lockX, player.getY(), lockZ);
        if (horizontalDistanceSqr > 0.0004D) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(lockX, player.getY(), lockZ, player.getYRot(), player.getXRot());
            } else {
                player.setPos(lockX, player.getY(), lockZ);
            }
        }
    }

    private static void enforceMobParalysis(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(MOB_NO_AI_STORED_KEY)) {
            data.putBoolean(MOB_NO_AI_STORED_KEY, true);
            data.putBoolean(MOB_ORIGINAL_NO_AI_KEY, mob.isNoAi());
        }

        mob.setNoAi(true);
        mob.getNavigation().stop();
        stopHorizontalMotionAndJump(mob);
    }

    private static void stopHorizontalMotionAndJump(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0D, Math.min(0.0D, movement.y), 0.0D);
        entity.setJumping(false);
    }

    private static void ensureFearPhase(LivingEntity entity, int fearLevel) {
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(FEAR_PHASE_ACTIVE_KEY) || data.getInt(FEAR_PHASE_LEVEL_KEY) != fearLevel) {
            data.putBoolean(FEAR_PHASE_ACTIVE_KEY, true);
            data.putInt(FEAR_PHASE_START_TICK_KEY, entity.tickCount);
            data.putInt(FEAR_PHASE_LEVEL_KEY, fearLevel);
        }
    }

    private static int getFearPhaseElapsed(LivingEntity entity, int fearLevel) {
        ensureFearPhase(entity, fearLevel);
        return Math.max(0, entity.tickCount - entity.getPersistentData().getInt(FEAR_PHASE_START_TICK_KEY));
    }

    private static int getFreezeTicks(int fearLevel) {
        int curveLevel = Math.min(MAX_MOVEMENT_CURVE_LEVEL, Math.max(1, fearLevel));
        if (curveLevel < FREEZE_START_LEVEL) {
            return 0;
        }
        return Math.round((curveLevel - FREEZE_START_LEVEL + 1) * (MAX_FREEZE_TICKS / 5.0F));
    }

    private static int getSlowTicks(int fearLevel) {
        int curveLevel = Math.min(MAX_MOVEMENT_CURVE_LEVEL, Math.max(1, fearLevel));
        return MIN_SLOW_TICKS + Math.round((curveLevel - 1) * (MAX_SLOW_TICKS - MIN_SLOW_TICKS) / 8.0F);
    }

    private static void applyFearSlowdown(LivingEntity entity, int phaseElapsed, int fearLevel) {
        int slowTicks = getSlowTicks(fearLevel);
        int slowElapsed = phaseElapsed - getFreezeTicks(fearLevel);
        double multiplier = 1.0D;
        if (slowElapsed < slowTicks) {
            double progress = (slowElapsed + 1.0D) / slowTicks;
            multiplier = MIN_SLOW_MOVEMENT_MULTIPLIER
                    + (1.0D - MIN_SLOW_MOVEMENT_MULTIPLIER) * Math.min(1.0D, progress);
        }

        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        movementSpeed.removeModifier(FEAR_SLOWDOWN_MODIFIER_UUID);
        if (multiplier < 1.0D) {
            movementSpeed.addTransientModifier(new AttributeModifier(
                    FEAR_SLOWDOWN_MODIFIER_UUID,
                    "Fear movement slowdown",
                    multiplier - 1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeFearSlowdown(LivingEntity entity) {
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(FEAR_SLOWDOWN_MODIFIER_UUID);
        }
    }

    private static void clearFearMovementState(LivingEntity entity) {
        clearParalysis(entity);
        removeFearSlowdown(entity);
        CompoundTag data = entity.getPersistentData();
        data.remove(FEAR_PHASE_ACTIVE_KEY);
        data.remove(FEAR_PHASE_START_TICK_KEY);
        data.remove(FEAR_PHASE_LEVEL_KEY);
    }

    private static void clearParalysis(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (entity instanceof Player) {
            data.remove(PLAYER_LOCKED_KEY);
            data.remove(PLAYER_LOCK_X_KEY);
            data.remove(PLAYER_LOCK_Z_KEY);
        }

        if (entity instanceof Mob mob && data.getBoolean(MOB_NO_AI_STORED_KEY)) {
            mob.setNoAi(data.getBoolean(MOB_ORIGINAL_NO_AI_KEY));
            data.remove(MOB_NO_AI_STORED_KEY);
            data.remove(MOB_ORIGINAL_NO_AI_KEY);
        }
    }

    private static void keepHotbarCoolingDown(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty()) {
                player.getCooldowns().addCooldown(stack.getItem(), HOTBAR_COOLDOWN_TICKS);
            }
        }
    }

    private static void applyHiddenBaseCoolTime(Player player) {
        MobEffect coolTime = KnYEffects.getCoolTimeEffect();
        if (coolTime != null) {
            player.addEffect(new MobEffectInstance(coolTime, HOTBAR_COOLDOWN_TICKS, 0, false, false, false));
        }
    }
}
