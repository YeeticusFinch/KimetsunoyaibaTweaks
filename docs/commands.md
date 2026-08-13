# Commands

Generated from command registration in `KimetsunoyaibaMultiplayer.onRegisterCommands` and `ClientCommandHandler.onRegisterClientCommands`.

## Server Commands

These are registered on `RegisterCommandsEvent`.

| Command | Permission | Source | Purpose |
| --- | --- | --- | --- |
| `/testcrowquest <x> <y> <z> [duration]` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestCrowQuestCommand.java` | Sends a simulated crow quest location message for client-side quest marker testing. Default duration is 1200 ticks; optional duration range is 1-72000 ticks. |
| `/clearcrowquest` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestCrowQuestCommand.java` | Clears the executing player's crow quest marker. |
| `/trainingsword` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TrainingSwordCommand.java` | Converts the held nichirin or breathing sword into a training sword. |
| `/trainingsword remove` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TrainingSwordCommand.java` | Removes training sword mode from the held sword. |
| `/giveblacksword <style>` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/GiveBlackSwordCommand.java` | Gives the executing player a black nichirin sword assigned to the requested breathing style. |
| `/sunbreathinglevel <level>` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SunBreathingLevelCommand.java` | Sets the executing player's Sun Breathing level. Level range is 0-12. |
| `/sunbreathinglevel <target> <level>` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SunBreathingLevelCommand.java` | Sets another player's Sun Breathing level. Level range is 0-12. |
| `/spawndemonslayer <style> <level> [demonized]` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SpawnDemonSlayerCommand.java` | Spawns a demon slayer entity at the command source position with a spawnable breathing style and power level 0-5. |
| `/spawndemonslayer <style> <level> [male\|female\|random] [skin] [demonized]` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SpawnDemonSlayerCommand.java` | Spawns a demon slayer with optional gender, skin 1-6, and demonized state. |
| `/demonize` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DemonizeCommand.java` | Demonizes the targeted `BreathingSlayerEntity` within 60 blocks. |
| `/torilgate confirm` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TorilGateCommand.java` | Confirms a pending Toril Gate teleport. |
| `/torilgate cancel` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TorilGateCommand.java` | Cancels a pending Toril Gate teleport. |
| `/torilgate return` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TorilGateCommand.java` | Returns the player to the previous Toril Gate position when available. |
| `/swordsmithvillage confirm` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SwordsmithVillageCommand.java` | Confirms a pending Swordsmith Village escort. |
| `/swordsmithvillage cancel` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SwordsmithVillageCommand.java` | Cancels a pending Swordsmith Village escort. |
| `/finalselection complete` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Completes Final Selection for the executing player. |
| `/finalselection ore` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Reopens ore selection during Final Selection in Mt. Fujikasane. |
| `/finalselection leave confirm` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Confirms leaving Final Selection. |
| `/finalselection leave cancel` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Cancels a Final Selection leave prompt. |
| `/finalselection kakushi accept` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Accepts a Kakushi offer. |
| `/finalselection kakushi decline` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/FinalSelectionCommand.java` | Declines a Kakushi offer. |
| `/oreselect` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/OreSelectCommand.java` | Opens the standalone Nichirin ore selection menu. |
| `/survivalraid start <level> [radius]` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SurvivalRaidCommand.java` | Starts a survival raid at night. Level range is 1-5; optional radius range is 32-1000. |
| `/survivalraid stop` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SurvivalRaidCommand.java` | Stops the active survival raid in the current dimension. |
| `/survivalraid status` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SurvivalRaidCommand.java` | Prints active survival raid state, difficulty, wave, boss, entity, and player counts. |
| `/meditation confirm` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/MeditationMenuCommand.java` | Accepts a pending meditation prompt and opens the meditation menu. |
| `/meditation decline` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/MeditationMenuCommand.java` | Declines a pending meditation prompt. |
| `/meditation open` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/MeditationMenuCommand.java` | Opens the meditation menu directly when custom progression is enabled. |
| `/quest stages` | Player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/QuestCommand.java` | Lists the selected quest's runtime stages. |
| `/quest skip <stage>` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/QuestCommand.java` | Skips the selected quest to a later stage. |
| `/skipquest <stage>` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/QuestCommand.java` | Alias for skipping the selected quest to a later stage. |
| `/debugplayerdims` | Any player | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DebugPlayerDimensionsCommand.java` | Prints server-side player dimension debug information. |
| `/debugplayerdims clear` | Any player | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DebugPlayerDimensionsCommand.java` | Clears the debug player dimension override. |
| `/debugplayerdims <height> <eyeHeight>` | Any player | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DebugPlayerDimensionsCommand.java` | Applies a debug player dimension override. Height range is 0.01-10.0; eye height range is 0.0-10.0. |
| `/repairhousetamayo` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/RepairHouseTamayoCommand.java` | Repairs the nearest Tamayo house structure. |
| `/testtamayohouse` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestTamayoHouseCommand.java` | Enables Tamayo house test particles for 45 seconds. |
| `/localpos` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/LocalPosCommand.java` | Prints the player's local position inside the current KimetsunoYaiba or multiplayer structure. |
| `/localposition` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/LocalPosCommand.java` | Alias for `/localpos`. |
| `/freerank <rank>` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DemonRankCommand.java` | Makes a demon rank takeable from its offline holder via the fallback entity. |
| `/clearrank <target>` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DemonRankCommand.java` | Removes a player's demon rank entirely. |
| `/setrank <target> <rank>` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DemonRankCommand.java` | Assigns a demon rank directly and applies its buffs. |
| `/knygravity get` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/KNYGravityCommand.java` | Prints current/base gravity and whether KNY gravity is enabled. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity set <direction>` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/KNYGravityCommand.java` | Sets the player's base gravity direction. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity reset` | OP level 2, player only | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/KNYGravityCommand.java` | Resets the player's gravity. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity field_debug on` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/KNYGravityCommand.java` | Enables gravity field debug mode. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity field_debug off` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/KNYGravityCommand.java` | Disables gravity field debug mode. Registered only when `KNYGravity.isEnabled()` is true. |

## Client-Only Commands

These are registered on `RegisterClientCommandsEvent` by `ClientCommandHandler`.

| Command | Permission | Source | Purpose |
| --- | --- | --- | --- |
| `/testanim [animation]` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestAnimCommand.java` | Tests sword animation particles for the held sword. Defaults to `sword_to_right`. |
| `/testparticles` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestParticlesCommand.java` | Prints the held KimetsunoYaiba sword type and mapped particle. |
| `/debugparticles` | OP level 2 | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DebugParticlesCommand.java` | Prints particle config, held item, sword mapping, and test animation sword-tip positions. |
| `/smallmist [count]` | Client command | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/commands/SmallMistParticleCommand.java` | Spawns small mist particles above the executing entity. Default count is 10; optional count range is 1-100. |
| `/testanimc [animation]` | Client command | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/ClientCommandHandler.java` | Plays a client-side test animation, spawns a sword slash model for sword slash animations, and sends an animation sync packet. Defaults to `sword_to_left`. |

## Command Classes Not Currently Registered

These classes contain command registration methods but are not called by the current server/client command registration paths:

| Command class | Command literal | Source |
| --- | --- | --- |
| `DebugCrowCommand` | `/debugcrow` | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/DebugCrowCommand.java` |
| `TestAnimationCommand` | `/testanim` | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestAnimationCommand.java` |
| `TestCrowRenderCommand` | `/testcrowrender` | `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TestCrowRenderCommand.java` |
