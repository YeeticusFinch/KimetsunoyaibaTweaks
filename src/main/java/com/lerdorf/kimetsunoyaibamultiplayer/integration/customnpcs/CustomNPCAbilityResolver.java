package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;

import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BloodDemonArtExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.BaseModBreathingExecutor;
import com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs.executors.CustomBreathingExecutor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Shared Custom NPC ability resolution.
 * Prefers the main hand, but falls back to the offhand so dual-wield setups can still trigger abilities.
 */
public final class CustomNPCAbilityResolver {

    private CustomNPCAbilityResolver() {
    }

    public static ItemStack findAbilityStack(LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = entity.getMainHandItem();
        if (isAbilityItem(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = entity.getOffhandItem();
        if (isAbilityItem(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    public static boolean isAbilityItem(ItemStack stack) {
        return isBreathingItem(stack) || isBloodDemonArtItem(stack);
    }

    public static boolean isBreathingItem(ItemStack stack) {
        return stack != null
            && !stack.isEmpty()
            && (CustomBreathingExecutor.isCustomBreathingSword(stack.getItem())
                || BaseModBreathingExecutor.isBaseModNichirinSword(stack.getItem()));
    }

    public static boolean isBloodDemonArtItem(ItemStack stack) {
        return stack != null
            && !stack.isEmpty()
            && BloodDemonArtExecutor.isBloodDemonArt(stack.getItem());
    }

    public static boolean executeAbility(LivingEntity npc, ItemStack stack) {
        if (npc == null || stack == null || stack.isEmpty()) {
            return false;
        }

        if (isBreathingItem(stack)) {
            return CustomBreathingExecutor.execute(npc, stack);
        }

        if (isBloodDemonArtItem(stack)) {
            return BloodDemonArtExecutor.execute(npc, stack);
        }

        return false;
    }
}
