package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonTransformationHandler {
    public static final String ONI_TRANSFORM_TAG = "oni_transform";
    public static final String DEMON_TRANSFORMATION_TAG = "demon_transformation";
    public static final String MUZAN_BLOOD_CONSUMED_KEY = "KnYMpMuzanBloodConsumed";

    private static final String DATA_ACTIVE = "KnYMpCustomDemonTransformActive";
    private static final String DATA_LAST_DURATION = "KnYMpCustomDemonTransformLastDuration";
    private static final String DATA_LAST_AMPLIFIER = "KnYMpCustomDemonTransformLastAmplifier";
    private static final int MIN_BASE_DURATION = 600;
    private static final int MAX_BASE_DURATION = 2400;
    private static final int MIN_RANK_BONUS_DURATION = 3000;
    private static final int MAX_RANK_BONUS_DURATION = 6000;
    private static final int MAX_TOTAL_DURATION = 36000;
    private static final int DEBUFF_REFRESH_TICKS = 40;
    private static final double TRANSFORMATION_AGGRO_CLEAR_RADIUS = 48.0D;
    private static final List<ResourceLocation> SLAYER_RANKS_ASCENDING = List.of(
        ResourceLocation.parse("kimetsunoyaiba:mizunoto"),
        ResourceLocation.parse("kimetsunoyaiba:mizunoe"),
        ResourceLocation.parse("kimetsunoyaiba:kanoto"),
        ResourceLocation.parse("kimetsunoyaiba:kanoe"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoto"),
        ResourceLocation.parse("kimetsunoyaiba:tsuchinoe"),
        ResourceLocation.parse("kimetsunoyaiba:hinoto"),
        ResourceLocation.parse("kimetsunoyaiba:hinoe"),
        ResourceLocation.parse("kimetsunoyaiba:kinoto"),
        ResourceLocation.parse("kimetsunoyaiba:kinoe"),
        ResourceLocation.parse("kimetsunoyaiba:hashira")
    );

    private DemonTransformationHandler() {
    }

    public static boolean isCustomDemonInitiationEnabled() {
        return CustomProgressionConfig.customDemonInitiation != null && CustomProgressionConfig.customDemonInitiation.get();
    }

    public static boolean isTransforming(Player player) {
        return player != null && (player.getPersistentData().getBoolean(DATA_ACTIVE)
            || player.getTags().contains(ONI_TRANSFORM_TAG)
            || player.hasEffect(ModEffects.DEMON_TRANSFORMATION.get()));
    }

    public static boolean hasTransformationTag(Player player) {
        return player != null && (player.getTags().contains(ONI_TRANSFORM_TAG) || player.getTags().contains(DEMON_TRANSFORMATION_TAG));
    }

    public static boolean hasTransformationTag(LivingEntity entity) {
        return entity instanceof Player player && hasTransformationTag(player);
    }

    public static boolean shouldSuppressAggro(Mob attacker, LivingEntity target) {
        if (!(target instanceof Player player) || !hasTransformationTag(target)) {
            return false;
        }

        boolean relevantAttacker = Damager.isDemon(attacker)
            || Damager.isDemonSlayer(attacker)
            || EntityTagHelper.isDemon(attacker)
            || EntityTagHelper.isDemonSlayer(attacker)
            || EntityTagHelper.isHashira(attacker);
        if (!relevantAttacker) {
            return false;
        }

        return !hasBeenProvoked(attacker, player);
    }

    public static boolean consumeCustomBlood(ServerPlayer player, ItemStack stack) {
        if (!isCustomDemonInitiationEnabled() || player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (Damager.isDemon(player)) {
            consumeBaseMuzanBlood(player, 1);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return true;
        }
        if (isTransforming(player)) {
            increaseTransformationLevel(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return true;
        }
        if (!startTransformation(player)) {
            return false;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static boolean startTransformation(ServerPlayer player) {
        return startTransformation(player, false);
    }

    public static boolean startTransformationFromProposition(ServerPlayer player) {
        return startTransformation(player, true);
    }

    private static boolean startTransformation(ServerPlayer player, boolean grantGraceResistance) {
        if (!isCustomDemonInitiationEnabled() || player == null || !player.isAlive() || Damager.isDemon(player) || isTransforming(player)) {
            return false;
        }

        int duration = calculateTransformationDuration(player);
        player.getPersistentData().putBoolean(DATA_ACTIVE, true);
        player.getPersistentData().putInt(DATA_LAST_DURATION, duration);
        player.getPersistentData().putInt(DATA_LAST_AMPLIFIER, 0);
        player.addTag(ONI_TRANSFORM_TAG);
        player.addTag(DEMON_TRANSFORMATION_TAG);
        player.addEffect(new MobEffectInstance(ModEffects.DEMON_TRANSFORMATION.get(), duration, 0, false, true, true));
        if (grantGraceResistance) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, true, true));
        }
        applyTransformationDebuffs(player, 0);
        clearNearbyDemonAggro(player);
        return true;
    }

    public static void cancelTransformation(ServerPlayer player) {
        if (player == null) {
            return;
        }
        clearTransformationState(player);
    }

    private static void completeTransformation(ServerPlayer player) {
        int transformationLevel = getTransformationLevel(player);
        clearTransformationState(player);
        consumeBaseMuzanBlood(player, transformationLevel);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        boolean active = player.getPersistentData().getBoolean(DATA_ACTIVE);
        MobEffectInstance effect = player.getEffect(ModEffects.DEMON_TRANSFORMATION.get());
        if (!active) {
            return;
        }

        if (effect != null) {
            if (effect.getDuration() <= 1) {
                completeTransformation(player);
                return;
            }
            player.getPersistentData().putInt(DATA_LAST_DURATION, effect.getDuration());
            player.getPersistentData().putInt(DATA_LAST_AMPLIFIER, effect.getAmplifier());
            player.addTag(ONI_TRANSFORM_TAG);
            player.addTag(DEMON_TRANSFORMATION_TAG);
            applyTransformationDebuffs(player, effect.getAmplifier());
            return;
        }

        int lastDuration = player.getPersistentData().getInt(DATA_LAST_DURATION);
        if (lastDuration <= 1) {
            completeTransformation(player);
        } else {
            cancelTransformation(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && isTransforming(player)) {
            cancelTransformation(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            cancelTransformation(player);
        }
    }

    private static void applyTransformationDebuffs(ServerPlayer player, int amplifier) {
        int debuffLevel = Math.max(0, amplifier);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_REFRESH_TICKS, debuffLevel, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_REFRESH_TICKS, debuffLevel, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, DEBUFF_REFRESH_TICKS, debuffLevel, false, true, true));
    }

    private static void clearTransformationState(ServerPlayer player) {
        player.getPersistentData().remove(DATA_ACTIVE);
        player.getPersistentData().remove(DATA_LAST_DURATION);
        player.getPersistentData().remove(DATA_LAST_AMPLIFIER);
        player.removeTag(ONI_TRANSFORM_TAG);
        player.removeTag(DEMON_TRANSFORMATION_TAG);
        player.removeEffect(ModEffects.DEMON_TRANSFORMATION.get());
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.HUNGER);
    }

    private static int calculateTransformationDuration(ServerPlayer player) {
        int duration = Mth.nextInt(player.getRandom(), MIN_BASE_DURATION, MAX_BASE_DURATION);
        int rankCount = getCompletedSlayerRanks(player);
        for (int i = 0; i < rankCount && duration < MAX_TOTAL_DURATION; i++) {
            duration += Mth.nextInt(player.getRandom(), MIN_RANK_BONUS_DURATION, MAX_RANK_BONUS_DURATION);
        }
        return Math.min(duration, MAX_TOTAL_DURATION);
    }

    private static int getCompletedSlayerRanks(ServerPlayer player) {
        int count = 0;
        for (ResourceLocation rankId : SLAYER_RANKS_ASCENDING) {
            if (hasAdvancement(player, rankId)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void increaseTransformationLevel(ServerPlayer player) {
        MobEffectInstance currentEffect = player.getEffect(ModEffects.DEMON_TRANSFORMATION.get());
        if (currentEffect == null) {
            return;
        }

        int newAmplifier = currentEffect.getAmplifier() + 1;
        int duration = Math.max(1, currentEffect.getDuration());
        player.getPersistentData().putInt(DATA_LAST_DURATION, duration);
        player.getPersistentData().putInt(DATA_LAST_AMPLIFIER, newAmplifier);
        player.addEffect(new MobEffectInstance(ModEffects.DEMON_TRANSFORMATION.get(), duration, newAmplifier, false, true, true));
        applyTransformationDebuffs(player, newAmplifier);
    }

    private static int getTransformationLevel(ServerPlayer player) {
        MobEffectInstance currentEffect = player.getEffect(ModEffects.DEMON_TRANSFORMATION.get());
        if (currentEffect != null) {
            return currentEffect.getAmplifier() + 1;
        }
        return Math.max(1, player.getPersistentData().getInt(DATA_LAST_AMPLIFIER) + 1);
    }

    public static void consumeBaseMuzanBlood(ServerPlayer player, int count) {
        int consumedCount = Math.max(1, count);
        for (int i = 0; i < consumedCount; i++) {
            ItemStack baseBlood = new ItemStack(KimetsunoyaibaModItems.BLOOD_OF_MUZAN.get());
            baseBlood.finishUsingItem(player.level(), player);
        }
        addTrackedMuzanBlood(player, consumedCount);
    }

    public static int getTrackedMuzanBlood(Player player) {
        return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(MUZAN_BLOOD_CONSUMED_KEY));
    }

    public static void addTrackedMuzanBlood(Player player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        player.getPersistentData().putInt(MUZAN_BLOOD_CONSUMED_KEY, getTrackedMuzanBlood(player) + amount);
    }

    public static void resetTrackedMuzanBlood(Player player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putInt(MUZAN_BLOOD_CONSUMED_KEY, 0);
    }

    private static void clearNearbyDemonAggro(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(TRANSFORMATION_AGGRO_CLEAR_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, searchBox, Mob::isAlive)) {
            if (!EntityTagHelper.isDemon(mob) && !Damager.isDemon(mob)) {
                continue;
            }
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (mob.getLastHurtByMob() == player) {
                mob.setLastHurtByMob(null);
            }
            mob.getNavigation().stop();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        }
    }

    private static boolean hasBeenProvoked(Mob attacker, Player player) {
        return attacker.getLastHurtByMob() == player
            || player.getLastHurtByMob() == attacker
            || DamageTracker.hasDamageHistory(attacker, player);
    }
}
