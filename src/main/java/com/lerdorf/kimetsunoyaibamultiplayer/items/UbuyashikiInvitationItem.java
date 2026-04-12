package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.events.TorilGateGuaranteeHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SetCrowQuestMarkerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class UbuyashikiInvitationItem extends Item {
    private static final int LOCATE_RADIUS_CHUNKS = 100;
    private static final int MARKER_SCAN_XZ_RADIUS = 24;
    private static final int MARKER_SCAN_Y_RADIUS = 12;

    private static final TagKey<Structure> TORIL_GATE_TAG = TagKey.create(
        Registries.STRUCTURE,
        new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "toril_gate")
    );

    public UbuyashikiInvitationItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
            .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line1")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line2")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line3")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line4")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line5")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.line6")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.kimetsunoyaibamultiplayer.ubuyashiki_invitation.hint")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.serverLevel();
            BlockPos playerPos = serverPlayer.blockPosition();

            BlockPos gatePos = serverLevel.findNearestMapStructure(TORIL_GATE_TAG, playerPos, LOCATE_RADIUS_CHUNKS, false);
            BlockPos naturalMarkerPos = gatePos == null ? null : findGateMarkerNearby(serverLevel, gatePos);
            BlockPos guaranteedMarkerPos = TorilGateGuaranteeHandler.getGuaranteedGateMarker(serverLevel);
            BlockPos chosenMarkerPos = chooseCloserMarker(playerPos, naturalMarkerPos, guaranteedMarkerPos);

            if (chosenMarkerPos != null) {
                // Format: "Toril Gate is at X ~ Z" — matches CrowQuestMarkerHandler regex
                serverPlayer.sendSystemMessage(
                    Component.literal("Toril Gate is at " + chosenMarkerPos.getX() + " ~ " + chosenMarkerPos.getZ())
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                );

                // Send quest marker packet to render particle arrow and destination marker
                int surfaceY = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, chosenMarkerPos.getX(), chosenMarkerPos.getZ());
                Vec3 markerTarget = new Vec3(chosenMarkerPos.getX() + 0.5D, surfaceY + 1.0D, chosenMarkerPos.getZ() + 0.5D);
                int durationTicks = 300; // 15 seconds
                ModNetworking.sendToPlayer(new SetCrowQuestMarkerPacket(markerTarget, durationTicks), serverPlayer);
            } else {
                serverPlayer.sendSystemMessage(
                    Component.literal("No Toril Gate could be found nearby. Explore new chunks and try again.")
                        .withStyle(ChatFormatting.RED)
                );
            }

            player.getCooldowns().addCooldown(this, 100); // 5 second cooldown
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static BlockPos findGateMarkerNearby(ServerLevel level, BlockPos center) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        for (int dx = -MARKER_SCAN_XZ_RADIUS; dx <= MARKER_SCAN_XZ_RADIUS; dx++) {
            for (int dz = -MARKER_SCAN_XZ_RADIUS; dz <= MARKER_SCAN_XZ_RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

                int startY = Math.max(minY, surfaceY - MARKER_SCAN_Y_RADIUS);
                int endY = Math.min(maxY, surfaceY + MARKER_SCAN_Y_RADIUS);
                for (int y = startY; y <= endY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.TORIL_GATE_MARKER.get())) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private static BlockPos chooseCloserMarker(BlockPos playerPos, BlockPos first, BlockPos second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }

        double firstDist = horizontalDistanceSq(playerPos, first);
        double secondDist = horizontalDistanceSq(playerPos, second);
        return firstDist <= secondDist ? first : second;
    }

    private static double horizontalDistanceSq(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return (double) dx * dx + (double) dz * dz;
    }
}
