package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Synchronizes player rotation (yaw/pitch/head rotation) from server to client
 */
public class PlayerRotationSyncPacket {
    private final UUID playerUUID;
    private final float yaw;
    private final float pitch;
    private final float headYaw;

    // Cache ShoulderSurfing mod availability
    private static Boolean shoulderSurfingPresent = null;
    private static Method setCameraRotationMethod = null;

    public PlayerRotationSyncPacket(UUID playerUUID, float yaw, float pitch, float headYaw) {
        this.playerUUID = playerUUID;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
    }

    public PlayerRotationSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.headYaw = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(headYaw);
    }

    /**
     * Check if ShoulderSurfing mod is present and initialize reflection if needed
     */
    private static boolean isShoulderSurfingPresent() {
        if (shoulderSurfingPresent == null) {
            shoulderSurfingPresent = ModList.get().isLoaded("shouldersurfing");

            if (shoulderSurfingPresent) {
                try {
                    // Try to find the ShoulderSurfing camera class and rotation method
                    Class<?> cameraClass = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera");
                    // Look for a method to set camera rotation (method name may vary)
                    for (Method method : cameraClass.getDeclaredMethods()) {
                        // Look for methods that might set camera rotation
                        if (method.getName().contains("Rotation") || method.getName().contains("rotation")) {
                            Class<?>[] params = method.getParameterTypes();
                            if (params.length == 2 && params[0] == float.class && params[1] == float.class) {
                                setCameraRotationMethod = method;
                                setCameraRotationMethod.setAccessible(true);
                                if (Config.logDebug) {
                                    Log.debug("Found ShoulderSurfing camera rotation method: {}", method.getName());
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    if (Config.logDebug) {
                        System.err.println("Could not initialize ShoulderSurfing integration: " + e.getMessage());
                    }
                    setCameraRotationMethod = null;
                }
            }
        }
        return shoulderSurfingPresent && setCameraRotationMethod != null;
    }

    /**
     * Try to update ShoulderSurfing camera rotation if the mod is present
     */
    private static void updateShoulderSurfingCamera(float yaw, float pitch) {
        if (!isShoulderSurfingPresent()) {
            return;
        }

        try {
            // Get the ShoulderSurfing camera instance and update rotation
            Class<?> instanceClass = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderInstance");
            Method getInstanceMethod = instanceClass.getDeclaredMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);

            Method getCameraMethod = instanceClass.getDeclaredMethod("getCamera");
            Object camera = getCameraMethod.invoke(instance);

            if (camera != null && setCameraRotationMethod != null) {
                setCameraRotationMethod.invoke(camera, yaw, pitch);

                if (Config.logDebug) {
                    Log.debug("Updated ShoulderSurfing camera rotation: yaw={}, pitch={}", yaw, pitch);
                }
            }
        } catch (Exception e) {
            if (Config.logDebug) {
                System.err.println("Failed to update ShoulderSurfing camera: " + e.getMessage());
            }
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // This packet only goes from server -> client
            if (ctx.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    Player player = mc.level.getPlayerByUUID(playerUUID);
                    if (player != null) {
                    	// Always update the entity rotation
                        player.setYRot(yaw);
                        player.setXRot(pitch);
                        player.setYHeadRot(headYaw);
                        player.yRotO = yaw;
                        player.xRotO = pitch;
                        player.yHeadRotO = headYaw;

                        // If this is the local client player, update the camera too
                        if (player == mc.player) {
                            mc.player.setYRot(yaw);
                            mc.player.setXRot(pitch);
                            mc.player.setYHeadRot(headYaw);

                            mc.player.yRotO = yaw;
                            mc.player.xRotO = pitch;
                            mc.player.yHeadRotO = headYaw;

                            // Update ShoulderSurfing camera if present
                            com.lerdorf.kimetsunoyaibamultiplayer.compat.ShoulderSurfingCompat.setShoulderCameraRotation(yaw, pitch);
                        }

                        if (Config.logDebug) {
                            Log.debug("Client received rotation sync for player {}: yaw={}, pitch={}, headYaw={}",
                                player.getName().getString(), yaw, pitch, headYaw);
                        }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
