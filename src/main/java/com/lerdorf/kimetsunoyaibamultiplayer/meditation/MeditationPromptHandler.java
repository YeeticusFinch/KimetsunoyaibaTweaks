package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CushionSeatEntity;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class MeditationPromptHandler {
    private static final int REQUIRED_SIT_TICKS = 40;
    private static final int PROMPT_COOLDOWN_TICKS = 100;
    private static final int PROMPT_EXPIRY_TICKS = 200;
    private static final String SIT_TICKS_KEY = "MeditationSitTicks";
    private static final String PROMPT_PENDING_UNTIL_KEY = "MeditationPromptPendingUntil";
    private static final String PROMPT_COOLDOWN_UNTIL_KEY = "MeditationPromptCooldownUntil";
    private static final String PROMPT_SHOWN_THIS_SEAT_KEY = "MeditationPromptShownThisSeat";

    private MeditationPromptHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!CustomProgressionConfig.isCustomProgressionEnabled()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime > player.getPersistentData().getLong(PROMPT_PENDING_UNTIL_KEY)) {
            player.getPersistentData().putLong(PROMPT_PENDING_UNTIL_KEY, 0L);
        }

        if (!(player.getVehicle() instanceof CushionSeatEntity)) {
            player.getPersistentData().putInt(SIT_TICKS_KEY, 0);
            player.getPersistentData().putBoolean(PROMPT_SHOWN_THIS_SEAT_KEY, false);
            clearPendingPrompt(player);
            return;
        }

        int sitTicks = player.getPersistentData().getInt(SIT_TICKS_KEY) + 1;
        player.getPersistentData().putInt(SIT_TICKS_KEY, sitTicks);

        if (sitTicks < REQUIRED_SIT_TICKS) {
            return;
        }
        if (player.getPersistentData().getBoolean(PROMPT_SHOWN_THIS_SEAT_KEY)) {
            return;
        }
        if (player.getPersistentData().getLong(PROMPT_COOLDOWN_UNTIL_KEY) > gameTime) {
            return;
        }
        if (hasPendingPrompt(player)) {
            return;
        }

        player.getPersistentData().putLong(PROMPT_PENDING_UNTIL_KEY, gameTime + PROMPT_EXPIRY_TICKS);
        player.getPersistentData().putLong(PROMPT_COOLDOWN_UNTIL_KEY, gameTime + PROMPT_COOLDOWN_TICKS);
        player.getPersistentData().putBoolean(PROMPT_SHOWN_THIS_SEAT_KEY, true);
        sendPrompt(player);
    }

    public static boolean hasPendingPrompt(ServerPlayer player) {
        return player.getPersistentData().getLong(PROMPT_PENDING_UNTIL_KEY) > player.level().getGameTime();
    }

    public static void clearPendingPrompt(ServerPlayer player) {
        player.getPersistentData().putLong(PROMPT_PENDING_UNTIL_KEY, 0L);
    }

    public static void applyResponseCooldown(ServerPlayer player) {
        player.getPersistentData().putLong(PROMPT_COOLDOWN_UNTIL_KEY,
            player.level().getGameTime() + PROMPT_COOLDOWN_TICKS);
    }

    private static void sendPrompt(ServerPlayer player) {
        MutableComponent prefix = Component.literal("§6[Meditation] §eDo you wish to meditate? ");
        Component yes = Component.literal("§a[YES]")
            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/meditation confirm")));
        Component no = Component.literal(" §c[NO]")
            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/meditation decline")));
        player.sendSystemMessage(prefix.append(yes).append(no));
    }
}
