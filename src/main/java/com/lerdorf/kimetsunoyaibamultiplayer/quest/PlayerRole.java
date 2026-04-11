package com.lerdorf.kimetsunoyaibamultiplayer.quest;

public enum PlayerRole {
    CIVILIAN("Civilian"),
    DEMON_SLAYER_IN_TRAINING("Demon Slayer in Training"),
    DEMON_SLAYER("Demon Slayer"),
    KAKUSHI("Kakushi"),
    SWORDSMITH("Swordsmith"),
    DEMON("Demon");

    private final String displayName;

    PlayerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
