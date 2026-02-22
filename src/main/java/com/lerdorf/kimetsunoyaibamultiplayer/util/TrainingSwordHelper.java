package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Utility class for the Training Sword system.
 * Training swords can only access the first form of their breathing style.
 * Any nichirin sword (base mod, our mod, or addon mods) can be converted to a training sword.
 */
public class TrainingSwordHelper {

    /** NBT tag key used to mark an item as a training sword */
    public static final String TRAINING_SWORD_TAG = "TrainingSword";

    /** UUID for the training sword damage reduction modifier */
    private static final UUID TRAINING_SWORD_DAMAGE_MODIFIER_UUID =
        UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d");

    /** Damage reduction amount for training swords (-1 damage) */
    private static final double TRAINING_SWORD_DAMAGE_REDUCTION = -1.0;

    /**
     * Checks if the given ItemStack is a training sword.
     * Detection is based on the presence of the TrainingSword NBT tag,
     * so it works even if the item has been renamed in an anvil.
     *
     * @param stack The ItemStack to check
     * @return true if the item is marked as a training sword
     */
    public static boolean isTrainingSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TRAINING_SWORD_TAG);
    }

    /**
     * Converts a nichirin sword into a training sword.
     * This adds the training sword NBT tag and modifies the display name appropriately.
     * Note: This version does NOT reset the player to first form. Use makeTrainingSword(ItemStack, Player) instead.
     *
     * Naming rules:
     * - "Nichirin Sword (Water)" becomes "Nichirin Training Sword (Water)"
     * - "Nichirin Naginata (Forest)" becomes "Nichirin Training Naginata (Forest)"
     * - If no "Nichirin" in name, prepends "Training " to the front
     *
     * @param stack The ItemStack to convert (must be a nichirin/breathing sword)
     * @return true if the conversion was successful, false if the item is not a valid sword
     */
    public static boolean makeTrainingSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if this is already a training sword
        if (isTrainingSword(stack)) {
            Log.debug("Item is already a training sword");
            return true;
        }

        // Check if this is a valid nichirin/breathing sword
        if (!isNichirinOrBreathingSword(stack)) {
            Log.debug("Item is not a nichirin or breathing sword, cannot convert to training sword");
            return false;
        }

        // Add the training sword NBT tag
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TRAINING_SWORD_TAG, true);

        // Add damage reduction attribute modifier (-1 damage)
        stack.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(
                TRAINING_SWORD_DAMAGE_MODIFIER_UUID,
                "Training sword damage reduction",
                TRAINING_SWORD_DAMAGE_REDUCTION,
                AttributeModifier.Operation.ADDITION
            ),
            EquipmentSlot.MAINHAND
        );

        // Modify the display name (use withStyle to prevent italic text)
        String currentName = stack.getHoverName().getString();
        String newName = generateTrainingSwordName(currentName);
        stack.setHoverName(Component.literal(newName).withStyle(style -> style.withItalic(false)));

        Log.debug("Converted sword to training sword: " + currentName + " -> " + newName + " (damage reduced by 1)");
        return true;
    }

    /**
     * Converts a nichirin sword into a training sword and resets the player to the first form.
     * This is the recommended method to use when converting a sword the player is holding.
     *
     * @param stack The ItemStack to convert (must be a nichirin/breathing sword)
     * @param player The player holding the sword (used to reset to first form)
     * @return true if the conversion was successful, false if the item is not a valid sword
     */
    public static boolean makeTrainingSword(ItemStack stack, Player player) {
        if (!makeTrainingSword(stack)) {
            return false;
        }

        // Reset player to first form
        resetToFirstForm(stack, player);
        return true;
    }

    /**
     * Resets the player to the first form of their current breathing style.
     * Works for both custom swords (BreathingSwordItem) and base mod swords.
     *
     * @param stack The sword ItemStack
     * @param player The player to reset
     */
    public static void resetToFirstForm(ItemStack stack, Player player) {
        if (player == null || stack.isEmpty()) {
            return;
        }

        // Get player breathing data
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());

        // Check if this is a custom breathing sword
        if (stack.getItem() instanceof BreathingSwordItem breathingSword) {
            // Handle custom breathing sword
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique technique =
                breathingSword.getBreathingTechnique();

            // Reset to first form (index 0)
            data.setCurrentFormIndex(0);

            // Get the first form to update breathes value
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm firstForm = technique.getForm(0);
            if (firstForm != null) {
                double formId = firstForm.getFormId();
                player.getPersistentData().putDouble("breathes", formId);
                data.setBaseModBreathesValue(formId);

                // Sync to client if on server
                if (player instanceof ServerPlayer serverPlayer) {
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket(
                            player.getUUID(), formId),
                        serverPlayer
                    );
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket(
                            player.getUUID(), 0)
                    );
                }

                Log.debug("Reset custom sword to first form: " + firstForm.getName());
            }
        } else {
            // Handle base mod sword
            double currentBreathes = player.getPersistentData().getDouble("breathes");

            if (currentBreathes > 0) {
                // Use getFirstFormForTrainingSword which handles black swords specially
                double firstForm = getFirstFormForTrainingSword(stack, currentBreathes);

                if (currentBreathes != firstForm) {
                    player.getPersistentData().putDouble("breathes", firstForm);

                    // Sync to client if on server
                    if (player instanceof ServerPlayer serverPlayer) {
                        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket(
                                player.getUUID(), firstForm),
                            serverPlayer
                        );
                    }

                    Log.debug("Reset base mod sword to first form: " + firstForm);
                }
            }
        }

        // Reset variation index for both sword types
        data.setCurrentVariationIndex(0);
        data.setBaseModFormName("");
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);

        // Sync variation reset if on server
        if (player instanceof ServerPlayer serverPlayer) {
            com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(
                    player.getUUID(), 0),
                serverPlayer
            );
        }
    }

    /**
     * Removes the training sword tag, damage reduction, and restores the original item name.
     * This converts a training sword back to a normal sword.
     *
     * @param stack The training sword ItemStack
     * @return true if the conversion was successful
     */
    public static boolean removeTrainingSword(ItemStack stack) {
        if (stack.isEmpty() || !isTrainingSword(stack)) {
            return false;
        }

        // Remove the training sword NBT tag
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TRAINING_SWORD_TAG);

            // Remove the damage reduction attribute modifier
            // The attribute modifiers are stored in the "AttributeModifiers" tag
            if (tag.contains("AttributeModifiers")) {
                net.minecraft.nbt.ListTag modifiers = tag.getList("AttributeModifiers", 10); // 10 = CompoundTag
                for (int i = modifiers.size() - 1; i >= 0; i--) {
                    CompoundTag modifier = modifiers.getCompound(i);
                    if (modifier.hasUUID("UUID")) {
                        UUID modifierUUID = modifier.getUUID("UUID");
                        if (TRAINING_SWORD_DAMAGE_MODIFIER_UUID.equals(modifierUUID)) {
                            modifiers.remove(i);
                            Log.debug("Removed training sword damage modifier");
                        }
                    }
                }
                // If no modifiers left, remove the tag entirely
                if (modifiers.isEmpty()) {
                    tag.remove("AttributeModifiers");
                }
            }
        }

        // Reset the custom name (will revert to default item name)
        // Note: This clears any custom name, including "Training" prefix
        stack.resetHoverName();

        Log.debug("Removed training sword tag from item");
        return true;
    }

    /**
     * Generates the appropriate training sword name based on the original name.
     *
     * Rules:
     * - If name contains "Nichirin ", insert "Training " after "Nichirin "
     * - Otherwise, prepend "Training " to the front
     *
     * @param originalName The original item name
     * @return The new training sword name
     */
    public static String generateTrainingSwordName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "Training Sword";
        }

        // Check if name already contains "Training"
        if (originalName.contains("Training")) {
            return originalName; // Already has training in the name
        }

        // Look for "Nichirin " in the name (case-insensitive search, but preserve case in result)
        String lowerName = originalName.toLowerCase();
        int nichirinIndex = lowerName.indexOf("nichirin ");

        if (nichirinIndex >= 0) {
            // Insert "Training " after "Nichirin "
            int insertPosition = nichirinIndex + "nichirin ".length();
            return originalName.substring(0, insertPosition) + "Training " + originalName.substring(insertPosition);
        }

        // No "Nichirin" found, prepend "Training " to front
        return "Training " + originalName;
    }

    /**
     * Checks if the given ItemStack is a nichirin or breathing sword from any supported mod.
     * This includes:
     * - Base mod (kimetsunoyaiba) nichirin swords
     * - Our mod's (kimetsunoyaibamultiplayer) breathing swords
     * - Addon mods that register swords through our API
     *
     * @param stack The ItemStack to check
     * @return true if the item is a nichirin/breathing sword
     */
    public static boolean isNichirinOrBreathingSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Use the existing detection from SwordParticleMapping which already handles all cases
        return SwordParticleMapping.isKimetsunoyaibaSword(stack);
    }

    /**
     * Gets the first form ID for a breathing style based on the current form.
     * Used to reset training swords to their first form.
     *
     * @param currentBreathes The current breathing form value
     * @return The first form ID for that breathing style
     */
    public static double getFirstFormForStyle(double currentBreathes) {
        if (currentBreathes <= 0) {
            return currentBreathes;
        }

        // Decode if it's an encoded variation value
        if (VariationEncoder.isEncoded(currentBreathes)) {
            currentBreathes = VariationEncoder.getFormId(currentBreathes);
        }

        int currentFormId = (int) currentBreathes;
        int breathingStyle = (currentFormId / 100) * 100;

        // Get the forms for this style
        int[] forms = BaseModStyleMapping.getFormsForStyle(breathingStyle);

        if (forms.length > 0) {
            return forms[0]; // Return the first form
        }

        // Fallback: return style base + 1 (e.g., 100 -> 101)
        return breathingStyle + 1;
    }

    /**
     * Gets the first form ID for a training sword, taking into account the sword type.
     * Black swords use water breathing for training purposes.
     *
     * @param sword The sword ItemStack
     * @param currentBreathes The current breathing form value
     * @return The first form ID for training
     */
    public static double getFirstFormForTrainingSword(ItemStack sword, double currentBreathes) {
        // Check if this is a black sword - black swords should use water breathing (100s) for training
        if (isBlackSword(sword)) {
            Log.debug("Black sword detected for training - using water breathing first form (101)");
            return 101; // Water breathing first form
        }

        return getFirstFormForStyle(currentBreathes);
    }

    /**
     * Check if the given sword is a black nichirin sword.
     *
     * @param sword The sword ItemStack
     * @return true if this is a black nichirin sword
     */
    public static boolean isBlackSword(ItemStack sword) {
        if (sword == null || sword.isEmpty()) {
            return false;
        }

        net.minecraft.resources.ResourceLocation itemId =
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(sword.getItem());
        if (itemId == null) {
            return false;
        }

        String itemIdString = itemId.toString().toLowerCase();
        return itemIdString.contains("nichirinsword_black") ||
               itemIdString.contains("black_nichirin") ||
               (itemIdString.contains("black") && itemIdString.contains("sword"));
    }
}
