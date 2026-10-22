package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtRuntime;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.FearEffectHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtAxeItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector;
import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.VariationEncoder;
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
            if (!Config.enhancedFormCycle || player == null || FearEffectHandler.isParalyzed(player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
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

            selectBaseModForm(player, stack, selection);
        });
        context.setPacketHandled(true);
    }

    private static void selectBaseModForm(ServerPlayer player, ItemStack stack, int formId) {
        if (!BreathingInfoDetector.isNichirinSword(stack)) {
            return;
        }

        double currentBreathes = player.getPersistentData().getDouble("breathes");
        int currentFormId = VariationEncoder.getFormId(currentBreathes);
        int styleRange = (currentFormId / 100) * 100;
        int[] forms = BaseModStyleMapping.getFormsForStyle(styleRange);
        boolean valid = false;
        for (int candidate : forms) {
            if (candidate == formId) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            return;
        }

        player.getPersistentData().putDouble("breathes", formId);
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);
        data.setBaseModBreathesValue(formId);
        data.setCurrentVariationIndex(0);
        data.setBaseModFormName("");
        PlayerBreathingData.saveToNBT(player);

        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new BreathesValueSyncPacket(player.getUUID(), formId), player);
        com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
            new VariationIndexSyncPacket(player.getUUID(), 0), player);

        BaseKnYForms.BaseForm form = BaseKnYForms.forms.get(formId);
        if (form != null) {
            if (!Config.suppressFormCycleChat) {
                player.sendSystemMessage(LocalizationHelper.coloredBreathingSelection(
                    formId,
                    form.name,
                    LocalizationHelper.breathingForm(formId),
                    form.color,
                    form.color));
            }
            BreathingFormAnnouncementHelper.announceBaseModForm(player, formId);
        }
    }
}
