package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraft.world.phys.Vec3;

public class FancyMath {
	
	
	public static Vec3 rotateYaw(Vec3 vec, float degrees) {

		Vec3 look = vec.normalize();
		// Convert 15 degrees to radians
		double angle = Math.toRadians(degrees);

		// Apply yaw rotation (around Y-axis)
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);

		// Rotate XZ components
		double newX = look.x * cos - look.z * sin;
		double newZ = look.x * sin + look.z * cos;

		// Keep same Y (we’re rotating horizontally)
		Vec3 rotated = new Vec3(newX, look.y, newZ).normalize().scale(vec.length());
		
		return rotated;
	}
}
