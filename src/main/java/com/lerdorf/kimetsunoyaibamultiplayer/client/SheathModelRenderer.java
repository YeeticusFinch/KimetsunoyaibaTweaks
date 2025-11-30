package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.items.SheathItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for rendering sheath models without requiring an actual item
 */
public class SheathModelRenderer {

	// Per-sheath scale multipliers (for sheaths that need different sizing)
	private static final Map<Item, Float> sheathScales = new HashMap<>();

	/**
	 * Registers a custom scale for a specific sheath item
	 */
	public static void registerSheathScale(Item sheathItem, float scale) {
		sheathScales.put(sheathItem, scale);
	}

	/**
	 * Gets the scale multiplier for a sheath item (defaults to 1.0)
	 */
	public static float getSheathScale(Item sheathItem) {
		return sheathScales.getOrDefault(sheathItem, 1.0f);
	}

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

	        // Apply combined scale: global config scale * per-sheath scale
	        float globalScale = (float) SwordDisplayConfig.scale;
	        float perSheathScale = getSheathScale(sheathItem);
	        float combinedScale = globalScale * perSheathScale;

	        if (combinedScale != 1.0f) {
	            poseStack.pushPose();
	            poseStack.scale(combinedScale, combinedScale, combinedScale);
	        }

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

	        if (combinedScale != 1.0f) {
	            poseStack.popPose();
	        }

	    } catch (Exception e) {
	        System.err.println("Error rendering sheath item " + sheathItem + ": " + e.getMessage());
	    }
	}
}
