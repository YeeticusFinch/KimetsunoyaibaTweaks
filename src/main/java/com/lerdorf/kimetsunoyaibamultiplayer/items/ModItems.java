package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.item.Item;
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

    // Nichirin swords with breathing techniques
    public static final RegistryObject<Item> NICHIRINSWORD_FROST = ITEMS.register("nichirinsword_frost",
        () -> new NichirinSwordFrost(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_ICE = ITEMS.register("nichirinsword_ice",
        () -> new NichirinSwordIce(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_HANAZAWA = ITEMS.register("nichirinsword_hanazawa",
        () -> new NichirinSwordHanazawa(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_HIORI = ITEMS.register("nichirinsword_hiori",
        () -> new NichirinSwordHiori(new Item.Properties().stacksTo(1).durability(2000)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
