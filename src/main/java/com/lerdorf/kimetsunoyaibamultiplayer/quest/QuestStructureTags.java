package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class QuestStructureTags {
    private QuestStructureTags() {
    }

    public static TagKey<Structure> tagFor(ResourceLocation structureId) {
        String path = structureId.getPath();
        return TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "quest_waypoint/" + path));
    }
}
