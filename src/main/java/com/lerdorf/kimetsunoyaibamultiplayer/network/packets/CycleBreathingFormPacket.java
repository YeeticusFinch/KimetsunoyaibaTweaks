package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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
                breathingSword.cycleForm(player, direction < 0);
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

                // Update player's breathing form
                player.getPersistentData().putDouble("breathes", newBreathes);

                if (Config.logDebug) {
                    Log.debug("Cycled base mod breathing form: " + currentBreathes + " -> " + newBreathes +
                             " (direction: " + (direction == 1 ? "forward" : "backward") + ")");
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
        return switch (style) {
            case 100 -> new int[]{101, 102, 103, 104, 105, 106, 107, 111, 112, 113}; // Water
            case 200 -> new int[]{201, 202, 203, 204, 205, 206, 207, 208, 209, 210}; // Beast
            case 300 -> new int[]{301, 302, 303, 304, 305, 309}; // Thunder
            case 400 -> new int[]{401, 402, 403, 404, 405, 409}; // Flame
            case 500 -> new int[]{501, 502, 503, 504, 505, 506, 507, 508, 509}; // Wind
            case 600 -> new int[]{601, 602, 603, 604, 605, 606, 607, 608, 609, 611}; // Stone
            case 700 -> new int[]{701, 702, 703, 704, 705, 707}; // Mist
            case 800 -> new int[]{801, 802, 803, 804, 805}; // Serpent
            case 900 -> new int[]{901, 902, 903, 904}; // Sound
            case 1000 -> new int[]{1001, 1002, 1003, 1004, 1005, 1006, 1007}; // Ice
            case 1100 -> new int[]{1101, 1102, 1103, 1105, 1106, 1107, 1108, 1109, 1110, 1114, 1116}; // Moon
            case 1200 -> new int[]{1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212}; // Sun
            case 1300 -> new int[]{1301, 1304, 1305}; // Flower
            case 1400 -> new int[]{1401, 1402, 1404, 1405, 1406}; // Insect
            case 1500 -> new int[]{1501, 1502, 1505, 1506}; // Love
            case 1600 -> new int[]{1601, 1602, 1603, 1604, 1605, 1606, 1607}; // Frost
            default -> new int[0];
        };
    }
}