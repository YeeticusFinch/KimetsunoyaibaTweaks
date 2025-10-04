package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.compat.ShoulderSurfingCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Helper methods for setting player velocity and rotation with proper synchronization
 */
public class MovementHelper {

    /**
     * Set player velocity with server synchronization
     * @param player The player to move
     * @param velocity The velocity vector
     */
    public static void setVelocity(Player player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        player.hasImpulse = true;
        player.hurtMarked = true; // Force velocity sync to clients

        // Additional sync for server players
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    /**
     * Set player velocity (shorthand for common cases)
     * @param player The player to move
     * @param x X velocity
     * @param y Y velocity
     * @param z Z velocity
     */
    public static void setVelocity(Player player, double x, double y, double z) {
        setVelocity(player, new Vec3(x, y, z));
    }

    /**
     * Add to player velocity with server synchronization
     * @param player The player to move
     * @param deltaVelocity The velocity to add
     */
    public static void addVelocity(Player player, Vec3 deltaVelocity) {
        setVelocity(player, player.getDeltaMovement().add(deltaVelocity));
    }

    /**
     * Add to player velocity (shorthand)
     * @param player The player to move
     * @param dx X velocity to add
     * @param dy Y velocity to add
     * @param dz Z velocity to add
     */
    public static void addVelocity(Player player, double dx, double dy, double dz) {
        addVelocity(player, new Vec3(dx, dy, dz));
    }

    /**
     * Set player rotation (yaw and pitch) with synchronization
     * Also rotates ShoulderSurfing camera if available
     */
    public static void setRotation(Player player, float yaw, float pitch) {
        // --- Update entity state ---
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);

        // update "old" values too for smooth interpolation
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.yHeadRotO = yaw;
        
        player.yBodyRot = yaw;

        // --- ShoulderSurfing camera integration (client-side only) ---
        if (player.level().isClientSide) {
            try {
            	ShoulderSurfingCompat.setShoulderCameraRotation(yaw, pitch);
            } catch (Exception e) {
                // ShoulderSurfing not available or API changed, skip camera rotation
            }
        }

        // --- Sync to ALL clients (including the player's own client) ---
        if (player instanceof ServerPlayer serverPlayer) {
            // Send to ALL clients so the player's own client receives it
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.PlayerRotationSyncPacket(
                    player.getUUID(), yaw, pitch, yaw
                )
            );
        }
    }

    /**
     * Make player look at a specific position
     * @param player The player to rotate
     * @param target The position to look at
     */
    public static void lookAt(Player player, Vec3 target) {
        Vec3 lookDir = target.subtract(player.position()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-lookDir.x, lookDir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(lookDir.y));
        setRotation(player, yaw, pitch);
    }

    /**
     * Make player look in a specific direction
     * @param player The player to rotate
     * @param direction The direction vector to look towards
     */
    public static void lookInDirection(Player player, Vec3 direction) {
        Vec3 normalized = direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-normalized.x, normalized.z));
        float pitch = (float) Math.toDegrees(-Math.asin(normalized.y));
        setRotation(player, yaw, pitch);
    }

    /**
     * Move player towards a target position with specified speed
     * @param player The player to move
     * @param target Target position
     * @param speed Movement speed multiplier
     */
    public static void moveTowards(Player player, Vec3 target, double speed) {
        Vec3 direction = target.subtract(player.position());
        Vec3 velocity = direction.normalize().scale(speed);
        setVelocity(player, velocity);
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

    /**
     * Set player's step-up height (ability to climb blocks)
     * @param player The player
     * @param stepHeight Maximum step height in blocks (default is 0.6)
     */
    public static void setStepHeight(Player player, float stepHeight) {
        player.setMaxUpStep(stepHeight);
        // Note: Step height changes are not synced to clients by vanilla,
        // but the movement itself is synced, so this should work fine
    }

	public static void stepUp(Player player, double vx, double vy, double vz) {
		// Calculate the block position in front of the player
		Vec3 targetPos = player.position().add(new Vec3(vx, vy, vz).normalize());

		BlockPos targetBlockPos = BlockPos.containing(targetPos);
		BlockPos aboveBlockPos = targetBlockPos.above();

		Level level = player.level();
		BlockState targetBlock = level.getBlockState(targetBlockPos);
		BlockState aboveBlock = level.getBlockState(aboveBlockPos);

		// A block is “solid” if it has a collision shape (not air, water, etc.)
		boolean targetIsSolid = !targetBlock.getCollisionShape(level, targetBlockPos).isEmpty();

		// A block is “passable” if it has no collision shape (air, water, grass, etc.)
		boolean aboveIsPassable = aboveBlock.getCollisionShape(level, aboveBlockPos).isEmpty();

		if (targetIsSolid && aboveIsPassable) {
		    player.teleportTo(player.getX(), player.getY() + 1.0, player.getZ());
		}
	}
}
