package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.customdemonart.CustomBloodDemonArtSavedData;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class BloodDemonArtAlchemyCatalog {
    public static final String FIRE_INFUSION_EFFECT_ID = "kimetsunoyaibamultiplayer:fire_infusion";
    public static final String FROZEN_INFUSION_EFFECT_ID = "kimetsunoyaibamultiplayer:frozen_infusion";

    public static final String MICROSCOPE_BLOCK_ID = "kimetsunoyaiba:microscope";
    private static final int CATALYST_RING_TINT = 0xD4AF37;
    private static final int INFUSION_RING_TINT = 0x5A9BFF;
    private static final int AMPLIFIER_RING_TINT = 0xD64A3A;
    private static final int BINDER_RING_TINT = 0xC0C7D1;
    private static final int OROCHI_ELIXIR_RING_TINT = 0x4A1D6F;
    private static final int LEGENDARY_MEDICINE_RING_TINT = 0xB10F1A;
    private static final String POTENCY_KEY = "Potency";

    private static final Map<String, Integer> ITEM_TINTS = Map.ofEntries(
        Map.entry("kimetsunoyaibamultiplayer:minced_human_flesh", 0xA64242),
        Map.entry("kimetsunoyaibamultiplayer:bone_dust", 0xEAE2CF),
        Map.entry("kimetsunoyaibamultiplayer:calcite_powder", 0xE7E2D4),
        Map.entry("kimetsunoyaibamultiplayer:fermented_orchid", 0x466E9A),
        Map.entry("kimetsunoyaibamultiplayer:immortal_daisy", 0xE8E39C),
        Map.entry("kimetsunoyaibamultiplayer:amethyst_lens", 0xB99AF0),
        Map.entry("kimetsunoyaibamultiplayer:empty_vial", 0xFFFFFF),
        Map.entry("kimetsunoyaibamultiplayer:empty_petri_dish", 0xFFFFFF),
        Map.entry("kimetsunoyaibamultiplayer:blood_sample", 0x8F1414),
        Map.entry("kimetsunoyaibamultiplayer:rotten_blood_sample", 0x556B2F),
        Map.entry("kimetsunoyaibamultiplayer:crude_vial", 0xB42E2E),
        Map.entry("kimetsunoyaibamultiplayer:botanical_vial", 0x4E8F75),
        Map.entry("kimetsunoyaibamultiplayer:distilled_vial", 0xD2F2EC),
        Map.entry("kimetsunoyaibamultiplayer:refined_vial", 0xC979D7),
        Map.entry("kimetsunoyaibamultiplayer:cruel_vial", 0x7A001C),
        Map.entry("kimetsunoyaibamultiplayer:catalytic_reagent", 0xF1E2A0),
        Map.entry("kimetsunoyaibamultiplayer:antivenom", 0xE9F5E6),
        Map.entry("kimetsunoyaibamultiplayer:unidentified_human_extract", 0xC87A7A),
        Map.entry("kimetsunoyaibamultiplayer:unidentified_rotten_extract", 0x6F7E4A),
        Map.entry("kimetsunoyaibamultiplayer:adrenal_gland", 0xE36161),
        Map.entry("kimetsunoyaibamultiplayer:muscle_fibre", 0xB84B58),
        Map.entry("kimetsunoyaibamultiplayer:bone_marrow", 0xD8D0B7),
        Map.entry("kimetsunoyaibamultiplayer:neural_tissue", 0xA68EE1),
        Map.entry("kimetsunoyaibamultiplayer:putrid_extract", 0x6F8B3D),
        Map.entry("kimetsunoyaibamultiplayer:diseased_culture", 0x8AAC49),
        Map.entry("kimetsunoyaibamultiplayer:necrotic_tissue", 0x5D4C68),
        Map.entry("kimetsunoyaibamultiplayer:wither_extract", 0x2B2B2B),
        Map.entry("kimetsunoyaibamultiplayer:stellar_extract", 0xE9EBCB),
        Map.entry("kimetsunoyaibamultiplayer:immortal_extract", 0xDBE8A8),
        Map.entry("kimetsunoyaibamultiplayer:blaze_extract", 0xF28B28),
        Map.entry("kimetsunoyaibamultiplayer:phantom_extract", 0x98A6D8),
        Map.entry("kimetsunoyaibamultiplayer:sculk_extract", 0x1A5560),
        Map.entry("kimetsunoyaibamultiplayer:azure_extract", 0xB8D4E8),
        Map.entry("kimetsunoyaibamultiplayer:noxious_extract", 0xC9B23B),
        Map.entry("kimetsunoyaibamultiplayer:guardian_extract", 0x56BFB4),
        Map.entry("kimetsunoyaibamultiplayer:golden_extract", 0xE8C44A),
        Map.entry("kimetsunoyaibamultiplayer:scute_extract", 0x7CAE6C),
        Map.entry("kimetsunoyaibamultiplayer:illagers_extract", 0x8A3A32),
        Map.entry("kimetsunoyaibamultiplayer:powdered_snow_extract", 0xDDEAF1),
        Map.entry("kimetsunoyaibamultiplayer:diamond_extract", 0xA9DDFE),
        Map.entry("kimetsunoyaibamultiplayer:yamato_orochi_extract", 0x5E9A6B),
        Map.entry("kimetsunoyaibamultiplayer:creeping_doom_extract", 0x6E4A3A),
        Map.entry("kimetsunoyaibamultiplayer:herbal_extract", 0x1F6F8C),
        Map.entry("kimetsunoyaibamultiplayer:doma_extract", 0xB7E8F0),
        Map.entry("kimetsunoyaibamultiplayer:wisteria_extract", 0xB78CE6),
        Map.entry("kimetsunoyaibamultiplayer:infernal_culture", 0xE5622F),
        Map.entry("kimetsunoyaibamultiplayer:fortified_culture", 0x82A1BF),
        Map.entry("kimetsunoyaibamultiplayer:neural_culture", 0x8F6CE0),
        Map.entry("kimetsunoyaibamultiplayer:viral_culture", 0x9EBA43),
        Map.entry("kimetsunoyaibamultiplayer:necrotic_culture", 0x68486E),
        Map.entry("kimetsunoyaibamultiplayer:herbal_culture", 0x6EB36A),
        Map.entry("kimetsunoyaibamultiplayer:vitality_culture", 0x88D678),
        Map.entry("kimetsunoyaibamultiplayer:electrolytic_culture", 0x67C4E8),
        Map.entry("kimetsunoyaibamultiplayer:hemomimetic_culture", 0xB76584),
        Map.entry("kimetsunoyaibamultiplayer:proteolytic_culture", 0xD7E7A2),
        Map.entry("kimetsunoyaibamultiplayer:exorcistic_culture", 0xC8A9F0),
        Map.entry("kimetsunoyaibamultiplayer:neurotoxic_culture", 0xEFE7D8),
        Map.entry("kimetsunoyaibamultiplayer:demonic_genesis_culture", 0xB11220),
        Map.entry("kimetsunoyaibamultiplayer:solar_genesis_culture", 0xF0D66A),
        Map.entry("kimetsunoyaibamultiplayer:withered_spider_lily_culture", 0x1A1018),
        Map.entry("kimetsunoyaibamultiplayer:saturation_culture", 0xC15036),
        Map.entry("kimetsunoyaibamultiplayer:demonic_restoration_culture", 0xD8A0A8),
        Map.entry("kimetsunoyaibamultiplayer:solar_smite_culture", 0xF4E172),
        Map.entry("kimetsunoyaibamultiplayer:unidentified_blue_spider_lily_culture", 0x2D58B8),
        Map.entry("kimetsunoyaibamultiplayer:unidentified_rainbow_lily_culture", 0xC46BCF),
        Map.entry("kimetsunoyaibamultiplayer:demon_genesis_medicine", 0x9E0C18),
        Map.entry("kimetsunoyaibamultiplayer:solar_ascension_cure", 0xF3C642),
        Map.entry("kimetsunoyaibamultiplayer:demonic_hunger_cure", 0xC83A2A),
        Map.entry("kimetsunoyaibamultiplayer:demonic_restoration_cure", 0xE89AA6),
        Map.entry("kimetsunoyaibamultiplayer:solar_smite_serum", 0xF5DF64),
        Map.entry("kimetsunoyaibamultiplayer:familiar_tonic", 0x7C9E46),
        Map.entry("kimetsunoyaibamultiplayer:blue_spider_lily_petals", 0x2D58B8),
        Map.entry("kimetsunoyaibamultiplayer:blue_spider_lily_tea", 0x3A62D8),
        Map.entry("kimetsunoyaibamultiplayer:orochi_elixir", 0x173B22),
        Map.entry("kimetsunoyaibamultiplayer:damage_amplifier_vial", 0xD64A3A),
        Map.entry("kimetsunoyaibamultiplayer:defense_amplifier_vial", 0x6994C9),
        Map.entry("kimetsunoyaibamultiplayer:range_amplifier_vial", 0xA06BE3),
        Map.entry("kimetsunoyaibamultiplayer:speed_amplifier_vial", 0xB3D547),
        Map.entry("kimetsunoyaibamultiplayer:harmful_effect_amplifier_vial", 0x7E4A86),
        Map.entry("kimetsunoyaibamultiplayer:beneficial_effect_amplifier_vial", 0x78CF71),
        Map.entry("kimetsunoyaibamultiplayer:effect_duration_amplifier_vial", 0x62C5E8),
        Map.entry("kimetsunoyaibamultiplayer:blindness_infusion", 0x404A7D),
        Map.entry("kimetsunoyaibamultiplayer:wither_infusion", 0x383838),
        Map.entry("kimetsunoyaibamultiplayer:darkness_infusion", 0x183A44),
        Map.entry("kimetsunoyaibamultiplayer:nausea_infusion", 0x918231),
        Map.entry("kimetsunoyaibamultiplayer:mining_fatigue_infusion", 0x47A7A6),
        Map.entry("kimetsunoyaibamultiplayer:haste_infusion", 0xF1C649),
        Map.entry("kimetsunoyaibamultiplayer:hunger_infusion", 0x6C803B),
        Map.entry("kimetsunoyaibamultiplayer:resistance_infusion", 0x5F9F66),
        Map.entry("kimetsunoyaibamultiplayer:bleeding_infusion", 0x9F2A38),
        Map.entry("kimetsunoyaibamultiplayer:fire_infusion", 0xF26A18),
        Map.entry("kimetsunoyaibamultiplayer:frozen_infusion", 0xC4E7F2),
        Map.entry("kimetsunoyaibamultiplayer:wisteria_infusion", 0xB062D8),
        Map.entry("kimetsunoyaibamultiplayer:division_inhibition_infusion", 0x9D78D5),
        Map.entry("kimetsunoyaibamultiplayer:cell_destruction_infusion", 0xE45C84),
        Map.entry("kimetsunoyaibamultiplayer:immovable_infusion", 0xE4E2CB),
        Map.entry("kimetsunoyaibamultiplayer:wither_catalyst", 0x383838),
        Map.entry("kimetsunoyaibamultiplayer:stellar_catalyst", 0xE9EBCB),
        Map.entry("kimetsunoyaibamultiplayer:immortal_catalyst", 0xDBE8A8),
        Map.entry("kimetsunoyaibamultiplayer:blaze_catalyst", 0xF28B28),
        Map.entry("kimetsunoyaibamultiplayer:phantom_catalyst", 0x98A6D8),
        Map.entry("kimetsunoyaibamultiplayer:sculk_catalyst_vial", 0x1A5560),
        Map.entry("kimetsunoyaibamultiplayer:azure_catalyst", 0xB8D4E8),
        Map.entry("kimetsunoyaibamultiplayer:noxious_catalyst", 0xC9B23B),
        Map.entry("kimetsunoyaibamultiplayer:guardian_catalyst", 0x56BFB4),
        Map.entry("kimetsunoyaibamultiplayer:golden_catalyst", 0xE8C44A),
        Map.entry("kimetsunoyaibamultiplayer:scute_catalyst", 0x7CAE6C),
        Map.entry("kimetsunoyaibamultiplayer:illagers_catalyst", 0x8A3A32),
        Map.entry("kimetsunoyaibamultiplayer:powdered_snow_catalyst", 0xDDEAF1),
        Map.entry("kimetsunoyaibamultiplayer:creeping_doom_catalyst", 0x6E4A3A),
        Map.entry("kimetsunoyaibamultiplayer:evokers_catalyst", 0xB2B26A),
        Map.entry("kimetsunoyaibamultiplayer:vex_catalyst", 0x9FC8F2),
        Map.entry("kimetsunoyaibamultiplayer:prison_catalyst", 0x74D2C2),
        Map.entry("kimetsunoyaibamultiplayer:sonic_catalyst", 0x2D7D89),
        Map.entry("kimetsunoyaibamultiplayer:night_catalyst", 0x5F6B91),
        Map.entry("kimetsunoyaibamultiplayer:infernal_catalyst", 0xE88E37),
        Map.entry("kimetsunoyaibamultiplayer:flytrap_catalyst", 0x88AF4E),
        Map.entry("kimetsunoyaibamultiplayer:grave_catalyst", 0x5F6A45),
        Map.entry("kimetsunoyaibamultiplayer:weightless_catalyst", 0xA8B3DC),
        Map.entry("kimetsunoyaibamultiplayer:meteor_catalyst", 0xD6DAB8),
        Map.entry("kimetsunoyaibamultiplayer:charged_catalyst", 0xA7926A),
        Map.entry("kimetsunoyaibamultiplayer:arc_catalyst", 0x55A6C0),
        Map.entry("kimetsunoyaibamultiplayer:incendiary_catalyst", 0xD9633F),
        Map.entry("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", 0x5C9C6B),
        Map.entry("kimetsunoyaibamultiplayer:serpent_nest_catalyst", 0x82C96C),
        Map.entry("kimetsunoyaibamultiplayer:ecdysis_catalyst", 0xCBD6AB),
        Map.entry("kimetsunoyaibamultiplayer:phantom_serpent_catalyst", 0x9CB1EA),
        Map.entry("kimetsunoyaibamultiplayer:yamato_ascension_catalyst", 0xD8E8F3),
        Map.entry("kimetsunoyaibamultiplayer:dark_star_catalyst", 0x3E2F66),
        Map.entry("kimetsunoyaibamultiplayer:dark_star", 0x3E2F66),
        Map.entry("kimetsunoyaibamultiplayer:potion_effect_binder", 0xF2F2F2)
    );

    private static final List<ExtractDefinition> EXTRACTS = List.of(
        new ExtractDefinition("minecraft:wither_skeleton_skull", "kimetsunoyaibamultiplayer:wither_extract"),
        new ExtractDefinition("minecraft:nether_star", "kimetsunoyaibamultiplayer:stellar_extract"),
        new ExtractDefinition("minecraft:totem_of_undying", "kimetsunoyaibamultiplayer:immortal_extract"),
        new ExtractDefinition("minecraft:blaze_rod", "kimetsunoyaibamultiplayer:blaze_extract"),
        new ExtractDefinition("minecraft:phantom_membrane", "kimetsunoyaibamultiplayer:phantom_extract"),
        new ExtractDefinition("minecraft:sculk_catalyst", "kimetsunoyaibamultiplayer:sculk_extract"),
        new ExtractDefinition("minecraft:azure_bluet", "kimetsunoyaibamultiplayer:azure_extract"),
        new ExtractDefinition("minecraft:pufferfish", "kimetsunoyaibamultiplayer:noxious_extract"),
        new ExtractDefinition("minecraft:prismarine_shard", "kimetsunoyaibamultiplayer:guardian_extract"),
        new ExtractDefinition("minecraft:gold_ingot", "kimetsunoyaibamultiplayer:golden_extract"),
        new ExtractDefinition("minecraft:scute", "kimetsunoyaibamultiplayer:scute_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:vindicator_demon_art", "kimetsunoyaibamultiplayer:illagers_extract"),
        new ExtractDefinition("minecraft:powder_snow_bucket", "kimetsunoyaibamultiplayer:powdered_snow_extract"),
        new ExtractDefinition("minecraft:diamond", "kimetsunoyaibamultiplayer:diamond_extract"),
        new ExtractDefinition("kimetsunoyaiba:blooddemonart_kamanue", "kimetsunoyaibamultiplayer:yamato_orochi_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:creeping_doom", "kimetsunoyaibamultiplayer:creeping_doom_extract"),
        new ExtractDefinition("minecraft:grass", "kimetsunoyaibamultiplayer:herbal_extract"),
        new ExtractDefinition("kimetsunoyaiba:handfan", "kimetsunoyaibamultiplayer:doma_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:glowing_wisteria_leaves_pink", "kimetsunoyaibamultiplayer:wisteria_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:glowing_wisteria_leaves_cyan", "kimetsunoyaibamultiplayer:wisteria_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:glowing_wisteria_leaves_lavender", "kimetsunoyaibamultiplayer:wisteria_extract"),
        new ExtractDefinition("kimetsunoyaibamultiplayer:glowing_wisteria_leaves_cream", "kimetsunoyaibamultiplayer:wisteria_extract")
    );

    private static final List<InfusionDefinition> INFUSIONS = List.of(
        new InfusionDefinition("kimetsunoyaibamultiplayer:azure_extract", "kimetsunoyaibamultiplayer:blindness_infusion", "minecraft:blindness", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:wither_extract", "kimetsunoyaibamultiplayer:wither_infusion", "minecraft:wither", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:sculk_extract", "kimetsunoyaibamultiplayer:darkness_infusion", "minecraft:darkness", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:noxious_extract", "kimetsunoyaibamultiplayer:nausea_infusion", "minecraft:nausea", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:guardian_extract", "kimetsunoyaibamultiplayer:mining_fatigue_infusion", "minecraft:mining_fatigue", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:golden_extract", "kimetsunoyaibamultiplayer:haste_infusion", "minecraft:haste", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:rotten_blood_sample", "kimetsunoyaibamultiplayer:hunger_infusion", "minecraft:hunger", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:scute_extract", "kimetsunoyaibamultiplayer:resistance_infusion", "minecraft:resistance", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:illagers_extract", "kimetsunoyaibamultiplayer:bleeding_infusion", "kimetsunoyaibamultiplayer:bleeding", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:blaze_extract", "kimetsunoyaibamultiplayer:fire_infusion", FIRE_INFUSION_EFFECT_ID, 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:powdered_snow_extract", "kimetsunoyaibamultiplayer:frozen_infusion", FROZEN_INFUSION_EFFECT_ID, 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:wisteria_extract", "kimetsunoyaibamultiplayer:wisteria_infusion", "kimetsunoyaiba:wisteriapoison", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:exorcistic_culture", "kimetsunoyaibamultiplayer:division_inhibition_infusion", "kimetsunoyaiba:regeneration_inhibition", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:proteolytic_culture", "kimetsunoyaibamultiplayer:cell_destruction_infusion", "kimetsunoyaiba:cell_destruction", 8, 1),
        new InfusionDefinition("kimetsunoyaibamultiplayer:neurotoxic_culture", "kimetsunoyaibamultiplayer:immovable_infusion", "kimetsunoyaiba:immvable", 8, 1)
    );

    private static final List<CatalystDefinition> CATALYSTS = List.of(
        new CatalystDefinition("kimetsunoyaibamultiplayer:wither_extract", "kimetsunoyaibamultiplayer:wither_catalyst", CustomBloodDemonArtSavedData.MoveType.WITHER_SKULL),
        new CatalystDefinition("kimetsunoyaibamultiplayer:stellar_extract", "kimetsunoyaibamultiplayer:stellar_catalyst", CustomBloodDemonArtSavedData.MoveType.SINGULARITY),
        new CatalystDefinition("kimetsunoyaibamultiplayer:stellar_extract", "kimetsunoyaibamultiplayer:dark_star_catalyst", CustomBloodDemonArtSavedData.MoveType.DARK_STAR),
        new CatalystDefinition("kimetsunoyaibamultiplayer:immortal_extract", "kimetsunoyaibamultiplayer:immortal_catalyst", CustomBloodDemonArtSavedData.MoveType.TASTE_OF_IMMORTALITY),
        new CatalystDefinition("kimetsunoyaibamultiplayer:blaze_extract", "kimetsunoyaibamultiplayer:blaze_catalyst", CustomBloodDemonArtSavedData.MoveType.BLAZE_BARRAGE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:phantom_extract", "kimetsunoyaibamultiplayer:phantom_catalyst", CustomBloodDemonArtSavedData.MoveType.GLIDE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:sculk_extract", "kimetsunoyaibamultiplayer:sculk_catalyst_vial", CustomBloodDemonArtSavedData.MoveType.ROAR),
        new CatalystDefinition("kimetsunoyaibamultiplayer:yamato_orochi_extract", "kimetsunoyaibamultiplayer:yamato_orochi_catalyst", CustomBloodDemonArtSavedData.MoveType.YAMATO_OROCHI),
        new CatalystDefinition("kimetsunoyaibamultiplayer:azure_extract", "kimetsunoyaibamultiplayer:azure_catalyst", CustomBloodDemonArtSavedData.MoveType.FLOWER_DANCE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:serpent_nest_catalyst", CustomBloodDemonArtSavedData.MoveType.EIGHTFOLD_AMBUSH),
        new CatalystDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:ecdysis_catalyst", CustomBloodDemonArtSavedData.MoveType.SKIN_SHED),
        new CatalystDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:phantom_serpent_catalyst", CustomBloodDemonArtSavedData.MoveType.SNAKE_STEP),
        new CatalystDefinition("kimetsunoyaibamultiplayer:noxious_extract", "kimetsunoyaibamultiplayer:noxious_catalyst", CustomBloodDemonArtSavedData.MoveType.SPINE_BURST),
        new CatalystDefinition("kimetsunoyaibamultiplayer:guardian_extract", "kimetsunoyaibamultiplayer:guardian_catalyst", CustomBloodDemonArtSavedData.MoveType.GUARDIAN_LASER),
        new CatalystDefinition("kimetsunoyaibamultiplayer:golden_extract", "kimetsunoyaibamultiplayer:golden_catalyst", CustomBloodDemonArtSavedData.MoveType.MIDAS_TOUCH),
        new CatalystDefinition("kimetsunoyaibamultiplayer:scute_extract", "kimetsunoyaibamultiplayer:scute_catalyst", CustomBloodDemonArtSavedData.MoveType.DEFEND),
        new CatalystDefinition("kimetsunoyaibamultiplayer:illagers_extract", "kimetsunoyaibamultiplayer:illagers_catalyst", CustomBloodDemonArtSavedData.MoveType.VINDICATORS_CLEAVE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:powdered_snow_extract", "kimetsunoyaibamultiplayer:powdered_snow_catalyst", CustomBloodDemonArtSavedData.MoveType.WHITEOUT),
        new CatalystDefinition("kimetsunoyaibamultiplayer:creeping_doom_extract", "kimetsunoyaibamultiplayer:creeping_doom_catalyst", CustomBloodDemonArtSavedData.MoveType.EXPLODE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:illagers_catalyst", "kimetsunoyaibamultiplayer:evokers_catalyst", CustomBloodDemonArtSavedData.MoveType.FANGS_OF_THE_EARTH),
        new CatalystDefinition("kimetsunoyaibamultiplayer:azure_catalyst", "kimetsunoyaibamultiplayer:vex_catalyst", CustomBloodDemonArtSavedData.MoveType.VEX_SWARM),
        new CatalystDefinition("kimetsunoyaibamultiplayer:immortal_catalyst", "kimetsunoyaibamultiplayer:prison_catalyst", CustomBloodDemonArtSavedData.MoveType.PRISON),
        new CatalystDefinition("kimetsunoyaibamultiplayer:guardian_catalyst", "kimetsunoyaibamultiplayer:sonic_catalyst", CustomBloodDemonArtSavedData.MoveType.SONIC_SHRIEK),
        new CatalystDefinition("kimetsunoyaibamultiplayer:phantom_catalyst", "kimetsunoyaibamultiplayer:night_catalyst", CustomBloodDemonArtSavedData.MoveType.NIGHT_TERROR),
        new CatalystDefinition("kimetsunoyaibamultiplayer:blaze_catalyst", "kimetsunoyaibamultiplayer:infernal_catalyst", CustomBloodDemonArtSavedData.MoveType.INFERNAL_SPIN),
        new CatalystDefinition("kimetsunoyaibamultiplayer:noxious_catalyst", "kimetsunoyaibamultiplayer:flytrap_catalyst", CustomBloodDemonArtSavedData.MoveType.FLYTRAP),
        new CatalystDefinition("kimetsunoyaibamultiplayer:wither_catalyst", "kimetsunoyaibamultiplayer:grave_catalyst", CustomBloodDemonArtSavedData.MoveType.GRAVE_PULSE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:phantom_catalyst", "kimetsunoyaibamultiplayer:weightless_catalyst", CustomBloodDemonArtSavedData.MoveType.HOVER),
        new CatalystDefinition("kimetsunoyaibamultiplayer:stellar_catalyst", "kimetsunoyaibamultiplayer:meteor_catalyst", CustomBloodDemonArtSavedData.MoveType.SHOOTING_STAR),
        new CatalystDefinition("kimetsunoyaibamultiplayer:creeping_doom_catalyst", "kimetsunoyaibamultiplayer:charged_catalyst", CustomBloodDemonArtSavedData.MoveType.LIGHTNING_CHARGE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:guardian_catalyst", "kimetsunoyaibamultiplayer:arc_catalyst", CustomBloodDemonArtSavedData.MoveType.CHAIN_LIGHTNING),
        new CatalystDefinition("kimetsunoyaibamultiplayer:blaze_catalyst", "kimetsunoyaibamultiplayer:incendiary_catalyst", CustomBloodDemonArtSavedData.MoveType.INCENDIARY_PROJECTILE),
        new CatalystDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:yamato_ascension_catalyst", CustomBloodDemonArtSavedData.MoveType.EIGHTFOLD_ASCENDANT)
    );

    private static final List<CatalystUpgradeDefinition> CATALYST_UPGRADES = List.of(
        new CatalystUpgradeDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:azure_extract", "kimetsunoyaibamultiplayer:serpent_nest_catalyst"),
        new CatalystUpgradeDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:golden_extract", "kimetsunoyaibamultiplayer:ecdysis_catalyst"),
        new CatalystUpgradeDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:phantom_extract", "kimetsunoyaibamultiplayer:phantom_serpent_catalyst"),
        new CatalystUpgradeDefinition("kimetsunoyaibamultiplayer:yamato_orochi_catalyst", "kimetsunoyaibamultiplayer:scute_extract", "kimetsunoyaibamultiplayer:yamato_ascension_catalyst")
    );

    private static final List<AmplifierDefinition> AMPLIFIERS = List.of(
        new AmplifierDefinition("kimetsunoyaibamultiplayer:infernal_culture", "kimetsunoyaibamultiplayer:damage_amplifier_vial", AmplifierKind.DAMAGE),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:fortified_culture", "kimetsunoyaibamultiplayer:defense_amplifier_vial", AmplifierKind.DEFENSE),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:neural_culture", "kimetsunoyaibamultiplayer:range_amplifier_vial", AmplifierKind.RANGE),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:viral_culture", "kimetsunoyaibamultiplayer:speed_amplifier_vial", AmplifierKind.SPEED),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:necrotic_culture", "kimetsunoyaibamultiplayer:harmful_effect_amplifier_vial", AmplifierKind.HARMFUL_EFFECT),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:vitality_culture", "kimetsunoyaibamultiplayer:beneficial_effect_amplifier_vial", AmplifierKind.BENEFICIAL_EFFECT),
        new AmplifierDefinition("kimetsunoyaibamultiplayer:electrolytic_culture", "kimetsunoyaibamultiplayer:effect_duration_amplifier_vial", AmplifierKind.DURATION)
    );

    private static final Map<String, List<String>> MICROSCOPE_OUTPUTS = Map.of(
        "kimetsunoyaibamultiplayer:unidentified_human_extract", List.of(
            "kimetsunoyaibamultiplayer:adrenal_gland",
            "kimetsunoyaibamultiplayer:muscle_fibre",
            "kimetsunoyaibamultiplayer:bone_marrow",
            "kimetsunoyaibamultiplayer:neural_tissue"
        ),
        "kimetsunoyaibamultiplayer:unidentified_rotten_extract", List.of(
            "kimetsunoyaibamultiplayer:putrid_extract",
            "kimetsunoyaibamultiplayer:diseased_culture",
            "kimetsunoyaibamultiplayer:necrotic_tissue"
        )
    );

    private BloodDemonArtAlchemyCatalog() {
    }

    public static boolean isMicroscopeBlock(BlockState state) {
        return MICROSCOPE_BLOCK_ID.equals(id(state.getBlock()));
    }

    public static String id(ItemStack stack) {
        return id(stack.getItem());
    }

    public static String id(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "" : key.toString();
    }

    public static String id(net.minecraft.world.level.block.Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? "" : key.toString();
    }

    public static int tintFor(ItemStack stack) {
        return ITEM_TINTS.getOrDefault(id(stack), 0xFFFFFF);
    }

    public static int tintForId(String itemId) {
        return ITEM_TINTS.getOrDefault(itemId, 0xFFFFFF);
    }

    public static int ringTintFor(ItemStack stack) {
        if (isCatalyst(stack)) {
            return CATALYST_RING_TINT;
        }
        if (isInfusion(stack)) {
            return INFUSION_RING_TINT;
        }
        if (isAmplifier(stack)) {
            return AMPLIFIER_RING_TINT;
        }
        if (matches(stack, "kimetsunoyaibamultiplayer:potion_effect_binder")) {
            return BINDER_RING_TINT;
        }
        if (matches(stack, "kimetsunoyaibamultiplayer:orochi_elixir")) {
            return OROCHI_ELIXIR_RING_TINT;
        }
        if (AlchemyMedicineHandler.isLegendaryMedicine(stack)) {
            return LEGENDARY_MEDICINE_RING_TINT;
        }
        return 0xFFFFFF;
    }

    public static boolean isSpecialAlchemyItem(ItemStack stack) {
        String id = id(stack);
        return isInfusion(stack) || isCatalyst(stack) || isAmplifier(stack)
            || "kimetsunoyaibamultiplayer:potion_effect_binder".equals(id)
            || "kimetsunoyaibamultiplayer:orochi_elixir".equals(id)
            || AlchemyMedicineHandler.isLegendaryMedicine(stack);
    }

    public static boolean isAlchemyTableDisplayContainer(ItemStack stack) {
        return isVialDisplayItem(stack) || isPetriDishDisplayItem(stack);
    }

    public static boolean isLikelyAlchemyTableBottomInput(ItemStack stack) {
        String itemId = id(stack);
        return isEmptyPetriDish(stack) || isVialDisplayItem(stack)
            || "kimetsunoyaibamultiplayer:crude_vial".equals(itemId)
            || "kimetsunoyaibamultiplayer:refined_vial".equals(itemId)
            || "kimetsunoyaibamultiplayer:cruel_vial".equals(itemId)
            || "kimetsunoyaibamultiplayer:familiar_tonic".equals(itemId)
            || "kimetsunoyaibamultiplayer:orochi_elixir".equals(itemId);
    }

    public static boolean isVialDisplayItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = ResourceLocation.tryParse(id(stack)) == null ? id(stack) : ResourceLocation.tryParse(id(stack)).getPath();
        return path.contains("vial")
            || path.contains("elixir")
            || path.contains("tonic")
            || path.contains("sample")
            || path.contains("infusion")
            || path.contains("catalyst")
            || path.contains("antivenom")
            || path.contains("medicine")
            || path.contains("cure")
            || path.contains("serum")
            || "potion_effect_binder".equals(path);
    }

    public static boolean isPetriDishDisplayItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = ResourceLocation.tryParse(id(stack)) == null ? id(stack) : ResourceLocation.tryParse(id(stack)).getPath();
        return path.contains("petri_dish")
            || path.contains("culture")
            || path.contains("extract")
            || path.contains("tissue")
            || path.contains("gland")
            || path.contains("marrow")
            || path.contains("fibre");
    }

    public static boolean isLens(ItemStack stack) {
        return matches(stack, "kimetsunoyaibamultiplayer:amethyst_lens");
    }

    public static boolean isEmptyVial(ItemStack stack) {
        return matches(stack, "kimetsunoyaibamultiplayer:empty_vial");
    }

    public static boolean isEmptyPetriDish(ItemStack stack) {
        return matches(stack, "kimetsunoyaibamultiplayer:empty_petri_dish");
    }

    public static boolean isUnidentifiedExtract(ItemStack stack) {
        return MICROSCOPE_OUTPUTS.containsKey(id(stack))
            || matches(stack, "kimetsunoyaibamultiplayer:unidentified_blue_spider_lily_culture")
            || matches(stack, "kimetsunoyaibamultiplayer:unidentified_rainbow_lily_culture");
    }

    public static boolean isInfusion(ItemStack stack) {
        String id = id(stack);
        return INFUSIONS.stream().anyMatch(def -> def.outputId().equals(id));
    }

    public static boolean isCatalyst(ItemStack stack) {
        String id = id(stack);
        return CATALYSTS.stream().anyMatch(def -> def.outputId().equals(id));
    }

    public static boolean isAmplifier(ItemStack stack) {
        String id = id(stack);
        return AMPLIFIERS.stream().anyMatch(def -> def.outputId().equals(id));
    }

    public static boolean isHumanFlesh(ItemStack stack) {
        String id = id(stack);
        return id.endsWith("human_flesh_3") || id.endsWith("human_flesh_4") || id.endsWith("human_flesh_5") || id.contains("human_flesh");
    }

    public static boolean matches(ItemStack stack, String itemId) {
        return itemId.equals(id(stack));
    }

    public static ItemStack stack(String itemId) {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static ItemStack stack(String itemId, int count) {
        ItemStack stack = stack(itemId);
        if (!stack.isEmpty()) {
            stack.setCount(count);
        }
        return stack;
    }

    public static ItemStack microscopeOutput(ItemStack input, RandomSource random) {
        if (matches(input, "kimetsunoyaibamultiplayer:unidentified_blue_spider_lily_culture")) {
            return blueSpiderLilyCultureMicroscopeOutput(input, random);
        }
        if (matches(input, "kimetsunoyaibamultiplayer:unidentified_rainbow_lily_culture")) {
            return rainbowLilyCultureMicroscopeOutput(input, random);
        }
        List<String> outputs = MICROSCOPE_OUTPUTS.get(id(input));
        if (outputs == null || outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack(outputs.get(random.nextInt(outputs.size())));
    }

    private static ItemStack blueSpiderLilyCultureMicroscopeOutput(ItemStack input, RandomSource random) {
        int potency = potency(input);
        if (random.nextInt(100) < rareCultureChance(potency, 10, 2)) {
            return stack("kimetsunoyaibamultiplayer:demonic_genesis_culture");
        }
        if (random.nextInt(100) < rareCultureChance(potency, 3, 7)) {
            return stack("kimetsunoyaibamultiplayer:solar_genesis_culture");
        }
        return random.nextInt(100) < 60
            ? stack("kimetsunoyaibamultiplayer:withered_spider_lily_culture")
            : stack("kimetsunoyaibamultiplayer:neurotoxic_culture");
    }

    private static ItemStack rainbowLilyCultureMicroscopeOutput(ItemStack input, RandomSource random) {
        int potency = potency(input);
        if (random.nextInt(100) < rareCultureChance(potency, 10, 2)) {
            return stack("kimetsunoyaibamultiplayer:demonic_restoration_culture");
        }
        if (random.nextInt(100) < rareCultureChance(potency, 3, 7)) {
            return stack("kimetsunoyaibamultiplayer:solar_smite_culture");
        }
        int roll = random.nextInt(100);
        if (potency >= 4) {
            if (roll < 90) {
                return stack("kimetsunoyaibamultiplayer:saturation_culture");
            }
            return roll < 95
                ? stack("kimetsunoyaibamultiplayer:withered_spider_lily_culture")
                : stack("kimetsunoyaibamultiplayer:herbal_culture");
        }
        if (roll < 40) {
            return stack("kimetsunoyaibamultiplayer:withered_spider_lily_culture");
        }
        if (roll < 80) {
            return stack("kimetsunoyaibamultiplayer:herbal_culture");
        }
        return stack("kimetsunoyaibamultiplayer:saturation_culture");
    }

    private static int rareCultureChance(int potency, int lowPotencyMultiplier, int highPotencyMultiplier) {
        int clampedPotency = Math.max(0, potency);
        return clampedPotency <= 4
            ? clampedPotency * lowPotencyMultiplier
            : clampedPotency * highPotencyMultiplier;
    }

    public static ItemStack withPotency(ItemStack stack, int potency) {
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putInt(POTENCY_KEY, Math.max(0, potency));
        }
        return stack;
    }

    public static int potency(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getInt(POTENCY_KEY)) : 0;
    }

    public static boolean isPotencyCulture(ItemStack stack) {
        return matches(stack, "kimetsunoyaibamultiplayer:unidentified_blue_spider_lily_culture")
            || matches(stack, "kimetsunoyaibamultiplayer:unidentified_rainbow_lily_culture");
    }

    public static ItemStack alchemyTableOutput(ItemStack first, ItemStack second) {
        if (isHumanFlesh(first) && isEmptyVial(second) || isHumanFlesh(second) && isEmptyVial(first)) {
            return stack("kimetsunoyaibamultiplayer:blood_sample");
        }
        if (matches(first, "minecraft:rotten_flesh") && isEmptyVial(second)
            || matches(second, "minecraft:rotten_flesh") && isEmptyVial(first)) {
            return stack("kimetsunoyaibamultiplayer:rotten_blood_sample");
        }
        if (isHumanFlesh(first) && isEmptyPetriDish(second) || isHumanFlesh(second) && isEmptyPetriDish(first)) {
            return stack("kimetsunoyaibamultiplayer:unidentified_human_extract");
        }
        if (matches(first, "minecraft:rotten_flesh") && isEmptyPetriDish(second)
            || matches(second, "minecraft:rotten_flesh") && isEmptyPetriDish(first)) {
            return stack("kimetsunoyaibamultiplayer:unidentified_rotten_extract");
        }

        for (ExtractDefinition definition : EXTRACTS) {
            if ((definition.matches(first) && isEmptyVial(second)) || (definition.matches(second) && isEmptyVial(first))) {
                return stack(definition.outputId());
            }
        }

        for (AmplifierDefinition definition : AMPLIFIERS) {
            if ((matches(first, definition.inputId()) && matches(second, "kimetsunoyaibamultiplayer:refined_vial"))
                || (matches(second, definition.inputId()) && matches(first, "kimetsunoyaibamultiplayer:refined_vial"))) {
                return stack(definition.outputId());
            }
        }

        if ((matches(first, "minecraft:lily_of_the_valley") && matches(second, "kimetsunoyaibamultiplayer:botanical_vial"))
            || (matches(second, "minecraft:lily_of_the_valley") && matches(first, "kimetsunoyaibamultiplayer:botanical_vial"))) {
            return stack("kimetsunoyaibamultiplayer:antivenom");
        }

        if ((matches(first, "kimetsunoyaibamultiplayer:orochi_scales") && matches(second, "kimetsunoyaibamultiplayer:familiar_tonic"))
            || (matches(second, "kimetsunoyaibamultiplayer:orochi_scales") && matches(first, "kimetsunoyaibamultiplayer:familiar_tonic"))) {
            return stack("kimetsunoyaibamultiplayer:orochi_elixir");
        }

        if ((matches(first, "kimetsunoyaibamultiplayer:demonic_genesis_culture") && matches(second, "kimetsunoyaibamultiplayer:cruel_vial"))
            || (matches(second, "kimetsunoyaibamultiplayer:demonic_genesis_culture") && matches(first, "kimetsunoyaibamultiplayer:cruel_vial"))) {
            return stack("kimetsunoyaibamultiplayer:demon_genesis_medicine");
        }
        if ((matches(first, "kimetsunoyaibamultiplayer:solar_genesis_culture") && matches(second, "kimetsunoyaibamultiplayer:cruel_vial"))
            || (matches(second, "kimetsunoyaibamultiplayer:solar_genesis_culture") && matches(first, "kimetsunoyaibamultiplayer:cruel_vial"))) {
            return stack("kimetsunoyaibamultiplayer:solar_ascension_cure");
        }
        if ((matches(first, "kimetsunoyaibamultiplayer:demonic_restoration_culture") && matches(second, "kimetsunoyaibamultiplayer:botanical_vial"))
            || (matches(second, "kimetsunoyaibamultiplayer:demonic_restoration_culture") && matches(first, "kimetsunoyaibamultiplayer:botanical_vial"))) {
            return stack("kimetsunoyaibamultiplayer:demonic_restoration_cure");
        }
        if ((matches(first, "kimetsunoyaibamultiplayer:solar_smite_culture") && matches(second, "kimetsunoyaibamultiplayer:botanical_vial"))
            || (matches(second, "kimetsunoyaibamultiplayer:solar_smite_culture") && matches(first, "kimetsunoyaibamultiplayer:botanical_vial"))) {
            return stack("kimetsunoyaibamultiplayer:solar_smite_serum");
        }
        if ((matches(first, "kimetsunoyaibamultiplayer:saturation_culture") && matches(second, "kimetsunoyaibamultiplayer:botanical_vial"))
            || (matches(second, "kimetsunoyaibamultiplayer:saturation_culture") && matches(first, "kimetsunoyaibamultiplayer:botanical_vial"))) {
            return stack("kimetsunoyaibamultiplayer:demonic_hunger_cure");
        }

        for (CatalystDefinition definition : CATALYSTS) {
            if (!definition.inputId().endsWith("_extract")) {
                continue;
            }
            if ((matches(first, definition.inputId()) && matches(second, "kimetsunoyaibamultiplayer:cruel_vial"))
                || (matches(second, definition.inputId()) && matches(first, "kimetsunoyaibamultiplayer:cruel_vial"))) {
                return stack(definition.outputId());
            }
        }

        for (CatalystUpgradeDefinition definition : CATALYST_UPGRADES) {
            if ((matches(first, definition.catalystId()) && matches(second, definition.ingredientId()))
                || (matches(second, definition.catalystId()) && matches(first, definition.ingredientId()))) {
                return stack(definition.outputId());
            }
        }

        return ItemStack.EMPTY;
    }

    public static String infusionEffectId(ItemStack stack) {
        String id = id(stack);
        for (InfusionDefinition definition : INFUSIONS) {
            if (definition.outputId().equals(id)) {
                return definition.effectId();
            }
        }
        return "";
    }

    public static int infusionDurationSeconds(ItemStack stack) {
        String id = id(stack);
        for (InfusionDefinition definition : INFUSIONS) {
            if (definition.outputId().equals(id)) {
                return definition.durationSeconds();
            }
        }
        return 8;
    }

    public static int infusionAmplifier(ItemStack stack) {
        String id = id(stack);
        for (InfusionDefinition definition : INFUSIONS) {
            if (definition.outputId().equals(id)) {
                return definition.amplifier();
            }
        }
        return 1;
    }

    public static boolean isFireInfusionEffectId(String effectId) {
        return FIRE_INFUSION_EFFECT_ID.equals(effectId);
    }

    public static boolean isFrozenInfusionEffectId(String effectId) {
        return FROZEN_INFUSION_EFFECT_ID.equals(effectId);
    }

    public static ItemStack brewingOutput(ItemStack ingredient, ItemStack input) {
        if (matches(ingredient, id(ModItems.HEMOLITH_DUST.get())) && matches(input, "kimetsunoyaibamultiplayer:blood_sample")) {
            return stack("kimetsunoyaibamultiplayer:crude_vial");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:calcite_powder") && matches(input, "kimetsunoyaibamultiplayer:herbal_extract")) {
            return stack("kimetsunoyaibamultiplayer:botanical_vial");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:fermented_orchid") && matches(input, "kimetsunoyaibamultiplayer:crude_vial")) {
            return stack("kimetsunoyaibamultiplayer:refined_vial");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:fermented_orchid") && matches(input, "kimetsunoyaibamultiplayer:botanical_vial")) {
            return stack("kimetsunoyaibamultiplayer:distilled_vial");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:immortal_daisy") && matches(input, "kimetsunoyaibamultiplayer:crude_vial")) {
            return stack("kimetsunoyaibamultiplayer:cruel_vial");
        }
        if (matches(ingredient, "minecraft:slime_ball") && matches(input, "kimetsunoyaibamultiplayer:refined_vial")) {
            return stack("kimetsunoyaibamultiplayer:potion_effect_binder");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:herbal_culture")
            && matches(input, "kimetsunoyaibamultiplayer:crude_vial")) {
            return stack("kimetsunoyaibamultiplayer:familiar_tonic");
        }
        if (matches(ingredient, "kimetsunoyaibamultiplayer:hemomimetic_culture")
            && matches(input, "kimetsunoyaibamultiplayer:cruel_vial")) {
            return stack("kimetsunoyaibamultiplayer:catalytic_reagent");
        }
        for (InfusionDefinition definition : INFUSIONS) {
            if (matches(ingredient, definition.inputId())
                && (matches(input, "kimetsunoyaibamultiplayer:crude_vial") || matches(input, "kimetsunoyaibamultiplayer:botanical_vial"))) {
                return stack(definition.outputId());
            }
        }
        for (CatalystDefinition definition : baseCatalystDefinitions()) {
            if (matches(ingredient, definition.inputId()) && matches(input, "kimetsunoyaibamultiplayer:catalytic_reagent")) {
                return stack(definition.outputId(), 3);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack containerReturn(ItemStack consumedStack) {
        if (isInfusion(consumedStack) || isAmplifier(consumedStack) || isCatalyst(consumedStack)
            || matches(consumedStack, "kimetsunoyaibamultiplayer:antivenom")
            || AlchemyMedicineHandler.isLegendaryMedicine(consumedStack)) {
            return stack("kimetsunoyaibamultiplayer:empty_vial");
        }
        if (matches(consumedStack, "kimetsunoyaibamultiplayer:orochi_elixir")
            || matches(consumedStack, "kimetsunoyaibamultiplayer:familiar_tonic")
            || matches(consumedStack, "kimetsunoyaibamultiplayer:catalytic_reagent")) {
            return stack("kimetsunoyaibamultiplayer:empty_vial");
        }
        if (matches(consumedStack, "kimetsunoyaibamultiplayer:blue_spider_lily_tea")) {
            return new ItemStack(Items.BOWL);
        }
        if (consumedStack.is(Items.POTION)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        return ItemStack.EMPTY;
    }

    public static CustomBloodDemonArtSavedData.MoveType catalystMove(ItemStack stack) {
        return catalystMove(id(stack));
    }

    public static CustomBloodDemonArtSavedData.MoveType catalystMove(String catalystItemId) {
        for (CatalystDefinition definition : CATALYSTS) {
            if (definition.outputId().equals(catalystItemId)) {
                return definition.unlockedMove();
            }
        }
        return null;
    }

    public static AmplifierKind amplifierKind(String amplifierItemId) {
        for (AmplifierDefinition definition : AMPLIFIERS) {
            if (definition.outputId().equals(amplifierItemId)) {
                return definition.kind();
            }
        }
        return AmplifierKind.NONE;
    }

    public static List<CatalystDefinition> baseCatalystDefinitions() {
        return CATALYSTS.stream()
            .filter(definition -> definition.inputId().endsWith("_extract"))
            .toList();
    }

    public static boolean isHarmfulEffect(MobEffect effect) {
        return effect == MobEffects.WITHER
            || effect == MobEffects.BLINDNESS
            || effect == MobEffects.DARKNESS
            || effect == MobEffects.CONFUSION
            || effect == MobEffects.MOVEMENT_SLOWDOWN
            || effect == MobEffects.DIG_SLOWDOWN
            || effect == MobEffects.HUNGER
            || Objects.equals(effect, ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "bleeding")));
    }

    public static List<String> allItemIdsForAssets() {
        List<String> ids = new ArrayList<>(ITEM_TINTS.keySet());
        ids.sort(String::compareTo);
        return ids;
    }

    public static String assetTextureStem(String itemId) {
        String path = ResourceLocation.tryParse(itemId) == null ? itemId : ResourceLocation.tryParse(itemId).getPath();
        if (path.contains("petri_dish") || path.contains("culture") || path.contains("extract") || path.contains("tissue")
            || path.contains("gland") || path.contains("marrow") || path.contains("fibre")) {
            return path.contains("empty") ? "empty_petri_dish" : "full_petri_dish";
        }
        if (path.contains("vial") || path.contains("elixir") || path.contains("tonic")
            || path.contains("sample") || path.contains("infusion") || path.contains("catalyst")
            || path.contains("medicine") || path.contains("cure") || path.contains("serum")) {
            return path.contains("empty") ? "empty_vial" : "full_vial";
        }
        if (path.contains("antivenom")) {
            return "full_vial";
        }
        return path;
    }

    public record ExtractDefinition(String inputId, String outputId) {
        public boolean matches(ItemStack stack) {
            return inputId.equals(id(stack));
        }
    }

    public record InfusionDefinition(String inputId, String outputId, String effectId, int durationSeconds, int amplifier) {
    }

    public record CatalystDefinition(String inputId, String outputId, CustomBloodDemonArtSavedData.MoveType unlockedMove) {
    }

    public record CatalystUpgradeDefinition(String catalystId, String ingredientId, String outputId) {
    }

    public record AmplifierDefinition(String inputId, String outputId, AmplifierKind kind) {
    }

    public enum AmplifierKind {
        NONE,
        DAMAGE,
        DEFENSE,
        RANGE,
        SPEED,
        HARMFUL_EFFECT,
        BENEFICIAL_EFFECT,
        DURATION
    }
}
