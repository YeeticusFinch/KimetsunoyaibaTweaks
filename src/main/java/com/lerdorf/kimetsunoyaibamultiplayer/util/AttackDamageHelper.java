package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * Utility for resolving per-hand attack damage in dual-wield scenarios.
 *
 * Minecraft only applies weapon attribute modifiers from the main hand by default.
 * This helper estimates offhand damage by swapping weapon contribution:
 *   currentAttackDamage - mainHandWeaponBonus + selectedHandWeaponBonus
 */
public final class AttackDamageHelper {

    private AttackDamageHelper() {}

    public static float getAttackDamageForHand(LivingEntity attacker, InteractionHand hand) {
        if (attacker == null) {
            return 0.0F;
        }

        double currentDamage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (hand == InteractionHand.MAIN_HAND) {
            return (float) currentDamage;
        }

        ItemStack mainHand = attacker.getMainHandItem();
        ItemStack selected = attacker.getItemInHand(hand);

        double mainHandBonus = getMainHandWeaponDamageBonus(mainHand);
        double selectedBonus = getMainHandWeaponDamageBonus(selected);

        double adjusted = currentDamage - mainHandBonus + selectedBonus;
        return (float) Math.max(0.0D, adjusted);
    }

    public static float getAverageDualWieldDamage(LivingEntity attacker) {
        float main = getAttackDamageForHand(attacker, InteractionHand.MAIN_HAND);
        float off = getAttackDamageForHand(attacker, InteractionHand.OFF_HAND);
        return (main + off) * 0.5F;
    }

    private static double getMainHandWeaponDamageBonus(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }

        Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers =
            stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        Collection<AttributeModifier> attackDamageModifiers = modifiers.get(Attributes.ATTACK_DAMAGE);

        if (attackDamageModifiers == null || attackDamageModifiers.isEmpty()) {
            return 0.0D;
        }

        // Weapon attack damage modifiers are typically ADDITION on swords.
        double bonus = 0.0D;
        for (AttributeModifier modifier : attackDamageModifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                bonus += modifier.getAmount();
            }
        }
        return bonus;
    }
}
