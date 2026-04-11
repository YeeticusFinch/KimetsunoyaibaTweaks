package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.List;
import java.util.Set;

public class QuestDefinition {
    private final String id;
    private final String name;
    private final QuestCategory category;
    private final Set<PlayerRole> allowedRoles;
    private final List<String> description;
    private final List<QuestReward> rewards;
    private final List<QuestStep> steps;

    public QuestDefinition(String id, String name, QuestCategory category, Set<PlayerRole> allowedRoles,
                           List<String> description, List<QuestReward> rewards, List<QuestStep> steps) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.allowedRoles = allowedRoles;
        this.description = List.copyOf(description);
        this.rewards = List.copyOf(rewards);
        this.steps = List.copyOf(steps);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public QuestCategory category() {
        return category;
    }

    public boolean isAvailableFor(PlayerRole role) {
        return allowedRoles.contains(role);
    }

    public List<String> description() {
        return description;
    }

    public List<QuestReward> rewards() {
        return rewards;
    }

    public List<QuestStep> steps() {
        return steps;
    }
}
