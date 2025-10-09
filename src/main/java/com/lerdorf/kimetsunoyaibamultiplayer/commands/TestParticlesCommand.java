package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.SwordParticleHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class TestParticlesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("testparticles")
            .requires(source -> source.hasPermission(2))
            .executes(TestParticlesCommand::testParticles));
    }

    private static int testParticles(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
                // Send info about the sword and its particle
                String swordType = SwordParticleMapping.getSwordTypeName(mainHand);
                var particleType = SwordParticleMapping.getParticleForSword(mainHand);

                if (particleType != null) {
                    ResourceLocation particleId = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType.getType());

                    player.sendSystemMessage(Component.literal("§aSword: " + swordType));
                    player.sendSystemMessage(Component.literal("§bParticle: " + particleId));
                    player.sendSystemMessage(Component.literal("§ePerform an animation to see particles!"));
                } else {
                    player.sendSystemMessage(Component.literal("§cNo particle found for sword: " + swordType));
                }
            } else {
                player.sendSystemMessage(Component.literal("§cYou must be holding a kimetsunoyaiba nichirin sword!"));
                player.sendSystemMessage(Component.literal("§7Current item: " +
                    BuiltInRegistries.ITEM.getKey(mainHand.getItem())));
            }
        }

        return 1;
    }
}