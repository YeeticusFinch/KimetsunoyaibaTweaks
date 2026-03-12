package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

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
    private static final String FLAME_MOVEMENT_TOKEN_TAG = "knymp_flame_movement_token";
    private static final String THUNDER_MOVEMENT_TOKEN_TAG = "knymp_thunder_movement_token";
    private static final String WATER_MOVEMENT_TOKEN_TAG = "knymp_water_movement_token";
    private static final String WIND_MOVEMENT_TOKEN_TAG = "knymp_wind_movement_token";
    private static final double FLAME_FORM3_LAUNCH_Y = 0.72;
    private static final double THUNDER_FORM4_LEAP_SPEED = 2.4;
    private static final double THUNDER_FORM6_ASCENT_Y = 0.69;
    private static final double THUNDER_FORM6_FORWARD_SPEED = 0.5;
    private static final double WATER_FORM2_LEAP_Y = 0.42;
    private static final double WATER_FORM2_FORWARD_SPEED = 1.2;
    private static final double WATER_FORM3_SPRINT_SPEED = 1.0;
    private static final double WATER_FORM8_LEAP_Y = 1.2;
    private static final double WIND_FORM2_LEAP_Y = 0.72;
    private static final double WIND_FORM5_LEAP_Y = 1.3;
    private static final double WIND_FORM7_LEAP_Y = 0.5;
    private static final double WIND_FORM9_LEAP_Y = 1.7;
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
        ItemStack executionStack = createExecutionStack(heldItem, runtimeFormId);

        heldItem.getOrCreateTag().putDouble("select", selectIndex);
        executionStack.getOrCreateTag().putDouble("select", selectIndex);
        entity.getPersistentData().putDouble("breathes", runtimeFormId);

        // Base procedures rely on these counters starting clean for movement-heavy forms.
        resetBaseModCounters(entity);

        Method method = getStartBreathesMethod();
        if (method == null) {
            return;
        }

        try {
            method.invoke(null, level, entity, executionStack);
            boolean blockedByVariation = hasActiveVariationSelection(entity, formId);
            if (Config.logDebug) {
                Log.debug("[BaseModFormExecution] entity={}, formId={}, runtimeFormId={}, blockedByVariation={}",
                    entity.getName().getString(), formId, runtimeFormId, blockedByVariation);
            }
            if (!blockedByVariation) {
                applyCustomMovementOverlay(entity, formId, runtimeFormId);
            }
        } catch (Exception e) {
            Log.error("Failed to execute base mod form {} (runtime {}) via StartBreathesProcedure: {}",
                formId, runtimeFormId, e.getMessage());
        }
    }

    private static boolean hasActiveVariationSelection(LivingEntity entity, int formId) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
        int variationIndex = data.getCurrentVariationIndex();
        if (variationIndex <= 0) {
            return false;
        }

        String swordId = null;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(held.getItem());
            if (registeredSword != null) {
                swordId = registeredSword.getSwordId();
            }
        }

        int totalVariations = VariationRegistry.getVariationCount(formId, swordId);
        boolean blocked = totalVariations > 0 && variationIndex <= totalVariations;
        if (Config.logDebug) {
            Log.debug("[BaseModFormExecution] variation check entity={}, formId={}, swordId={}, variationIndex={}, totalVariations={}, blocked={}",
                entity.getName().getString(), formId, swordId, variationIndex, totalVariations, blocked);
        }
        return blocked;
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

        // Water Breathing: expose canonical 108/109/110 on black sword,
        // but base runtime uses 111/112/113 for those slots.
        switch (formId) {
            case 108:
                return 111;
            case 109:
                return 112;
            case 110:
                return 113;
            default:
                break;
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

        // Moon breathing has gaps in our exposed form list but base runtime select
        // expects the native form number offset (e.g., 1106 -> select 5).
        if (styleRange == 1100) {
            return Math.max((formId % 100) - 1, 0);
        }

        int[] forms = BaseModStyleMapping.getFormsForStyle(styleRange);

        for (int i = 0; i < forms.length; i++) {
            if (forms[i] == formId) {
                return i;
            }
        }

        int fallback = (formId % 100) - 1;
        return Math.max(fallback, 0);
    }

    private static ItemStack createExecutionStack(ItemStack heldItem, int runtimeFormId) {
        if (!isBlackSword(heldItem)) {
            return heldItem;
        }

        ItemStack proxy = createProxySwordForRuntimeForm(runtimeFormId);
        return proxy.isEmpty() ? heldItem : proxy;
    }

    private static boolean isBlackSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }
        String path = itemId.getPath().toLowerCase();
        return path.contains("nichirinsword_black") || (path.contains("black") && path.contains("sword"));
    }

    private static ItemStack createProxySwordForRuntimeForm(int runtimeFormId) {
        int styleRange = (runtimeFormId / 100) * 100;
        String proxyId = switch (styleRange) {
            case 100 -> "kimetsunoyaiba:nichirinsword_water";
            case 200 -> "kimetsunoyaiba:nichirinsword_inosuke";
            case 300 -> "kimetsunoyaiba:nichirinsword_thunder";
            case 400 -> "kimetsunoyaiba:nichirinsword_flame";
            case 500 -> "kimetsunoyaiba:nichirinsword_wind";
            case 600, 1600 -> "kimetsunoyaiba:nichirinsword_stone";
            case 700 -> "kimetsunoyaiba:nichirinsword_mist";
            case 800 -> "kimetsunoyaiba:nichirinsword_serpent";
            case 900, 1300 -> "kimetsunoyaiba:nichirinsword_sound";
            case 1100 -> "kimetsunoyaiba:nichirinswordmoon";
            case 1200 -> "kimetsunoyaiba:nichirinsword_sun";
            case 1400 -> "kimetsunoyaiba:nichirinsword_insect";
            case 1500 -> "kimetsunoyaiba:nichirinsword_love";
            default -> null;
        };

        if (proxyId == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation id = ResourceLocation.tryParse(proxyId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
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
        if (isFlameForm1(originalFormId) || isFlameForm1(runtimeFormId)) {
            if (Config.logDebug) {
                Log.debug("[MovementOverlay] Flame 1 matched for entity={}, originalFormId={}, runtimeFormId={}",
                    entity.getName().getString(), originalFormId, runtimeFormId);
            }
            applyFlameForm1Movement(entity);
            return;
        }
        if (isFlameForm3(originalFormId) || isFlameForm3(runtimeFormId)) {
            applyFlameForm3Movement(entity);
            return;
        }
        if (isThunderForm1(originalFormId) || isThunderForm1(runtimeFormId)) {
            if (Config.logDebug) {
                Log.debug("[MovementOverlay] Thunder 1 matched for entity={}, originalFormId={}, runtimeFormId={}",
                    entity.getName().getString(), originalFormId, runtimeFormId);
            }
            applyThunderForm1Movement(entity);
            return;
        }
        if (isThunderForm3(originalFormId) || isThunderForm3(runtimeFormId)) {
            applyThunderForm3Movement(entity);
            return;
        }
        if (isThunderForm4(originalFormId) || isThunderForm4(runtimeFormId)) {
            applyThunderForm4Movement(entity);
            return;
        }
        if (isThunderForm6(originalFormId) || isThunderForm6(runtimeFormId)) {
            applyThunderForm6Movement(entity);
            return;
        }
        if (isWaterForm2(originalFormId) || isWaterForm2(runtimeFormId)) {
            applyWaterForm2Movement(entity);
            return;
        }
        if (isWaterForm3(originalFormId) || isWaterForm3(runtimeFormId)) {
            applyWaterForm3Movement(entity);
            return;
        }
        if (isWaterForm4(originalFormId) || isWaterForm4(runtimeFormId)) {
            applyWaterForm4Movement(entity);
            return;
        }
        if (isWaterForm5(originalFormId) || isWaterForm5(runtimeFormId)) {
            applyWaterForm5Movement(entity);
            return;
        }
        if (isWaterForm8(originalFormId) || isWaterForm8(runtimeFormId)) {
            applyWaterForm8Movement(entity);
            return;
        }
        if (isWaterForm9(originalFormId) || isWaterForm9(runtimeFormId)) {
            applyWaterForm9Movement(entity);
            return;
        }
        if (isWindForm1(originalFormId) || isWindForm1(runtimeFormId)) {
            applyWindForm1Movement(entity);
            return;
        }
        if (isWindForm2(originalFormId) || isWindForm2(runtimeFormId)) {
            applyWindForm2Movement(entity);
            return;
        }
        if (isWindForm5(originalFormId) || isWindForm5(runtimeFormId)) {
            applyWindForm5Movement(entity);
            return;
        }
        if (isWindForm7(originalFormId) || isWindForm7(runtimeFormId)) {
            applyWindForm7Movement(entity);
            return;
        }
        if (isWindForm8(originalFormId) || isWindForm8(runtimeFormId)) {
            applyWindForm8Movement(entity);
            return;
        }
        if (isWindForm9(originalFormId) || isWindForm9(runtimeFormId)) {
            applyWindForm9Movement(entity);
            return;
        }

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

    private static boolean isFlameForm1(int formId) {
        return formId == 401;
    }

    private static boolean isFlameForm3(int formId) {
        return formId == 403;
    }

    private static boolean isThunderForm1(int formId) {
        return formId == 301;
    }

    private static boolean isThunderForm3(int formId) {
        return formId == 303;
    }

    private static boolean isThunderForm4(int formId) {
        return formId == 304;
    }

    private static boolean isThunderForm6(int formId) {
        return formId == 306;
    }

    private static boolean isWaterForm2(int formId) {
        return formId == 102;
    }

    private static boolean isWaterForm3(int formId) {
        return formId == 103;
    }

    private static boolean isWaterForm4(int formId) {
        return formId == 104;
    }

    private static boolean isWaterForm5(int formId) {
        return formId == 105;
    }

    private static boolean isWaterForm8(int formId) {
        // Some paths may represent this as 111; support both IDs.
        return formId == 108 || formId == 111;
    }

    private static boolean isWaterForm9(int formId) {
        // Some paths may represent this as 112; support both IDs.
        return formId == 109 || formId == 112;
    }

    private static boolean isWindForm1(int formId) {
        return formId == 501;
    }

    private static boolean isWindForm2(int formId) {
        return formId == 502;
    }

    private static boolean isWindForm5(int formId) {
        return formId == 505;
    }

    private static boolean isWindForm7(int formId) {
        return formId == 507;
    }

    private static boolean isWindForm8(int formId) {
        return formId == 508;
    }

    private static boolean isWindForm9(int formId) {
        return formId == 509;
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
     * Flame Breathing First Form:
     * 1) Wait 10 ticks.
     * 2) Wait until the user is grounded.
     * 3) Snapshot look direction at that exact moment.
     * 4) For the next 10 ticks, force velocity in that snapshotted direction at magnitude 2.4.
     */
    private static void applyFlameForm1Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, FLAME_MOVEMENT_TOKEN_TAG);
        if (Config.logDebug) {
            Log.debug("[MovementOverlay] Flame 1 scheduled entity={}, token={}", entity.getName().getString(), token);
        }

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, FLAME_MOVEMENT_TOKEN_TAG, token)) {
                if (Config.logDebug) {
                    Log.debug("[MovementOverlay] Flame 1 aborted before delay completed entity={}, tokenActive={}",
                        entity.getName().getString(), isMovementTokenActive(entity, FLAME_MOVEMENT_TOKEN_TAG, token));
                }
                return;
            }
            if (Config.logDebug) {
                Log.debug("[MovementOverlay] Flame 1 delay complete entity={}, onGround={}",
                    entity.getName().getString(), entity.onGround());
            }

            // If already grounded at the 10-tick mark, begin immediately.
            if (entity.onGround()) {
                startLockedLookDash(entity, FLAME_MOVEMENT_TOKEN_TAG, token, 2.4);
                return;
            }

            final boolean[] started = {false};
            AbilityScheduler.scheduleRepeating(entity, () -> {
                if (!entity.isAlive() || !isMovementTokenActive(entity, FLAME_MOVEMENT_TOKEN_TAG, token) || started[0]) {
                    return;
                }

                if (entity.onGround()) {
                    started[0] = true;
                    if (Config.logDebug) {
                        Log.debug("[MovementOverlay] Flame 1 grounded start entity={}, token={}",
                            entity.getName().getString(), token);
                    }
                    startLockedLookDash(entity, FLAME_MOVEMENT_TOKEN_TAG, token, 2.4);
                }
            }, 1, 200);
        }, 10);
    }

    /**
     * Flame Breathing Third Form:
     * Apply one immediate upward launch so the user ascends about 3 blocks by ~tick 10.
     */
    private static void applyFlameForm3Movement(LivingEntity entity) {
        nextMovementToken(entity, FLAME_MOVEMENT_TOKEN_TAG);
        Vec3 velocity = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, velocity.x, FLAME_FORM3_LAUNCH_Y, velocity.z);
    }

    /**
     * Thunder Breathing First Form:
     * 1) Wait 10 ticks.
     * 2) Wait until grounded.
     * 3) Snapshot look direction.
     * 4) Move in that direction at 2.4 for 10 ticks.
     * 5) Then hard-stop velocity to (0,0,0).
     */
    private static void applyThunderForm1Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, THUNDER_MOVEMENT_TOKEN_TAG);
        if (Config.logDebug) {
            Log.debug("[MovementOverlay] Thunder 1 scheduled entity={}, token={}", entity.getName().getString(), token);
        }

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                if (Config.logDebug) {
                    Log.debug("[MovementOverlay] Thunder 1 aborted before delay completed entity={}, tokenActive={}",
                        entity.getName().getString(), isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token));
                }
                return;
            }
            if (Config.logDebug) {
                Log.debug("[MovementOverlay] Thunder 1 delay complete entity={}, onGround={}",
                    entity.getName().getString(), entity.onGround());
            }

            // If already grounded at the 10-tick mark, begin immediately.
            if (entity.onGround()) {
                startLockedLookDash(entity, THUNDER_MOVEMENT_TOKEN_TAG, token, 2.4);
                AbilityScheduler.scheduleOnce(entity, () -> {
                    if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                        return;
                    }
                    MovementHelper.setVelocity(entity, 0.0, 0.0, 0.0);
                }, 10);
                return;
            }

            final boolean[] started = {false};
            AbilityScheduler.scheduleRepeating(entity, () -> {
                if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token) || started[0]) {
                    return;
                }

                if (entity.onGround()) {
                    started[0] = true;
                    if (Config.logDebug) {
                        Log.debug("[MovementOverlay] Thunder 1 grounded start entity={}, token={}",
                            entity.getName().getString(), token);
                    }
                    startLockedLookDash(entity, THUNDER_MOVEMENT_TOKEN_TAG, token, 2.9);

                    AbilityScheduler.scheduleOnce(entity, () -> {
                        if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                            return;
                        }
                        MovementHelper.setVelocity(entity, 0.0, 0.0, 0.0);
                    }, 10);
                }
            }, 1, 200);
        }, 15);
    }

    /**
     * Thunder Breathing Third Form:
     * For 30 ticks, move the user forward horizontally based on current yaw at speed 1.0.
     */
    private static void applyThunderForm3Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, THUNDER_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            setHorizontalVelocityFromYawPreserveY(entity, 1.0);
        }, 1, 30);
    }

    /**
     * Thunder Breathing Fourth Form:
     * Wait 10 ticks, then do a one-time leap in the exact look direction.
     */
    private static void applyThunderForm4Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, THUNDER_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            Vec3 launchDirection = fullLook(entity).scale(THUNDER_FORM4_LEAP_SPEED);
            MovementHelper.setVelocity(entity, launchDirection);
        }, 10);
    }

    /**
     * Thunder Breathing Sixth Form:
     * 1) Leap upward and forward so the user rises and advances.
     * 2) After 10 ticks, force a 10-tick hover by zeroing Y velocity.
     */
    private static void applyThunderForm6Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, THUNDER_MOVEMENT_TOKEN_TAG);

        Vec3 horizontalForward = horizontalLook(entity).scale(THUNDER_FORM6_FORWARD_SPEED);
        MovementHelper.setVelocity(entity, horizontalForward.x, THUNDER_FORM6_ASCENT_Y, horizontalForward.z);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }

            //final boolean[] originalNoGravity = {entity.isNoGravity()};
            entity.setNoGravity(true);

            AbilityScheduler.scheduleRepeating(entity, () -> {
                if (!entity.isAlive() || !isMovementTokenActive(entity, THUNDER_MOVEMENT_TOKEN_TAG, token)) {
                    entity.setNoGravity(false);
                    return;
                }
                Vec3 current = entity.getDeltaMovement();
                MovementHelper.setVelocity(entity, current.x, 0.0, current.z);
                entity.fallDistance = 0.0F;
            }, 1, 10);

            AbilityScheduler.scheduleOnce(entity, () -> {
                if (!entity.isAlive()) {
                    return;
                }
                // Restore prior gravity state even if another movement token superseded this one.
                entity.setNoGravity(false);
            }, 10);
        }, 10);
    }

    /**
     * Water Breathing Second Form:
     * One-time leap forward with a small upward component.
     */
    private static void applyWaterForm2Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 horizontalForward = horizontalLook(entity).scale(WATER_FORM2_FORWARD_SPEED);
        MovementHelper.setVelocity(entity, horizontalForward.x, WATER_FORM2_LEAP_Y, horizontalForward.z);
    }

    /**
     * Water Breathing Third Form:
     * Sprint forward for 3 seconds (60 ticks) based on current yaw.
     */
    private static void applyWaterForm3Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            setHorizontalVelocityFromYawPreserveY(entity, WATER_FORM3_SPRINT_SPEED);
        }, 1, 40);
    }

    /**
     * Water Breathing Fourth Form:
     * Lock look direction once, then move in that direction for 10 ticks at 1.6.
     */
    private static void applyWaterForm4Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        Vec3 lockedDirection = fullLook(entity).normalize();
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, lockedDirection.scale(1.6));
        }, 1, 10);
    }

    /**
     * Water Breathing Fifth Form:
     * Lock look direction once, then move in that direction for 10 ticks at 1.4.
     */
    private static void applyWaterForm5Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        Vec3 lockedDirection = fullLook(entity).normalize();
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, lockedDirection.scale(1.4));
        }, 1, 10);
    }

    /**
     * Water Breathing Eighth Form:
     * One-time vertical leap targeting about 9 blocks of ascent.
     */
    private static void applyWaterForm8Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 current = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, current.x, WATER_FORM8_LEAP_Y, current.z);
    }

    /**
     * Water Breathing Ninth Form:
     * For 40 ticks, whenever grounded, leap in current look direction at magnitude 2.0.
     */
    private static void applyWaterForm9Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WATER_MOVEMENT_TOKEN_TAG);
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WATER_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            if (entity.onGround()) {
                setVelocityInLookDirection(entity, 2.0);
            }
        }, 1, 50);
    }

    /**
     * Wind Breathing First Form:
     * Lock horizontal yaw direction once, then for 20 ticks set only X/Z at speed 1.1.
     * Y velocity is preserved so gravity/falling remains intact.
     */
    private static void applyWindForm1Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        double yawRad = Math.toRadians(entity.getYRot());
        double x = -Math.sin(yawRad) * 1.1;
        double z = Math.cos(yawRad) * 1.1;

        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, x, entity.getDeltaMovement().y, z);
        }, 1, 20);
    }

    /**
     * Wind Breathing Second Form:
     * One-time upward leap tuned for roughly 3 blocks of ascent over ~10 ticks.
     */
    private static void applyWindForm2Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 current = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, current.x, WIND_FORM2_LEAP_Y, current.z);
    }

    /**
     * Wind Breathing Fifth Form:
     * One-time upward leap tuned for roughly 8 blocks of ascent over ~15 ticks.
     */
    private static void applyWindForm5Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 current = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, current.x, WIND_FORM5_LEAP_Y, current.z);
    }

    /**
     * Wind Breathing Seventh Form:
     * One-time upward leap of about 1.5 blocks.
     */
    private static void applyWindForm7Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 current = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, current.x, WIND_FORM7_LEAP_Y, current.z);
    }

    /**
     * Wind Breathing Eighth Form:
     * Lock exact look direction, force movement at 1.5 for 10 ticks, then hard stop.
     */
    private static void applyWindForm8Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        Vec3 lockedDirection = fullLook(entity);
        if (lockedDirection.lengthSqr() <= 1.0e-6) {
            lockedDirection = new Vec3(0.0, 0.0, 1.0);
        } else {
            lockedDirection = lockedDirection.normalize();
        }
        final Vec3 dash = lockedDirection.scale(1.5);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, dash);
        }, 1, 10);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
                return;
            }
            MovementHelper.setVelocity(entity, 0.0, 0.0, 0.0);
        }, 10);
    }

    /**
     * Wind Breathing Ninth Form:
     * One-time upward leap tuned for roughly 14 blocks of ascent over ~15 ticks.
     */
    private static void applyWindForm9Movement(LivingEntity entity) {
        final int token = nextMovementToken(entity, WIND_MOVEMENT_TOKEN_TAG);
        if (!entity.isAlive() || !isMovementTokenActive(entity, WIND_MOVEMENT_TOKEN_TAG, token)) {
            return;
        }
        Vec3 current = entity.getDeltaMovement();
        MovementHelper.setVelocity(entity, current.x, WIND_FORM9_LEAP_Y, current.z);
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

    private static void startLockedLookDash(LivingEntity entity, String tokenTag, int token, double speed) {
        Vec3 look = fullLook(entity);
        Vec3 lockedDirection = look.lengthSqr() > 1.0e-6 ? look.normalize() : horizontalLook(entity);
        if (Config.logDebug) {
            Log.debug("[MovementOverlay] startLockedLookDash entity={}, tokenTag={}, token={}, speed={}, dir=({}, {}, {})",
                entity.getName().getString(), tokenTag, token, speed,
                lockedDirection.x, lockedDirection.y, lockedDirection.z);
        }
        final int[] tickCounter = {0};
        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (!entity.isAlive() || !isMovementTokenActive(entity, tokenTag, token)) {
                if (Config.logDebug) {
                    Log.debug("[MovementOverlay] startLockedLookDash stopped entity={}, tokenTag={}, token={}, tokenActive={}",
                        entity.getName().getString(), tokenTag, token, isMovementTokenActive(entity, tokenTag, token));
                }
                return;
            }
            tickCounter[0]++;
            MovementHelper.setVelocity(entity, lockedDirection.scale(speed));
            if (Config.logDebug && (tickCounter[0] == 1 || tickCounter[0] == 10)) {
                Vec3 current = entity.getDeltaMovement();
                Log.debug("[MovementOverlay] startLockedLookDash tick entity={}, tokenTag={}, token={}, tick={}, velocity=({}, {}, {})",
                    entity.getName().getString(), tokenTag, token, tickCounter[0],
                    current.x, current.y, current.z);
            }
        }, 1, 10);
    }

    private static void setHorizontalVelocityFromYawPreserveY(LivingEntity entity, double horizontalSpeed) {
        double yawRad = Math.toRadians(entity.getYRot());
        double x = -Math.sin(yawRad) * horizontalSpeed;
        double z = Math.cos(yawRad) * horizontalSpeed;
        double currentYVelocity = entity.getDeltaMovement().y;
        MovementHelper.setVelocity(entity, x, currentYVelocity, z);
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
