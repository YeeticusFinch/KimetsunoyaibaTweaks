package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.FinalSelectionProcedure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles toril gate teleportation logic including chat confirmation,
 * cooldowns, and return position storage.
 */
public class TorilGateTeleportHandler {

    private static final ResourceKey<Level> MT_FUJIKASANE_KEY = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("kimetsunoyaibamultiplayer", "mt_fujikasane")
    );

    // Target coordinates in mt_fujikasane
    private static final double TARGET_X = 307.5;
    private static final double TARGET_Y = 79.0;
    private static final double TARGET_Z = 775.5;
    private static final float TARGET_YAW = 180.0f;
    private static final float TARGET_PITCH = -22.0f;

    // NBT keys for storing return position
    private static final String RETURN_TAG = "TorilGateReturn";
    private static final String RETURN_X = "ReturnX";
    private static final String RETURN_Y = "ReturnY";
    private static final String RETURN_Z = "ReturnZ";
    private static final String RETURN_YAW = "ReturnYaw";
    private static final String RETURN_PITCH = "ReturnPitch";
    private static final String RETURN_DIM = "ReturnDim";

    // Cooldown: player UUID -> last prompt time (millis)
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    // Pending confirmations: player UUID -> expiry time (millis)
    private static final Map<UUID, Long> PENDING = new ConcurrentHashMap<>();
    // Last time we detected the player inside a toril gate marker block
    private static final Map<UUID, Long> LAST_INSIDE_GATE = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 5000;      // 5 seconds between prompts
    private static final long CONFIRMATION_TIMEOUT_MS = 30000; // 30 seconds to confirm
    private static final long LEFT_GATE_RESET_MS = 1500; // treat >1.5s gap as leaving gate area

    /**
     * Called when a player walks into a toril gate marker block.
     */
    public static void onPlayerEnterGate(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();

        if (isDemonPlayer(player)) {
            PENDING.remove(uuid);
            Long lastPrompt = COOLDOWNS.get(uuid);
            if (lastPrompt == null || now - lastPrompt >= COOLDOWN_MS) {
                COOLDOWNS.put(uuid, now);
                sendDemonBlockedMessage(player);
            }
            return;
        }

        // Detect re-entry after leaving gate area and reset pending/cooldown state
        Long lastInside = LAST_INSIDE_GATE.put(uuid, now);
        if (lastInside != null && now - lastInside > LEFT_GATE_RESET_MS) {
            COOLDOWNS.remove(uuid);
            PENDING.remove(uuid);
        }

        // Clear expired pending entry if needed
        Long pendingExpiry = PENDING.get(uuid);
        if (pendingExpiry != null && now >= pendingExpiry) {
            PENDING.remove(uuid);
        }

        // Check cooldown
        Long lastPrompt = COOLDOWNS.get(uuid);
        if (lastPrompt != null && now - lastPrompt < COOLDOWN_MS) {
            return;
        }

        // Don't re-prompt if already pending
        pendingExpiry = PENDING.get(uuid);
        if (pendingExpiry != null && now < pendingExpiry) {
            return;
        }

        // Already in mt_fujikasane? Don't offer teleport
        if (player.level().dimension().equals(MT_FUJIKASANE_KEY)) {
            return;
        }

        // Set cooldown and pending
        COOLDOWNS.put(uuid, now);
        PENDING.put(uuid, now + CONFIRMATION_TIMEOUT_MS);

        // Send clickable chat confirmation
        sendConfirmationMessage(player);
    }

    private static void sendConfirmationMessage(ServerPlayer player) {
        MutableComponent message = Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.enter_prompt")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        MutableComponent yes = Component.translatable("message.kimetsunoyaibamultiplayer.common.yes")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/torilgate confirm"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.hover.confirm"))));

        MutableComponent no = Component.translatable("message.kimetsunoyaibamultiplayer.common.no")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/torilgate cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.hover.cancel"))));

        message.append(yes).append(Component.literal(" ")).append(no);

        player.sendSystemMessage(message);
    }

    /**
     * Called when a player runs /torilgate confirm.
     */
    public static boolean confirmTeleport(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();

        if (isDemonPlayer(player)) {
            PENDING.remove(uuid);
            sendDemonBlockedMessage(player);
            return false;
        }

        Long pendingExpiry = PENDING.get(uuid);

        if (pendingExpiry == null) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.no_pending")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        if (now > pendingExpiry) {
            PENDING.remove(uuid);
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.expired")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        if (!isPlayerInGate(uuid, now)) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.must_be_inside")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        PENDING.remove(uuid);

        // Store return position
        storeReturnPosition(player);

        // Get target dimension
        ServerLevel targetLevel = player.getServer().getLevel(MT_FUJIKASANE_KEY);
        if (targetLevel == null) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.dimension_missing")
                .withStyle(ChatFormatting.RED));
            Log.debug("Mt. Fujikasane dimension not available");
            return false;
        }

        // Teleport
        player.teleportTo(targetLevel, TARGET_X, TARGET_Y, TARGET_Z, TARGET_YAW, TARGET_PITCH);
        player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.welcome")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        Log.debug("Teleported " + player.getName().getString() + " to Mt. Fujikasane via toril gate");

        // Start or join the final selection procedure
        if (!FinalSelectionProcedure.isRunning()) {
            FinalSelectionProcedure.startOrGet(targetLevel);
            Log.debug("Started new Final Selection procedure");
        } else {
            Log.debug("Final Selection procedure already running, player joined");
        }

        return true;
    }

    /**
     * Called when a player runs /torilgate cancel.
     */
    public static void cancelTeleport(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (PENDING.remove(uuid) != null) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.cancelled")
                .withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.no_pending")
                .withStyle(ChatFormatting.RED));
        }
    }

    /**
     * Called when a player runs /torilgate return.
     * Teleports back to their pre-gate position.
     */
    public static boolean returnToPreviousPosition(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(RETURN_TAG)) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.no_return_position")
                .withStyle(ChatFormatting.RED));
            return false;
        }

        CompoundTag returnData = persistentData.getCompound(RETURN_TAG);
        String dimId = returnData.getString(RETURN_DIM);
        double x = returnData.getDouble(RETURN_X);
        double y = returnData.getDouble(RETURN_Y);
        double z = returnData.getDouble(RETURN_Z);
        float yaw = returnData.getFloat(RETURN_YAW);
        float pitch = returnData.getFloat(RETURN_PITCH);

        ResourceLocation dimLocation = ResourceLocation.tryParse(dimId);
        if (dimLocation == null) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.invalid_return_dimension")
                .withStyle(ChatFormatting.RED));
            persistentData.remove(RETURN_TAG);
            return false;
        }

        ResourceKey<Level> returnDimKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
        ServerLevel returnLevel = player.getServer().getLevel(returnDimKey);
        if (returnLevel == null) {
            player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.return_dimension_missing")
                .withStyle(ChatFormatting.RED));
            persistentData.remove(RETURN_TAG);
            return false;
        }

        player.teleportTo(returnLevel, x, y, z, yaw, pitch);
        persistentData.remove(RETURN_TAG);
        player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.returned")
            .withStyle(ChatFormatting.GREEN));
        Log.debug("Returned " + player.getName().getString() + " to " + dimId + " via toril gate return");

        return true;
    }

    private static void storeReturnPosition(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag returnData = new CompoundTag();

        returnData.putDouble(RETURN_X, player.getX());
        returnData.putDouble(RETURN_Y, player.getY());
        returnData.putDouble(RETURN_Z, player.getZ());
        returnData.putFloat(RETURN_YAW, player.getYRot());
        returnData.putFloat(RETURN_PITCH, player.getXRot());
        returnData.putString(RETURN_DIM, player.level().dimension().location().toString());

        persistentData.put(RETURN_TAG, returnData);
    }

    /**
     * Check if a player has a stored return position.
     */
    public static boolean hasReturnPosition(ServerPlayer player) {
        return player.getPersistentData().contains(RETURN_TAG);
    }

    private static boolean isPlayerInGate(UUID uuid, long now) {
        Long lastInside = LAST_INSIDE_GATE.get(uuid);
        return lastInside != null && now - lastInside <= LEFT_GATE_RESET_MS;
    }

    private static boolean isDemonPlayer(ServerPlayer player) {
        return Damager.isDemon(player);
    }

    private static void sendDemonBlockedMessage(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.kimetsunoyaibamultiplayer.toril_gate.demon_blocked")
            .withStyle(ChatFormatting.RED));
    }
}
