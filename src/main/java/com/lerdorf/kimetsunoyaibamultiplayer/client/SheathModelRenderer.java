package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Helper class for rendering sheath models without requiring an actual item
 */
public class SheathModelRenderer {

	/**
	 * Renders a sheath item at the current pose stack position
	 */
	public static void renderSheath(Item sheathItem, PoseStack poseStack,
	                               MultiBufferSource buffer, int packedLight, int levelId) {
	    if (sheathItem == null) {
	        return;
	    }

	    try {
	        Minecraft mc = Minecraft.getInstance();
	        ItemStack sheathStack = new ItemStack(sheathItem);

	        // Render the sheath item
	        mc.getItemRenderer().renderStatic(
	            sheathStack,
	            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
	            packedLight,
	            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
	            poseStack,
	            buffer,
	            mc.level,
	            levelId
	        );

	    } catch (Exception e) {
	        System.err.println("Error rendering sheath item " + sheathItem + ": " + e.getMessage());
	    }
	}
}
