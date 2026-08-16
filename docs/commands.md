# Commands

Generated from command registration in `KimetsunoyaibaMultiplayer.onRegisterCommands` and `ClientCommandHandler.onRegisterClientCommands`.

## Server Commands

These are registered on `RegisterCommandsEvent`.

| Command | Permission | Purpose |
| --- | --- | --- |
| `/testcrowquest <x> <y> <z> [duration]` | OP level 2 | Sends a simulated crow quest location message for client-side quest marker testing. Default duration is 1200 ticks; optional duration range is 1-72000 ticks. |
| `/clearcrowquest` | OP level 2 | Clears the executing player's crow quest marker. |
| `/trainingsword` | OP level 2, player only | Converts the held nichirin or breathing sword into a training sword. |
| `/trainingsword remove` | OP level 2, player only | Removes training sword mode from the held sword. |
| `/giveblacksword <style>` | OP level 2, player only | Gives the executing player a black nichirin sword assigned to the requested breathing style. |
| `/sunbreathinglevel <level>` | OP level 2, player only | Sets the executing player's Sun Breathing level. Level range is 0-12. |
| `/sunbreathinglevel <target> <level>` | OP level 2 | Sets another player's Sun Breathing level. Level range is 0-12. |
| `/spawndemonslayer <style> <level> [demonized]` | OP level 2 | Spawns a demon slayer entity at the command source position with a spawnable breathing style and power level 0-5. |
| `/spawndemonslayer <style> <level> [male\|female\|random] [skin] [demonized]` | OP level 2 | Spawns a demon slayer with optional gender, skin 1-6, and demonized state. |
| `/demonize` | OP level 2, player only | Demonizes the targeted `BreathingSlayerEntity` within 60 blocks. |
| `/torilgate confirm` | Player only | Confirms a pending Toril Gate teleport. |
| `/torilgate cancel` | Player only | Cancels a pending Toril Gate teleport. |
| `/torilgate return` | Player only | Returns the player to the previous Toril Gate position when available. |
| `/swordsmithvillage confirm` | Player only | Confirms a pending Swordsmith Village escort. |
| `/swordsmithvillage cancel` | Player only | Cancels a pending Swordsmith Village escort. |
| `/finalselection complete` | OP level 2, player only | Completes Final Selection for the executing player. |
| `/finalselection ore` | Player only | Reopens ore selection during Final Selection in Mt. Fujikasane. |
| `/finalselection leave confirm` | Player only | Confirms leaving Final Selection. |
| `/finalselection leave cancel` | Player only | Cancels a Final Selection leave prompt. |
| `/finalselection kakushi accept` | Player only | Accepts a Kakushi offer. |
| `/finalselection kakushi decline` | Player only | Declines a Kakushi offer. |
| `/oreselect` | OP level 2, player only | Opens the standalone Nichirin ore selection menu. |
| `/survivalraid start <level> [radius]` | OP level 2 | Starts a survival raid at night. Level range is 1-5; optional radius range is 32-1000. |
| `/survivalraid stop` | OP level 2 | Stops the active survival raid in the current dimension. |
| `/survivalraid status` | OP level 2 | Prints active survival raid state, difficulty, wave, boss, entity, and player counts. |
| `/meditation confirm` | Player only | Accepts a pending meditation prompt and opens the meditation menu. |
| `/meditation decline` | Player only | Declines a pending meditation prompt. |
| `/meditation open` | Player only | Opens the meditation menu directly when custom progression is enabled. |
| `/quest stages` | Player only | Lists the selected quest's runtime stages. |
| `/quest restart` | Player only | Restarts the current quest mission/stage from its first step and removes spawned entities for that mission/stage. |
| `/quest skip <stage>` | OP level 2, player only | Skips the selected quest to a later stage. |
| `/restartquest` | Player only | Alias for restarting the current quest mission/stage. |
| `/skipquest <stage>` | OP level 2, player only | Alias for skipping the selected quest to a later stage. |
| `/debugplayerdims` | OP level 2, player only | Prints server-side player dimension debug information. |
| `/debugplayerdims clear` | OP level 2, player only | Clears the debug player dimension override. |
| `/debugplayerdims <height> <eyeHeight>` | OP level 2, player only | Applies a debug player dimension override. Height range is 0.01-10.0; eye height range is 0.0-10.0. |
| `/repairhousetamayo` | OP level 2, player only | Repairs the nearest Tamayo house structure. |
| `/testtamayohouse` | OP level 2, player only | Enables Tamayo house test particles for 45 seconds. |
| `/localpos` | OP level 2, player only | Prints the player's local position inside the current KimetsunoYaiba or multiplayer structure. |
| `/localposition` | OP level 2, player only | Alias for `/localpos`. |
| `/freerank <rank>` | OP level 2 | Makes a demon rank takeable from its offline holder via the fallback entity. |
| `/clearrank <target>` | OP level 2 | Removes a player's demon rank entirely. |
| `/setrank <target> <rank>` | OP level 2 | Assigns a demon rank directly and applies its buffs. |
| `/knygravity get` | OP level 2, player only | Prints current/base gravity and whether KNY gravity is enabled. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity set <direction>` | OP level 2, player only | Sets the player's base gravity direction. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity reset` | OP level 2, player only | Resets the player's gravity. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity field_debug on` | OP level 2 | Enables gravity field debug mode. Registered only when `KNYGravity.isEnabled()` is true. |
| `/knygravity field_debug off` | OP level 2 | Disables gravity field debug mode. Registered only when `KNYGravity.isEnabled()` is true. |

## Client-Only Commands

These are registered on `RegisterClientCommandsEvent` by `ClientCommandHandler`.

| Command | Permission | Purpose |
| --- | --- | --- |
| `/testanim [animation]` | OP level 2 | Tests sword animation particles for the held sword. Defaults to `sword_to_right`. |
| `/testparticles` | OP level 2 | Prints the held KimetsunoYaiba sword type and mapped particle. |
| `/debugparticles` | OP level 2 | Prints particle config, held item, sword mapping, and test animation sword-tip positions. |
| `/smallmist [count]` | OP level 2 | Spawns small mist particles above the executing entity. Default count is 10; optional count range is 1-100. |
| `/testanimc [animation]` | OP level 2 | Plays a client-side test animation, spawns a sword slash model for sword slash animations, and sends an animation sync packet. Defaults to `sword_to_left`. |

## Command Classes Not Currently Registered

These classes contain command registration methods but are not called by the current server/client command registration paths:

| Command class | Command literal |
| --- | --- |
| `DebugCrowCommand` | `/debugcrow` |
| `TestAnimationCommand` | `/testanim` |
| `TestCrowRenderCommand` | `/testcrowrender` |
