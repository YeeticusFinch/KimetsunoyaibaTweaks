package com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ChestOfDrawersBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

public class ChestOfDrawersBlockEntity extends BlockEntity implements GeoBlockEntity {
    public enum DrawerUseResult {
        NONE,
        CONSUMED,
        FALLBACK_TO_BLOCK
    }

    public static final String INTERACTION_TAG = KimetsunoyaibaMultiplayer.MODID + ".drawer_interaction";
    public static final String POS_X_TAG = "drawer_pos_x";
    public static final String POS_Y_TAG = "drawer_pos_y";
    public static final String POS_Z_TAG = "drawer_pos_z";
    public static final String SLOT_TAG = "drawer_slot";

    public static final int SLOT_TOP = 0;
    public static final int SLOT_TOP_MIDDLE = 1;
    public static final int SLOT_MIDDLE_BOTTOM = 2;
    public static final int SLOT_BOTTOM_LEFT = 3;
    public static final int SLOT_BOTTOM_RIGHT = 4;

    private static final int DRAWER_COUNT = 4;
    private static final float DRAWER_PULL_DISTANCE = 9.0f;
    private static final int TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemStack[] storedItems = new ItemStack[] {
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private final float[] drawerOpenness = new float[DRAWER_COUNT];

    private int drawerState = 0;
    private int activeDrawer = -1;
    private int queuedState = -1;
    private int transitionTicksRemaining = 0;
    private boolean openingPhase = false;
    private boolean interactionRefreshQueued = true;
    private boolean lastFrontClear = true;

    public ChestOfDrawersBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEST_OF_DRAWERS.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ChestOfDrawersBlockEntity blockEntity) {
        blockEntity.tickCommon();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChestOfDrawersBlockEntity blockEntity) {
        blockEntity.tickCommon();

        boolean frontClear = blockEntity.isFrontClear();
        if (frontClear != blockEntity.lastFrontClear) {
            blockEntity.lastFrontClear = frontClear;
            blockEntity.queueInteractionRefresh();
        }

        if (blockEntity.interactionRefreshQueued) {
            blockEntity.refreshInteractionEntities();
            blockEntity.interactionRefreshQueued = false;
        }
    }

    private void tickCommon() {
        if (activeDrawer < 0 || transitionTicksRemaining <= 0) {
            return;
        }

        float step = 1.0f / TRANSITION_TICKS;
        if (openingPhase) {
            drawerOpenness[activeDrawer] = Math.min(1.0f, drawerOpenness[activeDrawer] + step);
        } else {
            drawerOpenness[activeDrawer] = Math.max(0.0f, drawerOpenness[activeDrawer] - step);
        }

        transitionTicksRemaining--;

        if (transitionTicksRemaining <= 0) {
            if (openingPhase) {
                drawerOpenness[activeDrawer] = 1.0f;
                drawerState = activeDrawer + 1;
                activeDrawer = -1;
                queuedState = -1;
                openingPhase = false;
                queueInteractionRefresh();
                sync();
            } else {
                drawerOpenness[activeDrawer] = 0.0f;
                int nextState = queuedState;
                activeDrawer = -1;
                queuedState = -1;

                if (nextState > 0) {
                    startOpening(nextState - 1);
                } else {
                    drawerState = 0;
                    openingPhase = false;
                    queueInteractionRefresh();
                    sync();
                }
            }
        }
    }

    public boolean handleBlockUse(Player player) {
        if (level == null || level.isClientSide) {
            return true;
        }

        if (activeDrawer >= 0) {
            return true;
        }

        if (drawerState == 0) {
            if (!isFrontClear()) {
                return false;
            }
            startOpening(0);
            return true;
        }

        if (drawerState < 4 && !isFrontClear()) {
            return false;
        }

        startClosing(drawerState == 4 ? 0 : drawerState + 1);
        return true;
    }

    public DrawerUseResult handleDrawerItemUse(Player player, int slot) {
        if (level == null || level.isClientSide || activeDrawer >= 0 || drawerState == 0) {
            return DrawerUseResult.NONE;
        }

        if (!isSlotAvailableForCurrentState(slot)) {
            return DrawerUseResult.NONE;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            return DrawerUseResult.FALLBACK_TO_BLOCK;
        }

        if (!canAcceptItem(slot, heldItem)) {
            return DrawerUseResult.NONE;
        }

        ItemStack previous = storedItems[slot];
        ItemStack placed = heldItem.copy();
        placed.setCount(1);
        storedItems[slot] = placed;
        heldItem.shrink(1);

        if (!previous.isEmpty()) {
            if (!player.addItem(previous.copy())) {
                spawnItem(previous.copy());
            }
        }

        playItemFrameSound();
        sync();
        return DrawerUseResult.CONSUMED;
    }

