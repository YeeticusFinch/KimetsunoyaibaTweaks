package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BridgerBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.BridgerBlockMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeMovement;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeType;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.EndcapPreviewMode;
import com.lerdorf.kimetsunoyaibamultiplayer.client.BridgerPreviewHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BridgerBlockEntity extends BlockEntity implements MenuProvider {
    private ResourceLocation bridgeType = BridgeTypes.BRIDGE_1_ID;
    private BridgeMovement movement = BridgeMovement.HORIZONTAL;
    private int maxLength = BridgeTypes.BRIDGE_1.defaultMaxLength();
    private int minLength = BridgeTypes.BRIDGE_1.minLength();
    private boolean allowEndcap = true;
    private boolean allowShortEndcap = true;
    private boolean allowConnectToOpposite = true;
    private boolean allowMerge = true;
    private int priority = 0;
    private boolean previewEnabled = false;
    private int previewLength = 16;
    private EndcapPreviewMode endcapPreviewMode = EndcapPreviewMode.NONE;

    public BridgerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BRIDGER_BLOCK.get(), pos, state);
    }

    public Direction getFacing() {
        return getBlockState().hasProperty(BridgerBlock.FACING) ? getBlockState().getValue(BridgerBlock.FACING) : Direction.EAST;
    }

    public ResourceLocation getBridgeType() {
        return bridgeType;
    }

    public BridgeMovement getMovement() {
        return movement;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public boolean isAllowEndcap() {
        return allowEndcap;
    }

    public boolean isAllowShortEndcap() {
        return allowShortEndcap;
    }

    public boolean isAllowConnectToOpposite() {
        return allowConnectToOpposite;
    }

    public boolean isAllowMerge() {
        return allowMerge;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public int getPreviewLength() {
        return previewLength;
    }

    public EndcapPreviewMode getEndcapPreviewMode() {
        return endcapPreviewMode;
    }

    public void setFacing(Direction facing) {
        Direction sanitized = sanitizeFacing(BridgeTypes.getOrDefault(bridgeType), movement, facing);
        if (level != null && !level.isClientSide && getBlockState().getValue(BridgerBlock.FACING) != sanitized) {
            level.setBlock(worldPosition, getBlockState().setValue(BridgerBlock.FACING, sanitized), 3);
        }
    }

    public void applySettings(ResourceLocation bridgeType, BridgeMovement movement, Direction facing,
                              int maxLength, int minLength, boolean allowEndcap, boolean allowShortEndcap,
                              boolean allowConnectToOpposite, boolean allowMerge, int priority,
                              boolean previewEnabled, int previewLength, EndcapPreviewMode endcapPreviewMode) {
        BridgeType type = BridgeTypes.getOrDefault(bridgeType);
        this.bridgeType = type.id();
        this.movement = BridgeTypes.sanitizeMovement(type, movement);
        this.minLength = Mth.clamp(minLength, 1, 512);
        this.maxLength = Mth.clamp(maxLength, this.minLength, 512);
        this.allowEndcap = allowEndcap;
        this.allowShortEndcap = allowShortEndcap;
        this.allowConnectToOpposite = allowConnectToOpposite;
        this.allowMerge = allowMerge;
        this.priority = Mth.clamp(priority, -1024, 1024);
        this.previewEnabled = previewEnabled;
        this.previewLength = Mth.clamp(previewLength, 1, this.maxLength);
        this.endcapPreviewMode = endcapPreviewMode == null ? EndcapPreviewMode.NONE : endcapPreviewMode;
        setFacing(sanitizeFacing(type, this.movement, facing));
        syncClient();
    }

    private static Direction sanitizeFacing(BridgeType type, BridgeMovement movement, Direction facing) {
        BridgeMovement sanitizedMovement = BridgeTypes.sanitizeMovement(type, movement);
        if (sanitizedMovement == BridgeMovement.VERTICAL_UP || sanitizedMovement == BridgeMovement.VERTICAL_STAIR_UP) {
            return Direction.UP;
        }
        if (sanitizedMovement == BridgeMovement.VERTICAL_DOWN || sanitizedMovement == BridgeMovement.VERTICAL_STAIR_DOWN) {
            return Direction.DOWN;
        }
        return facing.getAxis().isHorizontal() ? facing : Direction.EAST;
    }

    public void writeMenuData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeResourceLocation(bridgeType);
        buffer.writeEnum(movement);
        buffer.writeEnum(getFacing());
        buffer.writeVarInt(maxLength);
        buffer.writeVarInt(minLength);
        buffer.writeBoolean(allowEndcap);
        buffer.writeBoolean(allowShortEndcap);
        buffer.writeBoolean(allowConnectToOpposite);
        buffer.writeBoolean(allowMerge);
        buffer.writeVarInt(priority);
        buffer.writeBoolean(previewEnabled);
        buffer.writeVarInt(previewLength);
        buffer.writeEnum(endcapPreviewMode);
    }

    public void syncClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.kimetsunoyaibamultiplayer.bridger_block");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BridgerBlockMenu(containerId, inventory, worldPosition, bridgeType, movement, getFacing(),
            maxLength, minLength, allowEndcap, allowShortEndcap, allowConnectToOpposite, allowMerge,
            priority, previewEnabled, previewLength, endcapPreviewMode);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("BridgeType", bridgeType.toString());
        tag.putString("Movement", movement.name());
        tag.putInt("MaxLength", maxLength);
        tag.putInt("MinLength", minLength);
        tag.putBoolean("AllowEndcap", allowEndcap);
        tag.putBoolean("AllowShortEndcap", allowShortEndcap);
        tag.putBoolean("AllowConnectToOpposite", allowConnectToOpposite);
        tag.putBoolean("AllowMerge", allowMerge);
        tag.putInt("Priority", priority);
        tag.putBoolean("PreviewEnabled", previewEnabled);
        tag.putInt("PreviewLength", previewLength);
        tag.putString("EndcapPreviewMode", endcapPreviewMode.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        BridgeType type = BridgeTypes.getOrDefault(tag.contains("BridgeType") ? ResourceLocation.tryParse(tag.getString("BridgeType")) : BridgeTypes.BRIDGE_1_ID);
        bridgeType = type.id();
        movement = BridgeTypes.sanitizeMovement(type, readMovement(tag.getString("Movement"), type.defaultMovement()));
        maxLength = Mth.clamp(tag.contains("MaxLength") ? tag.getInt("MaxLength") : type.defaultMaxLength(), 1, 512);
        minLength = Mth.clamp(tag.contains("MinLength") ? tag.getInt("MinLength") : type.minLength(), 1, maxLength);
        allowEndcap = !tag.contains("AllowEndcap") || tag.getBoolean("AllowEndcap");
        allowShortEndcap = !tag.contains("AllowShortEndcap") || tag.getBoolean("AllowShortEndcap");
        allowConnectToOpposite = !tag.contains("AllowConnectToOpposite") || tag.getBoolean("AllowConnectToOpposite");
        allowMerge = !tag.contains("AllowMerge") || tag.getBoolean("AllowMerge");
        priority = tag.contains("Priority") ? tag.getInt("Priority") : 0;
        previewEnabled = tag.contains("PreviewEnabled") && tag.getBoolean("PreviewEnabled");
        previewLength = Mth.clamp(tag.contains("PreviewLength") ? tag.getInt("PreviewLength") : 16, 1, maxLength);
        endcapPreviewMode = readEndcapPreviewMode(tag.getString("EndcapPreviewMode"));
        if (level != null && level.isClientSide) {
            BridgerPreviewHooks.refresh(this);
        }
    }

    private static BridgeMovement readMovement(String name, BridgeMovement fallback) {
        try {
            return BridgeMovement.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static EndcapPreviewMode readEndcapPreviewMode(String name) {
        try {
            return EndcapPreviewMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return EndcapPreviewMode.NONE;
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

    @Override
    public void onChunkUnloaded() {
        BridgerPreviewHooks.clear(worldPosition);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        BridgerPreviewHooks.clear(worldPosition);
        super.setRemoved();
    }
}
