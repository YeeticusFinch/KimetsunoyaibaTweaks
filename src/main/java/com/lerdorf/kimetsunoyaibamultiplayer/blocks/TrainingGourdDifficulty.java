package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

public enum TrainingGourdDifficulty {
    EASY("easy", 10),
    HARD("hard", 5),
    EXTREME("extreme", 3);

    private final String name;
    private final int seconds;

    TrainingGourdDifficulty(String name, int seconds) {
        this.name = name;
        this.seconds = seconds;
    }

    public String getName() {
        return name;
    }

    public int getSeconds() {
        return seconds;
    }

    public static TrainingGourdDifficulty fromId(int id) {
        TrainingGourdDifficulty[] values = values();
        return id < 0 || id >= values.length ? EASY : values[id];
    }
}
