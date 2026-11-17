package com.lerdorf.kimetsunoyaibamultiplayer.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Default transient storage for Biokineses arm state. */
public class BiokinesesArmData implements IBiokinesesArmData {
    private boolean active;
    private boolean climbing;
    private boolean retracting;
    private long expiresAt;
    private int allocationMode = ALLOCATION_NORMAL;
    private final Vec3[] targets = new Vec3[ARM_COUNT];
    private final float[] progress = new float[ARM_COUNT];
    private final int[] modes = new int[ARM_COUNT];
    private final int[] heldEntityIds = new int[ARM_COUNT];
    private final long[] attackUntil = new long[ARM_COUNT];
    private final long[] attackCooldownUntil = new long[ARM_COUNT];
    private final int[] targetEntityIds = new int[ARM_COUNT];
    private final long[] anchorBlockPositions = new long[ARM_COUNT];
    private final int[] anchorFaces = new int[ARM_COUNT];
    private boolean locomotionActive;
    private boolean noGravityBeforeLocomotion;
    private float traversalForwardInput;
    private float traversalStrafeInput;
    private boolean traversalJumping;
    private boolean traversalDescending;
    private long traversalInputTick = Long.MIN_VALUE;
    private int airborneTicks;

    public BiokinesesArmData() {
        for (int i = 0; i < ARM_COUNT; i++) {
            targets[i] = Vec3.ZERO;
            heldEntityIds[i] = -1;
            targetEntityIds[i] = -1;
            anchorFaces[i] = -1;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isClimbing() {
        return climbing;
    }

    @Override
    public void setClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    @Override
    public boolean isRetracting() {
        return retracting;
    }

    @Override
    public void setRetracting(boolean retracting) {
        this.retracting = retracting;
    }

    @Override
    public long getExpiresAt() {
        return expiresAt;
    }

    @Override
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public int getAllocationMode() {
        return allocationMode;
    }

    @Override
    public void setAllocationMode(int allocationMode) {
        this.allocationMode = allocationMode;
    }

    @Override
    public Vec3 getTarget(int arm) {
        return validArm(arm) ? targets[arm] : Vec3.ZERO;
    }

    @Override
    public void setTarget(int arm, Vec3 target) {
        if (validArm(arm) && target != null) {
            targets[arm] = target;
        }
    }

    @Override
    public float getProgress(int arm) {
        return validArm(arm) ? progress[arm] : 0.0F;
    }

    @Override
    public void setProgress(int arm, float progress) {
        if (validArm(arm)) {
            this.progress[arm] = clamp(progress, 0.0F, 1.0F);
        }
    }

    @Override
    public int getMode(int arm) {
        return validArm(arm) ? modes[arm] : MODE_IDLE;
    }

    @Override
    public void setMode(int arm, int mode) {
        if (validArm(arm)) {
            modes[arm] = mode;
        }
    }

    @Override
    public int getHeldEntityId(int arm) {
        return validArm(arm) ? heldEntityIds[arm] : -1;
    }

    @Override
    public void setHeldEntityId(int arm, int entityId) {
        if (validArm(arm)) {
            heldEntityIds[arm] = entityId;
        }
    }

    @Override
    public long getAttackUntil(int arm) {
        return validArm(arm) ? attackUntil[arm] : 0L;
    }

    @Override
    public void setAttackUntil(int arm, long tick) {
        if (validArm(arm)) {
            attackUntil[arm] = tick;
        }
    }

    @Override
    public long getAttackCooldownUntil(int arm) {
        return validArm(arm) ? attackCooldownUntil[arm] : 0L;
    }

    @Override
    public void setAttackCooldownUntil(int arm, long tick) {
        if (validArm(arm)) {
            attackCooldownUntil[arm] = tick;
        }
    }

    @Override
    public int getTargetEntityId(int arm) {
        return validArm(arm) ? targetEntityIds[arm] : -1;
    }

    @Override
    public void setTargetEntityId(int arm, int entityId) {
        if (validArm(arm)) {
            targetEntityIds[arm] = entityId;
        }
    }

    @Override
    public boolean hasAnchor(int arm) {
        return validArm(arm) && anchorFaces[arm] >= 0;
    }

    @Override
    public void setAnchor(int arm, long blockPos, int face) {
        if (validArm(arm)) {
            anchorBlockPositions[arm] = blockPos;
            anchorFaces[arm] = face;
        }
    }

    @Override
    public void clearAnchor(int arm) {
        if (validArm(arm)) {
            anchorBlockPositions[arm] = 0L;
            anchorFaces[arm] = -1;
        }
    }

    @Override
    public long getAnchorBlockPos(int arm) {
        return validArm(arm) ? anchorBlockPositions[arm] : 0L;
    }

    @Override
    public int getAnchorFace(int arm) {
        return validArm(arm) ? anchorFaces[arm] : -1;
    }

    @Override
    public boolean isLocomotionActive() {
        return locomotionActive;
    }

    @Override
    public void setLocomotionActive(boolean active) {
        locomotionActive = active;
    }

    @Override
    public boolean wasNoGravityBeforeLocomotion() {
        return noGravityBeforeLocomotion;
    }

    @Override
    public void setNoGravityBeforeLocomotion(boolean noGravity) {
        noGravityBeforeLocomotion = noGravity;
    }

    @Override
    public float getTraversalForwardInput() {
        return traversalForwardInput;
    }

    @Override
    public float getTraversalStrafeInput() {
        return traversalStrafeInput;
    }

    @Override
    public boolean isTraversalJumping() {
        return traversalJumping;
    }

    @Override
    public boolean isTraversalDescending() {
        return traversalDescending;
    }

    @Override
    public long getTraversalInputTick() {
        return traversalInputTick;
    }

    @Override
    public void setTraversalInput(float forward, float strafe, boolean jumping, boolean descending, long tick) {
        traversalForwardInput = clamp(forward, -1.0F, 1.0F);
        traversalStrafeInput = clamp(strafe, -1.0F, 1.0F);
        traversalJumping = jumping;
        traversalDescending = descending;
        traversalInputTick = tick;
    }

    @Override
    public void clearTraversalInput() {
        traversalForwardInput = 0.0F;
        traversalStrafeInput = 0.0F;
        traversalJumping = false;
        traversalDescending = false;
        traversalInputTick = Long.MIN_VALUE;
    }

    @Override
    public int getAirborneTicks() {
        return airborneTicks;
    }

    @Override
    public void setAirborneTicks(int ticks) {
        airborneTicks = Math.max(0, ticks);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putBoolean("Climbing", climbing);
        tag.putBoolean("Retracting", retracting);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putInt("AllocationMode", allocationMode);
        tag.putBoolean("LocomotionActive", locomotionActive);
        tag.putBoolean("NoGravityBeforeLocomotion", noGravityBeforeLocomotion);
        tag.putInt("AirborneTicks", airborneTicks);
        for (int i = 0; i < ARM_COUNT; i++) {
            CompoundTag arm = new CompoundTag();
            Vec3 target = targets[i];
            arm.putDouble("TargetX", target.x);
            arm.putDouble("TargetY", target.y);
            arm.putDouble("TargetZ", target.z);
            arm.putFloat("Progress", progress[i]);
            arm.putInt("Mode", modes[i]);
            arm.putInt("HeldEntity", heldEntityIds[i]);
            arm.putLong("AttackUntil", attackUntil[i]);
            arm.putLong("AttackCooldownUntil", attackCooldownUntil[i]);
            arm.putInt("TargetEntity", targetEntityIds[i]);
            arm.putLong("AnchorBlock", anchorBlockPositions[i]);
            arm.putInt("AnchorFace", anchorFaces[i]);
            tag.put("Arm" + i, arm);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        active = tag.getBoolean("Active");
        climbing = tag.getBoolean("Climbing");
        retracting = tag.getBoolean("Retracting");
        expiresAt = tag.getLong("ExpiresAt");
        allocationMode = tag.getInt("AllocationMode");
        locomotionActive = tag.getBoolean("LocomotionActive");
        noGravityBeforeLocomotion = tag.getBoolean("NoGravityBeforeLocomotion");
        airborneTicks = tag.getInt("AirborneTicks");
        for (int i = 0; i < ARM_COUNT; i++) {
            CompoundTag arm = tag.getCompound("Arm" + i);
            targets[i] = new Vec3(arm.getDouble("TargetX"), arm.getDouble("TargetY"), arm.getDouble("TargetZ"));
            progress[i] = clamp(arm.getFloat("Progress"), 0.0F, 1.0F);
            modes[i] = arm.getInt("Mode");
            heldEntityIds[i] = arm.contains("HeldEntity") ? arm.getInt("HeldEntity") : -1;
            attackUntil[i] = arm.getLong("AttackUntil");
            attackCooldownUntil[i] = arm.getLong("AttackCooldownUntil");
            targetEntityIds[i] = arm.contains("TargetEntity") ? arm.getInt("TargetEntity") : -1;
            anchorBlockPositions[i] = arm.getLong("AnchorBlock");
            anchorFaces[i] = arm.contains("AnchorFace") ? arm.getInt("AnchorFace") : -1;
        }
    }

    private static boolean validArm(int arm) {
        return arm >= 0 && arm < ARM_COUNT;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
