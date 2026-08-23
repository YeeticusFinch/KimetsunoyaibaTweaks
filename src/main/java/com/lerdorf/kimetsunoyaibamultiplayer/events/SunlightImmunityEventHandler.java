package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.AlchemyMedicineHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunlightImmunityHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SunlightImmunityEventHandler {
    private SunlightImmunityEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (shouldBlockSunlightDamage(event.getEntity(), event.getSource())) {
            event.getEntity().clearFire();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (shouldBlockSunlightDamage(event.getEntity(), event.getSource())) {
            event.getEntity().clearFire();
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (shouldBlockSunlightDamage(event.getEntity(), event.getSource())) {
            event.getEntity().clearFire();
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (SunlightImmunityHelper.isBaseSunBreathingSunlightImmunityDisabled()
            && event.getEntity() instanceof ServerPlayer player
            && SunlightImmunityHelper.isOvercomeSunlightAdvancement(event.getAdvancement())) {
            SunlightImmunityHelper.revokeBaseOvercomeSunlightAdvancement(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
            || event.player.level().isClientSide()
            || !(event.player instanceof ServerPlayer player)
            || !SunlightImmunityHelper.isBaseSunBreathingSunlightImmunityDisabled()
            || player.tickCount % 20 != 0) {
            return;
        }
        if (SunlightImmunityHelper.hasBaseOvercomeSunlightAdvancement(player)) {
            SunlightImmunityHelper.revokeBaseOvercomeSunlightAdvancement(player);
        }
    }

    private static boolean shouldBlockSunlightDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (entity == null
            || entity.level().isClientSide()
            || !AlchemyMedicineHandler.hasSunlightImmunity(entity)
            || isKnownNonSunlightDamageProcedure()) {
            return false;
        }

        if (isSunlightBurnProcedureDamage()) {
            return SunlightImmunityHelper.isInSunlightExposureIgnoringRain(entity);
        }

        return SunlightImmunityHelper.isInSunlightExposure(entity)
            && SunlightImmunityHelper.isBaseSunlightGenericDamage(source);
    }

    private static boolean isSunlightBurnProcedureDamage() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if ("net.mcreator.kimetsunoyaiba.procedures.NikkoyakeProcedure".equals(className)) {
                return true;
            }
            if ("net.mcreator.kimetsunoyaiba.procedures.TestNikkoProcedure".equals(className)) {
                return true;
            }
            if ("com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler".equals(className)
                && "tickSunlightBurn".equals(methodName)) {
                return true;
            }
            if ("com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity".equals(className)
                && "tickSunlightBurn".equals(methodName)) {
                return true;
            }
            if ("com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity".equals(className)
                && "tickDemonizedSunlightBurn".equals(methodName)) {
                return true;
            }
            if ("com.lerdorf.kimetsunoyaibamultiplayer.MtFujikasaneDimensionDataHandler".equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKnownNonSunlightDamageProcedure() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if ("net.mcreator.kimetsunoyaiba.potion.FujinohanaMobEffect".equals(className)) {
                return true;
            }
            if ("net.mcreator.kimetsunoyaiba.procedures.PoisonOfFujiflowerProcedure".equals(className)) {
                return true;
            }
            if ("net.mcreator.kimetsunoyaiba.procedures.GivePoisonProcedure".equals(className)) {
                return true;
            }
        }
        return false;
    }
}
