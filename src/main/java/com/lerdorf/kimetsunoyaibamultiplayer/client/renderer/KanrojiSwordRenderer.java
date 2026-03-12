package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.KanrojiSwordModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
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

        //Log.debug("Display context: " + displayContext.toString());

        // Try to get the entity holding this item from render context
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.entity.LivingEntity renderingEntity = null;

        // First, try to get from EntityRenderContext (set during entity rendering)
        renderingEntity = com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext.getCurrentEntity();

        Log.debug("[KanrojiSwordRenderer] renderByItem() - displayContext: {}, EntityRenderContext.getCurrentEntity(): {}",
                  displayContext, renderingEntity != null ? renderingEntity.getName().getString() : "null");

        // If not rendering an entity, check if it's the player (first-person view)
        if (renderingEntity == null &&
            (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
             displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) {
            if (mc.player != null) {
                renderingEntity = mc.player;
            }
        }

        // Verify the entity is actually holding the Kanroji sword
        if (renderingEntity != null) {
            boolean isHoldingKanrojiSword =
                renderingEntity.getMainHandItem().getItem() instanceof NichirinSwordKanrojiAnimated ||
                renderingEntity.getOffhandItem().getItem() instanceof NichirinSwordKanrojiAnimated;

            if (!isHoldingKanrojiSword) {
                Log.debug("[KanrojiSwordRenderer] Entity {} is not holding Kanroji sword, setting to null",
                          renderingEntity.getName().getString());
                renderingEntity = null;
            } else {
                Log.debug("[KanrojiSwordRenderer] Entity {} IS holding Kanroji sword",
                          renderingEntity.getName().getString());
            }
        }

        //Log.debug("[KanrojiSwordRenderer] renderByItem() - displayContext={}, entity={}",
        //          displayContext, renderingEntity != null ? renderingEntity.getName().getString() : "null");

        // For GUI contexts (inventory, hotbar), use the static item model instead of GeckoLib
        if (displayContext == ItemDisplayContext.GUI ||
            displayContext == ItemDisplayContext.FIXED ||
            displayContext == ItemDisplayContext.GROUND) {

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
            //Log.debug("[KanrojiSwordRenderer] Static model not found, falling back to GeckoLib");
        }

        // Apply translation offset for third-person hand contexts to center the sword.
        if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            //Log.debug("Entering translation offset for third person right hand");
            //Log.debug("RenderingEntity: " + (renderingEntity != null ? renderingEntity.getName().toString() : "null"));
            double handSign = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0 : 1.0;
            poseStack.translate((1.25 * handSign) / 16.0, 0.7 / 16.0, (1.25 * handSign) / 16.0);

            // Only apply Kanroji entity hand offset when the entity is actually holding the sword (in combat)
            // When out of combat, the sword is on the back/hip and shouldn't use hand offsets
            if (renderingEntity instanceof BreathingSlayerEntity &&
                com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker.isInCombat(renderingEntity)) {
                //Log.debug("Entering translation offset for kanroji entity (in combat)");
                // Extra hand offset for Kanroji entity to compensate for its model alignment.
                poseStack.translate((Config.kanrojiEntityHandOffsetX * handSign) / 16.0,
                                   Config.kanrojiEntityHandOffsetY / 16.0,
                                   (Config.kanrojiEntityHandOffsetZ * handSign) / 16.0);
            }
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

        // Entity is already set by renderByItem(), so animation controller can read it
        //Log.debug("[KanrojiSwordRenderer] actuallyRender() called, context={}", currentDisplayContext);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                           isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
