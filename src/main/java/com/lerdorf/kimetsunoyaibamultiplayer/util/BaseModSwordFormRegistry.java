package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Runtime form IDs and item-specific availability for base-mod swords. */
public final class BaseModSwordFormRegistry {
    public record FormDefinition(int id, String name, String color) {
    }

    private static final Map<String, int[]> ITEM_FORMS = new HashMap<>();
    private static final Map<Integer, FormDefinition> FORMS = new HashMap<>();

    static {
        register("nichirinsword_thunder", new int[]{101, 102, 103, 104, 105, 106, 107, 111, 112, 113});
        register("nichirinsword_flame", new int[]{301, 302, 303, 304, 305});
        register("nichirinsword_rengoku", new int[]{301, 302, 303, 304, 305, 309});
        register("nichirinsword_mist", new int[]{401, 402, 403, 404, 405, 407});
        register("nichirinsword_wind", new int[]{501, 502, 503, 504, 505, 506, 507, 508, 509});
        register("nichirinsword_water", new int[]{601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611});
        register("nichirinsword_inosuke", new int[]{701, 702, 703, 704, 705, 706, 707, 708, 709, 710});
        register("nichirinsword_iguro", new int[]{801, 802, 803, 804, 805});
        register("nichirinsword_kocho", new int[]{901, 902, 904, 905});
        register("nichirinsword_senior", new int[]{1001, 1002, 1003, 1004, 1005, 1006, 1007});
        register("nichirinsword_senior_2", new int[]{1001, 1002, 1003, 1004, 1005, 1006, 1007});
        register("nichirinswordmoon", new int[]{1101, 1102, 1103, 1105, 1106});
        register("sword_kokushibo_1", new int[]{1101, 1102, 1103, 1105, 1106, 1121});
        register("sword_kokushibo_2", new int[]{1107, 1108, 1109, 1110, 1114, 1116, 1121});
        register("nichirinsword_yoriichi", new int[]{1201, 1202, 1203, 1204, 1205, 1206, 1207,
            1208, 1209, 1210, 1211, 1212, 1213});
        register("nichirinsword_uzui", new int[]{1301, 1304, 1305});
        register("nichirinsword_kanawo", new int[]{1401, 1402, 1404, 1405});
        register("nichirinsword_kanae", new int[]{1401, 1402, 1404, 1405});
        register("nichirinsword_kanroji", new int[]{1501, 1502, 1505, 1506});
        register("nichirinsword_himejima_1", new int[]{1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, 1611});
        register("nichirinsword_himejima_2", new int[]{1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, 1611});
        register("nichirinsword_bamboo", new int[]{1701, 1702, 1703, 1704, 1705, 1706, 1707, 1708, 1709, 1710, 1711, 1712});
        register("nichirinsword_bamboo_2", new int[]{1701, 1702, 1703, 1704, 1705, 1706, 1707, 1708, 1709, 1710, 1711, 1712});
        register("nichirinsword_cherry_blossom", new int[]{1801, 1802, 1805, 1806});

        add(101, "1st Form: Thunderclap and Flash", "§e");
        add(102, "2nd Form: Rice Spirit", "§e");
        add(103, "3rd Form: Thunder Swarm", "§e");
        add(104, "4th Form: Distant Thunder", "§e");
        add(105, "5th Form: Heat Lightning", "§e");
        add(106, "6th Form: Rumble and Flash", "§e");
        add(107, "7th Form: Honoikazuchi no Kami", "§e");
        add(111, "11th Form", "§e");
        add(112, "12th Form", "§e");
        add(113, "13th Form", "§e");

        add(301, "1st Form: Unknowing Fire", "§4");
        add(302, "2nd Form: Rising Scorching Sun", "§4");
        add(303, "3rd Form: Blazing Universe", "§4");
        add(304, "4th Form: Blooming Flame Undulation", "§4");
        add(305, "5th Form: Flame Tiger", "§4");
        add(309, "9th Form: Rengoku", "§4");

        add(401, "1st Form", "§b");
        add(402, "2nd Form", "§b");
        add(403, "3rd Form", "§b");
        add(404, "4th Form", "§b");
        add(405, "5th Form", "§b");
        add(407, "7th Form", "§b");
        addRange(501, 509, "Wind Breathing", "§2");
        addRange(601, 611, "Water Breathing", "§9");
        addRange(701, 710, "Beast Breathing", "§3");
        addRange(801, 805, "Serpent Breathing", "§5");
        add(901, "Butterfly Dance: Caprice", "§5");
        add(902, "Dance of the Bee Sting: True Flutter", "§5");
        add(904, "Dance of the Dragonfly: Compound Eye Hexagon", "§5");
        add(905, "Dance of the Centipede: Hundred-Legged Zigzag", "§5");
        addRange(1001, 1007, "Senior Breathing", "§e");

        add(1101, "1st Form: Dark Moon - Evening Palace", "§6");
        add(1102, "2nd Form: Pearl Flowers Moongazing", "§6");
        add(1103, "3rd Form: Loathsome Moon - Chains", "§6");
        add(1105, "5th Form: Moon Spirit Calamitous Eddy", "§6");
        add(1106, "6th Form: Perpetual Night, Lonely Moon - Incessant", "§6");
        add(1107, "7th Form: Mirror of Misfortune - Moonlit", "§5");
        add(1108, "8th Form: Moon-Dragon Ringtail", "§5");
        add(1109, "9th Form: Waning Moonswaths", "§5");
        add(1110, "10th Form: Drilling Slashes, Moon Through Bamboo Leaves", "§5");
        add(1114, "14th Form: Catastrophe, Tenman Crescent Moon", "§5");
        add(1116, "16th Form: Moonbow, Half Moon", "§5");
        add(1121, "Transformation Weapon", "§5");

        add(1201, "1st Form: Dance", "§c");
        add(1202, "2nd Form: Clear Blue Sky", "§c");
        add(1203, "3rd Form: Raging Sun", "§c");
        add(1204, "4th Form: Burning Bones, Summer Sun", "§c");
        add(1205, "5th Form: Sunflower Thrust", "§c");
        add(1206, "6th Form: Dragon Sun Halo Head Dance", "§c");
        add(1207, "7th Form: Setting Sun Transformation", "§c");
        add(1208, "8th Form: Solar Heat Haze", "§c");
        add(1209, "9th Form: Beneficent Radiance", "§c");
        add(1210, "10th Form: Fire Wheel", "§c");
        add(1211, "11th Form: Fake Rainbow", "§c");
        add(1212, "12th Form: Flame Dance", "§c");
        add(1213, "13th Form", "§c");
        addRange(1301, 1305, "Sound Breathing", "§6");
        addRange(1401, 1410, "Flower Breathing", "§5");
        addRange(1501, 1506, "Love Breathing", "§d");
        addRange(1601, 1611, "Stone Breathing", "§7");
        addRange(1701, 1712, "Bamboo Breathing", "§a");
        addRange(1801, 1806, "Sakura Breathing", "§d");
    }

