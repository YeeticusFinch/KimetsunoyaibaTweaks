package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Keeps the base-mod form used by an active enhanced ability separate from
 * selections made while that ability is still running.
 */
public final class BaseModEnhancedAbilityState {
    private static final String NBT_ACTIVE = "knymp_base_mod_ability_active";
    private static final String NBT_ACTIVE_FORM = "knymp_base_mod_ability_form";
    private static final String NBT_PENDING = "knymp_base_mod_ability_pending";
    private static final String NBT_PENDING_FORM = "knymp_base_mod_ability_pending_form";
    private static final String NBT_PENDING_ITEM = "knymp_base_mod_ability_pending_item";

    private BaseModEnhancedAbilityState() {
    }

    public static boolean isActive(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(NBT_ACTIVE);
    }

    public static void start(LivingEntity entity, int formId, ItemStack stack) {
        if (entity == null || formId <= 0) {
            return;
        }

        var data = entity.getPersistentData();
        data.putBoolean(NBT_ACTIVE, true);
        data.putDouble(NBT_ACTIVE_FORM, formId);
        data.putBoolean(NBT_PENDING, false);
        data.putDouble(NBT_PENDING_FORM, 0.0D);
        data.putString(NBT_PENDING_ITEM, "");
    }

    /** Keeps the active form locked until the base procedure resets breathes to zero. */
    public static boolean isFinishedOrRestore(LivingEntity entity) {
        if (!isActive(entity)) {
            return false;
        }

        var data = entity.getPersistentData();
        double currentBreathes = data.getDouble("breathes");
        if (currentBreathes == 0.0D) {
            return true;
        }

        double activeForm = data.getDouble(NBT_ACTIVE_FORM);
        if (activeForm > 0.0D && currentBreathes != activeForm) {
            data.putDouble("breathes", activeForm);
        }
        return false;
    }

    public static void queueSelection(LivingEntity entity, ItemStack stack, int formId) {
        if (entity == null || formId <= 0) {
            return;
        }
        queue(entity, stack, formId);
    }

    public static void queueSwordSwitch(LivingEntity entity, ItemStack stack) {
        int formId = BaseModSwordFormRegistry.isSupported(stack)
            ? BaseModSwordFormRegistry.getSelectedFormId(stack) : 0;
        queue(entity, stack, formId);
    }

    private static void queue(LivingEntity entity, ItemStack stack, int formId) {
        var data = entity.getPersistentData();
        data.putBoolean(NBT_PENDING, true);
        data.putDouble(NBT_PENDING_FORM, formId);
        data.putString(NBT_PENDING_ITEM, getItemId(stack));
    }

    public static boolean hasPending(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(NBT_PENDING);
    }

    public static int getPendingForm(LivingEntity entity) {
        return entity == null ? 0 : (int) entity.getPersistentData().getDouble(NBT_PENDING_FORM);
    }

    public static String getPendingItem(LivingEntity entity) {
        return entity == null ? "" : entity.getPersistentData().getString(NBT_PENDING_ITEM);
    }

    public static String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        var data = entity.getPersistentData();
        data.putBoolean(NBT_ACTIVE, false);
        data.putDouble(NBT_ACTIVE_FORM, 0.0D);
        data.putBoolean(NBT_PENDING, false);
        data.putDouble(NBT_PENDING_FORM, 0.0D);
        data.putString(NBT_PENDING_ITEM, "");
    }
}
