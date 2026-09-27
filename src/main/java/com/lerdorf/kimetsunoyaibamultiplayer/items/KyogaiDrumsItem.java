package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.KyogaiDrumsArt;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Kyogai's drums are a held cosmetic item rather than wearable armor.
 * The client render layers use the item stack to render the attached drum model.
 */
public class KyogaiDrumsItem extends BloodDemonArtItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KyogaiDrumsItem(Properties properties) {
        super(KyogaiDrumsArt.ART_ID, properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The armor model is static.
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
                    this.renderer = new com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.KyogaiDrumsArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }
}
