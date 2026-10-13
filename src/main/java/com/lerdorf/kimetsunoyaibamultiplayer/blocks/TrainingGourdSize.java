package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

public enum TrainingGourdSize {
    SMALL("small", 20),
    MEDIUM("medium", 40),
    LARGE("large", 80),
    HUGE("huge", 160),
    GARGANTUAN("gargantuan", 320);

    private final String name;
    private final int requiredBreaths;

    TrainingGourdSize(String name, int requiredBreaths) {
        this.name = name;
        this.requiredBreaths = requiredBreaths;
    }

    public String getName() {
        return name;
    }

    public int getRequiredBreaths() {
        return requiredBreaths;
    }

    public static TrainingGourdSize fromId(int id) {
        TrainingGourdSize[] values = values();
        return id < 0 || id >= values.length ? SMALL : values[id];
    }
}
