package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonSleepExecutionHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobSwordSlashPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ImpactParticleOptions;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.PlayerRole;
import com.lerdorf.kimetsunoyaibamultiplayer.util.AttackDamageHelper;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PassiveSkillManager {
    public static final String NIGHTVISION_ID = "demon_nightvision";
    public static final String REGENERATION_ID = "demon_regeneration";
    public static final String MARTIAL_ARTS_ID = "demon_martial_arts";
    public static final String CLAWS_ID = "demon_claws";

    private static final String DEMON_LAST_DAMAGE_TICK = "KnYPassiveDemonLastDamageTick";
    private static final String DEMON_PASSIVE_ATTACK_TICK = "KnYPassiveDemonAttackTick";
    private static final String MARTIAL_ART_INDEX = "KnYPassiveMartialArtIndex";
    private static final String CLAW_ART_INDEX = "KnYPassiveClawIndex";

    private static final int NIGHTVISION_MAX_LEVEL = 1;
    private static final int REGENERATION_MAX_LEVEL = 10;
    private static final int MARTIAL_ARTS_MAX_LEVEL = 5;
    private static final int CLAWS_MAX_LEVEL = 5;
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
            case DEMON_SLAYER, DEMON_SLAYER_IN_TRAINING -> List.of(
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

    public static int getAvailableSkillPoints(ServerPlayer player, PlayerRole role) {
        if (role != PlayerRole.DEMON || !isDemon(player)) {
            return 0;
        }
        return Math.max(0, getTotalSkillPoints(player) - getSpentSkillPoints(player));
    }

    public static boolean adjustSkillLevel(ServerPlayer player, String skillId, int delta) {
        if (!isDemon(player) || delta == 0) {
            return false;
        }
        SkillDefinition skill = demonSkill(skillId);
        if (skill == null) {
            return false;
        }

        CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(player.serverLevel());
        CustomBloodDemonArtSavedData.PlayerArtData artData = savedData.getOrCreate(player);
        int currentLevel = clamp(artData.passiveSkillLevel(skill.id()), 0, skill.maxLevel());
        if (delta > 0) {
            if (currentLevel >= skill.maxLevel() || getAvailableSkillPoints(player, PlayerRole.DEMON) <= 0) {
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

    public static void tick(ServerPlayer player) {
        if (!isDemon(player)) {
            return;
        }

        if (player.level().getGameTime() % 20L == 0L) {
            tickNightvision(player);
            tickDemonRegeneration(player);
        }
    }

    public static void recordDamage(Player player) {
        if (player == null || player.level().isClientSide() || !isDemon(player)) {
            return;
        }
        player.getPersistentData().putLong(DEMON_LAST_DAMAGE_TICK, player.level().getGameTime());
    }

    public static boolean handleCustomBdaPassiveAttack(ServerPlayer player) {
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
                performMartialArtsAttack(player, martialArtsLevel);
            } else {
                performClawAttack(player, clawsLevel);
            }
        } else if (martialArtsLevel > 0) {
            performMartialArtsAttack(player, martialArtsLevel);
        } else {
            performClawAttack(player, clawsLevel);
        }
        return true;
    }

    public static int getSkillLevel(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }
        SkillDefinition definition = demonSkill(skillId);
        if (definition == null) {
            return 0;
        }
        CustomBloodDemonArtSavedData.PlayerArtData artData =
            CustomBloodDemonArtSavedData.get(player.serverLevel()).getOrCreate(player);
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

    private static void performMartialArtsAttack(ServerPlayer player, int level) {
        int index = Math.floorMod(player.getPersistentData().getInt(MARTIAL_ART_INDEX), MARTIAL_ART_ANIMATIONS.length);
        String animation = MARTIAL_ART_ANIMATIONS[index];
        player.getPersistentData().putInt(MARTIAL_ART_INDEX, (index + 1) % MARTIAL_ART_ANIMATIONS.length);

        AnimationHelper.playAnimation(player, animation, 10);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnImpact(player);
        damageTargets(player, level, false);
    }

    private static void performClawAttack(ServerPlayer player, int level) {
        int index = Math.floorMod(player.getPersistentData().getInt(CLAW_ART_INDEX), CLAW_ANIMATIONS.length);
        String animation = CLAW_ANIMATIONS[index];
        player.getPersistentData().putInt(CLAW_ART_INDEX, (index + 1) % CLAW_ANIMATIONS.length);

        AnimationHelper.playAnimation(player, animation, 10);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        ModNetworking.sendToAllClients(new MobSwordSlashPacket(player.getUUID(), normalizeSlashAnimation(animation), 0));
        damageTargets(player, level, true);
    }

    private static void damageTargets(ServerPlayer player, int skillLevel, boolean claws) {
        Vec3 eyePos = player.position().add(0.0D, player.getEyeHeight(), 0.0D);
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 frontPos = eyePos.add(lookVec.scale(DEFAULT_AOE_BOX_SIZE / 1.5F));
        AABB attackBox = new AABB(
            frontPos.add(-DEFAULT_AOE_BOX_SIZE / 2.0F, -DEFAULT_AOE_BOX_SIZE / 2.0F, -DEFAULT_AOE_BOX_SIZE / 2.0F),
            frontPos.add(DEFAULT_AOE_BOX_SIZE / 2.0F, DEFAULT_AOE_BOX_SIZE / 2.0F, DEFAULT_AOE_BOX_SIZE / 2.0F)
        );

        float damage = Math.max(1.0F, AttackDamageHelper.getAttackDamageForHand(player, InteractionHand.MAIN_HAND))
            * damageScale(skillLevel);
        double knockback = 0.18D + 0.12D * skillLevel;
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            entity -> entity != player && entity.isAlive());

        for (LivingEntity target : targets) {
            if (DemonSleepExecutionHandler.isSleepingInBed(target) && !Damager.isDemon(target)) {
                DemonSleepExecutionHandler.executeSleepAttack(player, target);
                continue;
            }

            if (!EnhancedLoveForms.isTargetable(player, target)) {
                continue;
            }
            Damager.hurt(player, target, damage);
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

    private static int getSpentSkillPoints(ServerPlayer player) {
        int spent = 0;
        for (SkillDefinition skill : DEMON_SKILLS) {
            spent += getSkillLevel(player, skill.id());
        }
        return spent;
    }

    private static SkillDefinition demonSkill(String skillId) {
        for (SkillDefinition skill : DEMON_SKILLS) {
            if (skill.id().equals(skillId)) {
                return skill;
            }
        }
        return null;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SkillDefinition(String id, String name, int maxLevel, String description, String status) {
    }
}
