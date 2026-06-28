package com.lerdorf.kimetsunoyaibamultiplayer.customdemonart;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.VindicatorsBane;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DarkStarVisualEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SpineEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SwampHandEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.events.BleedingHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CustomBloodDemonArtRuntime {
    private static final String NBT_DAMAGE = "Damage";
    private static final String NBT_GUARD = "guard";
    private static final String NBT_ATTACK = "attack";
    private static final String NBT_CUSTOM_BREATHING_ACTIVE = "knymp_custom_breathing_active";
    private static final String NBT_CUSTOM_ART_ACTIVE = "knymp_custom_blood_demon_art_active";
    private static final String NBT_CUSTOM_ART_RUNTIME_ID = "knymp_custom_blood_demon_art_runtime_id";
    private static final String NBT_MIDAS_OWNER = "knymp_midas_owner";
    private static final String NBT_MIDAS_EXPIRES = "knymp_midas_expires";
    private static final String NBT_MIDAS_BONUS_MULTIPLIER = "knymp_midas_bonus_multiplier";
    private static final String NBT_MIDAS_MARK_TEAM = "knymp_midas_mark_team";
    private static final String NBT_MIDAS_PREVIOUS_TEAM = "knymp_midas_previous_team";

    private CustomBloodDemonArtRuntime() {
    }

    /**
     * Unified animation helper with layer and speed control.
     */
    private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName, int maxTicks, float speed, int layer) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
        } else if (entity instanceof BreathingSlayerEntity slayer) {
            slayer.playGeckoAnimation(animationName, maxTicks);
        } else {
            AnimationHelper.playAnimationOnLayer(entity, animationName, maxTicks, speed, layer);
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
        } else if (entity instanceof net.minecraft.world.entity.player.Player player) {
            KnYAPI.playAnimation(player, animation, duration);
        }
    }

    public static Component selectedFormName(ServerPlayer player, ItemStack stack) {
        CustomBloodDemonArtSavedData.PlayerArtData data = CustomBloodDemonArtSavedData.get((ServerLevel) player.level()).getOrCreate(player);
        int selectedSlot = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        return selectedFormDisplay(data, selectedSlot);
    }

    public static boolean cycleForm(ServerPlayer player, ItemStack stack, int direction) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(serverLevel);
        CustomBloodDemonArtSavedData.PlayerArtData data = savedData.getOrCreate(player);

        List<Integer> filledSlots = new ArrayList<>();
        int unlockedSlots = savedData.getUnlockedSlots(player);
        for (int i = 0; i < Math.min(unlockedSlots, data.slots().size()); i++) {
            if (data.slots().get(i).filled()) {
                filledSlots.add(i);
            }
        }
        if (filledSlots.isEmpty()) {
            player.displayClientMessage(Component.literal("No custom blood demon art forms are configured yet.")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        int current = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        int currentIndex = Math.max(0, filledSlots.indexOf(current));
        int nextIndex = Math.floorMod(currentIndex + (direction < 0 ? -1 : 1), filledSlots.size());
        int slotIndex = filledSlots.get(nextIndex);

        CustomDemonArtItem.setSelectedSlot(stack, slotIndex);
        savedData.setSelectedSlot(player, slotIndex);
        CustomDemonArtItem.setDisplayInfo(stack, safeArtName(data), safeFormName(data, slotIndex), data.coreSettings().chatColor());
        player.displayClientMessage(selectedFormDisplay(data, slotIndex), true);
        return true;
    }

    public static boolean use(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!Damager.isDemon(player)) {
            player.displayClientMessage(Component.literal("You must be a demon to use this ability")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        CustomBloodDemonArtSavedData.PlayerArtData data = CustomBloodDemonArtSavedData.get(serverLevel).getOrCreate(player);
        int slotIndex = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        if (slotIndex < 0 || slotIndex >= data.slots().size()) {
            return false;
        }
        CustomBloodDemonArtSavedData.CustomFormSlot slot = data.slots().get(slotIndex);
        if (!slot.filled() || slot.moves().isEmpty()) {
            player.displayClientMessage(Component.literal("That custom form has no moves configured yet.")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        clearCustomRuntimeState(player);
        long runtimeId = beginCustomRuntimeState(player);

        AmplifierTotals amplifierTotals = AmplifierTotals.fromCounts(slot.amplifierCounts());
        int tickOffset = 0;
        for (CustomBloodDemonArtSavedData.MoveType move : slot.moves()) {
            final int startAt = tickOffset;
            AbilityScheduler.scheduleOnce(player, () -> executeMove(player, data.coreSettings(), move, amplifierTotals), startAt);
            tickOffset += move.durationTicks();
        }
        final int totalDuration = tickOffset;

        // Run cleanup once after the full form sequence completes.
        AbilityScheduler.scheduleOnce(player, () -> {
            clearCustomRuntimeState(player, runtimeId);
        }, totalDuration + 2);

        int defenseAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE);
        if (defenseAmp > 0) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, Math.max(60, tickOffset + 20), defenseAmp - 1));
        }
        int speedAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED);
        if (speedAmp > 0) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, Math.max(60, tickOffset + 20), speedAmp - 1));
        }

        int cooldownTicks = Math.max(20, slot.cooldownSeconds() * 20);
        player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);
        CustomDemonArtItem.setDisplayInfo(stack, safeArtName(data), safeFormName(data, slotIndex), data.coreSettings().chatColor());
        Component display = selectedFormDisplay(data, slotIndex);
        player.displayClientMessage(display, true);
        BreathingFormAnnouncementHelper.announceCustomForm(player, display, data.coreSettings().chatColor());
        return true;
    }

    private static long beginCustomRuntimeState(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        long runtimeId = player.serverLevel().getGameTime() ^ player.getUUID().getLeastSignificantBits() ^ player.getRandom().nextLong();
        if (runtimeId == 0L) {
            runtimeId = 1L;
        }
        tag.putLong(NBT_CUSTOM_ART_RUNTIME_ID, runtimeId);
        tag.putBoolean(NBT_CUSTOM_ART_ACTIVE, true);
        return runtimeId;
    }

    private static void clearCustomRuntimeState(ServerPlayer player) {
        clearCustomRuntimeState(player, -1L);
    }

    private static void clearCustomRuntimeState(ServerPlayer player, long expectedRuntimeId) {
        CompoundTag tag = player.getPersistentData();
        if (expectedRuntimeId > 0L && tag.getLong(NBT_CUSTOM_ART_RUNTIME_ID) != expectedRuntimeId) {
            return;
        }

        GuardStateHelper.clearGuardState(player);
        setCancelAttackSwing(player, false);
        MovementHelper.resetStepHeight(player);
        player.setNoGravity(false);
        player.fallDistance = 0.0F;

        tag.remove(NBT_DAMAGE);
        tag.remove(NBT_GUARD);
        tag.remove(NBT_ATTACK);
        tag.remove(NBT_CUSTOM_BREATHING_ACTIVE);
        tag.remove(NBT_CUSTOM_ART_ACTIVE);
        tag.remove(NBT_CUSTOM_ART_RUNTIME_ID);
    }

    public static boolean grantItem(ServerPlayer player) {
        return grantItem(player, 1);
    }

    public static boolean grantItem(ServerPlayer player, int modelVariant) {
        if (player.experienceLevel < 5) {
            return false;
        }

        ItemStack existingMatch = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.CUSTOM_DEMON_ART.get()) {
                existingMatch = stack;
                break;
            }
        }
        if (!existingMatch.isEmpty() || player.getOffhandItem().getItem() == ModItems.CUSTOM_DEMON_ART.get()) {
            return false;
        }

        ItemStack stack = new ItemStack(ModItems.CUSTOM_DEMON_ART.get());
        CustomDemonArtItem.setPlayerSkin(stack, player.getUUID(), player.getGameProfile().getName());
        CustomDemonArtItem.setModelVariant(stack, modelVariant);
        CustomBloodDemonArtSavedData.PlayerArtData data = CustomBloodDemonArtSavedData.get((ServerLevel) player.level()).getOrCreate(player);
        CustomDemonArtItem.setDisplayInfo(stack, safeArtName(data), safeFormName(data, data.selectedSlot()), data.coreSettings().chatColor());
        player.giveExperienceLevels(-5);
        player.getInventory().add(stack);
        return true;
    }

    private static Component selectedFormDisplay(CustomBloodDemonArtSavedData.PlayerArtData data, int selectedSlot) {
        return Component.literal(safeArtName(data) + ": " + safeFormName(data, selectedSlot))
            .withStyle(style -> style.withColor(data.coreSettings().chatColor()));
    }

    private static String safeArtName(CustomBloodDemonArtSavedData.PlayerArtData data) {
        String artName = data.artName();
        return artName == null || artName.isBlank() ? CustomBloodDemonArtSavedData.DEFAULT_ART_NAME : artName;
    }

    private static String safeFormName(CustomBloodDemonArtSavedData.PlayerArtData data, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= data.slots().size()) {
            return "No Form";
        }
        CustomBloodDemonArtSavedData.CustomFormSlot slot = data.slots().get(slotIndex);
        if (!slot.filled()) {
            return "Empty Form";
        }
        return slot.name() == null || slot.name().isBlank() ? ("Form " + (slotIndex + 1)) : slot.name();
    }

    /**
     * Helper method to set cancel attack swing state and sync to client.
     * Only works for Player entities.
     */
    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return;
        }

        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA).ifPresent(data -> {
            data.setCancelAttackSwing(value);
        });

        if (player instanceof ServerPlayer serverPlayer) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket(
                    player.getUUID(), value
                ),
                serverPlayer
            );
        }
    }

    private static void executeMove(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                    CustomBloodDemonArtSavedData.MoveType move, AmplifierTotals amplifierTotals) {
        switch (move) {
            case PUNCH_RIGHT -> executePunch(player, core, "punch_right", amplifierTotals);
            case PUNCH_LEFT -> executePunch(player, core, "punch_left", amplifierTotals);
            case FRONT_FLIP -> executeFrontFlip(player, core, amplifierTotals);
            case MELEE_COMBO -> executeMeleeCombo(player, core, amplifierTotals);
            case WITHER_SKULL -> executeWitherSkull(player, core, amplifierTotals);
            case BLAZE_BARRAGE -> executeBlazeBarrage(player, core, amplifierTotals);
            case GUARDIAN_LASER -> executeGuardianLaser(player, core, amplifierTotals);
            case SINGULARITY -> executeSingularity(player, core, amplifierTotals);
            case DARK_STAR -> executeDarkStar(player, core, amplifierTotals);
            case TASTE_OF_IMMORTALITY -> executeTasteOfImmortality(player, core, amplifierTotals);
            case GLIDE -> executeGlide(player, core, amplifierTotals);
            case ROAR -> executeRoar(player, core, amplifierTotals);
            case FLOWER_DANCE -> executeFlowerDance(player, core, amplifierTotals);
            case YAMATO_OROCHI -> executeYamatoOrochi(player, core, amplifierTotals);
            case EIGHTFOLD_AMBUSH -> executeEightfoldAmbush(player, core, amplifierTotals);
            case SKIN_SHED -> executeSkinShed(player, core, amplifierTotals);
            case SNAKE_STEP -> executeSnakeStep(player, core, amplifierTotals);
            case EIGHTFOLD_ASCENDANT -> executeEightfoldAscendant(player, core, amplifierTotals);
            case SPINE_BURST -> executeSpineBurst(player, core, amplifierTotals);
            case MIDAS_TOUCH -> executeMidasTouch(player, core, amplifierTotals);
            case DEFEND -> executeDefend(player, core, amplifierTotals);
            case VINDICATORS_CLEAVE -> executeVindicatorsCleave(player, core, amplifierTotals);
            case WHITEOUT -> executeWhiteout(player, core, amplifierTotals);
            case EXPLODE -> executeExplode(player, core, amplifierTotals);
            case FANGS_OF_THE_EARTH -> executeFangsOfTheEarth(player, core, amplifierTotals);
            case VEX_SWARM -> executeVexSwarm(player, core, amplifierTotals);
            case PRISON -> executePrison(player, core, amplifierTotals);
            case SONIC_SHRIEK -> executeSonicShriek(player, core, amplifierTotals);
            case NIGHT_TERROR -> executeNightTerror(player, core, amplifierTotals);
            case INFERNAL_SPIN -> executeInfernalSpin(player, core, amplifierTotals);
            case FLYTRAP -> executeFlytrap(player, core, amplifierTotals);
            case GRAVE_PULSE -> executeGravePulse(player, core, amplifierTotals);
            case HOVER -> executeHover(player, core, amplifierTotals);
            case SHOOTING_STAR -> executeShootingStar(player, core, amplifierTotals);
            case LIGHTNING_CHARGE -> executeLightningCharge(player, core, amplifierTotals);
            case CHAIN_LIGHTNING -> executeChainLightning(player, core, amplifierTotals);
            case INCENDIARY_PROJECTILE -> executeIncendiaryProjectile(player, core, amplifierTotals);
        }
    }

    private static void executePunch(ServerPlayer player,
                                 CustomBloodDemonArtSavedData.CoreSettings core,
                                 String animationName,
                                 AmplifierTotals amplifierTotals) {
        
        ParticleOptions particle = resolveParticle(core.primaryParticle());
        double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));

        float damage = Damager.calculateScaledDamage(player, 5.0F * damageScale);
        GuardStateHelper.setGuardState(player, 5*ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE)));
        
        ServerLevel level = player.serverLevel();

        AnimationHelper.playAnimation(player, animationName, 10);

        // Yaw-based forward direction so the cone stays horizontal
        float yaw = (float) Math.toRadians(-player.getYRot());
        Vec3 forward = new Vec3(Math.sin(yaw), 0.0D, Math.cos(yaw)).normalize();

        // Right vector for drawing circular cone rings
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();

        Vec3 eyePos = player.getEyePosition();

        double maxDistance = 6.0D * rangeScale;
        double baseRadius = 0.45D;
        double radiusGrowth = 0.45D * rangeScale;

        // Forward cone shockwave particles
        for (double dist = 1.0D; dist <= maxDistance; dist += 0.6D) {
            final double distance = dist;
            AbilityScheduler.scheduleOnce(player, () -> {
                Vec3 center = eyePos.add(forward.scale(distance));
                double radius = baseRadius + distance * radiusGrowth;

                for (double angle = 0.0D; angle < Math.PI * 2.0D; angle += Math.max(0.15D, 1.0D / radius)) {
                    Vec3 particlePos = center
                        .add(right.scale(radius * Math.cos(angle)))
                        .add(0.0D, radius * Math.sin(angle), 0.0D);

                    level.sendParticles(
                        particle,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                    );
                }
            }, (int)((distance-1) * 3));
        }

        // Cone-shaped hit detection
        AABB hitBox = player.getBoundingBox().expandTowards(forward.scale(maxDistance)).inflate(3.5D * rangeScale);

        for (LivingEntity target : player.level().getEntitiesOfClass(
            LivingEntity.class,
            hitBox,
            entity -> entity != player && entity.isAlive()
        )) {
            Vec3 toTarget = target.position().subtract(player.position());
            Vec3 flatToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);

            if (flatToTarget.lengthSqr() < 0.001D) {
                continue;
            }

            double distance = flatToTarget.length();
            if (distance > maxDistance) {
                continue;
            }

            Vec3 direction = flatToTarget.normalize();
            double forwardDot = direction.dot(forward);

            // Higher = narrower cone. 0.55 is roughly a wide forward cone.
            if (forwardDot < 0.55D) {
                continue;
            }

            Damager.hurt(player, target, damage, true);

            MovementHelper.addVelocity(
                target,
                direction.x * 0.8D,
                0.35D,
                direction.z * 0.8D
            );

            applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
        }

        applySelfPotion(core.primaryPotion(), player, amplifierTotals);

        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS,
            1.0F,
            0.8F
        );
    }

    private static void executeFrontFlip(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
            AmplifierTotals amplifierTotals) {
        GuardStateHelper.setGuardState(player, Damager.calculateScaledDamage(player, 5)*ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE)));
        double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        AnimationHelper.playAnimation(player, "kimetsunoyaibamultiplayer:front_flip", 15);
        Vec3 look = player.getLookAngle().normalize();
        MovementHelper.setVelocity(player, look.x * 1.0D * speedScale * rangeScale, 0.65D * speedScale, look.z * 1.0D * speedScale * rangeScale);
        for (int i = 0; i < 5; i++) {
            final int offset = i * 3;
            AbilityScheduler.scheduleOnce(player, () -> {
                Vec3 pos = player.position().add(0.0D, 0.35D, 0.0D);
                spawnBurst(player.serverLevel(), core.secondaryParticle(), pos, 7);
            }, offset);
        }
        applySelfPotion(core.secondaryPotion(), player, amplifierTotals);
    }

    private static void executeMeleeCombo(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
            AmplifierTotals amplifierTotals) {
        GuardStateHelper.setGuardState(player, 4*ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE)));
        double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        float damage = Damager.calculateScaledDamage(player, 4.0F * damageScale);
        String[] animations = {"punch_right", "punch_left", "kick_right", "kick_left"};
        for (int i = 0; i < animations.length; i++) {
            final String animation = animations[i];
            AbilityScheduler.scheduleOnce(player, () -> {
                AnimationHelper.playAnimation(player, animation, 10);
                Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.3D * rangeScale));
                spawnBurst(player.serverLevel(), core.primaryParticle(), center, 10);
                AABB hitBox = new AABB(center, center).inflate(2.25D * rangeScale, 1.35D, 2.25D * rangeScale);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                    entity -> entity != player && entity.isAlive())) {
                    Damager.hurt(player, target, damage, true);
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.75F);
            }, i * 3);
        }
        applySelfPotion(core.primaryPotion(), player, amplifierTotals);
    }

    private static boolean isEnemy(ServerPlayer player, LivingEntity entity) {
        return Damager.isAngry(player, entity) || Damager.isDemonSlayer(entity);
    }

    private static void executeWitherSkull(ServerPlayer player,
                                       CustomBloodDemonArtSavedData.CoreSettings core,
                                       AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();

        final ParticleOptions trailParticle = resolveParticle(core.primaryParticle());

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final double baseSpeed = 0.9D;
        final double speed = baseSpeed * speedScale;

        final float baseDamage = 6.0F;
        final float explosionDamage = Damager.calculateScaledDamage(player, baseDamage * damageScale);

        final float baseExplosionRadius = 2.5F;
        final float explosionRadius = baseExplosionRadius * damageScale;

        // Lifetime scales with range. Higher speed also gets a little more lifetime
        // so the skull can actually benefit from both speed and range.
        final int maxLifetimeTicks = Math.max(20, (int) (100 * rangeScale));

        final float initialYaw = player.getYRot();
        final float initialPitch = player.getXRot();

        Vec3 launchDir = player.getLookAngle().normalize();
        Vec3 spawnPos = player.getEyePosition().add(launchDir.scale(1.1D));

        WitherSkull skull = new WitherSkull(level, player, launchDir.x, launchDir.y, launchDir.z);
        skull.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        skull.setYRot(initialYaw);
        skull.setXRot(initialPitch);
        skull.setDeltaMovement(launchDir.scale(speed));

        level.addFreshEntity(skull);

        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.WITHER_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );

        final int[] ticksAlive = {0};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!skull.isAlive()) {
                return;
            }

            ticksAlive[0]++;

            Vec3 skullPos = skull.position();

            level.sendParticles(
                trailParticle,
                skullPos.x,
                skullPos.y + 0.15D,
                skullPos.z,
                3,
                0.1D,
                0.1D,
                0.1D,
                0.0D
            );

            LivingEntity nearestEnemy = null;
            double nearestEnemyDistSqr = Double.MAX_VALUE;

            AABB nearby = skull.getBoundingBox().inflate(5.0D * rangeScale);

            for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                nearby,
                entity -> entity != player && entity.isAlive() && isEnemy(player, entity)
            )) {
                double distSqr = entity.distanceToSqr(skull);
                if (distSqr < nearestEnemyDistSqr) {
                    nearestEnemyDistSqr = distSqr;
                    nearestEnemy = entity;
                }
            }

            boolean shouldExplode =
                ticksAlive[0] >= maxLifetimeTicks ||
                nearestEnemyDistSqr <= 9.0D;

            if (shouldExplode) {
                level.explode(
                    skull,
                    skull.getX(),
                    skull.getY(),
                    skull.getZ(),
                    explosionRadius,
                    Level.ExplosionInteraction.MOB
                );

                AABB explosionBox = skull.getBoundingBox().inflate(explosionRadius);

                for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    explosionBox,
                    entity -> entity != player && entity.isAlive() && isEnemy(player, entity)
                )) {
                    Damager.hurt(player, entity, explosionDamage, true);

                    applyTargetPotion(core.primaryPotion(), player, entity, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, entity, amplifierTotals);
                }

                skull.discard();
                return;
            }

            Vec3 travelDir;

            if (nearestEnemy != null) {
                travelDir = nearestEnemy.getEyePosition()
                    .subtract(skull.position())
                    .normalize();
            } else {
                float yawDelta = Mth.wrapDegrees(player.getYRot() - initialYaw);
                float pitchDelta = Mth.wrapDegrees(player.getXRot() - initialPitch);

                float yaw = initialYaw + yawDelta * 2.0F;
                float pitch = Mth.clamp(initialPitch + pitchDelta * 2.0F, -89.9F, 89.9F);

                skull.setYRot(yaw);
                skull.setXRot(pitch);

                travelDir = Vec3.directionFromRotation(pitch, yaw);
            }

            skull.setDeltaMovement(travelDir.scale(speed));
            skull.hurtMarked = true;

        }, 1, maxLifetimeTicks + 5);
    }

    private static void executeBlazeBarrage(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                            AmplifierTotals amplifierTotals) {
        //executeCatalystPlaceholder(player, core, amplifierTotals);
        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final float damage = Damager.calculateScaledDamage(player, 5.0F * damageScale);
        final float projectileSpeed = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int projectileInterval = 10;
        final int projectileCount = 3;
        final int[] tick = {0};
        final Vec3[] dirs = new Vec3[projectileCount];
        final Vec3[] locs = new Vec3[projectileCount];
        final ParticleOptions particle1 = resolveParticle(core.primaryParticle());
        final ParticleOptions particle2 = resolveParticle(core.secondaryParticle());
        final ServerLevel level = player.serverLevel();
        final String[] animations = { "punch_right", "punch_left", "sword_rotate" };
        AbilityScheduler.scheduleRepeating(player, () -> {
            if (tick[0] % projectileInterval == 0 && tick[0] < projectileInterval * projectileCount) {
                // Launch projectile
                // - save the location of this projectile into an array
                // - save the direction of this projectile into an array
                dirs[(int) (tick[0] / projectileInterval)] = player.getLookAngle();
                locs[(int) (tick[0] / projectileInterval)] = player.getEyePosition();
                AnimationHelper.playAnimation(player, animations[(int) (tick[0] / projectileInterval)], projectileInterval);
                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BLAZE_SHOOT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
                );
            }
            for (int i = 0; i <= Math.min(tick[0] / projectileInterval, projectileCount); i++) {
                Vec3 particlePos = locs[i];

                 level.sendParticles(
                        particle2,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                    );

                locs[i] = locs[i].add(dirs[i].scale(projectileSpeed));
                
                particlePos = locs[i];

                    level.sendParticles(
                        particle1,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        4,
                        0.05D,
                        0.05D,
                        0.05D,
                        0.0D
                    );

                AABB hitBox = new AABB(locs[i], locs[i]).inflate(projectileSpeed, projectileSpeed, projectileSpeed);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitBox, entity -> entity != player && entity.isAlive())) {
                    spawnBurst(player.serverLevel(), core.primaryParticle(), locs[i], 10);
                    Damager.hurt(player, target, damage, false);
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                }
            }
            tick[0]++;
        }, 1, (int)(200 * rangeScale));
    }

    private static void executeGuardianLaser(ServerPlayer player,
                                         CustomBloodDemonArtSavedData.CoreSettings core,
                                         AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final double maxRange = 20.0D * rangeScale;
        final int durationTicks = CustomBloodDemonArtSavedData.MoveType.GUARDIAN_LASER.durationTicks();

        final ParticleOptions beamParticle = resolveParticle(core.primaryParticle());

        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final float pulseDamage = Damager.calculateScaledDamage(player, 3.0F * damageScale);

        final int[] lockTicks = {0};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            Vec3 eyePos = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            if (look.lengthSqr() < 1.0E-4D) {
                look = new Vec3(0.0D, 0.0D, 1.0D);
            }

            look = look.normalize();

            Vec3 maxEndPos = eyePos.add(look.scale(maxRange));

            BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos,
                maxEndPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));

            Vec3 beamEndPos = maxEndPos;

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                beamEndPos = blockHit.getLocation();
            }

            LivingEntity lockedTarget = findCrosshairTarget(player, maxRange);

            if (lockedTarget != null && lockedTarget.isAlive()) {
                Vec3 targetPos = lockedTarget.getEyePosition();

                BlockHitResult targetBlockHit = level.clip(new ClipContext(
                    eyePos,
                    targetPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                ));

                boolean blocked = targetBlockHit.getType() == HitResult.Type.BLOCK
                    && eyePos.distanceToSqr(targetBlockHit.getLocation()) + 0.01D < eyePos.distanceToSqr(targetPos);

                if (!blocked) {
                    beamEndPos = targetPos;
                }
            }

            Vec3 delta = beamEndPos.subtract(eyePos);
            double length = delta.length();

            if (length <= 0.001D) {
                return;
            }

            Vec3 dir = delta.scale(1.0D / length);

            // Draw the visible beam with particles
            for (double step = 0.0D; step <= length; step += 0.35D) {
                Vec3 p = eyePos.add(dir.scale(step));

                level.sendParticles(
                    beamParticle,
                    p.x,
                    p.y,
                    p.z,
                    3,
                    0.025D,
                    0.025D,
                    0.025D,
                    0.0D
                );
            }

            lockTicks[0]++;

            if (lockTicks[0] == 1 || lockTicks[0] % 20 == 0) {
                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GUARDIAN_ATTACK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.5F
                );
            }

            if (lockedTarget != null && lockedTarget.isAlive() && lockTicks[0] % 10 == 0) {
                Damager.hurt(player, lockedTarget, pulseDamage, false);
                applyTargetPotion(core.primaryPotion(), player, lockedTarget, amplifierTotals);
                applyTargetPotion(core.secondaryPotion(), player, lockedTarget, amplifierTotals);
            }
        }, 1, durationTicks);
    }

    private static LivingEntity findCrosshairTarget(ServerPlayer player, double maxRange) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        AABB searchBox = player.getBoundingBox()
            .expandTowards(look.scale(maxRange))
            .inflate(3.0D);

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity entity : player.level().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            e -> e != player && e.isAlive() && isEnemy(player, e)
        )) {
            Vec3 targetPos = entity.getEyePosition();
            Vec3 toTarget = targetPos.subtract(eyePos);

            double distance = toTarget.length();
            if (distance > maxRange || distance < 0.001D) {
                continue;
            }

            Vec3 dirToTarget = toTarget.normalize();
            double dot = look.dot(dirToTarget);

            // Higher = stricter aim. 0.95 is forgiving but still crosshair-like.
            if (dot < 0.95D) {
                continue;
            }

            BlockHitResult blockHit = player.serverLevel().clip(new ClipContext(
                eyePos,
                targetPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));

            if (blockHit.getType() == HitResult.Type.BLOCK
                && eyePos.distanceToSqr(blockHit.getLocation()) + 0.01D < eyePos.distanceToSqr(targetPos)) {
                continue;
            }

            double aimPenalty = 1.0D - dot;
            double score = distance + aimPenalty * 20.0D;

            if (score < bestScore) {
                bestScore = score;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }

    private static void executeSingularity(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                           AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final float slamDamage = Damager.calculateScaledDamage(player, 8.0F * damageScale);

        final double startRadius = 20.0D + (5.0D * rangeAmp);
        final double endRadius = 3.0D;
        final int durationTicks = Math.max(60, (int) Math.round(80.0D * ampScale(rangeAmp)));
        final Set<Integer> slammedTargets = new HashSet<>();
        final boolean[] processedBlockLift = {false};
        final int[] tick = {0};

        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.75F);

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            tick[0]++;
            double t = Mth.clamp((double) tick[0] / (double) durationTicks, 0.0D, 1.0D);
            double radius = Mth.lerp(t, startRadius, endRadius);
            Vec3 center = player.getEyePosition();

            // One-time heavy pass: lift some blocks into falling entities.
            if (!processedBlockLift[0]) {
                processedBlockLift[0] = true;
                int r = Mth.floor(radius);
                BlockPos centerPos = BlockPos.containing(center);
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            if ((x * x) + (y * y) + (z * z) > (radius * radius)) {
                                continue;
                            }
                            if (level.random.nextFloat() > 0.15F) {
                                continue;
                            }
                            BlockPos pos = centerPos.offset(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
                                continue;
                            }
                            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
                            Vec3 toCenter = center.subtract(falling.position());
                            if (toCenter.lengthSqr() > 1.0E-4D) {
                                falling.setDeltaMovement(toCenter.normalize().scale(0.35D * speedScale));
                            }
                        }
                    }
                }
            }

            // Sphere shell particles.
            for (double angle = 0.0D; angle < Math.PI * 2.0D; angle += Math.PI / 14.0D) {
                double px = center.x + Math.cos(angle) * radius;
                double pz = center.z + Math.sin(angle) * radius;
                level.sendParticles(secondaryParticle, px, center.y, pz, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }

            AABB pullBox = new AABB(center, center).inflate(radius);
            List<net.minecraft.world.entity.Entity> pulled = level.getEntities(player, pullBox,
                entity -> entity != null && entity.isAlive() && !entity.isRemoved());

            for (net.minecraft.world.entity.Entity entity : pulled) {
                Vec3 toCenter = center.subtract(entity.position());
                double dist = Math.max(0.001D, toCenter.length());
                if (dist > radius) {
                    continue;
                }

                double pullStrength = (0.08D + (0.14D * (1.0D - (dist / Math.max(radius, 0.001D))))) * speedScale;
                Vec3 pull = toCenter.scale(1.0D / dist).scale(pullStrength);
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
                entity.hurtMarked = true;

                if (entity instanceof LivingEntity living) {
                    applyTargetPotion(core.primaryPotion(), player, living, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, living, amplifierTotals);

                    if (tick[0] % 2 == 0) {
                        Vec3 from = player.getEyePosition();
                        Vec3 delta = living.getEyePosition().subtract(from);
                        double len = delta.length();
                        if (len > 1.0E-4D) {
                            Vec3 dir = delta.scale(1.0D / len);
                            for (double d = 0.2D; d <= len; d += 0.45D) {
                                Vec3 p = from.add(dir.scale(d));
                                level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                            }
                        }
                    }

                    if (dist <= 5.0D && slammedTargets.add(living.getId())) {
                        Damager.hurt(player, living, slamDamage, true);
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3, false, true));
                        level.playSound(null, living.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.0F, 0.5F);
                    }
                }
            }
        }, 1, durationTicks);
    }


    /*
    
    Dark Star should be similar to Singularity. 
    Instead of doing particles, it should render the models/item/dark_star.json item model with the textures/item/dark_star_texture.png texture, 
    tinted to a specific color. That specific color should be the chat color, using final int tintColor = core.chatColor() & 0xFFFFFF; 
    or something like that to tint the greyscale dark_star_texture executeDarkStar should last 4 seconds (80 ticks), 
    and it should show that dark star model rendered huge, like 10x scale, and it should be tilted, and spinning along its local vertical axis 
    (vertical axis is the y axis), and it should be spinning rapidly along that local vertical axis. The passed in ServerPlayer player should be
    immune to all the effects of executeDarkStar. All entities within the range should be pulled in and continuously pulled in to the center point 
    of that dark star with a pretty large strength. All entities within 10 blocks of that center point should be given the darkness and slowness and 
    mining fatigue and weakness effects, along with the primary and secondary target potion effects (only if they are target effects). 
    Every 5 ticks that dark star center should play the explosion sound effect at 0.5 pitch. 
    When the player uses dark star, it should raycast a laser of primary particle forward up to 60 blocks, and spawn the dark star on the first solid 
    block or entity that the laser hits.
    
    */
    
    private static void executeDarkStar(ServerPlayer player,
                                    CustomBloodDemonArtSavedData.CoreSettings core,
                                    AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final int tintColor = core.chatColor() & 0xFFFFFF;

        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));

        final int durationTicks = 80;
        final double pullRadius = 30.0D + (6.0D * rangeAmp);
        final double effectRadius = 12.0D;

        final float darkStarDamage = Damager.calculateScaledDamage(player, 3.0F * damageScale);
        final int blocksPerTick = 15 + (int)(2 * rangeAmp);
        final double blockLiftRadius = Math.min(20.0D + (4.0D * rangeAmp), pullRadius);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 maxEnd = eye.add(look.scale(60.0D));

        BlockHitResult blockHit = level.clip(new ClipContext(
            eye,
            maxEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level,
            player,
            eye,
            rayEnd,
            new AABB(eye, rayEnd).inflate(1.0D),
            entity -> entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && entity != player
                && entity.isPickable()
        );

        Vec3 center = rayEnd;
        if (entityHit != null) {
            double entityDist = entityHit.getLocation().distanceToSqr(eye);
            double blockDist = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : blockHit.getLocation().distanceToSqr(eye);

            if (entityDist <= blockDist) {
                center = entityHit.getLocation();
            }
        }

        // Laser trace using primary particle.
        double laserLen = center.distanceTo(eye);
        for (double d = 0.0D; d <= laserLen; d += 0.35D) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        //Log.warn("[DarkStar] Laser trace finished for {} at center={} rangeAmp={} speedScale={} damageScale={}",
        //    player.getName().getString(), center, rangeAmp, speedScale, damageScale);

        DarkStarVisualEntity visual = DarkStarVisualEntity.create(level, center, player.getUUID(), tintColor, 2.0F, durationTicks);
        level.addFreshEntity(visual);

        final Vec3 darkStarCenter = center;
        final int[] tick = {0};

        level.playSound(null, BlockPos.containing(darkStarCenter), SoundEvents.BEACON_ACTIVATE,
            SoundSource.PLAYERS, 1.0F, 0.65F);

        AbilityScheduler.scheduleRepeating(player, () -> {
            try {
                if (!player.isAlive() || visual.isRemoved()) {
                    visual.discard();
                    return;
                }

                tick[0]++;

                // Pull a few visible blocks every tick.
                int lifted = 0;
                int r = Mth.floor(blockLiftRadius);
                BlockPos centerPos = BlockPos.containing(darkStarCenter);

                for (int attempts = 0; attempts < 80 && lifted < blocksPerTick; attempts++) {
                    int x = level.random.nextInt(r * 2 + 1) - r;
                    int y = level.random.nextInt(r * 2 + 1) - r;
                    int z = level.random.nextInt(r * 2 + 1) - r;

                    if ((x * x) + (y * y) + (z * z) > blockLiftRadius * blockLiftRadius) {
                        continue;
                    }

                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
                        continue;
                    }

                    Vec3 blockCenter = Vec3.atCenterOf(pos);

                    BlockHitResult sight = level.clip(new ClipContext(
                            darkStarCenter,
                            blockCenter,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            player));

                    if (sight.getType() == HitResult.Type.BLOCK) {
                        BlockPos hitPos = sight.getBlockPos();

                        // Allow the target block itself to be the first thing hit.
                        if (!hitPos.equals(pos)) {
                            continue;
                        }
                    }

                    FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
                    Vec3 toCenter = darkStarCenter.subtract(falling.position());

                    if (toCenter.lengthSqr() > 1.0E-4D) {
                        falling.setDeltaMovement(toCenter.normalize().scale(0.35D * speedScale));
                        falling.hurtMarked = true;
                    }

                    lifted++;
                }
                
                // Destroy a few close visible blocks every tick, biased toward the center.
                int destroyed = 0;
                int destroyAttempts = 50;
                int destroyPerTick = 14 + rangeAmp;
                double destroyRadius = Math.min(7.0D + (2.0D * rangeAmp), blockLiftRadius);

                for (int attempts = 0; attempts < destroyAttempts && destroyed < destroyPerTick; attempts++) {
                    double biasedRadius = destroyRadius * Math.pow(level.random.nextDouble(), 1.8D);
                    double theta = level.random.nextDouble() * Math.PI * 2.0D;
                    double phi = Math.acos(2.0D * level.random.nextDouble() - 1.0D);

                    int x = Mth.floor(Math.sin(phi) * Math.cos(theta) * biasedRadius);
                    int y = Mth.floor(Math.cos(phi) * biasedRadius);
                    int z = Mth.floor(Math.sin(phi) * Math.sin(theta) * biasedRadius);

                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || state.hasBlockEntity()) {
                        continue;
                    }

                    Vec3 blockCenter = Vec3.atCenterOf(pos);

                    BlockHitResult sight = level.clip(new ClipContext(
                        darkStarCenter,
                        blockCenter,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                    ));

                    if (sight.getType() == HitResult.Type.BLOCK && !sight.getBlockPos().equals(pos)) {
                        continue;
                    }

                    level.destroyBlock(pos, true); // false = drops
                    level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                    );
                    destroyed++;
                }

                if (tick[0] % 5 == 0) {
                    level.playSound(null, BlockPos.containing(darkStarCenter), SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 1.5F, 0.5F);
                }

                AABB pullBox = new AABB(darkStarCenter, darkStarCenter).inflate(pullRadius);

                List<Entity> entities = level.getEntities(player, pullBox, entity ->
                    entity != null
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && entity != player
                        && entity.getId() != visual.getId()
                );

                for (Entity entity : entities) {
                    Vec3 toCenter = darkStarCenter.subtract(entity.position());
                    double dist = Math.max(0.001D, toCenter.length());

                    if (dist > pullRadius) {
                        continue;
                    }

                    double proximityBoost = 1.0D - (dist / pullRadius);
                    double pullStrength = (0.28D + (0.42D * proximityBoost)) * speedScale;

                    Vec3 pull = toCenter.normalize().scale(pullStrength);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
                    entity.hurtMarked = true;

                    if (entity instanceof LivingEntity living && dist <= effectRadius) {
                        living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 3, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3, false, true));

                        if (tick[0] % 5 == 0) {
                            Damager.hurt(player, living, darkStarDamage, true);
                        }

                        applyTargetPotion(core.primaryPotion(), player, living, amplifierTotals);
                        applyTargetPotion(core.secondaryPotion(), player, living, amplifierTotals);
                    }
                }

                if (tick[0] >= durationTicks) {
                    visual.discard();
                }
            } catch (Exception e) {
                visual.discard();
                throw e;
            }
        }, 1, durationTicks);
    }

    private static void executeTasteOfImmortality(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                                  AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());
        final int durationTicks = 100;
        final int[] totemCooldown = {0};

        GuardStateHelper.setGuardState(player, 20.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // High resistance + sustain effects during immortality window.
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks + 10, 9, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks + 10, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks + 10, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, durationTicks + 10, 0, false, false));
        applySelfPotion(core.primaryPotion(), player, amplifierTotals);
        applySelfPotion(core.secondaryPotion(), player, amplifierTotals);

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            if (totemCooldown[0] > 0) {
                totemCooldown[0]--;
            }

            double height = Mth.clamp((player.tickCount % durationTicks) / (double) durationTicks, 0.0D, 1.0D)
                * (player.getEyeHeight() + 0.7D);
            double radius = 1.25D;
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0D * i) / 16.0D;
                Vec3 p = player.position().add(Math.cos(a) * radius, height, Math.sin(a) * radius);
                level.sendParticles(i % 2 == 0 ? primaryParticle : secondaryParticle, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }

            // Emergency "totem" rescue whenever health gets critical.
            if (player.getHealth() <= 1.0F && totemCooldown[0] == 0) {
                totemCooldown[0] = 8;
                level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, 0.6D, 0.8D, 0.6D, 0.0D);
                player.setHealth(Math.max(player.getHealth(), 8.0F));
                player.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
            }
        }, 1, durationTicks);

        AbilityScheduler.scheduleOnce(player, () ->
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.5F),
            durationTicks + 1);
    }

    private static void executeGlide(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                     AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions trailParticle = resolveParticle(core.secondaryParticle());
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final int durationTicks = 200 + (40 * rangeAmp);
        final double glideSpeed = 0.9D * speedScale;

        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 1.25F);
        applySelfPotion(core.primaryPotion(), player, amplifierTotals);
        applySelfPotion(core.secondaryPotion(), player, amplifierTotals);

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            player.startFallFlying();
            player.fallDistance = 0.0F;

            Vec3 look = player.getLookAngle();
            if (look.lengthSqr() < 1.0E-4D) {
                look = new Vec3(0.0D, 0.0D, 1.0D);
            }
            look = look.normalize();

            double yVelocity = Mth.clamp(look.y * (0.55D * speedScale), -0.30D, 0.30D);
            MovementHelper.setVelocity(player, look.x * glideSpeed, yVelocity, look.z * glideSpeed);

            Vec3 pos = player.position().add(0.0D, 0.6D, 0.0D);
            level.sendParticles(trailParticle, pos.x, pos.y, pos.z, 6, 0.15D, 0.15D, 0.15D, 0.0D);
        }, 1, durationTicks);
    }

    private static void executeRoar(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                    AmplifierTotals amplifierTotals) {
        ParticleOptions particle = resolveParticle(core.primaryParticle());
        double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));

        float damage = Damager.calculateScaledDamage(player, 4.0F * damageScale);
        GuardStateHelper.setGuardState(player, 5*ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE)));
        
        ServerLevel level = player.serverLevel();

        AnimationHelper.playAnimation(player, "beast2", 20);

        // Yaw-based forward direction so the cone stays horizontal
        float yaw = (float) Math.toRadians(-player.getYRot());
        Vec3 forward = new Vec3(Math.sin(yaw), 0.0D, Math.cos(yaw)).normalize();

        // Right vector for drawing circular cone rings
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();

        Vec3 eyePos = player.getEyePosition();

        double maxDistance = 14.0D * rangeScale;
        double baseRadius = 0.45D;
        double radiusGrowth = 1D * rangeScale;

        // Forward cone shockwave particles
        for (double dist = 1.0D; dist <= maxDistance; dist += 0.6D) {
            final double distance = dist;
            AbilityScheduler.scheduleOnce(player, () -> {
                Vec3 center = eyePos.add(forward.scale(distance));
                double radius = baseRadius + distance * radiusGrowth;

                for (double angle = 0.0D; angle < Math.PI * 2.0D; angle += Math.max(0.15D, 1.0D / radius)) {
                    Vec3 particlePos = center
                        .add(right.scale(radius * Math.cos(angle)))
                        .add(0.0D, radius * Math.sin(angle), 0.0D);

                    level.sendParticles(
                        particle,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                    );
                }
            }, (int)((distance-1) * 3));
        }

        // Cone-shaped hit detection
        AABB hitBox = player.getBoundingBox().expandTowards(forward.scale(maxDistance)).inflate(3.5D * rangeScale);

        for (LivingEntity target : player.level().getEntitiesOfClass(
            LivingEntity.class,
            hitBox,
            entity -> entity != player && entity.isAlive()
        )) {
            Vec3 toTarget = target.position().subtract(player.position());
            Vec3 flatToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);

            if (flatToTarget.lengthSqr() < 0.001D) {
                continue;
            }

            double distance = flatToTarget.length();
            if (distance > maxDistance) {
                continue;
            }

            Vec3 direction = flatToTarget.normalize();
            double forwardDot = direction.dot(forward);

            // Higher = narrower cone. 0.55 is roughly a wide forward cone.
            if (forwardDot < 0.55D) {
                continue;
            }

            Damager.hurt(player, target, damage, true);

            MovementHelper.addVelocity(
                target,
                direction.x * 0.8D,
                0.35D,
                direction.z * 0.8D
            );

            applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
        }

        applySelfPotion(core.primaryPotion(), player, amplifierTotals);

        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.ENDER_DRAGON_GROWL,
            SoundSource.PLAYERS,
            1.0F,
            1.2F
        );
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.WARDEN_ROAR,
            SoundSource.PLAYERS,
            1.0F,
            1.2F
        );

        AbilityScheduler.scheduleOnce(player, () ->
            AnimationHelper.playAnimation(player, "cancel", 1),
            31);
    }

    private static void executeFlowerDance(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                           AmplifierTotals amplifierTotals) {
        // A wavelike dash with many attacks
        // Set step-up height to 3 blocks
        // Movement in a sinusoidal path forward
        // Alternate between punch_right, punch_left, kick_right, kick_left, sword_rotate animations
        // It should do one attack with one animation at each apex of the sine wave
        // Each time it does an attack with the animation, it should spawn an after image entity doing that same animation

        final ServerLevel level = player.serverLevel();

        final double speedScale =
            ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final double damageScale =
            ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));

        final double rangeScale =
            ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));

        final String[] animations = {
            "punch_right",
            "punch_left",
            "kick_right",
            "kick_left",
            "sword_rotate"
        };

        final int durationTicks = 80;
        final int attackInterval = 8; // attack at each sine apex

        final double forwardSpeed = 1.15D * speedScale;
        final double sideAmplitude = 0.9D * rangeScale;

        final float attackDamage =
            Damager.calculateScaledDamage(player, (float)(4.0F * damageScale));

        final int[] attackIndex = {0};

        MovementHelper.setStepHeight(player, 3);

        AnimationHelper.playAnimation(
            player,
            "kimetsunoyaibamultiplayer:flower_dance",
            durationTicks
        );

        int[] tick = { 0 };

        AbilityScheduler.scheduleRepeating(player, () -> {

            if (!player.isAlive()) {
                return;
            }

            float yawRad = (float)Math.toRadians(-player.getYRot());

            Vec3 forward = new Vec3(
                Math.sin(yawRad),
                0,
                Math.cos(yawRad)
            ).normalize();

            Vec3 right = new Vec3(
                -forward.z,
                0,
                forward.x
            ).normalize();

            double phase =
                (tick[0] / (double)attackInterval) * Math.PI;

            double lateral =
                Math.sin(phase) * sideAmplitude;

            Vec3 velocity =
                forward.scale(forwardSpeed)
                    .add(right.scale(lateral * 0.15D));

            MovementHelper.setVelocity(
                player,
                velocity.x,
                Math.max(player.getDeltaMovement().y, -0.05D),
                velocity.z
            );

            // Attack at every apex
            if (tick[0] % attackInterval == 0) {

                String anim = animations[attackIndex[0] % animations.length];

                attackIndex[0]++;

                AnimationHelper.playAnimation(
                    player,
                    anim,
                    10
                );

                // Spawn after image
                com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity afterImage =
                    new com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity(
                        level,
                        player,
                        40,
                        player.position(),
                        player.getYRot()
                    );

                afterImage.startVisibleWithFade();
                afterImage.setSwinging(true);

                playEntityAnimationOnLayer(
                    afterImage,
                    anim,
                    10,
                    3.0f,
                    4000
                );

                level.addFreshEntity(afterImage);

                // Damage sweep
                Vec3 attackCenter =
                    player.position()
                        .add(forward.scale(2.5D));

                AABB hitbox =
                    new AABB(
                        attackCenter.x - 2.0D,
                        attackCenter.y - 1.5D,
                        attackCenter.z - 2.0D,
                        attackCenter.x + 2.0D,
                        attackCenter.y + 1.5D,
                        attackCenter.z + 2.0D
                    );

                for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class,
                    hitbox,
                    e -> e != player
                        && e.isAlive()
                        && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity)
                )) {

                    Damager.hurt(
                        player,
                        target,
                        attackDamage,
                        true
                    );

                    applyTargetPotion(
                        core.primaryPotion(),
                        player,
                        target,
                        amplifierTotals
                    );

                    Vec3 knockback =
                        target.position()
                            .subtract(player.position());

                    if (knockback.lengthSqr() > 0.001D) {
                        knockback = knockback.normalize();

                        MovementHelper.addVelocity(
                            target,
                            knockback.x * 0.4D,
                            0.2D,
                            knockback.z * 0.4D
                        );
                    }
                }
            }
            tick[0]++;
        }, 1, durationTicks);

        AbilityScheduler.scheduleOnce(player, () -> {
            MovementHelper.resetStepHeight(player);
        }, durationTicks + 1);
    }

    private static void executeSpineBurst(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                          AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final int spineCount = Math.max(20, 28 + (rangeAmp * 6));
        final float spineDamage = Damager.calculateScaledDamage(player, 4.5F * damageScale);
        final double spineSpeed = 0.65D * speedScale;
        final double spawnRadius = 0.7D;

        final int tintColor = core.chatColor() & 0xFFFFFF;
        final Vec3 center = player.getEyePosition().add(0.0D, -0.2D, 0.0D);

        AnimationHelper.playAnimation(player, "speed_attack_punch", 10);
        level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.15F);

        ParticleOptions burstParticle = resolveParticle(core.primaryParticle());
        level.sendParticles(burstParticle, center.x, center.y, center.z, 20, 0.2D, 0.2D, 0.2D, 0.01D);

        for (int i = 0; i < spineCount; i++) {
            // Fibonacci sphere sampling keeps coverage even across all directions.
            double t = i + 0.5D;
            double y = 1.0D - (2.0D * t / spineCount);
            double radial = Math.sqrt(Math.max(0.0D, 1.0D - (y * y)));
            double theta = Math.PI * (3.0D - Math.sqrt(5.0D)) * i;

            Vec3 direction = new Vec3(Math.cos(theta) * radial, y, Math.sin(theta) * radial).normalize();
            Vec3 spawnPos = center.add(direction.scale(spawnRadius));
            Vec3 velocity = direction.scale(spineSpeed);

            SpineEntity spine = SpineEntity.create(level, player, spawnPos, velocity, spineDamage, tintColor);
            level.addFreshEntity(spine);
        }
    }

    private static void executeMidasTouch(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                          AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final int durationAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DURATION);

        final double markRange = 5.0D * rangeScale;
        final int markDurationTicks = Math.max(100, (int) Math.round(300.0D * ampScale(durationAmp)));
        final float bonusMultiplier = Math.max(1.1F, 1.15F + (0.20F * damageScale));
        final int tintColor = core.chatColor() & 0xFFFFFF;

        LivingEntity target = findCrosshairTarget(player, markRange);
        if (target == null || !target.isAlive()) {
            player.displayClientMessage(Component.literal("No valid target in range.")
                .withStyle(ChatFormatting.RED), true);
            return;
        }

        AnimationHelper.playAnimation(player, "punch_right", 10);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.75F);

        String teamName = "knymp_midas_" + String.format("%06x", tintColor);
        String scoreHolder = target.getScoreboardName();
        Scoreboard scoreboard = level.getScoreboard();

        PlayerTeam previousTeam = scoreboard.getPlayersTeam(scoreHolder);
        PlayerTeam markTeam = scoreboard.getPlayerTeam(teamName);
        if (markTeam == null) {
            markTeam = scoreboard.addPlayerTeam(teamName);
        }
        markTeam.setColor(nearestFormattingForColor(tintColor));
        scoreboard.addPlayerToTeam(scoreHolder, markTeam);

        CompoundTag tag = target.getPersistentData();
        tag.putUUID(NBT_MIDAS_OWNER, player.getUUID());
        tag.putLong(NBT_MIDAS_EXPIRES, level.getGameTime() + markDurationTicks);
        tag.putFloat(NBT_MIDAS_BONUS_MULTIPLIER, bonusMultiplier);
        tag.putString(NBT_MIDAS_MARK_TEAM, teamName);
        if (previousTeam != null) {
            tag.putString(NBT_MIDAS_PREVIOUS_TEAM, previousTeam.getName());
        } else {
            tag.remove(NBT_MIDAS_PREVIOUS_TEAM);
        }

        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, markDurationTicks + 5, 0, false, false, true));

        ParticleOptions particle = resolveParticle(core.primaryParticle());
        level.sendParticles(
            particle,
            target.getX(),
            target.getY() + (target.getBbHeight() * 0.6D),
            target.getZ(),
            24,
            0.25D,
            0.35D,
            0.25D,
            0.01D
        );

        UUID ownerId = player.getUUID();
        AbilityScheduler.scheduleOnce(player, () -> clearMidasMark(level, target, ownerId), markDurationTicks + 1);
    }

    private static void executeDefend(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                      AmplifierTotals amplifierTotals) {
        // Enter an extremely defensive stance and block incoming attacks
        // Play the guard animation for 40 ticks
        // Grant the player all self effects
        // Set the guard state to 60

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int defenseAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE);

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final int lifetimeTicks = (int)(40 * rangeScale);

        AnimationHelper.playAnimation(player, "guard", lifetimeTicks);

        GuardStateHelper.setGuardState(player, (float)(50*defenseAmp));

        AbilityScheduler.scheduleRepeating(player, new Runnable() {
            @Override
            public void run() {
                AnimationHelper.playAnimation(player, "guard", lifetimeTicks);
            }
        }, 10, lifetimeTicks);
        
    }

    private static void executeVindicatorsCleave(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
            AmplifierTotals amplifierTotals) {
         final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int defenseAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE);

        final ParticleOptions particle1 = resolveParticle(core.primaryParticle());
        final ParticleOptions particle2 = resolveParticle(core.secondaryParticle());

        float damage = DamageCalculator.calculateScaledDamage(player, 9 * damageScale);
        GuardStateHelper.setGuardState(player, 7*defenseAmp);

       Vec3 launch = VindicatorsBane.getTargetDirection(player).scale(0.55D * speedScale);
       playAnimation(player, "sword_to_upper", 10);
       ServerLevel level = player.serverLevel();
       VindicatorsBane.playSplitterLaunchSound(level, player);
        MovementHelper.setVelocity(player, player.getDeltaMovement().add(launch.x, 0.85D, launch.z));
        player.hurtMarked = true;

        AbilityScheduler.scheduleOnce(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            Vec3 dive = VindicatorsBane.getTargetDirection(player).scale(0.7D * speedScale);
            MovementHelper.setVelocity(player, dive.x, -1, dive.z);
            player.hurtMarked = true;
            playAnimation(player, "sword_overhead", 12);
            AbilityScheduler.scheduleOnce(player, () -> {
                if (!player.isAlive()) {
                    return;
                }

                VindicatorsBane.playSplitterImpactSound(level, player);
                level.sendParticles(particle1, player.getX(), player.getY(0.1D), player.getZ(), 12, 0.45D, 0.15D, 0.45D, 0.01D);
                level.sendParticles(particle2, player.getX(), player.getY(0.2D), player.getZ(), 20, 0.8D, 0.3D, 0.8D, 0.03D);

                
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(3.0D * rangeScale, 1.5D * rangeScale, 3.0D * rangeScale),
                    living -> living != player && living.isAlive() && player.distanceToSqr(living) <= 9.0D * rangeScale)) {
                    if (Damager.hurt(player, target, damage)) {
                        //BleedingHandler.applyOrRefreshBleeding(target, 20 * 10, 1);
                        applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                        applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                    }
                }
            }, 10);
        }, 10);
    }

    private static void executeWhiteout(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                        AmplifierTotals amplifierTotals) {
        executeCatalystPlaceholder(player, core, amplifierTotals);
    }

    private static void executeExplode(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                       AmplifierTotals amplifierTotals) {
        executeCatalystPlaceholder(player, core, amplifierTotals);
    }

    private static boolean isValidGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return state.isCollisionShapeFullBlock(level, pos) // solid
            || state.getFluidState().is(FluidTags.WATER);  // water
    }

    private static BlockPos findValidGroundSpawn(ServerLevel level, BlockPos start, int limit) {
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

    private static void executeFangsOfTheEarth(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                               AmplifierTotals amplifierTotals) {
        //executeCatalystPlaceholder(player, core, amplifierTotals);

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float)ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final float damage = Damager.calculateScaledDamage(player, 5*damageScale);
        ServerLevel level = player.serverLevel();
        
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        final Vec3[] velocity = {look.normalize().scale(1)};
        final Vec3[] currentPos = { start };

        final ParticleOptions particle1 = resolveParticle(core.primaryParticle());
        final ParticleOptions particle2 = resolveParticle(core.secondaryParticle());
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.0F);

        int lifetimeTicks = (int)(20 * rangeScale);

        int tickInterval = 2;

        AbilityScheduler.scheduleRepeating(player, new Runnable() {
            private boolean finished = false;
            private int ticks = 0;

            @Override
            public void run() {
                if (finished || !(player.level() instanceof ServerLevel activeLevel) || !player.isAlive()) {
                    finished = true;
                    return;
                }

                //velocity[0] = velocity[0].add(0.0D, -0.01D, 0.0D);
                Vec3 nextPos = currentPos[0].add(velocity[0]);
                BlockHitResult hit = activeLevel.clip(new ClipContext(currentPos[0], nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

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

                    int start = level.random.nextInt(4);

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
                        BlockPos foundPos = findValidGroundSpawn(level, candidateBase, limit);
                        if (foundPos == null) {
                            continue;
                        }

                        Vec3 spawnPos = new Vec3(
                            foundPos.getX() + 0.5D,
                            foundPos.getY(),
                            foundPos.getZ() + 0.5D
                        );

                        // if (hasSwampHandAt(sourceLevel, spawnPos)) {
                        //     continue;
                        // }

                        // level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                        //         SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);
                        
                        // SPAWN EVOKER FANG (I believe evoker fangs automatically play the evoker fangs attack sounds)
                        EvokerFangs fangs = new EvokerFangs(
                            level,
                            spawnPos.x,
                            spawnPos.y,
                            spawnPos.z,
                            player.getYRot(),
                            0,
                            player
                        );

                        level.addFreshEntity(fangs);

                        level.sendParticles(
                            particle1,
                            spawnPos.x,
                            spawnPos.y+1.3f,
                            spawnPos.z,
                            7,
                            0.0D,
                            0.08D,
                            0.0D,
                            0.0D
                        );

                        spawnRing(level, core.secondaryParticle(), spawnPos.add(0, 0.5f, 0), 0.5, 9);

                        AABB damageBox = new AABB(
                            spawnPos.x - 1.0D, spawnPos.y, spawnPos.z - 1.0D,
                            spawnPos.x + 1.0D, spawnPos.y + 1.5D, spawnPos.z + 1.0D
                        );

                        for (LivingEntity target : level.getEntitiesOfClass(
                            LivingEntity.class,
                            damageBox,
                            entity -> entity != player && entity.isAlive()
                        )) {
                            Damager.hurt(player, target, damage, true);
                            applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                            applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                        }

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

        AnimationHelper.playAnimation(player, "sword_overhead", 10);
    }

    private static void executeVexSwarm(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                        AmplifierTotals amplifierTotals) {
        // TODO: Play the "summon" animation from kimetsunoyaibamultiplayer and play the evoker spell sound effect
        // - While the summon animation is playing on the player, it should also do a spiral of primary particle around the player
        // Summon a cloud of vex entities (they should have their attack damage multiplied by the DAMAGE amplifier from amplifier totals)
        // The cloud of vex should be friendly to the player
        // The cloud of vex should automatically aggro on and target all nearby enemy entities
        // The cloud of vex should persist for 30 seconds
        // Find the point 15 blocks (multiplied by range amplifier) in front of the player
        //  - All enemy (use isEnemy function) entities within 20 blocks of that point (multiplied by range amplifier) should be targeted by the vex
        //  - Any vex entities that are more than 40 blocks away from the player (multiplied by range amplifier) should pathfind back towards the player
        //  - Any vex entities that are more than 80 blocks away from the player (multiplied by range amplifier) should get teleported to within 10 blocks of the player
        //  - Vex entities can die, please no null pointer errors
        // Upon spawning, vex entities should be given the same strength potion effect as the player (same level of strength)
        // Also, vex entities should be given an armor value according to the DEFENSE amplifier from amplifierTotals
        // the vex entities should leave a trail of secondaryParticle as they fly around
        // The vex entities should have a fly speed / movement speed / walk speed that gets multiplied by the SPEED amplifier
    
        ServerLevel level = player.serverLevel();

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int defenseAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE);

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final int lifetimeTicks = 30 * 20;
        final int summonTicks = 20;

        final double targetPointDistance = 15.0D * rangeScale;
        final double enemySearchRadius = 20.0D * rangeScale;
        final double returnDistance = 40.0D * rangeScale;
        final double teleportDistance = 80.0D * rangeScale;
        final double teleportRadius = 10.0D;

        final int vexCount = Math.max(3, 5 + amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));

        AnimationHelper.playAnimation(player, "summon", summonTicks);

        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.EVOKER_PREPARE_SUMMON,
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );

        java.util.List<Vex> vexes = new java.util.ArrayList<>();

        for (int i = 0; i < vexCount; i++) {
            double angle = (Math.PI * 2.0D * i) / vexCount;

            Vec3 spawnOffset = new Vec3(
                Math.cos(angle) * 1.5D,
                1.0D + level.random.nextDouble() * 0.8D,
                Math.sin(angle) * 1.5D
            );

            Vex vex = EntityType.VEX.create(level);
            if (vex == null) {
                continue;
            }

            Vec3 spawnPos = player.position().add(spawnOffset);

            vex.moveTo(
                spawnPos.x,
                spawnPos.y,
                spawnPos.z,
                player.getYRot(),
                player.getXRot()
            );

            //vex.setOwner(player);
            vex.setBoundOrigin(player.blockPosition());
            vex.setLimitedLife(lifetimeTicks);

            AttributeInstance attackDamage = vex.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.setBaseValue(attackDamage.getBaseValue() * damageScale);
            }

            AttributeInstance flyingSpeed = vex.getAttribute(Attributes.FLYING_SPEED);
            if (flyingSpeed != null) {
                flyingSpeed.setBaseValue(flyingSpeed.getBaseValue() * speedScale);
            }

            AttributeInstance movementSpeed = vex.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(movementSpeed.getBaseValue() * speedScale);
            }

            AttributeInstance armor = vex.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(armor.getBaseValue() + defenseAmp * 2.0D);
            }

            MobEffectInstance playerStrength = player.getEffect(MobEffects.DAMAGE_BOOST);
            if (playerStrength != null) {
                vex.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    lifetimeTicks,
                    playerStrength.getAmplifier(),
                    playerStrength.isAmbient(),
                    playerStrength.isVisible(),
                    playerStrength.showIcon()
                ));
            }

            level.addFreshEntity(vex);
            vexes.add(vex);
        }

        final int[] ticks = {0};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                for (Vex vex : vexes) {
                    if (vex != null && vex.isAlive()) {
                        vex.discard();
                    }
                }
                return;
            }

            ticks[0]++;

            Vec3 playerPos = player.position();
            Vec3 look = player.getLookAngle();

            if (look.lengthSqr() < 1.0E-4D) {
                look = new Vec3(0.0D, 0.0D, 1.0D);
            }

            Vec3 forward = look.normalize();
            Vec3 targetPoint = player.getEyePosition().add(forward.scale(targetPointDistance));

            // Spiral particles during summon animation
            if (ticks[0] <= summonTicks) {
                double t = ticks[0] * 0.45D;

                for (int i = 0; i < 3; i++) {
                    double angle = t + i * ((Math.PI * 2.0D) / 3.0D);
                    double radius = 1.0D + 0.04D * ticks[0];
                    double y = 0.2D + ticks[0] * 0.08D;

                    Vec3 p = player.position().add(
                        Math.cos(angle) * radius,
                        y,
                        Math.sin(angle) * radius
                    );

                    level.sendParticles(
                        primaryParticle,
                        p.x,
                        p.y,
                        p.z,
                        2,
                        0.03D,
                        0.03D,
                        0.03D,
                        0.0D
                    );
                }
            }

            AABB enemySearchBox = new AABB(targetPoint, targetPoint).inflate(enemySearchRadius);

            java.util.List<LivingEntity> nearbyEnemies = level.getEntitiesOfClass(
                LivingEntity.class,
                enemySearchBox,
                entity -> entity.isAlive() && isEnemy(player, entity)
            );

            vexes.removeIf(vex -> vex == null || !vex.isAlive());

            for (Vex vex : vexes) {
                if (vex == null || !vex.isAlive()) {
                    continue;
                }

                Vec3 vexPos = vex.position();
                double distToPlayerSqr = vex.distanceToSqr(player);

                level.sendParticles(
                    secondaryParticle,
                    vexPos.x,
                    vexPos.y + 0.25D,
                    vexPos.z,
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.0D
                );

                if (distToPlayerSqr > teleportDistance * teleportDistance) {
                    double angle = level.random.nextDouble() * Math.PI * 2.0D;
                    double distance = 2.0D + level.random.nextDouble() * teleportRadius;

                    Vec3 teleportPos = player.position().add(
                        Math.cos(angle) * distance,
                        1.0D + level.random.nextDouble() * 2.0D,
                        Math.sin(angle) * distance
                    );

                    vex.teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
                    vex.setBoundOrigin(player.blockPosition());
                    vex.setTarget(null);
                    continue;
                }

                if (distToPlayerSqr > returnDistance * returnDistance) {
                    vex.setTarget(null);
                    vex.setBoundOrigin(player.blockPosition());

                    Vec3 returnPos = player.getEyePosition().add(
                        (level.random.nextDouble() - 0.5D) * 4.0D,
                        level.random.nextDouble() * 2.0D,
                        (level.random.nextDouble() - 0.5D) * 4.0D
                    );

                    vex.getMoveControl().setWantedPosition(
                        returnPos.x,
                        returnPos.y,
                        returnPos.z,
                        1.2D * speedScale
                    );

                    continue;
                }

                LivingEntity currentTarget = vex.getTarget();

                if (currentTarget != null && (!currentTarget.isAlive() || !isEnemy(player, currentTarget))) {
                    vex.setTarget(null);
                    currentTarget = null;
                }

                if (currentTarget == null && !nearbyEnemies.isEmpty()) {
                    LivingEntity nearest = null;
                    double nearestDistSqr = Double.MAX_VALUE;

                    for (LivingEntity enemy : nearbyEnemies) {
                        if (enemy == null || !enemy.isAlive()) {
                            continue;
                        }

                        double distSqr = enemy.distanceToSqr(vex);
                        if (distSqr < nearestDistSqr) {
                            nearestDistSqr = distSqr;
                            nearest = enemy;
                        }
                    }

                    if (nearest != null) {
                        vex.setTarget(nearest);
                    }
                }
            }

            if (ticks[0] >= lifetimeTicks) {
                for (Vex vex : vexes) {
                    if (vex != null && vex.isAlive()) {
                        vex.discard();
                    }
                }
                vexes.clear();
            }
        }, 1, lifetimeTicks);
    }

    private static void executePrison(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                      AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int durationAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DURATION);

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final double searchRange = 8.0D * rangeScale;
        final double prisonRadius = 2.2D + (0.35D * amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final int lifetimeTicks = Math.max(80, (int) Math.round(160.0D * ampScale(durationAmp)));
        final int damagePulseInterval = Math.max(8, 18 - amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final float pulseDamage = Damager.calculateScaledDamage(player, 2.0F * damageScale);

        LivingEntity initialTarget = findCrosshairTarget(player, searchRange);
        Vec3 center = initialTarget != null
            ? initialTarget.position().add(0.0D, initialTarget.getBbHeight() * 0.5D, 0.0D)
            : player.getEyePosition().add(player.getLookAngle().normalize().scale(searchRange));

        final Vec3 prisonCenter = center;

        AnimationHelper.playAnimation(player, "sword_rotate", 12);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.25F);

        final int[] tick = {0};
        AbilityScheduler.scheduleRepeating(player, () -> {
            tick[0]++;

            // Render the prison shell.
            for (int a = 0; a < 24; a++) {
                double theta = (Math.PI * 2.0D * a) / 24.0D;
                for (int h = -3; h <= 3; h++) {
                    double y = (h / 3.0D) * prisonRadius;
                    double ringRadius = Math.sqrt(Math.max(0.0D, (prisonRadius * prisonRadius) - (y * y)));
                    Vec3 p = prisonCenter.add(Math.cos(theta) * ringRadius, y, Math.sin(theta) * ringRadius);
                    level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                }
            }

            // Center vortex effect.
            level.sendParticles(
                secondaryParticle,
                prisonCenter.x,
                prisonCenter.y,
                prisonCenter.z,
                8,
                0.18D,
                0.18D,
                0.18D,
                0.01D
            );

            AABB prisonBox = new AABB(prisonCenter, prisonCenter).inflate(prisonRadius + 0.6D);
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                prisonBox,
                e -> e != player && e.isAlive() && isEnemy(player, e)
            )) {
                Vec3 desired = prisonCenter.add(0.0D, 0.25D + (0.12D * Math.sin((tick[0] + target.getId()) * 0.25D)), 0.0D);
                Vec3 pull = desired.subtract(target.position()).scale(0.30D + (0.08D * speedScale));

                target.setDeltaMovement(target.getDeltaMovement().scale(0.2D).add(pull));
                target.hurtMarked = true;
                target.fallDistance = 0.0F;

                if (tick[0] % damagePulseInterval == 0) {
                    Damager.hurt(player, target, pulseDamage, true);
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                }
            }
        }, 1, lifetimeTicks);
    }

    private static void executeSonicShriek(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
            AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final double beamRange = 20.0D + (5.0D * rangeAmp);
        final float beamDamage = Damager.calculateScaledDamage(player, 10.0F * damageScale);
        final double beamRadius = 1.25D;

        AnimationHelper.playAnimation(player, "kimetsunoyaibamultiplayer:sonic_shriek", 20);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        AbilityScheduler.scheduleOnce(player, () -> AnimationHelper.playAnimation(player, "cancel", 1), 20);

        final int[] tick = {0};
        final boolean[] fired = {false};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            tick[0]++;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();

            if (!fired[0] && tick[0] >= 15) {
                fired[0] = true;
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);

                for (double d = 0.8D; d <= beamRange; d += 0.6D) {
                    Vec3 p = eye.add(look.scale(d));
                    level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
                }

                Set<Integer> hitIds = new HashSet<>();
                List<LivingEntity> targets = new ArrayList<>();
                for (double d = 0.5D; d <= beamRange; d += 0.75D) {
                    Vec3 p = eye.add(look.scale(d));
                    AABB sample = new AABB(p, p).inflate(beamRadius);
                    for (LivingEntity target : level.getEntitiesOfClass(
                        LivingEntity.class,
                        sample,
                        e -> e != player && e.isAlive()
                    )) {
                        if (hitIds.add(target.getId())) {
                            targets.add(target);
                        }
                    }
                }

                for (LivingEntity target : targets) {
                    Damager.hurt(player, target, beamDamage, true);
                    level.playSound(
                        null,
                        target.getX(),
                        target.getY() + (target.getBbHeight() * 0.5D),
                        target.getZ(),
                        SoundEvents.WARDEN_ATTACK_IMPACT,
                        SoundSource.HOSTILE,
                        0.8F,
                        1.0F
                    );
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                    MovementHelper.addVelocity(target, look.x * (0.7D * speedScale), 0.12D, look.z * (0.7D * speedScale));
                }
            }
        }, 1, 22);
    }

    private static void executeNightTerror(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                           AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final int lifetimeTicks = Math.max(120, (int) Math.round(120.0D * rangeScale));
        final float swoopDamage = Damager.calculateScaledDamage(player, 6.0F * damageScale);
        final double orbitRadius = 3.0D + (1.5D * rangeScale);
        final double glideSpeed = 0.7D * speedScale;

        final List<com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity> afterImages = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            float yaw = player.getYRot() + (i * 90.0F);
            com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity image =
                new com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity(level, player, lifetimeTicks + 30, player.position(), yaw);
            image.startVisibleWithFade();
            level.addFreshEntity(image);
            afterImages.add(image);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.8F);
        final int[] tick = {0};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            tick[0]++;
            player.startFallFlying();
            player.fallDistance = 0.0F;

            Vec3 look = player.getLookAngle().normalize();
            double yVelocity = Mth.clamp(look.y * (0.55D * speedScale), -0.28D, 0.28D);
            MovementHelper.setVelocity(player, look.x * glideSpeed, yVelocity, look.z * glideSpeed);

            for (double a = 0.0D; a < Math.PI * 2.0D; a += Math.PI / 4.0D) {
                Vec3 p = player.position().add(Math.cos(a) * 1.8D, 0.5D, Math.sin(a) * 1.8D);
                level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }

            for (int i = 0; i < afterImages.size(); i++) {
                com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity image = afterImages.get(i);
                if (image == null || !image.isAlive()) {
                    continue;
                }

                double angle = (tick[0] * 0.16D * speedScale) + (i * (Math.PI / 2.0D));
                Vec3 orbitCenter = player.position().add(0.0D, 6.0D, 0.0D);
                Vec3 orbitTarget = orbitCenter.add(Math.cos(angle) * orbitRadius, Math.sin(angle * 0.5D) * 1.5D, Math.sin(angle) * orbitRadius);

                LivingEntity enemy = null;
                if (tick[0] % 18 == 0) {
                    AABB seek = image.getBoundingBox().inflate(8.0D * rangeScale);
                    double nearest = Double.MAX_VALUE;
                    for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, seek, e -> e != player && e.isAlive() && isEnemy(player, e))) {
                        double ds = candidate.distanceToSqr(image);
                        if (ds < nearest) {
                            nearest = ds;
                            enemy = candidate;
                        }
                    }
                }

                Vec3 moveTarget = orbitTarget;
                if (enemy != null) {
                    moveTarget = enemy.getEyePosition();
                }

                Vec3 dir = moveTarget.subtract(image.position());
                if (dir.lengthSqr() > 1.0E-4D) {
                    MovementHelper.setVelocity(image, dir.normalize().scale(0.65D * speedScale));
                    MovementHelper.lookInDirection(image, dir);
                }

                AABB hit = image.getBoundingBox().inflate(0.9D);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hit, e -> e != player && e.isAlive() && isEnemy(player, e))) {
                    Damager.hurt(player, target, swoopDamage, false);
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                    applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                    Vec3 away = target.position().subtract(image.position()).normalize();
                    MovementHelper.addVelocity(target, away.x * 0.55D, 0.18D, away.z * 0.55D);
                }
            }

            if (tick[0] >= lifetimeTicks) {
                for (com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity image : afterImages) {
                    if (image != null && image.isAlive()) {
                        image.discard();
                    }
                }
            }
        }, 1, lifetimeTicks + 5);
    }

    private static void executeInfernalSpin(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                            AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final double radius = 20.0D * rangeScale;
        final float pulseDamage = Damager.calculateScaledDamage(player, 3.5F * damageScale);
        final int lifetimeTicks = 60;
        final int[] tick = {0};

        AnimationHelper.playAnimation(player, "kimetsunoyaibamultiplayer:spin_attack", lifetimeTicks);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.25F);

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }

            tick[0]++;
            double yaw = player.getYRot() + (float) (26.0D * speedScale);
            MovementHelper.setRotation(player, (float) yaw, player.getXRot());
            MovementHelper.setVelocity(player, player.getDeltaMovement().x * 0.45D, Math.max(player.getDeltaMovement().y, -0.06D), player.getDeltaMovement().z * 0.45D);

            Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
            for (double r = 1.0D; r <= Math.min(radius, 10.0D + tick[0] * 0.22D * speedScale); r += 1.0D) {
                double angle = (tick[0] * 0.45D * speedScale) + (r * 0.65D);
                Vec3 p1 = center.add(Math.cos(angle) * r, (r * 0.06D), Math.sin(angle) * r);
                Vec3 p2 = center.add(Math.cos(angle + Math.PI) * r, (r * 0.06D), Math.sin(angle + Math.PI) * r);
                level.sendParticles(primaryParticle, p1.x, p1.y, p1.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
                level.sendParticles(secondaryParticle, p2.x, p2.y, p2.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }

            AABB hitBox = new AABB(player.position(), player.position()).inflate(radius, 2.0D, radius);
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                hitBox,
                e -> e != player
                    && e.isAlive()
                    && !(e instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity)
                    && Math.abs(e.getY() - player.getY()) <= 2.0D
            )) {
                Damager.hurt(player, target, pulseDamage, true);
                applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                Vec3 dir = target.position().subtract(player.position());
                if (dir.lengthSqr() > 1.0E-4D) {
                    Vec3 kb = dir.normalize().scale(0.4D + (0.25D * speedScale));
                    MovementHelper.addVelocity(target, kb.x, 0.18D, kb.z);
                }
            }
        }, 1, lifetimeTicks);
    }

    private static void executeFlytrap(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                       AmplifierTotals amplifierTotals) {
        // Place a flower that will attack those who step on it
    }

    private static void executeGravePulse(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
            AmplifierTotals amplifierTotals) {
        
        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        final int defenseAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE);

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final float damage = Damager.calculateScaledDamage(player, 9 * damageScale);

        final int lifetimeTicks = (int)(20 * rangeScale);

        AnimationHelper.playAnimation(player, "sword_overhead", 20);

        GuardStateHelper.setGuardState(player, (float)(8*defenseAmp));

        final int[] tick = {0};

        final Vec3 center = player.position();

        final float step = (float)(0.5f * speedScale);

        final ServerLevel level = player.serverLevel();

        level.playSound(
                        null,
                        player.getOnPos(),
                        SoundEvents.WITHER_DEATH,
                        SoundSource.HOSTILE,
                        1.0F,
                        0.7F
                    );

        AbilityScheduler.scheduleRepeating(player, () -> {

            float r = step * tick[0];
            for (float a = 0; a < Math.PI * 2; a += (Math.PI * 2) / (tick[0] * 2)) {
                
                Vec3 loc = center.add(r * Math.cos(a), 0, r * Math.sin(a));
                BlockPos.MutableBlockPos bloc = new BlockPos((int) loc.x, (int) loc.y, (int) loc.z).mutable();
                int c = 0;
                int limit = 15;

                // Try going down first
                while (!isValidGround(level, bloc.below())) {
                    bloc.move(0, -1, 0);
                    if (++c > limit) break;
                }
                
                if (c < limit) {
                    level.sendParticles(tick[0] % 2 == 0 ? primaryParticle : secondaryParticle, loc.x, bloc.getY() + 1.5f, loc.z + 0.5f, 5, 0.1D, 0.1D, 0.1D,
                            0.04D);
                    BlockState state = level.getBlockState(bloc.below());

                    level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        bloc.getX() + 0.5D,
                        bloc.getY() + 0.5D,
                        bloc.getZ() + 0.5D,
                        10,
                        0.35D,
                        0.35D,
                        0.35D,
                        0.05D
                    );
                    SoundType soundType = state.getSoundType();

                    level.playSound(
                        null,
                        bloc,
                        soundType.getBreakSound(),
                        SoundSource.BLOCKS,
                        1.0F,
                        soundType.getPitch()
                    );
                    AABB enemySearchBox = new AABB(bloc.above().getCenter(), bloc.getCenter()).inflate(3);

                    java.util.List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        enemySearchBox,
                        entity -> entity.isAlive() && entity != player && entity.onGround()
                    );

                    for (LivingEntity target : targets) {
                        Damager.hurt(player, target, damage, false);
                        applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                        applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                    }
                }
            }
            tick[0]++;
        }, 1, lifetimeTicks);
    }

    private static void executeHover(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                     AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final int rangeAmp = amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE);
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final int lifetimeTicks = Math.max(50, (int) Math.round(60.0D * rangeScale));
        final double ringRadius = 1.6D + (0.25D * rangeScale);
        final double targetHoverHeight = 6.0D + (2.0D * rangeAmp);

        final int[] tick = {0};
        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }
            tick[0]++;

            player.fallDistance = 0.0F;
            BlockPos groundPos = player.blockPosition().mutable();
            int downChecks = 0;
            while (downChecks < 48 && !isValidGround(level, groundPos)) {
                groundPos = groundPos.below();
                downChecks++;
            }

            double currentHeightAboveGround = player.getY() - groundPos.getY();
            double heightError = targetHoverHeight - currentHeightAboveGround;
            double yVel = Mth.clamp(heightError * 0.12D, -0.2D, 0.35D) * speedScale;
            MovementHelper.setVelocity(player, player.getDeltaMovement().x * 0.7D, yVel, player.getDeltaMovement().z * 0.7D);

            double baseAngle = tick[0] * 0.26D * speedScale;
            for (int i = 0; i < 18; i++) {
                double a = baseAngle + ((Math.PI * 2.0D * i) / 18.0D);
                Vec3 p = player.position().add(Math.cos(a) * ringRadius, 0.4D + 0.3D * Math.sin(a * 2.0D), Math.sin(a) * ringRadius);
                level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }, 1, lifetimeTicks);
    }

    private static void executeShootingStar(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                            AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();

        final ParticleOptions burstParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions trailParticle = resolveParticle(core.secondaryParticle());

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final double baseSpeed = 1.1D;
        final double speed = baseSpeed * speedScale;

        final float baseDamage = 12.0F;
        final float explosionDamage = Damager.calculateScaledDamage(player, baseDamage * damageScale);

        final float baseExplosionRadius = 2.5F;
        final float explosionRadius = baseExplosionRadius * damageScale;

        final int maxLifetimeTicks = Math.max(100, (int) (100 * rangeScale));
        final int[] ticks = {0};
        final boolean[] exploded = {false};
        final int launchTicks = 6;
        final double launchUpVelocity = 0.85D + (0.1D * speedScale);

        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.85F);
        MovementHelper.setVelocity(player, 0.0D, launchUpVelocity, 0.0D);

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive() || exploded[0]) {
                return;
            }

            ticks[0]++;
            player.fallDistance = 0.0F;

            if (ticks[0] <= launchTicks) {
                MovementHelper.setVelocity(
                    player,
                    player.getDeltaMovement().x * 0.5D,
                    launchUpVelocity,
                    player.getDeltaMovement().z * 0.5D
                );
                level.sendParticles(
                    trailParticle,
                    player.getX(),
                    player.getY() + 0.4D,
                    player.getZ(),
                    10,
                    0.2D,
                    0.25D,
                    0.2D,
                    0.0D
                );
                return;
            }

            player.startFallFlying();

            Vec3 look = player.getLookAngle();
            if (look.lengthSqr() < 1.0E-4D) {
                look = new Vec3(0.0D, 0.0D, 1.0D);
            }
            look = look.normalize();

            MovementHelper.setVelocity(player, look.scale(speed));

            Vec3 pos = player.position().add(0.0D, 0.7D, 0.0D);
            level.sendParticles(trailParticle, pos.x, pos.y, pos.z, 8, 0.2D, 0.2D, 0.2D, 0.0D);

            boolean blockCrash = player.horizontalCollision || player.verticalCollision;
            boolean entityCrash = !level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(0.55D),
                e -> e != player && e.isAlive() && isEnemy(player, e)
            ).isEmpty();

            if (blockCrash || entityCrash || ticks[0] >= maxLifetimeTicks) {
                explodeShootingStar(level, player, core, amplifierTotals, burstParticle, explosionRadius, explosionDamage);
                exploded[0] = true;
            }
        }, 1, maxLifetimeTicks + 5);
    }

    private static void executeLightningCharge(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                               AmplifierTotals amplifierTotals) {
        final ServerLevel level = player.serverLevel();
        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));
        final double speedScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));

        final int lifetimeTicks = 150;
        final float arcDamage = Damager.calculateScaledDamage(player, 3.0F * damageScale);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setCause(player);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.0F);

        final int[] tick = {0};
        AbilityScheduler.scheduleRepeating(player, () -> {
            if (!player.isAlive()) {
                return;
            }
            tick[0]++;

            double angle = tick[0] * 0.45D * speedScale;
            for (int i = 0; i < 3; i++) {
                double a = angle + i * (Math.PI * 2.0D / 3.0D);
                Vec3 p = player.position().add(Math.cos(a) * 1.2D, 0.7D + (0.25D * i), Math.sin(a) * 1.2D);
                level.sendParticles(primaryParticle, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }

            applySelfPotion(core.primaryPotion(), player, amplifierTotals);
            applySelfPotion(core.secondaryPotion(), player, amplifierTotals);

            int speedAmp = Math.max(0, (int) Math.floor(speedScale * 2.0D) - 1);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, speedAmp, false, false));

            int killingIntentLevel = Math.max(1, (int) Math.round(20.0F * damageScale));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, Math.max(0, killingIntentLevel - 1), false, false));

            AABB meleeArc = player.getBoundingBox().inflate(2.2D);
            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                meleeArc,
                e -> e != player && e.isAlive() && isEnemy(player, e)
            )) {
                Vec3 toTarget = target.position().subtract(player.position());
                if (toTarget.lengthSqr() > 4.84D) {
                    continue;
                }
                Vec3 forward = player.getLookAngle().normalize();
                if (toTarget.normalize().dot(forward) < 0.35D) {
                    continue;
                }
                if (tick[0] % 8 != 0) {
                    continue;
                }

                Damager.hurt(player, target, arcDamage, true);
                applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
                applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
                MovementHelper.addVelocity(target, forward.x * 0.45D, 0.22D, forward.z * 0.45D);
            }
        }, 1, lifetimeTicks);
    }

    private static void explodeShootingStar(ServerLevel level,
                                            ServerPlayer player,
                                            CustomBloodDemonArtSavedData.CoreSettings core,
                                            AmplifierTotals amplifierTotals,
                                            ParticleOptions burstParticle,
                                            float explosionRadius,
                                            float explosionDamage) {
        Vec3 pos = player.position().add(0.0D, 0.5D, 0.0D);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false));
        player.fallDistance = 0.0F;

        level.explode(player, pos.x, pos.y, pos.z, explosionRadius, Level.ExplosionInteraction.MOB);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.95F);

        for (int i = 0; i < 24; i++) {
            double a = (Math.PI * 2.0D * i) / 24.0D;
            Vec3 dir = new Vec3(Math.cos(a), 0.18D + ((i % 3) * 0.08D), Math.sin(a)).normalize();
            for (double d = 0.5D; d <= explosionRadius * 2.8D; d += 0.5D) {
                Vec3 p = pos.add(dir.scale(d));
                level.sendParticles(burstParticle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        AABB hitBox = new AABB(pos, pos).inflate(explosionRadius);
        for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            hitBox,
            e -> e != player && e.isAlive() && isEnemy(player, e)
        )) {
            Damager.hurt(player, target, explosionDamage, true);
            applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
            applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
        }
    }

    private static void executeChainLightning(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                              AmplifierTotals amplifierTotals) {
        executeCatalystPlaceholder(player, core, amplifierTotals);
    }

    private static void executeIncendiaryProjectile(ServerPlayer player,
                                                CustomBloodDemonArtSavedData.CoreSettings core,
                                                AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();

        final ParticleOptions primaryParticle = resolveParticle(core.primaryParticle());
        final ParticleOptions secondaryParticle = resolveParticle(core.secondaryParticle());

        final double rangeScale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE));
        final float damageScale = (float) ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE));

        final double speed = 1.15D * rangeScale;
        final double gravity = 0.045D;

        final float baseExplosionRadius = 4.0F;
        final float explosionRadius = baseExplosionRadius * damageScale;

        final float baseDamage = 8.0F;
        final float explosionDamage = Damager.calculateScaledDamage(player, baseDamage * damageScale);

        final int maxLifetimeTicks = 80;

        AnimationHelper.playAnimation(player, "speed_attack_punch", 10);

        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.GHAST_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            0.9F
        );

        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.FIREWORK_ROCKET_LAUNCH,
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );

        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }

        final Vec3[] pos = {
            player.getEyePosition().add(look.normalize().scale(1.4D))
        };

        final Vec3[] velocity = {
            look.normalize().scale(speed)
        };

        final int[] ticks = {0};
        final boolean[] exploded = {false};

        AbilityScheduler.scheduleRepeating(player, () -> {
            if (exploded[0] || !player.isAlive()) {
                return;
            }

            ticks[0]++;

            Vec3 oldPos = pos[0];

            velocity[0] = velocity[0].add(0.0D, -gravity, 0.0D);
            Vec3 newPos = oldPos.add(velocity[0]);

            BlockHitResult blockHit = level.clip(new ClipContext(
                oldPos,
                newPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                pos[0] = blockHit.getLocation();
                explodeIncendiaryProjectile(level, player, core, amplifierTotals, pos[0], explosionRadius, explosionDamage);
                exploded[0] = true;
                return;
            }

            pos[0] = newPos;

            // Large primary particle sphere projectile
            double sphereRadius = 0.75D;
            for (double theta = 0.0D; theta < Math.PI * 2.0D; theta += Math.PI / 4.0D) {
                for (double phi = 0.0D; phi < Math.PI; phi += Math.PI / 4.0D) {
                    double x = Math.cos(theta) * Math.sin(phi) * sphereRadius;
                    double y = Math.cos(phi) * sphereRadius;
                    double z = Math.sin(theta) * Math.sin(phi) * sphereRadius;

                    level.sendParticles(
                        primaryParticle,
                        pos[0].x + x,
                        pos[0].y + y,
                        pos[0].z + z,
                        1,
                        0.02D,
                        0.02D,
                        0.02D,
                        0.0D
                    );
                }
            }

            // Secondary trailing particles
            level.sendParticles(
                secondaryParticle,
                oldPos.x,
                oldPos.y,
                oldPos.z,
                10,
                0.25D,
                0.25D,
                0.25D,
                0.01D
            );

            // Flash every 2 ticks
            if (ticks[0] % 2 == 0) {
                level.sendParticles(
                    ParticleTypes.FLASH,
                    pos[0].x,
                    pos[0].y,
                    pos[0].z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
                );
            }

            AABB projectileBox = new AABB(pos[0], pos[0]).inflate(0.9D);

            for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                projectileBox,
                entity -> entity != player && entity.isAlive()
            )) {
                explodeIncendiaryProjectile(level, player, core, amplifierTotals, pos[0], explosionRadius, explosionDamage);
                exploded[0] = true;
                return;
            }

            if (ticks[0] >= maxLifetimeTicks) {
                explodeIncendiaryProjectile(level, player, core, amplifierTotals, pos[0], explosionRadius, explosionDamage);
                exploded[0] = true;
            }
        }, 1, maxLifetimeTicks + 5);
    }

    private static void explodeIncendiaryProjectile(ServerLevel level,
                                                ServerPlayer player,
                                                CustomBloodDemonArtSavedData.CoreSettings core,
                                                AmplifierTotals amplifierTotals,
                                                Vec3 pos,
                                                float explosionRadius,
                                                float explosionDamage) {
        level.explode(
            player,
            pos.x,
            pos.y,
            pos.z,
            explosionRadius,
            Level.ExplosionInteraction.MOB
        );

        AABB explosionBox = new AABB(pos, pos).inflate(explosionRadius);

        for (LivingEntity target : level.getEntitiesOfClass(
            LivingEntity.class,
            explosionBox,
            entity -> entity != player && entity.isAlive()
        )) {
            Damager.hurt(player, target, explosionDamage, true);
            applyTargetPotion(core.primaryPotion(), player, target, amplifierTotals);
            applyTargetPotion(core.secondaryPotion(), player, target, amplifierTotals);
        }
    }

    private static void executeYamatoOrochi(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                            AmplifierTotals amplifierTotals) {
        ServerLevel level = player.serverLevel();
        EntityType<?> serpentType = ForgeRegistries.ENTITY_TYPES.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "serpent")
        );
        if (serpentType == null) {
            return;
        }

        Entity serpent = serpentType.create(level);
        if (serpent == null) {
            return;
        }

        Vec3 launchDir = player.getLookAngle().normalize();
        double launchSpeed = 1.35D * ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED));
        Vec3 spawnPos = player.getEyePosition().add(launchDir.scale(1.15D));

        serpent.moveTo(spawnPos.x, spawnPos.y - 0.15D, spawnPos.z, player.getYRot(), player.getXRot());
        serpent.setDeltaMovement(launchDir.scale(launchSpeed));
        serpent.setNoGravity(true);
        serpent.hasImpulse = true;
        serpent.hurtMarked = true;

        if (serpent instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        }

        level.addFreshEntity(serpent);
    }

    private static void executeEightfoldAmbush(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                                AmplifierTotals amplifierTotals) {
        // TODO: Implement Eightfold Ambush.
    }

    private static void executeSkinShed(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                        AmplifierTotals amplifierTotals) {
        // TODO: Implement Skin Shed.
    }

    private static void executeSnakeStep(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                         AmplifierTotals amplifierTotals) {
        // TODO: Implement Snake Step.
    }

    private static void executeEightfoldAscendant(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                                  AmplifierTotals amplifierTotals) {
        // TODO: Implement Eightfold Ascendant.
    }

    private static void executeCatalystPlaceholder(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                                   AmplifierTotals amplifierTotals) {
        // TODO: Implement catalyst move behavior.
    }

    private static void applySelfPotion(CustomBloodDemonArtSavedData.PotionSetting potionSetting, ServerPlayer player,
                                        AmplifierTotals amplifierTotals) {
        if (!potionSetting.selfEffect()) {
            return;
        }
        applyConfiguredEffect(potionSetting, player, player, amplifierTotals);
    }

    private static void applyTargetPotion(CustomBloodDemonArtSavedData.PotionSetting potionSetting, ServerPlayer player, LivingEntity target,
                                          AmplifierTotals amplifierTotals) {
        if (potionSetting.selfEffect()) {
            return;
        }
        applyConfiguredEffect(potionSetting, player, target, amplifierTotals);
    }

    private static void applyConfiguredEffect(CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                              ServerPlayer player, LivingEntity target, AmplifierTotals amplifierTotals) {
        if (potionSetting == null || target == null) {
            return;
        }
        String effectId = potionSetting.effectId();
        if (effectId == null || effectId.isBlank()) {
            return;
        }

        if (BloodDemonArtAlchemyCatalog.isFireInfusionEffectId(effectId)) {
            applyFireInfusionEffect(potionSetting, player, target, amplifierTotals);
            return;
        }
        if (BloodDemonArtAlchemyCatalog.isFrozenInfusionEffectId(effectId)) {
            applyFrozenInfusionEffect(potionSetting, target, amplifierTotals);
            return;
        }

        MobEffect effect = potionSetting.resolveEffect();
        if (effect != null) {
            target.addEffect(new MobEffectInstance(effect,
                amplifiedDurationTicks(potionSetting, amplifierTotals),
                amplifiedEffectAmplifier(effect, potionSetting, amplifierTotals)));
        }
    }

    private static void applyFireInfusionEffect(CustomBloodDemonArtSavedData.PotionSetting potionSetting, ServerPlayer player,
                                                LivingEntity target, AmplifierTotals amplifierTotals) {
        int fireTicks = amplifiedDurationTicks(potionSetting, amplifierTotals);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireTicks));

        int fireAmplifierLevel = amplifiedInfusionLevel(potionSetting, amplifierTotals, true);
        if (fireAmplifierLevel > 0) {
            Damager.hurt(player, target, fireAmplifierLevel, true);
        }
    }

    private static void applyFrozenInfusionEffect(CustomBloodDemonArtSavedData.PotionSetting potionSetting, LivingEntity target,
                                                  AmplifierTotals amplifierTotals) {
        int freezeTicks = amplifiedDurationTicks(potionSetting, amplifierTotals);
        int newFrozenTicks = Mth.clamp(
            Math.max(target.getTicksFrozen(), freezeTicks),
            0,
            target.getTicksRequiredToFreeze()
        );
        target.setTicksFrozen(newFrozenTicks);

        int slownessLevel = amplifiedInfusionLevel(potionSetting, amplifierTotals, true);
        if (slownessLevel > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, slownessLevel - 1));
        }
    }

    private static int amplifiedDurationTicks(CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                              AmplifierTotals amplifierTotals) {
        double scale = ampScale(amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.DURATION));
        return Math.max(40, (int) Math.round(potionSetting.durationSeconds() * 20 * scale));
    }

    private static int amplifiedEffectAmplifier(MobEffect effect, CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                                AmplifierTotals amplifierTotals) {
        int amplifier = Math.max(0, potionSetting.amplifier() - 1);
        boolean harmful = BloodDemonArtAlchemyCatalog.isHarmfulEffect(effect);
        if (harmful) {
            return amplifier + amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.HARMFUL_EFFECT);
        }
        if (!harmful) {
            return amplifier + amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.BENEFICIAL_EFFECT);
        }
        return amplifier;
    }

    private static int amplifiedInfusionLevel(CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                              AmplifierTotals amplifierTotals, boolean harmful) {
        int level = Math.max(0, potionSetting.amplifier());
        if (harmful) {
            level += amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.HARMFUL_EFFECT);
        } else {
            level += amplifierTotals.count(BloodDemonArtAlchemyCatalog.AmplifierKind.BENEFICIAL_EFFECT);
        }
        return Math.max(0, level);
    }

    private static double ampScale(int amplifierCount) {
        return 1.0D + (0.1D * Math.max(0, amplifierCount));
    }

    private static void clearMidasMark(ServerLevel level, LivingEntity target, UUID expectedOwner) {
        if (target == null) {
            return;
        }

        CompoundTag tag = target.getPersistentData();
        if (!tag.hasUUID(NBT_MIDAS_OWNER) || !tag.getUUID(NBT_MIDAS_OWNER).equals(expectedOwner)) {
            return;
        }

        String scoreHolder = target.getScoreboardName();
        Scoreboard scoreboard = level.getScoreboard();

        String markTeamName = tag.getString(NBT_MIDAS_MARK_TEAM);
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreHolder);
        if (!markTeamName.isEmpty() && currentTeam != null && currentTeam.getName().equals(markTeamName)) {
            scoreboard.removePlayerFromTeam(scoreHolder, currentTeam);
        }

        String previousTeamName = tag.getString(NBT_MIDAS_PREVIOUS_TEAM);
        if (!previousTeamName.isEmpty()) {
            PlayerTeam previousTeam = scoreboard.getPlayerTeam(previousTeamName);
            if (previousTeam != null) {
                scoreboard.addPlayerToTeam(scoreHolder, previousTeam);
            }
        }

        target.removeEffect(MobEffects.GLOWING);
        tag.remove(NBT_MIDAS_OWNER);
        tag.remove(NBT_MIDAS_EXPIRES);
        tag.remove(NBT_MIDAS_BONUS_MULTIPLIER);
        tag.remove(NBT_MIDAS_MARK_TEAM);
        tag.remove(NBT_MIDAS_PREVIOUS_TEAM);
    }

    private static ChatFormatting nearestFormattingForColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        ChatFormatting best = ChatFormatting.WHITE;
        long bestDist = Long.MAX_VALUE;

        best = pickNearest(best, r, g, b, ChatFormatting.BLACK, 0x000000, bestDist); bestDist = colorDistanceSq(r, g, b, 0x000000);
        if (colorDistanceSq(r, g, b, 0x0000AA) < bestDist) { best = ChatFormatting.DARK_BLUE; bestDist = colorDistanceSq(r, g, b, 0x0000AA); }
        if (colorDistanceSq(r, g, b, 0x00AA00) < bestDist) { best = ChatFormatting.DARK_GREEN; bestDist = colorDistanceSq(r, g, b, 0x00AA00); }
        if (colorDistanceSq(r, g, b, 0x00AAAA) < bestDist) { best = ChatFormatting.DARK_AQUA; bestDist = colorDistanceSq(r, g, b, 0x00AAAA); }
        if (colorDistanceSq(r, g, b, 0xAA0000) < bestDist) { best = ChatFormatting.DARK_RED; bestDist = colorDistanceSq(r, g, b, 0xAA0000); }
        if (colorDistanceSq(r, g, b, 0xAA00AA) < bestDist) { best = ChatFormatting.DARK_PURPLE; bestDist = colorDistanceSq(r, g, b, 0xAA00AA); }
        if (colorDistanceSq(r, g, b, 0xFFAA00) < bestDist) { best = ChatFormatting.GOLD; bestDist = colorDistanceSq(r, g, b, 0xFFAA00); }
        if (colorDistanceSq(r, g, b, 0xAAAAAA) < bestDist) { best = ChatFormatting.GRAY; bestDist = colorDistanceSq(r, g, b, 0xAAAAAA); }
        if (colorDistanceSq(r, g, b, 0x555555) < bestDist) { best = ChatFormatting.DARK_GRAY; bestDist = colorDistanceSq(r, g, b, 0x555555); }
        if (colorDistanceSq(r, g, b, 0x5555FF) < bestDist) { best = ChatFormatting.BLUE; bestDist = colorDistanceSq(r, g, b, 0x5555FF); }
        if (colorDistanceSq(r, g, b, 0x55FF55) < bestDist) { best = ChatFormatting.GREEN; bestDist = colorDistanceSq(r, g, b, 0x55FF55); }
        if (colorDistanceSq(r, g, b, 0x55FFFF) < bestDist) { best = ChatFormatting.AQUA; bestDist = colorDistanceSq(r, g, b, 0x55FFFF); }
        if (colorDistanceSq(r, g, b, 0xFF5555) < bestDist) { best = ChatFormatting.RED; bestDist = colorDistanceSq(r, g, b, 0xFF5555); }
        if (colorDistanceSq(r, g, b, 0xFF55FF) < bestDist) { best = ChatFormatting.LIGHT_PURPLE; bestDist = colorDistanceSq(r, g, b, 0xFF55FF); }
        if (colorDistanceSq(r, g, b, 0xFFFF55) < bestDist) { best = ChatFormatting.YELLOW; bestDist = colorDistanceSq(r, g, b, 0xFFFF55); }
        if (colorDistanceSq(r, g, b, 0xFFFFFF) < bestDist) { best = ChatFormatting.WHITE; }

        return best;
    }

    private static ChatFormatting pickNearest(ChatFormatting current, int r, int g, int b, ChatFormatting candidate, int candidateColor, long bestDist) {
        return colorDistanceSq(r, g, b, candidateColor) < bestDist ? candidate : current;
    }

    private static long colorDistanceSq(int r, int g, int b, int rgb) {
        long dr = r - ((rgb >> 16) & 0xFF);
        long dg = g - ((rgb >> 8) & 0xFF);
        long db = b - (rgb & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    private record AmplifierTotals(EnumMap<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> counts) {
        private static AmplifierTotals fromCounts(Map<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> source) {
            EnumMap<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> values =
                new EnumMap<>(BloodDemonArtAlchemyCatalog.AmplifierKind.class);
            if (source != null) {
                values.putAll(source);
            }
            return new AmplifierTotals(values);
        }

        private int count(BloodDemonArtAlchemyCatalog.AmplifierKind kind) {
            return Math.max(0, counts.getOrDefault(kind, 0));
        }
    }

    private static void spawnRing(ServerLevel level, CustomBloodDemonArtSavedData.ParticleStyle particleStyle, Vec3 center, double radius, int steps) {
        ParticleOptions particle = resolveParticle(particleStyle);
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2.0D * i) / steps;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 3, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    private static void spawnBurst(ServerLevel level, CustomBloodDemonArtSavedData.ParticleStyle particleStyle, Vec3 center, int count) {
        ParticleOptions particle = resolveParticle(particleStyle);
        level.sendParticles(particle, center.x, center.y, center.z, count, 0.3D, 0.25D, 0.3D, 0.01D);
    }

    private static ParticleOptions resolveParticle(CustomBloodDemonArtSavedData.ParticleStyle style) {
        ResourceLocation id = ResourceLocation.tryParse(style.particleId());
        ParticleType<?> particleType = id == null ? null : ForgeRegistries.PARTICLE_TYPES.getValue(id);
        if (particleType == null) {
            return net.minecraft.core.particles.ParticleTypes.SMOKE;
        }
        if (particleType == net.minecraft.core.particles.ParticleTypes.DUST || "minecraft:dust".equals(style.particleId())) {
            Vector3f color = new Vector3f(
                ((style.color() >> 16) & 0xFF) / 255.0F,
                ((style.color() >> 8) & 0xFF) / 255.0F,
                (style.color() & 0xFF) / 255.0F
            );
            return new DustParticleOptions(color, Mth.clamp(style.size(), 0.2F, 4.0F));
        }
        if (particleType == net.minecraft.core.particles.ParticleTypes.SMOKE) {
            return net.minecraft.core.particles.ParticleTypes.SMOKE;
        }
        return (ParticleOptions) particleType;
    }

}
