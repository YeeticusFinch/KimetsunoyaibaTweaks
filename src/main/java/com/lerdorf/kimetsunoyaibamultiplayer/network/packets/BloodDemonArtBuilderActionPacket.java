package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtRuntime;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BloodDemonArtBuilderActionPacket {
    private final String action;
    private final int slotIndex;
    private final String value;
    private final String nextView;
    private final int editorSlot;

    public BloodDemonArtBuilderActionPacket(String action, int slotIndex) {
        this(action, slotIndex, "", "main", -1);
    }

    public BloodDemonArtBuilderActionPacket(String action, int slotIndex, String value, String nextView, int editorSlot) {
        this.action = action;
        this.slotIndex = slotIndex;
        this.value = value == null ? "" : value;
        this.nextView = nextView == null ? "main" : nextView;
        this.editorSlot = editorSlot;
    }

    public BloodDemonArtBuilderActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.slotIndex = buf.readVarInt();
        this.value = buf.readUtf();
        this.nextView = buf.readUtf();
        this.editorSlot = buf.readVarInt();
    }

    public static void encode(BloodDemonArtBuilderActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.action);
        buf.writeVarInt(packet.slotIndex);
        buf.writeUtf(packet.value);
        buf.writeUtf(packet.nextView);
        buf.writeVarInt(packet.editorSlot);
    }

    public static void handle(BloodDemonArtBuilderActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel) || !player.getPersistentData().getBoolean("oni")) {
                return;
            }

            CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(serverLevel);
            String responseView = packet.nextView;
            int responseEditorSlot = packet.editorSlot;
            if ("grant_item".equals(packet.action)) {
                int modelVariant = parseInt(packet.value, 1);
                CustomBloodDemonArtRuntime.grantItem(player, modelVariant);
            } else if ("create_slot".equals(packet.action)) {
                savedData.createBlankForm(player, packet.slotIndex);
            } else if ("create_slot_and_edit".equals(packet.action)) {
                if (savedData.createBlankForm(player, packet.slotIndex)) {
                    responseView = "form_editor";
                    responseEditorSlot = packet.slotIndex;
                }
            } else if ("select_slot".equals(packet.action)) {
                savedData.setSelectedSlot(player, packet.slotIndex);
            } else if ("set_form_name".equals(packet.action)) {
                savedData.setFormName(player, packet.slotIndex, packet.value);
            } else if ("set_art_name".equals(packet.action)) {
                savedData.setArtName(player, packet.value);
            } else if ("apply_held_catalyst".equals(packet.action)) {
                savedData.applyHeldCatalyst(player, packet.slotIndex);
            } else if ("apply_held_amplifier".equals(packet.action)) {
                savedData.applyHeldAmplifier(player, packet.slotIndex);
            } else if ("add_move".equals(packet.action)) {
                CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                savedData.addMove(player, packet.slotIndex, move);
            } else if ("move_form_move_up".equals(packet.action)) {
                int moveIndex = parseInt(packet.value, -1);
                if (moveIndex >= 0) {
                    savedData.moveFormMoveUpByIndex(player, packet.slotIndex, moveIndex);
                } else {
                    CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                    savedData.moveFormMoveUp(player, packet.slotIndex, move);
                }
            } else if ("move_form_move_down".equals(packet.action)) {
                int moveIndex = parseInt(packet.value, -1);
                if (moveIndex >= 0) {
                    savedData.moveFormMoveDownByIndex(player, packet.slotIndex, moveIndex);
                } else {
                    CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                    savedData.moveFormMoveDown(player, packet.slotIndex, move);
                }
            } else if ("remove_form_move".equals(packet.action)) {
                int moveIndex = parseInt(packet.value, -1);
                if (moveIndex >= 0) {
                    savedData.removeFormMoveByIndex(player, packet.slotIndex, moveIndex);
                } else {
                    CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                    savedData.removeFormMove(player, packet.slotIndex, move);
                }
            } else if ("set_primary_particle".equals(packet.action)) {
                savedData.setCoreParticle(player, true, parseParticleStyle(packet.value,
                    savedData.getOrCreate(player).coreSettings().primaryParticle()));
            } else if ("set_secondary_particle".equals(packet.action)) {
                savedData.setCoreParticle(player, false, parseParticleStyle(packet.value,
                    savedData.getOrCreate(player).coreSettings().secondaryParticle()));
            } else if ("set_primary_potion_self".equals(packet.action)) {
                savedData.setCorePotionFromInventory(player, true, player.getInventory().selected, true);
            } else if ("set_primary_potion_target".equals(packet.action)) {
                savedData.setCorePotionFromInventory(player, true, player.getInventory().selected, false);
            } else if ("set_secondary_potion_self".equals(packet.action)) {
                savedData.setCorePotionFromInventory(player, false, player.getInventory().selected, true);
            } else if ("set_secondary_potion_target".equals(packet.action)) {
                savedData.setCorePotionFromInventory(player, false, player.getInventory().selected, false);
            } else if ("set_primary_potion_inventory".equals(packet.action)) {
                String[] parts = packet.value.split(";", 2);
                int inventorySlot = parts.length > 0 ? parseInt(parts[0], -1) : -1;
                boolean selfEffect = parts.length < 2 || !"target".equals(parts[1]);
                savedData.setCorePotionFromInventory(player, true, inventorySlot, selfEffect);
            } else if ("set_secondary_potion_inventory".equals(packet.action)) {
                String[] parts = packet.value.split(";", 2);
                int inventorySlot = parts.length > 0 ? parseInt(parts[0], -1) : -1;
                boolean selfEffect = parts.length < 2 || !"target".equals(parts[1]);
                savedData.setCorePotionFromInventory(player, false, inventorySlot, selfEffect);
            } else if ("toggle_primary_potion_target".equals(packet.action)) {
                savedData.togglePotionTargeting(player, true);
            } else if ("toggle_secondary_potion_target".equals(packet.action)) {
                savedData.togglePotionTargeting(player, false);
            } else if ("set_chat_color".equals(packet.action)) {
                savedData.setChatColor(player, parseInt(packet.value, savedData.getOrCreate(player).coreSettings().chatColor()));
            } else if ("bind_primary_effect".equals(packet.action)) {
                CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                savedData.bindPrimaryEffectToMove(player, packet.slotIndex, move);
            } else if ("bind_secondary_effect".equals(packet.action)) {
                CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                savedData.bindSecondaryEffectToMove(player, packet.slotIndex, move);
            } else if ("bind_held_effect".equals(packet.action)) {
                CustomBloodDemonArtSavedData.MoveType move = CustomBloodDemonArtSavedData.MoveType.byName(packet.value);
                savedData.bindCustomEffectToMove(player, packet.slotIndex, move, player.getInventory().selected);
            } else if ("bind_primary_effect_inventory".equals(packet.action)) {
                String[] parts = packet.value.split(";", 2);
                CustomBloodDemonArtSavedData.MoveType move = parts.length > 0 ? CustomBloodDemonArtSavedData.MoveType.byName(parts[0]) : null;
                int binderSlot = parts.length > 1 ? parseInt(parts[1], -1) : -1;
                savedData.bindPrimaryEffectToMove(player, packet.slotIndex, move, binderSlot);
            } else if ("bind_secondary_effect_inventory".equals(packet.action)) {
                String[] parts = packet.value.split(";", 2);
                CustomBloodDemonArtSavedData.MoveType move = parts.length > 0 ? CustomBloodDemonArtSavedData.MoveType.byName(parts[0]) : null;
                int binderSlot = parts.length > 1 ? parseInt(parts[1], -1) : -1;
                savedData.bindSecondaryEffectToMove(player, packet.slotIndex, move, binderSlot);
            } else if ("bind_custom_effect_inventory".equals(packet.action)) {
                String[] parts = packet.value.split(";", 3);
                CustomBloodDemonArtSavedData.MoveType move = parts.length > 0 ? CustomBloodDemonArtSavedData.MoveType.byName(parts[0]) : null;
                int binderSlot = parts.length > 1 ? parseInt(parts[1], -1) : -1;
                int effectSlot = parts.length > 2 ? parseInt(parts[2], -1) : -1;
                savedData.bindCustomEffectToMove(player, packet.slotIndex, move, binderSlot, effectSlot);
            } else if ("unlock_catalyst_inventory".equals(packet.action)) {
                int inventorySlot = parseInt(packet.value, -1);
                savedData.unlockCatalystFromInventory(player, inventorySlot);
            } else if ("add_amplifier_inventory".equals(packet.action)) {
                int inventorySlot = parseInt(packet.value, -1);
                savedData.applyAmplifierFromInventory(player, packet.slotIndex, inventorySlot);
            } else if ("consume_binder_inventory".equals(packet.action)) {
                int inventorySlot = parseInt(packet.value, -1);
                savedData.consumeBinderFromInventory(player, inventorySlot);
            }

            BloodDemonArtBuilderData data = BloodDemonArtBuilderData.fromPlayerData(
                player.experienceLevel,
                DemonTransformationHandler.getTrackedMuzanBlood(player),
                savedData.getUnlockedSlots(player),
                savedData.hasCustomItem(player, new net.minecraft.world.item.ItemStack(ModItems.CUSTOM_DEMON_ART.get())),
                savedData.getOrCreate(player)
            );
            ModNetworking.sendToPlayer(new OpenBloodDemonArtBuilderPacket(data, responseView, responseEditorSlot), player);
        });
        context.setPacketHandled(true);
    }

    private static CustomBloodDemonArtSavedData.ParticleStyle parseParticleStyle(
        String serialized,
        CustomBloodDemonArtSavedData.ParticleStyle fallback
    ) {
        if (serialized == null || serialized.isBlank()) {
            return fallback;
        }
        String[] parts = serialized.split(";", 4);
        if (parts.length < 4) {
            return fallback;
        }
        String particleId = parts[0].isBlank() ? fallback.particleId() : parts[0];
        int color;
        float size;
        try {
            color = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            color = fallback.color();
        }
        try {
            size = Float.parseFloat(parts[2]);
        } catch (NumberFormatException exception) {
            size = fallback.size();
        }
        String blockStateId = parts[3];
        return new CustomBloodDemonArtSavedData.ParticleStyle(particleId, color, size, blockStateId);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
