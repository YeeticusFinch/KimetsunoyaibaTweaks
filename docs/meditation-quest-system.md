# Meditation Menu And Quest Runtime Guide

This guide explains how the meditation menu works, how the custom quest runtime works, and how to add new quest content.

## Config Gate

Everything in this system is gated behind `custom_progression.demon_slayer_initiation.disable_base_mod_demon_slayer_initiation` in [CustomProgressionConfig.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomProgressionConfig.java).

When that config is `true`:
- The meditation prompt can appear while sitting on a cushion.
- The meditation menu can open.
- The custom quest runtime can advance steps and rewards.
- Right-clicking an owned kasugai crow uses the custom quest marker path.
- The client stops auto-parsing base crow quest chat, so our custom crow objective markers take over.

When that config is `false`:
- The meditation menu and its selection state are inert.
- The custom quest runtime does not advance.
- The fallback base crow chat parsing remains available.

## File Map

Core menu files:
- [MeditationPromptHandler.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationPromptHandler.java)
- [MeditationMenuService.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationMenuService.java)
- [MeditationMenuScreen.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/MeditationMenuScreen.java)
- [MeditationMenuData.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationMenuData.java)

Quest runtime files:
- [QuestGroupRegistry.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestGroupRegistry.java)
- [QuestGroupDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestGroupDefinition.java)
- [QuestStageDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStageDefinition.java)
- [QuestStepDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStepDefinition.java)
- [QuestRewardDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestRewardDefinition.java)
- [QuestProgressionManager.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionManager.java)
- [QuestProgressionHandler.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionHandler.java)
- [QuestScenarioActions.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestScenarioActions.java)
- [QuestStructureTags.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStructureTags.java)

Menu-only hidden items:
- [ModItems.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/items/ModItems.java)
- [quest_scroll.json](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/resources/assets/kimetsunoyaibamultiplayer/models/item/quest_scroll.json)
- [nav_pin_waypoint.json](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/resources/assets/kimetsunoyaibamultiplayer/models/item/nav_pin_waypoint.json)

## Meditation Menu Behavior

The meditation menu is a code-drawn screen. It does not require a dedicated custom GUI texture yet.

Current flow:
1. A player sits on a cushion long enough to trigger [MeditationPromptHandler.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationPromptHandler.java).
2. Clicking `YES` calls [MeditationMenuCommand.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/MeditationMenuCommand.java).
3. The server builds menu data in [MeditationMenuService.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationMenuService.java).
4. The client opens [MeditationMenuScreen.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/MeditationMenuScreen.java).

Current tabs:
- `Info`: role, rank, demon slayer or demon kill stats.
- `Navigation`: grouped quest list plus waypoint list.
- `Skills`: scaffold only for now.

Navigation rules:
- Quests use the hidden `quest_scroll` item.
- Waypoints use the hidden `nav_pin_waypoint` item.
- Only one selection is active at a time.
- A selection can be either one quest or one waypoint, never both.
- Both quest and location panes are scrollable.

Role-based waypoints currently come from [MeditationMenuService.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/meditation/MeditationMenuService.java) and include:
- `house_kocho`
- `graveyard`
- `house_rengoku`
- `house_tamayo`
- `house_tanjiro`
- `house_ubuyashiki`
- `house_urokodaki`

## Quest Model

The custom runtime uses a three-layer structure:

- `QuestGroupDefinition`: a long-running story arc such as `Cruel`.
- `QuestStageDefinition`: one mission inside that arc such as `Mission No.1 - Kidnapper's Bog`.
- `QuestStepDefinition`: one objective inside that mission such as entering a structure or talking to an NPC.

So `Cruel` is a quest group, while `Mission No.1 - Kidnapper's Bog` is a stage inside it.

The active runtime state is stored on the player with these persistent-data keys in [QuestProgressionManager.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionManager.java):
- `KnYQuestActiveGroup`
- `KnYQuestActiveStageIndex`
- `KnYQuestActiveStepIndex`
- `KnYQuestActiveStepStarted`

## Step Types

Supported step types are defined in [QuestStepType.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStepType.java):

- `ENTER_STRUCTURE`
- `ENTER_BIOME`
- `KILL_ENTITY`
- `TALK_TO_ENTITY`
- `OBTAIN_ITEM`
- `WAIT_FOR_NIGHT`
- `CUSTOM`

