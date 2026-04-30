package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.BloodDemonArtBuilderScreen;
import com.lerdorf.kimetsunoyaibamultiplayer.client.BloodDemonArtCoreConfigScreen;
import com.lerdorf.kimetsunoyaibamultiplayer.client.BloodDemonArtFormEditorScreen;
import com.lerdorf.kimetsunoyaibamultiplayer.client.BloodDemonArtFormsScreen;
import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.BloodDemonArtBuilderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenBloodDemonArtBuilderPacket {
    private final BloodDemonArtBuilderData data;
    private final String view;
    private final int editorSlot;

    public OpenBloodDemonArtBuilderPacket(BloodDemonArtBuilderData data) {
        this(data, "main", -1);
    }

    public OpenBloodDemonArtBuilderPacket(BloodDemonArtBuilderData data, String view, int editorSlot) {
        this.data = data;
        this.view = view == null ? "main" : view;
        this.editorSlot = editorSlot;
    }

    public OpenBloodDemonArtBuilderPacket(FriendlyByteBuf buf) {
        this.data = new BloodDemonArtBuilderData(buf);
        this.view = buf.readUtf();
        this.editorSlot = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        data.write(buf);
        buf.writeUtf(view);
        buf.writeVarInt(editorSlot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Screen parent = rootParent(minecraft.screen);
            BloodDemonArtBuilderScreen hub = new BloodDemonArtBuilderScreen(data, parent);
            BloodDemonArtFormsScreen forms = new BloodDemonArtFormsScreen(data, hub);
            Screen screen = switch (view) {
                case "core" -> new BloodDemonArtCoreConfigScreen(data, hub);
                case "forms" -> forms;
                case "form_editor" -> new BloodDemonArtFormEditorScreen(data, forms, editorSlot);
                default -> hub;
            };
            minecraft.setScreen(screen);
        });
        context.setPacketHandled(true);
        return true;
    }

    private static Screen rootParent(Screen screen) {
        if (screen instanceof BloodDemonArtBuilderScreen builderScreen) {
            return builderScreen.parentScreen();
        }
        if (screen instanceof BloodDemonArtCoreConfigScreen coreConfigScreen) {
            return rootParent(coreConfigScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtFormsScreen formsScreen) {
            return rootParent(formsScreen.parentScreen());
        }
        if (screen instanceof BloodDemonArtFormEditorScreen formEditorScreen) {
            return rootParent(formEditorScreen.parentScreen());
        }
        return screen;
    }
}