    private BaseModSwordFormRegistry() {
    }

    private static void register(String itemPath, int[] forms) {
        ITEM_FORMS.put(itemPath, forms);
    }

    private static void add(int id, String name, String color) {
        FORMS.put(id, new FormDefinition(id, name, color));
    }

    private static void addRange(int start, int end, String style, String color) {
        for (int id = start; id <= end; id++) {
            add(id, style + " " + (id % 100) + "th Form", color);
        }
    }

    public static int[] getFormIds(ItemStack stack) {
        String path = getItemPath(stack);
        int[] forms = ITEM_FORMS.get(path);
        return forms == null ? new int[0] : Arrays.copyOf(forms, forms.length);
    }

    public static boolean isSupported(ItemStack stack) {
        return ITEM_FORMS.containsKey(getItemPath(stack));
    }

    public static int getSelectIndex(ItemStack stack, int formId) {
        for (int id : getFormIds(stack)) {
            if (id == formId) {
                return formId / 100 == 11 && formId == 1121 ? 20 : formId % 100 - 1;
            }
        }
        return -1;
    }

    public static FormDefinition getForm(int formId) {
        return FORMS.get(formId);
    }

    public static String getColor(ItemStack stack, int formId) {
        if (formId == 1121 && "sword_kokushibo_2".equals(getItemPath(stack))) {
            return "§6";
        }
        FormDefinition form = getForm(formId);
        return form == null ? "" : form.color();
    }

    private static String getItemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "kimetsunoyaiba".equals(id.getNamespace()) ? id.getPath() : "";
    }
}
