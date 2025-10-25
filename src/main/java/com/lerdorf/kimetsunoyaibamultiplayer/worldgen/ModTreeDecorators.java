package com.lerdorf.kimetsunoyaibamultiplayer.worldgen;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom tree decorators
 */
public class ModTreeDecorators {
    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
        DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, KimetsunoyaibaMultiplayer.MODID);

    // Legacy decorator
    public static final RegistryObject<TreeDecoratorType<WisteriaPetalsDecorator>> WISTERIA_PETALS =
        TREE_DECORATORS.register("wisteria_petals",
            () -> new TreeDecoratorType<>(WisteriaPetalsDecorator.CODEC));

    // Color variant decorators
    public static final RegistryObject<TreeDecoratorType<WisteriaPetalsPinkDecorator>> WISTERIA_PETALS_PINK =
        TREE_DECORATORS.register("wisteria_petals_pink",
            () -> new TreeDecoratorType<>(WisteriaPetalsPinkDecorator.CODEC));

    public static final RegistryObject<TreeDecoratorType<WisteriaPetalsCyanDecorator>> WISTERIA_PETALS_CYAN =
        TREE_DECORATORS.register("wisteria_petals_cyan",
            () -> new TreeDecoratorType<>(WisteriaPetalsCyanDecorator.CODEC));

    public static final RegistryObject<TreeDecoratorType<WisteriaPetalsLavenderDecorator>> WISTERIA_PETALS_LAVENDER =
        TREE_DECORATORS.register("wisteria_petals_lavender",
            () -> new TreeDecoratorType<>(WisteriaPetalsLavenderDecorator.CODEC));

    public static final RegistryObject<TreeDecoratorType<WisteriaPetalsCreamDecorator>> WISTERIA_PETALS_CREAM =
        TREE_DECORATORS.register("wisteria_petals_cream",
            () -> new TreeDecoratorType<>(WisteriaPetalsCreamDecorator.CODEC));

    public static void register(IEventBus eventBus) {
        TREE_DECORATORS.register(eventBus);
    }
}
