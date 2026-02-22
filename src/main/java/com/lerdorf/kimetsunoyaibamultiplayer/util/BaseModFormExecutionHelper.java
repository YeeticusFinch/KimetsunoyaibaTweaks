package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Bridge for executing base-mod breathing forms from custom BreathingSwordItem forms.
 * Uses the base mod's StartBreathesProcedure entry point via reflection.
 */
public final class BaseModFormExecutionHelper {
    private static final String STONE_MOVEMENT_TOKEN_TAG = "knymp_stone_movement_token";
    private static final String SERPENT_MOVEMENT_TOKEN_TAG = "knymp_serpent_movement_token";
    private static final String SOUND_MOVEMENT_TOKEN_TAG = "knymp_sound_movement_token";
    private static final String INSECT_MOVEMENT_TOKEN_TAG = "knymp_insect_movement_token";
    private static Method startBreathesMethod;
    private static boolean startBreathesResolved = false;
    private static boolean startBreathesMissingLogged = false;
    private static Method resetCounterMethod;
    private static boolean resetCounterResolved = false;
    private static boolean resetCounterMissingLogged = false;

    private BaseModFormExecutionHelper() {
    }

    /**
     * Execute a specific base-mod form ID for an entity holding a sword.
     *
     * @param entity Entity using the form
     * @param level Current level
     * @param formId Base-mod form ID (e.g. 601, 801, 901, 1401)
     */
    public static void executeBaseModForm(LivingEntity entity, Level level, int formId) {
        if (entity == null || level == null || level.isClientSide) {
            return;
        }

        ItemStack heldItem = entity.getMainHandItem();
        if (heldItem.isEmpty()) {
            return;
        }

        int runtimeFormId = mapToRuntimeFormId(formId);
        if (Config.logDebug && runtimeFormId != formId) {
            Log.debug("Mapped custom form ID {} -> base runtime form ID {}", formId, runtimeFormId);
        }
        int selectIndex = resolveSelectIndex(runtimeFormId);
        heldItem.getOrCreateTag().putDouble("select", selectIndex);
        entity.getPersistentData().putDouble("breathes", runtimeFormId);

        // Base procedures rely on these counters starting clean for movement-heavy forms.
        resetBaseModCounters(entity);

        Method method = getStartBreathesMethod();
        if (method == null) {
            return;
        }

        try {
            method.invoke(null, level, entity, heldItem);
            applyCustomMovementOverlay(entity, formId, runtimeFormId);
        } catch (Exception e) {
            Log.error("Failed to execute base mod form {} (runtime {}) via StartBreathesProcedure: {}",
                formId, runtimeFormId, e.getMessage());
        }
    }

    /**
     * Map our legacy/registry-facing form IDs to the base mod's runtime breathes IDs.
     * This keeps existing custom sword form lists stable while executing the correct base procedures.
     */
    private static int mapToRuntimeFormId(int formId) {
        // Stone Breathing: legacy 601+ maps to runtime 1601+
        if (formId >= 601 && formId <= 609) {
            return formId + 1000;
        }
        if (formId == 611) {
            return 1611;
        }

        // Sound Breathing: legacy 901-904 maps to runtime slots used by PlayerBreathSoundProcedure.
        switch (formId) {
            case 901:
                return 1301;
            case 902:
                return 1304;
            case 903:
                return 1305;
            case 904:
                return 1320;
            default:
                break;
        }

        // Insect Breathing: legacy flower-range IDs map to runtime insect-range IDs.
        switch (formId) {
            case 1401:
                return 901;
            case 1402:
                return 902;
            case 1404:
                return 903;
            case 1405:
                return 904;
            default:
                return formId;
        }
    }

    /**
     * Convert a base-mod form ID to the base-mod "select" index for the held item.
     * Falls back to (formNumber - 1) when exact mapping is unknown.
     */
    private static int resolveSelectIndex(int formId) {
        int styleRange = (formId / 100) * 100;
        int[] forms = BaseModStyleMapping.getFormsForStyle(styleRange);

        for (int i = 0; i < forms.length; i++) {
            if (forms[i] == formId) {
                return i;
            }
        }

        int fallback = (formId % 100) - 1;
        return Math.max(fallback, 0);
    }

    private static Method getStartBreathesMethod() {
        if (startBreathesResolved) {
            return startBreathesMethod;
        }

        startBreathesResolved = true;

        try {
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures.StartBreathesProcedure");
            startBreathesMethod = procedureClass.getMethod(
                "execute",
                LevelAccessor.class,
                net.minecraft.world.entity.Entity.class,
                ItemStack.class
            );
        } catch (Exception e) {
            if (!startBreathesMissingLogged) {
                startBreathesMissingLogged = true;
                if (Config.logDebug) {
                    Log.warn("StartBreathesProcedure unavailable; base-mod form bridge disabled: {}", e.getMessage());
                }
            }
            startBreathesMethod = null;
        }

        return startBreathesMethod;
    }

