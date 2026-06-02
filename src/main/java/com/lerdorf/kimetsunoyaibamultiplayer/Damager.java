package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public class Damager {
	private static final String NBT_MIDAS_OWNER = "knymp_midas_owner";
	private static final String NBT_MIDAS_EXPIRES = "knymp_midas_expires";
	private static final String NBT_MIDAS_BONUS_MULTIPLIER = "knymp_midas_bonus_multiplier";

	/**
	 * Checks if an entity is a demon slayer (human or demon slayer corps member).
	 * Uses name-based detection as a fallback - ideally should be replaced with
	 * proper NBT tags or entity type checks.
	 */
	public static boolean isDemonSlayer(LivingEntity entity) {
		// Check if entity has the demon slayer corps NBT tag
		if (entity.getPersistentData().getBoolean("kisatsutai")) {
			return true;
		}

		// Player-specific check: not a demon
		if (entity instanceof Player) {
			if (!entity.getPersistentData().getBoolean("oni"))
				return true;
			else
				return false;
		}

		// Let's find a better way of doing this rather than just using names
		String name = entity.getName().getString().toLowerCase();
		String[] demonSlayers = {"demon_slayer", "dice_steak", "_slayer", "murata", "nezuko", "zenitsu", "zennitsu", "inosuke", "kanawo", "genya", "giyu", "shinobu", "rengoku", "uzui", "muichirou", "tokito", "kanroji", "iguro", "sanemi", "shinazugawa", "himejima", "masachika", "kanae", "kaigaku", "ubuyashiki", "michikatsu", "yoriichi", "komorebi", "shimizu"};

		for (String s : demonSlayers) {
			if (name.contains("demon") && !name.contains("slayer"))
				return false;
			if (name.contains(s))
				return true;
		}
		if (name.contains("tanjiro") && !name.contains("demon"))
			return true;
		
		return false;
	}

	/**
	 * Checks if an entity is a demon.
	 * This checks both entity tags (for KnY demon mobs) and player NBT data (for demon players).
	 *
	 * @param entity The entity to check
	 * @return true if the entity is a demon, false otherwise
	 */
	public static boolean isDemon(LivingEntity entity) {
		if (entity == null) {
			return false;
		}

		// Check if player has oni (demon) NBT tag
		if (entity instanceof Player) {
			return entity.getPersistentData().getBoolean("oni");
		}

		// Check if entity is tagged as a demon in the kimetsunoyaiba:demon entity tag
		boolean isDemonTagged = entity.getType().is(
			TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon"))
		);

        ResourceLocation entityId = entity.getType() == null ? null : net.minecraft.world.entity.EntityType.getKey(entity.getType());
        boolean isRegisteredAddonDemon = entityId != null && DemonRegistry.isRegistered(entityId);

		// Check if entity is tagged as a twelve kizuki (upper/lower moon demon)
		boolean isTwelveKizuki = entity.getType().is(
			TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "twelve_kizuki"))
		);

		// Also check via NBT tag as fallback
		boolean hasOniTag = entity.getPersistentData().getBoolean("oni");

		return isDemonTagged || isTwelveKizuki || hasOniTag || isRegisteredAddonDemon;
	}

	public static boolean isNeutral(LivingEntity entity) {
		if (entity == null) {
			return false;
		}

		// Check if entity is an animal (cows, pigs, sheep, etc.)
		if (entity instanceof Animal) {
			return false;
		}

		// Check if entity is a villager
		if (entity instanceof Villager) {
			return false;
		}

		// Check if entity is a player
		if (entity instanceof Player) {
			// Players are not "neutral" in the traditional sense
			// but you might want to handle them differently
			return false;
		}

		// Check if entity has the kisatsutai (demon slayer corps) tag
		// These are allies and should not be attacked
		if (entity.getPersistentData().getBoolean("kisatsutai")) {
			return true;
		}

		// Check if entity is not a demon and not aggressive
		if (!isDemon(entity)) {
			// If it's a mob, check if it has no target (not aggressive)
			if (entity instanceof Mob mob) {
				return mob.getTarget() == null;
			}
			// Non-mob living entities are generally neutral
			return true;
		}

		return false;
	}

	/**
	 * Checks if one entity is angry at (targeting) another entity.
	 * This is useful for determining if a mob is hostile towards a specific entity.
	 *
	 * REDUNDANCY: Also checks damage history as a fallback. If the entities have
	 * recently damaged each other, they are considered to be in combat.
	 *
	 * @param entity The entity that might be angry
	 * @param target The entity that might be targeted
	 * @return true if entity is targeting target, false otherwise
	 */
	public static boolean isAngry(LivingEntity entity, LivingEntity target) {
		if (entity == null || target == null) {
			return false;
		}

		// Check if the entity is a Mob and if its target matches
		if (entity instanceof Mob mob) {
			LivingEntity mobTarget = mob.getTarget();
			if (mobTarget != null && mobTarget.equals(target)) {
				return true;
			}
		}

		// Check the cnt_target NBT value (KnY mod uses this to track targeting)
		// If cnt_target > 6, the entity is actively targeting something
		double cntTarget = entity.getPersistentData().getDouble("cnt_target");
		if (cntTarget > 6.0) {
			// The entity is targeting something, but we need to verify it's the right target
			// This is a heuristic - in the original mod, cnt_target > 6 means active targeting
			if (entity instanceof Mob mob) {
				if (mob.getTarget() != null && mob.getTarget().equals(target)) {
					return true;
				}
			}
		}

		// REDUNDANCY CHECK: Check damage history
		// If these entities have recently damaged each other, they are in combat
		if (DamageTracker.hasDamageHistory(entity, target)) {
			return true;
		}

		return false;
	}

	/**
	 * Checks if an entity is currently targeting any entity (is aggressive).
	 *
	 * @param entity The entity to check
	 * @return true if the entity is targeting something, false otherwise
	 */
	public static boolean isAngry(LivingEntity entity) {
		if (entity == null) {
			return false;
		}

		// Check if the entity is a Mob with a target
		if (entity instanceof Mob mob) {
			return mob.getTarget() != null;
		}

		// Check the cnt_target NBT value
		double cntTarget = entity.getPersistentData().getDouble("cnt_target");
		return cntTarget > 6.0;
	}

	/**
	 * Checks if an entity should be considered hostile/attackable.
	 * This combines demon checking with aggression checking.
	 *
	 * @param entity The entity to check
	 * @param attacker The entity that would be attacking (to check targeting)
	 * @return true if the entity should be attackable, false otherwise
	 */
	public static boolean isHostile(LivingEntity entity, LivingEntity attacker) {
		if (entity == null) {
			return false;
		}

		// Don't attack self
		if (entity.equals(attacker)) {
			return false;
		}

		// Demons are always hostile
		if (isDemon(entity)) {
			return true;
		}

		// Neutral entities are not hostile
		if (isNeutral(entity)) {
			return false;
		}

		// If the entity is angry at the attacker, it's hostile
		if (isAngry(entity, attacker)) {
			return true;
		}

		// Otherwise, not hostile
		return false;
	}

	/**
	 * Applies damage from a source entity to a target entity.
	 *
	 * REDUNDANCY: Uses multiple checks to determine if demon slayers should damage each other:
	 * 1. Traditional isAngry() check (mob targeting)
	 * 2. Damage history tracking (30-second window)
	 * 3. If either indicates they are in combat, damage is allowed
	 *
	 * @param source The entity dealing the damage
	 * @param target The entity receiving the damage
	 * @param damage The amount of damage to deal
	 * @return true if damage was applied, false otherwise
	 */
	public static boolean hurt(LivingEntity source, LivingEntity target, float damage) {
		return hurt(source, target, damage, false);
	}


	/**
	 * Applies damage from a source entity to a target entity with optional invulnerability frame reset.
	 *
	 * REDUNDANCY: Uses multiple checks to determine if demon slayers should damage each other:
	 * 1. Traditional isAngry() check (mob targeting)
	 * 2. Damage history tracking (30-second window)
	 * 3. If either indicates they are in combat, damage is allowed
	 *
	 * When the source is not a player, damage is scaled based on difficulty:
	 * - Peaceful: 0.7x
	 * - Easy: 1.0x
	 * - Normal: 1.25x
	 * - Hard: 1.5x
	 *
	 * @param source The entity dealing the damage
	 * @param target The entity receiving the damage
	 * @param damage The amount of damage to deal
	 * @param resetInvulnerability If true, resets the target's invulnerability frames before applying damage,
	 *                            allowing the target to be damaged even if they were recently hit (within 10 ticks)
	 * @return true if damage was applied, false otherwise
	 */
	public static boolean hurt(LivingEntity source, LivingEntity target, float damage, boolean resetInvulnerability) {
		if (source == null || target == null) {
			return false;
		}

		if (source instanceof DemonSlayerEntity demonSlayer && !canDemonSlayerDamageTarget(demonSlayer, target)) {
			return false;
		}

		// Reset invulnerability frames if requested
		// This allows the target to take damage even if they were recently damaged
		if (resetInvulnerability) {
			target.invulnerableTime = 0;
		}

		// Apply difficulty scaling if source is not a player
		float scaledDamage = calculateScaledDamage(source, damage);
		scaledDamage = applyMidasBonus(source, target, scaledDamage);

		if (isDemonSlayer(source) && isDemonSlayer(target)) {
			// Demon slayers shouldn't damage other demon slayers with their abilities by accident

			// REDUNDANCY: Check both traditional anger detection AND damage history
			boolean traditionallyAngry = isAngry(source, target);
			boolean hasRecentCombat = DamageTracker.hasDamageHistory(source, target);

			// If either check indicates combat, allow damage
			if (traditionallyAngry || hasRecentCombat) {
				// They are fighting - damage is allowed
				boolean damaged = target.hurt(DamageCalculator.getDamageSource(source), scaledDamage);
				if (damaged) {
					DamageTracker.recordDamage(source, target);
				}
				return damaged;
			} else {
				// Not in combat - prevent friendly fire
				return false;
			}
		} else {
			// Not both demon slayers - apply damage normally
			boolean damaged = target.hurt(DamageCalculator.getDamageSource(source), scaledDamage);
			if (damaged) {
				DamageTracker.recordDamage(source, target);
			}
			return damaged;
		}
	}

	private static float applyMidasBonus(LivingEntity source, LivingEntity target, float damage) {
		if (source == null || target == null) {
			return damage;
		}

		CompoundTag tag = target.getPersistentData();
		if (!tag.hasUUID(NBT_MIDAS_OWNER)) {
			return damage;
		}

		if (!tag.getUUID(NBT_MIDAS_OWNER).equals(source.getUUID())) {
			return damage;
		}

		long expiresAt = tag.getLong(NBT_MIDAS_EXPIRES);
		if (expiresAt > 0L && source.level() != null && source.level().getGameTime() > expiresAt) {
			tag.remove(NBT_MIDAS_OWNER);
			tag.remove(NBT_MIDAS_EXPIRES);
			tag.remove(NBT_MIDAS_BONUS_MULTIPLIER);
			return damage;
		}

		float multiplier = Math.max(1.0F, tag.getFloat(NBT_MIDAS_BONUS_MULTIPLIER));
		return damage * multiplier;
	}

	/**
	 * Apply shared damage scaling for Damager-driven hits.
	 * Killing Intent grants +2% damage per level.
	 */
	public static float calculateScaledDamage(LivingEntity source, float baseDamage) {
		if (source == null) {
			return baseDamage;
		}

		// float scaled = baseDamage;
		// MobEffectInstance killingIntent = source.getEffect(ModEffects.KILLING_INTENT.get());
		// if (killingIntent != null) {
		// 	int level = killingIntent.getAmplifier() + 1;
		// 	scaled *= 1.0f + (0.02f * level);
		// }

		return DamageCalculator.calculateScaledDamage(source, baseDamage);
	}

	private static boolean canDemonSlayerDamageTarget(DemonSlayerEntity source, LivingEntity target) {
		if (source == null || target == null) {
			return false;
		}

		if (source == target) {
			return false;
		}

		// Primary aggro checks: either side is actively targeting the other.
		if (isAngry(source, target) || isAngry(target, source)) {
			return true;
		}

		// Explicit target/revenge references are treated as active combat intent.
		if (source.getTarget() == target || source.getLastHurtByMob() == target || target.getLastHurtByMob() == source) {
			return true;
		}

		// Fallback combat memory for AoE/ability overlap cases.
		return DamageTracker.hasDamageHistory(source, target);
	}
}
