package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.List;

public class QuestStageDefinition {
    private final String id;
    private final String name;
    private final String summary;
    private final List<QuestStepDefinition> steps;
    private final QuestRewardDefinition rewards;

    public QuestStageDefinition(String id, String name, String summary, List<QuestStepDefinition> steps, QuestRewardDefinition rewards) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.steps = List.copyOf(steps);
        this.rewards = rewards;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String summary() {
        return summary;
    }

    public List<QuestStepDefinition> steps() {
        return steps;
    }

    public QuestRewardDefinition rewards() {
        return rewards;
    }
}
