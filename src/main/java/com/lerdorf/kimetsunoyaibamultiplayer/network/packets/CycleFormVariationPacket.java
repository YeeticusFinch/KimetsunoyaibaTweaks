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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to cycle variations of the current breathing form.
 * Direction: -1 = backward, 1 = forward
 * For base mod swords, also includes the breathes value (since server-side NBT may be stale)
 */
public class CycleFormVariationPacket {
    private final int direction;
    private final double breathesValue; // For base mod swords (client sends this)

    public CycleFormVariationPacket(int direction) {
        this(direction, 0.0);
    }

    public CycleFormVariationPacket(int direction, double breathesValue) {
        this.direction = direction;
        this.breathesValue = breathesValue;
    }

    public CycleFormVariationPacket(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
        this.breathesValue = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(direction);
        buf.writeDouble(breathesValue);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack heldItem = player.getMainHandItem();

            // Custom breathing swords
            if (heldItem.getItem() instanceof BreathingSwordItem breathingSword) {
                PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
                int baseFormIndex = data.getCurrentFormIndex();

                BreathingTechnique technique = breathingSword.getBreathingTechnique();
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
                int newVariation = direction >= 0
                        ? (currentVariation + 1) % (variationCount + 1)
                        : (currentVariation - 1 + (variationCount + 1)) % (variationCount + 1);

                // Store variation index (don't modify breathes NBT)
                data.setCurrentVariationIndex(newVariation);
                ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), newVariation), player);

                if (!Config.suppressFormCycleChat) {
                    String displayName = getVariationDisplayName(formId, newVariation, swordId, breathingSword, baseFormIndex);
                    // Use one color: base form color (from technique formColor) for the entire line
                    String color = technique.getFormColor() != null ? technique.getFormColor() : "§f";
                    String message = color + technique.getName() + " - " + color + displayName;
                    player.sendSystemMessage(Component.literal(message));
                }

                if (Config.logDebug) {
                    Log.debug("Custom sword variation cycled: form " + formId + ", variation " + newVariation);
                }
            }
            // Base mod swords
            else if (com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(heldItem)) {
                handleBaseModSword(player, direction);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Handle variation cycling for base mod swords.
     */
    private void handleBaseModSword(ServerPlayer player, int direction) {
        ItemStack heldSword = player.getMainHandItem();

        // Get breathes value from server NBT, cache, or client
        double currentBreathes = player.getPersistentData().getDouble("breathes");
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());
        if (currentBreathes == 0.0) {
            currentBreathes = data.getBaseModBreathesValue();
        }
        if (currentBreathes == 0.0) {
            currentBreathes = this.breathesValue;
        }

        // CRITICAL FIX: Detect and correct out-of-range breathes values
        // Base mod sometimes writes form ID with +500 offset (e.g., 602 instead of 102)
        // We need to detect this and correct it to the TRUE form ID
        int expectedRange = BaseModStyleMapping.getExpectedRangeForSword(heldSword);
        int actualRange = ((int)currentBreathes / 100) * 100;

        double trueBreathes = currentBreathes;
        if (expectedRange > 0 && actualRange != expectedRange && actualRange == expectedRange + 500) {
            // Breathes is in wrong range (e.g., 602 when expecting 100s range)
            // Subtract 500 to get true form ID
            trueBreathes = currentBreathes - 500;
            if (Config.logDebug) {
                Log.debug("[CycleVariation] Corrected out-of-range breathes: " + currentBreathes +
                         " -> " + trueBreathes + " (expected range: " + expectedRange + ", was: " + actualRange + ")");
            }
        }

        int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(trueBreathes);

        if (Config.logDebug) {
            Log.debug("[CycleVariation] Variation cycling: formId=" + formId +
                     " (breathes=" + currentBreathes + ", corrected=" + trueBreathes + ")");
        }

        if (!BaseModStyleMapping.isKnownFormId(formId)) {
            // Try to recover from cached known form
            int cachedFormId = BaseModStyleMapping.isKnownFormId((int) data.getBaseModBreathesValue())
                ? (int) data.getBaseModBreathesValue()
                : 0;
            if (cachedFormId > 0) {
                formId = cachedFormId;
                currentBreathes = cachedFormId;
            }
        }

        if (formId == 0 || !BaseModStyleMapping.isKnownFormId(formId)) {
            return;
        }

        int variationCount = VariationRegistry.getVariationCount(formId, null);

        // If no variations, reset and sync
        if (variationCount == 0) {
            data.setCurrentVariationIndex(0);
            data.setBaseModBreathesValue(formId);
            player.getPersistentData().putDouble("breathes", formId);
            ModNetworking.sendToPlayer(new BreathesValueSyncPacket(player.getUUID(), formId), player);
            ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), 0), player);
            return;
        }

        int currentVariation = data.getCurrentVariationIndex();
        int newVariation = direction >= 0
                ? (currentVariation + 1) % (variationCount + 1)
                : (currentVariation - 1 + (variationCount + 1)) % (variationCount + 1);

        // CRITICAL: Don't touch the breathes NBT!
        // The base mod needs it as-is (e.g., 602) for sword swing animations/particles
        // We only need to track variation index separately for form execution

        // Cache the TRUE form ID (for variation lookups) and variation index
        data.setBaseModBreathesValue(formId);
        data.setCurrentVariationIndex(newVariation);

        // Sync variation index to client (DON'T sync breathes - leave it untouched)
        ModNetworking.sendToPlayer(new VariationIndexSyncPacket(player.getUUID(), newVariation), player);

        // Send chat message about variation change (single color taken from form color if available)
        if (!Config.suppressFormCycleChat) {
            String displayName = getBaseModVariationDisplayName(formId, newVariation, data);
            String styleName = getStyleDisplayName(formId);
            String color = "§b"; // default aqua; we will reuse style color if available from cached form name
            String cachedBase = data.getBaseModFormName();
            if (cachedBase != null && cachedBase.length() >= 2 && cachedBase.charAt(0) == '§') {
                color = cachedBase.substring(0, 2);
            }
            player.sendSystemMessage(Component.literal(color + styleName + " - " + color + displayName));
        }

        if (Config.logDebug) {
            Log.debug("Base mod variation cycled: breathes " + currentBreathes +
                     " -> base " + formId +
                     " (form ID " + formId + ", variation " + newVariation + ")");
        }
    }

    /**
     * Get display name for base mod variation.
     */
    private String getBaseModVariationDisplayName(int formId, int variationIndex, PlayerBreathingData.PlayerData data) {
        if (variationIndex == 0) {
            String cachedName = data.getBaseModFormName();
            if (cachedName != null && !cachedName.isEmpty()) {
                return cachedName;
            }
            return "Form ID " + formId;
        } else {
            BreathingFormVariation variation = VariationRegistry.getVariation(formId, variationIndex, null);
            return variation != null ? variation.getName() : "Unknown Variation";
        }
    }

    /**
     * Convert form ID to style display name.
     */
    private String getStyleDisplayName(int formId) {
        int styleRange = (formId / 100) * 100;

        return switch (styleRange) {
            case 100 -> "Water Breathing";
            case 200 -> "Beast Breathing";
            case 300 -> "Thunder Breathing";
            case 400 -> "Flame Breathing";
            case 500 -> "Wind Breathing";
            case 600 -> "Stone Breathing";
            case 700 -> "Mist Breathing";
            case 800 -> "Serpent Breathing";
            case 900 -> "Sound Breathing";
            case 1000 -> "Ice Breathing";
            case 1100 -> "Moon Breathing";
            case 1200 -> "Sun Breathing";
            case 1300 -> "Flower Breathing";
            case 1400 -> "Insect Breathing";
            case 1500 -> "Love Breathing";
            case 1600 -> "Frost Breathing";
            default -> "Unknown Style";
        };
    }

    /**
     * Get display name for current variation.
     * Returns form name for base (index 0), or variation name for variations (index 1+)
     */
    private static String getVariationDisplayName(
        int formId,
        int variationIndex,
        String swordId,
        BreathingSwordItem sword,
        int formIndex
    ) {
        if (variationIndex == 0) {
            BreathingTechnique technique = sword.getBreathingTechnique();
            var form = technique.getForm(formIndex);
            return form != null ? form.getName() : "Unknown Form";
        } else {
            BreathingFormVariation variation = VariationRegistry.getVariation(formId, variationIndex, swordId);
            return variation != null ? variation.getName() : "Unknown Variation";
        }
    }
}
