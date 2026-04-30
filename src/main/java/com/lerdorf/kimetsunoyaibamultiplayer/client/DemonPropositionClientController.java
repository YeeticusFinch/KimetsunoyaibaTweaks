package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class DemonPropositionClientController {
    private static CameraType previousCameraType;
    private static int attackerEntityId = -1;
    private static boolean active;

    private DemonPropositionClientController() {
    }

    public static void activate(int newAttackerEntityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active && minecraft.options != null) {
            previousCameraType = minecraft.options.getCameraType();
        }

        active = true;
        attackerEntityId = newAttackerEntityId;

        if (minecraft.options != null) {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public static void deactivate() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active && previousCameraType != null && minecraft.options != null) {
            minecraft.options.setCameraType(previousCameraType);
        }

        active = false;
        attackerEntityId = -1;
        previousCameraType = null;
    }

    public static boolean shouldOverrideLocalPlayer(LocalPlayer player) {
        return active && player != null && Minecraft.getInstance().player == player;
    }

    public static float getFacingYaw(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || player == null || minecraft.level == null || attackerEntityId == -1) {
            return player != null ? player.getYRot() : 0.0F;
        }

        Entity attacker = minecraft.level.getEntity(attackerEntityId);
        if (attacker == null) {
            return player.getYRot();
        }

        double dx = attacker.getX() - player.getX();
        double dz = attacker.getZ() - player.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
