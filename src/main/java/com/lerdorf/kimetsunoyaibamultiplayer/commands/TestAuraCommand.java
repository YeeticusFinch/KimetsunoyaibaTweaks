package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.BiokinesesAuraTexture;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BiokinesesAuraSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Shows one projected aura on the issuing player for renderer testing. */
public final class TestAuraCommand {
    private static final int LIFETIME_TICKS = 100;

    private TestAuraCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testaura")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("aura", StringArgumentType.word())
                .executes(TestAuraCommand::execute))
            .executes(context -> execute(context, BiokinesesAuraTexture.DARK)));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        BiokinesesAuraTexture texture = BiokinesesAuraTexture.fromId(
            StringArgumentType.getString(context, "aura"));
        if (texture == null) {
            context.getSource().sendFailure(Component.literal(
                "Unknown aura. Use dark, red, cracked, or white."));
            return 0;
        }
        return execute(context, texture);
    }

    private static int execute(CommandContext<CommandSourceStack> context,
                               BiokinesesAuraTexture texture) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        float opacity = switch (texture) {
            case DARK -> 0.9F;
            case RED_CIRCLES -> 0.82F;
            case CRACKED -> 0.72F;
            case WHITE -> 0.9F;
        };
        float radius = switch (texture) {
            case DARK -> 3.0F;
            case RED_CIRCLES -> 5.0F;
            case CRACKED -> 6.0F;
            case WHITE -> 4.0F;
        };
        float rotationSpeed = texture == BiokinesesAuraTexture.RED_CIRCLES ? 0.04F : 0.0F;
        Vec3 origin = player.position();
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNearby(new BiokinesesAuraSyncPacket(UUID.randomUUID(), true, true,
            texture, origin, player.getId(), KNYGravity.getGravityDirection(player), opacity,
            radius, 10.0D, 2.0D, 0.0F, rotationSpeed, LIFETIME_TICKS), level,
            origin.x, origin.y, origin.z, 128.0D);
        player.sendSystemMessage(Component.literal("Showing " + texture.id()
            + " aura for " + LIFETIME_TICKS + " ticks."));
        return 1;
    }
}
