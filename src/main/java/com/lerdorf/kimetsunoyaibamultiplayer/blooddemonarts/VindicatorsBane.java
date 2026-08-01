package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ParticleHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonVindicatorEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public final class VindicatorsBane {
    public static final String ART_ID = "vindicators_bane";
    public static final int FORM_EXECUTIONERS_CLEAVE = 3200;
    public static final int FORM_SPLITTER_STRIKE = 3201;
    public static final int FORM_BLOODLUST_RUSH = 3202;

    private VindicatorsBane() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }

        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Vindicator's Bane", createTechnique());
    }

    public static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Blood Demon Art: Vindicator's Bane",
            List.of(
                new BloodDemonArtForm(FORM_EXECUTIONERS_CLEAVE, "Executioner's Cleave", "Spin and cleave everything around you.", 8, VindicatorsBane::executeExecutionersCleave),
                new BloodDemonArtForm(FORM_SPLITTER_STRIKE, "Splitter Strike", "Leap up, crash down, and tear targets open.", 10, VindicatorsBane::executeSplitterStrike),
                new BloodDemonArtForm(FORM_BLOODLUST_RUSH, "Bloodlust Rush", "Rush forward in a flurry of front flips.", 12, VindicatorsBane::executeBloodlustRush)
            ),
            0x8f8f8f
        );
    }

    public static boolean isVindicatorsBaneItem(ItemStack stack) {
        return stack != null
            && stack.getItem() instanceof BloodDemonArtAxeItem axeItem
            && ART_ID.equals(axeItem.getArtId());
    }

    private static void executeExecutionersCleave(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        playAnimation(entity, "sword_rotate", 12);
        playDramaticCleaveSound(serverLevel, entity);

        ParticleHelper.spawnCircleParticles(serverLevel, entity.position().add(0.0D, 1.0D, 0.0D), 5.0D, ParticleTypes.SWEEP_ATTACK, 36);

        float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1F;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
            entity.getBoundingBox().inflate(5.0D, 1.5D, 5.0D),
            living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 25.0D)) {
            Damager.hurt(entity, target, damage);
        }
    }

    private static void executeSplitterStrike(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 launch = getTargetDirection(entity).scale(0.55D);
        playAnimation(entity, "sword_to_upper", 10);
        playSplitterLaunchSound(serverLevel, entity);
        entity.setDeltaMovement(entity.getDeltaMovement().add(launch.x, 0.85D, launch.z));
        entity.hurtMarked = true;

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive()) {
                return;
            }

            Vec3 dive = getTargetDirection(entity).scale(0.7D);
            entity.setDeltaMovement(dive.x, -1.0D, dive.z);
            entity.hurtMarked = true;
            playAnimation(entity, "sword_overhead", 12);
            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!entity.isAlive()) {
                    return;
                }

                playSplitterImpactSound(serverLevel, entity);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(0.1D), entity.getZ(), 12, 0.45D, 0.15D, 0.45D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY(0.2D), entity.getZ(), 20, 0.8D, 0.3D, 0.8D, 0.03D);

                float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.35F;
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(3.0D, 1.5D, 3.0D),
                    living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 9.0D)) {
                    if (Damager.hurt(entity, target, damage)) {
                        BleedingHandler.applyOrRefreshBleeding(target, 20 * 10, 1);
                    }
                }
            }, 10);
        }, 10);
    }

    private static void executeBloodlustRush(LivingEntity entity, Level level, int formId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AbilityScheduler.scheduleRepeating(entity, new Runnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!entity.isAlive()) {
                    return;
                }

                Vec3 dash = getLookDirection(entity).scale(1.0D);
                entity.setDeltaMovement(dash.x, 0.12D, dash.z);
                entity.hurtMarked = true;

                if (tick % 10 == 0) {
                    playAnimation(entity, "kimetsunoyaibamultiplayer:front_flip", 10);
                    playBloodlustRushPulse(serverLevel, entity, tick / 10);
                    double yaw = Math.atan2(dash.z, dash.x) - (Math.PI / 2.0D);
                    double pitch = Math.atan2(dash.y, Math.sqrt((dash.x * dash.x) + (dash.z * dash.z)));
                    Vec3 center = entity.position().add(0.0D, 1.0D, 0.0D);
                    ParticleHelper.spawnVerticalArc(serverLevel, center, yaw, pitch,
                        1.6D, 0.1D, 360, 10.0D, 0.0D, ParticleTypes.SWEEP_ATTACK, 24);
                    ParticleHelper.spawnVerticalArc(serverLevel, center, yaw, pitch,
                        1.8D, 0.1D, 360, 10.0D, 0.0D, ModParticles.BLOOD.get(), 24);

                    float damage = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(3.0D, 1.2D, 3.0D),
                        living -> living != entity && living.isAlive() && entity.distanceToSqr(living) <= 9.0D)) {
                        Damager.hurt(entity, target, damage);
                    }
                }

                tick++;
            }
        }, 1, 40);
    }

    private static void playDramaticCleaveSound(ServerLevel level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 0.8F, 1.7F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.75F + (entity.getRandom().nextFloat() * 0.1F));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.6F + (entity.getRandom().nextFloat() * 0.1F));
    }

    public static void playSplitterLaunchSound(ServerLevel level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.65F, 1.65F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    public static void playSplitterImpactSound(ServerLevel level, LivingEntity entity) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.1F, 0.75F + (entity.getRandom().nextFloat() * 0.1F));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.2F, 0.95F + (entity.getRandom().nextFloat() * 0.1F));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.35F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.55F, 1.65F);
    }

    private static void playBloodlustRushPulse(ServerLevel level, LivingEntity entity, int pulseIndex) {
        float stepPitch = 1.0F + (pulseIndex * 0.08F);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.85F + (entity.getRandom().nextFloat() * 0.1F));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.9F, stepPitch);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 0.5F, 1.55F - (pulseIndex * 0.05F));
    }

    private static void playAnimation(LivingEntity entity, String animation, int duration) {
        String resolvedAnimation = animation;
        int namespaceSplit = animation.indexOf(':');
        if (namespaceSplit >= 0) {
            resolvedAnimation = animation.substring(namespaceSplit + 1);
        }

        if (entity instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(resolvedAnimation, duration);
        } else if (entity instanceof net.minecraft.world.entity.player.Player player) {
            KnYAPI.playAnimation(player, animation, duration);
        }
    }

    public static Vec3 getTargetDirection(LivingEntity entity) {
        LivingEntity target = entity.getLastHurtMob();
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
            target = mob.getTarget();
        }

        if (target != null && target.isAlive()) {
            Vec3 towardTarget = target.position().subtract(entity.position());
            if (towardTarget.horizontalDistanceSqr() > 1.0E-4D) {
                return towardTarget.normalize();
            }
        }

        Vec3 look = entity.getLookAngle();
        return look.horizontalDistanceSqr() > 1.0E-4D ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static Vec3 getLookDirection(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        return look.lengthSqr() > 1.0E-4D ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private static final String LAST_LEFT_CLICK_TICK_TAG = "VindicatorsBaneLastLeftClickTick";
        private static final String LAST_ABILITY_USE_TICK_TAG = "VindicatorsBaneLastAbilityUseTick";

        private Events() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
                return;
            }

            Player player = event.player;
            if (isVindicatorsBaneItem(player.getMainHandItem())
                && player.swinging
                && player.swingTime == 0
                && !shouldIgnoreSwingForAbilityUse(player)) {
                handlePlayerLeftClick(player, null);
            }
        }

        @SubscribeEvent
        public static void onAttackEntity(AttackEntityEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }

            handlePlayerLeftClick(event.getEntity(), event.getTarget());
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getEntity().level().isClientSide || !isVindicatorsBaneItem(event.getItemStack())) {
                return;
            }

            handlePlayerLeftClick(event.getEntity(), null);
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }

            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker && !(attacker instanceof Player)
                && isVindicatorsBaneItem(attacker.getMainHandItem())) {
                BloodDemonArtM1AttackHandler.performNichirinLikeSlashAttack(attacker, event.getEntity().getUUID());
            }
        }

        private static void handlePlayerLeftClick(Player player, Entity target) {
            if (!isVindicatorsBaneItem(player.getMainHandItem()) || !markLeftClickHandled(player)) {
                return;
            }

            BloodDemonArtM1AttackHandler.performNichirinLikeSlashAttack(player, target != null ? target.getUUID() : null);
        }

        private static boolean markLeftClickHandled(Player player) {
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            long gameTime = serverLevel.getGameTime();
            long lastHandled = player.getPersistentData().getLong(LAST_LEFT_CLICK_TICK_TAG);
            if (lastHandled == gameTime) {
                return false;
            }

            player.getPersistentData().putLong(LAST_LEFT_CLICK_TICK_TAG, gameTime);
            return true;
        }

        private static boolean shouldIgnoreSwingForAbilityUse(Player player) {
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            long gameTime = serverLevel.getGameTime();
            long lastAbilityUseTick = player.getPersistentData().getLong(LAST_ABILITY_USE_TICK_TAG);
            return gameTime - lastAbilityUseTick <= 2L;
        }
    }
}
