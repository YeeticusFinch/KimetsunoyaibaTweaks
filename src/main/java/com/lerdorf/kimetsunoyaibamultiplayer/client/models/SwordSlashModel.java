package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Model descriptor for sword slash effects Provides resource locations for
 * model, texture, and animation files
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashModel extends GeoModel<SwordSlashRenderState> {

	private final String modelKey;

	/**
	 * Creates a sword slash model for a specific model key
	 * 
	 * @param modelKey The model key (e.g., "mist", "generic")
	 */
	public SwordSlashModel(String modelKey) {
		this.modelKey = modelKey;
	}

	public ResourceLocation getModelResource() {
		return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
				"geo/sword_slash_" + modelKey + ".geo.json");
	}

	public ResourceLocation getTextureResource() {
		return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
				"textures/entity/sword_slash_" + modelKey + ".png");
	}

	public ResourceLocation getAnimationResource() {
		// Optional: If you add animations later
		return ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID,
				"animations/sword_slash.animation.json");
	}

	/**
	 * Gets the model key for this slash model
	 * 
	 * @return The model key
	 */
	public String getModelKey() {
		return modelKey;
	}

	@Override
	public ResourceLocation getModelResource(SwordSlashRenderState animatable) {
		return new ResourceLocation(KimetsunoyaibaMultiplayer.MODID, "geo/sword_slash_" + modelKey + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SwordSlashRenderState animatable) {
		return new ResourceLocation(KimetsunoyaibaMultiplayer.MODID,
				"textures/entity/sword_slash_" + modelKey + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(SwordSlashRenderState animatable) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Renders the baked GeckoLib model to a buffer.
	 */
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int overlay, float red,
			float green, float blue, float alpha) {
		SwordSlashRenderState dummy = new SwordSlashRenderState();

		// Get the baked model from GeckoLib
		BakedGeoModel baked = getBakedModel(getModelResource(dummy));

		// Render the baked geometry to the buffer
		renderBakedModel(baked, poseStack, buffer, packedLight, overlay, red, green, blue, alpha);
	}

	public void renderBakedModel(BakedGeoModel baked, PoseStack poseStack, VertexConsumer buffer, int packedLight,
			int overlay, float red, float green, float blue, float alpha) {
		poseStack.pushPose();

		// For each top-level bone in the model
		for (GeoBone bone : baked.topLevelBones()) {
			renderBone(bone, poseStack, buffer, packedLight, overlay, red, green, blue, alpha);
		}

		poseStack.popPose();
	}

	private void renderBone(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight, int overlay,
			float red, float green, float blue, float alpha) {

		poseStack.pushPose();
		RenderUtils.translateToPivotPoint(poseStack, bone);
		RenderUtils.rotateMatrixAroundBone(poseStack, bone);
		RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

		Matrix4f matrix = poseStack.last().pose();
		Matrix3f normalMatrix = poseStack.last().normal();

// Manually draw each cube
		for (GeoCube cube : bone.getCubes()) {
			renderGeoCube(cube, matrix, normalMatrix, buffer, packedLight, overlay, red, green, blue, alpha);
		}

// Recurse into children
		for (GeoBone child : bone.getChildBones()) {
			renderBone(child, poseStack, buffer, packedLight, overlay, red, green, blue, alpha);
		}

		poseStack.popPose();
	}

	private void renderGeoCube(GeoCube cube, Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer buffer,
			int packedLight, int overlay, float red, float green, float blue, float alpha) {

		for (var quad : cube.quads()) {
			for (var vertex : quad.vertices()) {
				var pos = vertex.position();
				float u = vertex.texU();
				float v = vertex.texV();

				// Use original colors from texture for colored glow effect
				// The emissive render type will make Shimmer/shaders apply bloom
				// No brightness multiplication - preserve original texture colors and transparency

				// FIXED: Use NEW_ENTITY format with all required vertex elements
				// This matches what DualLayerSlashRenderer expects
				buffer.vertex(matrix, pos.x(), pos.y(), pos.z())
						.color(red, green, blue, alpha)  // Use original colors from texture
						.uv(u, v)
						.overlayCoords(overlay)  // RESTORED - required for NEW_ENTITY format
						.uv2(0xF000F0) // Force full bright lighting (no shadows, always bright)
						.normal(normalMatrix, 0f, 1f, 0f)  // RESTORED - required for NEW_ENTITY format
						.endVertex();
			}
		}
	}

}
