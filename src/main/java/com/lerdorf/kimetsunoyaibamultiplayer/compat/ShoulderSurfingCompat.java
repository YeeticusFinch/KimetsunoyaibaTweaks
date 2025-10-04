package com.lerdorf.kimetsunoyaibamultiplayer.compat;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;

import net.minecraftforge.fml.ModList;

public class ShoulderSurfingCompat {
    /** Is ShoulderSurfing mod present? */
    public static boolean isPresent() {
        return ModList.get().isLoaded("shouldersurfing");
    }

    /**
     * If ShoulderSurfing is present, set the shoulder camera rotation to match yaw/pitch.
     * Uses reflection to avoid compile-time dependency on ShoulderSurfing.
     *
     * IMPORTANT: do not call this on the server. Only call from client-side code.
     */
    public static void setShoulderCameraRotation(float yaw, float pitch) {
        if (!isPresent()) {
            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                Log.debug("ShoulderSurfing not present, skipping camera update");
            }
            return;
        }

        try {
            // Use reflection to access ShoulderSurfing API
            // Class: com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl
            Class<?> shoulderSurfingClass = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl");
            Object instance = shoulderSurfingClass.getMethod("getInstance").invoke(null);

            if (instance == null) {
                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    Log.debug("ShoulderSurfing instance is null");
                }
                return;
            }

            // Get camera object (returns ShoulderSurfingCamera)
            Object camera = shoulderSurfingClass.getMethod("getCamera").invoke(instance);
            if (camera != null) {
                // Set camera rotation using separate setYRot and setXRot methods
                Class<?> cameraClass = camera.getClass();
                cameraClass.getMethod("setYRot", float.class).invoke(camera, yaw);
                cameraClass.getMethod("setXRot", float.class).invoke(camera, pitch);

                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    Log.debug("ShoulderSurfing camera rotation updated: yaw={}, pitch={}", yaw, pitch);
                }
            } else {
                if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                    Log.debug("ShoulderSurfing camera is null");
                }
            }
        } catch (Exception ex) {
            // ShoulderSurfing API changed or not available - fail gracefully
            Log.warn("ShoulderSurfing present, but compat call failed: {}", ex.toString());
        }
    }
}