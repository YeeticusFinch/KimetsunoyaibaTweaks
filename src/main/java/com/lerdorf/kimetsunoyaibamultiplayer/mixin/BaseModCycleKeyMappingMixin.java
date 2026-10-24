package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModBloodDemonArtFormRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModSwordFormRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Prevents the base key mapping from sending its native R-cycle packet. */
@Mixin(targets = "net.mcreator.kimetsunoyaiba.init.KimetsunoyaibaModKeyMappings$1")
public abstract class BaseModCycleKeyMappingMixin {
    @Redirect(
        method = "m_7249_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"
        ),
        remap = false
    )
    private void kimetsu$skipNativeCyclePacket(SimpleChannel channel, Object message) {
        if (!kimetsu$shouldSuppressNativeCycle()) {
            channel.sendToServer(message);
        }
    }

    @Redirect(
        method = "m_7249_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/mcreator/kimetsunoyaiba/network/ChangeBreathesAndBloodArtMessage;pressAction(Lnet/minecraft/world/entity/player/Player;II)V"
        ),
        remap = false
    )
    private void kimetsu$skipNativeCycleAction(Player player, int action, int duration) {
        if (!kimetsu$shouldSuppressNativeCycle()) {
            net.mcreator.kimetsunoyaiba.network.ChangeBreathesAndBloodArtMessage.pressAction(
                player, action, duration);
        }
    }

    private static boolean kimetsu$shouldSuppressNativeCycle() {
        if (!Config.enhancedFormCycle || Minecraft.getInstance().player == null) {
            return false;
        }

        var heldItem = Minecraft.getInstance().player.getMainHandItem();
        return BaseModSwordFormRegistry.isSupported(heldItem)
            || BaseModBloodDemonArtFormRegistry.isSupported(heldItem);
    }
}
