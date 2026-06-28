package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestRegistry {
    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    static {
        register(new QuestDefinition(
            "cruel",
            "Cruel",
            QuestCategory.MAIN_STORY,
            Set.of(PlayerRole.DEMON_SLAYER, PlayerRole.DEMON_SLAYER_IN_TRAINING),
            List.of(
                "Investigate disappearances and grow from trainee into a true demon slayer.",
                "This quest line begins with Kidnapper's Bog and branches into later story missions."
            ),
            List.of(
                new QuestReward("XP"),
                new QuestReward("Story progression"),
                new QuestReward("Future mission rewards")
            ),
            List.of(
                new QuestStep("Mission No.1 - Kidnapper's Bog", "Investigate kidnappings in Northwest Town and speak with Kazumi."),
                new QuestStep("Mission No.2 - Asakusa", "Continue the trail into Asakusa."),
                new QuestStep("Mission No.3 - Tsuzumi Mansion", "Pursue the threat through Tsuzumi Mansion."),
                new QuestStep("Mission No.4 - Mount Natagumo", "Survive the horrors of Mount Natagumo.")
            )
        ));

        register(new QuestDefinition(
            "permanence",
            "Permanence",
            QuestCategory.MAIN_STORY,
            Set.of(PlayerRole.DEMON),
            List.of(
                "A demon's path toward power, loyalty, and survival.",
                "Begin by feeding your hunger and proving you can prey on humans."
            ),
            List.of(
                new QuestReward("XP"),
                new QuestReward("Demon progression")
            ),
            List.of(
                new QuestStep("Stage No.1 - First Taste of Blood", "Kill 10 humans and eat 10 human flesh items."),
                new QuestStep("Stage No.2 - Hunger Unending", "Find hunting grounds at night and continue feeding without drawing the Corps' attention."),
                new QuestStep("Stage No.3 - Slayer's Blood", "Learn what makes demon slayers dangerous, then break one.")
            )
        ));

        register(new QuestDefinition(
            "veil",
            "Veil",
            QuestCategory.MAIN_STORY,
            Set.of(PlayerRole.KAKUSHI),
            List.of(
                "Support the Corps from the shadows through logistics, rescue, and recovery.",
                "This framework quest will later branch into escort and field-support work."
            ),
            List.of(
                new QuestReward("XP"),
                new QuestReward("Support progression")
            ),
            List.of(
                new QuestStep("First Assignment", "Report for Kakushi support duty.")
            )
        ));

        register(new QuestDefinition(
            "temper",
            "Temper",
            QuestCategory.MAIN_STORY,
            Set.of(PlayerRole.SWORDSMITH),
            List.of(
                "Walk the path of a swordsmith and serve the slayers through craft.",
                "This framework quest will later cover ore, forging, and delivery milestones."
            ),
            List.of(
                new QuestReward("XP"),
                new QuestReward("Crafting progression")
            ),
            List.of(
                new QuestStep("The Forge", "Begin your training at the swordsmith village.")
            )
        ));
    }

    private QuestRegistry() {
    }

    private static void register(QuestDefinition definition) {
        QUESTS.put(definition.id(), definition);
    }

    public static List<QuestDefinition> getAvailableQuests(PlayerRole role) {
        return QUESTS.values().stream()
            .filter(quest -> quest.isAvailableFor(role))
            .toList();
    }
}
