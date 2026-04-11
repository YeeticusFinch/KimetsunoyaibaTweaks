package com.lerdorf.kimetsunoyaibamultiplayer.quest;

public enum QuestCategory {
    MAIN_STORY("Main Story Quest", 0xF5C542),
    SIDE("Side Quest", 0x55FFFF);

    private final String displayName;
    private final int color;

    QuestCategory(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
