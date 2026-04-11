package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class QuestRewardDefinition {
    private final List<ItemReward> itemRewards = new ArrayList<>();
    private final List<ResourceLocation> advancementRewards = new ArrayList<>();
    private int experiencePoints;
    private int sunBreathingLevelDelta;

    public QuestRewardDefinition item(ResourceLocation itemId, int count) {
        itemRewards.add(new ItemReward(itemId, count));
        return this;
    }

    public QuestRewardDefinition experiencePoints(int points) {
        this.experiencePoints = points;
        return this;
    }

    public QuestRewardDefinition sunBreathingLevels(int delta) {
        this.sunBreathingLevelDelta = delta;
        return this;
    }

    public QuestRewardDefinition advancement(ResourceLocation advancementId) {
        advancementRewards.add(advancementId);
        return this;
    }

    public List<ItemReward> itemRewards() {
        return List.copyOf(itemRewards);
    }

    public List<ResourceLocation> advancementRewards() {
        return List.copyOf(advancementRewards);
    }

    public int experiencePoints() {
        return experiencePoints;
    }

    public int sunBreathingLevelDelta() {
        return sunBreathingLevelDelta;
    }

    public record ItemReward(ResourceLocation itemId, int count) {
    }
}