    private static void resetBaseModCounters(LivingEntity entity) {
        Method method = getResetCounterMethod();
        if (method == null) {
            return;
        }

        try {
            method.invoke(null, entity);
        } catch (Exception e) {
            if (Config.logDebug) {
                Log.warn("Failed to execute ResetCounterProcedure: {}", e.getMessage());
            }
        }
    }

    private static Method getResetCounterMethod() {
        if (resetCounterResolved) {
            return resetCounterMethod;
        }

        resetCounterResolved = true;

        try {
            Class<?> procedureClass = Class.forName("net.mcreator.kimetsunoyaiba.procedures.ResetCounterProcedure");
            resetCounterMethod = procedureClass.getMethod(
                "execute",
                net.minecraft.world.entity.Entity.class
            );
        } catch (Exception e) {
            if (!resetCounterMissingLogged) {
                resetCounterMissingLogged = true;
                if (Config.logDebug) {
                    Log.warn("ResetCounterProcedure unavailable; counter reset bridge disabled: {}", e.getMessage());
                }
            }
            resetCounterMethod = null;
        }

        return resetCounterMethod;
    }

    private static void applyCustomMovementOverlay(LivingEntity entity, int originalFormId, int runtimeFormId) {
        if (isInsectForm1(originalFormId)) {
            applyInsectForm1Movement(entity);
            return;
        }
        if (isInsectForm2(originalFormId)) {
            applyInsectForm2Movement(entity);
            return;
        }
        if (isInsectForm4(originalFormId)) {
            applyInsectForm4Movement(entity);
            return;
        }

        if (isSerpentForm4(originalFormId) || isSerpentForm5(originalFormId)) {
            applySerpentForm4Or5Movement(entity);
            return;
        }

        // Guard against ID overlap: runtime 903 is also used by mapped insect forms.
        if (isSoundStyleOriginalForm(originalFormId) && isSoundForm5Runtime(runtimeFormId)) {
            applySoundForm5Movement(entity);
            return;
        }
        if (isSoundForm5Original(originalFormId)) {
            applySoundForm5Movement(entity);
            return;
        }

        if (isStoneForm4(originalFormId) || isStoneForm4(runtimeFormId)) {
            applyStoneForm4Movement(entity);
            return;
        }
        if (isStoneForm5(originalFormId) || isStoneForm5(runtimeFormId)) {
            applyStoneForm5Movement(entity);
        }
    }

    private static boolean isStoneForm4(int formId) {
        return formId == 604 || formId == 1604;
    }

    private static boolean isStoneForm5(int formId) {
        return formId == 605 || formId == 1605;
    }

    private static boolean isInsectForm1(int formId) {
        return formId == 1401;
    }

    private static boolean isInsectForm2(int formId) {
        return formId == 1402;
    }

    private static boolean isInsectForm4(int formId) {
        return formId == 1405;
    }

    private static boolean isSerpentForm4(int formId) {
        return formId == 804;
    }

    private static boolean isSerpentForm5(int formId) {
        return formId == 805;
    }

    private static boolean isSoundStyleOriginalForm(int formId) {
        return formId >= 901 && formId <= 904;
    }

    private static boolean isSoundForm5Original(int formId) {
        return formId == 903;
    }

    private static boolean isSoundForm5Runtime(int formId) {
        return formId == 1305;
    }

