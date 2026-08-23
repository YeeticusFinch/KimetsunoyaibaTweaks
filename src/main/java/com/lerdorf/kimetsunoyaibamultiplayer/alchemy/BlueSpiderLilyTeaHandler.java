package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class BlueSpiderLilyTeaHandler {
    private static final String TEA_DAYS_KEY = "KnYMpBlueSpiderLilyTeaDays";
    private static final String LAST_TEA_DAY_KEY = "KnYMpBlueSpiderLilyTeaLastDay";
    private static final int IMMUNITY_TEA_DAYS = 100;
    private static final int MIN_DURATION_TICKS = 20 * 60 * 2;
    private static final int MAX_DURATION_TICKS = 20 * 60 * 5;

    private BlueSpiderLilyTeaHandler() {
    }

    public static boolean isTea(ItemStack stack) {
        return BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:blue_spider_lily_tea");
    }

    public static int getTeaDays(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return 0;
        }
        return Math.max(0, player.getPersistentData().getInt(TEA_DAYS_KEY));
    }

    public static void applyTea(LivingEntity entity) {
        int resistanceLevel = 1;
        if (entity instanceof ServerPlayer player) {
            resistanceLevel = markTeaDrunkToday(player);
        }

        int durationTicks = MIN_DURATION_TICKS + entity.getRandom().nextInt((MAX_DURATION_TICKS - MIN_DURATION_TICKS) + 1);
        entity.addEffect(new MobEffectInstance(
            ModEffects.SUNLIGHT_RESISTANCE.get(),
            durationTicks,
            Math.max(0, resistanceLevel - 1),
            false,
            false,
            true
        ));
    }

    public static boolean shouldAdvanceSunlightBurn(LivingEntity entity) {
        int level = getActiveResistanceLevel(entity);
        if (level <= 0) {
            return true;
        }
        if (level >= IMMUNITY_TEA_DAYS) {
            return false;
        }

        int interval = Math.max(1, level + 1);
        return Math.floorMod(entity.level().getGameTime() + entity.getId(), interval) == 0;
    }

    public static void clearSkippedSunlightFire(LivingEntity entity) {
        if (getActiveResistanceLevel(entity) > 0) {
            entity.clearFire();
        }
    }

    private static int markTeaDrunkToday(ServerPlayer player) {
        long currentDay = player.level().getDayTime() / 24000L;
        long lastTeaDay = player.getPersistentData().contains(LAST_TEA_DAY_KEY)
            ? player.getPersistentData().getLong(LAST_TEA_DAY_KEY)
            : Long.MIN_VALUE;
        int teaDays = Math.max(0, player.getPersistentData().getInt(TEA_DAYS_KEY));

        if (lastTeaDay != currentDay) {
            teaDays = Math.min(IMMUNITY_TEA_DAYS, teaDays + 1);
            player.getPersistentData().putInt(TEA_DAYS_KEY, teaDays);
            player.getPersistentData().putLong(LAST_TEA_DAY_KEY, currentDay);
        }

        if (teaDays >= IMMUNITY_TEA_DAYS) {
            AlchemyMedicineHandler.grantSunlightImmunity(player);
        }

        return Math.max(1, teaDays);
    }

    private static int getActiveResistanceLevel(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(ModEffects.SUNLIGHT_RESISTANCE.get());
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
