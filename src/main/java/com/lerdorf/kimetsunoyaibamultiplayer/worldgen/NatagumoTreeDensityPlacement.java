package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedMountBiomeConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Scales a legacy Natagumo tree count without changing the feature's filters. */
public final class NatagumoTreeDensityPlacement extends PlacementModifier {
    public static final Codec<NatagumoTreeDensityPlacement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.intRange(0, 256).fieldOf("count")
                    .forGetter(NatagumoTreeDensityPlacement::count))
                    .apply(instance, NatagumoTreeDensityPlacement::new));

    private final int count;

    public NatagumoTreeDensityPlacement(int count) {
        this.count = count;
    }

    private int count() {
        return count;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!EnhancedMountBiomeConfig.enhancedMountNatagumoEnabled) {
            return IntStream.range(0, count).mapToObj(ignored -> pos);
        }
        double scaledCount = count * Math.max(0.0, Math.min(1.0,
                EnhancedMountBiomeConfig.natagumoTreeDensity));
        int attempts = (int) scaledCount;
        if (scaledCount > attempts && random.nextFloat() < scaledCount - attempts) {
            attempts++;
        }
        return IntStream.range(0, attempts).mapToObj(ignored -> pos);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.NATAGUMO_TREE_DENSITY.get();
    }
}
