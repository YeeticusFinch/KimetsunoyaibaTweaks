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

// All item classes are in the same package, no imports needed

/**
 * Registry for all mod items
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KimetsunoyaibaMultiplayer.MODID);

    // Nichirin swords with breathing techniques
    public static final RegistryObject<Item> NICHIRINSWORD_MIST = ITEMS.register("nichirinsword_mist",
        () -> new NichirinSwordMist(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_MUICHIRO = ITEMS.register("nichirinsword_muichiro",
        () -> new NichirinSwordMuichiro(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_KANROJI = ITEMS.register("nichirinsword_kanroji",
        () -> new NichirinSwordKanrojiAnimated(new Item.Properties().stacksTo(1).durability(2000)));

    // Spawn eggs
    public static final RegistryObject<Item> MUICHIRO_SPAWN_EGG = ITEMS.register("muichiro_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.MUICHIRO,
            0x8ABED4, 0xFFFFFF, // Light blue-gray body (Muichiro's hair color), white spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KANROJI_SPAWN_EGG = ITEMS.register("kanroji_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KANROJI,
            0xFFB6D9, 0x9FE88D, // Pink body (Mitsuri's hair), light green spots (hair gradient)
            new Item.Properties().stacksTo(64)));

    // Demon Slayer Marks - decorative items that render on entity models
    public static final RegistryObject<Item> DEMONSLAYERMARK_LOVE = ITEMS.register("demonslayermark_love",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Omen Potion Items - Muzan (for demon slayer players)
    public static final RegistryObject<Item> OMEN_OF_MUZAN_POTION_1 = ITEMS.register("omen_of_muzan_potion_1",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.MUZAN, 1));
    public static final RegistryObject<Item> OMEN_OF_MUZAN_POTION_2 = ITEMS.register("omen_of_muzan_potion_2",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.MUZAN, 2));
    public static final RegistryObject<Item> OMEN_OF_MUZAN_POTION_3 = ITEMS.register("omen_of_muzan_potion_3",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.MUZAN, 3));
    public static final RegistryObject<Item> OMEN_OF_MUZAN_POTION_4 = ITEMS.register("omen_of_muzan_potion_4",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.MUZAN, 4));
    public static final RegistryObject<Item> OMEN_OF_MUZAN_POTION_5 = ITEMS.register("omen_of_muzan_potion_5",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.MUZAN, 5));

    // Omen Potion Items - Ubuyashiki (for demon players)
    public static final RegistryObject<Item> OMEN_OF_UBUYASHIKI_POTION_1 = ITEMS.register("omen_of_ubuyashiki_potion_1",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.UBUYASHIKI, 1));
    public static final RegistryObject<Item> OMEN_OF_UBUYASHIKI_POTION_2 = ITEMS.register("omen_of_ubuyashiki_potion_2",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.UBUYASHIKI, 2));
    public static final RegistryObject<Item> OMEN_OF_UBUYASHIKI_POTION_3 = ITEMS.register("omen_of_ubuyashiki_potion_3",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.UBUYASHIKI, 3));
    public static final RegistryObject<Item> OMEN_OF_UBUYASHIKI_POTION_4 = ITEMS.register("omen_of_ubuyashiki_potion_4",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.UBUYASHIKI, 4));
    public static final RegistryObject<Item> OMEN_OF_UBUYASHIKI_POTION_5 = ITEMS.register("omen_of_ubuyashiki_potion_5",
        () -> new OmenPotionItem(new Item.Properties().stacksTo(16), OmenPotionItem.OmenType.UBUYASHIKI, 5));

    // Favor Potion Items - Muzan (demon protection - spawns demons when attacked by non-demons)
    public static final RegistryObject<Item> FAVOR_OF_MUZAN_POTION_1 = ITEMS.register("favor_of_muzan_potion_1",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.MUZAN, 1));
    public static final RegistryObject<Item> FAVOR_OF_MUZAN_POTION_2 = ITEMS.register("favor_of_muzan_potion_2",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.MUZAN, 2));
    public static final RegistryObject<Item> FAVOR_OF_MUZAN_POTION_3 = ITEMS.register("favor_of_muzan_potion_3",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.MUZAN, 3));

    // Favor Potion Items - Ubuyashiki (slayer protection - spawns slayers when attacked by demons/players)
    public static final RegistryObject<Item> FAVOR_OF_UBUYASHIKI_POTION_1 = ITEMS.register("favor_of_ubuyashiki_potion_1",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.UBUYASHIKI, 1));
    public static final RegistryObject<Item> FAVOR_OF_UBUYASHIKI_POTION_2 = ITEMS.register("favor_of_ubuyashiki_potion_2",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.UBUYASHIKI, 2));
    public static final RegistryObject<Item> FAVOR_OF_UBUYASHIKI_POTION_3 = ITEMS.register("favor_of_ubuyashiki_potion_3",
        () -> new FavorPotionItem(new Item.Properties().stacksTo(16), FavorPotionItem.FavorType.UBUYASHIKI, 3));

    // Creative tab
    public static final RegistryObject<CreativeModeTab> KNY_ADDITIONS_TAB = CREATIVE_MODE_TABS.register("kny_additions",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("KnY Additions"))
            .icon(() -> new ItemStack(NICHIRINSWORD_MIST.get()))
            .displayItems((parameters, output) -> {
                // Automatically add all swords registered via the API
                // This allows other mods to have their swords appear in this tab
                for (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.RegisteredSword sword :
                        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getAllSwords()) {
                    // Only add swords that are flagged to be in the creative tab
                    if (sword.shouldRegisterToCreativeTab()) {
                        output.accept(sword.getSwordItem());
                    }
                }

                // Wisteria blocks
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LOG.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_LOG.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_WOOD.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_WOOD.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PLANKS.get());

                // Wisteria Leaves (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CREAM.get());

                // Glowing Wisteria Leaves (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get());

                // Wisteria Saplings (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CREAM.get());

                // Wisteria Petals (all 4 colors)
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_PINK.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CYAN.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_LAVENDER.get());
                output.accept(com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CREAM.get());

                // Spawn eggs
                output.accept(MUICHIRO_SPAWN_EGG.get());
                output.accept(KANROJI_SPAWN_EGG.get());

                // Omen Potions
                output.accept(OMEN_OF_MUZAN_POTION_1.get());
                output.accept(OMEN_OF_MUZAN_POTION_2.get());
                output.accept(OMEN_OF_MUZAN_POTION_3.get());
                output.accept(OMEN_OF_MUZAN_POTION_4.get());
                output.accept(OMEN_OF_MUZAN_POTION_5.get());
                output.accept(OMEN_OF_UBUYASHIKI_POTION_1.get());
                output.accept(OMEN_OF_UBUYASHIKI_POTION_2.get());
                output.accept(OMEN_OF_UBUYASHIKI_POTION_3.get());
                output.accept(OMEN_OF_UBUYASHIKI_POTION_4.get());
                output.accept(OMEN_OF_UBUYASHIKI_POTION_5.get());

                // Favor Potions
                output.accept(FAVOR_OF_MUZAN_POTION_1.get());
                output.accept(FAVOR_OF_MUZAN_POTION_2.get());
                output.accept(FAVOR_OF_MUZAN_POTION_3.get());
                output.accept(FAVOR_OF_UBUYASHIKI_POTION_1.get());
                output.accept(FAVOR_OF_UBUYASHIKI_POTION_2.get());
                output.accept(FAVOR_OF_UBUYASHIKI_POTION_3.get());

                // Add spawn eggs from KnY Extra Additions (if mod is loaded)
                try {
                    // Ice Slayer spawn egg
                    net.minecraft.world.item.Item iceSlayerEgg = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(net.minecraft.resources.ResourceLocation.tryBuild("knyextraadditions", "ice_slayer_spawn_egg"));
                    if (iceSlayerEgg != null) output.accept(iceSlayerEgg);

                    // Frost Slayer spawn egg
                    net.minecraft.world.item.Item frostSlayerEgg = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(net.minecraft.resources.ResourceLocation.tryBuild("knyextraadditions", "frost_slayer_spawn_egg"));
                    if (frostSlayerEgg != null) output.accept(frostSlayerEgg);

                    // Komorebi spawn egg
                    net.minecraft.world.item.Item komorebiEgg = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(net.minecraft.resources.ResourceLocation.tryBuild("knyextraadditions", "komorebi_spawn_egg"));
                    if (komorebiEgg != null) output.accept(komorebiEgg);

                    // Shimizu spawn egg
                    net.minecraft.world.item.Item shimizuEgg = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(net.minecraft.resources.ResourceLocation.tryBuild("knyextraadditions", "shimizu_spawn_egg"));
                    if (shimizuEgg != null) output.accept(shimizuEgg);
                } catch (Exception e) {
                    // Silently ignore if KnY Extra Additions is not loaded
                }
            })
            .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }

    /**
     * Register swords with the SwordRegistry after items are created
     */
    public static void registerSwords() {
        // Register Kanroji sword with animation replacements
        java.util.Map<String, String> kanrojiAnimReplacements = new java.util.HashMap<>();
        kanrojiAnimReplacements.put("sword_overhead", "kanroji_sword_overhead");

        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_kanroji",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_KANROJI.get(),
            "love", // Love breathing style
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            net.minecraft.core.particles.ParticleTypes.HEART, // Pink heart particles for love breathing
            null, // No custom sound
            kanrojiAnimReplacements,
            true // Show in creative tab
        );
    }
}
