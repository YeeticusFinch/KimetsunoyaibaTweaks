package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.CustomDemonArtModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.UUID;

public class CustomDemonArtRenderer extends GeoItemRenderer<CustomDemonArtItem> {
    public CustomDemonArtRenderer(CustomDemonArtItem item) {
        super(new CustomDemonArtModel());
    }

    @Override
    public ResourceLocation getTextureLocation(CustomDemonArtItem animatable) {
        ItemStack stack = getCurrentItemStack();
        ResourceLocation playerSkin = getPlayerSkinTexture(stack);
        return playerSkin != null ? playerSkin : CustomDemonArtItem.getDefaultTexture();
    }

    private ResourceLocation getPlayerSkinTexture(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        UUID playerUuid = CustomDemonArtItem.getPlayerUuid(stack);
        String playerName = CustomDemonArtItem.getPlayerName(stack);

        if (minecraft.level != null) {
            if (playerUuid != null) {
                Player player = minecraft.level.getPlayerByUUID(playerUuid);
                if (player instanceof AbstractClientPlayer clientPlayer) {
                    return clientPlayer.getSkinTextureLocation();
                }
            }

            if (!playerName.isBlank()) {
                for (Player player : minecraft.level.players()) {
                    if (playerName.equals(player.getGameProfile().getName()) && player instanceof AbstractClientPlayer clientPlayer) {
                        return clientPlayer.getSkinTextureLocation();
                    }
                }
            }
        }

        if (minecraft.getConnection() != null && playerUuid != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerUuid);
            if (info != null) {
                return info.getSkinLocation();
            }
        }

        return null;
    }
}
