package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtRuntime;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModBloodDemonArtFormRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModSwordFormRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModEnhancedAbilityState;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectFormPacket {
    private final int selection;

    public SelectFormPacket(int selection) {
        this.selection = selection;
    }

    public SelectFormPacket(FriendlyByteBuf buf) {
        this.selection = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(selection);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (!Config.isEnhancedFormCycleEnabled() || player == null || FearEffectHandler.isParalyzed(player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            boolean abilityActive = Config.isEnhancedFormCycleEnabled()
                && BaseModEnhancedAbilityState.isActive(player);
            if (abilityActive && TrainingSwordHelper.isTrainingSword(stack)) {
                return;
            }
            if (TrainingSwordHelper.isTrainingSword(stack)) {
                // Enhanced selection must not bypass the training-sword restriction,
                // even if a client sends a forged selection packet.
                TrainingSwordHelper.resetToFirstForm(stack, player);
                return;
            }
            if (abilityActive && !BaseModSwordFormRegistry.isSupported(stack)) {
                return;
            }
            if (stack.getItem() instanceof BreathingSwordItem sword) {
                sword.selectForm(player, selection);
                return;
            }
            if (stack.getItem() instanceof BloodDemonArtItem art) {
                art.selectForm(player, stack, selection);
                player.getInventory().setChanged();
                return;
            }
            if (stack.getItem() instanceof BloodDemonArtAxeItem art) {
                art.selectForm(player, stack, selection);
                player.getInventory().setChanged();
                return;
            }
            if (stack.getItem() instanceof CustomDemonArtItem) {
                if (CustomBloodDemonArtRuntime.selectForm(player, stack, selection)) {
                    player.getInventory().setChanged();
                }
                return;
            }

            if (BaseModBloodDemonArtFormRegistry.isSupported(stack)) {
                if (BaseModFormExecutionHelper.selectBaseModBloodDemonArt(player, selection)) {
                    player.getInventory().setChanged();
                }
                return;
            }

            selectBaseModForm(player, stack, selection);
        });
        context.setPacketHandled(true);
    }

    private static void selectBaseModForm(ServerPlayer player, ItemStack stack, int formId) {
        boolean abilityActive = Config.isEnhancedFormCycleEnabled()
            && BaseModEnhancedAbilityState.isActive(player);
        if (!BaseModSwordFormRegistry.isSupported(stack)
            || !BaseModFormExecutionHelper.selectBaseModForm(player, formId)) {
            return;
        }
        if (abilityActive) {
            // The helper queued the request; keep the active form and client HUD
            // unchanged until the base-mod ability finishes.
            return;
        }

        int selectedFormId = BaseModSwordFormRegistry.getEffectiveSelectedFormId(stack, formId);
        player.getInventory().setChanged();

        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
        data.setBaseModBreathesValue(selectedFormId);
        data.setCurrentVariationIndex(0);
        data.setBaseModFormName("");
        PlayerBreathingData.saveToNBT(player);

        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new BreathesValueSyncPacket(player.getUUID(), selectedFormId), player);
        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new VariationIndexSyncPacket(player.getUUID(), 0), player);
        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new BaseModEnhancedSwingStateSyncPacket(player.getUUID(), true), player);

        BaseModSwordFormRegistry.FormDefinition form = BaseModSwordFormRegistry.getForm(stack, selectedFormId);
        if (form != null) {
            if (!Config.suppressFormCycleChat) {
                player.sendSystemMessage(BreathingFormAnnouncementHelper
                    .getBaseModSwordFormDisplay(stack, selectedFormId));
            }
        }
    }
}
