package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Armor rendering layer for GeckoLib entities using biped.geo.json
 * Renders vanilla armor models attached to the armor bones in the GeckoLib model
 */
public class GeoArmorLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final HumanoidModel<LivingEntity> innerArmorModel;
    private final HumanoidModel<LivingEntity> outerArmorModel;

    public GeoArmorLayer(GeoRenderer<T> renderer) {
        super(renderer);
        // Create vanilla humanoid armor models - same models used for player armor
        this.innerArmorModel = new HumanoidModel<>(Minecraft.getInstance()
            .getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outerArmorModel = new HumanoidModel<>(Minecraft.getInstance()
            .getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                      RenderType renderType, MultiBufferSource bufferSource,
                      VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        // Only render armor on LivingEntity instances
        if (!(animatable instanceof LivingEntity entity)) {
            return;
        }

        // Render each armor slot using vanilla armor models
        renderArmorSlot(poseStack, bufferSource, bakedModel, entity, EquipmentSlot.HEAD, packedLight, partialTick);
        renderArmorSlot(poseStack, bufferSource, bakedModel, entity, EquipmentSlot.CHEST, packedLight, partialTick);
        renderArmorSlot(poseStack, bufferSource, bakedModel, entity, EquipmentSlot.LEGS, packedLight, partialTick);
        renderArmorSlot(poseStack, bufferSource, bakedModel, entity, EquipmentSlot.FEET, packedLight, partialTick);
    }

    private void renderArmorSlot(PoseStack poseStack, MultiBufferSource bufferSource, BakedGeoModel bakedModel,
                                 LivingEntity entity, EquipmentSlot slot, int packedLight, float partialTick) {
        ItemStack itemstack = entity.getItemBySlot(slot);
        if (itemstack.isEmpty() || !(itemstack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        // Choose the correct model (inner for helmet/legs, outer for chest/boots)
        HumanoidModel<LivingEntity> model = slot == EquipmentSlot.LEGS ? this.innerArmorModel : this.outerArmorModel;

        // Get the armor texture
        ResourceLocation armorTexture = getArmorTexture(armorItem, slot);
        if (armorTexture == null) {
            return;
        }

        // Set up the armor model to match the entity's pose
        model.prepareMobModel(entity, 0, 0, partialTick);
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);

        // Render the armor model with the armor texture
        RenderType armorRenderType = RenderType.armorCutoutNoCull(armorTexture);
        VertexConsumer armorBuffer = ItemRenderer.getArmorFoilBuffer(bufferSource, armorRenderType, false, itemstack.hasFoil());

        // Render each armor piece at its corresponding bone
        switch (slot) {
            case HEAD:
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.head, "armorHead");
                break;
            case CHEST:
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.body, "armorBody");
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.rightArm, "armorRightArm");
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.leftArm, "armorLeftArm");
                break;
            case LEGS:
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.body, "armorBody");
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.rightLeg, "armorRightLeg");
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.leftLeg, "armorLeftLeg");
                break;
            case FEET:
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.rightLeg, "armorRightBoot");
                renderArmorPart(poseStack, bakedModel, armorBuffer, packedLight, model.leftLeg, "armorLeftBoot");
                break;
        }
    }

    private void renderArmorPart(PoseStack poseStack, BakedGeoModel bakedModel, VertexConsumer buffer,
                                 int packedLight, net.minecraft.client.model.geom.ModelPart modelPart, String boneName) {
        // Get the GeckoLib bone
        GeoBone bone = bakedModel.getBone(boneName).orElse(null);
        if (bone == null || bone.isHidden()) {
            return;
        }

        poseStack.pushPose();

        // Apply the bone's transformation matrix to position the armor part correctly
        // GeckoLib stores bone transformations in the bone's modelSpaceMatrix
        poseStack.mulPoseMatrix(bone.getModelSpaceMatrix());

        // Render this specific armor model part
        modelPart.visible = true;
        modelPart.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        modelPart.visible = false;

        poseStack.popPose();
    }

    private ResourceLocation getArmorTexture(ArmorItem armorItem, EquipmentSlot slot) {
        // Determine armor layer (1 for helmet/chestplate/boots, 2 for leggings)
        String layer = (slot == EquipmentSlot.LEGS) ? "layer_2" : "layer_1";

        // Get the armor item's registry location to derive the texture path
        ResourceLocation itemLocation = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(armorItem);

        if (itemLocation != null) {
            // Extract material name from the item name
            // Examples:
            // - "minecraft:iron_helmet" -> "iron"
            // - "kimetsunoyaiba:uniform_helmet" -> "uniform"
            String itemPath = itemLocation.getPath();
            String materialName = itemPath
                .replaceAll("_(helmet|chestplate|leggings|boots)$", "")
                .replaceAll("_armor$", "");

            // Construct texture path: namespace:textures/models/armor/{material}_layer_{1|2}.png
            return ResourceLocation.fromNamespaceAndPath(
                itemLocation.getNamespace(),
                "textures/models/armor/" + materialName + "_" + layer + ".png"
            );
        }

        return null;
    }
}
