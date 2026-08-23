package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.util.WisteriaResistanceHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WisteriaResistanceEventHandler {
    private WisteriaResistanceEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide() || !WisteriaResistanceHelper.hasResistance(entity)) {
            return;
        }
        if (isWisteriaPoisonDamageProcedure()) {
            event.setAmount(WisteriaResistanceHelper.reduceWisteriaDamage(entity, event.getAmount()));
        }
    }

    private static boolean isWisteriaPoisonDamageProcedure() {
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
