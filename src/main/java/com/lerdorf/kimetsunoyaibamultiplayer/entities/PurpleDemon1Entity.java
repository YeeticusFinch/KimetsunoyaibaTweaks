package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Purple demon variant using the demon 1 model and skin. */
public class PurpleDemon1Entity extends PurpleDemonEntity {
    public PurpleDemon1Entity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/purple_demon_1.png");
    }

    @Override
    public ResourceLocation getEyesTexture() {
        return ResourceLocation.fromNamespaceAndPath(
            "kimetsunoyaibamultiplayer", "textures/entity/purple_demon_1_eyes.png");
    }
}
