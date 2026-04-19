package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.client.models.HumanFleshModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.HumanFleshItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.UUID;

public class HumanFleshRenderer extends GeoItemRenderer<HumanFleshItem> {
    public HumanFleshRenderer(HumanFleshItem item) {
        super(new HumanFleshModel());
    }

    @Override
    public ResourceLocation getTextureLocation(HumanFleshItem animatable) {
        ItemStack stack = getCurrentItemStack();
        ResourceLocation playerSkin = getPlayerSkinTexture(stack);
        if (playerSkin != null) {
            return playerSkin;
        }
        return HumanFleshItem.getTexture(stack, animatable.getDefaultTexture());
    }

    private ResourceLocation getPlayerSkinTexture(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        UUID playerUuid = HumanFleshItem.getPlayerUuid(stack);
        String playerName = HumanFleshItem.getPlayerName(stack);

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
