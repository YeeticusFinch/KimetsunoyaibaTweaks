package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityRenderContext;
import com.lerdorf.kimetsunoyaibamultiplayer.client.models.LoveSwordModel;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib renderer for the generic Love Breathing animated sword.
 * In GUI/ground contexts, renders the static item model instead of the animated one.
 */
public class LoveSwordRenderer extends GeoItemRenderer<NichirinSwordLoveAnimated> {

    private static final ResourceLocation STATIC_MODEL_LOCATION =
        new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "item/nichirinsword_love_static");

    public LoveSwordRenderer() {
        super(new LoveSwordModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // For GUI/ground contexts, use the static item model
        if (displayContext == ItemDisplayContext.GUI ||
            displayContext == ItemDisplayContext.FIXED ||
            displayContext == ItemDisplayContext.GROUND) {

            Minecraft mc = Minecraft.getInstance();
            BakedModel staticModel = mc.getModelManager().getModel(STATIC_MODEL_LOCATION);

            if (staticModel != null && staticModel != mc.getModelManager().getMissingModel()) {
                mc.getItemRenderer().render(
                    stack, displayContext, false, poseStack,
                    bufferSource, packedLight, packedOverlay, staticModel);
                return;
            }
        }

        // Apply third-person hand offset for both hand contexts.
        if (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            double handSign = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0 : 1.0;
            poseStack.translate((1.25 * handSign) / 16.0, 0.7 / 16.0, (1.25 * handSign) / 16.0);

            // Apply entity hand offset for GeckoLib entities (BreathingSlayerEntity) in combat
            // Same offset as KanrojiSwordRenderer to compensate for biped model alignment
            LivingEntity renderingEntity = EntityRenderContext.getCurrentEntity();
            if (renderingEntity instanceof BreathingSlayerEntity &&
                EntityCombatStateTracker.isInCombat(renderingEntity)) {
                poseStack.translate((Config.kanrojiEntityHandOffsetX * handSign) / 16.0,
                                   Config.kanrojiEntityHandOffsetY / 16.0,
                                   (Config.kanrojiEntityHandOffsetZ * handSign) / 16.0);
            }
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
