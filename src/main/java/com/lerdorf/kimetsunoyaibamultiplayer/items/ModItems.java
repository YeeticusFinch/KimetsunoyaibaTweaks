package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.CreepingRuin;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.VindicatorsBane;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ItemLike;
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

    public static final RegistryObject<Item> NICHIRINSWORD_FLOWER = ITEMS.register("nichirinsword_flower",
        () -> new NichirinSwordFlower(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_KANAWO = ITEMS.register("nichirinsword_kanawo",
        () -> new NichirinSwordKanawo(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_KANAE = ITEMS.register("nichirinsword_kanae",
        () -> new NichirinSwordKanae(new Item.Properties().stacksTo(1).durability(2000)));

    // New nichirin swords
    public static final RegistryObject<Item> NICHIRINSWORD_SOUND = ITEMS.register("nichirinsword_sound",
        () -> new NichirinSwordSound(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_SNAKE = ITEMS.register("nichirinsword_snake",
        () -> new NichirinSwordSnake(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_INSECT = ITEMS.register("nichirinsword_insect",
        () -> new NichirinSwordInsect(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_STONE1 = ITEMS.register("nichirinsword_stone1",
        () -> new NichirinSwordStone1(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_STONE2 = ITEMS.register("nichirinsword_stone2",
        () -> new NichirinSwordStone2(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_BEAST = ITEMS.register("nichirinsword_beast",
        () -> new NichirinSwordBeast(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_INOSUKE = ITEMS.register("nichirinsword_inosuke",
        () -> new NichirinSwordInosuke(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_LOVE = ITEMS.register("nichirinsword_love",
        () -> new NichirinSwordLoveAnimated(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRINSWORD_BLACK = ITEMS.register("nichirinsword_black",
        () -> new NichirinSwordBlack(new Item.Properties().stacksTo(1).durability(2000)));

    public static final RegistryObject<Item> NICHIRIN_ORE = ITEMS.register("nichirin_ore",
        () -> new NichirinOreItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> YEN = ITEMS.register("yen",
        () -> new Item(new Item.Properties().stacksTo(999)));

    // Spawn eggs
    public static final RegistryObject<Item> MUICHIRO_SPAWN_EGG = ITEMS.register("muichiro_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.MUICHIRO,
            0x8ABED4, 0xFFFFFF, // Light blue-gray body (Muichiro's hair color), white spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> MUICHIRO_FP_SPAWN_EGG = ITEMS.register("muichiro_fp_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.MUICHIRO_FP,
            0x6FA2B7, 0xDAF1F8, // Deeper mist-blue body, pale cyan spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KANROJI_SPAWN_EGG = ITEMS.register("kanroji_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KANROJI,
            0xFFB6D9, 0x9FE88D, // Pink body (Mitsuri's hair), light green spots (hair gradient)
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KANAE_SPAWN_EGG = ITEMS.register("kanae_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KANAE,
            0xD6C2F5, 0xFFFFFF, // Lavender body, white spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KANAWO_SPAWN_EGG = ITEMS.register("kanawo_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KANAWO,
            0xB892D6, 0x7BCFA0, // Purple body, mint spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KANATA_SPAWN_EGG = ITEMS.register("kanata_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KANATA,
            0xF2EAD8, 0xC8B9A2, // Light cream body, tan spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KIRIYA_SPAWN_EGG = ITEMS.register("kiriya_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KIRIYA,
            0xDEE4EE, 0xA7B2C2, // Cool pale body, muted blue-gray spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> KAZUMI_SPAWN_EGG = ITEMS.register("kazumi_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KAZUMI,
            0x4A3027, 0xD8C3AF,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> PRINCESS_SPAWN_EGG = ITEMS.register("princess_spawn_egg",
        () -> new PrincessSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.PRINCESS,
            0xE9D9D0, 0xB68563,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_SLAYER_SPAWN_EGG = ITEMS.register("demon_slayer_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_SLAYER,
            0x2F4F5F, 0xD8D8D8, // Slate-blue body, light gray spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_SLAYER_FEMALE_SPAWN_EGG = ITEMS.register("demon_slayer_female_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_SLAYER_FEMALE,
            0xC08AA8, 0xF1E6EE, // Soft pink body, pale pink spots
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_CREEPER_SPAWN_EGG = ITEMS.register("demon_creeper_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_CREEPER,
            0x2C6B35, 0x8DE15C,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_VILLAGER_SPAWN_EGG = ITEMS.register("demon_villager_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_VILLAGER,
            0x6A5441, 0xC43D2F,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_PILLAGER_SPAWN_EGG = ITEMS.register("demon_pillager_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_PILLAGER,
            0x4E5A5F, 0xB83C2A,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DEMON_VINDICATOR_SPAWN_EGG = ITEMS.register("demon_vindicator_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.DEMON_VINDICATOR,
            0x4A565B, 0x9B2B21,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> SWAMP_DEMON_SPAWN_EGG = ITEMS.register("swamp_demon_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(
            com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SWAMP_DEMON,
            0x29443f, 0x6c847b,
            new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> CREEPER_DEMON_ART = ITEMS.register("creeper_demon_art",
        () -> new BloodDemonArtItem(CreepingRuin.ART_ID, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> VINDICATOR_DEMON_ART = ITEMS.register("vindicator_demon_art",
        () -> new BloodDemonArtAxeItem(VindicatorsBane.ART_ID, Tiers.IRON, 9.0F, 0.9F,
            new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SWAMP_DEMON_ART = ITEMS.register("swamp_demon_art",
        () -> new BloodDemonArtItem(com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.SwampDemonArt.ART_ID,
            4.0F, 1.6F, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PUDDLE = ITEMS.register("puddle",
        () -> new Item(new Item.Properties().stacksTo(1)));

    // Armor pieces
    public static final RegistryObject<Item> ANDON_BAKAMA = ITEMS.register("andon_bakama",
        () -> new AndonBakamaItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PURPLE_ANDON_BAKAMA = ITEMS.register("purple_andon_bakama",
        () -> new AndonBakamaItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1),
            "textures/armor/purple_andon_bakama.png"));

    public static final RegistryObject<Item> PURPLE_DEMON_SLAYER_UNIFORM_CHESTPLATE = ITEMS.register("purple_demon_slayer_uniform_chestplate",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1),
            "textures/armor/purple_demonslayer_uniform_chestplate.png",
            "textures/armor/purple_uniform_pants.png",
            "textures/armor/purple_uniform_boots.png"));

    public static final RegistryObject<Item> PURPLE_DEMON_SLAYER_UNIFORM_LEGGINGS = ITEMS.register("purple_demon_slayer_uniform_leggings",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1),
            "textures/armor/purple_demonslayer_uniform_chestplate.png",
            "textures/armor/purple_uniform_pants.png",
            "textures/armor/purple_uniform_boots.png"));

    public static final RegistryObject<Item> PURPLE_DEMON_SLAYER_UNIFORM_BOOTS = ITEMS.register("purple_demon_slayer_uniform_boots",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1),
            "textures/armor/purple_demonslayer_uniform_chestplate.png",
            "textures/armor/purple_uniform_pants.png",
            "textures/armor/purple_uniform_boots.png"));

    public static final RegistryObject<Item> UNIFORM_BOOTS_CHERRY = ITEMS.register("uniform_boots_cherry",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1),
            "geo/boots.geo.json",
            "textures/armor/uniform_boots_cherry.png",
            "textures/armor/uniform_boots_cherry.png",
            "textures/armor/uniform_boots_cherry.png"));

    public static final RegistryObject<Item> UNIFORM_BOOTS_GOLD = ITEMS.register("uniform_boots_gold",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1),
            "geo/boots.geo.json",
            "textures/armor/uniform_boots_gold.png",
            "textures/armor/uniform_boots_gold.png",
            "textures/armor/uniform_boots_gold.png"));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_CHESTPLATE = ITEMS.register("slayer_uniform_2_chestplate",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_LEGGINGS = ITEMS.register("slayer_uniform_2_leggings",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_BOOTS = ITEMS.register("slayer_uniform_2_boots",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_CHESTPLATE_PURPLE = ITEMS.register("slayer_uniform_2_chestplate_purple",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1),
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png"));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_LEGGINGS_PURPLE = ITEMS.register("slayer_uniform_2_leggings_purple",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1),
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png"));

    public static final RegistryObject<Item> SLAYER_UNIFORM_2_BOOTS_PURPLE = ITEMS.register("slayer_uniform_2_boots_purple",
        () -> new SlayerUniform2ArmorItem(CosmeticArmorMaterial.SLAYER_UNIFORM_2,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1),
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png",
            "textures/armor/slayer_uniform_2_purple.png"));

    public static final RegistryObject<Item> KAKUSHI_UNIFORM_HELMET = ITEMS.register("kakushi_uniform_helmet",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.KAKUSHI,
            net.minecraft.world.item.ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1),
            "geo/kakushi_uniform.geo.json",
            "textures/armor/kakushi_uniform_helmet.png",
            "textures/armor/kakushi_uniform_leggings.png",
            "textures/armor/kakushi_uniform_boots.png"));

    public static final RegistryObject<Item> KAKUSHI_UNIFORM_CHESTPLATE = ITEMS.register("kakushi_uniform_chestplate",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.KAKUSHI,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1),
            "geo/kakushi_uniform.geo.json",
            "textures/armor/kakushi_uniform_chestplate.png",
            "textures/armor/kakushi_uniform_leggings.png",
            "textures/armor/kakushi_uniform_boots.png"));

    public static final RegistryObject<Item> KAKUSHI_UNIFORM_LEGGINGS = ITEMS.register("kakushi_uniform_leggings",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.KAKUSHI,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().stacksTo(1),
            "geo/kakushi_uniform.geo.json",
            "textures/armor/kakushi_uniform_chestplate.png",
            "textures/armor/kakushi_uniform_leggings.png",
            "textures/armor/kakushi_uniform_boots.png"));

    public static final RegistryObject<Item> KAKUSHI_UNIFORM_BOOTS = ITEMS.register("kakushi_uniform_boots",
        () -> new SlayerUniformArmorItem(CosmeticArmorMaterial.KAKUSHI,
            net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().stacksTo(1),
            "geo/kakushi_uniform.geo.json",
            "textures/armor/kakushi_uniform_chestplate.png",
            "textures/armor/kakushi_uniform_leggings.png",
            "textures/armor/kakushi_uniform_boots.png"));

    public static final RegistryObject<Item> CLOTHES_MUICHIRO_FP_CHESTPLATE = ITEMS.register("clothes_muichiro_fp_chestplate",
        () -> new MuichiroHaoriItem(CosmeticArmorMaterial.MUICHIRO_HAORI,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> UNIFORM_MUICHIRO_FP_CHESTPLATE = ITEMS.register("uniform_muichiro_fp_chestplate",
        () -> new MuichiroUniformHaoriItem(CosmeticArmorMaterial.MUICHIRO_HAORI_UNIFORM,
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HAIR_MUICHIRO_FP = ITEMS.register("hair_muichiro_fp",
        () -> new HairMuichiroFpItem(CosmeticArmorMaterial.MUICHIRO_FP_HAIR,
            net.minecraft.world.item.ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLINDFOLD = ITEMS.register("blindfold",
        () -> new BlindfoldItem(CosmeticArmorMaterial.COSMETIC,
            net.minecraft.world.item.ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SATOKOS_BOW = ITEMS.register("satokos_bow",
        () -> new SatokosBowItem(CosmeticArmorMaterial.COSMETIC,
            net.minecraft.world.item.ArmorItem.Type.HELMET,
            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HAHNAFUDA_SIMPLE = ITEMS.register("hahnafuda_simple",
        () -> new HanafudaClassicItem(new Item.Properties().stacksTo(1)));

    // Demon Slayer Marks - decorative items that render on entity models
    public static final RegistryObject<Item> DEMONSLAYERMARK_LOVE = ITEMS.register("demonslayermark_love",
        () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> QUEST_SCROLL = ITEMS.register("quest_scroll",
        () -> new QuestScrollItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> NAV_PIN_WAYPOINT = ITEMS.register("nav_pin_waypoint",
        () -> new NavPinItem(new Item.Properties().stacksTo(1)));

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

    // Ubuyashiki's Invitation - locates nearest toril gate
    public static final RegistryObject<Item> UBUYASHIKI_INVITATION = ITEMS.register("ubuyashiki_invitation",
        () -> new UbuyashikiInvitationItem(new Item.Properties().stacksTo(1)));

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
                addSortedSwords(output);

                addItems(output,
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LOG.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_LOG.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_WOOD.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_WISTERIA_WOOD.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PLANKS.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.DARK_BAMBOO_FENCE.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.FUSUMA_BARS.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.DARK_BAMBOO_FUSUMA.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.DARK_OAK_WALL.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.STRIPPED_DARK_OAK_WALL.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.CHEST_OF_DRAWERS.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_PINK.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CYAN.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_LAVENDER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_LEAVES_CREAM.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_PINK.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CYAN.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_LAVENDER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_SAPLING_CREAM.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_PINK.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CYAN.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_LAVENDER.get(),
                    com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks.WISTERIA_PETALS_CREAM.get()
                );

                addItems(output,
                    ANDON_BAKAMA.get(),
                    PURPLE_ANDON_BAKAMA.get(),
                    PURPLE_DEMON_SLAYER_UNIFORM_CHESTPLATE.get(),
                    PURPLE_DEMON_SLAYER_UNIFORM_LEGGINGS.get(),
                    PURPLE_DEMON_SLAYER_UNIFORM_BOOTS.get(),
                    UNIFORM_BOOTS_CHERRY.get(),
                    UNIFORM_BOOTS_GOLD.get(),
                    SLAYER_UNIFORM_2_CHESTPLATE.get(),
                    SLAYER_UNIFORM_2_LEGGINGS.get(),
                    SLAYER_UNIFORM_2_BOOTS.get(),
                    SLAYER_UNIFORM_2_CHESTPLATE_PURPLE.get(),
                    SLAYER_UNIFORM_2_LEGGINGS_PURPLE.get(),
                    SLAYER_UNIFORM_2_BOOTS_PURPLE.get(),
                    KAKUSHI_UNIFORM_HELMET.get(),
                    KAKUSHI_UNIFORM_CHESTPLATE.get(),
                    KAKUSHI_UNIFORM_LEGGINGS.get(),
                    KAKUSHI_UNIFORM_BOOTS.get(),
                    CLOTHES_MUICHIRO_FP_CHESTPLATE.get(),
                    UNIFORM_MUICHIRO_FP_CHESTPLATE.get(),
                    HAIR_MUICHIRO_FP.get(),
                    BLINDFOLD.get(),
                    SATOKOS_BOW.get(),
                    HAHNAFUDA_SIMPLE.get()
                );

                addItems(output,
                    MUICHIRO_SPAWN_EGG.get(),
                    MUICHIRO_FP_SPAWN_EGG.get(),
                    KANROJI_SPAWN_EGG.get(),
                    KANAE_SPAWN_EGG.get(),
                    KANAWO_SPAWN_EGG.get(),
                    KANATA_SPAWN_EGG.get(),
                    KIRIYA_SPAWN_EGG.get(),
                    KAZUMI_SPAWN_EGG.get(),
                    PRINCESS_SPAWN_EGG.get(),
                    DEMON_SLAYER_SPAWN_EGG.get(),
                    DEMON_SLAYER_FEMALE_SPAWN_EGG.get(),
                    DEMON_CREEPER_SPAWN_EGG.get(),
                    DEMON_VILLAGER_SPAWN_EGG.get(),
                    DEMON_PILLAGER_SPAWN_EGG.get(),
                    DEMON_VINDICATOR_SPAWN_EGG.get(),
                    SWAMP_DEMON_SPAWN_EGG.get(),
                    CREEPER_DEMON_ART.get(),
                    VINDICATOR_DEMON_ART.get(),
                    SWAMP_DEMON_ART.get()
                );
                addOptionalSpawnEggs(output);

                addItems(output,
                    OMEN_OF_MUZAN_POTION_1.get(),
                    OMEN_OF_MUZAN_POTION_2.get(),
                    OMEN_OF_MUZAN_POTION_3.get(),
                    OMEN_OF_MUZAN_POTION_4.get(),
                    OMEN_OF_MUZAN_POTION_5.get(),
                    OMEN_OF_UBUYASHIKI_POTION_1.get(),
                    OMEN_OF_UBUYASHIKI_POTION_2.get(),
                    OMEN_OF_UBUYASHIKI_POTION_3.get(),
                    OMEN_OF_UBUYASHIKI_POTION_4.get(),
                    OMEN_OF_UBUYASHIKI_POTION_5.get(),
                    FAVOR_OF_MUZAN_POTION_1.get(),
                    FAVOR_OF_MUZAN_POTION_2.get(),
                    FAVOR_OF_MUZAN_POTION_3.get(),
                    FAVOR_OF_UBUYASHIKI_POTION_1.get(),
                    FAVOR_OF_UBUYASHIKI_POTION_2.get(),
                    FAVOR_OF_UBUYASHIKI_POTION_3.get()
                );

                addItems(output, UBUYASHIKI_INVITATION.get(), NICHIRIN_ORE.get(), YEN.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static void addSortedSwords(CreativeModeTab.Output output) {
        List<Item> swords = new ArrayList<>();

        for (com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.RegisteredSword sword :
                com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.getAllSwords()) {
            if (sword.shouldRegisterToCreativeTab()) {
                swords.add(sword.getSwordItem());
            }
        }

        addIfNotRegistryManaged(swords, NICHIRINSWORD_BEAST.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_INOSUKE.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_STONE1.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_STONE2.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_INSECT.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_SNAKE.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_LOVE.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_SOUND.get());
        addIfNotRegistryManaged(swords, NICHIRINSWORD_BLACK.get());

        swords.sort(Comparator.comparing(ModItems::getCreativeSortName));
        swords.forEach(output::accept);
    }

    private static String getCreativeSortName(Item item) {
        return item.getDescription().getString().toLowerCase(java.util.Locale.ROOT);
    }

    private static void addItems(CreativeModeTab.Output output, ItemLike... items) {
        for (ItemLike item : items) {
            output.accept(item);
        }
    }

    private static void addOptionalSpawnEggs(CreativeModeTab.Output output) {
        try {
            addIfPresent(output, "knyextraadditions", "ice_slayer_spawn_egg");
            addIfPresent(output, "knyextraadditions", "frost_slayer_spawn_egg");
            addIfPresent(output, "knyextraadditions", "komorebi_spawn_egg");
            addIfPresent(output, "knyextraadditions", "shimizu_spawn_egg");
        } catch (Exception e) {
            // Silently ignore if KnY Extra Additions is not loaded
        }
    }

    private static void addIfPresent(CreativeModeTab.Output output, String namespace, String path) {
        Item item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryBuild(namespace, path));
        if (item != null) {
            output.accept(item);
        }
    }

    private static void addIfNotRegistryManaged(CreativeModeTab.Output output, Item item) {
        if (!com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.isRegistered(item)) {
            output.accept(item);
        }
    }

    private static void addIfNotRegistryManaged(List<Item> items, Item item) {
        if (!com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.isRegistered(item)) {
            items.add(item);
        }
    }

    /**
     * Register swords with the SwordRegistry after items are created
     */
    public static void registerSwords() {
        // Register Kanroji sword with animation replacements
        // Sword level 2 = Hashira level (not eligible for color change transformation)
        java.util.Map<String, String> kanrojiAnimReplacements = new java.util.HashMap<>();
        kanrojiAnimReplacements.put("sword_overhead", "kanroji_sword_overhead");

        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_kanroji",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_KANROJI.get(),
            "love_breathing", // Love breathing style
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            net.minecraft.core.particles.ParticleTypes.HEART, // Pink heart particles for love breathing
            null, // No custom sound
            kanrojiAnimReplacements,
            true, // Show in creative tab
            2 // Sword level 2 = Hashira level
        );

        // Sound Breathing sword
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_sound",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_SOUND.get(),
            "sound_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Serpent Breathing sword
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_snake",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_SNAKE.get(),
            "serpent_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Insect Breathing sword
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_insect",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_INSECT.get(),
            "insect_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Stone Breathing sword (variant 1)
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_stone1",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_STONE1.get(),
            "stone_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Stone Breathing sword (variant 2)
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_stone2",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_STONE2.get(),
            "stone_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Beast Breathing sword (enhanced)
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_beast",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_BEAST.get(),
            "beast_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );

        // Beast Breathing named sword (Inosuke, level 1)
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_inosuke",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_INOSUKE.get(),
            "beast_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 1
        );

        // Love Breathing animated sword
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_love",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_LOVE.get(),
            "love_breathing",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            net.minecraft.core.particles.ParticleTypes.HEART,
            null, null, true, 0
        );

        // Black nichirin sword (style assigned per-item stack at use time)
        com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.register(
            "nichirinsword_black",
            (com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) NICHIRINSWORD_BLACK.get(),
            "black",
            com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry.SwordCategory.NICHIRIN,
            null, null, null, true, 0
        );
    }
}
