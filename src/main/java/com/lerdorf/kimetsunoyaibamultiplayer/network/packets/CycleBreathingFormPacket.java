package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

            // Check if this is a training sword - training swords can only use first form
            if (TrainingSwordHelper.isTrainingSword(heldItem)) {
                handleTrainingSwordCycleAttempt(player, heldItem);
                return;
            }

            // Check if this is a custom breathing sword (from our API)
            if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
                // Handle custom breathing sword cycling
                // Note: cycleForm() handles all syncing internally (FormSyncPacket + BreathesValueSyncPacket)
                breathingSword.cycleForm(player, direction < 0);
                // CRITICAL: Reset variation selection AND form name cache when cycling forms
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                data.setCurrentVariationIndex(0);
                data.setBaseModFormName(""); // Clear cached form name so it gets fresh from breathes value
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
                net.minecraft.nbt.CompoundTag heldTag = heldItem.getTag();
                if (heldTag != null && heldTag.contains("select")) {
                    double selectOffset = heldTag.getDouble("select");
                    // Reset variation selection AND form name cache even when deferring form cycling to the base mod
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                    data.setCurrentVariationIndex(0);
                    data.setBaseModFormName(""); // Clear cached form name so it gets fresh from breathes value
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

    /**
     * Handles when a player tries to cycle forms with a training sword.
     * Training swords can only use the first form, so we reset to first form and notify the player.
     */
    private void handleTrainingSwordCycleAttempt(ServerPlayer player, ItemStack heldItem) {
        // Get player breathing data
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());

        // Check if this is a custom breathing sword (from our API)
        if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem breathingSword) {
            // Handle custom breathing sword training restriction
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique technique =
                breathingSword instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack blackSword
                    ? blackSword.getEffectiveTechnique(heldItem, player)
                    : breathingSword.getBreathingTechnique();
            String styleKey = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData
                .getTechniqueKey(technique != null ? technique.getName() : breathingSword.getClass().getName());

            // Reset to first form (index 0)
            if (data.getCurrentFormIndex(styleKey) != 0) {
                data.setCurrentFormIndex(styleKey, 0);

                // Get the first form to update breathes value
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm firstForm = technique.getForm(0);
                if (firstForm != null) {
                    double formId = firstForm.getFormId();
                    player.getPersistentData().putDouble("breathes", formId);
                    data.setBaseModBreathesValue(formId);

                    // Sync breathes value
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                        new BreathesValueSyncPacket(player.getUUID(), formId),
                        player
                    );

                    // Sync form index
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket(
                            player.getUUID(), styleKey, 0)
                    );
                }
            }
        } else {
            // Handle base mod sword training restriction
            double currentBreathes = player.getPersistentData().getDouble("breathes");

            if (currentBreathes == 0.0) {
                // Send message anyway since they're trying to cycle
                player.displayClientMessage(
                    Component.literal("\u00A7e[Training Sword] \u00A7fOnly the 1st Form is available on a training sword."),
                    true
                );
                return;
            }

            // Get the first form for this style (handles black swords specially)
            double firstForm = TrainingSwordHelper.getFirstFormForTrainingSword(heldItem, currentBreathes);

            // Reset to first form if not already there
            if (currentBreathes != firstForm) {
                player.getPersistentData().putDouble("breathes", firstForm);

                // Sync the breathes value to the client
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new BreathesValueSyncPacket(player.getUUID(), firstForm),
                    player
                );
            }
        }

        // Reset variation index for both sword types
        data.setCurrentVariationIndex(0);
        data.setBaseModFormName("");
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.saveToNBT(player);

        // Sync variation reset
        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
            player
        );

        // Send message to player
        player.displayClientMessage(
            Component.literal("\u00A7e[Training Sword] \u00A7fOnly the 1st Form is available on a training sword."),
            true // Display in action bar
        );

        if (Config.logDebug) {
            Log.debug("Training sword form cycle blocked - player tried to cycle with training sword");
        }
    }
}
