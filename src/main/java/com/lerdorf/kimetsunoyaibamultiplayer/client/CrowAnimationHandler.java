package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Client-side handler to attempt to trigger flying animations on kasugai crows
 * This uses reflection to try to set animation states on the crow entity
 */
public class CrowAnimationHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean reflectionFailed = false;

    /**
     * Attempts to set the crow to flying animation state
     * This is called server-side to manipulate the entity's state
     */
    public static void setFlyingAnimation(Entity entity, boolean flying) {
        if (reflectionFailed) {
            return; // Don't spam reflection attempts if we know it fails
        }

        try {
            // Minecraft entities have an EntityDataAccessor for various flags
            // Try to set the entity to "fall flying" state (like elytra)
            // This often triggers wing-spread animations

            // Attempt 1: Try to set fall flying flag (used by players with elytra)
            tryInvokeMethod(entity, "setSharedFlag", 7, flying); // Flag 7 is fall flying

            // Attempt 2: Check for flying-related methods
            tryInvokeMethod(entity, "setFlying", flying);
            tryInvokeMethod(entity, "setIsFlying", flying);

            // Attempt 3: Try parrot-like methods (crows might use similar model)
            if (flying) {
                tryInvokeMethod(entity, "setOnGround", false);
            }

            // Attempt 4: Check for flying field
            trySetField(entity, "flying", flying);
            trySetField(entity, "isFlying", flying);

            // Attempt 5: Try to manipulate entity pose
            trySetPose(entity, flying);

            if (Config.logDebug) {
                LOGGER.debug("Attempted to set flying animation for crow");
            }

        } catch (Exception e) {
            if (Config.logDebug) {
                LOGGER.warn("Could not set flying animation via reflection: {}", e.getMessage());
            }
            reflectionFailed = true;
        }
    }

    private static void trySetPose(Entity entity, boolean flying) {
        try {
            // Try to set entity pose to FALL_FLYING (wings spread)
            Class<?> poseClass = Class.forName("net.minecraft.world.entity.Pose");
            Object[] poses = poseClass.getEnumConstants();

            for (Object pose : poses) {
                if (pose.toString().contains("FALL_FLYING") || pose.toString().contains("FLYING")) {
                    tryInvokeMethod(entity, "setPose", pose);
                    if (Config.logDebug)
                    	LOGGER.info("Set entity pose to: {}", pose);
                    break;
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    private static void tryInvokeMethod(Object obj, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Boolean) {
                    paramTypes[i] = boolean.class;
                } else if (args[i] == null) {
                    paramTypes[i] = null; // Will try all methods with matching name
                } else {
                    paramTypes[i] = args[i].getClass();
                }
            }

            Method method;
            if (args.length == 0 || args[0] == null) {
                // Find any method with this name
                method = findMethodByName(obj.getClass(), methodName);
            } else {
                method = obj.getClass().getMethod(methodName, paramTypes);
            }

            if (method != null) {
                method.setAccessible(true);
                if (args.length == 0 || args[0] == null) {
                    method.invoke(obj);
                } else {
                    method.invoke(obj, args);
                }
                if (Config.logDebug)
                	LOGGER.info("Successfully invoked method: {}", methodName);
            }
        } catch (Exception e) {
            // Silent fail - this is expected if method doesn't exist
        }
    }

    private static Method findMethodByName(Class<?> clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private static void trySetField(Object obj, String fieldName, Object value) {
        try {
            Field field = findFieldByName(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(obj, value);
                if (Config.logDebug)
                	LOGGER.info("Successfully set field: {}", fieldName);
            }
        } catch (Exception e) {
            // Silent fail - this is expected if field doesn't exist
        }
    }

    private static Field findFieldByName(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Check public fields too
                for (Field field : current.getFields()) {
                    if (field.getName().equals(name)) {
                        return field;
                    }
                }
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Checks all available fields and methods on the crow entity
     * This is for debugging - helps us understand what's available
     */
    public static void debugCrowEntity(Entity entity) {
        if (!Config.logDebug) {
            return;
        }
        
        LOGGER.info("=== CROW ENTITY DEBUG ===");
        LOGGER.info("Entity type: {}", entity.getType());
        LOGGER.info("Entity class: {}", entity.getClass().getName());

        LOGGER.info("Available methods:");
        for (Method method : entity.getClass().getMethods()) {
            if (method.getName().toLowerCase().contains("fly") ||
                method.getName().toLowerCase().contains("anim") ||
                method.getName().toLowerCase().contains("wing")) {
                LOGGER.info("  - {} ({})", method.getName(), method.getParameterCount());
            }
        }

        LOGGER.info("Available fields:");
        Class<?> current = entity.getClass();
        while (current != null && !current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (name.contains("fly") || name.contains("anim") || name.contains("wing")) {
                    LOGGER.info("  - {} ({})", field.getName(), field.getType().getSimpleName());
                }
            }
            current = current.getSuperclass();
        }
    }
}