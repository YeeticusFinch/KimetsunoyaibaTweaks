package com.lerdorf.kimetsunoyaibamultiplayer.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GravityApiCompat {
    public static final String MODID = "gravityapi";
    public static final String GRAVITY_CHANGER_MODID = "gravitychanger";
    private static final String API_CLASS_NAME = "com.min01.gravityapi.api.GravityChangerAPI";
    private static final String GRAVITY_CHANGER_API_CLASS_NAME = "gravitychanger.api.GravityChangerAPI";

    private static Class<?> apiClass;
    private static volatile boolean apiClassResolved;
    // Cache resolved method handles; getMethod() copies the method array on every
    // call and is far too slow for per-tick per-entity use.
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private GravityApiCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID) || ModList.get().isLoaded(GRAVITY_CHANGER_MODID);
    }

    public static boolean isAvailable() {
        return isLoaded() && apiClass() != null;
    }

    public static boolean isIntegrationEnabled() {
        return false;
    }

    public static Direction getGravityDirection(Entity entity) {
        return invokeDirection(method("getGravityDirection"), Direction.DOWN, entity);
    }

    public static Direction getBaseGravityDirection(Entity entity) {
        return invokeDirection(method("getBaseGravityDirection"), Direction.DOWN, entity);
    }

    public static void setBaseGravityDirection(Entity entity, Direction direction) {
        invokeVoid(method("setBaseGravityDirection"), entity, direction);
    }

    public static void resetGravity(Entity entity) {
        invokeVoid(method("resetGravity"), entity);
    }

    public static double getGravityStrength(Entity entity) {
        return invokeDouble(method("getGravityStrength"), 1.0D, entity);
    }

    public static double getBaseGravityStrength(Entity entity) {
        return invokeDouble(method("getBaseGravityStrength"), 1.0D, entity);
    }

    public static void setBaseGravityStrength(Entity entity, double strength) {
        invokeVoid(method("setBaseGravityStrength"), entity, strength);
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return invokeVec3(method("getWorldVelocity"), entity.getDeltaMovement(), entity);
    }

    public static void setWorldVelocity(Entity entity, Vec3 velocity) {
        invokeVoid(method("setWorldVelocity"), entity, velocity);
    }

    public static Vec3 getEyeOffset(Entity entity) {
        return invokeVec3(method("getEyeOffset"), new Vec3(0.0D, entity.getEyeHeight(), 0.0D), entity);
    }

    public static boolean canChangeGravity(Entity entity) {
        return invokeBoolean(method("canChangeGravity"), false, entity);
    }

    /**
     * The provider's smooth client-side rotation animation for the entity, or null.
     * The returned object exposes {@code getCurrentGravityRotation(Direction, long)}.
     * Both gravityapi and gravitychanger expose this with identical signatures.
     */
    public static Object getRotationAnimation(Entity entity) {
        return invoke(method("getRotationAnimation"), entity);
    }

    private static Method method(String name) {
        return METHOD_CACHE.computeIfAbsent(name, key -> {
            try {
                Class<?> api = apiClass();
                return api == null ? null : api.getMethod(key, parameterTypes(key));
            } catch (NoSuchMethodException exception) {
                return null;
            }
        });
    }

    private static Class<?>[] parameterTypes(String name) {
        return switch (name) {
            case "setBaseGravityDirection" -> new Class<?>[] { Entity.class, Direction.class };
            case "setBaseGravityStrength" -> new Class<?>[] { Entity.class, double.class };
            case "setWorldVelocity" -> new Class<?>[] { Entity.class, Vec3.class };
            default -> new Class<?>[] { Entity.class };
        };
    }

    private static Class<?> apiClass() {
        if (!isLoaded()) {
            return null;
        }
        if (!apiClassResolved) {
            apiClassResolved = true;
            try {
                // Both providers expose the same methods; preserve Gravity API precedence when both are installed.
                apiClass = Class.forName(ModList.get().isLoaded(MODID)
                    ? API_CLASS_NAME : GRAVITY_CHANGER_API_CLASS_NAME);
            } catch (ClassNotFoundException exception) {
                apiClass = null;
            }
        }
        return apiClass;
    }

    private static Object invoke(Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }

    private static Direction invokeDirection(Method method, Direction fallback, Object... args) {
        Object value = invoke(method, args);
        return value instanceof Direction direction ? direction : fallback;
    }

    private static double invokeDouble(Method method, double fallback, Object... args) {
        Object value = invoke(method, args);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean invokeBoolean(Method method, boolean fallback, Object... args) {
        Object value = invoke(method, args);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static Vec3 invokeVec3(Method method, Vec3 fallback, Object... args) {
        Object value = invoke(method, args);
        return value instanceof Vec3 vec3 ? vec3 : fallback;
    }

    private static void invokeVoid(Method method, Object... args) {
        invoke(method, args);
    }
}
