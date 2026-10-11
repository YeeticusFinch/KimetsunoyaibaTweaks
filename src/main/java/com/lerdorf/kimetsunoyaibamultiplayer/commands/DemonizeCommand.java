package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.compat.MinecraftComesAliveCompat;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class DemonizeCommand {
    private static final double TARGET_RANGE = 60.0D;

    private DemonizeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("demonize")
            .requires(source -> source.hasPermission(2))
            .executes(DemonizeCommand::execute);

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

        Entity target = findTargetedEntity(player);
        if (target instanceof LivingEntity living
            && MinecraftComesAliveCompat.demonize(living)) {
            source.sendSuccess(() -> Component.literal("Demonized " + living.getDisplayName().getString()), true);
            return 1;
        }

        if (target instanceof LivingEntity living) {
            Entity replacement = convertVanillaEntity(living);
            if (replacement != null) {
                source.sendSuccess(() -> Component.literal("Demonized " + living.getDisplayName().getString()), true);
                return 1;
            }
        }

        if (!(target instanceof BreathingSlayerEntity slayer)) {
            source.sendFailure(Component.literal("No demonizable entity found on your crosshair"));
            return 0;
        }
        if (slayer.isDemonized()) {
            source.sendFailure(Component.literal(slayer.getDisplayName().getString() + " is already demonized"));
            return 0;
        }

        slayer.demonize();
        source.sendSuccess(() -> Component.literal("Demonized " + slayer.getDisplayName().getString()), true);
        return 1;
    }

    private static Entity convertVanillaEntity(LivingEntity source) {
        Entity replacement;
        if (source instanceof Villager) {
            replacement = ModEntities.DEMON_VILLAGER.get().create(source.level());
        } else if (source instanceof Pillager) {
            replacement = ModEntities.DEMON_PILLAGER.get().create(source.level());
        } else if (source instanceof Creeper) {
            replacement = ModEntities.DEMON_CREEPER.get().create(source.level());
        } else if (source instanceof Vindicator) {
            replacement = ModEntities.DEMON_VINDICATOR.get().create(source.level());
        } else {
            return null;
        }

        if (replacement == null) {
            return null;
        }

        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        source.level().addFreshEntity(replacement);
        source.discard();
        return replacement;
    }

    private static Entity findTargetedEntity(ServerPlayer player) {
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
            DemonizeCommand::isTargetableDemonizeTarget
        );

        if (entityHit == null) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        return entity;
    }

    private static boolean isTargetableDemonizeTarget(Entity entity) {
        return (entity instanceof BreathingSlayerEntity
            || MinecraftComesAliveCompat.isMcaVillagerTarget(entity)
            || entity instanceof Villager
            || entity instanceof Pillager
            || entity instanceof Creeper
            || entity instanceof Vindicator)
            && entity.isAlive()
            && !entity.isRemoved()
            && entity.isPickable();
    }
}
