package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DemonSlayerAggroHandler {
    private static final String DEMON_AGGRO_ADDED_TAG = "knymp_added_demon_aggro_goal";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!EntityTagHelper.isDemonSlayer(mob)) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(DEMON_AGGRO_ADDED_TAG)) {
            return;
        }

        // BreathingSlayerEntity already targets demons via its own goals.
        if (mob instanceof BreathingSlayerEntity) {
            data.putBoolean(DEMON_AGGRO_ADDED_TAG, true);
            return;
        }

        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            mob,
            LivingEntity.class,
            10,
            true,
            false,
            DemonSlayerAggroHandler::isDemonTarget
        ));

        data.putBoolean(DEMON_AGGRO_ADDED_TAG, true);
    }

    private static boolean isDemonTarget(LivingEntity target) {
        if (target == null) {
            return false;
        }

        if (EntityTagHelper.isDemon(target) || EntityTagHelper.isTwelveKizuki(target)) {
            return true;
        }

        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(target);
        return entityId != null && EntityCategorization.isDemon(entityId);
    }
}
