package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DebugParticlesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debugparticles")
            .requires(source -> source.hasPermission(2))
            .executes(DebugParticlesCommand::debugParticles));
    }

    private static int debugParticles(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

            player.sendSystemMessage(Component.literal("§6=== Particle Debug Info ==="));

            // Config status
            player.sendSystemMessage(Component.literal("§bConfig:"));
            player.sendSystemMessage(Component.literal("  - Particles Enabled: " + Config.swordParticlesEnabled));
            player.sendSystemMessage(Component.literal("  - Other Entities: " + Config.swordParticlesForOtherEntities));
            player.sendSystemMessage(Component.literal("  - Trigger Mode: " + Config.particleTriggerMode));
            player.sendSystemMessage(Component.literal("  - Debug Logging: " + Config.logDebug));

            // Current item
            player.sendSystemMessage(Component.literal("§bCurrent Item:"));
            if (mainHand.isEmpty()) {
                player.sendSystemMessage(Component.literal("  - No item in main hand"));
            } else {
                String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
                player.sendSystemMessage(Component.literal("  - Item: " + itemId));
                player.sendSystemMessage(Component.literal("  - Is Kimetsunoyaiba Sword: " + SwordParticleMapping.isKimetsunoyaibaSword(mainHand)));

                if (SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
                    var particleType = SwordParticleMapping.getParticleForSword(mainHand);
                    if (particleType != null) {
                        String particleId = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType.getType()).toString();
                        player.sendSystemMessage(Component.literal("  - Particle: " + particleId));
                    } else {
                        player.sendSystemMessage(Component.literal("  - No particle found"));
                    }
                }
            }

            // Position test
            player.sendSystemMessage(Component.literal("§bPosition Tests:"));
            Vec3 playerPos = player.position();
            player.sendSystemMessage(Component.literal("  - Player position: " + String.format("(%.2f, %.2f, %.2f)", playerPos.x, playerPos.y, playerPos.z)));

            // Test different animation positions
            String[] testAnims = {"sword_to_right", "sword_to_left", "sword_rotate", "breath_sun2_1"};
            for (String anim : testAnims) {
                Vec3 swordTip = BonePositionTracker.getSwordTipPosition(player, anim);
                if (swordTip != null) {
                    player.sendSystemMessage(Component.literal("  - " + anim + ": " + String.format("(%.2f, %.2f, %.2f)", swordTip.x, swordTip.y, swordTip.z)));
                } else {
                    player.sendSystemMessage(Component.literal("  - " + anim + ": null"));
                }
            }

            // Instruction for testing
            if (SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
                player.sendSystemMessage(Component.literal("§aPerform a sword animation to test particles!"));
                player.sendSystemMessage(Component.literal("§7Use /testanim to trigger test animation"));
            }
        }

        return 1;
    }
}