    public boolean handleDrawerItemAttack(Player player, int slot) {
        if (level == null || level.isClientSide || activeDrawer >= 0 || drawerState == 0) {
            return false;
        }

        if (!isSlotAvailableForCurrentState(slot) || storedItems[slot].isEmpty()) {
            return false;
        }

        ItemStack removed = storedItems[slot];
        storedItems[slot] = ItemStack.EMPTY;
        spawnItem(removed.copy());
        playItemFrameSound();
        sync();
        return true;
    }

    private boolean isSlotAvailableForCurrentState(int slot) {
        return switch (drawerState) {
            case 1 -> slot == SLOT_TOP;
            case 2 -> slot == SLOT_TOP_MIDDLE;
            case 3 -> slot == SLOT_MIDDLE_BOTTOM;
            case 4 -> slot == SLOT_BOTTOM_LEFT || slot == SLOT_BOTTOM_RIGHT;
            default -> false;
        };
    }

    private boolean canAcceptItem(int slot, ItemStack stack) {
        if (slot == SLOT_BOTTOM_LEFT || slot == SLOT_BOTTOM_RIGHT) {
            return stack.getMaxStackSize() > 1;
        }
        return true;
    }

    private void startOpening(int drawerIndex) {
        activeDrawer = drawerIndex;
        queuedState = drawerIndex + 1;
        transitionTicksRemaining = TRANSITION_TICKS;
        openingPhase = true;
        queueInteractionRefresh();
        playDrawerSound(true);
        sync();
    }

    private void startClosing(int nextState) {
        activeDrawer = drawerState - 1;
        queuedState = nextState;
        transitionTicksRemaining = TRANSITION_TICKS;
        openingPhase = false;
        queueInteractionRefresh();
        playDrawerSound(false);
        sync();
    }

    private void playDrawerSound(boolean open) {
        if (level != null) {
            level.playSound(null, worldPosition, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
                SoundSource.BLOCKS, 1.0f, 1.5f);
        }
    }

    private void playItemFrameSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    public float getDrawerOpenAmount(int drawerIndex) {
        return drawerIndex >= 0 && drawerIndex < DRAWER_COUNT ? drawerOpenness[drawerIndex] : 0.0f;
    }

    public float getDrawerOffset(int drawerIndex) {
        return -DRAWER_PULL_DISTANCE * getDrawerOpenAmount(drawerIndex);
    }

    public ItemStack getItemForSlot(int slot) {
        return slot >= 0 && slot < storedItems.length ? storedItems[slot] : ItemStack.EMPTY;
    }

    public boolean shouldRenderSlotItem(int slot) {
        return isSlotVisible(slot);
    }

    public boolean canInteractWithDrawerFace() {
        return level != null && !level.isClientSide && activeDrawer < 0 && drawerState > 0;
    }

    public int getFrontInteractionSlot(Vec3 clickLocation) {
        if (!canInteractWithDrawerFace()) {
            return -1;
        }

        return switch (drawerState) {
            case 1 -> SLOT_TOP;
            case 2 -> SLOT_TOP_MIDDLE;
            case 3 -> SLOT_MIDDLE_BOTTOM;
            case 4 -> resolveBottomSlot(clickLocation);
            default -> -1;
        };
    }

    private boolean isSlotVisible(int slot) {
        int drawerIndex = getDrawerIndexForSlot(slot);
        if (drawerIndex < 0) {
            return false;
        }
        return drawerState == drawerIndex + 1 || activeDrawer == drawerIndex;
    }

    private int getDrawerIndexForSlot(int slot) {
        return switch (slot) {
            case SLOT_TOP -> 0;
            case SLOT_TOP_MIDDLE -> 1;
            case SLOT_MIDDLE_BOTTOM -> 2;
            case SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT -> 3;
            default -> -1;
        };
    }

    private int resolveBottomSlot(Vec3 clickLocation) {
        Direction right = ChestOfDrawersBlock.getRightDirection(getBlockState());
        Vec3 center = Vec3.atCenterOf(worldPosition);
        Vec3 fromCenter = clickLocation.subtract(center);
        double lateralOffset = fromCenter.dot(ChestOfDrawersBlock.worldVector(right));
        return lateralOffset < 0.0D ? SLOT_BOTTOM_LEFT : SLOT_BOTTOM_RIGHT;
    }

    private boolean isFrontClear() {
        if (level == null) {
            return true;
        }

        Direction facing = ChestOfDrawersBlock.getFrontDirection(getBlockState());
        BlockState frontState = level.getBlockState(worldPosition.relative(facing));
        return frontState.isAir() || !frontState.blocksMotion();
    }

    private void queueInteractionRefresh() {
        interactionRefreshQueued = true;
    }

