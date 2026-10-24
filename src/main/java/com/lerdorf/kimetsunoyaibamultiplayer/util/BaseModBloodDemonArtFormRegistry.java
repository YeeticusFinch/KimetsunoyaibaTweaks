package com.lerdorf.kimetsunoyaibamultiplayer.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Runtime demon-art IDs and item-specific availability for the base mod. */
public final class BaseModBloodDemonArtFormRegistry {
    public record FormDefinition(int id, String name, String color) {
    }

    private static final Map<String, int[]> ITEM_FORMS = new HashMap<>();
    private static final Map<Integer, FormDefinition> FORMS = new HashMap<>();

    static {
        register("handfan", new int[]{100, 101, 102, 103, 104, 105, 106, 107, 108});
        register("blooddemonart_rui", new int[]{200, 201, 202, 203, 204, 205, 206, 207});
        register("blooddemonart_nezuko", new int[]{300});
        register("blooddemonart_akaza", new int[]{400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 420, 421});
        register("chigama", new int[]{500, 501, 502, 520, 550});
        register("blooddemonart_demon_king", new int[]{600, 601, 608, 609, 650, 660});
        register("blooddemonart_gyokko", new int[]{700, 701, 702, 703, 704, 705, 706, 720, 721});
        register("blooddemonart_zohakuten", new int[]{800, 801, 802, 803, 804, 805, 821, 822});
        register("spear", new int[]{800, 801, 802, 803, 804, 805, 821, 822});
        register("urogi_hand", new int[]{800, 801, 802, 803, 804, 805, 821, 822});
        register("khakkhara", new int[]{800, 801, 802, 803, 804, 805, 821, 822});
        register("tengu_handfan", new int[]{800, 801, 802, 803, 804, 805, 821, 822});
        register("blooddemonart_yahaba", new int[]{1000, 1001});
        register("blooddemonart_kamanue", new int[]{1100, 1101, 1102, 1103, 1104});
        register("blooddemonart_enmu", new int[]{1200, 1201, 1202});
        register("rifle", new int[]{1300, 1301, 1302, 1308, 1309, 1320});
        register("blooddemonart_rokuro", new int[]{1400, 1401, 1402, 1403});
        register("clothes_hand_demon_chestplate", new int[]{900});
        register("drum", new int[]{1850});
        register("drum_armor_chestplate", new int[]{1850});

        addRange(100, 108, "Doma Blood Demon Art", "§b");
        addRange(200, 207, "Rui Blood Demon Art", "§5");
        add(300, "Exploding Blood", "§c");
        addRange(400, 409, "Akaza Blood Demon Art", "§4");
        add(420, "Akaza Blood Demon Art 21", "§4");
        add(421, "Akaza Blood Demon Art 22", "§4");
        addRange(500, 502, "Gyutaro Blood Demon Art", "§2");
        add(520, "Gyutaro Blood Demon Art 21", "§2");
        add(550, "Gyutaro Blood Demon Art 51", "§2");
        add(600, "Muzan Blood Demon Art", "§4");
        add(601, "Muzan Blood Demon Art 2", "§4");
        add(608, "Muzan Blood Demon Art 9", "§4");
        add(609, "Muzan Blood Demon Art 10", "§4");
        add(650, "Muzan Blood Demon Art 51", "§4");
        add(660, "Muzan Blood Demon Art 61", "§4");
        addRange(700, 706, "Gyokko Blood Demon Art", "§3");
        add(720, "Gyokko Blood Demon Art 21", "§3");
        add(721, "Gyokko Blood Demon Art 22", "§3");
        addRange(800, 805, "Hantengu Blood Demon Art", "§d");
        add(821, "Zohakuten Blood Demon Art 22", "§d");
        add(822, "Zohakuten Blood Demon Art 23", "§d");
        add(900, "Hand Demon Blood Demon Art", "§8");
        add(1000, "Yahaba Blood Demon Art", "§c");
        add(1001, "Yahaba Blood Demon Art 2", "§c");
        addRange(1100, 1104, "Kamanue Blood Demon Art", "§7");
        addRange(1200, 1202, "Enmu Blood Demon Art", "§9");
        add(1300, "Hairo Blood Demon Art", "§8");
        add(1301, "Hairo Blood Demon Art 2", "§8");
        add(1302, "Hairo Blood Demon Art 3", "§8");
        add(1308, "Hairo Blood Demon Art 9", "§8");
        add(1309, "Hairo Blood Demon Art 10", "§8");
        add(1320, "Hairo Blood Demon Art 21", "§8");
        addRange(1400, 1403, "Rokuro Blood Demon Art", "§7");
        add(1850, "Kyogai Blood Demon Art", "§e");
    }

    private BaseModBloodDemonArtFormRegistry() {
    }

    private static void register(String itemPath, int[] forms) {
        ITEM_FORMS.put(itemPath, forms);
    }

    private static void add(int id, String name, String color) {
        FORMS.put(id, new FormDefinition(id, name, color));
    }

    private static void addRange(int start, int end, String style, String color) {
        for (int id = start; id <= end; id++) {
            add(id, style + " " + (id % 100 + 1), color);
        }
    }

    public static int[] getFormIds(ItemStack stack) {
        int[] forms = ITEM_FORMS.get(getItemPath(stack));
        return forms == null ? new int[0] : Arrays.copyOf(forms, forms.length);
    }

    public static boolean isSupported(ItemStack stack) {
        return ITEM_FORMS.containsKey(getItemPath(stack));
    }

    public static int getSelectIndex(ItemStack stack, int formId) {
        for (int id : getFormIds(stack)) {
            if (id == formId) {
                return formId % 100;
            }
        }
        return -1;
    }

    public static FormDefinition getForm(int formId) {
        return FORMS.get(formId);
    }

    private static String getItemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "kimetsunoyaiba".equals(id.getNamespace()) ? id.getPath() : "";
    }
}
