package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Test command to spawn a marker entity at crow positions to debug visibility
 */
public class TestCrowRenderCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testcrowrender")
                .requires(source -> source.hasPermission(2))
                .executes(TestCrowRenderCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Get all crow entities
        level.getAllEntities().forEach(entity -> {
            if (entity.getType().toString().contains("kasugai_crow")) {
                Vec3 pos = entity.position();

                // Spawn a glowing chicken at the crow's position for visibility
                Chicken marker = new Chicken(EntityType.CHICKEN, level);
                marker.setPos(pos.x, pos.y, pos.z);
                marker.setGlowingTag(true);
                marker.setInvulnerable(true);
                marker.setNoAi(true);
                level.addFreshEntity(marker);

                // Spawn particles
                for (int i = 0; i < 100; i++) {
                    level.sendParticles(ParticleTypes.FLAME,
                        pos.x, pos.y + 0.5, pos.z,
                        1, 0.3, 0.3, 0.3, 0.01);
                }

                player.sendSystemMessage(Component.literal(
                    "Spawned marker at crow position: " + pos.x + ", " + pos.y + ", " + pos.z));

                LOGGER.info("Spawned marker chicken at crow position: {}, {}, {}", pos.x, pos.y, pos.z);
            }
        });

        player.sendSystemMessage(Component.literal("Crow render test complete - check for glowing chickens!"));
        return 1;
    }
}
