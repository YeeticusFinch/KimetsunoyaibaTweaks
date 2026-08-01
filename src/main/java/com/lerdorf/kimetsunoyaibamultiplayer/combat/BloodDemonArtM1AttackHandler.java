package com.lerdorf.kimetsunoyaibamultiplayer.combat;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonSleepExecutionHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ImpactParticleOptions;
import com.lerdorf.kimetsunoyaibamultiplayer.util.AttackDamageHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class BloodDemonArtM1AttackHandler {
    public static final int NEZUKO_COLOR = 0xFF3E71;
    public static final int SWAMP_COLOR = 0x66AE93;

    private static final String LAST_ATTACK_TICK_TAG = "KnYBdaM1AttackTick";
    private static final String MARTIAL_ART_INDEX_TAG = "KnYBdaM1MartialArtIndex";
    private static final String CLAW_ART_INDEX_TAG = "KnYBdaM1ClawIndex";
    private static final String SWORD_LEFT_TAG = "KnYBdaM1SwordLeft";
    private static final float DEFAULT_BOX_SIZE = 5.0F;
    private static final double WEAK_DEFENSE = 3.0D;

    private static final String[] MARTIAL_ART_ANIMATIONS = {
        "punch_right",
        "punch_left",
        "kick_right",
        "kick_left"
    };

    private static final String[] CLAW_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "left_sword_to_left",
        "left_sword_to_right",
        "sword_overhead",
        "left_sword_overhead"
    };

    private BloodDemonArtM1AttackHandler() {
    }

    public static boolean performNezukoAttack(LivingEntity attacker, UUID excludedTargetId) {
        if (!markHandledThisTick(attacker)) {
            return false;
        }

        if (attacker.getRandom().nextBoolean()) {
            performMartialArtsAttack(attacker, 4, NEZUKO_COLOR, excludedTargetId);
        } else {
            performClawAttack(attacker, 4, excludedTargetId);
        }
        return true;
    }

    public static boolean performSwampAttack(LivingEntity attacker, UUID excludedTargetId) {
        if (!markHandledThisTick(attacker)) {
            return false;
        }

        performMartialArtsAttack(attacker, 2, SWAMP_COLOR, excludedTargetId);
        return true;
    }

    public static boolean performNichirinLikeSlashAttack(LivingEntity attacker, UUID excludedTargetId) {
        if (!markHandledThisTick(attacker)) {
            return false;
        }

        String animation = chooseNichirinLikeAnimation(attacker);
        playAnimation(attacker, animation, 10);
        ModNetworking.sendToAllClients(new MobSwordSlashPacket(attacker.getUUID(), normalizeSlashAnimation(animation), 0));
        attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        applyWeakAttackState(attacker);
        int hitCount = damageTargets(attacker, 0.0D, excludedTargetId);

        if (Config.logDebug && hitCount > 0) {
            Log.debug("[BloodDemonArtM1AttackHandler] Slash M1 hit {} entities", hitCount);
        }
        return true;
    }

    private static void performMartialArtsAttack(LivingEntity attacker, int level, int color, UUID excludedTargetId) {
        int index = Math.floorMod(attacker.getPersistentData().getInt(MARTIAL_ART_INDEX_TAG), MARTIAL_ART_ANIMATIONS.length);
        String animation = MARTIAL_ART_ANIMATIONS[index];
        attacker.getPersistentData().putInt(MARTIAL_ART_INDEX_TAG, (index + 1) % MARTIAL_ART_ANIMATIONS.length);

        playAnimation(attacker, animation, 10);
        attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnImpact(attacker, color);
        applyWeakAttackState(attacker);
        damageTargets(attacker, 0.18D + 0.12D * level, excludedTargetId);
    }

    private static void performClawAttack(LivingEntity attacker, int level, UUID excludedTargetId) {
        int index = Math.floorMod(attacker.getPersistentData().getInt(CLAW_ART_INDEX_TAG), CLAW_ANIMATIONS.length);
        String animation = CLAW_ANIMATIONS[index];
        attacker.getPersistentData().putInt(CLAW_ART_INDEX_TAG, (index + 1) % CLAW_ANIMATIONS.length);

        playAnimation(attacker, animation, 10);
        attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        ModNetworking.sendToAllClients(new MobSwordSlashPacket(attacker.getUUID(), normalizeSlashAnimation(animation), 0));
        applyWeakAttackState(attacker);
        damageTargets(attacker, 0.18D + 0.12D * level, excludedTargetId);
    }

    private static int damageTargets(LivingEntity attacker, double knockback, UUID excludedTargetId) {
        Vec3 eyePos = attacker.position().add(0.0D, attacker.getEyeHeight(), 0.0D);
        Vec3 lookVec = attacker.getLookAngle().normalize();
        Vec3 frontPos = eyePos.add(lookVec.scale(DEFAULT_BOX_SIZE / 1.5F));
        AABB attackBox = new AABB(
            frontPos.add(-DEFAULT_BOX_SIZE / 2.0F, -DEFAULT_BOX_SIZE / 2.0F, -DEFAULT_BOX_SIZE / 2.0F),
            frontPos.add(DEFAULT_BOX_SIZE / 2.0F, DEFAULT_BOX_SIZE / 2.0F, DEFAULT_BOX_SIZE / 2.0F)
        );

        float damage = AttackDamageHelper.getM1AoeDamageByStrength(attacker);
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            entity -> entity != attacker && entity.isAlive() && (excludedTargetId == null || !entity.getUUID().equals(excludedTargetId)));

        int hitCount = 0;
        for (LivingEntity target : targets) {
            if (DemonSleepExecutionHandler.isSleepingInBed(target) && !Damager.isDemon(target)) {
                DemonSleepExecutionHandler.executeSleepAttack(attacker, target);
                hitCount++;
                continue;
            }

            if (!EnhancedLoveForms.isTargetable(attacker, target)) {
                continue;
            }

            Damager.hurt(attacker, target, damage, false, true);
            if (knockback > 0.0D) {
                target.knockback(knockback, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            }
            hitCount++;
        }
        return hitCount;
    }

    private static void spawnImpact(LivingEntity attacker, int color) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        Vec3 impactPos = attacker.position()
            .add(0.0D, attacker.getEyeHeight(), 0.0D)
            .add(attacker.getLookAngle().normalize().scale(2.0D));
        serverLevel.sendParticles(
            new ImpactParticleOptions(r, g, b, 1.0F),
            impactPos.x,
            impactPos.y,
            impactPos.z,
            1,
            attacker.getLookAngle().x * 0.02D,
            attacker.getLookAngle().y * 0.02D,
            attacker.getLookAngle().z * 0.02D,
            0.12D
        );
    }

    private static void applyWeakAttackState(LivingEntity attacker) {
        GuardStateHelper.setWeakAttackState(attacker, WEAK_DEFENSE);
        AbilityScheduler.scheduleOnce(attacker, () -> GuardStateHelper.clearGuardState(attacker), 10);
    }

    private static void playAnimation(LivingEntity entity, String animation, int duration) {
        AnimationHelper.playAnimation(entity, animation, duration);
    }

    private static String chooseNichirinLikeAnimation(LivingEntity attacker) {
        if (attacker.getRandom().nextInt(100) < 8) {
            return "sword_overhead";
        }

        boolean lastWasLeft = attacker.getPersistentData().getBoolean(SWORD_LEFT_TAG);
        attacker.getPersistentData().putBoolean(SWORD_LEFT_TAG, !lastWasLeft);
        return lastWasLeft ? "sword_to_right" : "sword_to_left";
    }

    private static boolean markHandledThisTick(LivingEntity attacker) {
        if (attacker == null || !(attacker.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        long now = serverLevel.getGameTime();
        long lastAttack = attacker.getPersistentData().getLong(LAST_ATTACK_TICK_TAG);
        if (lastAttack == now) {
            return false;
        }

        attacker.getPersistentData().putLong(LAST_ATTACK_TICK_TAG, now);
        return true;
    }

    private static String normalizeSlashAnimation(String animation) {
        if ("left_sword_to_left".equals(animation)) {
            return "sword_to_left";
        }
        if ("left_sword_to_right".equals(animation)) {
            return "sword_to_right";
        }
        if ("left_sword_overhead".equals(animation) || "double_sword_overhead".equals(animation)) {
            return "sword_overhead";
        }
        return animation;
    }
}