    public void clearInteractionEntities() {
        if (level == null || level.isClientSide) {
            return;
        }

        List<Interaction> interactions = level.getEntitiesOfClass(Interaction.class,
            new AABB(worldPosition).inflate(2.0),
            this::ownsInteractionEntity);
        for (Interaction interaction : interactions) {
            interaction.discard();
        }
    }

    private void refreshInteractionEntities() {
        clearInteractionEntities();

        if (level == null || level.isClientSide || activeDrawer >= 0 || drawerState == 0 || !isFrontClear()) {
            return;
        }

        switch (drawerState) {
            case 1 -> spawnInteraction(SLOT_TOP, 0.0, 0.84, 0.9f, 0.22f);
            case 2 -> spawnInteraction(SLOT_TOP_MIDDLE, 0.0, 0.63, 0.9f, 0.22f);
            case 3 -> spawnInteraction(SLOT_MIDDLE_BOTTOM, 0.0, 0.41, 0.9f, 0.22f);
            case 4 -> {
                spawnInteraction(SLOT_BOTTOM_LEFT, -0.23, 0.16, 0.42f, 0.26f);
                spawnInteraction(SLOT_BOTTOM_RIGHT, 0.23, 0.16, 0.42f, 0.26f);
            }
            default -> {
            }
        }
    }

    private void spawnInteraction(int slot, double sideOffset, double yOffset, float width, float height) {
        if (level == null) {
            return;
        }

        Interaction interaction = EntityType.INTERACTION.create(level);
        if (interaction == null) {
            return;
        }

        Vec3 localOffset = ChestOfDrawersBlock.localToWorld(getBlockState(), sideOffset, yOffset - 0.5D, 1.0D);
        Vec3 position = Vec3.atCenterOf(worldPosition).add(localOffset);
        double x = position.x;
        double y = position.y;
        double z = position.z;

        interaction.moveTo(x, y, z);
        CompoundTag interactionTag = interaction.saveWithoutId(new CompoundTag());
        interactionTag.putFloat("width", width);
        interactionTag.putFloat("height", height);
        interactionTag.putBoolean("response", true);
        interaction.load(interactionTag);
        interaction.moveTo(x, y, z);
        interaction.getPersistentData().putBoolean(INTERACTION_TAG, true);
        interaction.getPersistentData().putInt(POS_X_TAG, worldPosition.getX());
        interaction.getPersistentData().putInt(POS_Y_TAG, worldPosition.getY());
        interaction.getPersistentData().putInt(POS_Z_TAG, worldPosition.getZ());
        interaction.getPersistentData().putInt(SLOT_TAG, slot);
        level.addFreshEntity(interaction);
    }

    private boolean ownsInteractionEntity(Interaction interaction) {
        CompoundTag data = interaction.getPersistentData();
        return data.getBoolean(INTERACTION_TAG)
            && data.getInt(POS_X_TAG) == worldPosition.getX()
            && data.getInt(POS_Y_TAG) == worldPosition.getY()
            && data.getInt(POS_Z_TAG) == worldPosition.getZ();
    }

    private void spawnItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return;
        }

        Vec3 spawnPos = Vec3.atCenterOf(worldPosition)
            .add(ChestOfDrawersBlock.localToWorld(getBlockState(), 0.0D, 0.3D, 0.0D));
        ItemEntity entity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, stack);
        level.addFreshEntity(entity);
    }

    public void dropStoredItems() {
        for (int i = 0; i < storedItems.length; i++) {
            if (!storedItems[i].isEmpty()) {
                spawnItem(storedItems[i].copy());
                storedItems[i] = ItemStack.EMPTY;
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        drawerState = tag.getInt("DrawerState");
        activeDrawer = tag.getInt("ActiveDrawer");
        queuedState = tag.getInt("QueuedState");
        transitionTicksRemaining = tag.getInt("TransitionTicks");
        openingPhase = tag.getBoolean("OpeningPhase");

        for (int i = 0; i < DRAWER_COUNT; i++) {
            drawerOpenness[i] = Mth.clamp(tag.getFloat("DrawerOpenness" + i), 0.0f, 1.0f);
        }

        for (int i = 0; i < storedItems.length; i++) {
            storedItems[i] = ItemStack.of(tag.getCompound("StoredItem" + i));
        }

        interactionRefreshQueued = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DrawerState", drawerState);
        tag.putInt("ActiveDrawer", activeDrawer);
        tag.putInt("QueuedState", queuedState);
        tag.putInt("TransitionTicks", transitionTicksRemaining);
        tag.putBoolean("OpeningPhase", openingPhase);

        for (int i = 0; i < DRAWER_COUNT; i++) {
            tag.putFloat("DrawerOpenness" + i, drawerOpenness[i]);
        }

        for (int i = 0; i < storedItems.length; i++) {
            CompoundTag itemTag = new CompoundTag();
            storedItems[i].save(itemTag);
            tag.put("StoredItem" + i, itemTag);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "drawer_controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
