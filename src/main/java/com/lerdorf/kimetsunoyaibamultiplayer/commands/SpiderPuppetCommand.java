package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * /spiderpuppet - applies 5 minutes of the Puppetry effect to the entity on
 * the commanding player's crosshair, with that player set as puppet owner.
 */
public final class SpiderPuppetCommand {
    private static final double TARGET_RANGE = 60.0D;
    private static final int DURATION_TICKS = 5 * 60 * 20;

    private SpiderPuppetCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("spiderpuppet")
            .requires(source -> source.hasPermission(2))
            .executes(SpiderPuppetCommand::execute);

        dispatcher.register(command);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        LivingEntity target = findTargetedEntity(player);
        if (target == null) {
            source.sendFailure(Component.literal("No living entity found on your crosshair"));
            return 0;
        }
        if (target == player) {
            source.sendFailure(Component.literal("You cannot puppet yourself"));
            return 0;
        }

        if (PuppetryHandler.applyPuppetry(target, player, DURATION_TICKS)) {
            source.sendSuccess(() -> Component.literal("Puppeted "
                + target.getDisplayName().getString() + " for 5 minutes"), true);
            return 1;
        }
        source.sendFailure(Component.literal("Could not apply Puppetry to "
            + target.getDisplayName().getString()));
        return 0;
    }

    /** Ray-cast from the eyes up to {@value TARGET_RANGE} blocks; blocks stop the ray. */
    static LivingEntity findTargetedEntity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 maxEnd = eye.add(look.scale(TARGET_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            eye,
            maxEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level,
            player,
            eye,
            rayEnd,
            new AABB(eye, rayEnd).inflate(1.0D),
            SpiderPuppetCommand::isTargetable
        );

        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean isTargetable(Entity entity) {
        return entity instanceof LivingEntity
            && entity.isAlive()
            && !entity.isRemoved()
            && entity.isPickable();
    }
}
