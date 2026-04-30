package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAlchemyRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<RecipeSerializer<AlchemyTableRecipe>> ALCHEMY_TABLE_SERIALIZER =
        RECIPE_SERIALIZERS.register("alchemy_table", AlchemyTableRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<MicroscopeRecipe>> MICROSCOPE_SERIALIZER =
        RECIPE_SERIALIZERS.register("microscope", MicroscopeRecipe.Serializer::new);

    public static final RecipeType<AlchemyTableRecipe> ALCHEMY_TABLE_TYPE = createType("alchemy_table");
    public static final RecipeType<MicroscopeRecipe> MICROSCOPE_TYPE = createType("microscope");

    private ModAlchemyRecipes() {
    }

    private static <T extends Recipe<?>> RecipeType<T> createType(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, path);
        return new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
