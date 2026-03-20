package com.lerdorf.kimetsunoyaibamultiplayer.items;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Slayer Uniform armor pieces using slayer_uniform.geo.json.
 */
public class SlayerUniformArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String modelPath;
    private final String chestTexturePath;
    private final String leggingsTexturePath;
    private final String bootsTexturePath;

    public SlayerUniformArmorItem(ArmorMaterial material, Type type, Properties properties) {
        this(material, type, properties,
            "geo/slayer_uniform.geo.json",
            "textures/armor/slayer_uniform_chestplate.png",
            "textures/armor/slayer_uniform_leggings.png",
            "textures/armor/slayer_uniform_boots.png");
    }

    public SlayerUniformArmorItem(ArmorMaterial material, Type type, Properties properties,
                                   String chestTexturePath, String leggingsTexturePath, String bootsTexturePath) {
        this(material, type, properties, "geo/slayer_uniform.geo.json", chestTexturePath, leggingsTexturePath, bootsTexturePath);
    }

    public SlayerUniformArmorItem(ArmorMaterial material, Type type, Properties properties,
                                   String modelPath, String chestTexturePath, String leggingsTexturePath, String bootsTexturePath) {
        super(material, type, properties);
        this.modelPath = modelPath;
        this.chestTexturePath = chestTexturePath;
        this.leggingsTexturePath = leggingsTexturePath;
        this.bootsTexturePath = bootsTexturePath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public String getChestTexturePath() {
        return chestTexturePath;
    }

    public String getLeggingsTexturePath() {
        return leggingsTexturePath;
    }

    public String getBootsTexturePath() {
        return bootsTexturePath;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static armor
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.SlayerUniformArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }
}
