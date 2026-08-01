package com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

@AutoRegisterCapability
public interface IGravityCapability extends ICapabilitySerializable<CompoundTag> {
    ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "gravity");

    void tick();

    void sync(boolean noAnimation, Direction baseGravityDirection, Direction currentGravityDirection,
              double baseGravityStrength, double currentGravityStrength);
}
