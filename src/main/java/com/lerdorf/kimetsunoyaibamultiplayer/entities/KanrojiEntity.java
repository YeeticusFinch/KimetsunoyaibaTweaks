package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedMistForms;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;

/**
 * Mitsuri Kanroji - Love Hashira Wields nichirinsword_kanroji, uses Enhanced
 * Love Breathing (all 6 forms) 
 * Has 140 HP and enhanced stats based on Hashira-level
 * attributes
 *
 * Stats from NBT data: - HP: 140 (base) - Speed 2 (amplifier 0), Strength 9
 * (amplifier 10), Resistance 3 (amplifier 2) - Movement speed: 0.32 + 60% from
 * Speed 2 - Attack damage: 1.0 + 36.0 from Strength 9 - Armor: 19.0, Armor
 * toughness: 2.0
 */
public class KanrojiEntity extends BreathingSlayerEntity {
    private static final ResourceLocation DEMONIZED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "textures/entity/oni_kanroji.png");
    private static final ResourceLocation DEMONIZED_EYES_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "textures/entity/oni_kanroji_eyes.png");
	
	private enum MarkState {
        NORMAL,         // No mark
        TRANSFORMING,   // Currently transforming (5 seconds)
        TRANSFORMED     // Mark activated
    }
	
	private int breathingFormUsageCounter = 0; // Track when to use breathing forms
	private int lastDamageTick = -1000; // Server tick when last damaged
	
	private MarkState markState = MarkState.NORMAL; // Demon slayer mark state
    private int transformationTimer = 0;            // Timer for transformation (100 ticks = 5 seconds)
    private static final int TRANSFORMATION_DURATION = 100; // 5 seconds in ticks

	public KanrojiEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
		super(entityType, level);

		// Force set max health immediately after parent constructor
		if (!level.isClientSide) {
			AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null && maxHealth.getBaseValue() != 140.0D) {
				maxHealth.setBaseValue(140.0D);
			}
		}
	}

	@Override
	public void tick() {
		super.tick();

		// Safety check: ensure health never drops below intended max
		if (!this.level().isClientSide) {
			AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null && maxHealth.getBaseValue() != 140.0D) {
				maxHealth.setBaseValue(140.0D);
				this.setHealth(140.0F);
				Log.debug("[Kanroji] WARNING: Max health was changed, forcing back to 140");
			}
		}

		// Server-side AI behaviors
		if (!this.level().isClientSide) {
			boolean hasTarget = this.getTarget() != null;

			// Set sprinting flag for animation (unless transforming)
			if (markState != MarkState.TRANSFORMING) {
				this.setSprinting(hasTarget);
			} else {
				this.setSprinting(false); // No sprinting during transformation
			}

			// Activate Demon Slayer Mark when below 50% HP
			if (markState == MarkState.NORMAL && this.getHealth() <= (this.getMaxHealth() * 0.5f)) {
				activateDemonSlayerMark();
			}

			// Handle transformation timer
			if (markState == MarkState.TRANSFORMING) {
				transformationTimer++;

				// Spawn particle effects during transformation
				spawnTransformationParticles();

				// Play guardian laser charging sound every 20 ticks during transformation
				if (transformationTimer % 20 == 0) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
						SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.5F);
				}

				// Complete transformation after 5 seconds
				if (transformationTimer >= TRANSFORMATION_DURATION) {
					completeDemonSlayerMark();
				}
			}

			// Let BreathingFormAttackGoal handle ability usage; only manage sprint toggle
			// here
			if (!hasTarget) {
				breathingFormUsageCounter = 0;
			}
		}
	}

	/**
     * Starts the demon slayer mark transformation.
     * Kanroji kneels for 5 seconds with particle effects, then gains the mark.
     */
    private void activateDemonSlayerMark() {
        markState = MarkState.TRANSFORMING;
        transformationTimer = 0;

        Log.debug("[Kanroji] Starting demon slayer mark transformation at " + this.getHealth() + " HP");

        // Force kneel animation for the full transformation duration
        this.playGeckoAnimation("kneel", TRANSFORMATION_DURATION);

        // Apply super resistance (level 100) for 5 seconds
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TRANSFORMATION_DURATION, 99, false, false));

        // Apply immovable effect (kimetsunoyaiba:immovable) for 5 seconds
        net.minecraft.world.effect.MobEffect immovableEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "immovable"));
        if (immovableEffect != null) {
            this.addEffect(new MobEffectInstance(immovableEffect, TRANSFORMATION_DURATION, 0, false, false));
        }

        // Stop all current goals/movement
        this.getNavigation().stop();
        if (this.getTarget() != null) {
            this.setTarget(null); // Temporarily clear target to prevent attacks
        }
    }

	@Override
	public BreathingTechnique getBreathingTechnique() {
		return EnhancedLoveForms.createLoveBreathing();
	}

	/**
	 * Override to prevent Kanroji from being affected by power level changes
	 */
	@Override
	public void setPowerLevel(int level) {
		// Do nothing - Kanroji is always max power (Hashira)
	}

	/**
	 * Override to always return max power level
	 */
	@Override
	public int getPowerLevel() {
		return 4; // Hashira = max power
	}

	@Override
	public ItemStack getEquippedSword() {
		return new ItemStack(ModItems.NICHIRINSWORD_KANROJI.get());
	}

    @Override
    public ResourceLocation getDemonizedTextureOverride() {
        return DEMONIZED_TEXTURE;
    }

    @Override
    public ResourceLocation getDemonizedEyesTextureOverride() {
        return DEMONIZED_EYES_TEXTURE;
    }

	@Override
	public ItemStack[] getArmorEquipment() {
		// Load kanroji armor from kimetsunoyaiba mod
		
		/*
		Item uniformHelmet = ForgeRegistries.ITEMS
				.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_kanroji_helmet"));
		Item uniformChest = ForgeRegistries.ITEMS
				.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_kanroji_chestplate"));
		Item uniformLegs = ForgeRegistries.ITEMS
				.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_kanroji_leggings"));
		Item uniformBoots = ForgeRegistries.ITEMS
				.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_kanroji_boots"));

		return new ItemStack[] { uniformHelmet != null ? new ItemStack(uniformHelmet) : ItemStack.EMPTY,
				uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY,
				uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY,
				uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY };
				*/
		return new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY}; // no armor
	}

	/**
	 * Create attributes for Kanroji Tokito (Hashira-level stats) Based on NBT data
	 * from the original entity
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 140.0D) // Hashira health
				.add(Attributes.ATTACK_DAMAGE, 1.0D) // Base damage (Strength effect adds the rest)
				.add(Attributes.MOVEMENT_SPEED, 0.18D) // Fast movement (Speed effect multiplies this)
				.add(Attributes.ATTACK_SPEED, 16.0D) // Extremely fast attack speed baseline
				.add(Attributes.ARMOR, 16.0D) // From armor equipment
				.add(Attributes.ARMOR_TOUGHNESS, 3.0D) // From armor equipment
				.add(Attributes.FOLLOW_RANGE, 64.0D) // Same as base slayers
				.add(ForgeMod.ENTITY_REACH.get(), 15.0D) // Whip sword has 15 block reach
				.add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.8D); // Higher jumps for acrobatic combat
	}

	@Override
	@Nullable
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
			@Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
		// DO NOT call super.finalizeSpawn() - it applies random power levels
		// Instead, manually apply equipment and Hashira-level effects

		// Equip sword in main hand
		this.setItemSlot(EquipmentSlot.MAINHAND, getEquippedSword());

		// Equip armor
		//ItemStack[] armor = getArmorEquipment();
		//this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
		//this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
		//this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
		//this.setItemSlot(EquipmentSlot.FEET, armor[3]);

		// Prevent equipment from dropping
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setDropChance(slot, 0.2F);
		}

		// Set to full health (140 HP)
		this.setHealth(140.0F);

		// Apply Hashira-level permanent effects (matching NBT data)
		// Speed 2 (amplifier 0 = Speed I, which is Speed 2 in display)
		this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));

		// Strength 9 (amplifier 8 = Strength IX)
		this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 8, true, false));

		// Resistance 3 (amplifier 2 = Resistance III)
		this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, MAX_RESISTANCE_AMPLIFIER, true, false));

		// Set persistence required (don't despawn)
		this.setPersistenceRequired();

		// Increase attack speed significantly
		net.minecraft.world.entity.ai.attributes.AttributeInstance atkSpd = this.getAttribute(Attributes.ATTACK_SPEED);
		if (atkSpd != null) {
			// Set an even higher base attack speed for rapid swings
			atkSpd.setBaseValue(14.0D);
		}

		return spawnData;
	}

	@Override
	protected void registerGoals() {
		// Don't call super.registerGoals() - we need to customize the goals

		// Priority 0: Breathing form attacks (highest priority)
		this.goalSelector.addGoal(0, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.BreathingFormAttackGoal(this));

		// Priority 1: Float in water
		this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));

		// Priority 2: Side flip dodge (damage mitigation + counter-attack)
		this.goalSelector.addGoal(2, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.KanrojiSideFlipDodgeGoal(this));

		// Priority 3: Range management backstep (when too close to target)
		this.goalSelector.addGoal(3, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.KanrojiBackstepGoal(this));

		// Priority 4: Front flip (aggressive gap closer)
		this.goalSelector.addGoal(4, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.KanrojiFrontFlipGoal(this));

		// Priority 5: CUSTOM Kanroji melee attack with whip range
		this.goalSelector.addGoal(5, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.KanrojiAnimatedMeleeAttackGoal(this, 1.0D, false));

		// Priority 6: Random stroll
		this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));

		// Priority 7: Look at player
		this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 8.0F));

		// Priority 8: Random look around
		this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

		// Target goals
		this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));

		// Target demons from kimetsunoyaiba mod (using comprehensive demon detection)
		this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
			this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
			DemonSlayerAggroHandler::isDemonTarget));

		// Target players who are demons (check NBT data)
		this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
			this, net.minecraft.world.entity.player.Player.class, 10, true, false,
			(player) -> player.getPersistentData().getBoolean("oni")));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		// Mark this as Kanroji so we can restore effects on load
		tag.putBoolean("IsKanroji", true);
		tag.putBoolean("KanrojiMark", markState == MarkState.TRANSFORMED);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		// Restore Hashira-level effects if this is Kanroji
		if (tag.getBoolean("IsKanroji")) {
			this.setHealth(140.0F);
			this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, true, false));
			this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 10, true, false));
			this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, MAX_RESISTANCE_AMPLIFIER, true, false));
		}
		//this.markActivated = tag.getBoolean("KanrojiMark");
		if (tag.getBoolean("KanrojiMark"))
			markState = MarkState.TRANSFORMED;
	}

	/**
     * Completes the demon slayer mark transformation.
     * Activates the Love Breathing Demon Slayer Mark and applies permanent buffs.
     */
    private void completeDemonSlayerMark() {
        markState = MarkState.TRANSFORMED;

        Log.debug("[Kanroji] Demon slayer mark transformation complete!");

        // Play dramatic wither spawn sound to signify transformation completion
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);

        // Mark the entity's NBT so the render layer knows to show the mark
        this.getPersistentData().putBoolean("KanrojiMark", true);

        // Apply potion_demon_slayer_mark with effectively infinite duration
        net.minecraft.world.effect.MobEffect markEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "potion_demon_slayer_mark"));
        if (markEffect != null) {
            this.addEffect(new MobEffectInstance(markEffect, Integer.MAX_VALUE, 0, true, false));
        }

        // Upgrade Strength from 9 to 10 (amplifier 8 -> 9)
        this.removeEffect(MobEffects.DAMAGE_BOOST);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 9, true, false));

        // Upgrade Speed from 2 to 3 (amplifier 0 -> 1) - Love Breathing enhances agility
        this.removeEffect(MobEffects.MOVEMENT_SPEED);
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1, true, false));

        // Restore base Resistance 3 (was temporarily boosted during transformation)
        this.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, MAX_RESISTANCE_AMPLIFIER, true, false));

        // Animation will naturally transition back to idle when the kneel animation expires
    }
	
	/**
     * Spawns particle effects during the transformation:
     * - Spiral of heart particles circling around Kanroji (Love Breathing theme)
     * - Waves of pink particles traveling across the ground from her feet
     */
    private void spawnTransformationParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = this.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // Spawn spiral of heart particles every tick (Love Breathing theme)
        double angle = (transformationTimer * 0.3) % (2 * Math.PI); // Rotate over time
        double radius = 2.5;
        double height = (transformationTimer % 40) * 0.1; // Spiral upward

        for (int i = 0; i < 4; i++) {
            double spiralAngle = angle + (i * 2 * Math.PI / 4);
            double offsetX = Math.cos(spiralAngle) * radius;
            double offsetZ = Math.sin(spiralAngle) * radius;

            // Hearts from vanilla Minecraft
            serverLevel.sendParticles(
                ParticleTypes.HEART,
                x + offsetX, y + height, z + offsetZ,
                1, 0.1, 0.1, 0.1, 0.02
            );
        }

        // Spawn ground wave pink particles every 10 ticks (multiple shockwaves)
        if (transformationTimer % 10 == 0) {
            double waveRadius = (transformationTimer % 40) * 0.15; // Expand from feet

            for (int i = 0; i < 20; i++) {
                double waveAngle = (i / 20.0) * 2 * Math.PI;
                double waveX = x + Math.cos(waveAngle) * waveRadius;
                double waveZ = z + Math.sin(waveAngle) * waveRadius;

                // Use cherry leaves particles for pink effect (Love Breathing theme)
                serverLevel.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    waveX, y + 0.1, waveZ,
                    3, 0.1, 0.05, 0.1, 0.02
                );

                // Add some pink sparkles for extra flair (using witch particles)
                serverLevel.sendParticles(
                    ParticleTypes.WITCH,
                    waveX, y + 0.1, waveZ,
                    2, 0.15, 0.05, 0.15, 0.01
                );
            }
        }

        // Every 5 ticks, spawn some extra hearts floating upward for dramatic effect
        if (transformationTimer % 5 == 0) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (this.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetZ = (this.getRandom().nextDouble() - 0.5) * 2.0;

                serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    x + offsetX, y + 0.5, z + offsetZ,
                    1, 0.1, 0.2, 0.1, 0.05
                );
            }
        }
    }

	/**
	 * Override animation controller to use sprint animation when in combat
	 * and kneel animation during transformation
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Main controller - handles ALL animations (walk, idle, sprint, attacks,
		// abilities, transformation)
		controllers.add(new AnimationController<>(this, "controller", 0, state -> {
			// Death animation (highest priority)
			if (this.isDeadOrDying()) {
				return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
			}

			// Transformation kneel animation (second highest priority)
			if (markState == MarkState.TRANSFORMING) {
				return state.setAndContinue(RawAnimation.begin().thenPlay("kneel"));
			}

			String anim = getCurrentAnimation();
			int animTicks = getAnimationTicks();

			// Attack and ability animations (play once)
			if (animTicks > 0 && !anim.equals("idle") && !anim.equals("walk") && !anim.equals("walk_female") && !anim.equals("sprint")) {
				return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
			}

			// Movement animations (loop)
			if (state.isMoving()) {
				// Use sprint animation when sprinting (in combat with target)
				// Check both isSprinting() AND hasTarget to ensure we sprint during combat
				boolean shouldSprint = this.isSprinting()
						|| (this.getTarget() != null && this.getDeltaMovement().horizontalDistanceSqr() > 0.01);

				if (shouldSprint) {
					return state.setAndContinue(RawAnimation.begin().thenLoop("sprint"));
				} else {
					return state.setAndContinue(RawAnimation.begin().thenLoop("walk_female"));
				}
			} else {
				return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
			}
		}));
	}

	/**
	 * Increase movement speed when sprinting (in combat)
	 */
	@Override
	public double getMyRidingOffset() {
		return super.getMyRidingOffset();
	}

	/**
	 * Override to apply sprint speed modifier
	 */
	@Override
	public float getSpeed() {
		float baseSpeed = super.getSpeed();

		// Apply 50% speed boost when sprinting (in combat)
		if (this.isSprinting() && this.getTarget() != null) {
			return baseSpeed * 1.5F;
		}

		return baseSpeed;
	}

	// Reduce fall damage taken (80% resistance)
	@Override
	public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return super.causeFallDamage(fallDistance, damageMultiplier * 0.2F, damageSource);
	}

	// Custom equipment drops with Looting scaling
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);

		double baseChance = 0.10; // 10%
		double chance = Math.min(1.0, baseChance + (0.10 * Math.max(0, lootingLevel)));

		RandomSource rand = this.getRandom();

		// Sword (main hand)
		if (!this.getMainHandItem().isEmpty() && rand.nextDouble() < chance) {
			this.spawnAtLocation(this.getMainHandItem().copy());
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// Reduce fall damage to 30% (acrobatic fighting style)
		if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
			amount *= 0.3f; // Only take 30% fall damage
			Log.debug("[Kanroji] Reduced fall damage to 30%: {} damage", amount);
		}

		boolean result = super.hurt(source, amount);
		if (result && !this.level().isClientSide) {
			this.lastDamageTick = this.tickCount;
		}
		return result;
	}

	public boolean wasRecentlyDamaged(int withinTicks) {
		return this.tickCount - this.lastDamageTick <= withinTicks;
	}

	/**
	 * Checks if Kanroji is currently transforming (cannot move or use abilities)
	 */
	public boolean isTransforming() {
		return markState == MarkState.TRANSFORMING;
	}

	/**
	 * Checks if Kanroji has her demon slayer mark active
	 */
	public boolean hasMarkActivated() {
		return markState == MarkState.TRANSFORMED;
	}
}
