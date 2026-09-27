package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.biome.EnhancedMountBiomeSource;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Reports the imaginary Mount Natagumo ring or Boss Minions sub-region at the player. */
public final class NatagumoRegionCommand {
    private NatagumoRegionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("natagumoregion")
                .executes(context -> execute(context.getSource())));
    }

    private static int execute(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            source.sendFailure(Component.literal("Mount Natagumo regions can only be checked from the Overworld."));
            return 0;
        }
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            source.sendFailure(Component.literal("Enhanced Mount Natagumo generation is disabled on this server."));
            return 0;
        }

        int blockX = player.blockPosition().getX();
        int blockZ = player.blockPosition().getZ();
        EnhancedMountBiomeSource.NatagumoRegion region = EnhancedMountBiomeSource.getNatagumoRegion(
                player.serverLevel().getSeed(), blockX, blockZ);
        String regionName = EnhancedMountBiomeSource.natagumoRegionName(region, blockX, blockZ);
        if (regionName == null) {
            source.sendFailure(Component.literal("You are outside the Mount Natagumo rings."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("You are in the " + regionName + "."), false);
        return 1;
    }
}
