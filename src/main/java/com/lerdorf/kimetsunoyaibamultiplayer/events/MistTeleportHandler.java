package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class MistTeleportHandler {
    private static final Map<UUID, Long> lastTeleportTime = new HashMap<>();
    private static final long TELEPORT_COOLDOWN_MS = 2000;

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.getPersistentData().getBoolean("mist_7th_teleport_active")) {
                performSafeTeleport(attacker);
            }
        }
    }

    public static void performSafeTeleport(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // Check initial cooldown (prevents teleport immediately after activating 7th form)
        if (entity.getPersistentData().contains("mist_7th_teleport_cooldown")) {
            int cooldownTick = entity.getPersistentData().getInt("mist_7th_teleport_cooldown");
            if (entity.tickCount < cooldownTick) {
                Log.debug("[TELEPORT DEBUG] Initial cooldown active, teleport blocked");
                return;
            }
            // Remove cooldown tag once it expires
            entity.getPersistentData().remove("mist_7th_teleport_cooldown");
        }

        UUID playerId = entity.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastTeleportTime.get(playerId);
        if (lastTime != null && (currentTime - lastTime) < TELEPORT_COOLDOWN_MS) return;
        lastTeleportTime.put(playerId, currentTime);
        Vec3 lookVec = entity.getLookAngle();
        Vec3 startPos = entity.getEyePosition();
        float range = 15.0f;
        Vec3 targetPos = startPos.add(lookVec.scale(range));
        BlockHitResult hitResult = serverLevel.clip(new ClipContext(startPos.add(0, entity.getEyeHeight(), 0), targetPos.add(0, entity.getEyeHeight(), 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hitResult.getLocation();
            targetPos = startPos.add(hitPos.subtract(startPos).normalize().scale(Math.max(0, startPos.distanceTo(hitPos) - 1.0)));
        }
        entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        ParticleHelper.spawnParticleLine(serverLevel, startPos, targetPos, ModParticles.MIST_PARTICLE.get(), 30);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
