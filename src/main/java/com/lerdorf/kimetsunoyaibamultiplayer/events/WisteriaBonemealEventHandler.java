package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WisteriaBonemealEventHandler {
    private static final float SPIDER_LILY_CHANCE = 0.10F;
    private static final ResourceLocation WISTERIA_FOREST =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest");
    private static final ResourceLocation WISTERIA_FOREST_CYAN =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest_cyan");
    private static final ResourceLocation WISTERIA_FOREST_CREAM =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest_cream");

    private WisteriaBonemealEventHandler() {
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !event.getLevel().getBlockState(event.getPos()).is(Blocks.GRASS_BLOCK)
            || !isWisteriaForest(level, event.getPos())
            || level.random.nextFloat() >= SPIDER_LILY_CHANCE) {
            return;
        }

        placeSpiderLily(level, event.getPos(), level.random);
    }

    private static void placeSpiderLily(ServerLevel level, BlockPos grassPos, RandomSource random) {
        BlockState lilyState = ModBlocks.SPIDER_LILY.get().defaultBlockState();
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos candidate = grassPos.above().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (level.isEmptyBlock(candidate) && lilyState.canSurvive(level, candidate)) {
                level.setBlock(candidate, lilyState, 3);
                return;
            }
        }
    }

    private static boolean isWisteriaForest(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);
        if (biomeKey == null) {
            return false;
        }

        ResourceLocation biomeLocation = biomeKey.location();
        return biomeLocation.equals(WISTERIA_FOREST)
            || biomeLocation.equals(WISTERIA_FOREST_CYAN)
            || biomeLocation.equals(WISTERIA_FOREST_CREAM);
    }
}
