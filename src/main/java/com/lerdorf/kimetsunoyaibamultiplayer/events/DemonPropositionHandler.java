package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.CloseDemonPropositionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.OpenDemonPropositionPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetDemonPropositionStatePacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class DemonPropositionHandler {
    private static final int PROPOSITION_DURATION_TICKS = 23 * 20;
    private static final int PROPOSITION_MENU_DELAY_TICKS = 3 * 20;
    private static final int PROPOSITION_EFFECT_DURATION_TICKS = PROPOSITION_DURATION_TICKS + 20;
    private static final int TRANSFORMATION_REGEN_TICKS = 20 * 30;
    private static final int TRANSFORMATION_DEBUFF_TICKS = 20 * 20;
    private static final String CHOKE_ANIMATION = KimetsunoyaibaMultiplayer.MODID + ":choke";
    private static final String CHOKED_ANIMATION = KimetsunoyaibaMultiplayer.MODID + ":choked";
    private static final String CHOKE_SWORD_ANIMATION = KimetsunoyaibaMultiplayer.MODID + ":choke_sword";
    private static final String CHOKED_SWORD_ANIMATION = KimetsunoyaibaMultiplayer.MODID + ":choked_sword";
    private static final String STOP_MOB_ANIMATION = "__stop__";
    private static final String BYPASS_FATAL_PROPOSITION_KEY = "KnYMpBypassDemonProposition";
    private static final double TARGET_DISTANCE = 1.0D;
    private static final double ANIMATION_BROADCAST_RADIUS = 64.0D;

    private static final Map<UUID, PropositionSession> SESSIONS_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PARTICIPANT_TO_PLAYER = new ConcurrentHashMap<>();

    private DemonPropositionHandler() {
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (getSession(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (getSession(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();

        PropositionSession activeSession = getSession(target);
        if (activeSession != null) {
            event.setCanceled(true);
            return;
        }

        if (!(target instanceof ServerPlayer player) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.getPersistentData().getBoolean(BYPASS_FATAL_PROPOSITION_KEY)
            || Damager.isDemon(player)
            || DemonTransformationHandler.isTransforming(player)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !isEligibleKizuki(attacker)) {
            return;
        }
        if (attacker.level() != player.level() || !attacker.isAlive() || hasSession(attacker) || hasSession(player)) {
            return;
        }

        float finalDamage = event.getAmount();
        if (finalDamage <= 0.0F || player.getHealth() - finalDamage > 0.0F) {
            return;
        }

        event.setCanceled(true);
        startSession(attacker, player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        PropositionSession session = SESSIONS_BY_PLAYER.get(player.getUUID());
        if (session == null) {
            return;
        }

        LivingEntity attacker = resolveParticipant(player.serverLevel(), session.attackerUuid());
        if (attacker == null || !attacker.isAlive() || !player.isAlive()) {
            endSession(player.getUUID(), PropositionOutcome.CANCELLED);
            return;
        }

        long gameTime = player.level().getGameTime();
        if (!session.menuOpened() && gameTime >= session.menuOpenGameTime()) {
            session.setMenuOpened(true);
            ModNetworking.sendToPlayer(new OpenDemonPropositionPacket(attacker.getUUID(), attacker.getName(), session.endGameTime()), player);
        }

        if (gameTime >= session.endGameTime()) {
            endSession(player.getUUID(), PropositionOutcome.TIMED_OUT);
            return;
        }

        maintainSession(attacker, player, gameTime);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        PropositionSession session = getSession(event.getEntity());
        if (session != null) {
            endSession(session.playerUuid(), PropositionOutcome.CANCELLED);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PropositionSession session = SESSIONS_BY_PLAYER.get(event.getEntity().getUUID());
        if (session != null) {
            endSession(session.playerUuid(), PropositionOutcome.CANCELLED);
        }
    }

    public static void handleResponse(ServerPlayer player, boolean acceptTransformation) {
        PropositionSession session = SESSIONS_BY_PLAYER.get(player.getUUID());
        if (session == null) {
            return;
        }

        LivingEntity attacker = resolveParticipant(player.serverLevel(), session.attackerUuid());
        if (attacker == null || !attacker.isAlive()) {
            endSession(player.getUUID(), PropositionOutcome.CANCELLED);
            return;
        }

        endSession(player.getUUID(), acceptTransformation ? PropositionOutcome.ACCEPTED : PropositionOutcome.REJECTED);

        if (acceptTransformation) {
            transformIntoDemon(player);
        } else {
            killRejectedPlayer(attacker, player);
        }
    }

    private static void startSession(LivingEntity attacker, ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        long endGameTime = gameTime + PROPOSITION_DURATION_TICKS;
        PropositionSession session = new PropositionSession(
            attacker.getUUID(),
            player.getUUID(),
            gameTime + PROPOSITION_MENU_DELAY_TICKS,
            endGameTime,
            attacker instanceof Mob mob && mob.isNoAi(),
            getAbilityCooldown(attacker)
        );
        SESSIONS_BY_PLAYER.put(player.getUUID(), session);
        PARTICIPANT_TO_PLAYER.put(player.getUUID(), player.getUUID());
        PARTICIPANT_TO_PLAYER.put(attacker.getUUID(), player.getUUID());

        player.setHealth(1.0F);
        player.invulnerableTime = 0;

        lockAttacker(attacker);
        applyLockEffects(attacker);
        applyLockEffects(player);
        ModNetworking.sendToPlayer(new SetDemonPropositionStatePacket(true, attacker.getId()), player);
        maintainSession(attacker, player, player.level().getGameTime());
        playSessionAnimations(attacker, player);

        player.displayClientMessage(Component.literal("A kizuki is offering you demonhood."), true);
    }

    private static void maintainSession(LivingEntity attacker, ServerPlayer player, long gameTime) {
        Vec3 forward = horizontalForward(attacker);
        Vec3 targetPos = attacker.position().add(forward.scale(TARGET_DISTANCE));
        float playerFacingYaw = yawTowards(targetPos, attacker.position());

        freezeEntity(attacker);
        freezeEntity(player);
        keepPropositionParticipantsAlive(attacker, player);

        setEntityRotation(attacker, yawTowards(attacker.position(), targetPos));
        teleportPlayer(player, targetPos.x, attacker.getY(), targetPos.z);
        setPlayerRenderRotation(player, playerFacingYaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        if (gameTime % 10L == 0L) {
            applyLockEffects(attacker);
            applyLockEffects(player);
        }
    }

    private static void playSessionAnimations(LivingEntity attacker, ServerPlayer player) {
        String attackerAnimation = CHOKE_ANIMATION;
        String targetAnimation = CHOKED_ANIMATION;
        if (isHoldingNichirinSword(attacker)) {
            attackerAnimation = CHOKE_SWORD_ANIMATION;
            targetAnimation = CHOKED_SWORD_ANIMATION;
        }

        ModNetworking.sendToNearby(new MobAnimationSyncPacket(attacker.getId(), attackerAnimation),
            player.serverLevel(), attacker.getX(), attacker.getY(), attacker.getZ(), ANIMATION_BROADCAST_RADIUS);
        ModNetworking.sendToNearby(new AnimationSyncPacket(
                player.getUUID(),
                net.minecraft.resources.ResourceLocation.parse(targetAnimation),
                0,
                40,
                true,
                false
            ),
            player.serverLevel(), player.getX(), player.getY(), player.getZ(), ANIMATION_BROADCAST_RADIUS);
    }

    private static void stopSessionAnimations(LivingEntity attacker, ServerPlayer player) {
        ModNetworking.sendToNearby(new MobAnimationSyncPacket(attacker.getId(), STOP_MOB_ANIMATION),
            player.serverLevel(), attacker.getX(), attacker.getY(), attacker.getZ(), ANIMATION_BROADCAST_RADIUS);
        ModNetworking.sendToNearby(AnimationSyncPacket.createStopPacket(player.getUUID()),
            player.serverLevel(), player.getX(), player.getY(), player.getZ(), ANIMATION_BROADCAST_RADIUS);
    }

    private static void endSession(UUID playerUuid, PropositionOutcome outcome) {
        PropositionSession session = SESSIONS_BY_PLAYER.remove(playerUuid);
        if (session == null) {
            return;
        }

        ServerPlayer player = findPlayer(playerUuid);
        LivingEntity attacker = player != null ? resolveParticipant(player.serverLevel(), session.attackerUuid()) : null;

        PARTICIPANT_TO_PLAYER.remove(session.playerUuid());
        PARTICIPANT_TO_PLAYER.remove(session.attackerUuid());

        if (player != null) {
            ModNetworking.sendToPlayer(new CloseDemonPropositionPacket(), player);
            clearLockEffects(player);
            if (attacker != null) {
                stopSessionAnimations(attacker, player);
            } else {
                ModNetworking.sendToNearby(AnimationSyncPacket.createStopPacket(player.getUUID()),
                    player.serverLevel(), player.getX(), player.getY(), player.getZ(), ANIMATION_BROADCAST_RADIUS);
            }
        }
        if (attacker != null) {
            unlockAttacker(attacker, session);
            clearLockEffects(attacker);
        }
    }

    private static void transformIntoDemon(ServerPlayer player) {
        if (DemonTransformationHandler.isCustomDemonInitiationEnabled()) {
            DemonTransformationHandler.startTransformationFromProposition(player);
            return;
        }
        ItemStack muzanBlood = new ItemStack(KimetsunoyaibaModItems.BLOOD_OF_MUZAN.get());
        muzanBlood.finishUsingItem(player.level(), player);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, TRANSFORMATION_REGEN_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TRANSFORMATION_REGEN_TICKS, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TRANSFORMATION_DEBUFF_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TRANSFORMATION_DEBUFF_TICKS, 0, false, true, true));
    }

    private static void killRejectedPlayer(LivingEntity attacker, ServerPlayer player) {
        player.getPersistentData().putBoolean(BYPASS_FATAL_PROPOSITION_KEY, true);
        try {
            player.invulnerableTime = 0;
            DamageSource source = DamageCalculator.getDamageSource(attacker);
            player.hurt(source, Float.MAX_VALUE);
            if (player.isAlive()) {
                player.setHealth(0.0F);
                player.die(source);
            }
        } finally {
            player.getPersistentData().putBoolean(BYPASS_FATAL_PROPOSITION_KEY, false);
        }
    }

    private static void applyLockEffects(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PROPOSITION_EFFECT_DURATION_TICKS, 4, false, false, false));
        MobEffect immvable = KnYEffects.getImmvableEffect();
        if (immvable != null) {
            entity.addEffect(new MobEffectInstance(immvable, PROPOSITION_EFFECT_DURATION_TICKS, 0, false, false, false));
        }
    }

    private static void clearLockEffects(LivingEntity entity) {
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        MobEffect immvable = KnYEffects.getImmvableEffect();
        if (immvable != null) {
            entity.removeEffect(immvable);
        }
    }

    private static void freezeEntity(LivingEntity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.invulnerableTime = Math.max(entity.invulnerableTime, 20);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private static void keepPropositionParticipantsAlive(LivingEntity attacker, ServerPlayer player) {
        if (player.getHealth() < 1.0F) {
            player.setHealth(1.0F);
        }
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);

        if (attacker.getHealth() < 1.0F) {
            attacker.setHealth(1.0F);
        }
        attacker.invulnerableTime = Math.max(attacker.invulnerableTime, 20);
    }

    private static void lockAttacker(LivingEntity attacker) {
        if (attacker instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        setAbilityCooldown(attacker, PROPOSITION_DURATION_TICKS + 20);
    }

    private static void unlockAttacker(LivingEntity attacker, PropositionSession session) {
        if (attacker instanceof Mob mob) {
            mob.setNoAi(session.attackerHadNoAi());
        }
        restoreAbilityCooldown(attacker, session.attackerOriginalAbilityCooldown());
    }

    private static int getAbilityCooldown(LivingEntity attacker) {
        if (attacker instanceof AbstractDemonEntity demon) {
            return demon.getExternalBloodDemonArtCooldownTicks();
        }
        return -1;
    }

    private static void setAbilityCooldown(LivingEntity attacker, int cooldownTicks) {
        if (attacker instanceof AbstractDemonEntity demon) {
            demon.setExternalBloodDemonArtCooldownTicks(cooldownTicks);
        }
    }

    private static void restoreAbilityCooldown(LivingEntity attacker, int originalCooldown) {
        if (attacker instanceof AbstractDemonEntity demon && originalCooldown >= 0) {
            demon.setExternalBloodDemonArtCooldownTicks(originalCooldown);
        }
    }

    private static void teleportPlayer(ServerPlayer player, double x, double y, double z) {
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
    }

    private static void setPlayerRenderRotation(ServerPlayer player, float yaw) {
        player.setYBodyRot(yaw);
        player.setYHeadRot(yaw);
        player.yBodyRotO = yaw;
        player.yHeadRotO = yaw;
    }

    private static void setEntityRotation(LivingEntity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
        entity.yHeadRotO = yaw;
        entity.yBodyRotO = yaw;
    }

    private static Vec3 horizontalForward(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            float yawRad = entity.getYRot() * ((float) Math.PI / 180.0F);
            horizontal = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        }
        return horizontal.normalize();
    }

    private static float yawTowards(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
    }

    private static boolean isEligibleKizuki(LivingEntity entity) {
        return Damager.isDemon(entity) && EntityTagHelper.isTwelveKizuki(entity);
    }

    private static boolean isHoldingNichirinSword(LivingEntity entity) {
        return entity != null && BreathingInfoDetector.isNichirinSword(entity.getMainHandItem());
    }

    private static boolean hasSession(LivingEntity entity) {
        return PARTICIPANT_TO_PLAYER.containsKey(entity.getUUID());
    }

    private static PropositionSession getSession(LivingEntity entity) {
        UUID playerUuid = PARTICIPANT_TO_PLAYER.get(entity.getUUID());
        return playerUuid == null ? null : SESSIONS_BY_PLAYER.get(playerUuid);
    }

    private static ServerPlayer findPlayer(UUID playerUuid) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getUUID().equals(playerUuid)) {
                return player;
            }
        }
        return null;
    }

    private static LivingEntity resolveParticipant(ServerPlayer player, UUID uuid) {
        return resolveParticipant(player.serverLevel(), uuid);
    }

    private static LivingEntity resolveParticipant(net.minecraft.server.level.ServerLevel level, UUID uuid) {
        net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private enum PropositionOutcome {
        ACCEPTED,
        REJECTED,
        TIMED_OUT,
        CANCELLED
    }

    private static final class PropositionSession {
        private final UUID attackerUuid;
        private final UUID playerUuid;
        private final long menuOpenGameTime;
        private final long endGameTime;
        private final boolean attackerHadNoAi;
        private final int attackerOriginalAbilityCooldown;
        private boolean menuOpened;

        private PropositionSession(UUID attackerUuid, UUID playerUuid, long menuOpenGameTime, long endGameTime,
                                   boolean attackerHadNoAi, int attackerOriginalAbilityCooldown) {
            this.attackerUuid = attackerUuid;
            this.playerUuid = playerUuid;
            this.menuOpenGameTime = menuOpenGameTime;
            this.endGameTime = endGameTime;
            this.attackerHadNoAi = attackerHadNoAi;
            this.attackerOriginalAbilityCooldown = attackerOriginalAbilityCooldown;
        }

        public UUID attackerUuid() {
            return attackerUuid;
        }

        public UUID playerUuid() {
            return playerUuid;
        }

        public long menuOpenGameTime() {
            return menuOpenGameTime;
        }

        public long endGameTime() {
            return endGameTime;
        }

        public boolean attackerHadNoAi() {
            return attackerHadNoAi;
        }

        public int attackerOriginalAbilityCooldown() {
            return attackerOriginalAbilityCooldown;
        }

        public boolean menuOpened() {
            return menuOpened;
        }

        public void setMenuOpened(boolean menuOpened) {
            this.menuOpened = menuOpened;
        }
    }
}
