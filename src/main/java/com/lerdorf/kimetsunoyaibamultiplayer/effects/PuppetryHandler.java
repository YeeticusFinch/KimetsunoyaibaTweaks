package com.lerdorf.kimetsunoyaibamultiplayer.effects;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PuppetLineSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side enforcement of the Puppetry effect (Spider Family quests):
 *
 * - On application: stores an anchor position + puppet owner UUID in NBT.
 * - The puppet cannot move more than 20 blocks from its anchor; it is pulled back.
 * - If the puppet owner dies/despawns/becomes invalid, the effect ends immediately.
 * - Puppets hover (no gravity/fall) and stay perfectly still when not aggro.
 * - Mobs auto-aggro whatever the owner targets or whoever last hurt the owner.
 *   Demon-owned puppets also auto-aggro demon slayers / humans / non-demon players,
 *   pathfinding towards them while hovering.
 * - Players with the effect have no control of their character for the duration.
 * - Every hit taken subtracts 40 seconds from the remaining duration.
 * - On expiry everything is cleared and reverted.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class PuppetryHandler {

    public static final String ANCHOR_X_KEY = "KnYPuppetAnchorX";
    public static final String ANCHOR_Y_KEY = "KnYPuppetAnchorY";
    public static final String ANCHOR_Z_KEY = "KnYPuppetAnchorZ";
    public static final String OWNER_KEY = "KnYPuppetOwnerUuid";
    public static final String LINE_ANCHORS_KEY = "KnYPuppetLineAnchors";

    private static final String MOB_PUPPETED_KEY = "KnYPuppetAppliedMob";
    private static final String PLAYER_LOCKED_KEY = "KnYPuppetPlayerLocked";
    private static final String HAD_NO_GRAVITY_KEY = "KnYPuppetHadNoGravity";
    private static final String ATTACK_COOLDOWN_KEY = "KnYPuppetAttackCooldown";

    private static final double MAX_ANCHOR_DISTANCE = 20.0D;
    private static final double MAX_ANCHOR_DISTANCE_SQR = MAX_ANCHOR_DISTANCE * MAX_ANCHOR_DISTANCE;
    private static final int LINE_ANCHOR_COUNT = 10;
    private static final int DAMAGE_DURATION_PENALTY_TICKS = 40 * 20; // 40 seconds
    private static final double DEMON_OWNED_AGGRO_RANGE = 32.0D;
    private static final double PUPPET_FLY_SPEED = 0.36D;
    private static final double PUPPET_ATTACK_RANGE_SQR = 9.0D;
    private static final double PUPPET_STOP_RANGE_SQR = 4.0D;
    private static final int PUPPET_ATTACK_INTERVAL_TICKS = 20;

    private PuppetryHandler() {
    }

    // ==================== Application ====================

    /**
     * Applies puppetry with no particles for the given duration, storing
     * anchor position and puppet owner in NBT.
     */
    public static boolean applyPuppetry(LivingEntity target, LivingEntity owner, int durationTicks) {
        if (target == null || !target.isAlive() || target.level().isClientSide()) {
            return false;
        }
        MobEffectInstance instance = new MobEffectInstance(
            ModEffects.PUPPETRY.get(), durationTicks, 0, false, false, true);
        if (!target.addEffect(instance)) {
            return false;
        }
        storeAnchor(target);
        ensureLineAnchorsStored(target);
        CompoundTag data = target.getPersistentData();
        if (owner != null && owner.isAlive()) {
            data.putUUID(OWNER_KEY, owner.getUUID());
        }
        syncPuppetryLines(target, false);
        Log.debugVisible("[PuppetryLines] Applied puppetry to {} id={} owner={} anchors={}",
            target.getName().getString(), target.getId(),
            owner == null ? "none" : owner.getName().getString(),
            getLineAnchors(target).length);
        return true;
    }

    public static void storeAnchor(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        Vec3 pos = target.position();
        data.putDouble(ANCHOR_X_KEY, pos.x);
        data.putDouble(ANCHOR_Y_KEY, pos.y);
        data.putDouble(ANCHOR_Z_KEY, pos.z);
    }

    /** Clears all puppetry state and reverts the entity to its former self. */
    public static void clearPuppetry(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        restoreMovement(entity);
        data.remove(ANCHOR_X_KEY);
        data.remove(ANCHOR_Y_KEY);
        data.remove(ANCHOR_Z_KEY);
        data.remove(LINE_ANCHORS_KEY);
        data.remove(OWNER_KEY);
        data.remove(MOB_PUPPETED_KEY);
        data.remove(PLAYER_LOCKED_KEY);
        data.remove(HAD_NO_GRAVITY_KEY);
        data.remove(ATTACK_COOLDOWN_KEY);
        syncPuppetryLines(entity, true);
    }

    public static boolean hasPuppetry(LivingEntity entity) {
        return entity != null && !entity.level().isClientSide()
            && entity.hasEffect(ModEffects.PUPPETRY.get());
    }

    /** Resolves the stored puppet owner, or null if dead/despawned/invalid. */
    public static LivingEntity getPuppetOwner(LivingEntity puppet) {
        CompoundTag data = puppet.getPersistentData();
        if (!data.hasUUID(OWNER_KEY) || !(puppet.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(data.getUUID(OWNER_KEY));
        if (!(owner instanceof LivingEntity living) || !living.isAlive()) {
            return null;
        }
        return living;
    }

    // ==================== Per-tick enforcement ====================

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.isAlive() || !hasPuppetry(entity)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ANCHOR_X_KEY) || !data.contains(ANCHOR_Y_KEY) || !data.contains(ANCHOR_Z_KEY)) {
            storeAnchor(entity);
        }
        if (!data.contains(LINE_ANCHORS_KEY)) {
            ensureLineAnchorsStored(entity);
            syncPuppetryLines(entity, false);
        }

        // Owner gone -> effect ends immediately
        if (getPuppetOwner(entity) == null) {
            entity.removeEffect(ModEffects.PUPPETRY.get());
            clearPuppetry(entity);
            return;
        }

        if (!data.getBoolean(MOB_PUPPETED_KEY)) {
            data.putBoolean(MOB_PUPPETED_KEY, true);
            applyHoverAndFreeze(entity);
        }

        enforceAnchorLeash(entity);

        LivingEntity owner = getPuppetOwner(entity);
        if (owner == null) {
            return; // handled next tick
        }
        tickPuppet(entity, owner);
    }

    /** Hover: no gravity so puppets float instead of falling. */
    private static void applyHoverAndFreeze(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(HAD_NO_GRAVITY_KEY)) {
            data.putBoolean(HAD_NO_GRAVITY_KEY, entity.isNoGravity());
        }
        entity.setNoGravity(true);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    private static void restoreMovement(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        entity.setNoGravity(data.contains(HAD_NO_GRAVITY_KEY) && data.getBoolean(HAD_NO_GRAVITY_KEY));
        entity.fallDistance = 0.0F;
    }

    /** The puppet cannot move more than 20 blocks from where it received the effect. */
    private static void enforceAnchorLeash(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        Vec3 anchor = new Vec3(
            data.getDouble(ANCHOR_X_KEY),
            data.getDouble(ANCHOR_Y_KEY),
            data.getDouble(ANCHOR_Z_KEY));
        Vec3 delta = entity.position().subtract(anchor);
        if (delta.lengthSqr() > MAX_ANCHOR_DISTANCE_SQR) {
            Vec3 pulledBack = anchor.add(delta.normalize().scale(Math.sqrt(MAX_ANCHOR_DISTANCE_SQR)));
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.teleport(pulledBack.x, pulledBack.y, pulledBack.z,
                    serverPlayer.getYRot(), serverPlayer.getXRot());
            } else {
                entity.teleportTo(pulledBack.x, pulledBack.y, pulledBack.z);
            }
            entity.hurtMarked = true;
        }
    }

    private static void tickPuppet(LivingEntity puppet, LivingEntity owner) {
        puppet.setNoGravity(true);
        puppet.fallDistance = 0.0F;

        CompoundTag data = puppet.getPersistentData();
        if (data.getInt(ATTACK_COOLDOWN_KEY) > 0) {
            data.putInt(ATTACK_COOLDOWN_KEY, data.getInt(ATTACK_COOLDOWN_KEY) - 1);
        }

        LivingEntity desiredTarget = resolveDesiredTarget(puppet, owner);
        if (desiredTarget != null) {
            if (puppet instanceof Mob mob) {
                mob.setTarget(desiredTarget);
                mob.getLookControl().setLookAt(desiredTarget);
                mob.getNavigation().stop();
            }
            faceTarget(puppet, desiredTarget);

            double distSqr = puppet.distanceToSqr(desiredTarget);
            if (distSqr > PUPPET_STOP_RANGE_SQR) {
                movePuppetToward(puppet, desiredTarget);
            } else {
                puppet.setDeltaMovement(Vec3.ZERO);
            }

            if (distSqr <= PUPPET_ATTACK_RANGE_SQR && data.getInt(ATTACK_COOLDOWN_KEY) <= 0) {
                attackTarget(puppet, desiredTarget);
                data.putInt(ATTACK_COOLDOWN_KEY, PUPPET_ATTACK_INTERVAL_TICKS);
            }
        } else {
            // Stay perfectly still: no wandering, no random looking
            if (puppet instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
                mob.xxa = 0.0F;
                mob.zza = 0.0F;
            }
            puppet.setDeltaMovement(Vec3.ZERO);
            puppet.fallDistance = 0.0F;
        }

        if (puppet instanceof Player player) {
            enforcePlayerControlLock(player);
        }
    }

    private static void movePuppetToward(LivingEntity puppet, LivingEntity desiredTarget) {
        Vec3 targetCenter = desiredTarget.position().add(0.0D, desiredTarget.getBbHeight() * 0.5D, 0.0D);
        Vec3 puppetCenter = puppet.position().add(0.0D, puppet.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = targetCenter.subtract(puppetCenter);
        if (toTarget.lengthSqr() < 1.0E-4D) {
            puppet.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 move = toTarget.normalize().scale(PUPPET_FLY_SPEED);
        if (puppet.horizontalCollision) {
            move = move.add(0.0D, 0.22D, 0.0D);
        }

        Vec3 anchor = getAnchor(puppet);
        if (anchor != null && puppet.position().add(move).subtract(anchor).lengthSqr() > MAX_ANCHOR_DISTANCE_SQR) {
            Vec3 backToAnchor = anchor.subtract(puppet.position());
            if (backToAnchor.lengthSqr() > 1.0E-4D) {
                move = backToAnchor.normalize().scale(PUPPET_FLY_SPEED);
            }
        }

        puppet.setDeltaMovement(move);
        puppet.move(MoverType.SELF, move);
        puppet.hurtMarked = true;
    }

    private static void faceTarget(LivingEntity puppet, LivingEntity target) {
        Vec3 diff = target.getEyePosition().subtract(puppet.getEyePosition());
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * (180F / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(diff.y, horizontal) * (180F / Math.PI)));
        puppet.setYRot(yaw);
        puppet.setXRot(pitch);
        puppet.yHeadRot = yaw;
        puppet.yBodyRot = yaw;
    }

    private static void attackTarget(LivingEntity puppet, LivingEntity target) {
        float damage = 3.0F;
        if (puppet.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            damage = Math.max(damage, (float) puppet.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        Damager.hurt(puppet, target, damage, true);
        puppet.swing(InteractionHand.MAIN_HAND, true);
    }

    private static Vec3 getAnchor(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ANCHOR_X_KEY) || !data.contains(ANCHOR_Y_KEY) || !data.contains(ANCHOR_Z_KEY)) {
            return null;
        }
        return new Vec3(data.getDouble(ANCHOR_X_KEY), data.getDouble(ANCHOR_Y_KEY), data.getDouble(ANCHOR_Z_KEY));
    }

    private static void ensureLineAnchorsStored(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (data.contains(LINE_ANCHORS_KEY)) {
            return;
        }

        Vec3[] anchors = selectLineAnchorBlocks(target);
        ListTag list = new ListTag();
        for (Vec3 anchor : anchors) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("x", anchor.x);
            entry.putDouble("y", anchor.y);
            entry.putDouble("z", anchor.z);
            list.add(entry);
        }
        data.put(LINE_ANCHORS_KEY, list);
        Log.debugVisible("[PuppetryLines] Selected {} block anchors for {} id={} at {}",
            anchors.length, target.getName().getString(), target.getId(), target.blockPosition());
    }

    private static Vec3[] selectLineAnchorBlocks(LivingEntity target) {
        Vec3[] anchors = new Vec3[LINE_ANCHOR_COUNT];
        ServerLevel level = (ServerLevel) target.level();
        BlockPos origin = target.blockPosition();
        java.util.Random random = new java.util.Random(target.getUUID().getMostSignificantBits()
            ^ target.getUUID().getLeastSignificantBits()
            ^ level.getGameTime());

        // Eye-level origin for the upward raycasts
        Vec3 eye = target.getEyePosition();

        for (int i = 0; i < LINE_ANCHOR_COUNT; i++) {
            BlockPos selected = null;

            // 1) Straight-up raycasts: prioritize blocks above the entity
            for (int attempt = 0; attempt < 4 && selected == null; attempt++) {
                Vec3 upDir = new Vec3((random.nextDouble() - 0.5D) * 0.3D, 1.0D,
                    (random.nextDouble() - 0.5D) * 0.3D).normalize();
                selected = raycastAnchor(level, eye, upDir, 10 + random.nextInt(15));
            }

            // 2) Diagonally up + sideways raycasts (8 horizontal directions,
            //    pitched up between 25 and 65 degrees)
            for (int attempt = 0; attempt < 16 && selected == null; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double pitch = Math.toRadians(25.0D + random.nextDouble() * 40.0D);
                Vec3 dir = new Vec3(Math.cos(angle) * Math.cos(pitch), Math.sin(pitch),
                    Math.sin(angle) * Math.cos(pitch)).normalize();
                selected = raycastAnchor(level, eye, dir, 12 + random.nextInt(18));
            }

            // 3) Fallback: downward column scan around the entity (old behaviour)
            for (int attempt = 0; attempt < 48 && selected == null; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                int horizontalDistance = 6 + random.nextInt(17);
                int dx = Mth.floor(Math.cos(angle) * horizontalDistance);
                int dz = Mth.floor(Math.sin(angle) * horizontalDistance);
                int startY = origin.getY() + 6 + random.nextInt(12);
                int minY = Math.max(level.getMinBuildHeight(), origin.getY() - 4);
                for (int y = Math.min(startY, level.getMaxBuildHeight() - 1); y >= minY; y--) {
                    BlockPos candidate = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (isUsableLineAnchor(level, candidate)) {
                        selected = candidate;
                        break;
                    }
                }
            }

            if (selected == null) {
                double angle = (Math.PI * 2.0D * i) / LINE_ANCHOR_COUNT;
                selected = origin.offset(
                    Mth.floor(Math.cos(angle) * 16.0D),
                    8 + (i % 4),
                    Mth.floor(Math.sin(angle) * 16.0D));
            }
            anchors[i] = Vec3.atCenterOf(selected);
        }
        return anchors;
    }

    /**
     * Clips a ray from the given origin and stores the first block face hit as
     * a line anchor. Returns null if nothing solid is within range.
     */
    private static BlockPos raycastAnchor(ServerLevel level, Vec3 origin, Vec3 direction, int maxDistance) {
        ClipContext context = new ClipContext(origin,
            origin.add(direction.scale(maxDistance)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        BlockHitResult hit = level.clip(context);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos hitPos = hit.getBlockPos();
        return isUsableLineAnchor(level, hitPos) ? hitPos : null;
    }

    private static boolean isUsableLineAnchor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static Vec3[] getLineAnchors(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(LINE_ANCHORS_KEY)) {
            return new Vec3[0];
        }
        ListTag list = data.getList(LINE_ANCHORS_KEY, 10);
        Vec3[] anchors = new Vec3[list.size()];
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            anchors[i] = new Vec3(entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"));
        }
        return anchors;
    }

    private static void syncPuppetryLines(LivingEntity target, boolean clear) {
        if (target.level().isClientSide()) {
            return;
        }
        Vec3[] anchors = clear ? new Vec3[0] : getLineAnchors(target);
        ModNetworking.sendToTrackingAndSelf(new PuppetLineSyncPacket(target.getUUID(), target.getId(), !clear, anchors), target);
        Log.debugVisibleEvery("puppetry-lines-sync-" + target.getId(), 2000L,
            "[PuppetryLines] Sent {} line sync for {} id={} anchors={}",
            clear ? "clear" : "active", target.getName().getString(), target.getId(), anchors.length);
    }

    private static LivingEntity resolveDesiredTarget(LivingEntity puppet, LivingEntity owner) {
        LivingEntity target = owner instanceof Mob ownerMob ? ownerMob.getTarget() : null;
        if (isValidPuppetTarget(puppet, target)) {
            return target;
        }
        target = owner.getLastHurtByMob(); // whoever attacked the owner
        if (isValidPuppetTarget(puppet, target)) {
            return target;
        }
        target = owner.getLastHurtMob(); // whoever the owner recently attacked
        if (isValidPuppetTarget(puppet, target)) {
            return target;
        }
        // Demon-owned puppets automatically aggro demon slayers/humans/non-demon players.
        if (Damager.isDemon(owner)) {
            List<LivingEntity> nearby = puppet.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(puppet.blockPosition()).inflate(DEMON_OWNED_AGGRO_RANGE),
                candidate -> isValidPuppetTarget(puppet, candidate) && candidate != owner
                    && isDemonOwnerAutoTarget(candidate));
            Optional<LivingEntity> nearest = nearby.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(puppet)));
            if (nearest.isPresent()) {
                return nearest.get();
            }
        }
        return null;
    }

    private static boolean isValidPuppetTarget(LivingEntity puppet, LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && candidate != puppet
            && !candidate.hasEffect(ModEffects.PUPPETRY.get());
    }

    private static boolean isDemonOwnerAutoTarget(LivingEntity candidate) {
        if (Damager.isDemon(candidate)) {
            return false;
        }
        return candidate instanceof Player
            || candidate instanceof Villager
            || Damager.isDemonSlayer(candidate)
            || EntityTagHelper.isDemonSlayer(candidate)
            || candidate.getType().is(EntityTagHelper.CIVILIAN);
    }

    // ==================== Player control lock ====================

    private static void enforcePlayerControlLock(Player player) {
        player.getPersistentData().putBoolean(PLAYER_LOCKED_KEY, true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setSprinting(false);
    }

    public static boolean isPlayerPuppeted(Player player) {
        return player != null && !player.level().isClientSide()
            && player.getPersistentData().getBoolean(PLAYER_LOCKED_KEY)
            && hasPuppetry(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isPlayerPuppeted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelInteractionIfPuppeted(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelInteractionIfPuppeted(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelInteractionIfPuppeted(event);
    }

    private static void cancelInteractionIfPuppeted(PlayerInteractEvent event) {
        if (isPlayerPuppeted(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    // ==================== Damage shortens duration ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!hasPuppetry(entity)) {
            return;
        }
        MobEffectInstance instance = entity.getEffect(ModEffects.PUPPETRY.get());
        if (instance == null) {
            return;
        }
        int remaining = instance.getDuration() - DAMAGE_DURATION_PENALTY_TICKS;
        if (remaining <= 0) {
            entity.removeEffect(ModEffects.PUPPETRY.get()); // triggers cleanup below
        } else {
            entity.forceAddEffect(new MobEffectInstance(ModEffects.PUPPETRY.get(), remaining,
                instance.getAmplifier(), false, false, true), null);
        }
    }

    // ==================== External application (/effect give, splash potions) ====================

    /**
     * When puppetry is granted by anything outside {@link #applyPuppetry}
     * (vanilla /effect give, a splash potion of puppetry, etc.), anchor the
     * victim where they stand and attribute ownership to whoever applied it:
     * the commanding player for /effect give, or the thrower for a splash potion.
     */
    @SubscribeEvent
    public static void onEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()
            || event.getEffectInstance() == null
            || event.getEffectInstance().getEffect() != ModEffects.PUPPETRY.get()) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        // Don't clobber state when our own quest code re-applies/shortens the effect
        if (!data.hasUUID(OWNER_KEY)) {
            storeAnchor(target);
            ensureLineAnchorsStored(target);
            Entity applier = event.getEffectSource();
            if (applier instanceof LivingEntity livingApplier && livingApplier.isAlive()) {
                data.putUUID(OWNER_KEY, livingApplier.getUUID());
            }
            syncPuppetryLines(target, false);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity target) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!target.hasEffect(ModEffects.PUPPETRY.get())) {
            return;
        }
        ensureLineAnchorsStored(target);
        ModNetworking.sendToPlayer(new PuppetLineSyncPacket(target.getUUID(), target.getId(), true, getLineAnchors(target)), player);
        Log.debugVisibleEvery("puppetry-lines-track-" + target.getId() + "-" + player.getId(), 2000L,
            "[PuppetryLines] Sent tracking line sync for {} id={} to {}",
            target.getName().getString(), target.getId(), player.getName().getString());
    }

    // ==================== Cleanup on expiry/removal ====================

    @SubscribeEvent
    public static void onEffectExpire(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        handleEffectEnd(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void onEffectRemove(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        handleEffectEnd(event.getEntity(), new MobEffectInstance(ModEffects.PUPPETRY.get(), 1));
    }

    private static void handleEffectEnd(Entity entity, MobEffectInstance instance) {
        if (instance == null || !(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        if (instance.getEffect() == ModEffects.PUPPETRY.get()) {
            clearPuppetry(living);
        }
    }
}
