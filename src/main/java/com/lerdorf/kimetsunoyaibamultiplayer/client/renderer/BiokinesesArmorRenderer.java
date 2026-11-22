package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BiokinesesArmorRenderItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Renders Biokineses model 2 as temporary armor using an internal registered animatable item. */
public final class BiokinesesArmorRenderer extends GeoArmorRenderer<BiokinesesArmorRenderItem> {
    private static final BiokinesesArmorRenderItem RENDER_ITEM = ModItems.BIOKINESES_ARMOR_RENDER_ITEM.get();

    public BiokinesesArmorRenderer() {
        super(new Model());
    }

    /** Prepares the armor renderer with the held Biokineses stack. */
    public void prepForHeldItem(Entity entity, ItemStack stack, EquipmentSlot slot,
                                HumanoidModel<?> baseModel) {
        this.baseModel = baseModel;
        this.currentEntity = entity;
        this.currentStack = stack;
        this.animatable = RENDER_ITEM;
        this.currentSlot = slot;
    }

    @Override
    public long getInstanceId(BiokinesesArmorRenderItem animatable) {
        int entityId = this.currentEntity == null ? 0 : this.currentEntity.getId();
        int slotId = this.currentSlot == null ? 0 : this.currentSlot.ordinal();
        return ((long) entityId << 3) ^ slotId;
    }

    private static final class Model extends GeoModel<BiokinesesArmorRenderItem> {
        @Override
        public ResourceLocation getModelResource(BiokinesesArmorRenderItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "geo/biokineses_armor.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(BiokinesesArmorRenderItem item) {
            return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
                "textures/armor/biokineses_armor.png");
        }

        @Override
        public ResourceLocation getAnimationResource(BiokinesesArmorRenderItem item) {
            return null;
        }
    }
}
