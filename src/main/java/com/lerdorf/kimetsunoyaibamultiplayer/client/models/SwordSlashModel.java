package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordSwingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Model descriptor for sword slash effects Provides resource locations for
 * model, texture, and animation files
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashModel extends GeoModel<SwordSlashRenderState> {

	private static final java.util.Random RANDOM = new java.util.Random();
	private static final float PLANE_EPSILON = 0.0001F;
	private static final Map<ResourceLocation, AlphaMask> ALPHA_MASK_CACHE = new HashMap<>();
	private static final Set<String> CLAW_FACE_BONES = Set.of(
			"bb_main", "bb_main2", "bb_main3", "bb_main4", "bb_main5");

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
        int currentFrame = elapsedTicks / frameDelay;
        if (SwordSlashModelRegistry.holdsLastTextureFrame(modelKey)) {
            return Math.min(currentFrame, frameCount - 1);
        }

        return currentFrame % frameCount;
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

	private boolean isNezukoClawModel() {
		return "claw_nezuko".equals(modelKey);
	}

	private boolean isClawModel() {
		return "claw".equals(modelKey);
	}

	private String getGeometryModelKey() {
		return "moon".equals(modelKey) ? "generic" : modelKey;
	}

	private boolean isWebModel() {
		return "web".equals(modelKey);
	}

	public ResourceLocation getModelResource() {
		if (isNezukoClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/claw_nezuko.geo.json");
		}
		if (isClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/claw.geo.json");
		}
		if (isWebModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/slash_web.geo.json");
		}
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
				"geo/sword_slash_" + getGeometryModelKey() + ".geo.json");
	}

	public ResourceLocation getTextureResource() {
		String namespace = getResourceNamespace();
		if (isNezukoClawModel()) {
			if (frameCount <= 1) {
				return ResourceLocation.fromNamespaceAndPath(namespace,
						"textures/entity/slash_nezuko0.png");
			}
			int frame = getCurrentFrame();
			return ResourceLocation.fromNamespaceAndPath(namespace,
					"textures/entity/slash_nezuko" + frame + ".png");
		}
		if (isClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(namespace,
					"textures/entity/claw.png");
		}
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
		if (isNezukoClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/claw_nezuko.geo.json");
		}
		if (isClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/claw.geo.json");
		}
		if (isWebModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(), "geo/slash_web.geo.json");
		}
		return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
				"geo/sword_slash_" + getGeometryModelKey() + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SwordSlashRenderState animatable) {
		if (isNezukoClawModel()) {
			int frame = getCurrentFrame();
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
					"textures/entity/slash_nezuko" + frame + ".png");
		}
		if (isClawModel()) {
			return ResourceLocation.fromNamespaceAndPath(getResourceNamespace(),
					"textures/entity/claw.png");
		}
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
			float green, float blue, float alpha, ResourceLocation texture) {
		SwordSlashRenderState renderState = new SwordSlashRenderState();

		// Set the animation based on flip flag (for reference, but we apply manually)
		renderState.setAnimation(flipHorizontal ? "reverse" : "base");

		Log.info("[SwordSlashModel] renderToBuffer: modelKey=" + modelKey + ", flipHorizontal=" + flipHorizontal);

		// Get the baked model from GeckoLib
		BakedGeoModel baked = getBakedModel(getModelResource(renderState));

		// Render the baked geometry to the buffer
		// The flip is applied in renderBone() for the bb_main bone
		renderBakedModel(baked, poseStack, buffer, packedLight, overlay, red, green, blue, alpha,
				texture);
	}

	public void renderBakedModel(BakedGeoModel baked, PoseStack poseStack, VertexConsumer buffer, int packedLight,
			int overlay, float red, float green, float blue, float alpha, ResourceLocation texture) {
		poseStack.pushPose();

		// For each top-level bone in the model
		for (GeoBone bone : baked.topLevelBones()) {
			renderBone(bone, poseStack, buffer, packedLight, overlay, red, green, blue, alpha, texture);
		}

		poseStack.popPose();
	}

	private void renderBone(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight, int overlay,
			float red, float green, float blue, float alpha, ResourceLocation texture) {

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
			renderGeoCube(bone.getName(), cube, matrix, normalMatrix, buffer, packedLight, overlay, red, green, blue,
				alpha, texture);
		}

		// Recurse into children
		for (GeoBone child : bone.getChildBones()) {
			renderBone(child, poseStack, buffer, packedLight, overlay, red, green, blue, alpha, texture);
		}

		poseStack.popPose();
	}

	private void renderGeoCube(String boneName, GeoCube cube, Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer buffer,
			int packedLight, int overlay, float red, float green, float blue, float alpha, ResourceLocation texture) {

		if (renderVoxelizedCube(boneName, cube, matrix, normalMatrix, buffer, packedLight, overlay, red, green, blue,
				alpha, texture)) {
			return;
		}

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

	private boolean renderVoxelizedCube(String boneName, GeoCube cube, Matrix4f matrix, Matrix3f normalMatrix,
			VertexConsumer buffer,
			int packedLight, int overlay, float red, float green, float blue, float alpha, ResourceLocation texture) {
		if (!SwordSwingConfig.enableVoxelThickness || SwordSwingConfig.voxelThickness <= PLANE_EPSILON) {
			return false;
		}
		// The claw models are five separate flat faces. Keep every named face on
		// the voxel path, while avoiding accidental voxelization of other bones if
		// the model is expanded later.
		if ((isClawModel() || isNezukoClawModel()) && !CLAW_FACE_BONES.contains(boneName)) {
			return false;
		}

		PlaneMapping mapping = PlaneMapping.create(cube);
		if (mapping == null) {
			return false;
		}

		AlphaMask mask = getAlphaMask(texture);
		if (mask == null) {
			return false;
		}

		int minX = Math.max(0, (int) Math.floor(mapping.minU * mask.width));
		int maxX = Math.min(mask.width - 1, (int) Math.ceil(mapping.maxU * mask.width) - 1);
		int minY = Math.max(0, (int) Math.floor(mapping.minV * mask.height));
		int maxY = Math.min(mask.height - 1, (int) Math.ceil(mapping.maxV * mask.height) - 1);
		if (minX > maxX || minY > maxY) {
			return false;
		}

		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				if (!mask.isOpaque(x, y)) {
					continue;
				}

				float u0 = x / (float) mask.width;
				float u1 = (x + 1) / (float) mask.width;
				float v0 = y / (float) mask.height;
				float v1 = (y + 1) / (float) mask.height;
				float[] p00 = mapping.position(u0, v0);
				float[] p10 = mapping.position(u1, v0);
				float[] p01 = mapping.position(u0, v1);
				float[] p11 = mapping.position(u1, v1);
				float[] center = mapping.position((u0 + u1) * 0.5F, (v0 + v1) * 0.5F);
				float[] pixelUv = {(u0 + u1) * 0.5F, (v0 + v1) * 0.5F};
				float halfThickness = SwordSwingConfig.voxelThickness * 0.5F;
				renderVoxelFace(p00, p10, p11, p01, mapping.planeNormal, mapping.planeNormal, halfThickness, matrix, normalMatrix,
						buffer, packedLight, overlay, red, green, blue, alpha, u0, v0, u1, v1);
				float[] reverseNormal = {-mapping.planeNormal[0], -mapping.planeNormal[1], -mapping.planeNormal[2]};
				renderVoxelFace(p01, p11, p10, p00, reverseNormal, mapping.planeNormal, -halfThickness, matrix, normalMatrix,
						buffer, packedLight, overlay, red, green, blue, alpha, u0, v1, u1, v0);

				if (!mask.isOpaque(x - 1, y)) {
					renderVoxelSide(p00, p01, center, mapping.planeNormal, matrix, normalMatrix, buffer,
							packedLight, overlay, red, green, blue, alpha, pixelUv);
				}
				if (!mask.isOpaque(x + 1, y)) {
					renderVoxelSide(p10, p11, center, mapping.planeNormal, matrix,
							normalMatrix, buffer, packedLight, overlay, red, green, blue, alpha, pixelUv);
				}
				if (!mask.isOpaque(x, y - 1)) {
					renderVoxelSide(p00, p10, center, mapping.planeNormal, matrix, normalMatrix, buffer,
							packedLight, overlay, red, green, blue, alpha, pixelUv);
				}
				if (!mask.isOpaque(x, y + 1)) {
					renderVoxelSide(p01, p11, center, mapping.planeNormal, matrix,
							normalMatrix, buffer, packedLight, overlay, red, green, blue, alpha, pixelUv);
				}
			}
		}
		return true;
	}

	private void renderVoxelFace(float[] p00, float[] p10, float[] p11, float[] p01, float[] planeNormal,
			float[] offsetDirection, float offset, Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer buffer,
			int packedLight, int overlay,
			float red, float green, float blue, float alpha, float u0, float v0, float u1, float v1) {
		float[] p00Offset = offset(p00, offsetDirection, offset);
		float[] p10Offset = offset(p10, offsetDirection, offset);
		float[] p11Offset = offset(p11, offsetDirection, offset);
		float[] p01Offset = offset(p01, offsetDirection, offset);
		putVertex(buffer, matrix, normalMatrix, p00Offset, new float[] {u0, v0}, planeNormal[0], planeNormal[1],
				planeNormal[2], packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, p10Offset, new float[] {u1, v0}, planeNormal[0], planeNormal[1],
				planeNormal[2], packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, p11Offset, new float[] {u1, v1}, planeNormal[0], planeNormal[1],
				planeNormal[2], packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, p01Offset, new float[] {u0, v1}, planeNormal[0], planeNormal[1],
				planeNormal[2], packedLight, overlay, red, green, blue, alpha);
	}

	private void renderVoxelSide(float[] start, float[] end, float[] center, float[] planeNormal,
			Matrix4f matrix, Matrix3f normalMatrix, VertexConsumer buffer, int packedLight, int overlay,
			float red, float green, float blue, float alpha, float[] pixelUv) {
		float[] edgeCenter = {(start[0] + end[0]) * 0.5F, (start[1] + end[1]) * 0.5F,
				(start[2] + end[2]) * 0.5F};
		float normalX = edgeCenter[0] - center[0];
		float normalY = edgeCenter[1] - center[1];
		float normalZ = edgeCenter[2] - center[2];
		float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
		if (length < PLANE_EPSILON) {
			return;
		}
		normalX /= length;
		normalY /= length;
		normalZ /= length;

		float halfThickness = SwordSwingConfig.voxelThickness * 0.5F;
		float[] startNear = offset(start, planeNormal, -halfThickness);
		float[] endNear = offset(end, planeNormal, -halfThickness);
		float[] endFar = offset(end, planeNormal, halfThickness);
		float[] startFar = offset(start, planeNormal, halfThickness);
		putVertex(buffer, matrix, normalMatrix, startNear, pixelUv, normalX, normalY, normalZ,
				packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, endNear, pixelUv, normalX, normalY, normalZ,
				packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, endFar, pixelUv, normalX, normalY, normalZ,
				packedLight, overlay, red, green, blue, alpha);
		putVertex(buffer, matrix, normalMatrix, startFar, pixelUv, normalX, normalY, normalZ,
				packedLight, overlay, red, green, blue, alpha);
	}

	private static float[] offset(float[] point, float[] direction, float amount) {
		return new float[] {point[0] + direction[0] * amount, point[1] + direction[1] * amount,
				point[2] + direction[2] * amount};
	}

	private static void putVertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float[] position,
			float[] uv, float normalX, float normalY, float normalZ, int packedLight, int overlay,
			float red, float green, float blue, float alpha) {
		buffer.vertex(matrix, position[0], position[1], position[2])
				.color(red, green, blue, alpha)
				.uv(uv[0], uv[1])
				.overlayCoords(overlay)
				.uv2(0xF000F0)
				.normal(normalMatrix, normalX, normalY, normalZ)
				.endVertex();
	}

	private static AlphaMask getAlphaMask(ResourceLocation texture) {
		if (ALPHA_MASK_CACHE.containsKey(texture)) {
			return ALPHA_MASK_CACHE.get(texture);
		}

		AlphaMask mask = null;
		try {
			Resource resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
			if (resource != null) {
				try (InputStream input = resource.open()) {
					NativeImage image = NativeImage.read(input);
					try {
						boolean[][] opaque = new boolean[image.getHeight()][image.getWidth()];
						for (int y = 0; y < image.getHeight(); y++) {
							for (int x = 0; x < image.getWidth(); x++) {
								opaque[y][x] = (image.getPixelRGBA(x, y) >>> 24) != 0;
							}
						}
						mask = new AlphaMask(image.getWidth(), image.getHeight(), opaque);
					} finally {
						image.close();
					}
				}
			}
		} catch (Exception e) {
			Log.warn("Could not load slash texture alpha mask for " + texture + ": " + e.getMessage());
		}

		ALPHA_MASK_CACHE.put(texture, mask);
		return mask;
	}

	private static final class AlphaMask {
		private final int width;
		private final int height;
		private final boolean[][] opaque;

		private AlphaMask(int width, int height, boolean[][] opaque) {
			this.width = width;
			this.height = height;
			this.opaque = opaque;
		}

		private boolean isOpaque(int x, int y) {
			return x >= 0 && x < width && y >= 0 && y < height && opaque[y][x];
		}
	}

	private static final class PlaneMapping {
		private final int firstAxis;
		private final int secondAxis;
		private final float minFirst;
		private final float minSecond;
		private final float planeCoordinate;
		private final float firstSize;
		private final float secondSize;
		private final float u00;
		private final float v00;
		private final float duFirst;
		private final float duSecond;
		private final float dvFirst;
		private final float dvSecond;
		private final float[] planeNormal;
		private final float minU;
		private final float maxU;
		private final float minV;
		private final float maxV;

		private PlaneMapping(int firstAxis, int secondAxis, float minFirst, float minSecond, float planeCoordinate,
				float firstSize, float secondSize, float u00, float v00, float duFirst, float duSecond,
				float dvFirst, float dvSecond, float[] planeNormal, float minU, float maxU, float minV, float maxV) {
			this.firstAxis = firstAxis;
			this.secondAxis = secondAxis;
			this.minFirst = minFirst;
			this.minSecond = minSecond;
			this.planeCoordinate = planeCoordinate;
			this.firstSize = firstSize;
			this.secondSize = secondSize;
			this.u00 = u00;
			this.v00 = v00;
			this.duFirst = duFirst;
			this.duSecond = duSecond;
			this.dvFirst = dvFirst;
			this.dvSecond = dvSecond;
			this.planeNormal = planeNormal;
			this.minU = minU;
			this.maxU = maxU;
			this.minV = minV;
			this.maxV = maxV;
		}

		private float[] position(float u, float v) {
			float determinant = duFirst * dvSecond - duSecond * dvFirst;
			float du = u - u00;
			float dv = v - v00;
			float first = (du * dvSecond - duSecond * dv) / determinant;
			float second = (duFirst * dv - du * dvFirst) / determinant;
			float[] result = new float[3];
			result[firstAxis] = minFirst + first * firstSize;
			result[secondAxis] = minSecond + second * secondSize;
			result[3 - firstAxis - secondAxis] = planeCoordinate;
			return result;
		}

		private static PlaneMapping create(GeoCube cube) {
			GeoQuad bestQuad = null;
			float bestArea = 0.0F;
			for (GeoQuad quad : cube.quads()) {
				float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
				float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
				for (var vertex : quad.vertices()) {
					var position = vertex.position();
					min[0] = Math.min(min[0], position.x());
					min[1] = Math.min(min[1], position.y());
					min[2] = Math.min(min[2], position.z());
					max[0] = Math.max(max[0], position.x());
					max[1] = Math.max(max[1], position.y());
					max[2] = Math.max(max[2], position.z());
				}
				float spanX = max[0] - min[0];
				float spanY = max[1] - min[1];
				float spanZ = max[2] - min[2];
				float area = Math.max(spanX * spanY, Math.max(spanX * spanZ, spanY * spanZ));
				float minU = Float.MAX_VALUE;
				float maxU = -Float.MAX_VALUE;
				float minV = Float.MAX_VALUE;
				float maxV = -Float.MAX_VALUE;
				for (var vertex : quad.vertices()) {
					minU = Math.min(minU, vertex.texU());
					maxU = Math.max(maxU, vertex.texU());
					minV = Math.min(minV, vertex.texV());
					maxV = Math.max(maxV, vertex.texV());
				}
				// Zero-thickness slash cubes often store their usable UVs on the
				// geometrically degenerate up/down face.
				if (area > bestArea && (maxU - minU) * (maxV - minV) > PLANE_EPSILON) {
					bestArea = area;
					bestQuad = quad;
				}
			}

			if (bestQuad == null || bestArea <= PLANE_EPSILON) {
				return null;
			}

			float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
			float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
			for (var vertex : bestQuad.vertices()) {
				var position = vertex.position();
				min[0] = Math.min(min[0], position.x());
				min[1] = Math.min(min[1], position.y());
				min[2] = Math.min(min[2], position.z());
				max[0] = Math.max(max[0], position.x());
				max[1] = Math.max(max[1], position.y());
				max[2] = Math.max(max[2], position.z());
			}
			int normalAxis = 0;
			if (max[1] - min[1] < max[normalAxis] - min[normalAxis]) normalAxis = 1;
			if (max[2] - min[2] < max[normalAxis] - min[normalAxis]) normalAxis = 2;
			if (max[normalAxis] - min[normalAxis] > PLANE_EPSILON) return null;

			int firstAxis = normalAxis == 0 ? 1 : 0;
			int secondAxis = normalAxis == 2 ? 1 : 2;
			var vertices = bestQuad.vertices();
			var p00 = corner(vertices, min[firstAxis], min[secondAxis], firstAxis, secondAxis);
			var p10 = corner(vertices, max[firstAxis], min[secondAxis], firstAxis, secondAxis);
			var p01 = corner(vertices, min[firstAxis], max[secondAxis], firstAxis, secondAxis);
			if (p00 == null || p10 == null || p01 == null) return null;

			float firstSize = max[firstAxis] - min[firstAxis];
			float secondSize = max[secondAxis] - min[secondAxis];
			float duFirst = p10.texU() - p00.texU();
			float duSecond = p01.texU() - p00.texU();
			float dvFirst = p10.texV() - p00.texV();
			float dvSecond = p01.texV() - p00.texV();
			if (Math.abs(duFirst * dvSecond - duSecond * dvFirst) < PLANE_EPSILON) return null;

			float[] planeNormal = {0.0F, 0.0F, 0.0F};
			planeNormal[normalAxis] = 1.0F;
			return new PlaneMapping(firstAxis, secondAxis, min[firstAxis], min[secondAxis], min[normalAxis], firstSize,
					secondSize, p00.texU(), p00.texV(), duFirst, duSecond, dvFirst, dvSecond, planeNormal,
					Math.min(p00.texU(), Math.min(p10.texU(), p01.texU())),
					Math.max(p00.texU(), Math.max(p10.texU(), p01.texU())),
					Math.min(p00.texV(), Math.min(p10.texV(), p01.texV())),
					Math.max(p00.texV(), Math.max(p10.texV(), p01.texV())));
		}

		private static software.bernie.geckolib.cache.object.GeoVertex corner(
				software.bernie.geckolib.cache.object.GeoVertex[] vertices, float first, float second,
				int firstAxis, int secondAxis) {
			for (var vertex : vertices) {
				var position = vertex.position();
				float valueFirst = firstAxis == 0 ? position.x() : firstAxis == 1 ? position.y() : position.z();
				float valueSecond = secondAxis == 0 ? position.x() : secondAxis == 1 ? position.y() : position.z();
				if (Math.abs(valueFirst - first) < PLANE_EPSILON && Math.abs(valueSecond - second) < PLANE_EPSILON) {
					return vertex;
				}
			}
			return null;
		}
	}

	public static void clearAlphaMaskCache() {
		ALPHA_MASK_CACHE.clear();
	}

}
