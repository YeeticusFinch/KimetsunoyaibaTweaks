package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for sword sheath items.
 * These items are NOT added to creative tabs - they exist purely for model/texture registration
 * so that sheaths can be rendered on players' hips/backs.
 */
public class SheathItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    // Generic sheath (default for all swords)
    public static final RegistryObject<Item> SWORD_SHEATH = ITEMS.register("sword_sheath",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Kanroji-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_KANROJI = ITEMS.register("sword_sheath_kanroji",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Display-only Kanroji sword model used when the sword is sheathed on hips/backs/racks.
    public static final RegistryObject<Item> NICHIRINSWORD_KANROJI_SHEATHED = ITEMS.register("nichirinsword_kanroji_sheathed",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Rengoku-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_RENGOKU = ITEMS.register("sword_sheath_rengoku",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Uzui-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_UZUI = ITEMS.register("sword_sheath_uzui",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Iguro-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_IGURO = ITEMS.register("sword_sheath_iguro",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Kanawo-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_KANAWO = ITEMS.register("sword_sheath_kanawo",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Inosuke-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_INOSUKE = ITEMS.register("sword_sheath_inosuke",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Kokushibo-specific sheath
    public static final RegistryObject<Item> SWORD_SHEATH_KOKUSHIBO = ITEMS.register("sword_sheath_kokushibo",
        () -> new Item(new Item.Properties().stacksTo(1)));

    /**
     * Registers sheath items to the mod event bus
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
