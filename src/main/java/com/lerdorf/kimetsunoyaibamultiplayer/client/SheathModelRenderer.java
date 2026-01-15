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

	/**
	 * Full transformation offsets for a sheath (translation, rotation, scale)
	 */
	public static class SheathOffsets {
		public final double translateX;
		public final double translateY;
		public final double translateZ;
		public final double rotateX;
		public final double rotateY;
		public final double rotateZ;
		public final double scaleX;
		public final double scaleY;
		public final double scaleZ;

		public SheathOffsets(double translateX, double translateY, double translateZ,
		                    double rotateX, double rotateY, double rotateZ,
		                    double scaleX, double scaleY, double scaleZ) {
			this.translateX = translateX;
			this.translateY = translateY;
			this.translateZ = translateZ;
			this.rotateX = rotateX;
			this.rotateY = rotateY;
			this.rotateZ = rotateZ;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.scaleZ = scaleZ;
		}

		/**
		 * Convenience constructor for uniform scaling
		 */
		public SheathOffsets(double translateX, double translateY, double translateZ,
		                    double rotateX, double rotateY, double rotateZ,
		                    double uniformScale) {
			this(translateX, translateY, translateZ, rotateX, rotateY, rotateZ,
			     uniformScale, uniformScale, uniformScale);
		}
	}

	// Per-sheath scale multipliers (for sheaths that need different sizing)
	// DEPRECATED: Use registerSheathOffsets instead for full control
	private static final Map<Item, Float> sheathScales = new HashMap<>();

	// Per-sheath transformation offsets (translation, rotation, scale)
	private static final Map<Item, SheathOffsets> sheathOffsets = new HashMap<>();

	/**
	 * Registers a custom scale for a specific sheath item
	 * @deprecated Use registerSheathOffsets for full transformation control
	 */
	@Deprecated
	public static void registerSheathScale(Item sheathItem, float scale) {
		sheathScales.put(sheathItem, scale);
	}

	/**
	 * Registers full transformation offsets for a specific sheath item
	 */
	public static void registerSheathOffsets(Item sheathItem, SheathOffsets offsets) {
		sheathOffsets.put(sheathItem, offsets);
	}

	/**
	 * Gets the scale multiplier for a sheath item (defaults to 1.0)
	 * @deprecated Use getSheathOffsets instead
	 */
	@Deprecated
	public static float getSheathScale(Item sheathItem) {
		return sheathScales.getOrDefault(sheathItem, 1.0f);
	}

	/**
	 * Gets the transformation offsets for a sheath item (returns null if not registered)
	 */
	public static SheathOffsets getSheathOffsets(Item sheathItem) {
		return sheathOffsets.get(sheathItem);
	}

	/**
	 * Renders a sheath item at the current pose stack position
	 * Note: Sword scale should be applied by the caller (e.g., SwordDisplayRenderer)
	 * This applies the global sheath scale multiplier and per-sheath transformations
	 */
	public static void renderSheath(Item sheathItem, PoseStack poseStack,
	                               MultiBufferSource buffer, int packedLight, int levelId) {
	    if (sheathItem == null) {
	        return;
	    }

	    try {
	        Minecraft mc = Minecraft.getInstance();
	        ItemStack sheathStack = new ItemStack(sheathItem);

	        boolean hasTransforms = false;
	        SheathOffsets offsets = getSheathOffsets(sheathItem);

	        // Check for new offset system or legacy scale system
	        if (offsets != null) {
	            // Use new transformation offsets
	            hasTransforms = true;
	            poseStack.pushPose();

	            // Apply translation
	            if (offsets.translateX != 0 || offsets.translateY != 0 || offsets.translateZ != 0) {
	                poseStack.translate(offsets.translateX, offsets.translateY, offsets.translateZ);
	            }

	            // Apply rotation (in degrees, converted to radians)
	            if (offsets.rotateX != 0) {
	                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) offsets.rotateX));
	            }
	            if (offsets.rotateY != 0) {
	                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) offsets.rotateY));
	            }
	            if (offsets.rotateZ != 0) {
	                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) offsets.rotateZ));
	            }

	            // Apply scale (per-axis or uniform) * global sheath scale
	            float globalSheathScale = (float) SwordDisplayConfig.sheathScale;
	            poseStack.scale(
	                (float) (offsets.scaleX * globalSheathScale),
	                (float) (offsets.scaleY * globalSheathScale),
	                (float) (offsets.scaleZ * globalSheathScale)
	            );
	        } else {
	            // Legacy: use deprecated scale system
	            float globalSheathScale = (float) SwordDisplayConfig.sheathScale;
	            float perSheathScale = getSheathScale(sheathItem);
	            float combinedSheathScale = globalSheathScale * perSheathScale;

	            if (combinedSheathScale != 1.0f) {
	                hasTransforms = true;
	                poseStack.pushPose();
	                poseStack.scale(combinedSheathScale, combinedSheathScale, combinedSheathScale);
	            }
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

	        if (hasTransforms) {
	            poseStack.popPose();
	        }

	    } catch (Exception e) {
	        System.err.println("Error rendering sheath item " + sheathItem + ": " + e.getMessage());
	    }
	}
}
