package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<PlacementModifierType<NatagumoTreeDensityPlacement>>
            NATAGUMO_TREE_DENSITY = PLACEMENT_MODIFIERS.register("natagumo_tree_density",
            () -> new PlacementModifierType<>() {
                @Override
                public com.mojang.serialization.Codec<NatagumoTreeDensityPlacement> codec() {
                    return NatagumoTreeDensityPlacement.CODEC;
                }
            });

    private ModPlacementModifiers() {
    }

    public static void register(IEventBus eventBus) {
        PLACEMENT_MODIFIERS.register(eventBus);
    }
}
