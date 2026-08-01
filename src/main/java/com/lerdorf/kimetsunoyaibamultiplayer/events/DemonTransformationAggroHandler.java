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
            // Do not cancel or clear revenge memory while vanilla HurtByTargetGoal.start()
            // is assigning its target. That goal may still alert nearby mobs with the
            // just-assigned target; clearing it here can leave vanilla with a null alert target.
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
            clearSuppressedAggro(mob, target);
            return;
        }

        LivingEntity revengeTarget = mob.getLastHurtByMob();
        if (revengeTarget != null && DemonTransformationHandler.shouldSuppressAggro(mob, revengeTarget)) {
            clearSuppressedAggro(mob, revengeTarget);
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
                clearSuppressedAggro(mob, player);
            }
        }
    }

    private static void clearSuppressedAggro(Mob mob, LivingEntity target) {
        if (mob == null) {
            return;
        }

        if (mob.getTarget() == target) {
            mob.setTarget(null);
        }
        if (mob.getLastHurtByMob() == target) {
            mob.setLastHurtByMob(null);
        }
        if (target != null && target.getLastHurtByMob() == mob) {
            target.setLastHurtByMob(null);
        }
        mob.getNavigation().stop();
    }
}
