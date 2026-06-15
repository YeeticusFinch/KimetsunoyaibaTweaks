package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModAlchemyItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, KimetsunoyaibaMultiplayer.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KimetsunoyaibaMultiplayer.MODID);

    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryObject<Item> MINCED_HUMAN_FLESH = registerPlain("minced_human_flesh");
    public static final RegistryObject<Item> BONE_DUST = registerPlain("bone_dust");
    public static final RegistryObject<Item> CALCITE_POWDER = registerPlain("calcite_powder");
    public static final RegistryObject<Item> AMETHYST_LENS = register("amethyst_lens",
        () -> new AlchemyLensItem(new Item.Properties(), 0xB99AF0));
    public static final RegistryObject<Item> EMPTY_VIAL = registerPlain("empty_vial");
    public static final RegistryObject<Item> EMPTY_PETRI_DISH = registerPlain("empty_petri_dish");

    public static final RegistryObject<Item> BLOOD_SAMPLE = registerPlain("blood_sample");
    public static final RegistryObject<Item> ROTTEN_BLOOD_SAMPLE = registerPlain("rotten_blood_sample");
    public static final RegistryObject<Item> CRUDE_VIAL = registerPlain("crude_vial");
    public static final RegistryObject<Item> REFINED_VIAL = registerPlain("refined_vial");
    public static final RegistryObject<Item> CRUEL_VIAL = registerPlain("cruel_vial");

    public static final RegistryObject<Item> UNIDENTIFIED_HUMAN_EXTRACT = registerPlain("unidentified_human_extract");
    public static final RegistryObject<Item> UNIDENTIFIED_ROTTEN_EXTRACT = registerPlain("unidentified_rotten_extract");
    public static final RegistryObject<Item> ADRENAL_GLAND = registerPlain("adrenal_gland");
    public static final RegistryObject<Item> MUSCLE_FIBRE = registerPlain("muscle_fibre");
    public static final RegistryObject<Item> BONE_MARROW = registerPlain("bone_marrow");
    public static final RegistryObject<Item> NEURAL_TISSUE = registerPlain("neural_tissue");
    public static final RegistryObject<Item> PUTRID_EXTRACT = registerPlain("putrid_extract");
    public static final RegistryObject<Item> DISEASED_CULTURE = registerPlain("diseased_culture");
    public static final RegistryObject<Item> NECROTIC_TISSUE = registerPlain("necrotic_tissue");

    public static final RegistryObject<Item> WITHER_EXTRACT = registerPlain("wither_extract");
    public static final RegistryObject<Item> STELLAR_EXTRACT = registerPlain("stellar_extract");
    public static final RegistryObject<Item> IMMORTAL_EXTRACT = registerPlain("immortal_extract");
    public static final RegistryObject<Item> BLAZE_EXTRACT = registerPlain("blaze_extract");
    public static final RegistryObject<Item> PHANTOM_EXTRACT = registerPlain("phantom_extract");
    public static final RegistryObject<Item> SCULK_EXTRACT = registerPlain("sculk_extract");
    public static final RegistryObject<Item> AZURE_EXTRACT = registerPlain("azure_extract");
    public static final RegistryObject<Item> NOXIOUS_EXTRACT = registerPlain("noxious_extract");
    public static final RegistryObject<Item> GUARDIAN_EXTRACT = registerPlain("guardian_extract");
    public static final RegistryObject<Item> GOLDEN_EXTRACT = registerPlain("golden_extract");
    public static final RegistryObject<Item> SCUTE_EXTRACT = registerPlain("scute_extract");
    public static final RegistryObject<Item> ILLAGERS_EXTRACT = registerPlain("illagers_extract");
    public static final RegistryObject<Item> POWDERED_SNOW_EXTRACT = registerPlain("powdered_snow_extract");

    public static final RegistryObject<Item> INFERNAL_CULTURE = registerPlain("infernal_culture");
    public static final RegistryObject<Item> FORTIFIED_CULTURE = registerPlain("fortified_culture");
    public static final RegistryObject<Item> NEURAL_CULTURE = registerPlain("neural_culture");
    public static final RegistryObject<Item> VIRAL_CULTURE = registerPlain("viral_culture");
    public static final RegistryObject<Item> NECROTIC_CULTURE = registerPlain("necrotic_culture");
    public static final RegistryObject<Item> HERBAL_CULTURE = registerPlain("herbal_culture");
    public static final RegistryObject<Item> VITALITY_CULTURE = registerPlain("vitality_culture");
    public static final RegistryObject<Item> ELECTROLYTIC_CULTURE = registerPlain("electrolytic_culture");

    public static final RegistryObject<Item> DAMAGE_AMPLIFIER_VIAL = registerSpecial("damage_amplifier_vial");
    public static final RegistryObject<Item> DEFENSE_AMPLIFIER_VIAL = registerSpecial("defense_amplifier_vial");
    public static final RegistryObject<Item> RANGE_AMPLIFIER_VIAL = registerSpecial("range_amplifier_vial");
    public static final RegistryObject<Item> SPEED_AMPLIFIER_VIAL = registerSpecial("speed_amplifier_vial");
    public static final RegistryObject<Item> HARMFUL_EFFECT_AMPLIFIER_VIAL = registerSpecial("harmful_effect_amplifier_vial");
    public static final RegistryObject<Item> BENEFICIAL_EFFECT_AMPLIFIER_VIAL = registerSpecial("beneficial_effect_amplifier_vial");
    public static final RegistryObject<Item> EFFECT_DURATION_AMPLIFIER_VIAL = registerSpecial("effect_duration_amplifier_vial");

    public static final RegistryObject<Item> BLINDNESS_INFUSION = registerSpecial("blindness_infusion");
    public static final RegistryObject<Item> WITHER_INFUSION = registerSpecial("wither_infusion");
    public static final RegistryObject<Item> DARKNESS_INFUSION = registerSpecial("darkness_infusion");
    public static final RegistryObject<Item> NAUSEA_INFUSION = registerSpecial("nausea_infusion");
    public static final RegistryObject<Item> MINING_FATIGUE_INFUSION = registerSpecial("mining_fatigue_infusion");
    public static final RegistryObject<Item> HASTE_INFUSION = registerSpecial("haste_infusion");
    public static final RegistryObject<Item> HUNGER_INFUSION = registerSpecial("hunger_infusion");
    public static final RegistryObject<Item> RESISTANCE_INFUSION = registerSpecial("resistance_infusion");
    public static final RegistryObject<Item> BLEEDING_INFUSION = registerSpecial("bleeding_infusion");
    public static final RegistryObject<Item> FIRE_INFUSION = registerSpecial("fire_infusion");
    public static final RegistryObject<Item> FROZEN_INFUSION = registerSpecial("frozen_infusion");

    public static final RegistryObject<Item> WITHER_CATALYST = registerSpecial("wither_catalyst");
    public static final RegistryObject<Item> STELLAR_CATALYST = registerSpecial("stellar_catalyst");
    public static final RegistryObject<Item> IMMORTAL_CATALYST = registerSpecial("immortal_catalyst");
    public static final RegistryObject<Item> BLAZE_CATALYST = registerSpecial("blaze_catalyst");
    public static final RegistryObject<Item> PHANTOM_CATALYST = registerSpecial("phantom_catalyst");
    public static final RegistryObject<Item> SCULK_CATALYST_VIAL = registerSpecial("sculk_catalyst_vial");
    public static final RegistryObject<Item> AZURE_CATALYST = registerSpecial("azure_catalyst");
    public static final RegistryObject<Item> NOXIOUS_CATALYST = registerSpecial("noxious_catalyst");
    public static final RegistryObject<Item> GUARDIAN_CATALYST = registerSpecial("guardian_catalyst");
    public static final RegistryObject<Item> GOLDEN_CATALYST = registerSpecial("golden_catalyst");
    public static final RegistryObject<Item> SCUTE_CATALYST = registerSpecial("scute_catalyst");
    public static final RegistryObject<Item> ILLAGERS_CATALYST = registerSpecial("illagers_catalyst");
    public static final RegistryObject<Item> POWDERED_SNOW_CATALYST = registerSpecial("powdered_snow_catalyst");
    public static final RegistryObject<Item> CREEPING_DOOM_EXTRACT = registerSpecial("creeping_doom_extract");
    public static final RegistryObject<Item> CREEPING_DOOM_CATALYST = registerSpecial("creeping_doom_catalyst");
    public static final RegistryObject<Item> EVOKERS_CATALYST = registerSpecial("evokers_catalyst");
    public static final RegistryObject<Item> VEX_CATALYST = registerSpecial("vex_catalyst");
    public static final RegistryObject<Item> PRISON_CATALYST = registerSpecial("prison_catalyst");
    public static final RegistryObject<Item> SONIC_CATALYST = registerSpecial("sonic_catalyst");
    public static final RegistryObject<Item> NIGHT_CATALYST = registerSpecial("night_catalyst");
    public static final RegistryObject<Item> INFERNAL_CATALYST = registerSpecial("infernal_catalyst");
    public static final RegistryObject<Item> FLYTRAP_CATALYST = registerSpecial("flytrap_catalyst");
    public static final RegistryObject<Item> GRAVE_CATALYST = registerSpecial("grave_catalyst");
    public static final RegistryObject<Item> WEIGHTLESS_CATALYST = registerSpecial("weightless_catalyst");
    public static final RegistryObject<Item> METEOR_CATALYST = registerSpecial("meteor_catalyst");
    public static final RegistryObject<Item> CHARGED_CATALYST = registerSpecial("charged_catalyst");
    public static final RegistryObject<Item> ARC_CATALYST = registerSpecial("arc_catalyst");
    public static final RegistryObject<Item> INCENDIARY_CATALYST = registerSpecial("incendiary_catalyst");
    public static final RegistryObject<Item> POTION_EFFECT_BINDER = registerSpecial("potion_effect_binder");

    public static final RegistryObject<CreativeModeTab> ALCHEMY_TAB = CREATIVE_MODE_TABS.register("alchemy",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kimetsunoyaibamultiplayer.alchemy"))
            .icon(() -> new ItemStack(CRUDE_VIAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModAlchemyBlocks.ALCHEMY_TABLE.get());
                output.accept(ModAlchemyBlocks.VIAL_RACK.get());
                output.accept(ModAlchemyBlocks.FERMENTED_ORCHID.get());
                output.accept(ModAlchemyBlocks.IMMORTAL_DAISY.get());
                for (RegistryObject<Item> item : TAB_ITEMS) {
                    output.accept(item.get());
                }
            }).build());

    private ModAlchemyItems() {
    }

    private static RegistryObject<Item> register(String id, Supplier<Item> supplier) {
        RegistryObject<Item> item = ITEMS.register(id, supplier);
        TAB_ITEMS.add(item);
        return item;
    }

    private static RegistryObject<Item> registerPlain(String id) {
        return register(id, () -> new AlchemyItem(new Item.Properties(), false, tintOf(id)));
    }

    private static RegistryObject<Item> registerSpecial(String id) {
        return register(id, () -> new AlchemyItem(new Item.Properties(), true, tintOf(id)));
    }

    private static int tintOf(String path) {
        return BloodDemonArtAlchemyCatalog.tintForId(KimetsunoyaibaMultiplayer.MODID + ":" + path);
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
