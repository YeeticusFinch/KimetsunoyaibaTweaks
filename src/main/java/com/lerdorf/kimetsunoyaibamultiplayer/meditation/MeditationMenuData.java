package com.lerdorf.kimetsunoyaibamultiplayer.meditation;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class MeditationMenuData {
    private final String role;
    private final String rank;
    private final String muzanBlood;
    private final String humansConsumed;
    private final String kizukiRank;
    private final List<InfoSection> infoSections;
    private final List<QuestEntry> quests;
    private final List<LocationEntry> locations;
    private final List<PassiveSkillEntry> passiveSkills;
    private final int passiveSkillPoints;
    private final String selectedType;
    private final String selectedId;
    private final boolean demonPlayer;
    private final int demonEyesIndex;

    public MeditationMenuData(String role, String rank, String muzanBlood, String humansConsumed, String kizukiRank,
                              List<InfoSection> infoSections, List<QuestEntry> quests, List<LocationEntry> locations,
                              List<PassiveSkillEntry> passiveSkills,
                              int passiveSkillPoints,
                              String selectedType, String selectedId, boolean demonPlayer, int demonEyesIndex) {
        this.role = role;
        this.rank = rank;
        this.muzanBlood = muzanBlood;
        this.humansConsumed = humansConsumed;
        this.kizukiRank = kizukiRank;
        this.infoSections = List.copyOf(infoSections);
        this.quests = List.copyOf(quests);
        this.locations = List.copyOf(locations);
        this.passiveSkills = List.copyOf(passiveSkills);
        this.passiveSkillPoints = passiveSkillPoints;
        this.selectedType = selectedType;
        this.selectedId = selectedId;
        this.demonPlayer = demonPlayer;
        this.demonEyesIndex = demonEyesIndex;
    }

    public MeditationMenuData(FriendlyByteBuf buf) {
        this.role = buf.readUtf();
        this.rank = buf.readUtf();
        this.muzanBlood = buf.readUtf();
        this.humansConsumed = buf.readUtf();
        this.kizukiRank = buf.readUtf();
        this.infoSections = buf.readList(InfoSection::new);
        this.quests = buf.readList(QuestEntry::new);
        this.locations = buf.readList(LocationEntry::new);
        this.passiveSkills = buf.readList(PassiveSkillEntry::new);
        this.passiveSkillPoints = buf.readVarInt();
        this.selectedType = buf.readUtf();
        this.selectedId = buf.readUtf();
        this.demonPlayer = buf.readBoolean();
        this.demonEyesIndex = buf.readVarInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(role);
        buf.writeUtf(rank);
        buf.writeUtf(muzanBlood);
        buf.writeUtf(humansConsumed);
        buf.writeUtf(kizukiRank);
        buf.writeCollection(infoSections, (packetBuffer, section) -> section.write(packetBuffer));
        buf.writeCollection(quests, (packetBuffer, quest) -> quest.write(packetBuffer));
        buf.writeCollection(locations, (packetBuffer, location) -> location.write(packetBuffer));
        buf.writeCollection(passiveSkills, (packetBuffer, passiveSkill) -> passiveSkill.write(packetBuffer));
        buf.writeVarInt(passiveSkillPoints);
        buf.writeUtf(selectedType);
        buf.writeUtf(selectedId);
        buf.writeBoolean(demonPlayer);
        buf.writeVarInt(demonEyesIndex);
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(buf.readUtf());
        }
        return lines;
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeUtf(line);
        }
    }

    public String role() {
        return role;
    }

    public String rank() {
        return rank;
    }

    public String muzanBlood() {
        return muzanBlood;
    }

    public String humansConsumed() {
        return humansConsumed;
    }

    public String kizukiRank() {
        return kizukiRank;
    }

    public List<InfoSection> infoSections() {
        return infoSections;
    }

    public List<QuestEntry> quests() {
        return quests;
    }

    public List<LocationEntry> locations() {
        return locations;
    }

    public List<PassiveSkillEntry> passiveSkills() {
        return passiveSkills;
    }

    public int passiveSkillPoints() {
        return passiveSkillPoints;
    }

    public String selectedType() {
        return selectedType;
    }

    public String selectedId() {
        return selectedId;
    }

    public boolean demonPlayer() {
        return demonPlayer;
    }

    public int demonEyesIndex() {
        return demonEyesIndex;
    }

    public record InfoSection(String id, String title, int count, List<InfoSection> children) {
        public InfoSection(FriendlyByteBuf buf) {
            this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readList(InfoSection::new)
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(title);
            buf.writeInt(count);
            buf.writeCollection(children, (packetBuffer, child) -> child.write(packetBuffer));
        }
    }

    public record QuestEntry(String id, String name, String categoryLabel, int categoryColor, List<String> description,
                             List<String> rewards, String progressText, boolean completed, boolean selected) {
        public QuestEntry(FriendlyByteBuf buf) {
            this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                readStringList(buf),
                readStringList(buf),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(name);
            buf.writeUtf(categoryLabel);
            buf.writeInt(categoryColor);
            writeStringList(buf, description);
            writeStringList(buf, rewards);
            buf.writeUtf(progressText);
            buf.writeBoolean(completed);
            buf.writeBoolean(selected);
        }
    }

    public record LocationEntry(String id, String name, String description, boolean selected) {
        public LocationEntry(FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(name);
            buf.writeUtf(description);
            buf.writeBoolean(selected);
        }
    }

    public record PassiveSkillEntry(String id, String name, String role, int level, int maxLevel, String description,
                                    String statusText, boolean active, boolean canIncrease, boolean canDecrease) {
        public PassiveSkillEntry(FriendlyByteBuf buf) {
            this(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(name);
            buf.writeUtf(role);
            buf.writeVarInt(level);
            buf.writeVarInt(maxLevel);
            buf.writeUtf(description);
            buf.writeUtf(statusText);
            buf.writeBoolean(active);
            buf.writeBoolean(canIncrease);
            buf.writeBoolean(canDecrease);
        }
    }
}
