package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonSleepExecutionHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonSlayerSkillPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ImpactParticleOptions;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.util.AttackDamageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PassiveSkillManager {
    public static final String NIGHTVISION_ID = "demon_nightvision";
    public static final String REGENERATION_ID = "demon_regeneration";
    public static final String MARTIAL_ARTS_ID = "demon_martial_arts";
    public static final String CLAWS_ID = "demon_claws";
    public static final String SLAYER_GUARD_ID = "slayer_guard";
    public static final String SLAYER_DASH_ID = "slayer_dash";

    private static final String DEMON_LAST_DAMAGE_TICK = "KnYPassiveDemonLastDamageTick";
    private static final String DEMON_PASSIVE_ATTACK_TICK = "KnYPassiveDemonAttackTick";
    private static final String MARTIAL_ART_INDEX = "KnYPassiveMartialArtIndex";
    private static final String CLAW_ART_INDEX = "KnYPassiveClawIndex";
    private static final String SLAYER_GUARD_ACTIVE = "KnYSlayerGuardActive";
    private static final String SLAYER_GUARD_REMAINING = "KnYSlayerGuardRemaining";
    private static final String SLAYER_GUARD_LAST_INPUT = "KnYSlayerGuardLastInput";
    private static final String SLAYER_GUARD_LAST_DRAIN = "KnYSlayerGuardLastDrain";
    private static final String SLAYER_GUARD_ITEM = "KnYSlayerGuardItem";
    private static final String SLAYER_GUARD_COOLDOWN_UNTIL = "KnYSlayerGuardCooldownUntil";
    private static final String SLAYER_DASH_COOLDOWN_UNTIL = "KnYSlayerDashCooldownUntil";

    private static final int NIGHTVISION_MAX_LEVEL = 1;
    private static final int REGENERATION_MAX_LEVEL = 10;
    private static final int MARTIAL_ARTS_MAX_LEVEL = 5;
    private static final int CLAWS_MAX_LEVEL = 5;
    private static final int SLAYER_GUARD_MAX_LEVEL = 5;
    private static final int SLAYER_DASH_MAX_LEVEL = 5;
    private static final int SLAYER_GUARD_COOLDOWN_TICKS = 60;
    private static final int SLAYER_DASH_COOLDOWN_TICKS_LEVEL_ONE = 60;
    private static final int SLAYER_DASH_COOLDOWN_TICKS_LEVEL_FIVE = 20;
    private static final UUID SLAYER_GUARD_MOVEMENT_MODIFIER_UUID =
        UUID.fromString("5d6f2e21-95b8-4da3-9c90-6c6d8b3e16ce");
    private static final int NIGHTVISION_LIGHT_THRESHOLD = 5;
    private static final int PASSIVE_ATTACK_COOLDOWN_TICKS = 8;
    private static final float DEFAULT_AOE_BOX_SIZE = 5.0F;

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

    private static final List<SkillDefinition> DEMON_SKILLS = List.of(
        new SkillDefinition(NIGHTVISION_ID, "Nightvision", NIGHTVISION_MAX_LEVEL,
            "In dark areas, gain ambient Night Vision with no particles.",
            "Checks every second. Requires block light below 5; sky exposure only works at night."),
        new SkillDefinition(REGENERATION_ID, "Regeneration", REGENERATION_MAX_LEVEL,
            "After avoiding damage, briefly gain Regeneration equal to this skill level.",
            "Requires missing health and more than 6 hunger bars. Each application consumes hunger."),
        new SkillDefinition(MARTIAL_ARTS_ID, "Martial Arts", MARTIAL_ARTS_MAX_LEVEL,
            "Custom BDA left-clicks can become punch and kick AOE attacks.",
            "Damage and knockback scale with level. Spawns a BDA-colored impact particle."),
        new SkillDefinition(CLAWS_ID, "Claws", CLAWS_MAX_LEVEL,
            "Custom BDA left-clicks can become claw slash AOE attacks.",
            "Damage scales with level and may apply one configured target BDA effect.")
    );

    private static final List<SkillDefinition> SLAYER_SKILLS = List.of(
        new SkillDefinition(SLAYER_GUARD_ID, "Guard", SLAYER_GUARD_MAX_LEVEL,
            "Hold X to maintain a defensive stance and reduce movement speed.",
            "Guard strength and duration scale with level. Ends on release or interruption."),
        new SkillDefinition(SLAYER_DASH_ID, "Dash", SLAYER_DASH_MAX_LEVEL,
            "Press Z to launch forward with a short cooldown.",
            "Cooldown decreases and dash power increases with level.")
    );

    private PassiveSkillManager() {
    }

    public static List<MeditationMenuData.PassiveSkillEntry> buildEntries(ServerPlayer player, PlayerRole role) {
        if (role == PlayerRole.DEMON) {
            int availablePoints = getAvailableSkillPoints(player, role);
            List<MeditationMenuData.PassiveSkillEntry> entries = new ArrayList<>();
            for (SkillDefinition skill : DEMON_SKILLS) {
                int level = getSkillLevel(player, skill.id());
                entries.add(new MeditationMenuData.PassiveSkillEntry(
                    skill.id(),
                    skill.name(),
                    "Demon",
                    level,
                    skill.maxLevel(),
                    skill.description(),
                    statusText(player, skill, level),
                    true,
                    availablePoints > 0 && level < skill.maxLevel(),
                    level > 0
                ));
            }
            return entries;
        }

        return switch (role) {
            case DEMON_SLAYER -> buildSlayerEntries(player);
            case DEMON_SLAYER_IN_TRAINING -> List.of(
                pending("slayer_total_concentration", "Total Concentration", "Demon Slayer"),
                pending("slayer_breath_control", "Breath Control", "Demon Slayer")
            );
            case KAKUSHI -> List.of(
                pending("kakushi_field_medicine", "Field Medicine", "Kakushi"),
                pending("kakushi_silent_steps", "Silent Steps", "Kakushi")
            );
            case SWORDSMITH -> List.of(
                pending("swordsmith_forge_focus", "Forge Focus", "Swordsmith"),
                pending("swordsmith_weapon_care", "Weapon Care", "Swordsmith")
            );
            default -> List.of();
        };
    }

    private static List<MeditationMenuData.PassiveSkillEntry> buildSlayerEntries(ServerPlayer player) {
        int availablePoints = getAvailableSkillPoints(player, PlayerRole.DEMON_SLAYER);
        List<MeditationMenuData.PassiveSkillEntry> entries = new ArrayList<>();
        for (SkillDefinition skill : SLAYER_SKILLS) {
            int level = getSkillLevel(player, skill.id());
            entries.add(new MeditationMenuData.PassiveSkillEntry(
                skill.id(),
                skill.name(),
                "Demon Slayer",
                level,
                skill.maxLevel(),
                skill.description(),
                statusText(player, skill, level),
                true,
                availablePoints > 0 && level < skill.maxLevel(),
                level > 0
            ));
        }
        return entries;
    }

    public static int getAvailableSkillPoints(ServerPlayer player, PlayerRole role) {
        if (role == PlayerRole.DEMON && isDemon(player)) {
            return Math.max(0, getTotalSkillPoints(player) - getSpentSkillPoints(player, DEMON_SKILLS));
        }
        if (role == PlayerRole.DEMON_SLAYER && isDemonSlayer(player)) {
            CustomBloodDemonArtSavedData.PlayerArtData artData = getArtData(player);
            return Math.max(0, artData.demonSlayerSkillPoints() - getSpentSkillPoints(player, SLAYER_SKILLS));
        }
        return 0;
    }

    public static boolean adjustSkillLevel(ServerPlayer player, String skillId, int delta) {
        if (player == null || delta == 0) {
            return false;
        }
        PlayerRole role = MeditationMenuService.resolveRoleForProgression(player);
        List<SkillDefinition> skills = role == PlayerRole.DEMON ? DEMON_SKILLS
            : role == PlayerRole.DEMON_SLAYER ? SLAYER_SKILLS : List.of();
        SkillDefinition skill = findSkill(skills, skillId);
        if (skill == null) {
            return false;
        }

        CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(player.serverLevel());
        CustomBloodDemonArtSavedData.PlayerArtData artData = savedData.getOrCreate(player);
        int currentLevel = clamp(artData.passiveSkillLevel(skill.id()), 0, skill.maxLevel());
        if (delta > 0) {
            if (currentLevel >= skill.maxLevel() || getAvailableSkillPoints(player, role) <= 0) {
                return false;
            }
            artData.setPassiveSkillLevel(skill.id(), currentLevel + 1);
        } else {
            if (currentLevel <= 0) {
                return false;
            }
            artData.setPassiveSkillLevel(skill.id(), currentLevel - 1);
        }
        savedData.setDirty();
        return true;
    }

    public static boolean addSkillPoints(ServerPlayer player, int amount) {
        if (player == null || amount <= 0 || !isDemonSlayer(player)
            || MeditationMenuService.resolveRoleForProgression(player) != PlayerRole.DEMON_SLAYER) {
            return false;
        }

        CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(player.serverLevel());
        savedData.getOrCreate(player).addDemonSlayerSkillPoints(amount);
        savedData.setDirty();
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (isDemon(player)) {
            if (player.level().getGameTime() % 20L == 0L) {
                tickNightvision(player);
                tickDemonRegeneration(player);
            }
        }

        if (isSlayerGuardActive(player)) {
            if (isDemonSlayer(player)
                && MeditationMenuService.resolveRoleForProgression(player) == PlayerRole.DEMON_SLAYER) {
                tickSlayerGuard(player);
            } else {
                cancelGuard(player);
            }
        }
    }

    public static boolean startGuard(ServerPlayer player) {
        if (player == null || !isDemonSlayer(player)
            || MeditationMenuService.resolveRoleForProgression(player) != PlayerRole.DEMON_SLAYER) {
            return false;
        }
        if (isSlayerGuardActive(player)) {
            refreshGuardInput(player);
            return true;
        }

        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(SLAYER_GUARD_COOLDOWN_UNTIL)) {
            return false;
        }

        int level = getSkillLevel(player, SLAYER_GUARD_ID);
        if (level <= 0) {
            return false;
        }

        int guardPower = guardPower(level);
        player.getPersistentData().putBoolean(SLAYER_GUARD_ACTIVE, true);
        player.getPersistentData().putInt(SLAYER_GUARD_REMAINING, guardPower);
        player.getPersistentData().putLong(SLAYER_GUARD_LAST_INPUT, now);
        player.getPersistentData().putLong(SLAYER_GUARD_LAST_DRAIN, now);
        player.getPersistentData().putString(SLAYER_GUARD_ITEM, heldItemId(player));
        GuardStateHelper.setGuardState(player, guardPower, 0.0D, false);
        setGuardMovementModifier(player, true);

        int animationIndex = player.getRandom().nextInt(6);
        AnimationHelper.playAnimation(player, "guard_" + animationIndex, -1);
        ModNetworking.sendToPlayer(new DemonSlayerSkillPacket(DemonSlayerSkillPacket.GUARD_STATE, guardPower), player);
        return true;
    }

    public static void refreshGuardInput(ServerPlayer player) {
        if (isSlayerGuardActive(player)) {
            player.getPersistentData().putLong(SLAYER_GUARD_LAST_INPUT, player.level().getGameTime());
        }
    }

    public static void cancelGuard(ServerPlayer player) {
        if (player == null || !isSlayerGuardActive(player)) {
            return;
        }

        player.getPersistentData().putBoolean(SLAYER_GUARD_ACTIVE, false);
        GuardStateHelper.clearGuardState(player);
        setGuardMovementModifier(player, false);
        player.getPersistentData().remove(SLAYER_GUARD_REMAINING);
        player.getPersistentData().remove(SLAYER_GUARD_LAST_INPUT);
        player.getPersistentData().remove(SLAYER_GUARD_LAST_DRAIN);
        player.getPersistentData().remove(SLAYER_GUARD_ITEM);
        player.getPersistentData().putLong(SLAYER_GUARD_COOLDOWN_UNTIL,
            player.level().getGameTime() + SLAYER_GUARD_COOLDOWN_TICKS);
        ModNetworking.sendToPlayer(new DemonSlayerSkillPacket(DemonSlayerSkillPacket.GUARD_STATE, 0), player);
        ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
    }

    public static boolean useDash(ServerPlayer player) {
        if (player == null || !isDemonSlayer(player)
            || MeditationMenuService.resolveRoleForProgression(player) != PlayerRole.DEMON_SLAYER) {
            return false;
        }

        int level = getSkillLevel(player, SLAYER_DASH_ID);
        if (level <= 0) {
            return false;
        }

        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(SLAYER_DASH_COOLDOWN_UNTIL)) {
            return false;
        }

        cancelGuard(player);
        Vec3 direction = player.getLookAngle();
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D).yRot(-player.getYRot() * ((float) Math.PI / 180.0F));
        } else {
            horizontal = horizontal.normalize();
        }
        double power = 0.9D + (0.2D * (clamp(level, 1, SLAYER_DASH_MAX_LEVEL) - 1));
        MovementHelper.setVelocity(player, horizontal.scale(power).add(0.0D, 0.12D, 0.0D));
        player.getPersistentData().putLong(SLAYER_DASH_COOLDOWN_UNTIL, now + dashCooldown(level));
        AnimationHelper.playAnimation(player, "sprint", 8);
        return true;
    }

    public static boolean isSlayerGuardActive(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(SLAYER_GUARD_ACTIVE);
    }

    private static void tickSlayerGuard(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (player.isDeadOrDying()
            || now - player.getPersistentData().getLong(SLAYER_GUARD_LAST_INPUT) > 10L
            || !heldItemId(player).equals(player.getPersistentData().getString(SLAYER_GUARD_ITEM))) {
            cancelGuard(player);
            return;
        }

        long lastDrain = player.getPersistentData().getLong(SLAYER_GUARD_LAST_DRAIN);
        long elapsed = now - lastDrain;
        if (elapsed < 2L) {
            return;
        }

        int drain = (int) (elapsed / 2L);
        int remaining = player.getPersistentData().getInt(SLAYER_GUARD_REMAINING) - drain;
        player.getPersistentData().putLong(SLAYER_GUARD_LAST_DRAIN, lastDrain + drain * 2L);
        if (remaining <= 0) {
            cancelGuard(player);
            return;
        }

        player.getPersistentData().putInt(SLAYER_GUARD_REMAINING, remaining);
        GuardStateHelper.setGuardState(player, remaining, 0.0D, false);
        ModNetworking.sendToPlayer(new DemonSlayerSkillPacket(DemonSlayerSkillPacket.GUARD_STATE, remaining), player);
    }

    public static void recordDamage(Player player) {
        if (player == null || player.level().isClientSide() || !isDemon(player)) {
            return;
        }
        player.getPersistentData().putLong(DEMON_LAST_DAMAGE_TICK, player.level().getGameTime());
    }

    public static boolean handleCustomBdaPassiveAttack(ServerPlayer player) {
        return handleCustomBdaPassiveAttack(player, null);
    }

    public static boolean handleCustomBdaPassiveAttack(ServerPlayer player, UUID excludedTargetId) {
        if (!isDemon(player) || !isHoldingCustomBda(player)) {
            return false;
        }

        long now = player.level().getGameTime();
        long lastAttack = player.getPersistentData().getLong(DEMON_PASSIVE_ATTACK_TICK);
        if (now - lastAttack < PASSIVE_ATTACK_COOLDOWN_TICKS) {
            return false;
        }

        int martialArtsLevel = getSkillLevel(player, MARTIAL_ARTS_ID);
        int clawsLevel = getSkillLevel(player, CLAWS_ID);
        if (martialArtsLevel <= 0 && clawsLevel <= 0) {
            return false;
        }

        player.getPersistentData().putLong(DEMON_PASSIVE_ATTACK_TICK, now);
        if (martialArtsLevel > 0 && clawsLevel > 0) {
            if (player.getRandom().nextBoolean()) {
                performMartialArtsAttack(player, martialArtsLevel, excludedTargetId);
            } else {
                performClawAttack(player, clawsLevel, excludedTargetId);
            }
        } else if (martialArtsLevel > 0) {
            performMartialArtsAttack(player, martialArtsLevel, excludedTargetId);
        } else {
            performClawAttack(player, clawsLevel, excludedTargetId);
        }
        return true;
    }

    public static int getSkillLevel(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }
        SkillDefinition definition = findSkill(DEMON_SKILLS, skillId);
        if (definition == null) {
            definition = findSkill(SLAYER_SKILLS, skillId);
        }
        if (definition == null) {
            return 0;
        }
        CustomBloodDemonArtSavedData.PlayerArtData artData = getArtData(player);
        return clamp(artData.passiveSkillLevel(definition.id()), 0, definition.maxLevel());
    }

    private static MeditationMenuData.PassiveSkillEntry pending(String id, String name, String role) {
        return new MeditationMenuData.PassiveSkillEntry(
            id,
            name,
            role,
            0,
            1,
            "Passive skill slot reserved for " + role + " progression.",
            "No server effect is configured for this passive yet.",
            false,
            false,
            false
        );
    }

    private static void tickNightvision(ServerPlayer player) {
        if (getSkillLevel(player, NIGHTVISION_ID) <= 0) {
            return;
        }

        BlockPos pos = player.blockPosition();
        int blockLight = player.level().getBrightness(LightLayer.BLOCK, pos);
        if (blockLight >= NIGHTVISION_LIGHT_THRESHOLD) {
            return;
        }
        if (player.level().canSeeSky(pos) && !isNight(player.level())) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 10, 0, true, false, true));
    }

    private static void tickDemonRegeneration(ServerPlayer player) {
        int level = getSkillLevel(player, REGENERATION_ID);
        if (level <= 0 || player.getHealth() >= player.getMaxHealth() || player.getFoodData().getFoodLevel() <= 12) {
            return;
        }

        long now = player.level().getGameTime();
        long lastDamageTick = player.getPersistentData().getLong(DEMON_LAST_DAMAGE_TICK);
        if (lastDamageTick <= 0L) {
            player.getPersistentData().putLong(DEMON_LAST_DAMAGE_TICK, now);
            return;
        }
        if (now - lastDamageTick < regenerationWaitTicks(level)) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, Math.max(0, level - 1), true, false, true));
        player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 1));
    }

    private static void performMartialArtsAttack(ServerPlayer player, int level, UUID excludedTargetId) {
        int index = Math.floorMod(player.getPersistentData().getInt(MARTIAL_ART_INDEX), MARTIAL_ART_ANIMATIONS.length);
        String animation = MARTIAL_ART_ANIMATIONS[index];
        player.getPersistentData().putInt(MARTIAL_ART_INDEX, (index + 1) % MARTIAL_ART_ANIMATIONS.length);

        AnimationHelper.playAnimation(player, animation, 10);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnImpact(player);
        damageTargets(player, level, false, excludedTargetId);
    }

    private static void performClawAttack(ServerPlayer player, int level, UUID excludedTargetId) {
        int index = Math.floorMod(player.getPersistentData().getInt(CLAW_ART_INDEX), CLAW_ANIMATIONS.length);
        String animation = CLAW_ANIMATIONS[index];
        player.getPersistentData().putInt(CLAW_ART_INDEX, (index + 1) % CLAW_ANIMATIONS.length);

        AnimationHelper.playAnimation(player, animation, 10);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        ModNetworking.sendToAllClients(new MobSwordSlashPacket(player.getUUID(), normalizeSlashAnimation(animation), 0));
        damageTargets(player, level, true, excludedTargetId);
    }

    private static void damageTargets(ServerPlayer player, int skillLevel, boolean claws, UUID excludedTargetId) {
        Vec3 eyePos = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 frontPos = eyePos.add(lookVec.scale(DEFAULT_AOE_BOX_SIZE / 1.5F));
        AABB attackBox = new AABB(
            frontPos.add(-DEFAULT_AOE_BOX_SIZE / 2.0F, -DEFAULT_AOE_BOX_SIZE / 2.0F, -DEFAULT_AOE_BOX_SIZE / 2.0F),
            frontPos.add(DEFAULT_AOE_BOX_SIZE / 2.0F, DEFAULT_AOE_BOX_SIZE / 2.0F, DEFAULT_AOE_BOX_SIZE / 2.0F)
        );

        float damage = AttackDamageHelper.getM1AoeDamageByStrength(player);
        double knockback = 0.18D + 0.12D * skillLevel;
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            entity -> entity != player && entity.isAlive() && (excludedTargetId == null || !entity.getUUID().equals(excludedTargetId)));

        for (LivingEntity target : targets) {
            if (DemonSleepExecutionHandler.isSleepingInBed(target) && !Damager.isDemon(target)) {
                DemonSleepExecutionHandler.executeSleepAttack(player, target);
                continue;
            }

            if (!EnhancedLoveForms.isTargetable(player, target)) {
                continue;
            }
            Damager.hurt(player, target, damage, false, true);
            target.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ());
            if (claws) {
                tryApplyClawTargetEffect(player, target, skillLevel);
            }
        }
    }

    private static void tryApplyClawTargetEffect(ServerPlayer player, LivingEntity target, int skillLevel) {
        float chance = 0.05F * skillLevel;
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }

        CustomBloodDemonArtSavedData.CoreSettings core =
            CustomBloodDemonArtSavedData.get(player.serverLevel()).getOrCreate(player).coreSettings();
        List<CustomBloodDemonArtSavedData.PotionSetting> targetEffects = new ArrayList<>();
        addTargetEffect(targetEffects, core.primaryPotion());
        addTargetEffect(targetEffects, core.secondaryPotion());
        if (targetEffects.isEmpty()) {
            return;
        }

        CustomBloodDemonArtSavedData.PotionSetting setting = targetEffects.get(player.getRandom().nextInt(targetEffects.size()));
        MobEffect effect = setting.resolveEffect();
        if (effect != null) {
            target.addEffect(new MobEffectInstance(
                effect,
                Math.max(20, setting.durationSeconds() * 20),
                Math.max(0, setting.amplifier() - 1),
                false,
                true,
                true
            ));
        }
    }

    private static void addTargetEffect(List<CustomBloodDemonArtSavedData.PotionSetting> targetEffects,
                                        CustomBloodDemonArtSavedData.PotionSetting setting) {
        if (setting != null && !setting.selfEffect() && setting.effectId() != null && !setting.effectId().isBlank()) {
            targetEffects.add(setting);
        }
    }

    private static void spawnImpact(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int color = CustomBloodDemonArtSavedData.get(serverLevel).getOrCreate(player).coreSettings().chatColor() & 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        Vec3 impactPos = player.position()
            .add(0.0D, player.getEyeHeight(), 0.0D)
            .add(player.getLookAngle().normalize().scale(2.0D));
        serverLevel.sendParticles(
            new ImpactParticleOptions(r, g, b, 1.0F),
            impactPos.x,
            impactPos.y,
            impactPos.z,
            1,
            player.getLookAngle().x * 0.02D,
            player.getLookAngle().y * 0.02D,
            player.getLookAngle().z * 0.02D,
            0.12D
        );
    }

    private static String normalizeSlashAnimation(String animation) {
        if ("left_sword_to_left".equals(animation)) {
            return "sword_to_left";
        }
        if ("left_sword_to_right".equals(animation)) {
            return "sword_to_right";
        }
        if ("left_sword_overhead".equals(animation)) {
            return "sword_overhead";
        }
        return animation;
    }

    private static String statusText(ServerPlayer player, SkillDefinition skill, int level) {
        return switch (skill.id()) {
            case NIGHTVISION_ID -> level > 0
                ? "Unlocked. Reapplies every 20 ticks while darkness conditions are met."
                : "Max level 1.";
            case REGENERATION_ID -> level > 0
                ? "Current wait: " + formatSeconds(regenerationWaitTicks(level) / 20.0D) + "s. Regeneration level " + level + "."
                : "Max level 10. Wait becomes shorter with each level.";
            case MARTIAL_ARTS_ID -> level > 0
                ? "Damage: " + Math.round(damageScale(level) * 100.0F) + "%. Knockback scales with level."
                : "Max level 5. Requires custom BDA item left-clicks.";
            case CLAWS_ID -> level > 0
                ? "Damage: " + Math.round(damageScale(level) * 100.0F) + "%. Target-effect chance: " + Math.round(0.05F * level * 100.0F) + "%."
                : "Max level 5. Requires custom BDA item left-clicks.";
            case SLAYER_GUARD_ID -> level > 0
                ? "Defensive power: " + guardPower(level) + ". Movement speed is reduced to 30% while held."
                : "Max level 5. Hold X to guard; ends on release or interruption.";
            case SLAYER_DASH_ID -> level > 0
                ? "Cooldown: " + dashCooldown(level) + " ticks. Dash power increases with level."
                : "Max level 5. Press Z to dash forward.";
            default -> skill.status();
        };
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static int regenerationWaitTicks(int level) {
        return Math.max(1, Math.round((21.0F - clamp(level, 1, REGENERATION_MAX_LEVEL) * 1.5F) * 20.0F));
    }

    private static float damageScale(int level) {
        return switch (clamp(level, 1, 5)) {
            case 1 -> 0.5F;
            case 2 -> 0.6F;
            case 3 -> 0.7F;
            case 4 -> 0.85F;
            default -> 1.0F;
        };
    }

    private static int getTotalSkillPoints(ServerPlayer player) {
        int effectiveBlood = DemonTransformationHandler.getEffectiveMuzanBlood(player);
        return effectiveBlood <= 0 ? 0 : 1 + (effectiveBlood / 10);
    }

    private static int getSpentSkillPoints(ServerPlayer player, List<SkillDefinition> skills) {
        int spent = 0;
        for (SkillDefinition skill : skills) {
            spent += getSkillLevel(player, skill.id());
        }
        return spent;
    }

    private static SkillDefinition findSkill(List<SkillDefinition> skills, String skillId) {
        for (SkillDefinition skill : skills) {
            if (skill.id().equals(skillId)) {
                return skill;
            }
        }
        return null;
    }

    private static CustomBloodDemonArtSavedData.PlayerArtData getArtData(ServerPlayer player) {
        return CustomBloodDemonArtSavedData.get(player.serverLevel()).getOrCreate(player);
    }

    private static int guardPower(int level) {
        int clampedLevel = clamp(level, 1, SLAYER_GUARD_MAX_LEVEL);
        return 6 + Math.round((clampedLevel - 1) * (44.0F / 4.0F));
    }

    private static int dashCooldown(int level) {
        int clampedLevel = clamp(level, 1, SLAYER_DASH_MAX_LEVEL);
        int reductionPerLevel = (SLAYER_DASH_COOLDOWN_TICKS_LEVEL_ONE - SLAYER_DASH_COOLDOWN_TICKS_LEVEL_FIVE)
            / (SLAYER_DASH_MAX_LEVEL - 1);
        return SLAYER_DASH_COOLDOWN_TICKS_LEVEL_ONE - (clampedLevel - 1) * reductionPerLevel;
    }

    private static String heldItemId(ServerPlayer player) {
        return BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
    }

    private static void setGuardMovementModifier(ServerPlayer player, boolean active) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        movement.removeModifier(SLAYER_GUARD_MOVEMENT_MODIFIER_UUID);
        if (active) {
            movement.addTransientModifier(new AttributeModifier(
                SLAYER_GUARD_MOVEMENT_MODIFIER_UUID,
                "Demon Slayer guard movement penalty",
                -0.7D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static boolean isHoldingCustomBda(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty() && held.getItem() == ModItems.CUSTOM_DEMON_ART.get();
    }

    private static boolean isNight(Level level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private static boolean isDemon(Player player) {
        return player != null && Damager.isDemon(player);
    }

    private static boolean isDemonSlayer(ServerPlayer player) {
        return player != null
            && MeditationMenuService.resolveRoleForProgression(player) == PlayerRole.DEMON_SLAYER;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SkillDefinition(String id, String name, int maxLevel, String description, String status) {
    }
}
