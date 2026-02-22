package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles sword transformation overrides and training sword name fixes.
 *
 * This handler has two functions:
 *
 * 1. When replaceColorChangingProcedure config is enabled:
 *    - Blocks the base mod's ColorChangeProcedure by resetting cnt_x counter
 *    - Logs when transformation would have occurred
 *
 * 2. Always active for training swords:
 *    - Detects when a training sword has been transformed by the base mod
 *    - The base mod copies NBT (including custom name) when transforming
 *    - Fixes the name to match the new sword type (e.g., "Nichirin Training Sword (Water)")
 *    - Re-applies training sword modifications if needed
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class ColorChangeProcedureOverride {

    // Base mod nichirinsword registry name (the one that gets transformed)
    private static final String NICHIRINSWORD = "kimetsunoyaiba:nichirinsword";

    // NBT key used by base mod to count ticks held
    private static final String CNT_X_TAG = "cnt_x";

    // Threshold at which base mod transforms the sword
    private static final double TRANSFORM_THRESHOLD = 30.0;

    /**
     * On player tick, handle:
     * 1. Blocking base mod transformation if config is enabled
     * 2. Fixing training sword names after transformation
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide()) {
            return; // Only run on server
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
        if (itemId == null) {
            return;
        }

        String itemIdString = itemId.toString();

        // Handle blocking transformation if config is enabled
        if (CustomProgressionConfig.replaceColorChangingProcedure.get()) {
            if (itemIdString.equals(NICHIRINSWORD)) {
                handleTransformationBlock(player, mainHand);
            }
        }

        // Always handle training sword name fixes (for any nichirin sword)
        if (TrainingSwordHelper.isTrainingSword(mainHand)) {
            handleTrainingSwordNameFix(player, mainHand, itemId);
        }
    }

    private static final Random RANDOM = new Random();

    /**
     * Handle the transformation of a nichirinsword using the custom color change system.
     * When the cnt_x counter reaches the threshold, transform to a random level-0 sword.
     */
    private static void handleTransformationBlock(Player player, ItemStack sword) {
        CompoundTag tag = sword.getTag();
        if (tag == null) {
            return;
        }

        if (!tag.contains(CNT_X_TAG)) {
            return;
        }

        double cntX = tag.getDouble(CNT_X_TAG);

        // Check if we've reached the transformation threshold
        // We transform at THRESHOLD - 1 (29) to beat the base mod which transforms at 30
        if (cntX >= TRANSFORM_THRESHOLD - 1) {
            // Perform the custom transformation
            performCustomTransformation(player, sword);

            // Reset counter after transformation to prevent base mod from also transforming
            tag.putDouble(CNT_X_TAG, 0.0);

            if (CustomProgressionConfig.enableDebugLogging.get()) {
                System.out.println("[KnY-MP ColorChangeOverride] Performed custom transformation for " +
                    player.getName().getString() + " at cnt_x=" + cntX);
            }
        }
        // Note: We intentionally do NOT reset cnt_x every tick anymore.
        // The previous code was resetting cnt_x to 0 every tick, which prevented
        // the counter from ever reaching 29 for custom transformation.
        // Now we let the base mod increment cnt_x naturally, and intercept at 29.
    }

    /**
     * Perform the custom sword transformation using registered style and sword metadata.
     */
    private static void performCustomTransformation(Player player, ItemStack originalSword) {
        // Check if original sword was a training sword BEFORE we do anything else
        boolean wasTrainingSword = TrainingSwordHelper.isTrainingSword(originalSword);

        // Get all color-change-eligible styles
        List<StyleMetadataRegistry.StyleMetadata> eligibleStyles =
                StyleMetadataRegistry.getColorChangeEligibleStyles();

        if (Config.logDebug)
            for (StyleMetadataRegistry.StyleMetadata style : eligibleStyles)
                System.out.println("[Eligible Style] " + style.getStyleId());

        if (eligibleStyles.isEmpty()) {
            System.out.println("[KnY-MP ColorChangeOverride] No color-change-eligible styles registered!");
            return;
        }

        // Pick a random eligible style
        StyleMetadataRegistry.StyleMetadata chosenStyle =
            eligibleStyles.get(RANDOM.nextInt(eligibleStyles.size()));

        // Get all level-0 swords for that style (from both registries)
        List<Item> eligibleSwords = getEligibleSwordsForStyle(chosenStyle.getStyleId());

        if (eligibleSwords.isEmpty()) {
            System.out.println("[KnY-MP ColorChangeOverride] No level-0 swords found for style: " +
                chosenStyle.getStyleId());
            return;
        }

        // Pick a random sword
        Item chosenSword = eligibleSwords.get(RANDOM.nextInt(eligibleSwords.size()));
        ResourceLocation swordId = ForgeRegistries.ITEMS.getKey(chosenSword);

        System.out.println("[KnY-MP ColorChangeOverride] Transforming sword for " +
            player.getName().getString() + " -> " + swordId +
            " (style: " + chosenStyle.getStyleId() + ")" +
            (wasTrainingSword ? " [Training Sword]" : ""));

        // Create the new sword item stack - DO NOT copy metadata from original sword
        // The new sword should start fresh with its default properties
        ItemStack newSword = new ItemStack(chosenSword);

        // If the original sword was a training sword, apply training sword modifications to the new one
        if (wasTrainingSword) {
            TrainingSwordHelper.makeTrainingSword(newSword, player);
            System.out.println("[KnY-MP ColorChangeOverride] Applied training sword tag to new sword");
        }

        // Replace the sword in the player's hand
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, newSword);

        // Play transformation sound
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Send transformation message to player
        String styleName = formatStyleName(chosenStyle.getStyleId());
        Component message = Component.literal("Your blade has awakened as a " + styleName + " sword!");
        player.displayClientMessage(message, true);  // Action bar message
        player.sendSystemMessage(message);           // Chat message
    }

    /**
     * Get all level-0 swords for a given style from both registries.
     * When enhanced breathing is enabled for a style, prefer our enhanced swords.
     */
    private static List<Item> getEligibleSwordsForStyle(String styleId) {
        List<Item> swords = new ArrayList<>();

        // Check if we should prefer enhanced swords for this style
        boolean preferEnhanced = shouldPreferEnhancedSwords(styleId);

        // Get from SwordRegistry first (addon swords using BreathingSwordItem - our enhanced versions)
        List<SwordRegistry.RegisteredSword> addonSwords =
            SwordRegistry.getSwordsByStyleAndLevel(styleId, 0);
        for (SwordRegistry.RegisteredSword registered : addonSwords) {
            swords.add(registered.getSwordItem());
        }

        // If we found enhanced swords and should prefer them, return early
        if (!swords.isEmpty() && preferEnhanced) {
            return swords;
        }

        // Get from SwordMetadataRegistry (base mod swords)
        List<SwordMetadataRegistry.SwordMetadata> baseSwords =
            SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0);
        for (SwordMetadataRegistry.SwordMetadata meta : baseSwords) {
            Item item = meta.getSwordItem();
            if (item != null) {
                swords.add(item);
            }
        }

        return swords;
    }

    /**
     * Check if we should prefer enhanced swords for a given style.
     * This is based on the enhanced breathing config.
     */
    private static boolean shouldPreferEnhancedSwords(String styleId) {
        return switch (styleId) {
            case "mist_breathing" -> com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.enhancedMistBreathing;
            case "love_breathing" -> com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.enhancedLoveBreathing;
            default -> false;
        };
    }

    /**
     * Format a style ID into a readable name.
     */
    private static String formatStyleName(String styleId) {
        // water_breathing -> Water Breathing
        return styleId.replace("_", " ")
            .substring(0, 1).toUpperCase() +
            styleId.replace("_", " ").substring(1);
    }

    /**
     * Fix training sword names after the base mod transforms the sword.
     *
     * When the base mod transforms a nichirinsword into a colored version (e.g., nichirinsword_water),
     * it copies all NBT including the custom display name. This means a training sword keeps
     * "Nichirin Training Sword" instead of becoming "Nichirin Training Sword (Water)".
     *
     * This method detects the mismatch and fixes the name.
     */
    private static void handleTrainingSwordNameFix(Player player, ItemStack sword, ResourceLocation itemId) {
        // Get the current display name
        String currentName = sword.getHoverName().getString();

        // Get what the default name should be for this sword type
        String defaultName = new ItemStack(sword.getItem()).getHoverName().getString();

        // Generate what the training sword name SHOULD be based on the default name
        String expectedTrainingName = TrainingSwordHelper.generateTrainingSwordName(defaultName);

        // If the current name doesn't match the expected training name, fix it
        if (!currentName.equals(expectedTrainingName)) {
            // Check if it looks like a training sword name (contains "Training")
            // This prevents us from overwriting intentional custom names
            if (currentName.contains("Training")) {
                sword.setHoverName(Component.literal(expectedTrainingName).withStyle(style -> style.withItalic(false)));

                System.out.println("[KnY-MP ColorChangeOverride] Fixed training sword name for " +
                    player.getName().getString() + ": '" + currentName + "' -> '" + expectedTrainingName + "'");
            }
        }
    }
}