    /**
     * Insect Breathing First Form (Butterfly Dance: Caprice):
     * One-time vertical leap, then one forward burst when descent starts.
     */
    private static void applyInsectForm1Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, INSECT_MOVEMENT_TOKEN_TAG);
        MovementHelper.setVelocity(entity, 0.0, 1.5, 0.0);
        AnimationHelper.playAnimation(entity, "speed_attack1");

        final boolean[] burstDone = {false};
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, INSECT_MOVEMENT_TOKEN_TAG, token) || burstDone[0]) {
                return;
            }

            if (entity.getDeltaMovement().y <= 0.0) {
                setVelocityInLookDirection(entity, 1.5);
                AnimationHelper.playAnimation(entity, "speed_attack_sword");
                burstDone[0] = true;
            }
        }, 1, 25);
    }

    /**
     * Insect Breathing Second Form (Dance of the Bee Sting: True Flutter):
     * Continuous forward launch for 10 ticks, then hard stop.
     */
    private static void applyInsectForm2Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, INSECT_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, INSECT_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            setVelocityInLookDirection(entity, 1.2);
        }, 1, 10);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, INSECT_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, 0.0, 0.0, 0.0);
        }, 10);
    }

    /**
     * Insect Breathing Fourth Form (Dance of the Centipede: Hundred-Legged Zigzag):
     * 5 ticks horizontal glide (gravity preserved), then one large leap in look direction.
     */
    private static void applyInsectForm4Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, INSECT_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, INSECT_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            setHorizontalVelocityPreserveY(entity, 1.0);
        }, 1, 5);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, INSECT_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            setVelocityInLookDirection(entity, 2.0);
        }, 5);
    }

    /**
     * Serpent Breathing Fourth/Fifth Forms:
     * Constant horizontal forward movement for 10 ticks while preserving Y velocity.
     */
    private static void applySerpentForm4Or5Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, SERPENT_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, SERPENT_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }

            setHorizontalVelocityPreserveY(entity, 1.0);
        }, 1, 10);
    }

    /**
     * Sound Breathing Fifth Form (String Performance):
     * Move forward at walk speed while preserving Y velocity.
     */
    private static void applySoundForm5Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, SOUND_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, SOUND_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }

            setHorizontalVelocityPreserveY(entity, 0.12);
        }, 1, 20);
    }

    /**
     * Stone Breathing Fourth Form (Volcanic Rock, Rapid Conquest):
     * Slide forward horizontally for 5 ticks while preserving current Y velocity (gravity still applies).
     */
    private static void applyStoneForm4Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, STONE_MOVEMENT_TOKEN_TAG);
        // Apply immediately to override any initial pitch-based impulse from the base procedure.
        setHorizontalVelocityGravityOnly(entity, 1.0);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, STONE_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }

            setHorizontalVelocityGravityOnly(entity, 1.0);
        }, 1, 5);
    }

    /**
     * Stone Breathing Fifth Form (Arcs of Justice):
     * Ascend to ~8 blocks over 10 ticks, hover for 20 ticks, then force a downward drop.
     */
    private static void applyStoneForm5Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, STONE_MOVEMENT_TOKEN_TAG);
        final double startY = entity.getY();
        final double hoverY = startY + 8.0;
        final int[] tickCounter = {0};

        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, STONE_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }

            tickCounter[0]++;
            double yVelocity;

            if (tickCounter[0] <= 10) {
                // Smoothly reach +8 blocks over the first 10 ticks.
                int remainingTicks = Math.max(1, 11 - tickCounter[0]);
                double yError = hoverY - entity.getY();
                yVelocity = Mth.clamp((yError / remainingTicks) + 0.08, 0.35, 1.25);
            } else {
                // Hover near the +8 block target for the next 20 ticks.
                double yError = hoverY - entity.getY();
                yVelocity = Mth.clamp((yError * 0.85) + 0.08, -0.35, 0.35);
            }

            MovementHelper.setVelocity(entity, 0.0, yVelocity, 0.0);
        }, 1, 30);

        // After ascent + hover, drop back down.
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, STONE_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, 0.0, -1.25, 0.0);
        }, 30);
    }

    private static int nextMovementToken(LivingEntity entity, String tagKey) {
        int next = entity.getPersistentData().getInt(tagKey) + 1;
        entity.getPersistentData().putInt(tagKey, next);
        return next;
    }

    private static boolean isMovementTokenActive(LivingEntity entity, String tagKey, int token) {
        return entity.getPersistentData().getInt(tagKey) == token;
    }

    private static Vec3 horizontalLook(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0e-6) {
            return horizontal.normalize();
        }

        // Fallback in case the look vector is near-vertical.
        double yawRad = Math.toRadians(entity.getYRot());
        double x = -Math.sin(yawRad);
        double z = Math.cos(yawRad);
        return new Vec3(x, 0.0, z).normalize();
    }

    private static Vec3 fullLook(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        if (look.lengthSqr() > 1.0e-6) {
            return look.normalize();
        }
        return horizontalLook(entity);
    }

    /**
     * Apply horizontal movement only while preserving current vertical velocity.
     * This keeps normal gravity/falling behavior intact.
     */
    private static void setHorizontalVelocityPreserveY(LivingEntity entity, double horizontalSpeed) {
        Vec3 horizontalForward = horizontalLook(entity).scale(horizontalSpeed);
        double currentYVelocity = entity.getDeltaMovement().y;
        MovementHelper.setVelocity(entity, horizontalForward.x, currentYVelocity, horizontalForward.z);
    }

    private static void setVelocityInLookDirection(LivingEntity entity, double speed) {
        Vec3 forward = fullLook(entity).scale(speed);
        MovementHelper.setVelocity(entity, forward);
    }

    /**
     * Apply horizontal movement while preserving gravity/falling behavior.
     * Any upward vertical impulse is suppressed so movement stays yaw-only.
     */
    private static void setHorizontalVelocityGravityOnly(LivingEntity entity, double horizontalSpeed) {
        Vec3 horizontalForward = horizontalLook(entity).scale(horizontalSpeed);
        double gravityY = Math.min(entity.getDeltaMovement().y, 0.0);
        MovementHelper.setVelocity(entity, horizontalForward.x, gravityY, horizontalForward.z);
    }
}
