package com.lerdorf.kimetsunoyaibamultiplayer.gravity.field;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.GravityBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

public final class GravityFieldOutlineManager {
    private static final DustParticleOptions OUTLINE_PARTICLE = new DustParticleOptions(new Vector3f(0.25F, 0.8F, 1.0F), 1.0F);
    private static final DustParticleOptions ARROW_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.2F), 0.55F);
    private static final int PREVIEW_RADIUS = 20;
    private static final int PREVIEW_INTERVAL_TICKS = 5;

    private GravityFieldOutlineManager() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() % PREVIEW_INTERVAL_TICKS != 0L || !GravityBlockEntity.isHoldingGravityBlock(player)) {
            return;
        }

        BlockPos center = player.blockPosition();
        BlockPos min = center.offset(-PREVIEW_RADIUS, -PREVIEW_RADIUS, -PREVIEW_RADIUS);
        BlockPos max = center.offset(PREVIEW_RADIUS, PREVIEW_RADIUS, PREVIEW_RADIUS);
        double maxDistanceSqr = PREVIEW_RADIUS * PREVIEW_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.distSqr(center) > maxDistanceSqr) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof GravityBlockEntity gravityBlock) {
                spawnBox(level, player, gravityBlock.getFieldBox());
                spawnArrow(level, player, gravityBlock.getBlockPos(), gravityBlock.getWorldGravityDirection());
            }
        }
    }

    private static void spawnBox(ServerLevel level, ServerPlayer player, AABB box) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        spawnLine(level, player, new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ));
        spawnLine(level, player, new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ));
        spawnLine(level, player, new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ));
        spawnLine(level, player, new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ));

        spawnLine(level, player, new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ));
        spawnLine(level, player, new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ));
        spawnLine(level, player, new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ));
        spawnLine(level, player, new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ));

        spawnLine(level, player, new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ));
        spawnLine(level, player, new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ));
        spawnLine(level, player, new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ));
        spawnLine(level, player, new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ));
    }

    private static void spawnLine(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.ceil(delta.length() / 0.5D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(player, OUTLINE_PARTICLE, true, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnArrow(ServerLevel level, ServerPlayer player, BlockPos blockPos, Direction direction) {
        Vec3 center = Vec3.atCenterOf(blockPos);
        Vec3 vector = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 start = center.subtract(vector.scale(0.18D));
        Vec3 end = center.add(vector.scale(0.38D));
        spawnParticleLine(level, player, start, end, ARROW_PARTICLE, 0.14D);

        Vec3 sideA = perpendicular(direction).scale(0.12D);
        Vec3 sideB = sideA.scale(-1.0D);
        Vec3 headBase = end.subtract(vector.scale(0.14D));
        spawnParticleLine(level, player, end, headBase.add(sideA), ARROW_PARTICLE, 0.1D);
        spawnParticleLine(level, player, end, headBase.add(sideB), ARROW_PARTICLE, 0.1D);
    }

    private static Vec3 perpendicular(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> new Vec3(0.0D, 1.0D, 0.0D);
            case Y -> new Vec3(1.0D, 0.0D, 0.0D);
            case Z -> new Vec3(0.0D, 1.0D, 0.0D);
        };
    }

    private static void spawnParticleLine(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end, DustParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.ceil(delta.length() / spacing));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(player, particle, true, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
