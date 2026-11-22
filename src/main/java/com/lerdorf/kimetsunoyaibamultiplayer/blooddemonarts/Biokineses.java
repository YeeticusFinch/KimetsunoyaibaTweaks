package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.IBiokinesesArmData;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.MinecraftComesAliveCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodTornadoSpawner;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BloodLanceEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BoneWhipEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.FleshVineEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonEyesSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BrambleSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderLightningSparkSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Muzan's Biokineses blood demon art. */
public final class Biokineses {
    public static final String ART_ID = "biokineses";
    public static final int FORM_BLACK_BLOOD_BRAMBLES = 3900;
    public static final int FORM_MERCILESS_PUNISHMENT = 3904;
    public static final int FORM_ULTIMATE_AUTHORITY = 3905;
    public static final int FORM_BLACK_BLOOD_DESCENDENCE = 3906;
    public static final int FORM_FORCED_TRANSITION = 3907;
    public static final int FORM_SHOCKWAVE_ENERGY_BLAST = 3908;
    public static final int FORM_BLADES_OF_DISASTER = 3909;
    public static final int FORM_BLOOD_KINGS_MASSACRE = 3910;
    public static final int FORM_DEMON_KINGS_WRATH = 3911;
    public static final int FORM_DEMON_KINGS_BLOOD_DANCE = 3912;
    public static final int FORM_ABHORRENT_GALAXY = 3913;

