package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
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

	private static final java.util.Random RANDOM = new java.util.Random();

	private final String modelKey;
	private int frameCount = 1; // Default to 1 frame (static texture)
	private float currentProgress = 0.0f;
	private boolean flipHorizontal = false; // Default to no flip (base animation)
	private long startTimeMillis = 0; // Time when rendering started (for tick-based frame calculation)
	private int duration = 0; // Duration in milliseconds
	private final int randomFrame; // Fixed random frame for models using random texture selection
	private final boolean usesRandomSelection; // Whether this model uses random texture selection

	/**
	 * Creates a sword slash model for a specific model key
	 *
	 * @param modelKey The model key (e.g., "mist", "generic")
	 */
	public SwordSlashModel(String modelKey) {
		this.modelKey = modelKey;
		this.usesRandomSelection = SwordSlashModelRegistry.usesRandomTextureSelection(modelKey);
		// Pre-select random frame at construction time (only used if usesRandomSelection is true)
		int registeredFrameCount = SwordSlashModelRegistry.getFrameCount(modelKey);
		this.randomFrame = registeredFrameCount > 1 ? RANDOM.nextInt(registeredFrameCount) : 0;
	}

	/**
	 * Sets the number of animation frames for this model
	 *
	 * @param frameCount Number of frames (1 for static, >1 for animated)
	 */
	public void setFrameCount(int frameCount) {
		this.frameCount = Math.max(1, frameCount);
	}

	/**
	 * Gets the frame count for this model
	 */
	public int getFrameCount() {
		return frameCount;
	}

	/**
	 * Sets the current animation progress
	 *
	 * @param progress Animation progress from 0.0 to 1.0
	 */
	public void setProgress(float progress) {
		this.currentProgress = progress;
	}

	/**
	 * Sets the timing information for tick-based frame calculation
	 * @param startTimeMillis Start time in milliseconds
	 * @param durationMillis Duration in milliseconds
	 */
	public void setTiming(long startTimeMillis, int durationMillis) {
		this.startTimeMillis = startTimeMillis;
		this.duration = durationMillis;
	}

	/**
	 * Sets whether to flip the model horizontally using the "reverse" animation
	 *
	 * @param flip true to use "reverse" animation (flip), false to use "base" animation (no flip)
	 */
	public void setFlipHorizontal(boolean flip) {
		this.flipHorizontal = flip;
	}

	/**
	 * Gets whether the model is flipped horizontally
	 */
	public boolean isFlippedHorizontal() {
		return this.flipHorizontal;
	}

	/**
	 * Gets the current frame number based on elapsed ticks and frame delay,
	 * or returns the fixed random frame if using random texture selection.
	 */
	public int getCurrentFrame() {
		if (frameCount <= 1) {
			return 0;
		}

		// If using random texture selection, return the pre-selected random frame
		if (usesRandomSelection) {
			return randomFrame;
		}

		// Get frame delay from registry (ticks per frame)
		int frameDelay = SwordSlashModelRegistry.getFrameDelay(modelKey);

		// Calculate elapsed time
		long elapsedMillis = System.currentTimeMillis() - startTimeMillis;

		// Convert to ticks (1 tick = 50ms)
		int elapsedTicks = (int)(elapsedMillis / 50);

		// Calculate current frame based on frame delay
		// Each frame stays visible for frameDelay ticks
		int currentFrame = (elapsedTicks / frameDelay) % frameCount;

		return currentFrame;
	}

	/**
	 * Gets the randomly selected frame (only meaningful if usesRandomSelection is true)
	 */
	public int getRandomFrame() {
		return randomFrame;
	}

	/**
	 * Checks if this model uses random texture selection
	 */
	public boolean usesRandomTextureSelection() {
		return usesRandomSelection;
	}

	private String getResourceNamespace() {
		return SwordSlashModelRegistry.getNamespaceForModelKey(modelKey);
	}

	public ResourceLocation getModelResource() {
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
				"geo/sword_slash_" + modelKey + ".geo.json");
	}

	public ResourceLocation getTextureResource() {
		String namespace = getResourceNamespace();
		if (frameCount <= 1) {
			// Static texture
			return ResourceLocation.fromNamespaceAndPath(namespace,
					"textures/entity/sword_slash_" + modelKey + ".png");
		} else {
			// Animated texture - return frame based on progress
			int frame = getCurrentFrame();
			return ResourceLocation.fromNamespaceAndPath(namespace,
					"textures/entity/sword_slash_" + modelKey + frame + ".png");
		}
	}

	public ResourceLocation getAnimationResource() {
		// Optional: If you add animations later
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
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
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
				"geo/sword_slash_" + modelKey + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SwordSlashRenderState animatable) {
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
				"textures/entity/sword_slash_" + modelKey + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(SwordSlashRenderState animatable) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Renders the baked GeckoLib model to a buffer with manual flip transformation.
	 */
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int overlay, float red,
			float green, float blue, float alpha) {
		SwordSlashRenderState renderState = new SwordSlashRenderState();

		// Set the animation based on flip flag (for reference, but we apply manually)
		renderState.setAnimation(flipHorizontal ? "reverse" : "base");

		Log.info("[SwordSlashModel] renderToBuffer: modelKey=" + modelKey + ", flipHorizontal=" + flipHorizontal);

		// Get the baked model from GeckoLib
		BakedGeoModel baked = getBakedModel(getModelResource(renderState));

		// Render the baked geometry to the buffer
		// The flip is applied in renderBone() for the bb_main bone
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

		// Apply horizontal flip for bb_main bone when flipHorizontal is set
		// This is done by applying the scale BEFORE bone transforms to affect everything
		if (flipHorizontal) {
			if (bone.getName().equals("bb_main")) {
				Log.info("[SwordSlashModel] FLIPPING bone: " + bone.getName() + " for modelKey: " + modelKey);
				poseStack.scale(-1.0f, 1.0f, 1.0f);
			} else {
				Log.info("[SwordSlashModel] Skipping flip for bone: " + bone.getName() + " (not bb_main)");
			}
		}

		RenderUtils.translateToPivotPoint(poseStack, bone);
		RenderUtils.rotateMatrixAroundBone(poseStack, bone);
		RenderUtils.scaleMatrixForBone(poseStack, bone);  // Apply bone scale from model
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
