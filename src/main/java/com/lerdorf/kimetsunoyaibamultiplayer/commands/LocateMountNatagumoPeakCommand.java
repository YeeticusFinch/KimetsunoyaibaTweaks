package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.NatagumoPeakSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Locates the nearest discovered Mount Natagumo peak for operators. */
public final class LocateMountNatagumoPeakCommand {
    private LocateMountNatagumoPeakCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locatemountnatagumopeak")
                .requires(source -> source.hasPermission(2))
                .executes(context -> locate(context.getSource())));
    }

    private static int locate(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            source.sendFailure(Component.literal("Mount Natagumo peaks can only be located from the Overworld."));
            return 0;
        }

        ServerLevel overworld = player.getServer().overworld();
        NatagumoPeakSavedData.Peak nearest = NatagumoPeakSavedData.get(overworld).peaks().stream()
                .min((first, second) -> Double.compare(
                        distanceSquared(player.getX(), player.getZ(), first),
                        distanceSquared(player.getX(), player.getZ(), second)))
                .orElse(null);
        if (nearest == null) {
            source.sendFailure(Component.literal("No generated Mount Natagumo peaks have been discovered yet."));
            return 0;
        }

        String teleportCommand = "/execute in minecraft:overworld run tp @s "
                + nearest.x() + " " + nearest.y() + " " + nearest.z();
        MutableComponent coordinates = Component.literal(
                        "[" + nearest.x() + ", " + nearest.y() + ", " + nearest.z() + "]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to teleport to Mount Natagumo"))));
        source.sendSuccess(() -> Component.literal("Nearest Mount Natagumo peak: ").append(coordinates), false);
        return 1;
    }

    private static double distanceSquared(double playerX, double playerZ, NatagumoPeakSavedData.Peak peak) {
        double dx = playerX - peak.x();
        double dz = playerZ - peak.z();
        return dx * dx + dz * dz;
    }
}
