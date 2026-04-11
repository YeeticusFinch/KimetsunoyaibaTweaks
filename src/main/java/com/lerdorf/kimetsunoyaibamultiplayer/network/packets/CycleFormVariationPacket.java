package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to cycle variations of the current breathing form.
 * Direction: -1 = backward, 1 = forward
 * For base mod swords, also includes the form name (resolved client-side from breathes value)
 */
public class CycleFormVariationPacket {
    private final int direction;
    private final String formName; // For base mod swords (client sends the form name)

    public CycleFormVariationPacket(int direction) {
        this(direction, "");
    }

    public CycleFormVariationPacket(int direction, String formName) {
        this.direction = direction;
        this.formName = formName != null ? formName : "";
    }

    public CycleFormVariationPacket(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
        this.formName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(direction);
        buf.writeUtf(formName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Log.debug("[CycleFormVariationPacket] SERVER received packet, direction={}, formName={}", direction, formName);
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                Log.warn("[CycleFormVariationPacket] Player is null, aborting");
                return;
            }

            ItemStack heldItem = player.getMainHandItem();
            Log.debug("[CycleFormVariationPacket] Player holding: {}", heldItem.getItem());

            // Custom breathing swords
            if (heldItem.getItem() instanceof BreathingSwordItem breathingSword) {
                Log.debug("[CycleFormVariationPacket] Handling CUSTOM breathing sword");
                PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);

                BreathingTechnique technique = breathingSword.getBreathingTechnique();
                String styleKey = PlayerBreathingData.getTechniqueKey(technique.getName());
                int baseFormIndex = data.getCurrentFormIndex(styleKey);
                var form = technique.getForm(baseFormIndex);
                if (form == null) {
                    return;
                }

                int formId = form.getFormId();
                SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(heldItem.getItem());
                String swordId = registeredSword != null ? registeredSword.getSwordId() : breathingSword.toString();

                int variationCount = VariationRegistry.getVariationCount(formId, swordId);
                if (variationCount == 0) {
                    data.setCurrentVariationIndex(0);
                    ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), 0), player);
                    ModNetworking.sendToPlayer(new BreathesValueSyncPacket(player.getUUID(), formId), player);
                    return;
                }

                int currentVariation = data.getCurrentVariationIndex();
                Log.debug("[CycleFormVariationPacket] Custom sword: currentVariation={}, variationCount={}, direction={}",
                    currentVariation, variationCount, direction);

                // Cycle variation with proper wrapping
                // variationCount + 1 = total options (base form + variations)
                // Examples: variationCount=1 means 2 total (0=base, 1=variation)
                //   Forward from 1: (1+1) % 2 = 0 ✓ wraps to base
                //   Backward from 0: (0-1+2) % 2 = 1 ✓ wraps to variation
                int totalOptions = variationCount + 1;
                int newVariation;
                if (direction >= 0) {
                    // Forward: wrap after last variation
                    newVariation = (currentVariation + 1) % totalOptions;
                } else {
                    // Backward: wrap before base form
                    // Add totalOptions to handle negative modulo correctly
                    newVariation = ((currentVariation - 1) % totalOptions + totalOptions) % totalOptions;
                }

                Log.debug("[CycleFormVariationPacket] Custom sword: newVariation={} (display as {}/{})",
                    newVariation, newVariation + 1, totalOptions);

                // Store variation index (don't modify breathes NBT)
                data.setCurrentVariationIndex(newVariation);
                // CRITICAL: Save to NBT so it persists when getOrCreate() loads from NBT later
                PlayerBreathingData.saveToNBT(player);
                ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), newVariation), player);

                if (!Config.suppressFormCycleChat) {
                    Component displayName = getVariationDisplayName(formId, newVariation, swordId, breathingSword, baseFormIndex);
                    player.sendSystemMessage(
                        LocalizationHelper.breathingStyleFromFormId(formId, technique.getName()).copy()
                            .append(Component.literal(" "))
                            .append(displayName)
                    );
                }

                if (Config.logDebug) {
                    Log.debug("Custom sword variation cycled: form " + formId + ", variation " + newVariation);
                }
            }
            // Base mod swords
            else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(heldItem)) {
                Log.debug("[CycleFormVariationPacket] Handling BASE MOD sword, calling handleBaseModSword() with formName={}", formName);
                handleBaseModSword(player, direction, formName);
            }
            else {
                Log.debug("[CycleFormVariationPacket] Not a breathing sword!");
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Handle variation cycling for base mod swords.
     * Uses string-based form name matching instead of form ID.
     * @param player The player cycling variations
     * @param direction Cycle direction (-1 = backward, 1 = forward)
     * @param clientFormName The form name sent from the client (resolved from breathes value)
     */
    private void handleBaseModSword(ServerPlayer player, int direction, String clientFormName) {
        Log.debug("[handleBaseModSword] ENTERED, direction={}, clientFormName={}", direction, clientFormName);
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());

        // Get the held sword to determine sword ID
        ItemStack heldItem = player.getMainHandItem();
        String swordId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(heldItem.getItem()).toString();
        Log.debug("[handleBaseModSword] Held sword ID: {}", swordId);

        // Use the form name sent from the client
        String currentFormName = clientFormName;

        // If client didn't send form name, fall back to cached value
        if (currentFormName == null || currentFormName.isEmpty()) {
            currentFormName = data.getBaseModFormName();
            Log.debug("[handleBaseModSword] Client form name empty, using cached: {}", currentFormName);
        } else {
            Log.debug("[handleBaseModSword] Using form name from client: {}", currentFormName);
            // Cache it for future use
            data.setBaseModFormName(currentFormName);
        }

        if (currentFormName == null || currentFormName.isEmpty()) {
            Log.debug("[handleBaseModSword] No form name available - aborting");
            if (Config.logDebug) {
                Log.debug("[CycleVariation] No form name available - cannot cycle variations");
            }
            return;
        }

        // Use substring matching to get variation count, filtering by sword ID
        int variationCount = VariationRegistry.getVariationCountBySubstring(currentFormName, swordId);
        Log.debug("[handleBaseModSword] Form '{}' has {} variations for sword '{}'", currentFormName, variationCount, swordId);

        // If no variations, reset and sync
        if (variationCount == 0) {
            Log.debug("[handleBaseModSword] No variations found, resetting to 0");
            data.setCurrentVariationIndex(0);
            ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), 0), player);
            if (Config.logDebug) {
                Log.debug("[CycleVariation] No variations found for form: " + currentFormName);
            }
            return;
        }

        // Cycle variation
        int currentVariation = data.getCurrentVariationIndex();
        Log.debug("[handleBaseModSword] Current variation index: {}", currentVariation);
        int newVariation = direction >= 0
                ? (currentVariation + 1) % (variationCount + 1)
                : (currentVariation - 1 + (variationCount + 1)) % (variationCount + 1);
        Log.debug("[handleBaseModSword] New variation index: {}", newVariation);

        // Store variation index
        data.setCurrentVariationIndex(newVariation);
        Log.debug("[handleBaseModSword] Stored new variation index in PlayerData");

        // Sync variation index to client
        ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), newVariation), player);
        Log.debug("[handleBaseModSword] Sent VariationIndexSyncPacket to client");

        // Send chat message about variation change
        // Note: Chat handler will NOT cache these messages (they contain variation names and/or counters)
        if (!Config.suppressFormCycleChat) {
            Component displayName = getBaseModVariationDisplayNameByString(currentFormName, newVariation, swordId);
            player.sendSystemMessage(
                displayName.copy().append(Component.literal(" (" + (newVariation + 1) + "/" + (variationCount + 1) + ")"))
            );
            Log.debug("[handleBaseModSword] Sent chat message: {}", displayName.getString());
        } else {
            Log.debug("[handleBaseModSword] Chat messages suppressed by config");
        }

        if (Config.logDebug) {
            Log.debug("Base mod variation cycled: form '" + currentFormName +
                     "', variation " + newVariation + "/" + variationCount);
        }
    }

    /**
     * Get display name for base mod variation using string-based matching.
     */
    private Component getBaseModVariationDisplayNameByString(String currentFormName, int variationIndex, String swordId) {
        if (variationIndex == 0) {
            int formId = findBaseFormIdByName(currentFormName);
            if (formId != -1) {
                return LocalizationHelper.breathingForm(formId);
            }
            return Component.literal(
                currentFormName.startsWith("§") && currentFormName.length() > 2
                    ? currentFormName.substring(2)
                    : currentFormName
            );
        } else {
            BreathingFormVariation variation = VariationRegistry.getVariationBySubstring(
                currentFormName, variationIndex, swordId
            );
            return variation != null ? variation.getDisplayName() : Component.literal("Unknown Variation");
        }
    }

    /**
     * Extract color code from form name (e.g., "§1Water Breathing..." -> "§1").
     */
    private String extractColorFromFormName(String formName) {
        // First check if the form name already has a color code
        if (formName != null && formName.length() >= 2 && formName.charAt(0) == '§') {
            return formName.substring(0, 2);
        }

        // Look up the color from BaseKnYForms using substring matching
        if (formName != null) {
            // Strip color codes from form name if present for matching
            String cleanFormName = formName.replaceAll("§.", "");

            for (com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.BaseForm form :
                 com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.values()) {
                // Exact match (case-insensitive)
                if (cleanFormName.equalsIgnoreCase(form.name)) {
                    Log.debug("[extractColor] Exact match: '{}' -> color: {}", cleanFormName, form.color);
                    return form.color;
                }
            }

            // Fallback: substring match
            for (com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.BaseForm form :
                 com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.values()) {
                if (cleanFormName.toLowerCase().contains(form.name.toLowerCase()) ||
                    form.name.toLowerCase().contains(cleanFormName.toLowerCase())) {
                    Log.debug("[extractColor] Substring match: '{}' with '{}' -> color: {}", cleanFormName, form.name, form.color);
                    return form.color;
                }
            }

            Log.debug("[extractColor] No match for '{}', using default §b", cleanFormName);
        }

        return "§b"; // Default aqua if no match found
    }

    /**
     * Get display name for current variation.
     * Returns form name for base (index 0), or variation name for variations (index 1+)
     */
    private static Component getVariationDisplayName(
        int formId,
        int variationIndex,
        String swordId,
        BreathingSwordItem sword,
        int formIndex
    ) {
        if (variationIndex == 0) {
            BreathingTechnique technique = sword.getBreathingTechnique();
            var form = technique.getForm(formIndex);
            return form != null ? form.getDisplayName() : Component.literal("Unknown Form");
        } else {
            BreathingFormVariation variation = VariationRegistry.getVariation(formId, variationIndex, swordId);
            return variation != null ? variation.getDisplayName() : Component.literal("Unknown Variation");
        }
    }

    private static int findBaseFormIdByName(String currentFormName) {
        if (currentFormName == null) {
            return -1;
        }
        String cleanFormName = currentFormName.replaceAll("§.", "");
        for (var entry : com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms.forms.entrySet()) {
            if (cleanFormName.equalsIgnoreCase(entry.getValue().name)) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
