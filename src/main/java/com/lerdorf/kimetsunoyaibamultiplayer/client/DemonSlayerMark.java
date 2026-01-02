package com.lerdorf.kimetsunoyaibamultiplayer.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a demon slayer mark that can be attached to an entity's bone.
 * Each mark has a specific bone attachment point, model resource, and transform offsets.
 */
public class DemonSlayerMark {
    private final String boneName;
    private final ResourceLocation modelLocation;
    private final Vec3 positionOffset;
    private final Vec3 rotationOffset;
    private final Vec3 scaleOffset;

    /**
     * Creates a new demon slayer mark definition.
     *
     * @param boneName The bone to attach this mark to (e.g., "head", "body", "torso")
     * @param modelLocation The resource location of the item model to use (e.g., "kimetsunoyaibamultiplayer:demonslayermark_love")
     * @param positionOffset Position offset from the bone (X, Y, Z)
     * @param rotationOffset Rotation offset in degrees (pitch, yaw, roll)
     * @param scaleOffset Scale multiplier (X, Y, Z) - use Vec3(1, 1, 1) for default scale
     */
    public DemonSlayerMark(String boneName, ResourceLocation modelLocation, Vec3 positionOffset, Vec3 rotationOffset, Vec3 scaleOffset) {
        this.boneName = boneName;
        this.modelLocation = modelLocation;
        this.positionOffset = positionOffset;
        this.rotationOffset = rotationOffset;
        this.scaleOffset = scaleOffset;
    }

    /**
     * Gets the bone this mark should attach to.
     */
    public String getBoneName() {
        return boneName;
    }

    /**
     * Gets the model resource location for this mark.
     */
    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    /**
     * Gets the position offset from the bone origin.
     */
    public Vec3 getPositionOffset() {
        return positionOffset;
    }

    /**
     * Gets the rotation offset in degrees (pitch, yaw, roll).
     */
    public Vec3 getRotationOffset() {
        return rotationOffset;
    }

    /**
     * Gets the scale offset multiplier.
     */
    public Vec3 getScaleOffset() {
        return scaleOffset;
    }

    /**
     * Checks if this mark should be rendered on the given entity.
     * Override this in subclasses to add custom logic.
     */
    public boolean shouldRender(LivingEntity entity) {
        return true;
    }
}
