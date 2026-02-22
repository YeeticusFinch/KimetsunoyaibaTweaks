package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.compat.ShoulderSurfingCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * Helper methods for setting entity velocity and rotation with proper synchronization
 * Works with any LivingEntity (players, mobs, custom entities)
 */
public class MovementHelper {

    /**
     * Set entity velocity with server synchronization
     * @param entity The entity to move
     * @param velocity The velocity vector
     */
    public static void setVelocity(LivingEntity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hasImpulse = true;
        entity.hurtMarked = true; // Force velocity sync to clients

        // Additional sync for server players
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    /**
     * Set entity velocity (shorthand for common cases)
     * @param entity The entity to move
     * @param x X velocity
     * @param y Y velocity
     * @param z Z velocity
     */
    public static void setVelocity(LivingEntity entity, double x, double y, double z) {
        setVelocity(entity, new Vec3(x, y, z));
    }

    /**
     * Add to entity velocity with server synchronization
     * @param entity The entity to move
     * @param deltaVelocity The velocity to add
     */
    public static void addVelocity(LivingEntity entity, Vec3 deltaVelocity) {
        setVelocity(entity, entity.getDeltaMovement().add(deltaVelocity));
    }

    /**
     * Add to entity velocity (shorthand)
     * @param entity The entity to move
     * @param dx X velocity to add
     * @param dy Y velocity to add
     * @param dz Z velocity to add
     */
    public static void addVelocity(LivingEntity entity, double dx, double dy, double dz) {
        addVelocity(entity, new Vec3(dx, dy, dz));
    }

    /**
     * Set entity rotation (yaw and pitch) with synchronization
     * Also rotates ShoulderSurfing camera if entity is a player
     */
    public static void setRotation(LivingEntity entity, float yaw, float pitch) {
        // --- Update entity state ---
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);

        // update "old" values too for smooth interpolation
        entity.yRotO = yaw;
        entity.xRotO = pitch;
        entity.yHeadRotO = yaw;

        entity.yBodyRot = yaw;

        // --- ShoulderSurfing camera integration (client-side only, players only) ---
        if (entity instanceof Player player && entity.level().isClientSide) {
            try {
            	ShoulderSurfingCompat.setShoulderCameraRotation(yaw, pitch);
            } catch (Exception e) {
                // ShoulderSurfing not available or API changed, skip camera rotation
            }
        }

        // --- Sync to ALL clients (including the player's own client) ---
        if (entity instanceof ServerPlayer serverPlayer) {
            // Send to ALL clients so the player's own client receives it
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket(
                    entity.getUUID(), yaw, pitch, yaw
                )
            );
        }
    }

    /**
     * Make entity look at a specific position
     * @param entity The entity to rotate
     * @param target The position to look at
     */
    public static void lookAt(LivingEntity entity, Vec3 target) {
        Vec3 lookDir = target.subtract(entity.position()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(lookDir.y));
        setRotation(entity, yaw, pitch);
    }
    
    /**
     * Make entity look at a specific position
     * @param entity The entity to rotate
     * @param target The position to look at
     */
    public static void lookAtNoY(LivingEntity entity, Vec3 target) {
        Vec3 lookDir = target.subtract(entity.position()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        //float pitch = (float) Math.toDegrees(-Math.asin(lookDir.y));
        Vec3 normalized = entity.getLookAngle();
        float pitch = (float) Math.toDegrees(-Math.asin(normalized.y));
        setRotation(entity, yaw, pitch);
    }


    /**
     * Make entity look in a specific direction
     * @param entity The entity to rotate
     * @param direction The direction vector to look towards
     */
    public static void lookInDirection(LivingEntity entity, Vec3 direction) {
        Vec3 normalized = direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-normalized.x, normalized.z));
        float pitch = (float) Math.toDegrees(-Math.asin(normalized.y));
        setRotation(entity, yaw, pitch);
    }

    /**
     * Force a non-player entity to look at its current combat target, if any.
     * Does nothing for players or if no target exists.
     */
    public static void lookAtTarget(LivingEntity entity) {
        // Only for non-players
        if (entity instanceof Player) {
            return;
        }

        // Only mobs have a combat target
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            // Look at target's eye position for a natural head tilt
            Vec3 targetPos = target.position().add(0, target.getEyeHeight(), 0);
            lookAt(entity, targetPos);
        }
    }

    /**
     * Move entity towards a target position with specified speed
     * @param entity The entity to move
     * @param target Target position
     * @param speed Movement speed multiplier
     */
    public static void moveTowards(LivingEntity entity, Vec3 target, double speed) {
        Vec3 direction = target.subtract(entity.position());
        Vec3 velocity = direction.normalize().scale(speed);
        setVelocity(entity, velocity);
    }

    /**
     * Calculate circular movement velocity
     * @param center Center of the circle
     * @param radius Radius of the circle
     * @param angle Current angle in radians (0 = +X axis, increases counterclockwise when viewed from above)
     * @param angularVelocity Angular velocity (radians per tick, positive = counterclockwise)
     * @return Velocity vector for circular movement
     */
    public static Vec3 calculateCircularVelocity(Vec3 center, double radius, double angle, double angularVelocity) {
        // For circular motion, velocity is perpendicular to radius
        // Tangent direction for counterclockwise motion: (-sin(θ), 0, cos(θ))
        // Linear speed = radius * angular velocity
        double linearSpeed = radius * Math.abs(angularVelocity);

        // Tangent direction (perpendicular to radius, counterclockwise if angularVelocity > 0)
        double tangentX = -Math.sin(angle) * Math.signum(angularVelocity);
        double tangentZ = Math.cos(angle) * Math.signum(angularVelocity);

        return new Vec3(tangentX * linearSpeed, 0, tangentZ * linearSpeed);
    }

    /**
     * Calculate the position on a circle at a given angle
     * @param center Center of the circle
     * @param radius Radius of the circle
     * @param angle Angle in radians
     * @return Position on the circle
     */
    public static Vec3 calculateCirclePosition(Vec3 center, double radius, double angle) {
        double x = center.x + radius * Math.cos(angle);
        double z = center.z + radius * Math.sin(angle);
        return new Vec3(x, center.y, z);
    }

    // UUID for step height attribute modifier (consistent across all breathing forms)
    private static final UUID STEP_HEIGHT_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    /**
     * Set entity's step-up height using Forge's attribute system (ability to climb blocks)
     * Uses attributes which properly sync and reset, unlike setMaxUpStep() which doesn't decrease properly.
     * @param entity The entity
     * @param stepHeight Maximum step height in blocks (default is 0.6, this adds to that base value)
     */
    public static void setStepHeight(LivingEntity entity, float stepHeight) {
        AttributeInstance attribute = entity.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attribute == null) {
            System.err.println("Warning: Entity " + entity.getName().getString() + " does not have STEP_HEIGHT_ADDITION attribute");
            return;
        }

        // Remove existing modifier if present
        attribute.removeModifier(STEP_HEIGHT_MODIFIER_UUID);

        // Calculate the addition needed (stepHeight is total desired, base is 0.6)
        // For example: if stepHeight=3, we want to add 2.4 to the base 0.6
        double addition = stepHeight - 0.6;

        if (addition > 0.1) {
            // Add new modifier with the calculated addition
            AttributeModifier modifier = new AttributeModifier(
                STEP_HEIGHT_MODIFIER_UUID,
                "Breathing form step height",
                addition,
                AttributeModifier.Operation.ADDITION
            );
            attribute.addTransientModifier(modifier);
            Log.debug("Set step height to " + stepHeight + " (added " + addition + " to base)");
        } else {
            // If addition is 0 or negative, just remove the modifier (reset to default 0.6)
            Log.debug("Reset step height to default (0.6)");
        }
    }

    /**
     * Reset entity's step height to default (0.6 blocks)
     * @param entity The entity
     */
    public static void resetStepHeight(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attribute != null) {
            attribute.removeModifier(STEP_HEIGHT_MODIFIER_UUID);
            Log.debug("Reset step height to default");
        }
    }

	public static void stepUp(LivingEntity entity, double vx, double vy, double vz) {
		// Calculate the block position in front of the entity
		Vec3 targetPos = entity.position().add(new Vec3(vx, vy, vz).normalize());

		BlockPos targetBlockPos = BlockPos.containing(targetPos);
		BlockPos aboveBlockPos = targetBlockPos.above();

		Level level = entity.level();
		BlockState targetBlock = level.getBlockState(targetBlockPos);
		BlockState aboveBlock = level.getBlockState(aboveBlockPos);

		// A block is "solid" if it has a collision shape (not air, water, etc.)
		boolean targetIsSolid = !targetBlock.getCollisionShape(level, targetBlockPos).isEmpty();

		// A block is "passable" if it has no collision shape (air, water, grass, etc.)
		boolean aboveIsPassable = aboveBlock.getCollisionShape(level, aboveBlockPos).isEmpty();

		if (targetIsSolid && aboveIsPassable) {
		    entity.teleportTo(entity.getX(), entity.getY() + 1.0, entity.getZ());
		}
	}
}
