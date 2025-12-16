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
                // Reset variation selection
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                data.setCurrentVariationIndex(0);
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(player.getUUID(), 0),
                    player
                );

                if (Config.logDebug) {
                    Log.debug("Cycled custom breathing form for: " + heldItem.getItem().toString() +
                             " (direction: " + (direction >= 0 ? "forward" : "backward") + ")");
                }
            } else {
                // Handle base mod (kimetsunoyaiba) breathing form cycling
                // Get current breathing value
                double currentBreathes = player.getPersistentData().getDouble("breathes");

                if (currentBreathes == 0.0) {
                    return; // No breathing form active
                }

                // Calculate new breathing value based on direction
                double newBreathes = cycleBreathingForm(currentBreathes, direction, heldItem);

                // CRITICAL: Reset to base form (no variation) when form changes
                // If newBreathes is encoded (from a previous variation), decode and reset
                if (com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.isEncoded(newBreathes)) {
                    int formId = com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder.getFormId(newBreathes);
                    newBreathes = formId; // Reset to base form (no encoding)
                }

                // Update player's breathing form
                player.getPersistentData().putDouble("breathes", newBreathes);

                // Cache for BaseModVariationHandler
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.PlayerData data =
                    com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData.getOrCreate(player.getUUID());
                data.setBaseModBreathesValue(newBreathes);
                data.setCurrentVariationIndex(0); // reset variation on form change

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
                             " (direction: " + (direction == 1 ? "forward" : "backward") + "), reset to base form");
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Cycles through breathing forms with wraparound.
     */
    private static double cycleBreathingForm(double current, int direction, ItemStack sword) {
        // Check if multi-style sword with offset
        double selectOffset = 0.0;
        if (sword != null && !sword.isEmpty() && sword.getOrCreateTag().contains("select")) {
            selectOffset = sword.getOrCreateTag().getDouble("select");
        }

        // Calculate actual breathes (considering multi-style offset)
        double actualBreathes = current + selectOffset;
        int breathingStyle = (int)(actualBreathes / 100) * 100;

        // Define form lists for each breathing style
        int[] forms = getFormsForStyle(breathingStyle);

        if (forms.length == 0) {
            return current; // Unknown style
        }

        // Find current index in the array
        int currentIndex = -1;
        for (int i = 0; i < forms.length; i++) {
            if (forms[i] == (int)actualBreathes) {
                currentIndex = i;
                break;
            }
        }

        // If current form not found, default to first form
        if (currentIndex == -1) {
            return forms[0] - selectOffset;
        }

        // Calculate new index with wraparound
        int newIndex = currentIndex + direction;

        if (newIndex < 0) {
            // Wrap to last form
            newIndex = forms.length - 1;
        } else if (newIndex >= forms.length) {
            // Wrap to first form
            newIndex = 0;
        }

        // Return new breathes value (subtract offset for multi-style swords)
        return forms[newIndex] - selectOffset;
    }

    /**
     * Gets the list of available forms for a breathing style.
     */
    private static int[] getFormsForStyle(int style) {
        return BaseModStyleMapping.getFormsForStyle(style);
    }
}
