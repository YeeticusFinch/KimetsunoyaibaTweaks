package com.lerdorf.kimetsunoyaibamultiplayer.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class GravityApiCompat {
    public static final String MODID = "gravityapi";
    private static final String API_CLASS_NAME = "com.min01.gravityapi.api.GravityChangerAPI";

    private static Class<?> apiClass;
    private static boolean apiClassResolved;

    private GravityApiCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    public static boolean isAvailable() {
        return isLoaded() && apiClass() != null;
    }

    public static boolean isIntegrationEnabled() {
        return false;
    }

    public static Direction getGravityDirection(Entity entity) {
        return invokeDirection(method("getGravityDirection", Entity.class), Direction.DOWN, entity);
    }

    public static Direction getBaseGravityDirection(Entity entity) {
        return invokeDirection(method("getBaseGravityDirection", Entity.class), Direction.DOWN, entity);
    }

    public static void setBaseGravityDirection(Entity entity, Direction direction) {
        invokeVoid(method("setBaseGravityDirection", Entity.class, Direction.class), entity, direction);
    }

    public static void resetGravity(Entity entity) {
        invokeVoid(method("resetGravity", Entity.class), entity);
    }

    public static double getGravityStrength(Entity entity) {
        return invokeDouble(method("getGravityStrength", Entity.class), 1.0D, entity);
    }

    public static double getBaseGravityStrength(Entity entity) {
        return invokeDouble(method("getBaseGravityStrength", Entity.class), 1.0D, entity);
    }

    public static void setBaseGravityStrength(Entity entity, double strength) {
        invokeVoid(method("setBaseGravityStrength", Entity.class, double.class), entity, strength);
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return invokeVec3(method("getWorldVelocity", Entity.class), entity.getDeltaMovement(), entity);
    }

    public static void setWorldVelocity(Entity entity, Vec3 velocity) {
        invokeVoid(method("setWorldVelocity", Entity.class, Vec3.class), entity, velocity);
    }

    public static Vec3 getEyeOffset(Entity entity) {
        return invokeVec3(method("getEyeOffset", Entity.class), new Vec3(0.0D, entity.getEyeHeight(), 0.0D), entity);
    }

    public static boolean canChangeGravity(Entity entity) {
        return invokeBoolean(method("canChangeGravity", Entity.class), false, entity);
    }

    private static Method method(String name, Class<?>... parameterTypes) {
        try {
            Class<?> api = apiClass();
            return api == null ? null : api.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Class<?> apiClass() {
        if (!isLoaded()) {
            return null;
        }
        if (!apiClassResolved) {
            apiClassResolved = true;
            try {
                apiClass = Class.forName(API_CLASS_NAME);
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
