package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.GravityFieldProjectorBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.config.GravityConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityField;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.field.GravityFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GravityFieldProjectorBlockEntity extends BlockEntity {
    private UUID fieldId = UUID.randomUUID();
    private Direction gravityDirection = Direction.DOWN;
    private int range = 8;
    private int width = 5;
    private int height = 5;
    private boolean enabled = true;
    private double priority = 100.0D;

    public GravityFieldProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_FIELD_PROJECTOR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updateField();
        }
    }

    @Override
    public void onChunkUnloaded() {
        unregisterField();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        unregisterField();
        super.setRemoved();
    }

    public void updateField() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.isGravityIntegrationEnabled()
            || !GravityConfig.enableGravityChanging || !enabled) {
            unregisterField();
            syncClient();
            return;
        }
        Direction projectorFacing = getBlockState().hasProperty(GravityFieldProjectorBlock.FACING)
            ? getBlockState().getValue(GravityFieldProjectorBlock.FACING)
            : gravityDirection;
        GravityFieldManager.register(new GravityField(
            fieldId,
            level.dimension(),
            buildFieldBox(worldPosition, projectorFacing, range, width, height),
            projectorFacing,
            gravityDirection,
            range,
            width,
            height,
            enabled,
            priority,
            worldPosition
        ));
        syncClient();
    }

    public void unregisterField() {
        if (level != null && !level.isClientSide) {
            GravityFieldManager.unregister(level.dimension(), fieldId);
        }
    }

    private static AABB buildFieldBox(BlockPos sourcePos, Direction facing, int range, int width, int height) {
        BlockPos start = sourcePos.relative(facing);
        BlockPos end = sourcePos.relative(facing, range);
        double minX = Math.min(start.getX(), end.getX());
        double minY = Math.min(start.getY(), end.getY());
        double minZ = Math.min(start.getZ(), end.getZ());
        double maxX = Math.max(start.getX(), end.getX()) + 1.0D;
        double maxY = Math.max(start.getY(), end.getY()) + 1.0D;
        double maxZ = Math.max(start.getZ(), end.getZ()) + 1.0D;

        double halfWidth = Math.max(0, width - 1) / 2.0D;
        double halfHeight = Math.max(0, height - 1) / 2.0D;
        return switch (facing.getAxis()) {
            case X -> new AABB(minX, sourcePos.getY() - halfHeight, sourcePos.getZ() - halfWidth,
                maxX, sourcePos.getY() + 1.0D + halfHeight, sourcePos.getZ() + 1.0D + halfWidth);
            case Y -> new AABB(sourcePos.getX() - halfWidth, minY, sourcePos.getZ() - halfWidth,
                sourcePos.getX() + 1.0D + halfWidth, maxY, sourcePos.getZ() + 1.0D + halfWidth);
            case Z -> new AABB(sourcePos.getX() - halfWidth, sourcePos.getY() - halfHeight, minZ,
                sourcePos.getX() + 1.0D + halfWidth, sourcePos.getY() + 1.0D + halfHeight, maxZ);
        };
    }

    public void setGravityDirection(Direction gravityDirection) {
        this.gravityDirection = gravityDirection == null ? Direction.DOWN : gravityDirection;
        updateField();
    }

    public void cycleGravityDirection() {
        Direction[] values = Direction.values();
        setGravityDirection(values[(gravityDirection.ordinal() + 1) % values.length]);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateField();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("FieldId", fieldId);
        tag.putString("GravityDirection", gravityDirection.getName());
        tag.putInt("Range", range);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putBoolean("Enabled", enabled);
        tag.putDouble("Priority", priority);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("FieldId")) {
            fieldId = tag.getUUID("FieldId");
        }
        Direction loadedDirection = Direction.byName(tag.getString("GravityDirection"));
        gravityDirection = loadedDirection == null ? Direction.DOWN : loadedDirection;
        range = tag.contains("Range") ? Math.min(tag.getInt("Range"), GravityConfig.maxFieldRange) : 8;
        width = tag.contains("Width") ? Math.max(1, tag.getInt("Width")) : 5;
        height = tag.contains("Height") ? Math.max(1, tag.getInt("Height")) : 5;
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        priority = tag.contains("Priority") ? tag.getDouble("Priority") : 100.0D;
        if (level != null && !level.isClientSide) {
            updateField();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GravityFieldProjectorBlockEntity entity) {
        if (!level.isClientSide && level.getGameTime() % 100L == 0L) {
            entity.updateField();
        }
    }
}
