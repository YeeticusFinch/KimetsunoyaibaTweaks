package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.StructureMarkerMenu;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** A persistent, visual-only marker used to identify structure locations. */
public class StructureMarkerEntity extends Entity {
    public static final int MAX_ID_LENGTH = 64;
    private static final EntityDataAccessor<String> MARKER_ID =
        SynchedEntityData.defineId(StructureMarkerEntity.class, EntityDataSerializers.STRING);

    public StructureMarkerEntity(EntityType<? extends StructureMarkerEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        blocksBuilding = false;
        noCulling = true;
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MARKER_ID, "");
    }

    public String getMarkerId() {
        return entityData.get(MARKER_ID);
    }

    public void setMarkerId(String id) {
        entityData.set(MARKER_ID, sanitizeId(id));
    }

    public static String sanitizeId(String id) {
        if (id == null) {
            return "";
        }
        String value = id.length() > MAX_ID_LENGTH ? id.substring(0, MAX_ID_LENGTH) : id;
        return value.codePoints().anyMatch(Character::isWhitespace) ? "" : value;
    }

    public static boolean isHoldingMarker(Player player) {
        return player.getMainHandItem().is(ModItems.STRUCTURE_MARKER.get())
            || player.getOffhandItem().is(ModItems.STRUCTURE_MARKER.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isCreative() || !held.is(ModItems.STRUCTURE_MARKER.get())) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                new StructureMarkerMenu.Provider(this),
                buffer -> {
                    buffer.writeUUID(getUUID());
                    buffer.writeUtf(getMarkerId(), MAX_ID_LENGTH);
                });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // Keep the physical bounding box effectively absent; getPickRadius only
        // provides enough selection range for the marker to be right-clicked.
        return EntityDimensions.fixed(0.0F, 0.0F);
    }

    @Override
    public float getPickRadius() {
        return 0.5F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("MarkerId", getMarkerId());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setMarkerId(tag.getString("MarkerId"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public Component getName() {
        return Component.translatable("entity.kimetsunoyaibamultiplayer.structure_marker");
    }
}
