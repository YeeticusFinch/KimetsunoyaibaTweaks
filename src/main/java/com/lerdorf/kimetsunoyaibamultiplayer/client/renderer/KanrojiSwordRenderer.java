package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.KanrojiSwordModel;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for Kanroji's animated sword.
 *
 * This renderer uses the kanroji_sword.geo.json model and plays animations
 * based on the player/entity state and active animations.
 *
 * In GUI (inventory/hotbar), the default "sheath" animation is displayed.
 * When held in hand, animations are triggered via KanrojiSwordAnimationHandler.
 */
public class KanrojiSwordRenderer extends GeoItemRenderer<NichirinSwordKanrojiAnimated> {
    private static boolean logged = false;
    private ItemDisplayContext currentDisplayContext = null;

    // ResourceLocation for the static GUI model
    private static final ResourceLocation STATIC_MODEL_LOCATION =
        new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "item/kanroji_sword_static");

    public KanrojiSwordRenderer() {
        super(new KanrojiSwordModel());
        if (!logged) {
            Log.info("[KanrojiSwordRenderer] Constructor called");
            logged = true;
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Store the display context so we can check it in actuallyRender
        this.currentDisplayContext = displayContext;

        // For GUI contexts (inventory, hotbar), use the static item model instead of GeckoLib
        if (displayContext == ItemDisplayContext.GUI ||
            displayContext == ItemDisplayContext.FIXED ||
            displayContext == ItemDisplayContext.GROUND) {

            Minecraft mc = Minecraft.getInstance();
            BakedModel staticModel = mc.getModelManager().getModel(STATIC_MODEL_LOCATION);

            if (staticModel != null && staticModel != mc.getModelManager().getMissingModel()) {
                // Render the static model using vanilla item renderer
                mc.getItemRenderer().render(
                    stack,
                    displayContext,
                    false, // leftHand
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    staticModel
                );
                return;
            }
            // Fallback to GeckoLib if static model not found
            Log.debug("[KanrojiSwordRenderer] Static model not found, falling back to GeckoLib");
        }

        // For hand rendering and other contexts, use the animated GeckoLib model
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, NichirinSwordKanrojiAnimated animatable,
                               software.bernie.geckolib.cache.object.BakedGeoModel model,
                               net.minecraft.client.renderer.RenderType renderType,
                               MultiBufferSource bufferSource,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {

        Log.debug("[KanrojiSwordRenderer] actuallyRender() called, context={}", currentDisplayContext);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                           isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
