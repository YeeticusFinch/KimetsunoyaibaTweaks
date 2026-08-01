package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, KimetsunoyaibaMultiplayer.MODID);

    public static final RegistryObject<MenuType<SwordRackMenu>> SWORD_RACK =
        MENUS.register("sword_rack", () -> IForgeMenuType.create(SwordRackMenu::new));

    public static final RegistryObject<MenuType<GravityBlockMenu>> GRAVITY_BLOCK =
        MENUS.register("gravity_block", () -> IForgeMenuType.create(GravityBlockMenu::new));

    public static final RegistryObject<MenuType<BridgerBlockMenu>> BRIDGER_BLOCK =
        MENUS.register("bridger_block", () -> IForgeMenuType.create(BridgerBlockMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
