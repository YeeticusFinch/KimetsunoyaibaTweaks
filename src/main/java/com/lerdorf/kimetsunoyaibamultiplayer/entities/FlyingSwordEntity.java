package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.mojang.math.Transformation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for creating and managing flying sword projectiles using
 * ItemDisplay entities Used for Frost Breathing Sixth Form: Polar Mark
 */
public class FlyingSwordEntity {

	/**
	 * Creates a flying sword projectile that flies forward and returns to the
	 * thrower
	 * 
	 * @param level      The world
	 * @param thrower    The entity throwing the sword
	 * @param swordStack The sword item being thrown
	 */
	public static void create(Level level, LivingEntity thrower, ItemStack swordStack) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		Display.ItemDisplay display = createItemDisplay(level, swordStack, thrower.getX(), thrower.getY(), thrower.getZ(), 1.5f, 1.5f, 1.5f);

		// position
		Vec3 start = thrower.getEyePosition().subtract(0, 0.1, 0);
		display.setPos(start.x, start.y, start.z);

		// disable interpolation
		setInterpolation(display,0,0);

		serverLevel.addFreshEntity(display);

		// Store references
		final UUID displayUUID = display.getUUID();
		final Vec3 velocity = thrower.getLookAngle().scale(1.5);
		final int maxTicks = 40; // 2 seconds
		final int[] tickCounter = { 0 };

		if (Config.logDebug) {
			Log.debug("Created flying sword ItemDisplay with UUID: {}", displayUUID);
		}

		// Animate the sword flying
		AbilityScheduler.scheduleRepeating(thrower, () -> {
			int currentTick = tickCounter[0]++;

			// Find the display entity
			Display.ItemDisplay sword = (Display.ItemDisplay) serverLevel.getEntity(displayUUID);
			if (sword == null) {
				if (Config.logDebug) {
					Log.debug("Flying sword display entity not found, stopping");
				}
				return;
			}

			// Update position with physics
			Vec3 currentPos = sword.position();
			double gravity = 0.05 * currentTick; // Accumulate gravity
			Vec3 newPos = currentPos.add(velocity.x, velocity.y - gravity, velocity.z);
			sword.setPos(newPos.x, newPos.y, newPos.z);

			// Rotate to face movement direction
			Vec3 motion = new Vec3(velocity.x, velocity.y - gravity, velocity.z);
			if (motion.lengthSqr() > 0.01) {
				double yaw = Math.atan2(motion.z, motion.x);
				double pitch = Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z));

				// Create rotation quaternion (rotate 90 degrees on Y to point forward)
				Quaternionf rotation = new Quaternionf().rotateY((float) yaw + (float) Math.PI / 2)
						.rotateX((float) pitch);

				setTransformation(sword, new com.mojang.math.Transformation(
						new Vector3f(0, 0, 0), rotation, new Vector3f(1.5f, 1.5f, 1.5f), new Quaternionf()));
			}

			// Check for entity collisions
			AABB hitBox = sword.getBoundingBox().inflate(1.5);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
					e -> e != thrower && e.isAlive());

			for (LivingEntity target : targets) {
				float damage = DamageCalculator.calculateScaledDamage(thrower, 7.0F);
				Damager.hurt(thrower, target, damage);
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2));
				target.setTicksFrozen(target.getTicksFrozen() + 400);
			}

			// Spawn trail particles
			serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, newPos.x, newPos.y, newPos.z, 2, 0.1, 0.1, 0.1, 0);

			// Remove and return sword at end
			if (currentTick >= maxTicks - 1) {
				sword.discard();

				// Return sword to player
				if (thrower instanceof Player player) {
					if (!player.getInventory().add(swordStack)) {
						player.drop(swordStack, false);
					}

					level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F,
							1.0F);
				}

				if (Config.logDebug) {
					Log.debug("Flying sword returned to owner");
				}
			}
		}, 1, maxTicks);
	}
	
	public static Display.ItemDisplay createItemDisplay(Level level, ItemStack item, double x, double y, double z, float sx, float sy, float sz) {
	    Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
	    display.moveTo(x, y, z);

	    // --- Set item using entityData (the private method just does this internally) ---
	    display.getEntityData().set(
	        getDataItemStackAccessor(),
	       item.copy()
	    );

	    // --- Set transformation ---
	    Transformation transform = new Transformation(
	        new Vector3f(0f, 0f, 0f),       // translation
	        new Quaternionf(),               // left rotation
	        new Vector3f(sx, sy, sz),        // scale
	        new Quaternionf()                // right rotation
	    );
	    setTransformation(display, transform);

	    level.addFreshEntity(display);
	    return display;
	}
	
	private static void setInterpolation(Display display, int startDelta, int duration) {
	    try {
	        var setDuration = Display.class.getDeclaredMethod("setInterpolationDuration", int.class);
	        var setStart = Display.class.getDeclaredMethod("setInterpolationDelay", int.class);
	        setDuration.setAccessible(true);
	        setStart.setAccessible(true);
	        setDuration.invoke(display, duration);
	        setStart.invoke(display, startDelta);
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to call interpolation methods", e);
	    }
	}
	
	/*
	private static EntityDataAccessor<ItemStack> getDataItemStackAccessor() {
	    try {
	        Field f = Display.ItemDisplay.class.getDeclaredField("DATA_ITEM_STACK_ID");
	        f.setAccessible(true);
	        @SuppressWarnings("unchecked")
	        EntityDataAccessor<ItemStack> accessor = (EntityDataAccessor<ItemStack>) f.get(null);
	        return accessor;
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to access DATA_ITEM_STACK_ID", e);
	    }
	}*/
	
	@SuppressWarnings("unchecked")
	private static EntityDataAccessor<ItemStack> getDataItemStackAccessor() {
	    for (Field f : Display.ItemDisplay.class.getDeclaredFields()) {
	        if (EntityDataAccessor.class.isAssignableFrom(f.getType())) {
	            f.setAccessible(true);
	            try {
	                Object val = f.get(null);
	                // Confirm it’s the correct accessor type for ItemStack
	                if (val instanceof EntityDataAccessor<?> accessor) {
	                    // Check the generic target type via name guess
	                    if (f.getName().toLowerCase().contains("item_stack")) {
	                        return (EntityDataAccessor<ItemStack>) accessor;
	                    }
	                }
	            } catch (Exception ignored) {}
	        }
	    }
	    throw new RuntimeException("Could not find DATA_ITEM_STACK_ID accessor!");
	}

	private static void setTransformation(Display display, Transformation transform) {
	    try {
	        var m = Display.class.getDeclaredMethod("setTransformation", Transformation.class);
	        m.setAccessible(true);
	        m.invoke(display, transform);
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to call setTransformation()", e);
	    }
	}
}
