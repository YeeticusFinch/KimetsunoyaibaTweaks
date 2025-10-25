package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nullable;

/**
 * Tree growers for each wisteria color variant
 */
public class WisteriaTreeGrowers {

    // Pink wisteria tree grower
    public static final AbstractTreeGrower PINK = new AbstractTreeGrower() {
        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_pink"));
        }
    };

    // Cyan wisteria tree grower
    public static final AbstractTreeGrower CYAN = new AbstractTreeGrower() {
        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_cyan"));
        }
    };

    // Lavender wisteria tree grower
    public static final AbstractTreeGrower LAVENDER = new AbstractTreeGrower() {
        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_lavender"));
        }
    };

    // Cream wisteria tree grower
    public static final AbstractTreeGrower CREAM = new AbstractTreeGrower() {
        @Nullable
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "wisteria_tree_cream"));
        }
    };
}
