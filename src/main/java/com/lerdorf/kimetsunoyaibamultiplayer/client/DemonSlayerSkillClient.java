package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.DemonSlayerSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class DemonSlayerSkillClient {
    private static boolean guardRequested;
    private static int guardRemaining;
    private static int heartbeatTicks;

    private DemonSlayerSkillClient() {
    }

    public static boolean handleKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        if (ModKeyBindings.DEMON_SLAYER_GUARD.matches(event.getKey(), event.getScanCode())) {
            if (event.getAction() == GLFW.GLFW_PRESS && mc.screen == null) {
                startGuard();
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                stopGuard();
            }
            return true;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return false;
        }

        if (ModKeyBindings.DEMON_SLAYER_DASH.matches(event.getKey(), event.getScanCode())) {
            stopGuard();
            if (mc.screen == null) {
                send(DemonSlayerSkillPacket.DASH);
            }
            return true;
        }

        stopGuard();
        return false;
    }

    public static boolean handleMouseInput(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        if (ModKeyBindings.DEMON_SLAYER_GUARD.matchesMouse(event.getButton())) {
            if (event.getAction() == GLFW.GLFW_PRESS && mc.screen == null) {
                startGuard();
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                stopGuard();
            }
            return true;
        }

        if (ModKeyBindings.DEMON_SLAYER_DASH.matchesMouse(event.getButton())) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                stopGuard();
                if (mc.screen == null) {
                    send(DemonSlayerSkillPacket.DASH);
                }
            }
            return true;
        }

        if (event.getAction() == GLFW.GLFW_PRESS) {
            stopGuard();
        }
        return false;
    }

    public static void tick() {
        if (!guardRequested) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.isDeadOrDying()
            || !ModKeyBindings.DEMON_SLAYER_GUARD.isDown()
            || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) {
            stopGuard();
            return;
        }

        if (++heartbeatTicks >= 5) {
            heartbeatTicks = 0;
            send(DemonSlayerSkillPacket.GUARD_HEARTBEAT);
        }
    }

    public static void updateGuardState(int remaining) {
        guardRemaining = Math.max(0, remaining);
    }

    public static boolean isGuardActive() {
        return guardRemaining > 0;
    }

    public static int getGuardRemaining() {
        return guardRemaining;
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        stopGuard();
    }

    private static void startGuard() {
        if (guardRequested) {
            return;
        }
        guardRequested = true;
        heartbeatTicks = 0;
        send(DemonSlayerSkillPacket.GUARD_START);
    }

    private static void stopGuard() {
        if (!guardRequested) {
            return;
        }
        guardRequested = false;
        guardRemaining = 0;
        heartbeatTicks = 0;
        send(DemonSlayerSkillPacket.GUARD_STOP);
    }

    private static void send(int action) {
        ModNetworking.sendToServer(new DemonSlayerSkillPacket(action));
    }
}
