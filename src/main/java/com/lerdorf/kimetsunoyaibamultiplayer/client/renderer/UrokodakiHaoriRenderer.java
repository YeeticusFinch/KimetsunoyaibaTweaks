package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.items.UrokodakiHaoriItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * GeckoLib armor renderer for haori_urokodaki.
 */
public class UrokodakiHaoriRenderer extends GeoArmorRenderer<UrokodakiHaoriItem> {
    public UrokodakiHaoriRenderer() {
        super(new Model());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        // Urokodaki's closed haori model includes leg cloth on armorLeftLeg/armorRightLeg.
        // Keep those bones visible when this item renders in the chest slot.
        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.rightLeg, true);
            setBoneVisible(this.leftLeg, true);
        }
    }

    private static class Model extends GeoModel<UrokodakiHaoriItem> {
        @Override
        public ResourceLocation getModelResource(UrokodakiHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "geo/haori_closed.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(UrokodakiHaoriItem item) {
            return new ResourceLocation("kimetsunoyaibamultiplayer", "textures/armor/haori_urokodaki.png");
        }

        @Override
        public ResourceLocation getAnimationResource(UrokodakiHaoriItem item) {
            return null;
        }
    }
}
