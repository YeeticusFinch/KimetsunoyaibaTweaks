package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.List;
import java.util.Set;

public class QuestGroupDefinition {
    private final String id;
    private final String name;
    private final String summary;
    private final Set<PlayerRole> allowedRoles;
    private final List<QuestStageDefinition> stages;

    public QuestGroupDefinition(String id, String name, String summary, Set<PlayerRole> allowedRoles,
                                List<QuestStageDefinition> stages) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.allowedRoles = Set.copyOf(allowedRoles);
        this.stages = List.copyOf(stages);
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

    public boolean isAvailableFor(PlayerRole role) {
        return allowedRoles.contains(role);
    }

    public List<QuestStageDefinition> stages() {
        return stages;
    }
}
