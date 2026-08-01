package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeMovement;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.EndcapPreviewMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class BridgerBlockMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final ResourceLocation bridgeType;
    private final BridgeMovement movement;
    private final Direction facing;
    private final int maxLength;
    private final int minLength;
    private final boolean allowEndcap;
    private final boolean allowShortEndcap;
    private final boolean allowConnectToOpposite;
    private final boolean allowMerge;
    private final int priority;
    private final boolean previewEnabled;
    private final int previewLength;
    private final EndcapPreviewMode endcapPreviewMode;
    private final ContainerLevelAccess access;

    public BridgerBlockMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory,
            buffer.readBlockPos(),
            buffer.readResourceLocation(),
            buffer.readEnum(BridgeMovement.class),
            buffer.readEnum(Direction.class),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readEnum(EndcapPreviewMode.class));
    }

    public BridgerBlockMenu(int containerId, Inventory inventory, BlockPos blockPos, ResourceLocation bridgeType,
                            BridgeMovement movement, Direction facing, int maxLength, int minLength,
                            boolean allowEndcap, boolean allowShortEndcap, boolean allowConnectToOpposite,
                            boolean allowMerge, int priority, boolean previewEnabled, int previewLength,
                            EndcapPreviewMode endcapPreviewMode) {
        super(ModMenus.BRIDGER_BLOCK.get(), containerId);
        this.blockPos = blockPos;
        this.bridgeType = bridgeType;
        this.movement = movement;
        this.facing = facing;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.allowEndcap = allowEndcap;
        this.allowShortEndcap = allowShortEndcap;
        this.allowConnectToOpposite = allowConnectToOpposite;
        this.allowMerge = allowMerge;
        this.priority = priority;
        this.previewEnabled = previewEnabled;
        this.previewLength = previewLength;
        this.endcapPreviewMode = endcapPreviewMode;
        this.access = ContainerLevelAccess.create(inventory.player.level(), blockPos);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ResourceLocation getBridgeType() {
        return bridgeType;
    }

    public BridgeMovement getMovement() {
        return movement;
    }

    public Direction getFacing() {
        return facing;
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
            level.getBlockState(pos).is(ModBlocks.BRIDGER_BLOCK.get())
                && BridgeTypes.get(bridgeType).isPresent()
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
            false);
    }
}
