package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.MotherEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Blood Demon Art used by Mother: manifestations, traversal, and puppetry. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class DemonwebPuppetry {
    public static final String ART_ID = "demonweb_puppetry";
    public static final int FORM_SPIDER_MANIFESTATION = 3600;
    public static final int FORM_WEB_TRAVERSAL_ANCHOR = 3601;
    public static final int FORM_PUPPETRY = 3602;

    public static final String MANIFESTATION_OWNER_KEY = "KnYSpiderManifestationOwner";
    private static final String MANIFESTATION_TARGET_KEY = "KnYSpiderManifestationTarget";
    private static final String MANIFESTATION_LOCKED_TARGET_KEY = "KnYSpiderManifestationLockedTarget";
    private static final String MANIFESTATION_TRIGGERED_KEY = "KnYSpiderManifestationTriggered";
    private static final String MANIFESTATION_AGE_KEY = "KnYSpiderManifestationAge";
    private static final String WEB_ANCHOR_X_KEY = "KnYWebTraversalAnchorX";
    private static final String WEB_ANCHOR_Y_KEY = "KnYWebTraversalAnchorY";
    private static final String WEB_ANCHOR_Z_KEY = "KnYWebTraversalAnchorZ";
    private static final String WEB_HAD_NO_GRAVITY_KEY = "KnYWebTraversalHadNoGravity";
    private static final String WEB_HAD_MAYFLY_KEY = "KnYWebTraversalHadMayfly";
    private static final String WEB_HAD_FLYING_KEY = "KnYWebTraversalHadFlying";

    private static final net.minecraft.resources.ResourceLocation SPIDER_DEMON_ID =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "spider_demon");
    private static final double MAX_TARGET_RANGE = 200.0D;
    private static final double WEB_MAX_DISTANCE = 30.0D;
    private static final int WEB_DURATION_TICKS = 5 * 60 * 20;
    private static final int MANIFESTATION_LIFETIME_TICKS = 60 * 20;
    private static final int PUPPETRY_DURATION_TICKS = 5 * 60 * 20;

    private DemonwebPuppetry() {
    }

    public static void register() {
        if (!BloodDemonArtRegistry.isRegistered(ART_ID)) {
            KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Demonweb Puppetry", createTechnique());
        }
    }

    private static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Demonweb Puppetry",
            List.of(
                new BloodDemonArtForm(FORM_SPIDER_MANIFESTATION, "Spider Manifestation",
                    "Send a marked spider demon after a distant human target.", 5,
                    DemonwebPuppetry::executeSpiderManifestation),
                new BloodDemonArtForm(FORM_WEB_TRAVERSAL_ANCHOR, "Web Traversal Anchor",
                    "Float freely within 30 blocks of an anchor point.", 5,
                    DemonwebPuppetry::executeWebTraversal),
                new BloodDemonArtForm(FORM_PUPPETRY, "Puppetry",
                    "Bind a visible human target to your control.", 5,
                    DemonwebPuppetry::executePuppetry)
            ),
            0xE8E8E8
        );
    }

    private static void executeSpiderManifestation(LivingEntity caster, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity crosshairTarget = resolveManifestationCrosshairTarget(caster, MAX_TARGET_RANGE);
        LivingEntity target = crosshairTarget != null
            ? crosshairTarget : findNearestEnemy(caster, serverLevel, MAX_TARGET_RANGE, false);
        if (target != null) {
            spawnManifestation(caster, target, serverLevel, crosshairTarget != null);
        }
    }

    private static void executeWebTraversal(LivingEntity caster, Level level, int formId) {
        activateWebTraversal(caster);
    }

    private static void executePuppetry(LivingEntity caster, Level level, int formId) {
        LivingEntity target = resolveDirectTarget(caster, MAX_TARGET_RANGE);
        if (target != null) {
            applyPuppetryToTarget(caster, target);
        }
    }

    public static boolean applyPuppetryToTarget(LivingEntity caster, LivingEntity target) {
        if (caster == null || target == null || !isEnemyTarget(target)
            || target.hasEffect(ModEffects.PUPPETRY.get())) {
            return false;
        }
        boolean applied = PuppetryHandler.applyPuppetry(target, caster, PUPPETRY_DURATION_TICKS);
        if (applied && caster instanceof Mob mob) {
            mob.setTarget(target);
        }
        return applied;
    }

    public static boolean spawnManifestation(LivingEntity owner, LivingEntity target, ServerLevel level) {
        return spawnManifestation(owner, target, level, false);
    }

    private static boolean spawnManifestation(LivingEntity owner, LivingEntity target, ServerLevel level,
                                              boolean lockTarget) {
        if (owner == null || target == null || !isValidManifestationTarget(owner, target)) {
            return false;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(SPIDER_DEMON_ID);
        if (type == null) {
            return false;
        }
        Entity entity = type.create(level);
        if (!(entity instanceof Mob manifestation)) {
            return false;
        }
        Vec3 launchDirection = owner.getLookAngle().normalize();
        Vec3 spawnPosition = owner.getEyePosition().add(launchDirection);
        manifestation.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z,
            owner.getYRot(), owner.getXRot());
        manifestation.setDeltaMovement(launchDirection);
        manifestation.hasImpulse = true;
        manifestation.setPersistenceRequired();
        CompoundTag data = manifestation.getPersistentData();
        data.putUUID(MANIFESTATION_OWNER_KEY, owner.getUUID());
        data.putUUID(MANIFESTATION_TARGET_KEY, target.getUUID());
        data.putBoolean(MANIFESTATION_LOCKED_TARGET_KEY, lockTarget);
        data.putInt(MANIFESTATION_AGE_KEY, 0);
        manifestation.setTarget(target);
        return level.addFreshEntity(manifestation);
    }

    private static LivingEntity resolveManifestationCrosshairTarget(LivingEntity caster, double range) {
        HitResult hit = caster.pick(range, 1.0F, false);
        if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity target
            && isValidManifestationTarget(caster, target)) {
            return target;
        }
        return null;
    }

    public static LivingEntity findVisibleEnemy(MotherEntity mother) {
        LivingEntity target = mother.getTarget();
        if (isVisibleEnemy(mother, target)) {
            return target;
        }
        target = mother.getLastHurtByMob();
        if (isVisibleEnemy(mother, target)) {
            return target;
        }
        target = mother.getLastHurtMob();
        if (isVisibleEnemy(mother, target)) {
            return target;
        }
        if (!(mother.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return findNearestEnemy(mother, serverLevel, MAX_TARGET_RANGE, true);
    }

    public static LivingEntity findNearestEnemy(LivingEntity source, ServerLevel level,
                                                 double range, boolean requireLineOfSight) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
            source.getBoundingBox().inflate(range), target -> isEnemyTarget(target)
                && source.distanceToSqr(target) <= range * range
                && (!requireLineOfSight || !(source instanceof Mob mob) || mob.getSensing().hasLineOfSight(target)));
        return candidates.stream().min(Comparator.comparingDouble(source::distanceToSqr)).orElse(null);
    }

    public static boolean isEnemyTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target.hasEffect(ModEffects.PUPPETRY.get())
            || DamagerFacade.isDemon(target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return target instanceof Villager || target instanceof Player
            || DamagerFacade.isDemonSlayer(target)
            || EntityTagHelper.isDemonSlayer(target)
            || target.getType().is(EntityTagHelper.CIVILIAN);
    }

    private static boolean isVisibleEnemy(MotherEntity mother, LivingEntity target) {
        return isEnemyTarget(target) && mother.distanceToSqr(target) <= MAX_TARGET_RANGE * MAX_TARGET_RANGE
            && mother.getSensing().hasLineOfSight(target);
    }

    private static LivingEntity resolveDirectTarget(LivingEntity caster, double range) {
        HitResult hit = caster.pick(range, 1.0F, false);
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity target && isEnemyTarget(target)) {
            return target;
        }
        if (caster instanceof Mob mob && isEnemyTarget(mob.getTarget())
            && mob.getSensing().hasLineOfSight(mob.getTarget())) {
            return mob.getTarget();
        }
        return null;
    }

    public static int countPuppets(MotherEntity mother) {
        if (!(mother.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return serverLevel.getEntitiesOfClass(LivingEntity.class,
            mother.getBoundingBox().inflate(MAX_TARGET_RANGE), candidate ->
                PuppetryHandler.getPuppetOwner(candidate) == mother).size();
    }

    public static boolean hasActiveManifestation(MotherEntity mother) {
        if (!(mother.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return !serverLevel.getEntitiesOfClass(Mob.class, mother.getBoundingBox().inflate(MAX_TARGET_RANGE),
            candidate -> isMarkedManifestation(candidate)
                && candidate.getPersistentData().hasUUID(MANIFESTATION_OWNER_KEY)
                && mother.getUUID().equals(candidate.getPersistentData().getUUID(MANIFESTATION_OWNER_KEY))).isEmpty();
    }

    private static boolean isMarkedManifestation(Entity entity) {
        return entity != null && SPIDER_DEMON_ID.equals(EntityType.getKey(entity.getType()))
            && entity.getPersistentData().hasUUID(MANIFESTATION_OWNER_KEY);
    }

    public static boolean activateWebTraversal(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return false;
        }
        if (entity.hasEffect(ModEffects.WEB_TRAVERSAL.get())) {
            entity.removeEffect(ModEffects.WEB_TRAVERSAL.get());
            return false;
        }
        CompoundTag data = entity.getPersistentData();
        Vec3 position = entity.position();
        data.putDouble(WEB_ANCHOR_X_KEY, position.x);
        data.putDouble(WEB_ANCHOR_Y_KEY, position.y);
        data.putDouble(WEB_ANCHOR_Z_KEY, position.z);
        data.putBoolean(WEB_HAD_NO_GRAVITY_KEY, entity.isNoGravity());
        entity.setNoGravity(true);
        if (entity instanceof Player player) {
            data.putBoolean(WEB_HAD_MAYFLY_KEY, player.getAbilities().mayfly);
            data.putBoolean(WEB_HAD_FLYING_KEY, player.getAbilities().flying);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        entity.addEffect(new MobEffectInstance(ModEffects.WEB_TRAVERSAL.get(), WEB_DURATION_TICKS,
            0, false, false, true));
        PuppetryHandler.syncLineAnchors(entity);
        return true;
    }

    public static boolean hasWebTraversal(LivingEntity entity) {
        return entity != null && !entity.level().isClientSide()
            && entity.hasEffect(ModEffects.WEB_TRAVERSAL.get());
    }

    public static void deactivateWebTraversal(LivingEntity entity) {
        if (entity != null && entity.hasEffect(ModEffects.WEB_TRAVERSAL.get())) {
            entity.removeEffect(ModEffects.WEB_TRAVERSAL.get());
        } else if (entity != null) {
            clearWebTraversal(entity);
        }
    }

    private static void tickWebTraversal(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(WEB_ANCHOR_X_KEY) || !data.contains(WEB_ANCHOR_Y_KEY)
            || !data.contains(WEB_ANCHOR_Z_KEY)) {
            Vec3 position = entity.position();
            data.putDouble(WEB_ANCHOR_X_KEY, position.x);
            data.putDouble(WEB_ANCHOR_Y_KEY, position.y);
            data.putDouble(WEB_ANCHOR_Z_KEY, position.z);
        }
        if (!data.contains(PuppetryHandler.LINE_ANCHORS_KEY)) {
            PuppetryHandler.syncLineAnchors(entity);
        }
        Vec3 anchor = new Vec3(data.getDouble(WEB_ANCHOR_X_KEY), data.getDouble(WEB_ANCHOR_Y_KEY),
            data.getDouble(WEB_ANCHOR_Z_KEY));
        if (entity.distanceToSqr(anchor) > WEB_MAX_DISTANCE * WEB_MAX_DISTANCE) {
            entity.removeEffect(ModEffects.WEB_TRAVERSAL.get());
            return;
        }
        entity.setNoGravity(true);
        if (entity instanceof Player player && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    private static void clearWebTraversal(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        entity.setNoGravity(data.getBoolean(WEB_HAD_NO_GRAVITY_KEY));
        if (entity instanceof Player player) {
            player.getAbilities().mayfly = data.getBoolean(WEB_HAD_MAYFLY_KEY);
            player.getAbilities().flying = data.getBoolean(WEB_HAD_FLYING_KEY) && player.getAbilities().mayfly;
            player.onUpdateAbilities();
        }
        data.remove(WEB_ANCHOR_X_KEY);
        data.remove(WEB_ANCHOR_Y_KEY);
        data.remove(WEB_ANCHOR_Z_KEY);
        data.remove(WEB_HAD_NO_GRAVITY_KEY);
        data.remove(WEB_HAD_MAYFLY_KEY);
        data.remove(WEB_HAD_FLYING_KEY);
        if (entity.hasEffect(ModEffects.PUPPETRY.get())) {
            PuppetryHandler.syncLineAnchors(entity);
        } else {
            PuppetryHandler.clearLineAnchors(entity);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        try {
            if (entity.hasEffect(ModEffects.WEB_TRAVERSAL.get())) {
                tickWebTraversal(entity);
            }
            if (isMarkedManifestation(entity)) {
                tickManifestation(entity);
            }
        } catch (Exception ex) {
            System.err.println("[DemonwebPuppetry] Tick handler failed: " + ex);
        }
    }

    private static void tickManifestation(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int age = data.getInt(MANIFESTATION_AGE_KEY) + 1;
        data.putInt(MANIFESTATION_AGE_KEY, age);
        if (age >= MANIFESTATION_LIFETIME_TICKS || !(entity.level() instanceof ServerLevel serverLevel)
            || !data.hasUUID(MANIFESTATION_OWNER_KEY) || !data.hasUUID(MANIFESTATION_TARGET_KEY)) {
            entity.discard();
            return;
        }
        LivingEntity owner = getManifestationOwner(serverLevel, data);
        if (owner == null) {
            entity.discard();
            return;
        }
        if (entity instanceof Mob mob) {
            LivingEntity collision = findManifestationCollision(mob, owner);
            if (collision != null) {
                applyManifestationPuppetry(mob, collision);
                if (mob.isRemoved()) {
                    return;
                }
            }
            applyManifestationPuppetry(mob, mob.getLastHurtMob());
            if (mob.isRemoved()) {
                return;
            }
            mob.setTarget(resolveManifestationTarget(mob, owner, serverLevel));
        }
    }

    private static LivingEntity findManifestationCollision(Mob manifestation, LivingEntity owner) {
        Vec3 motion = manifestation.getDeltaMovement();
        AABB collisionBox = manifestation.getBoundingBox().expandTowards(motion).inflate(0.15D);
        return manifestation.level().getEntitiesOfClass(LivingEntity.class, collisionBox,
                target -> isValidManifestationTarget(owner, target) && target != manifestation)
            .stream()
            .min(Comparator.comparingDouble(manifestation::distanceToSqr))
            .orElse(null);
    }

    private static LivingEntity resolveManifestationTarget(Mob manifestation, LivingEntity owner,
                                                           ServerLevel level) {
        CompoundTag data = manifestation.getPersistentData();
        if (data.getBoolean(MANIFESTATION_LOCKED_TARGET_KEY)) {
            LivingEntity lockedTarget = getManifestationTarget(level, data);
            return isValidManifestationTarget(owner, lockedTarget) ? lockedTarget : null;
        }

        LivingEntity target = owner instanceof Mob ownerMob ? ownerMob.getTarget() : null;
        if (isValidManifestationTarget(owner, target)) {
            return target;
        }
        target = owner.getLastHurtMob();
        if (isValidManifestationTarget(owner, target)) {
            return target;
        }
        target = owner.getLastHurtByMob();
        if (isValidManifestationTarget(owner, target)) {
            return target;
        }
        target = getManifestationTarget(level, data);
        return isValidManifestationTarget(owner, target) ? target : null;
    }

    private static LivingEntity getManifestationTarget(ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(MANIFESTATION_TARGET_KEY)) {
            return null;
        }
        Entity target = level.getEntity(data.getUUID(MANIFESTATION_TARGET_KEY));
        return target instanceof LivingEntity livingTarget ? livingTarget : null;
    }

    private static LivingEntity getManifestationOwner(ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(MANIFESTATION_OWNER_KEY)) {
            return null;
        }
        UUID ownerUuid = data.getUUID(MANIFESTATION_OWNER_KEY);
        Entity owner = level.getEntity(ownerUuid);
        if (owner == null && level.getServer() != null) {
            for (ServerLevel loadedLevel : level.getServer().getAllLevels()) {
                owner = loadedLevel.getEntity(ownerUuid);
                if (owner != null) {
                    break;
                }
            }
        }
        return owner instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static boolean isValidManifestationTarget(LivingEntity owner, LivingEntity target) {
        return target != null && target.isAlive() && target != owner
            && !isMarkedManifestation(target)
            && !target.hasEffect(ModEffects.PUPPETRY.get());
    }

    @SubscribeEvent
    public static void onManifestationTargetChange(LivingChangeTargetEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            if (!isMarkedManifestation(entity) || !(entity instanceof Mob manifestation)
                || !(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            CompoundTag data = entity.getPersistentData();
            Entity ownerEntity = data.hasUUID(MANIFESTATION_OWNER_KEY)
                ? serverLevel.getEntity(data.getUUID(MANIFESTATION_OWNER_KEY)) : null;
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {
                event.setNewTarget(resolveManifestationTarget(manifestation, owner, serverLevel));
            } else {
                event.setNewTarget(null);
            }
        } catch (Exception ex) {
            System.err.println("[DemonwebPuppetry] Manifestation target handler failed: " + ex);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        try {
            Entity attacker = event.getSource().getDirectEntity();
            if (attacker == null) {
                attacker = event.getSource().getEntity();
            }
            applyManifestationPuppetry(attacker, event.getEntity());
        } catch (Exception ex) {
            System.err.println("[DemonwebPuppetry] Manifestation damage handler failed: " + ex);
        }
    }

    private static void applyManifestationPuppetry(Entity attacker, LivingEntity target) {
        if (!isMarkedManifestation(attacker) || !(attacker.level() instanceof ServerLevel serverLevel)
            || target == null || !target.isAlive()) {
            return;
        }
        CompoundTag data = attacker.getPersistentData();
        if (data.getBoolean(MANIFESTATION_TRIGGERED_KEY)) {
            return;
        }
        LivingEntity owner = getManifestationOwner(serverLevel, data);
        if (owner != null && isValidManifestationTarget(owner, target)
            && PuppetryHandler.applyPuppetry(target, owner, PUPPETRY_DURATION_TICKS)) {
            data.putBoolean(MANIFESTATION_TRIGGERED_KEY, true);
            attacker.discard();
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        try {
            Entity attacker = event.getSource().getDirectEntity();
            if (attacker == null) {
                attacker = event.getSource().getEntity();
            }
            if (!event.isCanceled()) {
                applyManifestationPuppetry(attacker, event.getEntity());
            }
        } catch (Exception ex) {
            System.err.println("[DemonwebPuppetry] Manifestation attack handler failed: " + ex);
        }
    }

    @SubscribeEvent
    public static void onEffectExpire(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
            && event.getEffectInstance().getEffect() == ModEffects.WEB_TRAVERSAL.get()) {
            clearWebTraversal(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.WEB_TRAVERSAL.get()) {
            clearWebTraversal(event.getEntity());
        }
    }

    private static final class DamagerFacade {
        private static boolean isDemon(LivingEntity entity) {
            return com.lerdorf.kimetsunoyaibamultiplayer.Damager.isDemon(entity);
        }

        private static boolean isDemonSlayer(LivingEntity entity) {
            return com.lerdorf.kimetsunoyaibamultiplayer.Damager.isDemonSlayer(entity);
        }
    }
}
