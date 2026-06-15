package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAlchemyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<MenuType<MicroscopeMenu>> MICROSCOPE =
        MENUS.register("microscope", () -> IForgeMenuType.create(MicroscopeMenu::new));

    public static final RegistryObject<MenuType<AlchemyTableMenu>> ALCHEMY_TABLE =
        MENUS.register("alchemy_table", () -> IForgeMenuType.create(AlchemyTableMenu::new));

    public static final RegistryObject<MenuType<VialRackMenu>> VIAL_RACK =
        MENUS.register("vial_rack", () -> IForgeMenuType.create(VialRackMenu::new));

    private ModAlchemyMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
