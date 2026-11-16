package com.lerdorf.kimetsunoyaibamultiplayer.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;

/** Server and client state for the six organic Biokineses arms. */
public interface IBiokinesesArmData extends INBTSerializable<CompoundTag> {
    int ARM_COUNT = 6;

    int MODE_IDLE = 0;
    int MODE_ANCHOR = 1;
    int MODE_COMBAT = 2;
    int MODE_ABILITY = 3;
    int MODE_GRAB = 4;
    int MODE_ATTACK = 5;
    int MODE_FAST_RETRACT = 6;

    int ALLOCATION_NORMAL = 0;
    int ALLOCATION_ALL = 1;

    boolean isActive();

    void setActive(boolean active);

    boolean isRetracting();

    void setRetracting(boolean retracting);

    long getExpiresAt();

    void setExpiresAt(long expiresAt);

    int getAllocationMode();

    void setAllocationMode(int allocationMode);

    Vec3 getTarget(int arm);

    void setTarget(int arm, Vec3 target);

    float getProgress(int arm);

    void setProgress(int arm, float progress);

    int getMode(int arm);

    void setMode(int arm, int mode);

    int getHeldEntityId(int arm);

    void setHeldEntityId(int arm, int entityId);

    long getAttackUntil(int arm);

    void setAttackUntil(int arm, long tick);

    long getAttackCooldownUntil(int arm);

    void setAttackCooldownUntil(int arm, long tick);

    int getTargetEntityId(int arm);

    void setTargetEntityId(int arm, int entityId);

    boolean hasAnchor(int arm);

    void setAnchor(int arm, long blockPos, int face);

    void clearAnchor(int arm);

    long getAnchorBlockPos(int arm);

    int getAnchorFace(int arm);

    boolean isLocomotionActive();

    void setLocomotionActive(boolean active);

    boolean wasNoGravityBeforeLocomotion();

    void setNoGravityBeforeLocomotion(boolean noGravity);

    float getTraversalForwardInput();

    float getTraversalStrafeInput();

    boolean isTraversalJumping();

    boolean isTraversalDescending();

    long getTraversalInputTick();

    void setTraversalInput(float forward, float strafe, boolean jumping, boolean descending, long tick);

    void clearTraversalInput();
}
