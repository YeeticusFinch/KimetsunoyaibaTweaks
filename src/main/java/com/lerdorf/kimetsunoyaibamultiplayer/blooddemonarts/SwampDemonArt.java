package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampHandEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampPuddleEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwampPuddleStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class SwampDemonArt {
    public static final String ART_ID = "swamp_demon_art";
    public static final int FORM_PUDDLE = 3300;
    public static final int FORM_SWAMP_HANDS = 3301;
    public static final int FORM_SWAMP_DOMAIN = 3302;
    public static final int FORM_SWAMPY_CLOUD = 3303;
    public static final int FORM_AQUATIC_DASH = 3304;

    public static final String PUDDLE_ACTIVE_TAG = "SwampPuddleActive";
    public static final String PUDDLE_END_TICK_TAG = "SwampPuddleEndTick";
    public static final String PUDDLE_ENTITY_TAG = "SwampPuddleEntity";
    public static final String SWAMP_COMBO_INDEX_TAG = "SwampComboIndex";
    public static final String PUDDLE_STATE_TAG = "SwampPuddleState";
    public static final String PUDDLE_STATE_END_TICK_TAG = "SwampPuddleStateEndTick";
    public static final String PUDDLE_ATTACK_END_TICK_TAG = "SwampPuddleAttackEndTick";
    public static final String PUDDLE_LOOP_TAG = "SwampPuddleLoopAnimation";
    public static final String PUDDLE_LOOP_REFRESH_TICK_TAG = "SwampPuddleLoopRefreshTick";
    public static final String PUDDLE_DEMON_CROUCH_TAG = "SwampPuddleDemonCrouch";
    public static final String PUDDLE_DEMON_CROUCH_END_TICK_TAG = "SwampPuddleDemonCrouchEndTick";
    public static final String SWAMP_RETURN_DIM_TAG = "SwampDomainReturnDimension";
    public static final String SWAMP_RETURN_X_TAG = "SwampDomainReturnX";
    public static final String SWAMP_RETURN_Y_TAG = "SwampDomainReturnY";
    public static final String SWAMP_RETURN_Z_TAG = "SwampDomainReturnZ";
    public static final String SWAMP_RETURN_YAW_TAG = "SwampDomainReturnYaw";
    public static final String SWAMP_RETURN_PITCH_TAG = "SwampDomainReturnPitch";
    public static final String SWAMP_DOMAIN_ENTRY_TICK_TAG = "SwampDomainEntryTick";
    public static final String SWAMP_DOMAIN_ENTRY_X_TAG = "SwampDomainEntryX";
    public static final String SWAMP_DOMAIN_ENTRY_Y_TAG = "SwampDomainEntryY";
    public static final String SWAMP_DOMAIN_ENTRY_Z_TAG = "SwampDomainEntryZ";
    public static final String DEBUG_DIMENSIONS_ACTIVE_TAG = "SwampDebugDimensionsActive";
    public static final String DEBUG_DIMENSIONS_HEIGHT_TAG = "SwampDebugDimensionsHeight";
    public static final String DEBUG_DIMENSIONS_EYE_HEIGHT_TAG = "SwampDebugDimensionsEyeHeight";
    private static final String PUDDLE_ACTIVE_SYNC_TAG = KimetsunoyaibaMultiplayer.MODID + ".swamp_puddle_active";
    private static final String PUDDLE_HIDDEN_SYNC_TAG = KimetsunoyaibaMultiplayer.MODID + ".swamp_puddle_hidden";
    private static final String CLIENT_PUDDLE_STATE_CACHE_TAG = "SwampPuddleClientCached";
    private static final String CLIENT_PUDDLE_ACTIVE_TAG = "SwampPuddleClientActive";
    private static final String CLIENT_PUDDLE_HIDDEN_TAG = "SwampPuddleClientHidden";
    private static final String SWAMP_LAST_LEFT_CLICK_TICK_TAG = "SwampLastLeftClickTick";
    private static final String SWAMP_LAST_ABILITY_USE_TICK_TAG = "SwampLastAbilityUseTick";
    private static final String SWAMP_PLAYER_ANIM_SERIAL_TAG = "SwampPlayerAnimSerial";

    public static final ResourceKey<Level> SWAMP_DOMAIN_LEVEL = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "swamp_domain"));
    public static final ResourceKey<Level> MT_FUJIKASANE_LEVEL = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "mt_fujikasane"));
    private static final int SWAMP_DOMAIN_CEILING_CHECK_INTERVAL_TICKS = 20;
    private static final double SWAMP_DOMAIN_CEILING_Y = 120.0D;
    private static final double SWAMP_DOMAIN_CEILING_DOWNWARD_SPEED = -1.0D;
    private static final double SWAMP_DOMAIN_MAX_CONTROLLED_RISE_SPEED = 0.18D;
    private static final double SWAMP_DOMAIN_PASSIVE_SINK_SPEED = -0.015D;

    public static final double SWAMP_DOMAIN_MAX_DEMON_Y = 125.0D;
    public static final double SWAMP_DOMAIN_DEMON_DENSITY_RADIUS = 100.0D;
    public static final int SWAMP_DOMAIN_MAX_SWAMP_DEMONS_PER_RADIUS = 4;

    private static final int PUDDLE_DURATION_TICKS = 20 * 60;
    private static final int PUDDLE_TRANSITION_TICKS = 10;
    private static final int PUDDLE_LOOP_REFRESH_TICKS = 8;
    private static final int PUDDLE_EXIT_ANIMATION_TICKS = 10;
    private static final int PORTAL_DURATION_TICKS = 20 * 20;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    private static final int SWAMP_DOMAIN_MIN_STAY_TICKS = 20 * 10;
    private static final int SWAMP_DOMAIN_REENTRY_COOLDOWN_TICKS = 20 * 30;
    private static final int SWAMP_DOMAIN_WATER_BREATHING_TICKS = 20 * 60 * 30;
    private static final String PORTAL_COOLDOWN_TAG = "SwampPortalCooldown";
    private static final String SWAMP_DOMAIN_EXIT_LOCK_TAG = "SwampDomainExitLockUntil";
    private static final String SWAMP_DOMAIN_ENTRY_LOCK_TAG = "SwampDomainEntryLockUntil";
    private static final float PUDDLE_STEP_HEIGHT = 1.4F;
    private static final double SWAMP_DOMAIN_PROJECTILE_SPEED = 0.5D;
    private static final int SWAMP_DOMAIN_PROJECTILE_LIFETIME_TICKS = 20 * 5;
    private static final UUID PUDDLE_HIDDEN_REACH_UUID = UUID.fromString("bf1549cd-1ee9-4d47-b8f0-1f72b2af21b2");
    private static final AttributeModifier PUDDLE_HIDDEN_REACH_MODIFIER =
        new AttributeModifier(PUDDLE_HIDDEN_REACH_UUID, "Swamp puddle hidden reach", -1.5D, AttributeModifier.Operation.ADDITION);

    private static final DustParticleOptions SWAMP_DUST = new DustParticleOptions(
            new Vector3f(0.16F, 0.27F, 0.25F), 5.5F);
    private static final DustParticleOptions PUDDLE_DUST = new DustParticleOptions(
        new Vector3f(0.16F, 0.27F, 0.25F), 0.5F);
    private static final String PUDDLE_STATE_ENTERING = "entering";
    private static final String PUDDLE_STATE_VISIBLE = "visible";
    private static final String PUDDLE_STATE_HIDING = "hiding";
    private static final String PUDDLE_STATE_HIDDEN = "hidden";
    private static final String PUDDLE_STATE_SHOWING = "showing";
    private static final String[] SWAMP_MELEE_COMBO = {"punch_right", "punch_left", "kick_right", "kick_left"};

    private SwampDemonArt() {
    }

    public static boolean isMtFujikasane(Level level) {
        return level != null && level.dimension().equals(MT_FUJIKASANE_LEVEL);
    }

    public static boolean hasDebugDimensionOverride(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(DEBUG_DIMENSIONS_ACTIVE_TAG);
    }

    public static void setDebugDimensionOverride(LivingEntity entity, float height, float eyeHeight) {
        entity.getPersistentData().putBoolean(DEBUG_DIMENSIONS_ACTIVE_TAG, true);
        entity.getPersistentData().putFloat(DEBUG_DIMENSIONS_HEIGHT_TAG, height);
        entity.getPersistentData().putFloat(DEBUG_DIMENSIONS_EYE_HEIGHT_TAG, eyeHeight);
        applyCurrentDimensions(entity);
        syncDebugDimensionOverride(entity);
    }

    public static void clearDebugDimensionOverride(LivingEntity entity) {
        entity.getPersistentData().remove(DEBUG_DIMENSIONS_ACTIVE_TAG);
        entity.getPersistentData().remove(DEBUG_DIMENSIONS_HEIGHT_TAG);
        entity.getPersistentData().remove(DEBUG_DIMENSIONS_EYE_HEIGHT_TAG);
        applyCurrentDimensions(entity);
        syncDebugDimensionOverride(entity);
    }

    public static float getDebugDimensionHeight(LivingEntity entity) {
        return entity.getPersistentData().getFloat(DEBUG_DIMENSIONS_HEIGHT_TAG);
    }

    public static float getDebugDimensionEyeHeight(LivingEntity entity) {
        return entity.getPersistentData().getFloat(DEBUG_DIMENSIONS_EYE_HEIGHT_TAG);
    }

    public static void applyCurrentDimensions(LivingEntity entity) {
        entity.refreshDimensions();
        float targetHeight = hasDebugDimensionOverride(entity) ? getDebugDimensionHeight(entity)
            : (isPuddled(entity) ? getTargetPuddleHeight(entity) : entity.getDimensions(entity.getPose()).height);
        float width = entity.getDimensions(entity.getPose()).width;
        double halfWidth = width * 0.5D;
        Vec3 pos = entity.position();
        entity.setBoundingBox(new AABB(
            pos.x - halfWidth,
            pos.y,
            pos.z - halfWidth,
            pos.x + halfWidth,
            pos.y + targetHeight,
            pos.z + halfWidth
        ));
    }

    public static String buildDimensionDebugSummary(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        return String.format(
            "pose=%s dims=(%.3f x %.3f) bb=(%.3f x %.3f) eye=%.3f minY=%.3f maxY=%.3f debug=%s puddled=%s hidden=%s",
            entity.getPose().name(),
            entity.getDimensions(entity.getPose()).width,
            entity.getDimensions(entity.getPose()).height,
            box.getXsize(),
            box.getYsize(),
            entity.getEyeHeight(),
            box.minY,
            box.maxY,
            hasDebugDimensionOverride(entity),
            isPuddled(entity),
            isPuddleFullyHidden(entity)
        );
    }

    public static void syncDebugDimensionOverride(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket packet =
            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DebugPlayerDimensionsPacket(
                entity.getId(),
                hasDebugDimensionOverride(entity),
                hasDebugDimensionOverride(entity) ? getDebugDimensionHeight(entity) : 0.0F,
                hasDebugDimensionOverride(entity) ? getDebugDimensionEyeHeight(entity) : 0.0F
            );
        ModNetworking.sendToNearby(packet, serverLevel, entity.getX(), entity.getY(), entity.getZ(), 96.0D);
        if (entity instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(packet, serverPlayer);
        }
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }

        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Swamp Demon Art", createTechnique());
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Swamp Demon Art",
                List.of(
                new BloodDemonArtForm(FORM_PUDDLE, "Puddle", "Melt into a nearly flat puddle for up to one minute.", 2, SwampDemonArt::executePuddle),
                new BloodDemonArtForm(FORM_SWAMP_HANDS, "Swamp Hands", "Launch hands out of the ground for a long range attack.", 3, SwampDemonArt::executeSwampHands),
                new BloodDemonArtForm(FORM_SWAMP_DOMAIN, "Swamp Domain", "Create a swamp portal puddle for twenty seconds.", 10, SwampDemonArt::executeSwampDomain),
                new BloodDemonArtForm(FORM_SWAMPY_CLOUD, "Swampy Cloud", "Blanket an area in murky dust that blinds and slows.", 4, SwampDemonArt::executeSwampyCloud),
                new BloodDemonArtForm(FORM_AQUATIC_DASH, "Aquatic Dash", "Surge forward through water in a burst of bubbles.", 3, SwampDemonArt::executeAquaticDash)
            ),
            0x28463f
        );
    }

    private static void executePuddle(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity.isInWaterOrBubble()) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.literal("Puddle cannot be used in water.")
                    .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (isPuddled(entity)) {
            exitPuddle(entity, true);
            return;
        }

        activatePuddle(entity, serverLevel);
    }

    private static void applyPuddleSlowness(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN,
            PUDDLE_DURATION_TICKS,
            1,      // amplifier 1 = Slowness II
            true,   // ambient
            false,  // show particles
            false   // show icon
        ));
    }

    private static void removePuddleSlowness(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (effect != null && effect.getAmplifier() == 1 && effect.isAmbient()) {
            entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void executeSwampHands(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity
                && serverLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            return;
        }
        
        throwHands(entity, serverLevel);

        if (isPuddled(entity)) {
            playAnimation(entity, "puddle_attack", 10);
        }
        else {
            playAnimation(entity, "sword_overhead", 10);
        }
    }

    private static void executeSwampDomain(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isMtFujikasane(serverLevel)) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.literal("Swamp Domain cannot be used during Final Selection.")
                    .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity
            && serverLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            return;
        }

        entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, SWAMP_DOMAIN_WATER_BREATHING_TICKS, 0, false, true, true));

        //exitPuddle(entity, false);

        if (serverLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            if (!teleportOutOfSwampDomain(entity, serverLevel)) {
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.literal("Swamp Domain is unavailable right now.")
                        .withStyle(ChatFormatting.RED), true);
                }
                return;
            }

            playAnimation(entity, "sword_to_lower", 10);
            return;
        }

        launchSwampDomainProjectile(entity, serverLevel);
        if (isPuddled(entity)) {
            playAnimation(entity, "puddle_attack", 10);
        }
        else {
            playAnimation(entity, "sword_overhead", 10);
        }
    }

    private static void executeSwampyCloud(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        //exitPuddle(entity, false);
        if (isPuddled(entity)) {
            playAnimation(entity, "puddle_attack", 10);
        }
        else {
            playAnimation(entity, "sword_rotate", 10);
        }

        Vec3 center = findTargetPoint(entity, 10.0D);
        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0F, 0.55F);

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                    return;
                }

                double radius = 5.5D;
                activeLevel.sendParticles(SWAMP_DUST, center.x, center.y + 1.0D, center.z, 90, radius, 1.2D, radius, 0.003D);
                activeLevel.sendParticles(ParticleTypes.SMOKE, center.x, center.y + 1.0D, center.z, 45, radius * 0.7D, 0.8D, radius * 0.7D, 0.01D);

                AABB cloudArea = new AABB(center, center).inflate(radius, 2.0D, radius);
                for (LivingEntity target : activeLevel.getEntitiesOfClass(LivingEntity.class, cloudArea,
                    living -> living != entity && living.isAlive() && !living.isSpectator())) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                }

                tick++;
            }
        }, 1, 60);
    }

    private static void executeAquaticDash(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        exitPuddle(entity, false);

        if (!entity.isInWaterOrBubble()) {
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.literal("Aquatic Dash can only be used while swimming.")
                    .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        playAnimation(entity, KimetsunoyaibaMultiplayer.MODID + ":swim", 10);

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!(entity.level() instanceof ServerLevel activeLevel) || !entity.isAlive()) {
                    return;
                }

                tick++;
                if (!entity.isInWaterOrBubble()) {
                    return;
                }

                Vec3 look = entity.getLookAngle();
                if (look.lengthSqr() < 1.0E-4D) {
                    look = new Vec3(0.0D, 0.0D, 1.0D);
                }
                look = look.normalize();

                MovementHelper.setVelocity(entity, look.scale(0.9D).add(0.0D, Math.max(entity.getDeltaMovement().y, 0.02D), 0.0D));
                activeLevel.sendParticles(ParticleTypes.BUBBLE, entity.getX(), entity.getY(0.4D), entity.getZ(), 16, 0.35D, 0.3D, 0.35D, 0.08D);
                activeLevel.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY(0.4D), entity.getZ(), 10, 0.25D, 0.15D, 0.25D, 0.05D);
                activeLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.PLAYER_SPLASH, SoundSource.HOSTILE, 0.5F, 1.05F);
                activeLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.HOSTILE, 0.45F, 1.2F);

                AABB dashArea = entity.getBoundingBox().inflate(3.0D);
                float damage = Damager.calculateScaledDamage(entity, 5);
                for (LivingEntity target : activeLevel.getEntitiesOfClass(LivingEntity.class, dashArea,
                    living -> living != entity && living.isAlive() && !living.isSpectator())) {
                    Damager.hurt(entity, target, damage);
                }

                if (tick >= 10) {
                    playAnimation(entity, "cancel", 1);
                }
            }
        }, 1, 10);
    }

    private static void activatePuddle(LivingEntity entity, ServerLevel level) {
        entity.getPersistentData().putBoolean(PUDDLE_ACTIVE_TAG, true);
        entity.getPersistentData().putLong(PUDDLE_END_TICK_TAG, level.getGameTime() + PUDDLE_DURATION_TICKS);
        entity.getPersistentData().putString(PUDDLE_STATE_TAG, PUDDLE_STATE_ENTERING);
        entity.getPersistentData().putLong(PUDDLE_STATE_END_TICK_TAG, level.getGameTime() + PUDDLE_TRANSITION_TICKS);
        entity.getPersistentData().remove(PUDDLE_ATTACK_END_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_REFRESH_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_TAG);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_END_TICK_TAG);
        applyPuddleSlowness(entity);
        entity.addTag(PUDDLE_ACTIVE_SYNC_TAG);
        entity.removeTag(PUDDLE_HIDDEN_SYNC_TAG);
        syncPuddleState(entity);
        entity.refreshDimensions();
        syncPuddleCollisionBox(entity);
        setPuddleShiftState(entity, false);
        playPuddleAnimation(entity, "puddle_enter", PUDDLE_TRANSITION_TICKS);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 0.9F, 0.65F);
        ensurePuddleAvatar(level, entity);
    }

    
    public static void activateSpawnPuddle(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || isPuddled(entity) || entity.isInWaterOrBubble()) {
            return;
        }

        entity.getPersistentData().putBoolean(PUDDLE_ACTIVE_TAG, true);
        entity.getPersistentData().putLong(PUDDLE_END_TICK_TAG, level.getGameTime() + PUDDLE_DURATION_TICKS);

        entity.getPersistentData().remove(PUDDLE_ATTACK_END_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_REFRESH_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_END_TICK_TAG);

        // start hidden/crouched
        entity.getPersistentData().putBoolean(PUDDLE_DEMON_CROUCH_TAG, true);
        entity.addTag(PUDDLE_ACTIVE_SYNC_TAG);
        entity.addTag(PUDDLE_HIDDEN_SYNC_TAG);

        syncPuddleState(entity);
        entity.refreshDimensions();
        syncPuddleCollisionBox(entity);
        applyPuddleSlowness(entity);
        ensurePuddleAvatar(level, entity);

        // immediately begin rising into visible puddle form
        setPuddleState(entity, PUDDLE_STATE_SHOWING, level.getGameTime() + PUDDLE_TRANSITION_TICKS);
        setPuddleShiftState(entity, false);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_TAG);
        playPuddleAnimation(entity, "puddle_show", PUDDLE_TRANSITION_TICKS);

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 0.9F, 0.8F);
    }

    public static void markAbilityUse(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            player.getPersistentData().putLong(SWAMP_LAST_ABILITY_USE_TICK_TAG, serverLevel.getGameTime());
        }
    }

    public static boolean isPuddled(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(PUDDLE_ACTIVE_TAG)
            || entity.getTags().contains(PUDDLE_ACTIVE_SYNC_TAG)
            || entity.getPersistentData().getBoolean(CLIENT_PUDDLE_ACTIVE_TAG);
    }

    public static boolean isPuddleFullyHidden(LivingEntity entity) {
        return isPuddled(entity) && (PUDDLE_STATE_HIDDEN.equals(entity.getPersistentData().getString(PUDDLE_STATE_TAG))
            || entity.getTags().contains(PUDDLE_HIDDEN_SYNC_TAG)
            || entity.getPersistentData().getBoolean(CLIENT_PUDDLE_HIDDEN_TAG));
    }

    private static String getPuddleState(LivingEntity entity) {
        String state = entity.getPersistentData().getString(PUDDLE_STATE_TAG);
        if (state.isEmpty() && entity.getTags().contains(PUDDLE_HIDDEN_SYNC_TAG)) {
            return PUDDLE_STATE_HIDDEN;
        }
        if (state.isEmpty() && entity.getTags().contains(PUDDLE_ACTIVE_SYNC_TAG)) {
            return PUDDLE_STATE_VISIBLE;
        }
        return state.isEmpty() ? PUDDLE_STATE_VISIBLE : state;
    }

    private static boolean isPuddleTransitionState(String state) {
        return PUDDLE_STATE_ENTERING.equals(state)
            || PUDDLE_STATE_HIDING.equals(state)
            || PUDDLE_STATE_SHOWING.equals(state);
    }

    private static void setPuddleState(LivingEntity entity, String state, long endTick) {
        entity.getPersistentData().putString(PUDDLE_STATE_TAG, state);
        if (PUDDLE_STATE_HIDDEN.equals(state)) {
            entity.addTag(PUDDLE_HIDDEN_SYNC_TAG);
        } else {
            entity.removeTag(PUDDLE_HIDDEN_SYNC_TAG);
        }
        syncPuddleState(entity);
        if (endTick > 0L) {
            entity.getPersistentData().putLong(PUDDLE_STATE_END_TICK_TAG, endTick);
        } else {
            entity.getPersistentData().remove(PUDDLE_STATE_END_TICK_TAG);
        }
    }

    private static void exitPuddle(LivingEntity entity, boolean playSound) {
        if (!isPuddled(entity)) {
            return;
        }

        removeHiddenPuddleReach(entity);
        MovementHelper.resetStepHeight(entity);
        removePuddleSlowness(entity);
        entity.getPersistentData().remove(PUDDLE_ACTIVE_TAG);
        entity.getPersistentData().remove(PUDDLE_END_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_ENTITY_TAG);
        entity.getPersistentData().remove(PUDDLE_STATE_TAG);
        entity.getPersistentData().remove(PUDDLE_STATE_END_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_ATTACK_END_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_REFRESH_TICK_TAG);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_TAG);
        entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_END_TICK_TAG);
        entity.removeTag(PUDDLE_ACTIVE_SYNC_TAG);
        entity.removeTag(PUDDLE_HIDDEN_SYNC_TAG);
        syncPuddleState(entity);
        setPuddleShiftState(entity, false);
        //entity.removeEffect(MobEffects.INVISIBILITY);
        entity.refreshDimensions();
        entity.setBoundingBox(entity.getDimensions(entity.getPose()).makeBoundingBox(entity.position()));
        playPuddleAnimation(entity, "puddle_exit", PUDDLE_EXIT_ANIMATION_TICKS);
        if (entity.level() instanceof ServerLevel serverLevel) {
            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!entity.isAlive()) {
                    return;
                }
                if (entity instanceof AbstractDemonEntity demon) {
                    demon.playGeckoAnimation("idle", 0);
                } else if (entity instanceof Player player) {
                    ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
                }
            }, PUDDLE_EXIT_ANIMATION_TICKS + 1);
        }
        if (playSound && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SLIME_JUMP_SMALL, SoundSource.HOSTILE, 0.8F, 1.1F);
        }
    }

    private static void ensurePuddleAvatar(ServerLevel level, LivingEntity entity) {
        UUID puddleId = entity.getPersistentData().hasUUID(PUDDLE_ENTITY_TAG)
            ? entity.getPersistentData().getUUID(PUDDLE_ENTITY_TAG) : null;
        if (puddleId != null && level.getEntity(puddleId) instanceof SwampPuddleEntity puddle && puddle.isAlive()) {
            puddle.bindAvatar(entity);
            return;
        }

        SwampPuddleEntity puddle = ModEntities.SWAMP_PUDDLE.get().create(level);
        if (puddle == null) {
            return;
        }
        puddle.bindAvatar(entity);
        puddle.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0.0F);
        if (level.addFreshEntity(puddle)) {
            entity.getPersistentData().putUUID(PUDDLE_ENTITY_TAG, puddle.getUUID());
        }
    }

    private static void spawnSwampDomainPortal(ServerLevel sourceLevel, Vec3 sourcePos, Vec3 targetPos, LivingEntity caster) {
        if (isMtFujikasane(sourceLevel)) {
            return;
        }

        SwampPuddleEntity sourcePortal = ModEntities.SWAMP_PUDDLE.get().create(sourceLevel);
        if (sourcePortal == null) {
            return;
        }

        sourcePortal.makePortal(SWAMP_DOMAIN_LEVEL, targetPos, PORTAL_DURATION_TICKS, 1.85F);
        sourcePortal.moveTo(sourcePos.x, sourcePos.y, sourcePos.z, 0.0F, 0.0F);
        sourceLevel.addFreshEntity(sourcePortal);
        storeReturnPosition(caster, sourceLevel, sourcePos);
    }

    private static void syncPuddleState(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        SwampPuddleStatePacket packet = new SwampPuddleStatePacket(entity.getId(), isPuddled(entity), isPuddleFullyHidden(entity));
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(entity) <= 128.0D * 128.0D || player.getId() == entity.getId()) {
                ModNetworking.sendToPlayer(packet, player);
            }
        }
    }

    private static boolean isValidGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return state.isCollisionShapeFullBlock(level, pos) // solid
            || state.getFluidState().is(FluidTags.WATER);  // water
    }

    private static BlockPos findValidSwampHandSpawn(ServerLevel level, BlockPos start, int limit) {
        BlockPos.MutableBlockPos checkPos = start.mutable();
        int c = 0;

        // Try going down first
        while (!isValidGround(level, checkPos.below())) {
            checkPos.move(0, -1, 0);
            if (++c > limit) break;
        }

        if (c <= limit) {
            return checkPos.immutable();
        }

        // Reset and try going up
        checkPos.set(start);
        c = 0;

        while (!isValidGround(level, checkPos.below())) {
            checkPos.move(0, 1, 0);
            if (++c > limit) break;
        }

        return c <= limit ? checkPos.immutable() : null;
    }

    private static void throwHands(LivingEntity caster, ServerLevel sourceLevel) {
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        final Vec3[] velocity = {look.normalize().scale(1)};
        final Vec3[] currentPos = { start };
        
        sourceLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
            SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.HOSTILE, 0.7F, 0.7F);

        int lifetimeTicks = 30;

        int tickInterval = 2;

        AbilityScheduler.scheduleRepeating(caster, new Runnable() {
            private boolean finished = false;
            private int ticks = 0;

            @Override
            public void run() {
                if (finished || !(caster.level() instanceof ServerLevel activeLevel) || !caster.isAlive()) {
                    finished = true;
                    return;
                }

                //velocity[0] = velocity[0].add(0.0D, -0.01D, 0.0D);
                Vec3 nextPos = currentPos[0].add(velocity[0]);
                BlockHitResult hit = activeLevel.clip(new ClipContext(currentPos[0], nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    finished = true;
                    return;
                }

                currentPos[0] = nextPos;

                if (ticks % tickInterval == 0) {
                    Vec3 basePos = currentPos[0];

                    int limit = 10;

                    

                    // Use nearest block in X/Z instead of flooring/truncating
                    int baseX = (int) Math.round(basePos.x);
                    int baseY = Mth.floor(basePos.y);
                    int baseZ = (int) Math.round(basePos.z);

                    // Try center first, then random horizontal neighboring blocks
                    java.util.List<BlockPos> candidateBases = new java.util.ArrayList<>();
                    candidateBases.add(new BlockPos(baseX, baseY, baseZ));

                    int start = sourceLevel.random.nextInt(4);

                    BlockPos[] offsets = new BlockPos[] {
                        new BlockPos(1, 0, 0),
                        new BlockPos(-1, 0, 0),
                        new BlockPos(0, 0, 1),
                        new BlockPos(0, 0, -1)
                    };

                    for (int i = 0; i < 4; i++) {
                        BlockPos offset = offsets[(start + i) % 4];
                        candidateBases.add(new BlockPos(baseX + offset.getX(), baseY, baseZ + offset.getZ()));
                    }

                    for (BlockPos candidateBase : candidateBases) {
                        BlockPos foundPos = findValidSwampHandSpawn(sourceLevel, candidateBase, limit);
                        if (foundPos == null) {
                            continue;
                        }

                        Vec3 spawnPos = new Vec3(
                            foundPos.getX() + 0.5D,
                            foundPos.getY(),
                            foundPos.getZ() + 0.5D
                        );

                        if (hasSwampHandAt(sourceLevel, spawnPos)) {
                            continue;
                        }

                        sourceLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 1.2F, 1.2F);
                        SwampHandEntity.spawn(sourceLevel, spawnPos, caster);
                        break;
                    }
                }

                //activeLevel.sendParticles(SWAMP_DUST, currentPos[0].x, currentPos[0].y, currentPos[0].z, 6, 0.08D, 0.08D, 0.08D, 0.001D);
                //activeLevel.sendParticles(ParticleTypes.SMOKE, currentPos[0].x, currentPos[0].y, currentPos[0].z, 2, 0.04D, 0.04D, 0.04D, 0.0D);

                ticks++;
                if (ticks >= lifetimeTicks) {
                    finished = true;
                }
            }
        }, 1, lifetimeTicks);
    }

    private static boolean hasSwampHandAt(ServerLevel level, Vec3 spawnPos) {
        AABB box = new AABB(
            spawnPos.x - 1D, spawnPos.y - 1D, spawnPos.z - 1D,
            spawnPos.x + 1D, spawnPos.y + 1.5D, spawnPos.z + 1D
        );

        return !level.getEntitiesOfClass(SwampHandEntity.class, box).isEmpty();
    }

    private static void launchSwampDomainProjectile(LivingEntity caster, ServerLevel sourceLevel) {
        if (isMtFujikasane(sourceLevel)) {
            return;
        }

        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        final Vec3[] velocity = {look.normalize().scale(SWAMP_DOMAIN_PROJECTILE_SPEED)};
        final Vec3[] currentPos = {start};

        sourceLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
            SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.HOSTILE, 0.7F, 0.7F);

        AbilityScheduler.scheduleRepeating(caster, new Runnable() {
            private boolean finished = false;
            private int ticks = 0;

            @Override
            public void run() {
                if (finished || !(caster.level() instanceof ServerLevel activeLevel) || !caster.isAlive()) {
                    finished = true;
                    return;
                }

                velocity[0] = velocity[0].add(0.0D, -0.01D, 0.0D);
                Vec3 nextPos = currentPos[0].add(velocity[0]);
                BlockHitResult hit = activeLevel.clip(new ClipContext(currentPos[0], nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    Vec3 impact = clampPortalSpawn(activeLevel, hit.getLocation());
                    Vec3 targetPos = new Vec3(impact.x, 32.0D, impact.z);
                    spawnSwampDomainPortal(activeLevel, impact, targetPos, caster);
                    activeLevel.playSound(null, impact.x, impact.y, impact.z,
                        SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.8F, 0.8F);
                    activeLevel.sendParticles(SWAMP_DUST, impact.x, impact.y + 0.05D, impact.z, 35, 0.45D, 0.02D, 0.45D, 0.002D);
                    finished = true;
                    return;
                }

                AABB captureArea = new AABB(currentPos[0], nextPos).inflate(2.0D);

                for (LivingEntity target : activeLevel.getEntitiesOfClass(LivingEntity.class, captureArea,
                    living -> living.isAlive()
                        && living != caster
                        && !(living instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity))) {

                    Vec3 targetPos = new Vec3(nextPos.x, 32.0D, nextPos.z);
                    teleportThroughPortal(target, SWAMP_DOMAIN_LEVEL, targetPos);

                    activeLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.7F, 1.1F);
                    activeLevel.sendParticles(SWAMP_DUST, target.getX(), target.getY() + 0.05D, target.getZ(),
                        20, 0.3D, 0.05D, 0.3D, 0.002D);
                }

                currentPos[0] = nextPos;
                activeLevel.sendParticles(SWAMP_DUST, currentPos[0].x, currentPos[0].y, currentPos[0].z, 6, 0.08D, 0.08D, 0.08D, 0.001D);
                activeLevel.sendParticles(ParticleTypes.SMOKE, currentPos[0].x, currentPos[0].y, currentPos[0].z, 2, 0.04D, 0.04D, 0.04D, 0.0D);

                ticks++;
                if (ticks >= SWAMP_DOMAIN_PROJECTILE_LIFETIME_TICKS) {
                    finished = true;
                }
            }
        }, 1, SWAMP_DOMAIN_PROJECTILE_LIFETIME_TICKS);
    }

    private static void storeReturnPosition(LivingEntity entity, ServerLevel level, Vec3 pos) {
        entity.getPersistentData().putString(SWAMP_RETURN_DIM_TAG, level.dimension().location().toString());
        entity.getPersistentData().putDouble(SWAMP_RETURN_X_TAG, pos.x);
        entity.getPersistentData().putDouble(SWAMP_RETURN_Y_TAG, Math.max(level.getMinBuildHeight() + 1, pos.y));
        entity.getPersistentData().putDouble(SWAMP_RETURN_Z_TAG, pos.z);
        entity.getPersistentData().putFloat(SWAMP_RETURN_YAW_TAG, entity.getYRot());
        entity.getPersistentData().putFloat(SWAMP_RETURN_PITCH_TAG, entity.getXRot());
    }

    private static ServerLevel resolvePortalTargetLevel(ServerLevel currentLevel, LivingEntity entity) {
        if (!currentLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            return currentLevel.getServer().getLevel(SWAMP_DOMAIN_LEVEL);
        }

        return resolveSwampDomainReturnLevel(currentLevel, entity);
    }

    private static ServerLevel resolveSwampDomainReturnLevel(ServerLevel currentLevel, Entity entity) {
        String storedDim = entity.getPersistentData().getString(SWAMP_RETURN_DIM_TAG);
        if (!storedDim.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(storedDim);
            if (id != null && !SWAMP_DOMAIN_LEVEL.location().equals(id)) {
                ResourceKey<Level> returnKey = ResourceKey.create(Registries.DIMENSION, id);
                ServerLevel returnLevel = currentLevel.getServer().getLevel(returnKey);
                if (returnLevel != null) {
                    return returnLevel;
                }
            }
        }

        return currentLevel.getServer().overworld();
    }

    private static boolean teleportOutOfSwampDomain(LivingEntity entity, ServerLevel sourceLevel) {
        if (!sourceLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            return false;
        }

        ServerLevel returnLevel = resolveSwampDomainReturnLevel(sourceLevel, entity);
        Vec3 returnPos = getStoredReturnPosition(entity);
        if (returnPos == null || returnLevel.dimension().equals(SWAMP_DOMAIN_LEVEL)) {
            BlockPos spawn = returnLevel.getSharedSpawnPos();
            returnPos = new Vec3(spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D);
        }

        return teleportThroughPortal(entity, returnLevel.dimension(), returnPos, true);
    }

    private static Vec3 clampPortalSpawn(ServerLevel level, Vec3 pos) {
        BlockPos top = BlockPos.containing(pos);
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        return new Vec3(pos.x, Math.min(maxY, Math.max(minY, top.getY())) + 0.02D, pos.z);
    }

    private static Vec3 findTargetPoint(LivingEntity entity, double distance) {
        Vec3 eye = entity.getEyePosition();
        Vec3 target = eye.add(entity.getLookAngle().scale(distance));
        BlockHitResult hit = entity.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
        if (hit.getType() == HitResult.Type.MISS) {
            return target;
        }
        return hit.getLocation();
    }

    private static Vec3 getStoredReturnPosition(Entity entity) {
        if (!entity.getPersistentData().contains(SWAMP_RETURN_X_TAG)) {
            return null;
        }
        return new Vec3(
            entity.getPersistentData().getDouble(SWAMP_RETURN_X_TAG),
            entity.getPersistentData().getDouble(SWAMP_RETURN_Y_TAG),
            entity.getPersistentData().getDouble(SWAMP_RETURN_Z_TAG)
        );
    }

    private static float getStoredReturnYaw(Entity entity) {
        return entity.getPersistentData().getFloat(SWAMP_RETURN_YAW_TAG);
    }

    private static float getStoredReturnPitch(Entity entity) {
        return entity.getPersistentData().getFloat(SWAMP_RETURN_PITCH_TAG);
    }

    public static Vec3 getSwampDomainEntryPosition(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(SWAMP_DOMAIN_ENTRY_X_TAG)) {
            return null;
        }
        return new Vec3(
            entity.getPersistentData().getDouble(SWAMP_DOMAIN_ENTRY_X_TAG),
            entity.getPersistentData().getDouble(SWAMP_DOMAIN_ENTRY_Y_TAG),
            entity.getPersistentData().getDouble(SWAMP_DOMAIN_ENTRY_Z_TAG)
        );
    }

    public static Vec3 resolveSwampDomainEntryPosition(Entity entity, Vec3 fallback) {
        Vec3 stored = getSwampDomainEntryPosition(entity);
        return stored != null ? stored : fallback;
    }

    public static double clampSwampDomainDemonY(double y) {
        return Math.min(SWAMP_DOMAIN_MAX_DEMON_Y, y);
    }

    private static boolean isLeavingSwampDomain(ServerLevel sourceLevel, ResourceKey<Level> targetDimension) {
        return sourceLevel.dimension().equals(SWAMP_DOMAIN_LEVEL) && !SWAMP_DOMAIN_LEVEL.equals(targetDimension);
    }

    private static void neutralizeSwampDomainBuoyancy(LivingEntity entity) {
        if (entity instanceof Player
            || !entity.level().dimension().equals(SWAMP_DOMAIN_LEVEL)
            || !entity.isInWaterOrBubble()) {
            return;
        }

        Vec3 movement = entity.getDeltaMovement();
        double yVelocity = movement.y;
        boolean allowControlledSwampDemonRise = entity instanceof SwampDemonEntity demon
            && demon.getTarget() != null
            && demon.getTarget().isAlive();

        if (yVelocity > 0.0D) {
            yVelocity = allowControlledSwampDemonRise
                ? Math.min(yVelocity, SWAMP_DOMAIN_MAX_CONTROLLED_RISE_SPEED)
                : 0.0D;
        } else if (Math.abs(yVelocity) < 0.003D && !allowControlledSwampDemonRise) {
            yVelocity = SWAMP_DOMAIN_PASSIVE_SINK_SPEED;
        }

        if (yVelocity != movement.y) {
            entity.setDeltaMovement(movement.x, yVelocity, movement.z);
            entity.hurtMarked = true;
        }
        entity.fallDistance = 0.0F;
    }

    private static void clearNearbySwampPortals(ServerLevel level, Vec3 center, double radius) {
        AABB area = new AABB(center, center).inflate(radius);
        for (SwampPuddleEntity portal : level.getEntitiesOfClass(SwampPuddleEntity.class, area,
            puddle -> puddle != null && puddle.isAlive() && puddle.isPortalMode())) {
            portal.discard();
        }
    }

    public static void handleAbilityItemUse(Player player, ItemStack stack, int selectedFormId) {
        if (!isPuddled(player)) {
            return;
        }

        Item item = stack.getItem();
        boolean usingThisArtItem =
            item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem bloodDemonArtItem
                && ART_ID.equals(bloodDemonArtItem.getArtId());

        if (usingThisArtItem && selectedFormId == FORM_PUDDLE) {
            return;
        }

        exitPuddle(player, true);
    }

    private static ResourceLocation parseAnimationId(String animation) {
        if (animation.contains(":")) {
            String[] parts = animation.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animation);
    }

    private static boolean isLoopingPlayerAnimation(String animation) {
        String key = animation;
        int namespaceSplit = key.indexOf(':');
        if (namespaceSplit >= 0) {
            key = key.substring(namespaceSplit + 1);
        }
        return "puddle_idle".equals(key) || "puddle_walk".equals(key) || "invisibility".equals(key);
    }

    private static void playPlayerAnimation(Player player, String animation, int duration) {
        if (player.level().isClientSide) {
            return;
        }

        ResourceLocation animationId = parseAnimationId(animation);
        long animationSerial = player.getPersistentData().getLong(SWAMP_PLAYER_ANIM_SERIAL_TAG) + 1L;
        player.getPersistentData().putLong(SWAMP_PLAYER_ANIM_SERIAL_TAG, animationSerial);
        ModNetworking.sendToAllClients(new AnimationSyncPacket(
            player.getUUID(),
            animationId,
            0,
            Math.max(1, duration),
            isLoopingPlayerAnimation(animation),
            false,
            null,
            1.0F,
            3000
        ));

        if (duration > 0 && !isLoopingPlayerAnimation(animation)) {
            AbilityScheduler.scheduleOnce(player, () -> {
                if (player.isAlive() && player.getPersistentData().getLong(SWAMP_PLAYER_ANIM_SERIAL_TAG) == animationSerial) {
                    ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
                }
            }, duration + 1);
        }
    }

    private static void playAnimation(LivingEntity entity, String animation, int duration) {
        String resolvedAnimation = animation;
        int namespaceSplit = animation.indexOf(':');
        if (namespaceSplit >= 0) {
            resolvedAnimation = animation.substring(namespaceSplit + 1);
        }

        if (entity instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(resolvedAnimation, duration);
        } else if (entity instanceof Player player) {
            playPlayerAnimation(player, animation, duration);
        }
    }

    private static void playPuddleAnimation(LivingEntity entity, String animation, int duration) {
        if (entity instanceof Player) {
            playAnimation(entity, KimetsunoyaibaMultiplayer.MODID + ":" + animation, duration);
            return;
        }
        playAnimation(entity, animation, duration);
    }

    public static void onPuddleAttack(LivingEntity entity) {
        if (!isPuddled(entity) || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        entity.getPersistentData().putLong(PUDDLE_ATTACK_END_TICK_TAG, serverLevel.getGameTime() + PUDDLE_TRANSITION_TICKS);
        entity.getPersistentData().remove(PUDDLE_LOOP_TAG);
        entity.getPersistentData().remove(PUDDLE_LOOP_REFRESH_TICK_TAG);
        playPuddleAnimation(entity, isPuddleFullyHidden(entity) ? "puddle_hide_attack" : "puddle_attack", PUDDLE_TRANSITION_TICKS);
    }

    private static boolean isSwampDemonArtMeleeItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem bloodDemonArtItem) {
            return ART_ID.equals(bloodDemonArtItem.getArtId());
        }
        return item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
    }

    private static boolean canHandlePlayerLeftClick(Player player) {
        return isPuddled(player) || isSwampDemonArtMeleeItem(player.getMainHandItem());
    }

    private static boolean markLeftClickHandled(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        long gameTime = serverLevel.getGameTime();
        long lastHandled = player.getPersistentData().getLong(SWAMP_LAST_LEFT_CLICK_TICK_TAG);
        if (lastHandled == gameTime) {
            return false;
        }

        player.getPersistentData().putLong(SWAMP_LAST_LEFT_CLICK_TICK_TAG, gameTime);
        return true;
    }

    private static boolean shouldIgnoreSwingForAbilityUse(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        long gameTime = serverLevel.getGameTime();
        long lastAbilityUseTick = player.getPersistentData().getLong(SWAMP_LAST_ABILITY_USE_TICK_TAG);
        return gameTime - lastAbilityUseTick <= 2L;
    }

    private static void handlePlayerLeftClick(Player player, Entity target) {
        if (!canHandlePlayerLeftClick(player) || !markLeftClickHandled(player)) {
            return;
        }

        if (isPuddled(player)) {
            onPuddleAttack(player);
            if (target != null && isPuddleFullyHidden(player)) {
                ambushTeleportTarget(player, target);
            }
            return;
        }

        playRegularMeleeCombo(player);
    }

    public static void playRegularMeleeCombo(LivingEntity entity) {
        if (isPuddled(entity)) {
            return;
        }

        int comboIndex = Math.floorMod(entity.getPersistentData().getInt(SWAMP_COMBO_INDEX_TAG), SWAMP_MELEE_COMBO.length);
        playAnimation(entity, SWAMP_MELEE_COMBO[comboIndex], 10);
        entity.getPersistentData().putInt(SWAMP_COMBO_INDEX_TAG, (comboIndex + 1) % SWAMP_MELEE_COMBO.length);
    }

    public static boolean isSwampDemonArtItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem bloodDemonArtItem
            && ART_ID.equals(bloodDemonArtItem.getArtId());
    }

    private static boolean shouldHidePuddle(LivingEntity entity, ServerLevel level) {
        if (entity instanceof Player player) {
            return player.isShiftKeyDown();
        }

        if (entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity) {
            if (entity.getPersistentData().getBoolean(PUDDLE_DEMON_CROUCH_TAG)) {
                long crouchEndTick = entity.getPersistentData().getLong(PUDDLE_DEMON_CROUCH_END_TICK_TAG);
                if (level.getGameTime() >= crouchEndTick) {
                    entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_TAG);
                    entity.getPersistentData().remove(PUDDLE_DEMON_CROUCH_END_TICK_TAG);
                    return false;
                }
                return true;
            }

            if (entity.getRandom().nextInt(200) == 0) {
                entity.getPersistentData().putBoolean(PUDDLE_DEMON_CROUCH_TAG, true);
                entity.getPersistentData().putLong(PUDDLE_DEMON_CROUCH_END_TICK_TAG,
                    level.getGameTime() + (20L * (10 + entity.getRandom().nextInt(21))));
                return true;
            }
        }

        return false;
    }

    private static void setPuddleShiftState(LivingEntity entity, boolean crouching) {
        if (!(entity instanceof Player)) {
            entity.setShiftKeyDown(crouching);
        }
    }

    private static void syncPuddleCollisionBox(LivingEntity entity) {
        if (!isPuddled(entity)) {
            return;
        }

        float targetHeight = getTargetPuddleHeight(entity);
        if (Math.abs((float) entity.getBoundingBox().getYsize() - targetHeight) > 0.02F) {
            entity.refreshDimensions();
        }

        float width = entity.getDimensions(entity.getPose()).width;
        double halfWidth = width * 0.5D;
        Vec3 pos = entity.position();
        entity.setBoundingBox(new AABB(
            pos.x - halfWidth,
            pos.y,
            pos.z - halfWidth,
            pos.x + halfWidth,
            pos.y + targetHeight,
            pos.z + halfWidth
        ));
    }

    private static float getTargetPuddleHeight(LivingEntity entity) {
        return isPuddleFullyHidden(entity) ? 0.2F : 0.95F;
    }

    private static float getTargetPuddleEyeHeight(LivingEntity entity) {
        return isPuddleFullyHidden(entity) ? 0.3F : 0.95F;
    }

    private static void syncHiddenPuddleReach(LivingEntity entity) {
        AttributeInstance reach = entity.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (reach == null) {
            return;
        }

        boolean shouldLimitReach = isPuddleFullyHidden(entity);
        boolean hasModifier = reach.getModifier(PUDDLE_HIDDEN_REACH_UUID) != null;
        if (shouldLimitReach && !hasModifier) {
            reach.addTransientModifier(PUDDLE_HIDDEN_REACH_MODIFIER);
        } else if (!shouldLimitReach && hasModifier) {
            reach.removeModifier(PUDDLE_HIDDEN_REACH_UUID);
        }
    }

    private static void removeHiddenPuddleReach(LivingEntity entity) {
        AttributeInstance reach = entity.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (reach != null && reach.getModifier(PUDDLE_HIDDEN_REACH_UUID) != null) {
            reach.removeModifier(PUDDLE_HIDDEN_REACH_UUID);
        }
    }

    private static void ambushTeleportTarget(LivingEntity attacker, Entity target) {
        if (!(attacker.level() instanceof ServerLevel serverLevel) || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        if (serverLevel.dimension().equals(SWAMP_DOMAIN_LEVEL) || isMtFujikasane(serverLevel)) {
            return;
        }

        ServerLevel swampDomain = serverLevel.getServer().getLevel(SWAMP_DOMAIN_LEVEL);
        if (swampDomain == null) {
            return;
        }

        Vec3 targetPos = new Vec3(attacker.getX(), 32.0D, attacker.getZ());
        teleportThroughPortal(livingTarget, SWAMP_DOMAIN_LEVEL, targetPos);
    }

    private static final String PUDDLE_LAST_X_TAG = "SwampPuddleLastX";
    private static final String PUDDLE_LAST_Z_TAG = "SwampPuddleLastZ";

    private static String getDesiredPuddleLoop(LivingEntity entity) {
        if (!isPuddled(entity)) {
            return null;
        }

        if (isPuddleFullyHidden(entity)) {
            return "invisibility";
        }

        double lastX = entity.getPersistentData().getDouble(PUDDLE_LAST_X_TAG);
        double lastZ = entity.getPersistentData().getDouble(PUDDLE_LAST_Z_TAG);
        double dx = entity.getX() - lastX;
        double dz = entity.getZ() - lastZ;
        double travelSq = (dx * dx) + (dz * dz);

        entity.getPersistentData().putDouble(PUDDLE_LAST_X_TAG, entity.getX());
        entity.getPersistentData().putDouble(PUDDLE_LAST_Z_TAG, entity.getZ());

        boolean activelyMoving = travelSq > 1.0E-5D
            || Math.abs(entity.xxa) > 0.001F
            || Math.abs(entity.zza) > 0.001F;

        return activelyMoving ? "puddle_walk" : "puddle_idle";
    }

    private static void syncPlayerPuddleLoop(LivingEntity entity, ServerLevel level) {
        if (!(entity instanceof Player) || !isPuddled(entity)) {
            return;
        }

        String state = getPuddleState(entity);
        long attackEndTick = entity.getPersistentData().getLong(PUDDLE_ATTACK_END_TICK_TAG);
        if (isPuddleTransitionState(state) || attackEndTick > level.getGameTime()) {
            return;
        }

        String desiredLoop = getDesiredPuddleLoop(entity);
        if (desiredLoop == null) {
            return;
        }

        String currentLoop = entity.getPersistentData().getString(PUDDLE_LOOP_TAG);
        long lastRefreshTick = entity.getPersistentData().getLong(PUDDLE_LOOP_REFRESH_TICK_TAG);
        boolean shouldRefreshHiddenLoop = "invisibility".equals(desiredLoop)
            && level.getGameTime() - lastRefreshTick >= PUDDLE_LOOP_REFRESH_TICKS;
        if (!desiredLoop.equals(currentLoop) || shouldRefreshHiddenLoop) {
            playPuddleAnimation(entity, desiredLoop, PUDDLE_LOOP_REFRESH_TICKS);
            entity.getPersistentData().putString(PUDDLE_LOOP_TAG, desiredLoop);
            entity.getPersistentData().putLong(PUDDLE_LOOP_REFRESH_TICK_TAG, level.getGameTime());
        }
    }

    private static void spawnSwampDomainDemons(ServerLevel level, Vec3 center, LivingEntity target) {
        int count = 3; // start with 3, tune later
        Vec3 densityCenter = target != null
            ? resolveSwampDomainEntryPosition(target, center)
            : center;

        // Check if target is a player doing the kidnapper's bog quest
        boolean isOnKidnappersBogQuest = false;
        if (target instanceof ServerPlayer player) {
            isOnKidnappersBogQuest = player.getPersistentData().getBoolean(
                com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.KIDNAPPERS_BOG_ACTIVE_TAG);
        }

        if (isOnKidnappersBogQuest && !hasNearbySatokosBowDemon(level, densityCenter)) {
            spawnForcedSatokosBowClone(level, center, target);
        }

        for (int i = 0; i < count; i++) {
            int nearbyCount = level.getEntitiesOfClass(
                SwampDemonEntity.class,
                new AABB(densityCenter, densityCenter).inflate(SWAMP_DOMAIN_DEMON_DENSITY_RADIUS)
            ).size();
            if (nearbyCount >= SWAMP_DOMAIN_MAX_SWAMP_DEMONS_PER_RADIUS) {
                continue;
            }

            SwampDemonEntity demon = ModEntities.SWAMP_DEMON.get().create(level);
            if (demon == null) {
                continue;
            }

            double angle = (Math.PI * 2.0D * i) / count;
            double distance = 20.0D;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = clampSwampDomainDemonY(center.y + level.random.nextFloat() * 10);

            demon.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
            demon.setSplitClone(true);
            demon.setHealth(demon.getMaxHealth() * 0.40F);

            if (target != null) {
                demon.setTarget(target);
            }

            level.addFreshEntity(demon);
        }
    }

    private static boolean hasNearbySatokosBowDemon(ServerLevel level, Vec3 center) {
        return !level.getEntitiesOfClass(
            SwampDemonEntity.class,
            new AABB(center, center).inflate(SWAMP_DOMAIN_DEMON_DENSITY_RADIUS),
            demon -> demon.isAlive() && isSatokosBowQuestTarget(demon)
        ).isEmpty();
    }

    private static boolean isSatokosBowQuestTarget(Entity entity) {
        return "swamp_demon_kidnappers_bog_satoko".equals(entity.getPersistentData().getString(
            com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG));
    }

    private static void spawnForcedSatokosBowClone(ServerLevel level, Vec3 center, LivingEntity target) {
        SwampDemonEntity demon = ModEntities.SWAMP_DEMON.get().create(level);
        if (demon == null) {
            return;
        }

        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = 16.0D;
        double x = center.x + Math.cos(angle) * distance;
        double z = center.z + Math.sin(angle) * distance;
        double y = clampSwampDomainDemonY(center.y);

        demon.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        demon.setSplitClone(true);
        demon.setTextureVariant(1 + level.random.nextInt(3));
        demon.setHealth(demon.getMaxHealth() * 0.40F);
        demon.setPersistenceRequired();
        demon.getPersistentData().putString(
            com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG,
            "swamp_demon_kidnappers_bog_satoko");
        demon.setCustomName(Component.literal("Numa, the Swamp Demon"));
        demon.setCustomNameVisible(true);
        demon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SATOKOS_BOW.get()));

        if (target != null) {
            demon.setTarget(target);
        }

        level.addFreshEntity(demon);
    }

    public static boolean teleportThroughPortal(Entity entity, ResourceKey<Level> targetDimension, Vec3 targetPos) {
        return teleportThroughPortal(entity, targetDimension, targetPos, false);
    }

    private static boolean teleportThroughPortal(Entity entity, ResourceKey<Level> targetDimension, Vec3 targetPos,
                                                boolean ignoreSwampDomainExitLock) {
        if (!(entity.level() instanceof ServerLevel sourceLevel)) {
            return false;
        }
        if (isMtFujikasane(sourceLevel) && SWAMP_DOMAIN_LEVEL.equals(targetDimension)) {
            return false;
        }
        if (sourceLevel.dimension().equals(targetDimension)) {
            return false;
        }
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(targetDimension);
        if (targetLevel == null) {
            return false;
        }

        // System.out.println("SWAMP TELEPORT: " + entity.getName().getString()
        //     + " from " + sourceLevel.dimension().location()
        //     + " to " + targetDimension.location()
        //     + " puddled=" + (entity instanceof LivingEntity living && isPuddled(living)));

        boolean leavingSwampDomain = isLeavingSwampDomain(sourceLevel, targetDimension);
        boolean enteringSwampDomain = !sourceLevel.dimension().equals(SWAMP_DOMAIN_LEVEL) && SWAMP_DOMAIN_LEVEL.equals(targetDimension);
        if (isPortalTeleportBlocked(entity, targetDimension, ignoreSwampDomainExitLock)) {
            return false;
        }
        Vec3 storedReturnPos = getStoredReturnPosition(entity);
        Vec3 destinationPos = leavingSwampDomain && storedReturnPos != null ? storedReturnPos : targetPos;
        float destinationYaw = leavingSwampDomain ? getStoredReturnYaw(entity) : entity.getYRot();
        float destinationPitch = leavingSwampDomain ? getStoredReturnPitch(entity) : entity.getXRot();

        if (entity instanceof LivingEntity living) {
            exitPuddle(living, false);
        }

        if (entity instanceof LivingEntity living && !sourceLevel.dimension().equals(SWAMP_DOMAIN_LEVEL) && SWAMP_DOMAIN_LEVEL.equals(targetDimension)) {
            storeReturnPosition(living, sourceLevel, entity.position());
        }

        if (entity instanceof ServerPlayer player) {
            boolean result = player.teleportTo(targetLevel, destinationPos.x, destinationPos.y, destinationPos.z, Set.of(), destinationYaw, destinationPitch);
            if (result) {
                if (enteringSwampDomain) {
                    player.getPersistentData().putDouble(SWAMP_DOMAIN_ENTRY_X_TAG, destinationPos.x);
                    player.getPersistentData().putDouble(SWAMP_DOMAIN_ENTRY_Y_TAG, destinationPos.y);
                    player.getPersistentData().putDouble(SWAMP_DOMAIN_ENTRY_Z_TAG, destinationPos.z);
                }
                applyPortalGracePeriods(player, targetLevel, enteringSwampDomain, leavingSwampDomain);
                targetLevel.playSound(null, destinationPos.x, destinationPos.y, destinationPos.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.75F);

                if (targetDimension.equals(SWAMP_DOMAIN_LEVEL)) {
                    spawnSwampDomainDemons(targetLevel, destinationPos, player);
                    SwampDemonEntity.retargetSwampDomainDemonsToNearestPlayer(targetLevel);
                }
            }
            return result;
        }

        Vec3 finalDestinationPos = destinationPos;
        float finalDestinationPitch = destinationPitch;
        Entity moved = entity.changeDimension(targetLevel, new ITeleporter() {
            @Override
            public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                Entity teleported = repositionEntity.apply(false);
                teleported.moveTo(finalDestinationPos.x, finalDestinationPos.y, finalDestinationPos.z, destinationYaw, finalDestinationPitch);
                return teleported;
            }
        });

        if (moved != null) {
            int cooldown = leavingSwampDomain ? TELEPORT_COOLDOWN_TICKS : TELEPORT_COOLDOWN_TICKS;
            moved.getPersistentData().putLong(PORTAL_COOLDOWN_TAG, targetLevel.getGameTime() + cooldown);
            applyPortalGracePeriods(moved, targetLevel, enteringSwampDomain, leavingSwampDomain);
            if (leavingSwampDomain) {
                clearNearbySwampPortals(targetLevel, destinationPos, 15.0D);
            }
            return true;
        }
        return false;
    }

    private static void applyPortalGracePeriods(Entity entity, ServerLevel currentLevel, boolean enteringSwampDomain, boolean leavingSwampDomain) {
        if (enteringSwampDomain) {
            entity.getPersistentData().putLong(SWAMP_DOMAIN_EXIT_LOCK_TAG, currentLevel.getGameTime() + SWAMP_DOMAIN_MIN_STAY_TICKS);
        }
        if (leavingSwampDomain) {
            entity.getPersistentData().putLong(SWAMP_DOMAIN_ENTRY_LOCK_TAG, currentLevel.getGameTime() + SWAMP_DOMAIN_REENTRY_COOLDOWN_TICKS);
        }
    }

    public static boolean isPortalTeleportBlocked(Entity entity, ResourceKey<Level> targetDimension) {
        return isPortalTeleportBlocked(entity, targetDimension, false);
    }

    private static boolean isPortalTeleportBlocked(Entity entity, ResourceKey<Level> targetDimension,
                                                  boolean ignoreSwampDomainExitLock) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (isMtFujikasane(serverLevel) && SWAMP_DOMAIN_LEVEL.equals(targetDimension)) {
            return true;
        }

        long gameTime = serverLevel.getGameTime();
        if (entity.getPersistentData().getLong(PORTAL_COOLDOWN_TAG) > gameTime) {
            return true;
        }

        boolean leavingSwampDomain = isLeavingSwampDomain(serverLevel, targetDimension);
        if (leavingSwampDomain) {
            return !ignoreSwampDomainExitLock
                && entity.getPersistentData().getLong(SWAMP_DOMAIN_EXIT_LOCK_TAG) > gameTime;
        }

        boolean enteringSwampDomain = !serverLevel.dimension().equals(SWAMP_DOMAIN_LEVEL) && SWAMP_DOMAIN_LEVEL.equals(targetDimension);
        if (enteringSwampDomain) {
            return entity.getPersistentData().getLong(SWAMP_DOMAIN_ENTRY_LOCK_TAG) > gameTime;
        }

        return false;
    }

    public static boolean isPortalCooldownActive(Entity entity) {
        long gameTime = entity.level() instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : 0L;
        return entity.getPersistentData().getLong(PORTAL_COOLDOWN_TAG) > gameTime;
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
    public static class Handler {
        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) {
                boolean puddledNow = isPuddled(entity);
                boolean puddledBefore = entity.getPersistentData().getBoolean(CLIENT_PUDDLE_STATE_CACHE_TAG);
                if (puddledNow != puddledBefore) {
                    entity.refreshDimensions();
                    if (puddledNow) {
                        syncPuddleCollisionBox(entity);
                    } else {
                        entity.setBoundingBox(entity.getDimensions(entity.getPose()).makeBoundingBox(entity.position()));
                    }
                    entity.getPersistentData().putBoolean(CLIENT_PUDDLE_STATE_CACHE_TAG, puddledNow);
                } else if (puddledNow) {
                    syncPuddleCollisionBox(entity);
                }
            }
            if (!entity.level().isClientSide && entity.level().dimension().equals(SWAMP_DOMAIN_LEVEL)) {
                neutralizeSwampDomainBuoyancy(entity);
            }
            if (entity.level().dimension().equals(SWAMP_DOMAIN_LEVEL)
                && !(entity instanceof Player)
                && !(entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampDemonEntity)
                && entity.level() instanceof ServerLevel domainLevel) {
                long entryTick = entity.getPersistentData().contains(SWAMP_DOMAIN_ENTRY_TICK_TAG)
                    ? entity.getPersistentData().getLong(SWAMP_DOMAIN_ENTRY_TICK_TAG)
                    : -1L;
                if (entryTick < 0L) {
                    entity.getPersistentData().putLong(SWAMP_DOMAIN_ENTRY_TICK_TAG, domainLevel.getGameTime());
                } else if (domainLevel.getGameTime() - entryTick >= 20L * 30L) {
                    String storedDim = entity.getPersistentData().getString(SWAMP_RETURN_DIM_TAG);
                    ResourceLocation id = ResourceLocation.tryParse(storedDim);
                    Vec3 returnPos = getStoredReturnPosition(entity);
                    if (id != null && returnPos != null) {
                        teleportThroughPortal(entity, ResourceKey.create(Registries.DIMENSION, id), returnPos);
                        return;
                    }
                }
            } else {
                entity.getPersistentData().remove(SWAMP_DOMAIN_ENTRY_TICK_TAG);
            }

            if (!isPuddled(entity)) {
                return;
            }

            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                removeHiddenPuddleReach(entity);
                return;
            }

            if (entity.isInWaterOrBubble()) {
                exitPuddle(entity, true);
                return;
            }

            if (!entity.isAlive() || serverLevel.getGameTime() >= entity.getPersistentData().getLong(PUDDLE_END_TICK_TAG)) {
                exitPuddle(entity, true);
                return;
            }

            if (entity.tickCount % 10 == 0) {
                ensurePuddleAvatar(serverLevel, entity);
            }

            boolean shouldHide = shouldHidePuddle(entity, serverLevel);
            String state = getPuddleState(entity);
            long stateEndTick = entity.getPersistentData().getLong(PUDDLE_STATE_END_TICK_TAG);

            if (isPuddleTransitionState(state) && serverLevel.getGameTime() >= stateEndTick) {
                if (PUDDLE_STATE_ENTERING.equals(state)) {
                    setPuddleState(entity, PUDDLE_STATE_VISIBLE, 0L);
                    state = PUDDLE_STATE_VISIBLE;
                } else if (PUDDLE_STATE_HIDING.equals(state)) {
                    setPuddleState(entity, PUDDLE_STATE_HIDDEN, 0L);
                    setPuddleShiftState(entity, true);
                    entity.refreshDimensions();
                    syncPuddleCollisionBox(entity);
                    state = PUDDLE_STATE_HIDDEN;
                } else if (PUDDLE_STATE_SHOWING.equals(state)) {
                    setPuddleState(entity, PUDDLE_STATE_VISIBLE, 0L);
                    syncPuddleCollisionBox(entity);
                    state = PUDDLE_STATE_VISIBLE;
                }
            }

            if (PUDDLE_STATE_HIDING.equals(state) && stateEndTick - serverLevel.getGameTime() <= 1L) {
                playPuddleAnimation(entity, "invisibility", 3);
            }

            if (shouldHide) {
                if (PUDDLE_STATE_VISIBLE.equals(state)) {
                    setPuddleState(entity, PUDDLE_STATE_HIDING, serverLevel.getGameTime() + PUDDLE_TRANSITION_TICKS);
                    playPuddleAnimation(entity, "puddle_hide", PUDDLE_TRANSITION_TICKS);
                    state = PUDDLE_STATE_HIDING;
                }
            } else if (PUDDLE_STATE_HIDDEN.equals(state) || PUDDLE_STATE_HIDING.equals(state)) {
                setPuddleState(entity, PUDDLE_STATE_SHOWING, serverLevel.getGameTime() + PUDDLE_TRANSITION_TICKS);
                setPuddleShiftState(entity, false);
                entity.refreshDimensions();
                syncPuddleCollisionBox(entity);
                playPuddleAnimation(entity, "puddle_show", PUDDLE_TRANSITION_TICKS);
                state = PUDDLE_STATE_SHOWING;
            }

            entity.setDeltaMovement(entity.getDeltaMovement().x * 0.82D, Math.min(entity.getDeltaMovement().y, 0.0D), entity.getDeltaMovement().z * 0.82D);
            entity.setSprinting(false);
            entity.fallDistance = 0.0F;
            //entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
            syncPuddleCollisionBox(entity);
            syncHiddenPuddleReach(entity);
            MovementHelper.setStepHeight(entity, PUDDLE_STEP_HEIGHT);
            serverLevel.sendParticles(PUDDLE_DUST, entity.getX(), entity.getY() + 0.03D, entity.getZ(), 4, 0.18D, 0.01D, 0.18D, 0.001D);
            syncPlayerPuddleLoop(entity, serverLevel);
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Player player = event.player;
            if (!player.level().isClientSide
                && canHandlePlayerLeftClick(player)
                && player.swinging
                && player.swingTime == 0
                && !shouldIgnoreSwingForAbilityUse(player)) {
                handlePlayerLeftClick(player, null);
            }
            if (!isPuddled(player)) {
                return;
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
                return;
            }
            if (event.getServer().getTickCount() % SWAMP_DOMAIN_CEILING_CHECK_INTERVAL_TICKS != 0) {
                return;
            }

            ServerLevel swampDomain = event.getServer().getLevel(SWAMP_DOMAIN_LEVEL);
            if (swampDomain == null) {
                return;
            }

            List<LivingEntity> entitiesAboveCeiling = swampDomain.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(-30000000.0D, SWAMP_DOMAIN_CEILING_Y, -30000000.0D, 30000000.0D, 30000000.0D, 30000000.0D),
                living -> living != null && living.isAlive() && living.getY() > SWAMP_DOMAIN_CEILING_Y
            );

            for (LivingEntity living : entitiesAboveCeiling) {
                if (living instanceof ServerPlayer player) {
                    teleportOutOfSwampDomain(player, swampDomain);
                    continue;
                }

                Vec3 movement = living.getDeltaMovement();
                double yVelocity = Math.min(movement.y, SWAMP_DOMAIN_CEILING_DOWNWARD_SPEED);
                living.setDeltaMovement(movement.x, yVelocity, movement.z);
                living.hurtMarked = true;
            }
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            Player player = event.getEntity();
            if (!isPuddled(player)) {
                return;
            }

            Item item = event.getItemStack().getItem();
            if (event.getHand() == InteractionHand.MAIN_HAND
                && item != null
                && !(item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem)
                && !(item instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem)) {
                exitPuddle(player, true);
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }
            Player player = event.getEntity();
            if (player != null) {
                handlePlayerLeftClick(player, event.getTarget());
            } else if (isPuddled(event.getEntity())) {
                onPuddleAttack(event.getEntity());
            } else if (isSwampDemonArtItem(event.getEntity().getMainHandItem())) {
                playRegularMeleeCombo(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }
            Player player = event.getEntity();
            if (player != null) {
                handlePlayerLeftClick(player, null);
            } else if (isPuddled(event.getEntity())) {
                onPuddleAttack(event.getEntity());
            } else if (isSwampDemonArtItem(event.getEntity().getMainHandItem())) {
                playRegularMeleeCombo(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onEntitySize(EntityEvent.Size event) {
            if (event.getEntity() instanceof LivingEntity living && (hasDebugDimensionOverride(living) || isPuddled(living))) {
                float targetHeight = hasDebugDimensionOverride(living) ? getDebugDimensionHeight(living) : getTargetPuddleHeight(living);
                float width = event.getOldSize().width;
                event.setNewSize(net.minecraft.world.entity.EntityDimensions.scalable(width, targetHeight), true);
                event.setNewEyeHeight(hasDebugDimensionOverride(living) ? getDebugDimensionEyeHeight(living) : getTargetPuddleEyeHeight(living));
                Log.infoEvery("debug-dims-event-" + living.getId(), 1000L,
                    "Applied size event override for {}: width={}, height={}, eye={}, puddled={}, debug={}",
                    living.getScoreboardName(), width, targetHeight, event.getNewEyeHeight(), isPuddled(living), hasDebugDimensionOverride(living));
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            LivingEntity entity = event.getEntity();
            if (isPuddled(entity) && !entity.level().isClientSide && event.getAmount() > 0.0F && entity.getRandom().nextFloat() < 0.04F) {
                exitPuddle(entity, true);
            }

            // Check if this is the quest-targeted swamp demon at low health
            if (!entity.level().isClientSide && entity instanceof SwampDemonEntity demon) {
                String targetKey = entity.getPersistentData().getString(
                    com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.QUEST_TARGET_ID_TAG);
                if ("swamp_demon_kidnappers_bog_satoko".equals(targetKey)) {
                    float healthPercent = entity.getHealth() / entity.getMaxHealth();
                    if (healthPercent <= 0.20F && !entity.getPersistentData().getBoolean("KnYSwampDemonLowHealthDialoguePlayed")) {
                        entity.getPersistentData().putBoolean("KnYSwampDemonLowHealthDialoguePlayed", true);
                        // Send dialogue to nearby players
                        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                                if (player.distanceToSqr(entity.position()) < 64.0D) {
                                    com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestScenarioActions.sendSwampDemonLowHealthDialogue(player);
                                }
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            LivingEntity entity = event.getEntity();
            if (isPuddled(entity)) {
                exitPuddle(entity, false);
            }
        }

        @SubscribeEvent
        public static void onLivingJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
            LivingEntity entity = event.getEntity();
            if (!isPuddled(entity)) {
                return;
            }

            if (entity instanceof Player) {
                exitPuddle(entity, true);
                return;
            }

            entity.setDeltaMovement(entity.getDeltaMovement().x, Math.min(0.0D, entity.getDeltaMovement().y), entity.getDeltaMovement().z);
            entity.hurtMarked = true;
        }
    }

}
