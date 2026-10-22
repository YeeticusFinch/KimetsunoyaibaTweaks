package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Mugen demon using the demon 0 biped model with doubled purple demon stats. */
public class MugenDemonEntity extends PurpleDemonEntity {
    public MugenDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NamedDemonEntity.createAttributes(80.0D, 6.0D, 0.30D, 5.0D, 0.3D, 0.3D);
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/mugen_demon_0.png");
    }

    @Override
    public ResourceLocation getEyesTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/mugen_demon_0_eyes.png");
    }
}