    private static final int CHARGE_TICKS = 18;
    private static final int ARM_TOGGLE_COOLDOWN_TICKS = 20;
    private static final String ARM_TOGGLE_COOLDOWN_UNTIL_TAG = "BiokinesesArmToggleCooldownUntil";
    private static final int BRAMBLE_END_TICK = 60;
    private static final int SEQUENCE_END_TICK = 61;
    private static final int BRAMBLE_COUNT = 5;
    private static final double BRAMBLE_RANGE_STEP = 6.0D;
    private static final double BRAMBLE_MAX_RANGE = 60.0D;
    private static final double BRAMBLE_CONE_RADIUS_RATIO = 1.0D / 3.0D;
    private static final double BRAMBLE_RAYCAST_RANGE = 60.0D;
    private static final double BRAMBLE_START_FORWARD_OFFSET = 0.45D;
    private static final double BRAMBLE_START_LEFT_OFFSET = 0.3D;
    private static final double BRAMBLE_START_DOWN_OFFSET = 0.25D;
    private static final double BRAMBLE_START_OFFSET = 0.1D;
    private static final float BRAMBLE_MIN_WIDTH = 0.05F;
    private static final float BRAMBLE_MAX_WIDTH = 0.1F;
    private static final float BRAMBLE_DAMAGE = 4.0F;
    private static final int BRAMBLE_LIFETIME = BRAMBLE_END_TICK - CHARGE_TICKS + 2;
    private static final int CHARGE_RING_COUNT = 5;
    private static final int CHARGE_RING_PARTICLES = 8;
    private static final double CHARGE_RING_RADIUS = 0.3D;
    private static final double CHARGE_COLUMN_HEIGHT = 0.8D;
    private static final float CLAW_DAMAGE = 14.0F;
    private static final int CLAW_GUARD_TICKS = 10;
    private static final double CLAW_AOE_SIZE = 5.0D;
    private static final int MERCILESS_END_TICK = 21;
    private static final int ULTIMATE_CHARGE_END_TICK = 15;
    private static final int ULTIMATE_SLAM_TICK = 17;
    private static final int ULTIMATE_CANCEL_TICK = 57;
    private static final int ULTIMATE_SEQUENCE_END_TICK = 62;
    private static final int ULTIMATE_CHARGE_VINE_END_TICK = 19;
    private static final int ULTIMATE_CHARGE_VINE_LIFETIME = ULTIMATE_CHARGE_VINE_END_TICK + 1;
    private static final int ULTIMATE_SURGE_COUNT = 5;
    private static final double ULTIMATE_HOVER_HEIGHT = 3.0D;
    private static final double ULTIMATE_CHARGE_START_BELOW = 1.0D;
    private static final double ULTIMATE_CHARGE_END_ABOVE = 2.0D;
    private static final double ULTIMATE_SURGE_SPACING = 3.0D;
    private static final double ULTIMATE_SURGE_HALF_WIDTH = 6.0D;
    private static final double ULTIMATE_SURGE_START_BELOW_GROUND = 1.0D;
    private static final int DESCENDENCE_CHARGE_END_TICK = 20;
    private static final int DESCENDENCE_IMPACT_TICK = 25;
    private static final int DESCENDENCE_CLEANUP_TICK = 56;
    private static final int DESCENDENCE_BRAMBLE_LIFETIME = 30;
    private static final int DESCENDENCE_BRAMBLE_COUNT = 10;
    private static final double DESCENDENCE_HOVER_HEIGHT = 20.0D;
    private static final double DESCENDENCE_BACKWARD_DISTANCE = 10.0D;
    private static final double DESCENDENCE_TARGET_RANGE = 40.0D;
    private static final double DESCENDENCE_FALLBACK_RAYCAST = 80.0D;
    private static final double DESCENDENCE_IMPACT_RADIUS = 6.0D;
    private static final float DESCENDENCE_IMPACT_DAMAGE = 14.0F;
    private static final float DESCENDENCE_BRAMBLE_DAMAGE = 6.0F;
    private static final double DESCENDENCE_KNOCKBACK = 1.35D;
    private static final double DESCENDENCE_BRAMBLE_HEIGHT = 30.0D;
    private static final double DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET = 6.0D;
    private static final double DESCENDENCE_BRAMBLE_ARC = 3.0D;
    private static final float DESCENDENCE_BRAMBLE_WIDTH = 0.12F;
    private static final double DESCENDENCE_BRAMBLE_START_SPREAD = 6.0D;
    private static final int FORCED_TRANSITION_END_TICK = 50;
    private static final int FORCED_TRANSITION_LATCH_TICKS = 20;
    private static final int FORCED_TRANSITION_DAMAGE_INTERVAL = 10;
    private static final float FORCED_TRANSITION_DAMAGE = 30.0F;
    private static final int FORCED_TRANSITION_DEBUFF_TICKS = 200;
    private static final double FORCED_TRANSITION_RANGE = 80.0D;
    private static final double FORCED_TRANSITION_VINE_SPEED = 2.0D;
    private static final double FORCED_TRANSITION_VINE_WIDTH = 0.75D;
    private static final double FORCED_TRANSITION_SLITHER_AMPLITUDE = 0.9D;
    private static final int SHOCKWAVE_CHARGE_END_TICK = 12;
    private static final int SHOCKWAVE_BLAST_END_TICK = 26;
    private static final int SHOCKWAVE_CLEANUP_TICK = 30;
    private static final int SHOCKWAVE_TARGET_EFFECT_TICKS = 100;
    private static final int SHOCKWAVE_TARGET_SPARK_TICKS = 10;
    private static final int SHOCKWAVE_CHARGE_VINE_COUNT = 3;
    private static final int SHOCKWAVE_CHARGE_VINE_ARC_POINTS = 24;
    private static final int SHOCKWAVE_BLAST_SPARK_COUNT = 10;
    private static final int SHOCKWAVE_FLASH_PARTICLE_COUNT = 48;
    private static final double SHOCKWAVE_CHARGE_START_RADIUS = 4.0D;
    private static final double SHOCKWAVE_CHARGE_VINE_ARC = Math.PI * 1.5D;
    private static final double SHOCKWAVE_MAX_RADIUS = 50.0D;
    private static final float SHOCKWAVE_SPARK_WIDTH = 0.8F;
    private static final float SHOCKWAVE_DAMAGE = 24.0F;
    private static final int BLADES_CLEANUP_TICK = 30;
    private static final int BLADES_WHIP_END_TICK = 8;
    private static final double BLADES_WHIP_START_ANGLE = Math.toRadians(45.0D);
    private static final double BLADES_WHIP_SWEEP_ANGLE = Math.toRadians(90.0D);
    private static final double BLADES_WHIP_ARC = 8.0D;
    private static final float BLADES_DAMAGE = 10.0F;
    private static final int MASSACRE_CLEANUP_TICK = 70;
    private static final int MASSACRE_RETRACTION_TICK = 54;
    private static final float MASSACRE_DAMAGE = 12.0F;
    private static final int WRATH_CLEANUP_TICK = 83;
    private static final int WRATH_WHIP_START_TICK = 16;
    private static final int WRATH_WHIP_END_TICK = 66;
    private static final int WRATH_SHOCKWAVE_TICK = 56;
    private static final float WRATH_DAMAGE = 16.0F;
    private static final int BLOOD_DANCE_TARGET_TICK = 15;
    private static final int BLOOD_DANCE_DANCE_END_TICK = 76;
    private static final int BLOOD_DANCE_RETRACTION_END_TICK = 87;
    private static final int BLOOD_DANCE_CLEANUP_TICK = 88;
    private static final float BLOOD_DANCE_DAMAGE = 14.0F;
    private static final int ABHORRENT_CHARGE_END_TICK = 15;
    private static final int ABHORRENT_DANCE_END_TICK = 85;
    private static final int ABHORRENT_RETRACTION_END_TICK = 96;
    private static final int ABHORRENT_CLEANUP_TICK = 97;
    private static final float ABHORRENT_DAMAGE = 12.0F;
    public static final String MODEL_VARIANT_TAG = "BiokinesesModelVariant";
    private static final Vec3 WHITE = new Vec3(1.0D, 1.0D, 1.0D);
    private static final ResourceLocation KNY_LIGHTNING_PARTICLE =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_lightning");
    private static final String LAST_CLAW_TICK_TAG = "BiokinesesLastClawTick";
    private static final String CLAW_ANIMATION_INDEX_TAG = "BiokinesesClawAnimationIndex";
    private static final String[] CLAW_ANIMATIONS = {
        "sword_to_left",
        "sword_to_right",
        "left_sword_to_left",
        "left_sword_to_right",
        "left_sword_overhead"
    };
    private static final Vec3 RED = new Vec3(0.72D, 0.02D, 0.02D);
    private static final Vec3 BLACK = new Vec3(0.01D, 0.005D, 0.005D);
    private static final DustParticleOptions RED_DUST =
        new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.02F), 0.08F);
    private static final DustParticleOptions BLACK_DUST =
        new DustParticleOptions(new Vector3f(0.01F, 0.005F, 0.005F), 0.08F);
    private static final DustParticleOptions CHARGE_RED_DUST =
        new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.02F), 0.1F);
    private static final DustParticleOptions CHARGE_BLACK_DUST =
        new DustParticleOptions(new Vector3f(0.01F, 0.005F, 0.005F), 0.1F);

    private Biokineses() {
    }

    public static void register() {
        if (!BloodDemonArtRegistry.isRegistered(ART_ID)) {
            KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Biokineses", createTechnique());
        }
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Biokineses",
            List.of(new BloodDemonArtForm(
                FORM_BLACK_BLOOD_BRAMBLES,
             "Black Blood, Brambles",
                 "Charge red-and-black blood brambles that slash through a long cone.",
                 3,
                 Biokineses::executeBlackBloodBrambles
             ),
              new BloodDemonArtForm(
                   FORM_MERCILESS_PUNISHMENT,
                   "Merciless Punishment",
                  "Swipe with a red claw storm and launch a blood tornado along the ground.",
                   3,
                   Biokineses::executeMercilessPunishment
               ),
                new BloodDemonArtForm(
                    FORM_ULTIMATE_AUTHORITY,
                    "Ultimate Authority",
                     "Stomp and send five sinusoidal flesh vines surging forward.",
                     4,
                     Biokineses::executeUltimateAuthority
                ),
                new BloodDemonArtForm(
                    FORM_BLACK_BLOOD_DESCENDENCE,
                    "Black Blood, Descendence",
                    "Leap into the air and crash down in a storm of red-and-black brambles.",
                    4,
                    Biokineses::executeBlackBloodDescendence
                ),
                new BloodDemonArtForm(
                    FORM_FORCED_TRANSITION,
                    "Forced Transition",
                    "Latch onto a living target with a flesh vine and force a demonic transformation.",
                    4,
                    Biokineses::executeForcedTransition
                ),
                new BloodDemonArtForm(
                    FORM_SHOCKWAVE_ENERGY_BLAST,
                    "Shockwave Energy Blast",
                    "Release a radial shockwave that stuns, damages, and suffocates nearby enemies.",
                    6,
                    Biokineses::executeShockwaveEnergyBlast
                ),
                new BloodDemonArtForm(
                    FORM_BLADES_OF_DISASTER,
                    "Blades of Disaster",
                    "Cross two bone whips into a devastating slash followed by blood projectiles.",
                    6,
                    Biokineses::executeBladesOfDisaster
                ),
                new BloodDemonArtForm(
                    FORM_BLOOD_KINGS_MASSACRE,
                    "Blood King's Massacre",
                    "Thrash two bone whips through a wide area and drag nearby victims inward.",
                    8,
                    Biokineses::executeBloodKingsMassacre
                ),
                new BloodDemonArtForm(
                    FORM_DEMON_KINGS_WRATH,
                    "Demon King's Wrath",
                    "Unleash four bone whips, explosions, and a final expanding shockwave.",
                    10,
                    Biokineses::executeDemonKingsWrath
                ),
                new BloodDemonArtForm(
                    FORM_DEMON_KINGS_BLOOD_DANCE,
                    "Demon King's Blood Dance",
                    "Assign tentacles and bone whips to nearby enemies before tearing through them in a deadly dance.",
                    12,
                    Biokineses::executeDemonKingsBloodDance
                ),
                new BloodDemonArtForm(
                    FORM_ABHORRENT_GALAXY,
                    "Abhorrent Galaxy",
                    "Spiral flesh vines and tentacles around the user while lances and blood tornadoes tear through the disk.",
                    14,
                    Biokineses::executeAbhorrentGalaxy
                )
             ),
            0x8B1018
        );
    }

    private static void setModelVariant(LivingEntity entity, int variant) {
        ItemStack stack = entity.getMainHandItem();
        if (!isBiokinesesItem(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt(MODEL_VARIANT_TAG, variant);
    }

    private static BoneWhipEntity createBoneWhip(ServerLevel level, LivingEntity owner,
                                                  Vec3 start, Vec3 end, List<Vec3> path,
                                                  int lifetime) {
        BoneWhipEntity whip = ModEntities.BONE_WHIP.get().create(level);
        if (whip == null) {
            return null;
        }
        whip.initializeCharge(start, end, owner, owner.getRandom().nextInt(),
            BoneWhipEntity.DEFAULT_DIAMETER, lifetime);
        whip.updateChargePath(start, end, path);
        level.addFreshEntity(whip);
        return whip;
    }

    private static void updateBoneWhip(BoneWhipEntity whip, Vec3 start, Vec3 end,
                                       Vec3 up, List<Vec3> path) {
        if (whip == null || !whip.isAlive() || whip.getMode() != FleshVineEntity.MODE_CHARGE) {
            return;
        }
        whip.setUp(up);
        whip.updateChargePath(start, end, path);
    }

    private static List<Vec3> buildWhipPath(Vec3 start, Vec3 end, Vec3 arc,
                                            double wobble, int seed) {
        int points = 16;
        List<Vec3> path = new ArrayList<>(points + 1);
        Vec3 tangent = end.subtract(start);
        Vec3 reference = Math.abs(tangent.y) < 0.9D
            ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = tangent.cross(reference);
        if (side.lengthSqr() < 1.0E-8D) {
            side = tangent.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }
        side = side.lengthSqr() < 1.0E-8D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        Vec3 otherSide = tangent.cross(side).normalize();
        for (int index = 0; index <= points; index++) {
            double progress = index / (double) points;
            double envelope = Math.sin(progress * Math.PI);
            double wave = seed * 0.17D + index * 1.31D;
            Vec3 point = start.lerp(end, progress).add(arc.scale(envelope));
            point = point.add(side.scale(Math.sin(wave) * wobble * envelope))
                .add(otherSide.scale(Math.cos(wave * 0.73D) * wobble * envelope));
            path.add(point);
        }
        return path;
    }

    private static Vec3 armAnchor(LivingEntity entity, boolean left) {
        Vec3 center = CombatGravityFrame.world(CombatGravityFrame.position(entity));
        Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(entity).normalize();
        Vec3 side = up.cross(forward);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        return center.add(up.scale(entity.getBbHeight() * 0.62D))
            .add(side.scale(left ? -0.42D : 0.42D)).add(forward.scale(0.25D));
    }

    private static Vec3 horizontalDirection(Vec3 forward, Vec3 up, double angle) {
        Vec3 side = up.cross(forward);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        return forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
    }

    private static void discardBoneWhips(List<BoneWhipEntity> whips) {
        for (BoneWhipEntity whip : whips) {
            if (whip.isAlive()) {
                whip.beginFade();
            }
        }
        whips.clear();
    }

    private static void damageBoneWhipPath(ServerLevel level, LivingEntity owner, List<Vec3> path,
                                            float damage, double radius) {
        if (path.size() < 2) {
            return;
        }
        Vec3 min = path.get(0);
        Vec3 max = min;
        for (Vec3 point : path) {
            min = new Vec3(Math.min(min.x, point.x), Math.min(min.y, point.y), Math.min(min.z, point.z));
            max = new Vec3(Math.max(max.x, point.x), Math.max(max.y, point.y), Math.max(max.z, point.z));
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(min, max).inflate(radius), candidate -> candidate != owner
                && candidate.isAlive() && !candidate.isSpectator())) {
            Vec3 targetPoint = target.getBoundingBox().getCenter();
            boolean hit = false;
            for (int index = 1; index < path.size(); index++) {
                if (distanceToSegmentSqr(targetPoint, path.get(index - 1), path.get(index))
                    <= (radius + target.getBbWidth() * 0.5D) * (radius + target.getBbWidth() * 0.5D)) {
                    hit = true;
                    break;
                }
            }
            if (hit && Damager.hurt(owner, target, damage, true)) {
                Vec3 impact = target.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.BLACKRED_IMPACT.get(), impact.x, impact.y, impact.z,
                    2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static void spawnBloodLanceFan(ServerLevel level, LivingEntity owner, Vec3 center,
                                           Vec3 forward, Vec3 up, int count, double spacing,
                                           double angleSpacing, float damage) {
        Vec3 side = up.cross(forward);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        for (int index = 0; index < count; index++) {
            double centered = index - (count - 1) * 0.5D;
            Vec3 position = center.add(side.scale(centered * spacing));
            Vec3 direction = horizontalDirection(forward, up, centered * angleSpacing);
            BloodLanceEntity lance = BloodLanceEntity.create(level, owner, position,
                direction.scale(1.4D), damage);
            level.addFreshEntity(lance);
        }
    }

    private static void spawnBloodLanceRing(ServerLevel level, LivingEntity owner, Vec3 center,
                                            Vec3 forward, Vec3 up, int count, double radius,
                                            boolean tangential, float damage) {
        for (int index = 0; index < count; index++) {
            double angle = index * Math.PI * 2.0D / count;
            Vec3 radial = horizontalDirection(forward, up, angle);
            Vec3 direction;
            if (tangential) {
                direction = up.cross(radial).normalize();
            } else {
                direction = radial;
            }
            Vec3 position = center.add(radial.scale(radius));
            BloodLanceEntity lance = BloodLanceEntity.create(level, owner, position,
                direction.scale(1.4D), damage);
            level.addFreshEntity(lance);
        }
    }

    private static void applyMassacreDamage(ServerLevel level, LivingEntity owner, Vec3 center,
                                            double radius, float damage) {
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate != owner && candidate.isAlive() && !candidate.isSpectator())) {
            if (target.getRandom().nextBoolean() && Damager.hurt(owner, target, damage, true)) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4, false, true, true));
                Vec3 pull = center.subtract(target.position());
                if (pull.lengthSqr() > 1.0E-8D) {
                    MovementHelper.addVelocity(target, pull.normalize().scale(0.3D));
                }
                Vec3 impact = target.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.BLACKRED_IMPACT.get(), impact.x, impact.y, impact.z,
                    2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static void updateMassacreTethers(ServerLevel level, LivingEntity owner,
                                               Map<UUID, FleshVineEntity> tethers) {
        Set<UUID> activeTargets = new HashSet<>();
        Vec3 center = CombatGravityFrame.world(CombatGravityFrame.position(owner))
            .add(0.0D, owner.getBbHeight() * 0.5D, 0.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(10.0D), candidate -> candidate != owner
                && candidate.isAlive() && !candidate.isSpectator())) {
            activeTargets.add(target.getUUID());
            Vec3 end = target.getBoundingBox().getCenter();
            Vec3 start = center;
            List<Vec3> path = buildWhipPath(start, end, new Vec3(0.0D, 1.0D, 0.0D), 0.3D,
                owner.getId() + target.getId());
            FleshVineEntity vine = tethers.get(target.getUUID());
            if (vine == null || !vine.isAlive()) {
                vine = ModEntities.FLESH_VINE.get().create(level);
                if (vine == null) {
                    continue;
                }
                vine.initializeTransition(start, owner, owner.getRandom().nextInt(),
                    FleshVineEntity.DEFAULT_DIAMETER, 30);
                vine.updateTransition(start, end, path);
                level.addFreshEntity(vine);
                tethers.put(target.getUUID(), vine);
            }
            vine.updateTransition(start, end, path);
        }
        tethers.entrySet().removeIf(entry -> {
            if (activeTargets.contains(entry.getKey())) {
                return false;
            }
            if (entry.getValue().isAlive()) {
                entry.getValue().beginFade();
            }
            return true;
        });
    }

    private static void discardTethers(Map<UUID, FleshVineEntity> tethers) {
        for (FleshVineEntity vine : tethers.values()) {
            if (vine.isAlive()) {
                vine.beginFade();
            }
        }
        tethers.clear();
    }

    private static void executeBladesOfDisaster(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CombatGravityFrame.run(entity, () -> {
            List<BoneWhipEntity> whips = new ArrayList<>();
            setModelVariant(entity, 2);
            GuardStateHelper.setGuardState(entity, BLADES_DAMAGE, formId, true);
            AnimationHelper.playAnimation(entity, "beast2", BLADES_CLEANUP_TICK);
            Vec3 center = shockwaveCenter(entity);
            Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            Vec3 forward = GravityDirectionHelper.getLookDirection(entity).normalize();
            for (boolean left : new boolean[] {true, false}) {
                Vec3 start = armAnchor(entity, left);
                double rotationSign = left ? -1.0D : 1.0D;
                double angle = -rotationSign * BLADES_WHIP_START_ANGLE;
                Vec3 direction = horizontalDirection(forward, up, angle);
                Vec3 tangent = horizontalDirection(forward, up, angle + rotationSign * Math.PI * 0.5D);
                BoneWhipEntity whip = createBoneWhip(serverLevel, entity, start,
                    start.add(direction.scale(30.0D)),
                    buildWhipPath(start, start.add(direction.scale(30.0D)), tangent.scale(BLADES_WHIP_ARC), 0.25D,
                        entity.getRandom().nextInt()), BLADES_CLEANUP_TICK + 2);
                if (whip != null) {
                    whips.add(whip);
                }
            }

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardBoneWhips(whips);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                        return;
                    }
                    Vec3 currentCenter = shockwaveCenter(entity);
                    Vec3 currentUp = GravityDirectionHelper.localToWorld(entity,
                        new Vec3(0.0D, 1.0D, 0.0D)).normalize();
                    Vec3 currentForward = GravityDirectionHelper.getLookDirection(entity).normalize();
                    if (tick <= BLADES_WHIP_END_TICK) {
                        for (int index = 0; index < whips.size(); index++) {
                            boolean left = index == 0;
                            Vec3 start = armAnchor(entity, left);
                            double rotationSign = left ? -1.0D : 1.0D;
                            double angle = -rotationSign * BLADES_WHIP_START_ANGLE
                                + rotationSign * BLADES_WHIP_SWEEP_ANGLE * tick / BLADES_WHIP_END_TICK;
                            Vec3 direction = horizontalDirection(currentForward, currentUp, angle);
                            Vec3 tangent = horizontalDirection(currentForward, currentUp,
                                angle + rotationSign * Math.PI * 0.5D);
                            Vec3 end = currentCenter.add(direction.scale(30.0D));
                            List<Vec3> path = buildWhipPath(start, end,
                                tangent.scale(BLADES_WHIP_ARC), 0.3D, entity.getId() + index);
                            updateBoneWhip(whips.get(index), start, end, currentUp, path);
                            damageBoneWhipPath(serverLevel, entity, path, BLADES_DAMAGE, 0.75D);
                            for (int pathIndex = 1; pathIndex < path.size(); pathIndex += 2) {
                                Vec3 particle = path.get(pathIndex);
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                    particle.x, particle.y, particle.z, 1,
                                    0.0D, 0.0D, 0.0D, 0.0D);
                                serverLevel.sendParticles(ParticleTypes.CRIT,
                                    particle.x, particle.y, particle.z, 1,
                                    0.08D, 0.08D, 0.08D, 0.12D);
                            }
                            Vec3 slash = path.get(path.size() - 1);
                            serverLevel.sendParticles(ModParticles.BLACKRED_SLASH.get(), slash.x, slash.y, slash.z,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                    }
                    if (tick == 15) {
                        spawnBloodLanceFan(serverLevel, entity, currentCenter, currentForward, currentUp,
                            10, 0.5D, Math.toRadians(2.0D), BLADES_DAMAGE);
                        for (int index = -2; index <= 3; index++) {
                            Vec3 direction = horizontalDirection(currentForward, currentUp, Math.toRadians(index * 7.0D));
                            BloodTornadoSpawner.spawn(serverLevel, currentCenter, direction, entity, 40);
                        }
                    }
                    if (tick == BLADES_CLEANUP_TICK) {
                        discardBoneWhips(whips);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, BLADES_CLEANUP_TICK + 1);
        });
    }

    private static void executeDemonKingsBloodDance(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CombatGravityFrame.run(entity, () -> {
            List<LivingEntity> targets = selectBloodDanceTargets(serverLevel, entity);
            List<BoneWhipEntity> whips = new ArrayList<>();
            List<LivingEntity> whipTargets = new ArrayList<>();
            setModelVariant(entity, 2);
            BiokinesesArmController.claimAllForAbility(entity);
            GuardStateHelper.setGuardState(entity, BLOOD_DANCE_DAMAGE, formId, true);
            AnimationHelper.playAnimation(entity, "kneel", BLOOD_DANCE_CLEANUP_TICK);
            setBloodDanceArmTargets(entity, targets, 0);

            for (int index = IBiokinesesArmData.ARM_COUNT; index < targets.size(); index++) {
                LivingEntity target = targets.get(index);
                Vec3 start = shockwaveCenter(entity);
                Vec3 end = target.getBoundingBox().getCenter();
                BoneWhipEntity whip = createBoneWhip(serverLevel, entity, start, end,
                    buildWhipPath(start, end, GravityDirectionHelper.localToWorld(entity,
                        new Vec3(0.0D, 1.5D, 0.0D)), 0.35D, entity.getId() + index),
                    BLOOD_DANCE_CLEANUP_TICK + 2);
                if (whip != null) {
                    whips.add(whip);
                    whipTargets.add(target);
                }
            }

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardBoneWhips(whips);
                        clearAbilityTargets(entity);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= BLOOD_DANCE_TARGET_TICK) {
                        limitMovement(entity, 0.0D);
                    }
                    setBloodDanceArmTargets(entity, targets, tick);

                    for (int index = 0; index < whipTargets.size(); index++) {
                        LivingEntity target = whipTargets.get(index);
                        if (target == null || !target.isAlive()) {
                            continue;
                        }
                        Vec3 start = shockwaveCenter(entity);
                        Vec3 targetPoint = tick <= BLOOD_DANCE_TARGET_TICK
                            ? start.lerp(target.getBoundingBox().getCenter(),
                                Math.min(1.0D, tick / (double) BLOOD_DANCE_TARGET_TICK))
                            : bloodDanceTargetPoint(entity, index + IBiokinesesArmData.ARM_COUNT, tick);
                        List<Vec3> path = buildWhipPath(start, targetPoint,
                            GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.5D, 0.0D)),
                            0.45D, entity.getId() + index * 13 + tick);
                        updateBoneWhip(whips.get(index), start, targetPoint,
                            GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)), path);
                        if (tick >= BLOOD_DANCE_TARGET_TICK && tick <= BLOOD_DANCE_DANCE_END_TICK) {
                            moveDanceTarget(target, targetPoint);
                        }
                    }

                    if (tick >= BLOOD_DANCE_TARGET_TICK && tick <= BLOOD_DANCE_DANCE_END_TICK) {
                        for (int index = 0; index < Math.min(IBiokinesesArmData.ARM_COUNT, targets.size()); index++) {
                            LivingEntity target = targets.get(index);
                            if (target != null && target.isAlive()) {
                                moveDanceTarget(target, bloodDanceTargetPoint(entity, index, tick));
                            }
                        }
                        if (tick % 10 == 0) {
                            damageBloodDanceTargets(serverLevel, entity, targets);
                        }
                    } else if (tick == BLOOD_DANCE_TARGET_TICK + 1) {
                        attachAbilityTargets(entity, targets);
                    } else if (tick > BLOOD_DANCE_DANCE_END_TICK
                        && tick <= BLOOD_DANCE_RETRACTION_END_TICK) {
                        clearAbilityTargets(entity);
                    }

                    if (tick == BLOOD_DANCE_CLEANUP_TICK) {
                        discardBoneWhips(whips);
                        clearAbilityTargets(entity);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, BLOOD_DANCE_CLEANUP_TICK + 1);
        });
    }

    private static void executeAbhorrentGalaxy(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CombatGravityFrame.run(entity, () -> {
            List<FleshVineEntity> vines = new ArrayList<>();
            List<BloodTornadoEntity> tornados = new ArrayList<>();
            setModelVariant(entity, 1);
            BiokinesesArmController.claimAllForAbility(entity);
            GuardStateHelper.setGuardState(entity, ABHORRENT_DAMAGE, formId, true);
            AnimationHelper.playAnimation(entity, "right_arm_chargeup", ABHORRENT_CHARGE_END_TICK);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardVines(vines);
                        discardBloodTornados(tornados);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        cleanupForm(entity);
                        return;
                    }

                    Vec3 center = shockwaveCenter(entity);
                    Vec3 up = GravityDirectionHelper.localToWorld(entity,
                        new Vec3(0.0D, 1.0D, 0.0D)).normalize();
                    Vec3 forward = GravityDirectionHelper.getLookDirection(entity).normalize();
                    if (tick <= ABHORRENT_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        renderAbhorrentAuras(serverLevel, center, up, tick);
                        setAbhorrentArmTargets(entity, tick, 3.0D);
                    }
                    if (tick == ABHORRENT_CHARGE_END_TICK) {
                        spawnAbhorrentVines(serverLevel, entity, vines);
                        spawnAbhorrentTornados(serverLevel, entity, tornados, center, forward, up);
                        AnimationHelper.playAnimation(entity, "summon", ABHORRENT_DANCE_END_TICK - tick + 1);
                    }
                    if (tick >= ABHORRENT_CHARGE_END_TICK && tick <= ABHORRENT_DANCE_END_TICK) {
                        setAbhorrentArmTargets(entity, tick, 25.0D);
                        updateAbhorrentVines(entity, vines, tick, center, forward, up);
                        updateAbhorrentTornados(entity, tornados, center, forward, up, tick);
                        pullAbhorrentTargets(serverLevel, entity, center, up);
                        if (tick % 2 == 0) {
                            spawnAbhorrentLances(serverLevel, entity, center, forward, up, tick);
                        }
                        if (tick % 10 == 0) {
                            damageAbhorrentDisk(serverLevel, entity, center, up);
                            Vec3 explosion = randomDiskPoint(center, up, entity.getRandom());
                            serverLevel.explode(entity, explosion.x, explosion.y, explosion.z,
                                1.0F, Level.ExplosionInteraction.MOB);
                        }
                    } else if (tick > ABHORRENT_DANCE_END_TICK
                        && tick <= ABHORRENT_RETRACTION_END_TICK) {
                        setAbhorrentArmTargets(entity, tick, 1.0D);
                        updateAbhorrentVines(entity, vines, tick, center, forward, up);
                    }
                    if (tick == ABHORRENT_CLEANUP_TICK) {
                        discardVines(vines);
                        discardBloodTornados(tornados);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, ABHORRENT_CLEANUP_TICK + 1);
        });
    }

    private static List<LivingEntity> selectBloodDanceTargets(ServerLevel level, LivingEntity owner) {
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
            owner.getBoundingBox().inflate(25.0D), candidate -> candidate != owner
                && candidate.isAlive() && !candidate.isSpectator()
                && EnhancedLoveForms.isTargetable(owner, candidate)));
        Collections.shuffle(targets, new java.util.Random(owner.getRandom().nextLong()));
        return targets.size() > 15 ? new ArrayList<>(targets.subList(0, 15)) : targets;
    }

    private static void setBloodDanceArmTargets(LivingEntity owner, List<LivingEntity> targets, int tick) {
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (arm >= targets.size() || !targets.get(arm).isAlive()) {
                continue;
            }
            Vec3 target = tick <= BLOOD_DANCE_TARGET_TICK
                ? targets.get(arm).getBoundingBox().getCenter()
                : tick <= BLOOD_DANCE_DANCE_END_TICK
                    ? bloodDanceTargetPoint(owner, arm, tick) : shockwaveCenter(owner);
            BiokinesesArmController.setAbilityTarget(owner, arm, target);
        }
    }

    private static Vec3 bloodDanceTargetPoint(LivingEntity owner, int index, int tick) {
        Vec3 center = shockwaveCenter(owner);
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner).normalize();
        double angle = tick * 0.34D + index * Math.PI * 0.41D;
        double radius = 8.0D + (index % 8) * 1.65D;
        Vec3 radial = horizontalDirection(forward, up, angle);
        return center.add(radial.scale(radius)).add(up.scale(Math.sin(angle * 1.7D) * 4.0D));
    }

    private static void attachAbilityTargets(LivingEntity owner, List<LivingEntity> targets) {
        IBiokinesesArmData data = BiokinesesArmController.data(owner);
        if (data == null) {
            return;
        }
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            if (arm < targets.size() && targets.get(arm).isAlive()) {
                data.setHeldEntityId(arm, targets.get(arm).getId());
                data.setTargetEntityId(arm, targets.get(arm).getId());
            }
        }
    }

    private static void clearAbilityTargets(LivingEntity owner) {
        IBiokinesesArmData data = BiokinesesArmController.data(owner);
        if (data == null) {
            return;
        }
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            data.setHeldEntityId(arm, -1);
            data.setTargetEntityId(arm, -1);
        }
    }

    private static void moveDanceTarget(LivingEntity target, Vec3 center) {
        target.teleportTo(center.x, center.y - target.getBbHeight() * 0.5D, center.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
    }

    private static void damageBloodDanceTargets(ServerLevel level, LivingEntity owner,
                                                List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (!target.isAlive() || !target.getRandom().nextBoolean()) {
                continue;
            }
            if (Damager.hurt(owner, target, BLOOD_DANCE_DAMAGE, true)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9, false, false, false));
                BleedingHandler.applyOrRefreshBleeding(target, 300, 4);
                Vec3 impact = target.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.BLACKRED_IMPACT.get(), impact.x, impact.y, impact.z,
                    2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static void setAbhorrentArmTargets(LivingEntity owner, int tick, double radius) {
        Vec3 center = shockwaveCenter(owner);
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner).normalize();
        for (int arm = 0; arm < IBiokinesesArmData.ARM_COUNT; arm++) {
            double angle = tick * 0.16D + arm * Math.PI * 2.0D / IBiokinesesArmData.ARM_COUNT;
            Vec3 radial = horizontalDirection(forward, up, angle);
            Vec3 target = center.add(radial.scale(radius)).add(up.scale(Math.sin(angle * 2.0D) * 2.5D));
            BiokinesesArmController.setAbilityTarget(owner, arm, target);
        }
    }

    private static void renderAbhorrentAuras(ServerLevel level, Vec3 center, Vec3 up, int tick) {
        int count = 16;
        double darkRadius = 0.5D + tick * 0.12D;
        double redRadius = 3.5D - tick * 0.12D;
        Vec3 side = up.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = up.cross(side).normalize();
        level.sendParticles(CHARGE_BLACK_DUST, center.x, center.y, center.z,
            1, darkRadius, 0.05D, darkRadius, 0.0D);
        for (int index = 0; index < count; index++) {
            double angle = tick * 0.3D + index * Math.PI * 2.0D / count;
            Vec3 point = center.add(side.scale(Math.cos(angle) * redRadius))
                .add(otherSide.scale(Math.sin(angle) * redRadius));
            level.sendParticles(CHARGE_RED_DUST, point.x, point.y, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnAbhorrentVines(ServerLevel level, LivingEntity owner,
                                            List<FleshVineEntity> vines) {
        Vec3 center = shockwaveCenter(owner);
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner).normalize();
        for (int index = 0; index < 4; index++) {
            Vec3 start = galaxyArmStart(owner, index, up, forward);
            Vec3 end = center.add(horizontalDirection(forward, up,
                index * Math.PI * 0.5D).scale(25.0D));
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                continue;
            }
            vine.initializeTransition(start, owner, owner.getRandom().nextInt(),
                FleshVineEntity.DEFAULT_DIAMETER, ABHORRENT_CLEANUP_TICK + 2);
            vine.updateTransition(start, end, buildWhipPath(start, end, up.scale(4.0D), 0.5D,
                owner.getId() + index));
            level.addFreshEntity(vine);
            vines.add(vine);
        }
    }

    private static Vec3 galaxyArmStart(LivingEntity owner, int index, Vec3 up, Vec3 forward) {
        Vec3 side = up.cross(forward).normalize();
        boolean left = index < 2;
        Vec3 start = armAnchor(owner, left);
        return start.add(side.scale((index % 2 == 0 ? -1.0D : 1.0D) * 0.35D))
            .add(up.scale((index % 2 == 0 ? 0.25D : -0.25D)));
    }

    private static void updateAbhorrentVines(LivingEntity owner, List<FleshVineEntity> vines, int tick,
                                             Vec3 center, Vec3 forward, Vec3 up) {
        double retract = tick > ABHORRENT_DANCE_END_TICK
            ? (tick - ABHORRENT_DANCE_END_TICK) / 11.0D : 0.0D;
        double radius = 25.0D + (1.0D - 25.0D) * Math.min(1.0D, retract);
        for (int index = 0; index < vines.size(); index++) {
            FleshVineEntity vine = vines.get(index);
            if (!vine.isAlive()) {
                continue;
            }
            Vec3 start = galaxyArmStart(owner, index, up, forward);
            double angle = tick * 0.15D + index * Math.PI * 0.5D;
            Vec3 direction = horizontalDirection(forward, up, angle);
            Vec3 end = center.add(direction.scale(radius)).add(up.scale(Math.sin(angle * 1.5D) * 5.0D));
            Vec3 tangent = horizontalDirection(forward, up, angle + Math.PI * 0.5D);
            List<Vec3> path = buildWhipPath(start, end, tangent.scale(9.0D), 0.65D,
                owner.getId() + index * 17 + tick);
            vine.updateTransition(start, end, path);
            if (tick % 3 == 0) {
                for (int pathIndex = 2; pathIndex < path.size(); pathIndex += 4) {
                    Vec3 mouth = path.get(pathIndex);
                    if (owner.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z,
                            1, 0.0D, 0.02D, 0.0D, 0.0D);
                    }
                }
            }
        }
    }

    private static void spawnAbhorrentTornados(ServerLevel level, LivingEntity owner,
                                               List<BloodTornadoEntity> tornados, Vec3 center,
                                               Vec3 forward, Vec3 up) {
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI * 0.5D;
            Vec3 radial = horizontalDirection(forward, up, angle);
            BloodTornadoEntity tornado = BloodTornadoSpawner.spawnNoGravity(level, center.add(radial.scale(18.0D)),
                up.cross(radial).normalize(), owner, ABHORRENT_CLEANUP_TICK - ABHORRENT_CHARGE_END_TICK + 2);
            if (tornado != null) {
                tornados.add(tornado);
            }
        }
    }

    private static void updateAbhorrentTornados(LivingEntity owner, List<BloodTornadoEntity> tornados,
                                                Vec3 center, Vec3 forward, Vec3 up, int tick) {
        for (int index = 0; index < tornados.size(); index++) {
            BloodTornadoEntity tornado = tornados.get(index);
            if (!tornado.isAlive()) {
                continue;
            }
            Vec3 radial = horizontalDirection(forward, up,
                index * Math.PI * 0.5D + tick * 0.12D);
            tornado.setPos(center.add(radial.scale(18.0D)));
            tornado.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void pullAbhorrentTargets(ServerLevel level, LivingEntity owner, Vec3 center, Vec3 up) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(35.0D), candidate -> candidate != owner
                && candidate.isAlive() && !candidate.isSpectator())) {
            Vec3 delta = center.subtract(target.position());
            double vertical = delta.dot(up);
            Vec3 horizontal = delta.subtract(up.scale(vertical));
            double distance = horizontal.length();
            if (distance > 5.0D && (Math.abs(vertical) > 3.0D || distance > 20.0D)) {
                MovementHelper.addVelocity(target, delta.normalize().scale(0.12D));
            }
        }
    }

    private static void spawnAbhorrentLances(ServerLevel level, LivingEntity owner, Vec3 center,
                                             Vec3 forward, Vec3 up, int tick) {
        double offset = Math.toRadians(tick * 3.0D);
        for (int index = 0; index < 4; index++) {
            Vec3 direction = horizontalDirection(forward, up, offset + index * Math.PI * 0.5D);
            BloodLanceEntity lance = BloodLanceEntity.create(level, owner, center,
                direction.scale(1.4D), ABHORRENT_DAMAGE);
            level.addFreshEntity(lance);
        }
    }

    private static void damageAbhorrentDisk(ServerLevel level, LivingEntity owner, Vec3 center, Vec3 up) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(25.0D), candidate -> candidate != owner
                && candidate.isAlive() && !candidate.isSpectator())) {
            Vec3 delta = target.position().subtract(center);
            double vertical = delta.dot(up);
            double horizontal = delta.subtract(up.scale(vertical)).length();
            if (horizontal <= 25.0D && Math.abs(vertical) <= 4.0D
                && target.getRandom().nextBoolean() && Damager.hurt(owner, target, ABHORRENT_DAMAGE, true)) {
                Vec3 impact = target.getBoundingBox().getCenter();
                level.sendParticles(ModParticles.BLACKRED_IMPACT.get(), impact.x, impact.y, impact.z,
                    2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static Vec3 randomDiskPoint(Vec3 center, Vec3 up, RandomSource random) {
        Vec3 side = up.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = up.cross(side).normalize();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = Math.sqrt(random.nextDouble()) * 25.0D;
        return center.add(side.scale(Math.cos(angle) * radius))
            .add(otherSide.scale(Math.sin(angle) * radius));
    }

    private static void discardBloodTornados(List<BloodTornadoEntity> tornados) {
        for (BloodTornadoEntity tornado : tornados) {
            if (tornado.isAlive()) {
                tornado.discard();
            }
        }
        tornados.clear();
    }

    private static void executeBloodKingsMassacre(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CombatGravityFrame.run(entity, () -> {
            List<BoneWhipEntity> whips = new ArrayList<>();
            Map<UUID, FleshVineEntity> tethers = new HashMap<>();
            setModelVariant(entity, 2);
            BiokinesesArmController.claimAllForAbility(entity);
            GuardStateHelper.setGuardState(entity, MASSACRE_DAMAGE, formId, true);
            AnimationHelper.playAnimation(entity, "summon", MASSACRE_CLEANUP_TICK);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardBoneWhips(whips);
                        discardTethers(tethers);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                        return;
                    }
                    Vec3 center = shockwaveCenter(entity);
                    Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
                    Vec3 forward = GravityDirectionHelper.getLookDirection(entity).normalize();
                    if (tick == 11) {
                        Vec3 start = armAnchor(entity, true);
                        Vec3 end = center.add(horizontalDirection(forward, up, -Math.PI * 0.5D).scale(5.0D));
                        BoneWhipEntity whip = createBoneWhip(serverLevel, entity, start, end,
                            buildWhipPath(start, end, up.scale(1.4D), 0.35D, entity.getId()), MASSACRE_CLEANUP_TICK);
                        if (whip != null) {
                            whips.add(whip);
                        }
                        AnimationHelper.playAnimation(entity, "left_sword_to_left", 12);
                        spawnBloodLanceRing(serverLevel, entity, center, forward, up, 12, 2.0D, false, MASSACRE_DAMAGE);
                    } else if (tick == 17) {
                        Vec3 start = armAnchor(entity, false);
                        Vec3 end = center.add(up.scale(8.0D)).add(forward.scale(2.0D));
                        BoneWhipEntity whip = createBoneWhip(serverLevel, entity, start, end,
                            buildWhipPath(start, end, forward.scale(2.0D), 0.45D, entity.getId() + 1), MASSACRE_CLEANUP_TICK);
                        if (whip != null) {
                            whips.add(whip);
                        }
                        AnimationHelper.playAnimation(entity, "sword_overhead", 12);
                        spawnBloodLanceRing(serverLevel, entity, center, forward, up, 12, 8.0D, true, MASSACRE_DAMAGE);
                    } else if (tick == 23) {
                        AnimationHelper.playAnimation(entity, "heel_drop", 30);
                        spawnBloodLanceRing(serverLevel, entity, center, forward, up, 12, 18.0D, true, MASSACRE_DAMAGE);
                    }

                    if (tick >= 11 && tick <= 53 && whips.size() == 2) {
                        double progress = (tick - 11) / 42.0D;
                        Vec3 leftStart = armAnchor(entity, true);
                        Vec3 leftEnd = center.add(horizontalDirection(forward, up,
                            -Math.PI * 0.5D + progress * Math.PI * 4.0D).scale(5.0D));
                        updateBoneWhip(whips.get(0), leftStart, leftEnd, up,
                            buildWhipPath(leftStart, leftEnd, up.scale(1.5D), 0.4D, tick));
                        Vec3 rightStart = armAnchor(entity, false);
                        Vec3 rightEnd = center.add(up.scale(Math.sin(progress * Math.PI * 6.0D) * 8.0D))
                            .add(forward.scale(Math.cos(progress * Math.PI * 6.0D) * 8.0D));
                        updateBoneWhip(whips.get(1), rightStart, rightEnd, up,
                            buildWhipPath(rightStart, rightEnd, forward.scale(2.0D), 0.5D, tick + 7));
                    }
                    if (tick >= 23 && tick <= 53) {
                        applyMassacreDamage(serverLevel, entity, center, 25.0D, MASSACRE_DAMAGE);
                        updateMassacreTethers(serverLevel, entity, tethers);
                    }
                    if (tick == MASSACRE_RETRACTION_TICK) {
                        AnimationHelper.playAnimation(entity, "kimetsunoyaiba:cancel", 1);
                    }
                    if (tick == MASSACRE_CLEANUP_TICK) {
                        discardBoneWhips(whips);
                        discardTethers(tethers);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, MASSACRE_CLEANUP_TICK + 1);
        });
    }

    private static void executeDemonKingsWrath(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CombatGravityFrame.run(entity, () -> {
            List<BoneWhipEntity> whips = new ArrayList<>();
            List<UUID> radialSparks = new ArrayList<>();
            Map<UUID, UUID> targetSparks = new HashMap<>();
            Set<UUID> shockedTargets = new HashSet<>();
            setModelVariant(entity, 2);
            BiokinesesArmController.claimAllForAbility(entity);
            GuardStateHelper.setGuardState(entity, WRATH_DAMAGE, formId, true);
            AnimationHelper.playAnimation(entity, "summon", WRATH_CLEANUP_TICK);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardBoneWhips(whips);
                        clearShockwaveSparks(serverLevel, radialSparks, targetSparks, entity);
                        entity.setNoGravity(false);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                        return;
                    }
                    Vec3 center = shockwaveCenter(entity);
                    Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
                    if (tick >= WRATH_WHIP_START_TICK && tick <= WRATH_WHIP_END_TICK) {
                        entity.setNoGravity(true);
                        limitMovement(entity, 0.25D);
                        while (whips.size() < 4) {
                            boolean left = whips.size() < 2;
                            Vec3 start = armAnchor(entity, left);
                            Vec3 end = center.add(randomUnitVector(entity.getRandom()).scale(25.0D));
                            BoneWhipEntity whip = createBoneWhip(serverLevel, entity, start, end,
                                buildWhipPath(start, end, randomUnitVector(entity.getRandom()).scale(5.0D), 0.8D,
                                    entity.getRandom().nextInt()), WRATH_CLEANUP_TICK);
                            if (whip == null) {
                                break;
                            }
                            whips.add(whip);
                        }
                        for (int index = 0; index < whips.size(); index++) {
                            Vec3 start = armAnchor(entity, index >= 2);
                            Vec3 end = center.add(randomUnitVector(entity.getRandom()).scale(25.0D));
                            List<Vec3> path = buildWhipPath(start, end,
                                randomUnitVector(entity.getRandom()).scale(5.0D), 0.8D, tick + index * 19);
                            updateBoneWhip(whips.get(index), start, end, up, path);
                            serverLevel.sendParticles(ModParticles.BLACKRED_SLASH.get(), end.x, end.y, end.z,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                        if (tick % 5 == 0) {
                            applyMassacreDamage(serverLevel, entity, center, 25.0D, WRATH_DAMAGE);
                        }
                        if (tick % 2 == 0) {
                            Vec3 explosion = randomSpherePoint(center, 25.0D, entity);
                            serverLevel.explode(entity, explosion.x, explosion.y, explosion.z, 1.0F,
                                Level.ExplosionInteraction.MOB);
                        }
                    }
                    if (tick == WRATH_SHOCKWAVE_TICK) {
                        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                        for (int index = 0; index < SHOCKWAVE_BLAST_SPARK_COUNT; index++) {
                            UUID sparkId = UUID.randomUUID();
                            radialSparks.add(sparkId);
                            sendShockwaveSpark(serverLevel, sparkId, center,
                                randomSpherePoint(center, 1.0D, entity), true, 10);
                        }
                    }
                    if (tick >= WRATH_SHOCKWAVE_TICK && tick <= WRATH_SHOCKWAVE_TICK + 10) {
                        double radius = 1.0D + 39.0D * (tick - WRATH_SHOCKWAVE_TICK) / 10.0D;
                        spawnShockwaveFlashSphere(serverLevel, center, radius, entity);
                        for (UUID sparkId : radialSparks) {
                            sendShockwaveSpark(serverLevel, sparkId, center,
                                randomSpherePoint(center, radius, entity), false, 10);
                        }
                        applyShockwaveTargets(serverLevel, entity, center, radius, shockedTargets, targetSparks);
                    }
                    if (tick >= 67 && tick <= 82) {
                        entity.setNoGravity(false);
                        limitMovement(entity, 0.1D);
                        double radius = 25.0D - 24.0D * (tick - 67) / 15.0D;
                        for (BoneWhipEntity whip : whips) {
                            Vec3 start = armAnchor(entity, whips.indexOf(whip) >= 2);
                            Vec3 end = center.add(randomUnitVector(entity.getRandom()).scale(radius));
                            updateBoneWhip(whip, start, end, up,
                                buildWhipPath(start, end, randomUnitVector(entity.getRandom()).scale(2.0D), 0.35D, tick));
                        }
                    }
                    if (tick == WRATH_CLEANUP_TICK) {
                        discardBoneWhips(whips);
                        clearShockwaveSparks(serverLevel, radialSparks, targetSparks, entity);
                        entity.setNoGravity(false);
                        BiokinesesArmController.releaseAbilityClaim(entity);
                        setModelVariant(entity, 0);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, WRATH_CLEANUP_TICK + 1);
        });
    }

    public static void toggleClimbing(Player player) {
        if (player == null || player.level().isClientSide() || !isBiokinesesItem(player.getMainHandItem())
            || KnYEffects.hasSuffocation(player)
            || (!player.getAbilities().instabuild && !Damager.isDemon(player))) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(ARM_TOGGLE_COOLDOWN_UNTIL_TAG) > now) {
            return;
        }

        var data = BiokinesesArmController.data(player);
        if (data == null) {
            return;
        }
        if (data.isClimbing()) {
            BiokinesesArmController.retract(player);
        } else {
            BiokinesesArmController.activateForClimbing(player);
        }
        player.getPersistentData().putLong(ARM_TOGGLE_COOLDOWN_UNTIL_TAG, now + ARM_TOGGLE_COOLDOWN_TICKS);
    }

    private static void executeMercilessPunishment(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            setModelVariant(entity, 0);
            final Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
            final Vec3 position = CombatGravityFrame.world(CombatGravityFrame.position(entity));
            final BloodTornadoEntity tornado = BloodTornadoSpawner.spawn(
                serverLevel, position, direction, entity, MERCILESS_END_TICK);
            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        if (tornado != null) {
                            tornado.discard();
                        }
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= 20) {
                        limitMovement(entity, 0.5D);
                    }
                    if (tick == 0) {
                        performClawSlash(entity, null, "left_arm_overhead", formId);
                    } else if (tick == MERCILESS_END_TICK) {
                        if (tornado != null) {
                            tornado.discard();
                        }
                        cleanupForm(entity);
                    }

                    tick++;
                }
            }, 1, MERCILESS_END_TICK + 1);
        });
    }

    private static void executeUltimateAuthority(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            setModelVariant(entity, 1);
            Vec3 startPosition = entity.position();
            List<FleshVineEntity> chargeVines = new ArrayList<>();
            spawnChargeVines(serverLevel, entity, chargeVines, startPosition, 0);
            Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            Vec3 hoverTarget = startPosition.add(up.scale(ULTIMATE_HOVER_HEIGHT));
            GuardStateHelper.setGuardState(entity, CLAW_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                private boolean slamming;
                private boolean impacted;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardVines(chargeVines);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= ULTIMATE_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick <= 5) {
                            double rise = ULTIMATE_HOVER_HEIGHT * tick / 5.0D;
                            entity.teleportTo(startPosition.add(up.scale(rise)).x,
                                startPosition.add(up.scale(rise)).y,
                                startPosition.add(up.scale(rise)).z);
                        } else {
                            entity.teleportTo(hoverTarget.x, hoverTarget.y, hoverTarget.z);
                        }
                        updateChargeVines(entity, chargeVines, tick);
                    } else if (tick <= ULTIMATE_CHARGE_VINE_END_TICK) {
                        // Keep the vine bases attached to the user while the slam begins.
                        updateChargeVines(entity, chargeVines, tick);
                    }
                    if (tick == 0) {
                        AnimationHelper.playAnimation(entity, "right_arm_chargeup", ULTIMATE_SLAM_TICK);
                    } else if (tick == ULTIMATE_SLAM_TICK) {
                        slamming = true;
                        AnimationHelper.playAnimation(entity, "heel_drop", ULTIMATE_CANCEL_TICK - tick);
                        MovementHelper.setVelocity(entity,
                            GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, -1.1D, 0.0D)));
                    } else if (slamming && !impacted && findGroundHit(serverLevel, entity) != null) {
                        impacted = true;
                        discardVines(chargeVines);
                        spawnSurgeVines(serverLevel, entity, findGroundHit(serverLevel, entity));
                        MovementHelper.setVelocity(entity, Vec3.ZERO);
                    } else if (slamming && !impacted && tick >= ULTIMATE_CANCEL_TICK) {
                        discardVines(chargeVines);
                        cleanupForm(entity);
                    } else if (impacted && tick >= ULTIMATE_SEQUENCE_END_TICK) {
                        cleanupForm(entity);
                    }

                    if (tick == ULTIMATE_CHARGE_VINE_END_TICK + 1 && !impacted) {
                        discardVines(chargeVines);
                    }

                    tick++;
                }
            }, 1, ULTIMATE_SEQUENCE_END_TICK + 1);
        });
    }

    private static void executeBlackBloodDescendence(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            setModelVariant(entity, 0);
            Vec3 startPosition = entity.position();
            Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            Vec3 forward = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
            Vec3 hoverTarget = startPosition.add(up.scale(DESCENDENCE_HOVER_HEIGHT))
                .subtract(forward.scale(DESCENDENCE_BACKWARD_DISTANCE));
            GuardStateHelper.setGuardState(entity, DESCENDENCE_IMPACT_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                private Vec3 targetPosition;
                private final List<DescendenceBramble> brambles = new ArrayList<>();

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        clearDescendenceBrambles(serverLevel, entity, brambles);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= DESCENDENCE_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick <= 10) {
                            double progress = tick / 10.0D;
                            Vec3 position = startPosition.lerp(hoverTarget, progress);
                            entity.teleportTo(position.x, position.y, position.z);
                        } else {
                            entity.teleportTo(hoverTarget.x, hoverTarget.y, hoverTarget.z);
                        }
                        spawnDescendenceDust(serverLevel, entity, tick);
                    }

                    if (tick == 0) {
                        AnimationHelper.playAnimation(entity, "right_arm_chargeup", DESCENDENCE_CHARGE_END_TICK);
                    } else if (tick == DESCENDENCE_CHARGE_END_TICK) {
                        targetPosition = findDescendenceTarget(serverLevel, entity);
                        AnimationHelper.playAnimation(entity, "heel_drop",
                            DESCENDENCE_CLEANUP_TICK - DESCENDENCE_CHARGE_END_TICK);
                    } else if (tick > DESCENDENCE_CHARGE_END_TICK && tick <= DESCENDENCE_IMPACT_TICK) {
                        double progress = (tick - DESCENDENCE_CHARGE_END_TICK)
                            / (double) (DESCENDENCE_IMPACT_TICK - DESCENDENCE_CHARGE_END_TICK);
                        Vec3 position = hoverTarget.lerp(targetPosition, progress);
                        entity.teleportTo(position.x, position.y, position.z);
                        MovementHelper.setVelocity(entity, Vec3.ZERO);
                        if (tick == DESCENDENCE_IMPACT_TICK) {
                            applyDescendenceImpact(serverLevel, entity, targetPosition);
                            spawnDescendenceBrambles(serverLevel, entity, targetPosition, brambles);
                        }
                    } else if (tick <= DESCENDENCE_CLEANUP_TICK) {
                        limitMovement(entity, 0.25D);
                        if (tick == DESCENDENCE_CLEANUP_TICK) {
                            clearDescendenceBrambles(serverLevel, entity, brambles);
                            cleanupForm(entity);
                        }
                    }

                    tick++;
                }
            }, 1, DESCENDENCE_CLEANUP_TICK + 1);
        });
    }

    private static Vec3 findDescendenceTarget(ServerLevel level, LivingEntity entity) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(entity));
        Vec3 forward = CombatGravityFrame.world(CombatGravityFrame.look(entity)).normalize();
        Vec3 end = start.add(forward.scale(DESCENDENCE_TARGET_RANGE));
        BlockHitResult forwardHit = level.clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        if (forwardHit.getType() == HitResult.Type.BLOCK) {
            return forwardHit.getLocation().add(up.scale(0.1D));
        }

        Vec3 down = up.scale(-1.0D);
        Vec3 fallbackEnd = end.add(down.scale(DESCENDENCE_FALLBACK_RAYCAST));
        BlockHitResult groundHit = level.clip(new ClipContext(end, fallbackEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return groundHit.getType() == HitResult.Type.BLOCK
            ? groundHit.getLocation().add(up.scale(0.1D)) : end;
    }

    private static void applyDescendenceImpact(ServerLevel level, LivingEntity attacker, Vec3 impact) {
        CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_IMPACT.get(),
            impact.x, impact.y, impact.z, 24, 1.0D, 0.25D, 1.0D, 0.05D);

        AABB area = new AABB(impact, impact).inflate(DESCENDENCE_IMPACT_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            if (!EnhancedLoveForms.isTargetable(attacker, target)
                || target.distanceToSqr(impact) > DESCENDENCE_IMPACT_RADIUS * DESCENDENCE_IMPACT_RADIUS) {
                continue;
            }
            if (Damager.hurt(attacker, target, DESCENDENCE_IMPACT_DAMAGE, true)) {
                Vec3 localOffset = CombatGravityFrame.local(target.position().subtract(impact));
                if (localOffset.lengthSqr() < 1.0E-8D) {
                    localOffset = new Vec3(0.0D, 0.0D, 1.0D);
                }
                Vec3 knockback = localOffset.normalize().scale(DESCENDENCE_KNOCKBACK)
                    .add(0.0D, 0.75D, 0.0D);
                MovementHelper.addVelocity(target, knockback);
            }
        }
    }

    private static void spawnDescendenceBrambles(ServerLevel level, LivingEntity owner,
                                                  Vec3 impact, List<DescendenceBramble> brambles) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner);
        forward = forward.subtract(up.scale(forward.dot(up)));
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forward = forward.normalize();
        }
        Vec3 side = up.cross(forward).normalize();

        for (int index = 0; index < DESCENDENCE_BRAMBLE_COUNT; index++) {
            double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(owner.getRandom().nextDouble())
                * DESCENDENCE_BRAMBLE_START_SPREAD;
            Vec3 rawStart = impact
                .add(side.scale(Math.cos(angle) * distance))
                .add(forward.scale(Math.sin(angle) * distance));
            BlockHitResult ground = findGroundBelow(level, owner, rawStart, up);
            Vec3 start = ground == null ? rawStart : ground.getLocation();
            Vec3 horizontalOffset = side.scale((owner.getRandom().nextDouble() * 2.0D - 1.0D)
                * DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET)
                .add(forward.scale((owner.getRandom().nextDouble() * 2.0D - 1.0D)
                    * DESCENDENCE_BRAMBLE_HORIZONTAL_OFFSET));
            Vec3 end = start.add(up.scale(DESCENDENCE_BRAMBLE_HEIGHT)).add(horizontalOffset);
            Vec3 arcing = randomUnitVector().scale(owner.getRandom().nextDouble()
                * DESCENDENCE_BRAMBLE_ARC);
            DescendenceBramble bramble = new DescendenceBramble(UUID.randomUUID(), start, end,
                arcing, DESCENDENCE_BRAMBLE_WIDTH);
            brambles.add(bramble);
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, true, true, bramble.start, bramble.end, Vec3.ZERO, bramble.arcing,
                RED, BLACK, bramble.width, DESCENDENCE_BRAMBLE_LIFETIME), level,
                start.x, start.y, start.z, 128.0D);
            damageDescendenceBramble(level, owner, bramble);
        }
    }

    private static void damageDescendenceBramble(ServerLevel level, LivingEntity attacker,
                                                 DescendenceBramble bramble) {
        AABB area = new AABB(bramble.start, bramble.end).inflate(1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            if (!EnhancedLoveForms.isTargetable(attacker, target)
                || !isNearDescendenceBramble(target, bramble)) {
                continue;
            }
            if (Damager.hurt(attacker, target, DESCENDENCE_BRAMBLE_DAMAGE, true)) {
                Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                    impact.x, impact.y, impact.z, 2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
    }

    private static boolean isNearDescendenceBramble(LivingEntity target, DescendenceBramble bramble) {
        Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 previous = bramble.start;
        for (int segment = 1; segment <= 8; segment++) {
            double progress = segment / 8.0D;
            Vec3 current = bramble.start.lerp(bramble.end, progress)
                .add(bramble.arcing.scale(1.0D - Math.abs(progress * 2.0D - 1.0D)));
            if (distanceToSegmentSqr(targetPoint, previous, current)
                <= Math.max(0.85D, target.getBbWidth() * 0.5D + bramble.width)
                    * Math.max(0.85D, target.getBbWidth() * 0.5D + bramble.width)) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double progress = point.subtract(start).dot(segment) / lengthSqr;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return point.distanceToSqr(start.lerp(end, progress));
    }

    private static void clearDescendenceBrambles(ServerLevel level, LivingEntity owner,
                                                  List<DescendenceBramble> brambles) {
        Vec3 position = owner.position();
        for (DescendenceBramble bramble : brambles) {
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, false, false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO,
                RED, BLACK, bramble.width, 1), level,
                position.x, position.y, position.z, 128.0D);
        }
        brambles.clear();
    }

    private static void spawnDescendenceDust(ServerLevel level, LivingEntity entity, int tick) {
        Vec3 center = CombatGravityFrame.position(entity).add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        for (int index = 0; index < 10; index++) {
            double angle = tick * 0.35D + index * (Math.PI * 2.0D / 10.0D);
            double radius = 0.65D + (index % 3) * 0.2D;
            Vec3 point = center.add(Math.cos(angle) * radius, Math.sin(tick * 0.2D + index) * 0.8D,
                Math.sin(angle) * radius);
            DustParticleOptions dust = (index & 1) == 0 ? CHARGE_RED_DUST : CHARGE_BLACK_DUST;
            CombatGravityFrame.sendParticles(level, dust, point.x, point.y, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void executeForcedTransition(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            setModelVariant(entity, 1);
            TransitionTarget selectedTarget = selectForcedTransitionTarget(serverLevel, entity);
            List<FleshVineEntity> vines = new ArrayList<>();
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(serverLevel);
            if (vine != null) {
                Vec3 start = forcedTransitionArmPosition(entity);
                vine.initializeTransition(start, entity, entity.getRandom().nextInt(),
                    FleshVineEntity.DEFAULT_DIAMETER, FORCED_TRANSITION_END_TICK + 4);
                serverLevel.addFreshEntity(vine);
                vines.add(vine);
            }
            GuardStateHelper.setGuardState(entity, FORCED_TRANSITION_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;
                 private LivingEntity latchedTarget;
                 private int latchTick = -1;
                 private boolean outcomeResolved;
                 private Vec3 endpoint;
                 private final List<Vec3> traversedPath = new ArrayList<>();

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardVines(vines);
                        cleanupForm(entity);
                        return;
                    }

                    if (tick <= 40) {
                        limitMovement(entity, 0.5D);
                        AnimationHelper.playAnimation(entity, "right_arm_forward", 2);
                        Vec3 start = forcedTransitionArmPosition(entity);
                         Vec3 targetPoint = selectedTarget == null
                             ? start : forcedTransitionTargetPoint(selectedTarget);

                         if (traversedPath.isEmpty()) {
                             traversedPath.add(start);
                             endpoint = start;
                         } else {
                             traversedPath.set(0, start);
                         }

                         if (latchedTarget == null) {
                             endpoint = forcedTransitionNextEndpoint(endpoint, targetPoint, entity, tick);
                             traversedPath.add(endpoint);
                             if (!vines.isEmpty() && vines.get(0).isAlive()) {
                                 vines.get(0).updateTransition(start, endpoint, traversedPath);
                             }
                            LivingEntity hit = findForcedTransitionHit(serverLevel, entity, start, endpoint,
                                selectedTarget == null ? null : selectedTarget.entity);
                            if (hit != null) {
                                latchedTarget = hit;
                                latchTick = tick;
                            }
                        } else {
                             Vec3 attachedPoint = latchedTarget.position()
                                 .add(0.0D, latchedTarget.getBbHeight() * 0.5D, 0.0D);
                             if (!traversedPath.isEmpty()) {
                                 traversedPath.set(traversedPath.size() - 1, attachedPoint);
                             }
                             if (!vines.isEmpty() && vines.get(0).isAlive()) {
                                 vines.get(0).updateTransition(start, attachedPoint, traversedPath);
                            }
                            int attachedTicks = tick - latchTick;
                            if (attachedTicks >= 0 && attachedTicks < FORCED_TRANSITION_LATCH_TICKS
                                && attachedTicks % FORCED_TRANSITION_DAMAGE_INTERVAL == 0) {
                                outcomeResolved = damageForcedTransitionTarget(entity, latchedTarget);
                            }
                            if (attachedTicks >= FORCED_TRANSITION_LATCH_TICKS && !outcomeResolved) {
                                applyForcedTransitionDebuffs(latchedTarget);
                                outcomeResolved = true;
                            }
                        }
                     } else if (tick == FORCED_TRANSITION_END_TICK) {
                         discardVines(vines);
                         AnimationHelper.stopAnimation(entity, "right_arm_forward");
                         AnimationHelper.playAnimation(entity, "kimetsunoyaiba:cancel", 1);
                         cleanupForm(entity);
                     }

                    tick++;
                }
            }, 1, FORCED_TRANSITION_END_TICK + 1);
        });
    }

    private static void executeShockwaveEnergyBlast(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CombatGravityFrame.run(entity, () -> {
            setModelVariant(entity, 0);
            List<FleshVineEntity> chargeVines = new ArrayList<>();
            List<UUID> radialSparks = new ArrayList<>();
            Map<UUID, UUID> targetSparks = new HashMap<>();
            Set<UUID> shockedTargets = new HashSet<>();

            AnimationHelper.playAnimation(entity, "shockwave", SHOCKWAVE_CLEANUP_TICK);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, 1.0F, 0.75F);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GUARDIAN_ATTACK,
                SoundSource.HOSTILE, 1.0F, 0.75F);
            GuardStateHelper.setGuardState(entity, SHOCKWAVE_DAMAGE, formId, true);

            AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                private int tick;

                @Override
                public void run() {
                    if (!entity.isAlive()) {
                        discardShockwaveVines(chargeVines);
                        clearShockwaveSparks(serverLevel, radialSparks, targetSparks, entity);
                        cleanupForm(entity);
                        return;
                    }

                    Vec3 center = shockwaveCenter(entity);
                    if (tick <= SHOCKWAVE_CHARGE_END_TICK) {
                        limitMovement(entity, 0.25D);
                        updateShockwaveChargeVines(serverLevel, entity, chargeVines, center, tick);
                    } else if (tick == SHOCKWAVE_CHARGE_END_TICK + 1) {
                        discardShockwaveVines(chargeVines);
                        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                        for (int index = 0; index < SHOCKWAVE_BLAST_SPARK_COUNT; index++) {
                            UUID sparkId = UUID.randomUUID();
                            radialSparks.add(sparkId);
                            sendShockwaveSpark(serverLevel, sparkId, center,
                                randomSpherePoint(center, 1.0D, entity), true, 10);
                        }
                    }

                    if (tick >= SHOCKWAVE_CHARGE_END_TICK + 1 && tick <= SHOCKWAVE_BLAST_END_TICK) {
                        double radius = shockwaveBlastRadius(tick);
                        spawnShockwaveFlashSphere(serverLevel, center, radius, entity);
                        for (UUID sparkId : radialSparks) {
                            sendShockwaveSpark(serverLevel, sparkId, center,
                                randomSpherePoint(center, radius, entity), false, 10);
                        }
                        applyShockwaveTargets(serverLevel, entity, center, radius,
                            shockedTargets, targetSparks);
                    }

                    if (tick == SHOCKWAVE_CLEANUP_TICK) {
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            SHOCKWAVE_TARGET_EFFECT_TICKS, 9, false, true, true));
                        clearShockwaveSparks(serverLevel, radialSparks, targetSparks, entity);
                        cleanupForm(entity);
                    }
                    tick++;
                }
            }, 1, SHOCKWAVE_CLEANUP_TICK + 1);
        });
    }

    private static void updateShockwaveChargeVines(ServerLevel level, LivingEntity owner,
                                                   List<FleshVineEntity> vines, Vec3 center, int tick) {
        while (vines.size() < SHOCKWAVE_CHARGE_VINE_COUNT) {
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                break;
            }
            vines.add(vine);
            level.addFreshEntity(vine);
        }

        double radius = SHOCKWAVE_CHARGE_START_RADIUS
            * (1.0D - tick / (double) SHOCKWAVE_CHARGE_END_TICK);
        double orientation = owner.getRandom().nextDouble() * Math.PI * 2.0D;
        Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        for (int index = 0; index < vines.size(); index++) {
            FleshVineEntity vine = vines.get(index);
            net.minecraft.util.RandomSource random = owner.getRandom();
            Vec3 axis = randomUnitVector(random);
            Vec3 reference = Math.abs(axis.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 baseRadial = reference.cross(axis).normalize();
            Vec3 baseTangent = axis.cross(baseRadial).normalize();
            double spin = orientation + index * Math.PI * 2.0D / SHOCKWAVE_CHARGE_VINE_COUNT
                + tick * (1.8D + random.nextDouble() * 1.2D);
            Vec3 radial = baseRadial.scale(Math.cos(spin)).add(baseTangent.scale(Math.sin(spin))).normalize();
            Vec3 tangent = axis.cross(radial).normalize();
            List<Vec3> arc = new ArrayList<>(SHOCKWAVE_CHARGE_VINE_ARC_POINTS + 1);
            for (int pointIndex = 0; pointIndex <= SHOCKWAVE_CHARGE_VINE_ARC_POINTS; pointIndex++) {
                double arcAngle = SHOCKWAVE_CHARGE_VINE_ARC * pointIndex
                    / SHOCKWAVE_CHARGE_VINE_ARC_POINTS;
                Vec3 offset = radial.scale(Math.cos(arcAngle) * radius)
                    .add(tangent.scale(Math.sin(arcAngle) * radius));
                arc.add(center.add(offset));
            }
            if (radius <= 1.0E-6D) {
                arc.clear();
                arc.add(center);
                arc.add(center.add(tangent.scale(1.0E-4D)));
            }
            Vec3 start = arc.get(0);
            Vec3 end = arc.get(arc.size() - 1);
            vine.setUp(up);
            if (tick == 0) {
                vine.initializeCharge(start, end, owner, owner.getRandom().nextInt(),
                    FleshVineEntity.DEFAULT_DIAMETER, SHOCKWAVE_CHARGE_END_TICK + 2);
                vine.setUp(up);
            }
            vine.updateChargePath(start, end, arc);
        }
    }

    private static Vec3 randomUnitVector(net.minecraft.util.RandomSource random) {
        double vertical = random.nextDouble() * 2.0D - 1.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        return new Vec3(horizontal * Math.cos(angle), vertical, horizontal * Math.sin(angle));
    }

    private static void applyShockwaveTargets(ServerLevel level, LivingEntity owner, Vec3 center,
                                              double radius, Set<UUID> shockedTargets,
                                              Map<UUID, UUID> targetSparks) {
        double radiusSquared = radius * radius;
        AABB searchArea = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            candidate -> candidate != owner && candidate.isAlive() && !candidate.isSpectator())) {
            if (target.position().distanceToSqr(center) > radiusSquared) {
                continue;
            }

            Vec3 targetCenter = target.getBoundingBox().getCenter();
            UUID sparkId = targetSparks.computeIfAbsent(target.getUUID(), ignored -> {
                UUID created = UUID.randomUUID();
                sendShockwaveSpark(level, created, center, targetCenter, true,
                    SHOCKWAVE_TARGET_SPARK_TICKS);
                return created;
            });
            sendShockwaveSpark(level, sparkId, center, targetCenter, false,
                SHOCKWAVE_TARGET_SPARK_TICKS);

            if (shockedTargets.add(target.getUUID())) {
                if (Damager.hurt(owner, target, SHOCKWAVE_DAMAGE, true)) {
                    MovementHelper.setVelocity(target, Vec3.ZERO);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        SHOCKWAVE_TARGET_EFFECT_TICKS, 19, false, false, true));
                    MobEffect suffocation = KnYEffects.getSuffocationEffect();
                    if (suffocation != null) {
                        target.addEffect(new MobEffectInstance(suffocation,
                            SHOCKWAVE_TARGET_EFFECT_TICKS, 0, false, false, true));
                    }
                    cancelShockwaveTargetActions(target);
                    scheduleShockwaveLightningParticles(owner, target);
                }
            }
        }
    }

    private static void scheduleShockwaveLightningParticles(LivingEntity owner, LivingEntity target) {
        AbilityScheduler.scheduleRepeating(owner, () -> {
            if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) {
                return;
            }
            var particle = ForgeRegistries.PARTICLE_TYPES.getValue(KNY_LIGHTNING_PARTICLE);
            if (particle instanceof SimpleParticleType simpleParticle) {
                Vec3 point = target.getBoundingBox().getCenter();
                level.sendParticles(simpleParticle, point.x, point.y, point.z,
                    3, 0.25D, 0.4D, 0.25D, 0.0D);
            }
        }, 1, SHOCKWAVE_TARGET_EFFECT_TICKS);
    }

    private static void cancelShockwaveTargetActions(LivingEntity target) {
        AbilityScheduler.cancelAll(target.getUUID());
        GuardStateHelper.clearGuardState(target);
        GuardStateHelper.clearAttackFlag(target);
        if (target instanceof Player player) {
            ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
        }
    }

    private static void sendShockwaveSpark(ServerLevel level, UUID sparkId, Vec3 start, Vec3 end,
                                           boolean spawn, int lifetime) {
        ThunderLightningSparkSyncPacket packet = new ThunderLightningSparkSyncPacket(
            sparkId, true, spawn, start, end, Vec3.ZERO, Vec3.ZERO,
            WHITE, WHITE, SHOCKWAVE_SPARK_WIDTH, lifetime, true);
        ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 128.0D);
    }

    private static void clearShockwaveSparks(ServerLevel level, List<UUID> radialSparks,
                                             Map<UUID, UUID> targetSparks, LivingEntity owner) {
        Vec3 center = shockwaveCenter(owner);
        for (UUID sparkId : radialSparks) {
            clearShockwaveSpark(level, sparkId, center);
        }
        for (UUID sparkId : targetSparks.values()) {
            clearShockwaveSpark(level, sparkId, center);
        }
        radialSparks.clear();
        targetSparks.clear();
    }

    private static void clearShockwaveSpark(ServerLevel level, UUID sparkId, Vec3 position) {
        ModNetworking.sendToNearby(new ThunderLightningSparkSyncPacket(
            sparkId, false, false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO,
            WHITE, WHITE, SHOCKWAVE_SPARK_WIDTH, 1, true), level,
            position.x, position.y, position.z, 128.0D);
    }

    private static void discardShockwaveVines(List<FleshVineEntity> vines) {
        for (FleshVineEntity vine : vines) {
            vine.discard();
        }
        vines.clear();
    }

    private static void spawnShockwaveFlashSphere(ServerLevel level, Vec3 center, double radius,
                                                   LivingEntity owner) {
        for (int index = 0; index < SHOCKWAVE_FLASH_PARTICLE_COUNT; index++) {
            Vec3 point = randomSpherePoint(center, radius, owner);
            level.sendParticles(ParticleTypes.FLASH, point.x, point.y, point.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static double shockwaveBlastRadius(int tick) {
        double progress = (tick - (SHOCKWAVE_CHARGE_END_TICK + 1))
            / (double) (SHOCKWAVE_BLAST_END_TICK - SHOCKWAVE_CHARGE_END_TICK - 1);
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return 1.0D + (SHOCKWAVE_MAX_RADIUS - 1.0D) * progress;
    }

    private static Vec3 shockwaveCenter(LivingEntity entity) {
        Vec3 localFeet = CombatGravityFrame.position(entity);
        Vec3 localHead = CombatGravityFrame.eye(entity);
        return CombatGravityFrame.world(localFeet.lerp(localHead, 0.5D));
    }

    private static Vec3 randomSpherePoint(Vec3 center, double radius, LivingEntity owner) {
        Vec3 direction;
        do {
            direction = new Vec3(owner.getRandom().nextDouble() * 2.0D - 1.0D,
                owner.getRandom().nextDouble() * 2.0D - 1.0D,
                owner.getRandom().nextDouble() * 2.0D - 1.0D);
        } while (direction.lengthSqr() < 1.0E-8D || direction.lengthSqr() > 1.0D);
        return center.add(direction.normalize().scale(radius));
    }

    private static TransitionTarget selectForcedTransitionTarget(ServerLevel level, LivingEntity owner) {
        Vec3 start = CombatGravityFrame.world(CombatGravityFrame.eye(owner));
        Vec3 direction = CombatGravityFrame.world(CombatGravityFrame.look(owner)).normalize();
        Vec3 maxEnd = start.add(direction.scale(FORCED_TRANSITION_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxEnd;

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB searchArea = new AABB(start, rayEnd).inflate(FORCED_TRANSITION_VINE_WIDTH);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> target != owner && target.isAlive() && !target.isSpectator())) {
            var hit = candidate.getBoundingBox().clip(start, rayEnd);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return new TransitionTarget(closest, rayEnd);
    }

    private static Vec3 forcedTransitionArmPosition(LivingEntity entity) {
        // PlayerAnimator's arm bone is client-only. Use the same head-relative
        // right-arm anchor on the server, recalculated while the user moves.
        Vec3 head = CombatGravityFrame.eye(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 localArm = head.add(0.0D, -0.4D, 0.0D)
            .add(right.scale(0.3D)).add(forward.scale(0.2D));
        return CombatGravityFrame.world(localArm);
    }

    private static Vec3 forcedTransitionTargetPoint(TransitionTarget target) {
        if (target.entity != null && target.entity.isAlive()) {
            return target.entity.position().add(0.0D, target.entity.getBbHeight() * 0.5D, 0.0D);
        }
        return target.location;
    }

    private static Vec3 forcedTransitionNextEndpoint(Vec3 current, Vec3 target,
                                                      LivingEntity owner, int tick) {
        Vec3 toTarget = target.subtract(current);
        if (toTarget.lengthSqr() <= FORCED_TRANSITION_VINE_SPEED * FORCED_TRANSITION_VINE_SPEED) {
            return target;
        }
        Vec3 direction = toTarget.normalize();
        Vec3 up = CombatGravityFrame.world(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 side = up.cross(direction);
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 otherSide = direction.cross(side).normalize();
        double phase = owner.getId() * 0.37D;
        Vec3 slither = side.scale(Math.sin(tick * 0.75D + phase) * FORCED_TRANSITION_SLITHER_AMPLITUDE)
            .add(otherSide.scale(Math.cos(tick * 0.61D + phase) * 0.6D));
        return current.add(direction.add(slither).normalize().scale(FORCED_TRANSITION_VINE_SPEED));
    }

    private static LivingEntity findForcedTransitionHit(ServerLevel level, LivingEntity owner,
                                                         Vec3 start, Vec3 end, LivingEntity preferred) {
        if (preferred != null && preferred.isAlive()
            && preferred.getBoundingBox().inflate(FORCED_TRANSITION_VINE_WIDTH).clip(start, end).isPresent()) {
            return preferred;
        }

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB area = new AABB(start, end).inflate(FORCED_TRANSITION_VINE_WIDTH);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
            target -> target != owner && target.isAlive() && !target.isSpectator())) {
            var hit = candidate.getBoundingBox().inflate(FORCED_TRANSITION_VINE_WIDTH).clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static boolean damageForcedTransitionTarget(LivingEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return true;
        }

        if (target.getHealth() <= FORCED_TRANSITION_DAMAGE
            && target.getRandom().nextBoolean() && tryInstantDemonize(target)) {
            return true;
        }
        Damager.hurt(owner, target, FORCED_TRANSITION_DAMAGE, true);
        return !target.isAlive();
    }

    private static void applyForcedTransitionDebuffs(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,
            FORCED_TRANSITION_DEBUFF_TICKS, 4, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
            FORCED_TRANSITION_DEBUFF_TICKS, 2, false, true, true));
    }

    private static boolean tryInstantDemonize(LivingEntity target) {
        if (Damager.isDemon(target)) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            DemonTransformationHandler.cancelTransformation(player);
            DemonTransformationHandler.consumeBaseMuzanBlood(player, 10 + player.getRandom().nextInt(11));
            player.getPersistentData().putBoolean("oni", true);
            DemonEyesSyncHandler.broadcastState(player);
            return true;
        }
        if (target instanceof BreathingSlayerEntity slayer && !slayer.isDemonized()) {
            slayer.demonize();
            return true;
        }
        if (MinecraftComesAliveCompat.demonize(target)) {
            return true;
        }
        Entity replacement = createForcedTransitionDemon(target);
        return replacement != null;
    }

    private static Entity createForcedTransitionDemon(LivingEntity source) {
        Entity replacement;
        if (source instanceof Villager) {
            replacement = ModEntities.DEMON_VILLAGER.get().create(source.level());
        } else if (source instanceof Pillager) {
            replacement = ModEntities.DEMON_PILLAGER.get().create(source.level());
        } else if (source instanceof Creeper) {
            replacement = ModEntities.DEMON_CREEPER.get().create(source.level());
        } else if (source instanceof Vindicator) {
            replacement = ModEntities.DEMON_VINDICATOR.get().create(source.level());
        } else {
            return null;
        }
        if (replacement == null) {
            return null;
        }
        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        source.level().addFreshEntity(replacement);
        source.discard();
        return replacement;
    }

    private static void spawnChargeVines(ServerLevel level, LivingEntity owner,
                                          List<FleshVineEntity> vines, Vec3 center, int tick) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        for (int index = 0; index < 3; index++) {
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                continue;
            }
            Vec3 start = center.subtract(up.scale(ULTIMATE_CHARGE_START_BELOW));
            Vec3 end = center.add(up.scale(ULTIMATE_CHARGE_END_ABOVE))
                .add(randomPerpendicular(up, owner).scale(1.5D));
            vine.initializeCharge(start, end, owner, owner.getRandom().nextInt(),
                FleshVineEntity.DEFAULT_DIAMETER, ULTIMATE_CHARGE_VINE_LIFETIME);
            vine.setUp(up);
            level.addFreshEntity(vine);
            vines.add(vine);
        }
    }

    private static void updateChargeVines(LivingEntity owner, List<FleshVineEntity> vines, int tick) {
        Vec3 center = owner.position();
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 side = GravityDirectionHelper.getLookDirection(owner);
        side = side.subtract(up.scale(side.dot(up)));
        if (side.lengthSqr() < 1.0E-8D) {
            side = randomPerpendicular(up, owner);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = up.cross(side).normalize();
        for (int index = 0; index < vines.size(); index++) {
            FleshVineEntity vine = vines.get(index);
            if (!vine.isAlive()) {
                continue;
            }
            Vec3 start = center.subtract(up.scale(ULTIMATE_CHARGE_START_BELOW));
            double angle = tick * 0.55D + index * Math.PI * 2.0D / 3.0D;
            Vec3 offset = side.scale(Math.cos(angle) * 1.5D)
                .add(otherSide.scale(Math.sin(angle) * 1.5D));
            Vec3 end = center.add(up.scale(ULTIMATE_CHARGE_END_ABOVE))
                .add(offset);
            vine.updateCharge(start, end);
        }
    }

    private static void spawnSurgeVines(ServerLevel level, LivingEntity owner, BlockHitResult impact) {
        Vec3 up = GravityDirectionHelper.localToWorld(owner, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 forward = GravityDirectionHelper.getLookDirection(owner);
        forward = forward.subtract(up.scale(forward.dot(up)));
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 line = forward.cross(up);
        if (line.lengthSqr() < 1.0E-8D) {
            line = new Vec3(1.0D, 0.0D, 0.0D);
        }
        line = line.normalize();

        for (int index = 0; index < ULTIMATE_SURGE_COUNT; index++) {
            double offset = (index - (ULTIMATE_SURGE_COUNT - 1) * 0.5D) * ULTIMATE_SURGE_SPACING;
            Vec3 point = impact.getLocation().add(line.scale(offset));
            BlockHitResult ground = findGroundBelow(level, owner, point, up);
            Vec3 surface = ground == null ? point : ground.getLocation();
            Vec3 start = surface.subtract(up.scale(ULTIMATE_SURGE_START_BELOW_GROUND));
            FleshVineEntity vine = ModEntities.FLESH_VINE.get().create(level);
            if (vine == null) {
                continue;
            }
            vine.initializeSurge(start, forward, up, owner, owner.getRandom().nextInt(),
                FleshVineEntity.DEFAULT_DIAMETER, owner.getRandom().nextDouble() * Math.PI * 2.0D);
            vine.setHomingEnabled(true);
            level.addFreshEntity(vine);
        }
    }

    private static BlockHitResult findGroundHit(ServerLevel level, LivingEntity entity) {
        Vec3 up = GravityDirectionHelper.localToWorld(entity, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 position = entity.position();
        return findGroundBelow(level, entity, position, up);
    }

    private static BlockHitResult findGroundBelow(ServerLevel level, LivingEntity entity,
                                                   Vec3 point, Vec3 up) {
        Vec3 from = point.add(up.scale(0.25D));
        Vec3 to = point.subtract(up.scale(2.0D));
        BlockHitResult hit = level.clip(new ClipContext(from, to,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static void discardVines(List<FleshVineEntity> vines) {
        for (FleshVineEntity vine : vines) {
            if (vine.isAlive()) {
                vine.beginFade();
            }
        }
        vines.clear();
    }

    private static Vec3 randomPerpendicular(Vec3 normal, LivingEntity owner) {
        Vec3 random = new Vec3(owner.getRandom().nextDouble() * 2.0D - 1.0D,
            owner.getRandom().nextDouble() * 2.0D - 1.0D,
            owner.getRandom().nextDouble() * 2.0D - 1.0D);
        Vec3 perpendicular = random.subtract(normal.scale(random.dot(normal)));
        if (perpendicular.lengthSqr() < 1.0E-8D) {
            perpendicular = normal.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return perpendicular.lengthSqr() < 1.0E-8D
            ? new Vec3(0.0D, 0.0D, 1.0D) : perpendicular.normalize();
    }

    private static void executeBlackBloodBrambles(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        setModelVariant(entity, 0);
        if (entity instanceof Player player) {
            KnYAPI.playAnimation(player, "brambles", BRAMBLE_END_TICK);
        }
        GuardStateHelper.setGuardState(entity, BRAMBLE_DAMAGE, formId, true);

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick;
            private final List<Bramble> brambles = new ArrayList<>();

            @Override
            public void run() {
                if (!entity.isAlive()) {
                    clearBrambles(serverLevel, entity, brambles);
                    cleanupForm(entity);
                    return;
                }

                if (tick < CHARGE_TICKS) {
                    limitChargeMovement(entity);
                    spawnChargeDust(serverLevel, entity, tick);
                } else if (tick <= BRAMBLE_END_TICK) {
                    double range = Math.min(BRAMBLE_MAX_RANGE,
                        (tick - CHARGE_TICKS + 1) * BRAMBLE_RANGE_STEP);
                    if (tick == CHARGE_TICKS) {
                        createBrambles(brambles);
                    }
                    updateBrambles(serverLevel, entity, brambles, range, tick == CHARGE_TICKS);
                    spawnConeParticles(serverLevel, brambleStartLocal(entity),
                        CombatGravityFrame.look(entity).normalize(), range, tick);
                    if ((tick - CHARGE_TICKS) % 2 == 0) {
                        damageCone(serverLevel, entity, range);
                        spawnRaycastSlashes(serverLevel, entity);
                    }
                } else if (tick == SEQUENCE_END_TICK) {
                    clearBrambles(serverLevel, entity, brambles);
                    cleanupForm(entity);
                }

                tick++;
            }
        }, 1, SEQUENCE_END_TICK + 1);
    }

    public static boolean isBiokinesesItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem item
            && ART_ID.equals(item.getArtId());
    }

    private static void handleLeftClick(LivingEntity attacker, Entity clickedEntity) {
        if (KnYEffects.hasSuffocation(attacker) || !isBiokinesesItem(attacker.getMainHandItem())) {
            return;
        }
        if (!markClawHandled(attacker)) {
            return;
        }
        if (attacker instanceof Player player) {
            BiokinesesArmController.startCombatWhip(player, clickedEntity);
        }
        performClawSlash(attacker, clickedEntity == null ? null : clickedEntity.getUUID());
    }

    public static void handleClientLeftClick(Player player, java.util.UUID excludedTargetId) {
        if (!isBiokinesesItem(player.getMainHandItem()) || !markClawHandled(player)) {
            return;
        }
        BiokinesesArmController.startCombatWhip(player, null);
        performClawSlash(player, excludedTargetId);
    }

    private static boolean markClawHandled(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long gameTime = serverLevel.getGameTime();
        if (attacker.getPersistentData().getLong(LAST_CLAW_TICK_TAG) == gameTime) {
            return false;
        }
        attacker.getPersistentData().putLong(LAST_CLAW_TICK_TAG, gameTime);
        return true;
    }

    private static void performClawSlash(LivingEntity attacker, java.util.UUID excludedTargetId) {
        int index = Math.floorMod(attacker.getPersistentData().getInt(CLAW_ANIMATION_INDEX_TAG), CLAW_ANIMATIONS.length);
        String animation = CLAW_ANIMATIONS[index];
        attacker.getPersistentData().putInt(CLAW_ANIMATION_INDEX_TAG, (index + 1) % CLAW_ANIMATIONS.length);
        performClawSlash(attacker, excludedTargetId, animation, FORM_BLACK_BLOOD_BRAMBLES);
    }

    private static void performClawSlash(LivingEntity attacker, java.util.UUID excludedTargetId,
                                         String animation, int formId) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var previousGravityFrame = CombatGravityFrame.enter(attacker);
        try {
            AnimationHelper.playAnimation(attacker, animation, CLAW_GUARD_TICKS);
            if (attacker instanceof ServerPlayer serverPlayer) {
                ModNetworking.sendToPlayer(new AnimationSyncPacket(
                    attacker.getUUID(),
                    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animation),
                    0,
                    CLAW_GUARD_TICKS,
                    false,
                    false,
                    null,
                    1.0F,
                    3000
                ), serverPlayer);
            }
            ModNetworking.sendToAllClients(new MobSwordSlashPacket(
                attacker.getUUID(), animation, 0, "claw_nezuko"));
            attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.0F);

            GuardStateHelper.setGuardState(attacker, CLAW_DAMAGE, formId, false);
            AbilityScheduler.scheduleOnce(attacker, () -> GuardStateHelper.clearGuardState(attacker), CLAW_GUARD_TICKS);
            damageClawArea(serverLevel, attacker, excludedTargetId);
        } finally {
            CombatGravityFrame.restore(previousGravityFrame);
        }
    }

    private static void damageClawArea(ServerLevel level, LivingEntity attacker, java.util.UUID excludedTargetId) {
        Vec3 eye = CombatGravityFrame.eye(attacker);
        Vec3 forward = CombatGravityFrame.look(attacker).normalize();
        Vec3 center = eye.add(forward.scale(CLAW_AOE_SIZE / 1.5D));
        AABB localArea = new AABB(center, center).inflate(CLAW_AOE_SIZE * 0.5D);
        AABB worldArea = CombatGravityFrame.world(localArea);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, worldArea,
            entity -> entity != attacker && entity.isAlive() && !entity.isSpectator()
                && (excludedTargetId == null || !entity.getUUID().equals(excludedTargetId)))) {
            if (EnhancedLoveForms.isTargetable(attacker, target)) {
                Damager.hurt(attacker, target, CLAW_DAMAGE, false, true);
            }
        }
    }

    private static void createBrambles(List<Bramble> brambles) {
        for (int i = 0; i < BRAMBLE_COUNT; i++) {
            Vec3 deviation = randomUnitVector().scale(Math.random() * BRAMBLE_CONE_RADIUS_RATIO);
            BrambleColor color = i == 0 ? BrambleColor.BLACK_ONLY
                : i == 1 ? BrambleColor.RED_ONLY : BrambleColor.BOTH;
            float width = (float) (BRAMBLE_MIN_WIDTH
                + Math.random() * (BRAMBLE_MAX_WIDTH - BRAMBLE_MIN_WIDTH));
            brambles.add(new Bramble(UUID.randomUUID(), deviation, color, width));
        }
    }

    private static void updateBrambles(ServerLevel level, LivingEntity entity, List<Bramble> brambles,
                                       double range, boolean spawn) {
        Vec3 baseStartLocal = brambleStartLocal(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();

        for (Bramble bramble : brambles) {
            Vec3 startLocal = baseStartLocal.add(randomUnitVector().scale(Math.random() * BRAMBLE_START_OFFSET));
            bramble.deviation = randomUnitVector().scale(Math.random() * BRAMBLE_CONE_RADIUS_RATIO);
            Vec3 arcing = CombatGravityFrame.world(randomUnitVector().scale(Math.random() * range * 0.2D));
            Vec3 endLocal = startLocal.add(forward.scale(range)).add(bramble.deviation.scale(range));
            Vec3 startWorld = CombatGravityFrame.world(startLocal);
            Vec3 endWorld = CombatGravityFrame.world(endLocal);
            Vec3 brambleRed = bramble.color == BrambleColor.BLACK_ONLY ? BLACK : RED;
            Vec3 brambleBlack = bramble.color == BrambleColor.RED_ONLY ? RED : BLACK;
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, true, spawn, startWorld, endWorld, Vec3.ZERO, arcing,
                brambleRed, brambleBlack, bramble.width, BRAMBLE_LIFETIME), level,
                startWorld.x, startWorld.y, startWorld.z, 128.0D);
            CombatGravityFrame.sendParticles(level, ModParticles.BLACKRED_SLASH.get(),
                endLocal.x, endLocal.y, endLocal.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void clearBrambles(ServerLevel level, LivingEntity entity, List<Bramble> brambles) {
        Vec3 position = entity.position();
        for (Bramble bramble : brambles) {
            ModNetworking.sendToNearby(new BrambleSyncPacket(
                bramble.id, false, false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO,
                RED, BLACK, bramble.width, 1), level,
                position.x, position.y, position.z, 128.0D);
        }
        brambles.clear();
    }

    private static Vec3 brambleStartLocal(LivingEntity entity) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 left = localLeft(forward);
        Vec3 down = CombatGravityFrame.local(GravityDirectionHelper.getLocalDown(entity)).normalize();
        return CombatGravityFrame.eye(entity)
            .add(forward.scale(BRAMBLE_START_FORWARD_OFFSET))
            .add(left.scale(BRAMBLE_START_LEFT_OFFSET))
            .add(down.scale(BRAMBLE_START_DOWN_OFFSET));
    }

    private static void limitChargeMovement(LivingEntity entity) {
        limitMovement(entity, 0.25D);
    }

    private static void limitMovement(LivingEntity entity, double horizontalScale) {
        Vec3 velocity = CombatGravityFrame.velocity(entity);
        MovementHelper.setVelocity(entity, velocity.multiply(horizontalScale, 1.0D, horizontalScale));
        entity.setSprinting(false);
    }

    private static void spawnChargeDust(ServerLevel level, LivingEntity entity, int tick) {
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-8D) {
            horizontalForward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 left = localLeft(horizontalForward);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 center = CombatGravityFrame.eye(entity).add(forward.scale(0.5D)).add(left.scale(0.4D));

        for (int ring = 0; ring < CHARGE_RING_COUNT; ring++) {
            double height = (ring / (double) (CHARGE_RING_COUNT - 1) - 0.5D) * CHARGE_COLUMN_HEIGHT;
            double spiralOffset = tick * 0.2D + ring * 0.55D;
            for (int particleIndex = 0; particleIndex < CHARGE_RING_PARTICLES; particleIndex++) {
                double angle = spiralOffset + particleIndex * (Math.PI * 2.0D / CHARGE_RING_PARTICLES);
                Vec3 particle = center.add(up.scale(height))
                    .add(horizontalForward.scale(Math.cos(angle) * CHARGE_RING_RADIUS))
                    .add(left.scale(Math.sin(angle) * CHARGE_RING_RADIUS));
                DustParticleOptions dust = ((ring + particleIndex) & 1) == 0
                    ? CHARGE_RED_DUST : CHARGE_BLACK_DUST;
                CombatGravityFrame.sendParticles(level, dust, particle.x, particle.y, particle.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static Vec3 localLeft(Vec3 forward) {
        Vec3 left = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (left.lengthSqr() < 1.0E-8D) {
            return new Vec3(-1.0D, 0.0D, 0.0D);
        }
        return left.normalize();
    }

    private static Vec3 localUp(Vec3 forward) {
        Vec3 left = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (left.lengthSqr() < 1.0E-8D) {
            left = new Vec3(-1.0D, 0.0D, 0.0D);
        }
        Vec3 up = left.cross(forward);
        return up.normalize();
    }

    private static void spawnConeParticles(ServerLevel level, Vec3 start, Vec3 forward,
                                           double range, int tick) {
        Vec3 side = randomPerpendicular(forward);
        Vec3 otherSide = forward.cross(side).normalize();
        for (int i = 0; i < 12; i++) {
            double progress = level.random.nextDouble();
            double distance = Math.max(0.15D, progress * range);
            double coneRadius = range * BRAMBLE_CONE_RADIUS_RATIO * progress;
            double angle = level.random.nextDouble() * Math.PI * 2.0D + tick * 0.35D;
            double offset = Math.sqrt(level.random.nextDouble()) * coneRadius;
            Vec3 point = start.add(forward.scale(distance))
                .add(side.scale(Math.cos(angle) * offset))
                .add(otherSide.scale(Math.sin(angle) * offset));
            DustParticleOptions dust = (i & 1) == 0 ? RED_DUST : BLACK_DUST;
            CombatGravityFrame.sendParticles(level, dust, point.x, point.y, point.z,
                1, 0.035D, 0.035D, 0.035D, 0.025D);
            if ((i & 3) == 0) {
                CombatGravityFrame.sendParticles(level, ParticleTypes.CRIT, point.x, point.y, point.z,
                    1, 0.04D, 0.04D, 0.04D, 0.08D);
            }
        }
    }

    private static Vec3 randomPerpendicular(Vec3 forward) {
        Vec3 random = randomUnitVector();
        Vec3 side = random.subtract(forward.scale(random.dot(forward)));
        if (side.lengthSqr() < 1.0E-8D) {
            side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        return side.lengthSqr() < 1.0E-8D
            ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
    }

    private static void damageCone(ServerLevel level, LivingEntity attacker, double range) {
        Vec3 start = brambleStartLocal(attacker);
        Vec3 forward = CombatGravityFrame.look(attacker).normalize();
        double radius = range * BRAMBLE_CONE_RADIUS_RATIO;
        Vec3 center = start.add(forward.scale(range * 0.5D));
        AABB localArea = new AABB(center, center).inflate(range * 0.5D + radius);
        AABB worldArea = CombatGravityFrame.world(localArea);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, worldArea,
            candidate -> candidate != attacker && candidate.isAlive() && !candidate.isSpectator())) {
            Vec3 targetCenter = CombatGravityFrame.local(target.position()
                .add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            Vec3 offset = targetCenter.subtract(start);
            double projection = offset.dot(forward);
            if (projection < 0.0D || projection > range) {
                continue;
            }
            double lateralDistance = offset.subtract(forward.scale(projection)).length();
            double allowedRadius = radius * (projection / range) + target.getBbWidth() * 0.5D;
            if (lateralDistance <= allowedRadius && Damager.hurt(attacker, target, BRAMBLE_DAMAGE, true)) {
                Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_IMPACT.get(),
                    impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void spawnRaycastSlashes(ServerLevel level, LivingEntity entity) {
        Vec3 start = CombatGravityFrame.eye(entity);
        Vec3 forward = CombatGravityFrame.look(entity).normalize();
        Set<BlockPos> visitedBlocks = new HashSet<>();
        for (double distance = 0.0D; distance <= BRAMBLE_RAYCAST_RANGE; distance += 0.2D) {
            Vec3 point = start.add(forward.scale(distance));
            Vec3 worldPoint = CombatGravityFrame.world(point);
            BlockPos blockPos = BlockPos.containing(worldPoint);
            if (visitedBlocks.add(blockPos)
                && level.getBlockState(blockPos).isSolidRender(level, blockPos)) {
                CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                    worldPoint.x, worldPoint.y, worldPoint.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                for (int particle = 0; particle < 3; particle++) {
                    Vec3 offset = randomUnitVector().scale(level.random.nextDouble() * 7.0D);
                    Vec3 nearbySlash = worldPoint.add(offset);
                    CombatGravityFrame.sendWorldParticles(level, ModParticles.BLACKRED_SLASH.get(),
                        nearbySlash.x, nearbySlash.y, nearbySlash.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static Vec3 randomUnitVector() {
        Vec3 vector = new Vec3(
            Math.random() * 2.0D - 1.0D,
            Math.random() * 2.0D - 1.0D,
            Math.random() * 2.0D - 1.0D);
        return vector.lengthSqr() < 1.0E-8D ? new Vec3(1.0D, 0.0D, 0.0D) : vector.normalize();
    }

    @Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide() && BiokinesesArmController.isActive(entity)) {
                try {
                    BiokinesesArmController.tick(entity);
                } catch (Exception exception) {
                    System.err.println("[Biokineses] Arm controller tick failed: " + exception);
                }
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                handleLeftClick(event.getEntity(), event.getTarget());
            }
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getEntity().level().isClientSide()) {
                handleLeftClick(event.getEntity(), null);
            }
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (!event.getEntity().level().isClientSide()
                && KnYEffects.hasSuffocation(event.getEntity())
                && SwordRegistry.getSword(event.getItemStack().getItem()) != null) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker && !(source instanceof Player)
                && isBiokinesesItem(attacker.getMainHandItem())) {
                handleLeftClick(attacker, event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !event.player.swinging || event.player.swingTime != 0
                || SwampDemonArt.shouldIgnoreSwingForAbilityUse(event.player)) {
                return;
            }
            handleLeftClick(event.player, null);
        }
    }

    private static void cleanupForm(LivingEntity entity) {
        setModelVariant(entity, 0);
        GuardStateHelper.clearGuardState(entity);
        GuardStateHelper.clearAttackFlag(entity);
        if (entity instanceof Player player) {
            ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
        }
    }

    private static final class Bramble {
        private final UUID id;
        private Vec3 deviation;
        private final BrambleColor color;
        private final float width;

        private Bramble(UUID id, Vec3 deviation, BrambleColor color, float width) {
            this.id = id;
            this.deviation = deviation;
            this.color = color;
            this.width = width;
        }
    }

    private static final class DescendenceBramble {
        private final UUID id;
        private final Vec3 start;
        private final Vec3 end;
        private final Vec3 arcing;
        private final float width;

        private DescendenceBramble(UUID id, Vec3 start, Vec3 end, Vec3 arcing, float width) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.arcing = arcing;
            this.width = width;
        }
    }

    private static final class TransitionTarget {
        private final LivingEntity entity;
        private final Vec3 location;

        private TransitionTarget(LivingEntity entity, Vec3 location) {
            this.entity = entity;
            this.location = location;
        }
    }

    private enum BrambleColor {
        BLACK_ONLY,
        RED_ONLY,
        BOTH
    }
}
