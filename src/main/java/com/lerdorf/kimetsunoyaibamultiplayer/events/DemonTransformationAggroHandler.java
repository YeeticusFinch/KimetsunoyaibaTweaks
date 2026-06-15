package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonTransformationAggroHandler {
    private DemonTransformationAggroHandler() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        LivingEntity target = event.getNewTarget();
        if (target != null && DemonTransformationHandler.shouldSuppressAggro(mob, target)) {
            event.setCanceled(true);
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && DemonTransformationHandler.shouldSuppressAggro(mob, target)) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            if (target.getLastHurtByMob() == mob) {
                target.setLastHurtByMob(null);
            }
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player player) || !DemonTransformationHandler.isTransforming(player)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        LivingEntity livingAttacker;
        if (attacker instanceof LivingEntity living) {
            livingAttacker = living;
        } else {
            attacker = event.getSource().getDirectEntity();
            if (!(attacker instanceof LivingEntity directLiving)) {
                return;
            }
            livingAttacker = directLiving;
        }

        if (Damager.isDemon(livingAttacker) || EntityTagHelper.isDemon(livingAttacker)) {
            event.setCanceled(true);
            if (livingAttacker instanceof Mob mob) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                mob.getNavigation().stop();
            }
        }
    }
}
