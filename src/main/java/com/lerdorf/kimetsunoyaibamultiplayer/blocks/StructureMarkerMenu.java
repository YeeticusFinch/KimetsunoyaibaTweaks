package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.entities.StructureMarkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class StructureMarkerMenu extends AbstractContainerMenu {
    private final UUID markerUuid;
    private final String markerId;

    public StructureMarkerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readUUID(), buffer.readUtf(StructureMarkerEntity.MAX_ID_LENGTH));
    }

    public StructureMarkerMenu(int containerId, Inventory inventory, UUID markerUuid, String markerId) {
        super(ModMenus.STRUCTURE_MARKER.get(), containerId);
        this.markerUuid = markerUuid;
        this.markerId = StructureMarkerEntity.sanitizeId(markerId);
    }

    public UUID getMarkerUuid() {
        return markerUuid;
    }

    public String getMarkerId() {
        return markerId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.isCreative() || !StructureMarkerEntity.isHoldingMarker(player)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        Entity entity = serverLevel.getEntity(markerUuid);
        return entity instanceof StructureMarkerEntity
            && player.distanceToSqr(entity) <= 64.0D;
    }

    public static class Provider implements MenuProvider {
        private final StructureMarkerEntity marker;

        public Provider(StructureMarkerEntity marker) {
            this.marker = marker;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("menu.kimetsunoyaibamultiplayer.structure_marker");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new StructureMarkerMenu(containerId, inventory, marker.getUUID(), marker.getMarkerId());
        }
    }
}
