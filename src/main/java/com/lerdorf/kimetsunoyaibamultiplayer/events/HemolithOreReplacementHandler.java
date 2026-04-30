package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public final class HemolithOreReplacementHandler {
    private static final Set<ResourceLocation> BASE_MUZAN_BLOOD_ORE_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "muzanblood_ore"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "ore_muzan_blood")
    );

    private HemolithOreReplacementHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !isReplacementEnabled()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!isBaseMuzanBloodOre(state)) {
            return;
        }

        event.getLevel().setBlock(pos, ModBlocks.HEMOLITH_ORE.get().defaultBlockState(), Block.UPDATE_ALL);
        logReplacement("Swapped base muzan blood ore to hemolith ore at {}", pos);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !isReplacementEnabled()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        if (!isBaseMuzanBloodOre(state)) {
            return;
        }

        event.setCanceled(true);

        if (!event.getPlayer().getAbilities().instabuild) {
            Block.dropResources(
                ModBlocks.HEMOLITH_ORE.get().defaultBlockState(),
                serverLevel,
                pos,
                serverLevel.getBlockEntity(pos),
                event.getPlayer(),
                event.getPlayer().getMainHandItem()
            );
        }

        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        logReplacement("Intercepted base muzan blood ore break at {} and forced hemolith drops", pos);
    }

    private static boolean isReplacementEnabled() {
        return CustomProgressionConfig.replaceMuzanBloodOre != null
            && CustomProgressionConfig.replaceMuzanBloodOre.get();
    }

    private static boolean isBaseMuzanBloodOre(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return blockId != null && BASE_MUZAN_BLOOD_ORE_IDS.contains(blockId);
    }

    private static void logReplacement(String message, BlockPos pos) {
        if (CustomProgressionConfig.enableDebugLogging != null
            && CustomProgressionConfig.enableDebugLogging.get()) {
            Log.debug(message, pos);
        }
    }
}
