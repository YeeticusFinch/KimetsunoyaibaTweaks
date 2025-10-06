package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for all mod items
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KimetsunoyaibaMultiplayer.MODID);

    // Nichirin swords with breathing techniques
    public static final RegistryObject<Item> NICHIRINSWORD_FROST = ITEMS.register("nichirinsword_frost",
        () -> new NichirinSwordFrost(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_ICE = ITEMS.register("nichirinsword_ice",
        () -> new NichirinSwordIce(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_HANAZAWA = ITEMS.register("nichirinsword_hanazawa",
        () -> new NichirinSwordHanazawa(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_HIORI = ITEMS.register("nichirinsword_hiori",
        () -> new NichirinSwordHiori(new Item.Properties().stacksTo(1).durability(2000)));

    // Hiori's cosmetic armor
    public static final RegistryObject<Item> HIORI_HAIR = ITEMS.register("hiori_hair",
        () -> new HioriHairItem());

    public static final RegistryObject<Item> HIORI_HAORI = ITEMS.register("hiori_haori",
        () -> new HioriHaoriItem());

    public static final RegistryObject<Item> HIORI_HAKAMA = ITEMS.register("hiori_hakama",
        () -> new HioriHakamaItem());

    // Hanazawa's cosmetic armor
    public static final RegistryObject<Item> HANAZAWA_HAIR = ITEMS.register("hanazawa_hair",
        () -> new HanazawaHairItem());

    public static final RegistryObject<Item> HANAZAWA_HAORI = ITEMS.register("hanazawa_haori",
        () -> new HanazawaHaoriItem());

    // Spawn eggs for breathing slayer entities
    public static final RegistryObject<Item> ICE_SLAYER_SPAWN_EGG = ITEMS.register("ice_slayer_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.ICE_SLAYER,
            0x5DBCD2, 0xFFFFFF, // Light blue body, white spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FROST_SLAYER_SPAWN_EGG = ITEMS.register("frost_slayer_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.FROST_SLAYER,
            0xB0E0E6, 0xF0F8FF, // Powder blue body, alice blue spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HIORI_SPAWN_EGG = ITEMS.register("hiori_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.HIORI,
            0x8B4513, 0xFFD700, // Saddle brown body, gold spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> HANAZAWA_SPAWN_EGG = ITEMS.register("hanazawa_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.HANAZAWA,
            0xE6E6FA, 0x9370DB, // Lavender body, medium purple spots
            new Item.Properties().stacksTo(64)));

    // Creative tab
    public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB = CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_ICE.get()))
            .displayItems((parameters, output) -> {
                output.accept(NICHIRINSWORD_ICE.get());
                output.accept(NICHIRINSWORD_FROST.get());
                output.accept(NICHIRINSWORD_HANAZAWA.get());
                output.accept(NICHIRINSWORD_HIORI.get());
                output.accept(HIORI_HAIR.get());
                output.accept(HIORI_HAORI.get());
                output.accept(HIORI_HAKAMA.get());
                output.accept(HANAZAWA_HAIR.get());
                output.accept(HANAZAWA_HAORI.get());
                output.accept(ICE_SLAYER_SPAWN_EGG.get());
                output.accept(FROST_SLAYER_SPAWN_EGG.get());
                output.accept(HIORI_SPAWN_EGG.get());
                output.accept(HANAZAWA_SPAWN_EGG.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
