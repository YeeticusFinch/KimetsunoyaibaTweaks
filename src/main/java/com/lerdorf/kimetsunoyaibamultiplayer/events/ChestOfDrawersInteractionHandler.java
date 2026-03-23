package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ChestOfDrawersBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.ChestOfDrawersBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class ChestOfDrawersInteractionHandler {
    private ChestOfDrawersInteractionHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity target = event.getTarget();
        if (!(target instanceof Interaction interaction)) {
            return;
        }

        ChestOfDrawersBlockEntity blockEntity = resolveBlockEntity(interaction, event.getEntity());
        if (blockEntity == null) {
            return;
        }

        int slot = interaction.getPersistentData().getInt(ChestOfDrawersBlockEntity.SLOT_TAG);
        ChestOfDrawersBlockEntity.DrawerUseResult result = blockEntity.handleDrawerItemUse(event.getEntity(), slot);
        if (result == ChestOfDrawersBlockEntity.DrawerUseResult.CONSUMED) {
            event.setCanceled(true);
        } else if (result == ChestOfDrawersBlockEntity.DrawerUseResult.FALLBACK_TO_BLOCK) {
            if (blockEntity.handleBlockUse(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof Interaction interaction)) {
            return;
        }

        ChestOfDrawersBlockEntity blockEntity = resolveBlockEntity(interaction, event.getEntity());
        if (blockEntity == null) {
            return;
        }

        int slot = interaction.getPersistentData().getInt(ChestOfDrawersBlockEntity.SLOT_TAG);
        if (blockEntity.handleDrawerItemAttack(event.getEntity(), slot)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        DrawerClickTarget target = resolveDrawerClickTarget(
            event.getPos(),
            event.getFace(),
            event.getHitVec().getLocation(),
            event.getEntity()
        );
        if (target == null) {
            return;
        }

        ChestOfDrawersBlockEntity.DrawerUseResult result = target.blockEntity.handleDrawerItemUse(event.getEntity(), target.slot);
        if (result == ChestOfDrawersBlockEntity.DrawerUseResult.CONSUMED) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        } else if (result == ChestOfDrawersBlockEntity.DrawerUseResult.FALLBACK_TO_BLOCK
            && target.blockEntity.handleBlockUse(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        DrawerClickTarget target = resolveDrawerClickTarget(
            event.getPos(),
            event.getFace(),
            approximateHitVec(event.getEntity(), event.getPos(), event.getFace()),
            event.getEntity()
        );
        if (target == null) {
            return;
        }

        if (target.blockEntity.handleDrawerItemAttack(event.getEntity(), target.slot)) {
            event.setCanceled(true);
        }
    }

    private static ChestOfDrawersBlockEntity resolveBlockEntity(Interaction interaction, Player player) {
        if (!interaction.getPersistentData().getBoolean(ChestOfDrawersBlockEntity.INTERACTION_TAG)) {
            return null;
        }

        BlockPos pos = new BlockPos(
            interaction.getPersistentData().getInt(ChestOfDrawersBlockEntity.POS_X_TAG),
            interaction.getPersistentData().getInt(ChestOfDrawersBlockEntity.POS_Y_TAG),
            interaction.getPersistentData().getInt(ChestOfDrawersBlockEntity.POS_Z_TAG)
        );

        if (!(player.level().getBlockEntity(pos) instanceof ChestOfDrawersBlockEntity blockEntity)) {
            interaction.discard();
            return null;
        }

        return blockEntity;
    }

    private static DrawerClickTarget resolveDrawerClickTarget(BlockPos clickedPos, Direction face, Vec3 hitVec, Player player) {
        if (face == null || !face.getAxis().isHorizontal()) {
            return null;
        }

        if (player.level().getBlockEntity(clickedPos) instanceof ChestOfDrawersBlockEntity clickedDrawerEntity) {
            if (!(player.level().getBlockState(clickedPos).getBlock() instanceof ChestOfDrawersBlock)) {
                return null;
            }
            Direction facing = player.level().getBlockState(clickedPos).getValue(ChestOfDrawersBlock.FACING);
            if (face != facing) {
                return null;
            }

            int slot = clickedDrawerEntity.getFrontInteractionSlot(hitVec);
            return slot >= 0 ? new DrawerClickTarget(clickedDrawerEntity, slot) : null;
        }

        BlockPos drawerPos = clickedPos.relative(face);
        if (!(player.level().getBlockState(drawerPos).getBlock() instanceof ChestOfDrawersBlock)) {
            return null;
        }
        if (!(player.level().getBlockEntity(drawerPos) instanceof ChestOfDrawersBlockEntity drawerEntity)) {
            return null;
        }

        Direction facing = player.level().getBlockState(drawerPos).getValue(ChestOfDrawersBlock.FACING);
        if (facing != face.getOpposite()) {
            return null;
        }

        int slot = drawerEntity.getFrontInteractionSlot(hitVec);
        return slot >= 0 ? new DrawerClickTarget(drawerEntity, slot) : null;
    }

    private static Vec3 approximateHitVec(Player player, BlockPos clickedPos, Direction face) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double plane;
        double axisVelocity;

        switch (face.getAxis()) {
            case X -> {
                plane = clickedPos.getX() + (face == Direction.EAST ? 1.0D : 0.0D);
                axisVelocity = look.x;
            }
            case Y -> {
                plane = clickedPos.getY() + (face == Direction.UP ? 1.0D : 0.0D);
                axisVelocity = look.y;
            }
            case Z -> {
                plane = clickedPos.getZ() + (face == Direction.SOUTH ? 1.0D : 0.0D);
                axisVelocity = look.z;
            }
            default -> {
                return Vec3.atCenterOf(clickedPos);
            }
        }

        if (Math.abs(axisVelocity) < 1.0E-6D) {
            return Vec3.atCenterOf(clickedPos);
        }

        double t = switch (face.getAxis()) {
            case X -> (plane - eyePos.x) / axisVelocity;
            case Y -> (plane - eyePos.y) / axisVelocity;
            case Z -> (plane - eyePos.z) / axisVelocity;
        };
        if (t < 0.0D || t > 8.0D) {
            return Vec3.atCenterOf(clickedPos);
        }

        return eyePos.add(look.scale(t));
    }

    private record DrawerClickTarget(ChestOfDrawersBlockEntity blockEntity, int slot) {
    }
}