Out of the box helpers on [QuestStepDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStepDefinition.java):
- `enterStructure(...)`
- `enterBiome(...)`
- `killEntity(...)`
- `killQuestTarget(...)`
- `talkToEntity(...)`
- `obtainItem(...)`
- `waitForNight(...)`
- `builder(...)` for advanced cases

What the runtime checks automatically:
- `ENTER_STRUCTURE`: player is currently inside the target structure.
- `ENTER_BIOME`: player is currently in the target biome.
- `OBTAIN_ITEM`: player inventory contains the required item count.
- `WAIT_FOR_NIGHT`: the world is night.
- `KILL_ENTITY`: completion comes from a kill event.
- `TALK_TO_ENTITY`: completion comes from right-clicking an entity.
- `CUSTOM`: completion comes from `customCheck`.

## Rewards

Rewards are configured per stage with [QuestRewardDefinition.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestRewardDefinition.java).

Supported reward types:
- Items via `.item(itemId, count)`
- XP via `.experiencePoints(points)`
- Sun breathing progression via `.sunBreathingLevels(delta)`
- Advancements via `.advancement(advancementId)`

The reward application happens in [QuestProgressionManager.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionManager.java).

## Crow Objective Markers

Right-clicking an owned, tamed kasugai crow triggers the custom objective marker path in [QuestProgressionHandler.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionHandler.java).

How it works:
1. The interaction is validated so only the crow's owner can use it.
2. [QuestProgressionManager.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestProgressionManager.java) asks the active step for a marker target.
3. The server sends [SetCrowQuestMarkerPacket.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/network/packets/SetCrowQuestMarkerPacket.java) to the client.
4. The client renders the marker with [CrowQuestMarkerHandlerClient.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/CrowQuestMarkerHandlerClient.java).

To make a step crow-trackable, set a `markerResolver(...)` on the step builder.

Examples:
- Structure step: return the nearest structure center.
- Talk step: return the nearest tagged NPC entity.
- Kill step: return the nearest tagged quest target entity.

If custom progression is enabled, [CrowQuestMarkerHandlerClient.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/CrowQuestMarkerHandlerClient.java) ignores base crow chat auto-detection so the custom objective marker path overrides it.

## NPC And Special Target Spawning

[QuestScenarioActions.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestScenarioActions.java) exists for scenario-specific behaviors that do not fit the generic step checks.

Current helpers:
- `storeCurrentStructureCenter(...)`
- `ensureKazumiSpawned(...)`
- `ensureSwampDemonSpawned(...)`
- `findNearestQuestEntity(...)`
- `findNearestStructure(...)`
- `sendKazumiDialogue(...)`

Important pattern:
- Use `onStart(...)` when something should happen once when a step becomes active.
- Use `onTick(...)` when something should be retried until the player finishes the step.
- Use persistent entity tags like `KnYQuestNpcId` and `KnYQuestTargetId` so talk and kill steps can match specific spawned entities.

## Structure Tags For Waypoints And Structure Steps

Crow structure lookup uses structure tags generated by [QuestStructureTags.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestStructureTags.java).

If you want crow markers for a structure, add:
1. A `ResourceLocation` for that structure in your quest code.
2. A matching tag JSON under `src/main/resources/data/kimetsunoyaibamultiplayer/tags/worldgen/structure/quest_waypoint/`.

Example file already present:
- [village_swamp.json](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/resources/data/kimetsunoyaibamultiplayer/tags/worldgen/structure/quest_waypoint/village_swamp.json)

## How To Add A New Quest Group

Most new quest content should be added in [QuestGroupRegistry.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestGroupRegistry.java).

### 1. Register the group

Add a new `register(new QuestGroupDefinition(...))` entry.

Example skeleton:

```java
register(new QuestGroupDefinition(
    "cruel",
    "Cruel",
    "The demon slayer main story arc built from linked mission quests.",
    Set.of(PlayerRole.DEMON_SLAYER, PlayerRole.DEMON_SLAYER_IN_TRAINING),
    List.of(
        // stages go here
    )
));
```

### 2. Add one or more stages

Each mission inside the arc is a `QuestStageDefinition`.

Example:

```java
new QuestStageDefinition(
    "kidnappers_bog",
    "Mission No.1 - Kidnapper's Bog",
    "Investigate the kidnappings in Northwest Town.",
    List.of(
        // steps go here
    ),
    new QuestRewardDefinition()
        .experiencePoints(150)
        .item(ResourceLocation.parse("minecraft:bread"), 4)
        .sunBreathingLevels(1)
);
```

