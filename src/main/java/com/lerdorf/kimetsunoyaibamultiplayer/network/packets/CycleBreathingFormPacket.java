package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to cycle breathing forms.
 * Direction: -1 = backward, 1 = forward
 */
public class CycleBreathingFormPacket {
    private final int direction;

    public CycleBreathingFormPacket(int direction) {
        this.direction = direction;
    }

    public CycleBreathingFormPacket(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(direction);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack heldItem = player.getMainHandItem();

            // Check if this is a custom breathing sword (from our API)
            if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
                // Handle custom breathing sword cycling
                // Note: cycleForm() handles all syncing internally (FormSyncPacket + BreathesValueSyncPacket)
                breathingSword.cycleForm(player, direction < 0);
                // CRITICAL: Reset variation selection when cycling forms
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                data.setCurrentVariationIndex(0);
                // Save to NBT to persist the reset
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);
                // Sync to client
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
                    player
                );

                if (Config.logDebug) {
                    Log.debug("Cycled custom breathing form for: " + heldItem.getItem().toString() +
                             " (direction: " + (direction >= 0 ? "forward" : "backward") + "), reset variation to 0");
                }
            } else {
                // Handle base mod (kimetsunoyaiba) breathing form cycling
                // CRITICAL: Check if this is a multi-style sword - if so, let base mod handle it
                if (heldItem.getOrCreateTag().contains("select")) {
                    double selectOffset = heldItem.getOrCreateTag().getDouble("select");
                    // Reset variation selection even when deferring form cycling to the base mod
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                    data.setCurrentVariationIndex(0);
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
                        player
                    );
                    if (Config.logDebug) {
                        Log.debug("[CycleBreathingForm] Multi-style sword detected (select=" + selectOffset + "), reset variation to 0 and skipping our cycling logic - let base mod handle it");
                    }
                    return; // Let the base mod's cycling logic handle multi-style swords
                }

                // Get current breathing value (should be exact form ID like 101, 102, 601, 602, etc.)
                double currentBreathes = player.getPersistentData().getDouble("breathes");

                if (Config.logDebug) {
                    Log.debug("[CycleBreathingForm] Single-style base mod sword. Current breathes NBT: " + currentBreathes +
                             ", Sword item: " + heldItem.getItem());
                }

                if (currentBreathes == 0.0) {
                    return; // No breathing form active
                }

                // Calculate new breathing value based on direction
                // For single-style swords, we don't need to worry about selectOffset
                double newBreathes = cycleBreathingFormSimple(currentBreathes, direction);

                if (Config.logDebug) {
                    Log.debug("[CycleBreathingForm] Calculated new breathes: " + newBreathes);
                }

                // CRITICAL: Reset to base form (no variation) when form changes
                // If newBreathes is encoded (from a previous variation), decode and reset
                if (com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.isEncoded(newBreathes)) {
                    int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(newBreathes);
                    newBreathes = formId; // Reset to base form (no encoding)
                }

                // Update player's breathing form
                player.getPersistentData().putDouble("breathes", newBreathes);

                // Cache for BaseModVariationHandler and reset variation
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                data.setBaseModBreathesValue(newBreathes);
                data.setCurrentVariationIndex(0); // CRITICAL: reset variation on form change
                // Save to NBT to persist the reset
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);

                // Sync the new breathes value to the client
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new BreathesValueSyncPacket(player.getUUID(), newBreathes),
                    player
                );
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
                    player
                );

                if (Config.logDebug) {
                    Log.debug("Cycled base mod breathing form: " + currentBreathes + " -> " + newBreathes +
                             " (direction: " + (direction == 1 ? "forward" : "backward") + "), reset variation to 0");
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Simplified cycling for single-style swords - no selectOffset needed.
     * The breathes NBT value IS the form ID (e.g., 101, 102, 601, 602).
     */
    private static double cycleBreathingFormSimple(double current, int direction) {
        // Determine breathing style from current form ID
        int currentFormId = (int) current;
        int breathingStyle = (currentFormId / 100) * 100;

        if (Config.logDebug) {
            Log.debug("[cycleBreathingFormSimple] Input: current=" + current +
                     ", formId=" + currentFormId +
                     ", breathingStyle=" + breathingStyle +
                     ", direction=" + direction);
        }

        // Get form list for this style
        int[] forms = getFormsForStyle(breathingStyle);

        if (Config.logDebug) {
            Log.debug("[cycleBreathingFormSimple] Found " + forms.length + " forms for style " + breathingStyle);
            if (forms.length > 0) {
                Log.debug("[cycleBreathingFormSimple] First form: " + forms[0] + ", Last form: " + forms[forms.length - 1]);
            }
        }

        if (forms.length == 0) {
            if (Config.logDebug) {
                Log.debug("[cycleBreathingFormSimple] Unknown style, returning current: " + current);
            }
            return current; // Unknown style
        }

        // Find current form in the array
        int currentIndex = -1;
        for (int i = 0; i < forms.length; i++) {
            if (forms[i] == currentFormId) {
                currentIndex = i;
                break;
            }
        }

        if (Config.logDebug) {
            Log.debug("[cycleBreathingFormSimple] Current index: " + currentIndex + " (searching for " + currentFormId + ")");
        }

        // If current form not found, default to first form
        if (currentIndex == -1) {
            if (Config.logDebug) {
                Log.debug("[cycleBreathingFormSimple] Form not found in list, defaulting to first form: " + forms[0]);
            }
            return forms[0];
        }

        // Calculate new index with wraparound
        int newIndex = currentIndex + direction;

        if (newIndex < 0) {
            newIndex = forms.length - 1; // Wrap to last
        } else if (newIndex >= forms.length) {
            newIndex = 0; // Wrap to first
        }

        int newFormId = forms[newIndex];

        if (Config.logDebug) {
            Log.debug("[cycleBreathingFormSimple] New index: " + newIndex + ", New form ID: " + newFormId);
        }

        return newFormId;
    }

    /**
     * Gets the list of available forms for a breathing style.
     */
    private static int[] getFormsForStyle(int style) {
        return BaseModStyleMapping.getFormsForStyle(style);
    }
}