### 3. Add multiple steps to the stage

Simple helper examples:

```java
QuestStepDefinition.enterStructure(
    "enter_village_swamp",
    "Reach Kidnapper's Bog",
    "Enter a kimetsunoyaiba:village_swamp structure.",
    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp")
);

QuestStepDefinition.enterBiome(
    "enter_misty_biome",
    "Enter The Mist",
    "Travel into the target biome.",
    ResourceLocation.fromNamespaceAndPath("minecraft", "swamp")
);

QuestStepDefinition.obtainItem(
    "collect_wisteria",
    "Gather Wisteria",
    "Bring back 6 wisteria flowers.",
    ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_flower"),
    6
);

QuestStepDefinition.waitForNight(
    "wait_for_night",
    "Wait for Nightfall",
    "Remain in the area until night arrives."
);
```

### 4. Use the builder for talk, kill, or custom logic

Talking to a specific entity:

```java
QuestStepDefinition.builder(
        "talk_to_kazumi",
        "Talk to Kazumi",
        "Find and speak with Kazumi inside the village.",
        QuestStepType.TALK_TO_ENTITY
    )
    .targetKey("kazumi")
    .onStart(QuestScenarioActions::ensureKazumiSpawned)
    .onComplete((player, context) -> QuestScenarioActions.sendKazumiDialogue(player))
    .markerResolver((player, context) ->
        QuestScenarioActions.findNearestQuestEntity(player, "kazumi", 400.0D))
    .build();
```

Killing a specific spawned target:

```java
QuestStepDefinition.builder(
        "kill_swamp_demon",
        "Defeat the Swamp Demon",
        "Kill the special swamp demon attacking the village.",
        QuestStepType.KILL_ENTITY
    )
    .targetKey("swamp_demon_kidnappers_bog")
    .onTick(QuestScenarioActions::ensureSwampDemonSpawned)
    .markerResolver((player, context) ->
        QuestScenarioActions.findNearestQuestEntity(player, "swamp_demon_kidnappers_bog", 256.0D))
    .build();
```

Custom completion logic:

```java
QuestStepDefinition.builder(
        "custom_trial",
        "Pass The Trial",
        "Satisfy a custom scripted condition.",
        QuestStepType.CUSTOM
    )
    .onStart((player, context) -> {
        // one-time setup
    })
    .onTick((player, context) -> {
        // repeated maintenance or spawning
    })
    .customCheck((player, context) ->
        player.getPersistentData().getBoolean("MyCustomQuestFlag"))
    .markerResolver((player, context) -> player.blockPosition())
    .build();
```

### 5. Update initial routing if the new group should auto-start

[QuestGroupRegistry.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestGroupRegistry.java) currently chooses the initial active group with `getInitialActiveGroup(...)`.

If your new group should become the default story arc for a role, update that switch.

## Worked Example: Cruel -> Mission No.1 - Kidnapper's Bog

The current runtime implementation of `Cruel` is a good template.

Flow:
1. Enter `kimetsunoyaiba:village_swamp`.
2. Spawn and talk to `Kazumi`.
3. Wait until night.
4. Spawn and kill the quest-tagged swamp demon.
5. Grant stage rewards and move to the next stage.

This is implemented in [QuestGroupRegistry.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestGroupRegistry.java) and [QuestScenarioActions.java](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/quest/QuestScenarioActions.java).

## Developer Notes And Constraints

- `TALK_TO_ENTITY` and `KILL_ENTITY` steps usually work best with `targetKey(...)` plus persistent entity tags.
- If you need crow guidance to a structure, make sure the structure tag JSON exists.
- If you need a spawned NPC or enemy, prefer adding a helper in `QuestScenarioActions` instead of bloating `QuestProgressionManager`.
- Rewards are stage-based right now, not per-step.
- The menu currently displays runtime groups and the current active stage/step. It does not yet expose a full completed-quest history.
- Placeholder groups for `Permanence`, `Veil`, and `Temper` are already registered but still need authored stages.

## Recommended Workflow For Adding A Quest

1. Add any needed structure tag JSON files for crow pathing.
2. Add scenario helpers in `QuestScenarioActions` if the quest needs custom NPC or target spawning.
3. Register the quest group or stage in `QuestGroupRegistry`.
4. Add rewards with `QuestRewardDefinition`.
5. Verify that the stage appears in the meditation menu.
6. Test crow right-click guidance for each step that should be trackable.
7. Run `cmd.exe /c gradlew.bat compileJava`.
