# Config Options

Generated from Forge config declarations in `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer`. Descriptions use the option comments from the source code.

## Registered Config Files

- `config/kimetsunoyaibamultiplayer/common.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/Config.java`
- `config/kimetsunoyaibamultiplayer/sword_slash.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordSwingConfig.java`
- `config/kimetsunoyaibamultiplayer/particles.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/ParticleConfig.java`
- `config/kimetsunoyaibamultiplayer/entities.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EntityConfig.java`
- `config/kimetsunoyaibamultiplayer/sword_display.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordDisplayConfig.java`
- `config/kimetsunoyaibamultiplayer/sword_rack.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordRackConfig.java`
- `config/kimetsunoyaibamultiplayer/biomes.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/BiomeConfig.java`
- `config/kimetsunoyaibamultiplayer/spawn_rates.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SpawnRateConfig.java`
- `config/kimetsunoyaibamultiplayer/enhanced_spawning.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedSpawnConfig.java`
- `config/kimetsunoyaibamultiplayer/raids.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/RaidConfig.java`
- `config/kimetsunoyaibamultiplayer/survival_raids.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SurvivalRaidConfig.java`
- `config/kimetsunoyaibamultiplayer/final_selection_raids.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/FinalSelectionRaidConfig.java`
- `config/kimetsunoyaibamultiplayer/enhanced_breathing.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedBreathingConfig.java`
- `config/kimetsunoyaibamultiplayer/customnpcs.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomNPCConfig.java`
- `config/kimetsunoyaibamultiplayer/variations.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/VariationConfig.java`
- `config/kimetsunoyaibamultiplayer/custom_progression.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomProgressionConfig.java`
- `config/kimetsunoyaibamultiplayer/demon_slayer.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/DemonSlayerConfig.java`
- `config/kimetsunoyaibamultiplayer/enhanced_blocks.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedBlocksConfig.java`
- `config/kimetsunoyaibamultiplayer/swordsmith_village.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordsmithVillageConfig.java`
- `config/kimetsunoyaibamultiplayer/demon_ranking.toml` from `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/DemonRankingConfig.java`

## Options

### Config

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/Config.java`
- Config file: `config/kimetsunoyaibamultiplayer/common.toml`

| Option path | Description |
| --- | --- |
| `common.log-debug` | Enable debug logging for the mod |
| `common.log-info` | Enable info logging for the mod |
| `common.log-warning` | Enable warning logging for the mod |
| `common.log-error` | Enable error logging for the mod |
| `common.on-screen-debug` | Enable on-screen debug information display |
| `common.show-breathes-value` | Show the raw breathes NBT value in the breathing display (useful for debugging form IDs) |
| `common.show-breathing-display` | Show on-screen breathing form display when holding a nichirin sword |
| `common.breathing-display-position` | Position of the breathing form display on screen: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BELOW_CROSSHAIR |
| `common.breathing-display-scale` | Scale/size of the breathing form display text (0.5 = half size, 1.0 = normal, 2.0 = double size) |
| `common.suppress-form-cycle-chat` | Suppress chat messages when cycling breathing forms with R key |
| `common.enable-sword-clashing` | Enable sword clashing system where attacks can be deflected or mitigated |
| `common.enable-nichirin-sprint-animation` | Enable custom sprint animation when holding a nichirin sword |
| `common.disable-base-mod-sword-swing-particles` | Disable sword swing particles from the base KimetsunoYaiba mod (left-click particles only, does not affect breathing form or right-click particles) |
| `common.breathing-form-announcements.players-announce-breathing-forms` | Announce breathing forms used by players in chat to nearby players |
| `common.breathing-form-announcements.entities-announce-breathing-forms` | Announce breathing forms used by entities in chat to nearby players |
| `common.breathing-form-announcements.breathing-form-announcement-radius` | Radius in blocks for breathing form chat announcements |
| `common.mob-slash-broadcast-range` | Max distance in blocks to send mob sword slash packets to clients<br>Lower to reduce network traffic; Default: 100 |
| `common.kanroji-entity-hand-offset-x` | X offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16) |
| `common.kanroji-entity-hand-offset-y` | Y offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16) |
| `common.kanroji-entity-hand-offset-z` | Z offset for Kanroji entity sword position to compensate for model alignment (in pixels, divided by 16) |

### FirstPersonSwordSwingConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/FirstPersonSwordSwingConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/common.toml` (embedded in `Config.java`)

| Option path | Description |
| --- | --- |
| `first_person_sword_swing.customSwingEnabled` | Enable custom first-person swing animations for nichirin swords<br>Default: true |
| `first_person_sword_swing.counter_vanilla_swing.rotateX` | X-axis rotation counter (pitch)<br>Adjusts how much vanilla's pitch swing is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.rotateX2` | X-axis square root rotation counter (pitch)<br>Adjusts how much vanilla's pitch swing is reversed<br>Default: 85.0 |
| `first_person_sword_swing.counter_vanilla_swing.rotateY` | Y-axis rotation counter (yaw)<br>Adjusts how much vanilla's yaw swing is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.rotateY2` | Y-axis square root rotation counter (yaw)<br>Adjusts how much vanilla's yaw swing is reversed<br>Default: -23.0 |
| `first_person_sword_swing.counter_vanilla_swing.rotateZ` | Z-axis rotation counter (roll)<br>Adjusts how much vanilla's roll swing is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.rotateZ2` | Z-axis square root rotation counter (roll)<br>Adjusts how much vanilla's roll swing is reversed<br>Default: 25.0 |
| `first_person_sword_swing.counter_vanilla_swing.translateX` | X-axis translation counter<br>Adjusts how much vanilla's X translation is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.translateX2` | X-axis square root translation counter<br>Adjusts how much vanilla's X translation is reversed<br>Default: -1.2 |
| `first_person_sword_swing.counter_vanilla_swing.translateY` | Y-axis translation counter<br>Adjusts how much vanilla's Y translation is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.translateY2` | Y-axis square root translation counter<br>Adjusts how much vanilla's Y translation is reversed<br>Default: -1.05 |
| `first_person_sword_swing.counter_vanilla_swing.translateZ` | Z-axis translation counter<br>Adjusts how much vanilla's Z translation is reversed<br>Default: 0.5 |
| `first_person_sword_swing.counter_vanilla_swing.translateZ2` | Z-axis square root translation counter<br>Adjusts how much vanilla's Z translation is reversed<br>Default: 0.0 |
| `first_person_sword_swing.counter_vanilla_swing.translateScale` | Translate Scale<br>Adjusts the translation scale for first person sword swing animations<br>Default: 1.3 |

### BiomeConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/BiomeConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/biomes.toml`

| Option path | Description |
| --- | --- |
| `enableBiomeFix` | Enable custom biome additions to the overworld<br>Controls spawning of custom biomes (wisteria_forest) |
| `logBiomeChanges` | Log biome addition information to console |
| `wisteriaForestReplacementChance` | === Wisteria Forest Biome Replacement Settings ===<br>Chance to START replacing vanilla forest/plains/birch_forest/flower_forest with main wisteria_forest (0.0 - 1.0)<br>When replacement starts, it creates large continuous forests via cluster expansion<br>This uses deterministic replacement - same world seed = same biome distribution<br>0.025 = 2.5% chance to start a wisteria forest, but forest will be large when it spawns<br>RECOMMENDED: 0.02-0.03 for rare but large, impressive wisteria forests |
| `wisteriaForestCyanReplacementChance` | Chance to START replacing vanilla forests with cyan wisteria_forest (0.0 - 1.0)<br>Creates large continuous cyan forests when replacement starts<br>RECOMMENDED: 0.01-0.015 for rare, impressive cyan forests |
| `wisteriaForestCreamReplacementChance` | Chance to START replacing vanilla forests with cream wisteria_forest (0.0 - 1.0)<br>Creates large continuous cream forests when replacement starts<br>RECOMMENDED: 0.01-0.015 for rare, impressive cream forests |
| `wisteriaForestClusterSize` | Cluster size for wisteria forest biome replacement (1-10)<br>When a replacement starts, this many adjacent biome points are also replaced<br>Higher values create larger, more continuous wisteria forests<br>1 = small patches, 3-5 = medium forests, 7-10 = huge forests<br>RECOMMENDED: 5-7 for large impressive forests |
| `wisteriaForestCyanSizeMultiplier` | === Legacy Settings (currently not used) ===<br>Size multiplier for wisteria_forest_cyan biome (1.0 = default, higher = larger biomes)<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 0.5 (small) to 3.0 (very large) |
| `wisteriaForestCyanSpawnFrequency` | Spawn frequency for wisteria_forest_cyan biome<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 1 to 5 |
| `wisteriaForestCreamSizeMultiplier` | Size multiplier for wisteria_forest_cream biome<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 0.5 (small) to 3.0 (very large) |
| `wisteriaForestCreamSpawnFrequency` | Spawn frequency for wisteria_forest_cream biome<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 1 to 5 |
| `wisteriaForestSizeMultiplier` | Size multiplier for wisteria_forest biome (lavender+pink, default)<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 0.5 (small) to 3.0 (very large) |
| `wisteriaForestSpawnFrequency` | Spawn frequency for wisteria_forest biome (lavender+pink, default)<br>NOTE: Currently not used - biome replacement approach is active<br>Range: 1 to 5 |
| `mtFujikasaneSizeMultiplier` | === Mount Fujikasane Settings ===<br>Size multiplier for mt_fujikasane biome (1.0 = default, higher = larger mountain)<br>Larger values create a bigger mountain area (300-400 block radius at 1.0)<br>Range: 0.5 (small) to 2.0 (huge) |
| `mtFujikasaneSpawnFrequency` | Spawn frequency for mt_fujikasane biome (1 = default, higher = more common)<br>RECOMMENDED: Keep at 1 for rarity. Higher values make multiple mountains spawn<br>Range: 1 to 3 |
| `wisteriaRingWidthMultiplier` | Width multiplier for the Wisteria forest ring around Mt Fujikasane (1.0 = default)<br>Controls how wide the protective Wisteria ring is around the mountain<br>Range: 0.5 (narrow) to 2.0 (wide) |

### CustomNPCConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomNPCConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/customnpcs.toml`

| Option path | Description |
| --- | --- |
| `Custom NPCs Compatibility.enableCustomNPCCompat` | Enable Custom NPCs mod compatibility.<br>When enabled, NPCs holding nichirin swords or blood demon arts will use those abilities when attacking. |
| `Custom NPCs Compatibility.npcCooldownMultiplier` | Cooldown multiplier for NPCs.<br>Multiplies the base cooldown of breathing forms and demon arts for NPCs.<br>1.0 = same as players, 2.0 = twice as long, 0.5 = half as long |
| `Custom NPCs Compatibility.npcTriggerChance` | Probability that an NPC will use an ability when attacking (if not on cooldown).<br>0.0 = never use abilities, 1.0 = always use abilities, 0.8 = 90% chance |
| `Custom NPCs Compatibility.debugNPCAbilities` | Enable debug logging for NPC ability usage.<br>Prints detailed information to console when NPCs trigger abilities. |
| `Custom NPCs Compatibility.enableRangedAbilities` | Enable NPCs to use abilities from range (not just melee attacks).<br>When enabled, NPCs will use breathing forms when they have a target within range. |
| `Custom NPCs Compatibility.rangedAbilityRange` | Maximum range for NPCs to trigger abilities (in blocks).<br>NPCs will use abilities when their attack target is within this range. |
| `Custom NPCs Compatibility.rangedAbilityCheckInterval` | How often to check for ranged ability triggering (in ticks).<br>Lower values = more frequent checks but more processing.<br>20 ticks = 1 second, 40 ticks = 2 seconds |
| `Custom NPCs Compatibility.enableAttackAnimations` | Enable basic attack animations for NPCs (sword_to_left, sword_to_right, sword_overhead).<br>When enabled, NPCs will play random sword swing animations on normal attacks. |
| `Custom NPCs Compatibility.enableMaxCooldownClear` | Enable automatic cooldown clearing after maximum time.<br>If enabled, all NPC cooldowns will be cleared after the max cooldown time passes.<br>This prevents cooldowns from getting stuck indefinitely. |
| `Custom NPCs Compatibility.maxCooldownSeconds` | Maximum cooldown time in seconds before auto-clearing all cooldowns.<br>After an NPC uses any breathing form, if this many seconds pass, all cooldowns are cleared.<br>This timer resets every time the NPC uses a breathing form. |
| `Custom NPCs Compatibility.Form Selection Weights.form1Weight` | Weighted probability for form selection. Lower forms are used more often.<br>These weights determine how often each form tier is selected.<br>Higher values = more likely to be selected.<br>Weight for Form 1 (First Form) |
| `Custom NPCs Compatibility.Form Selection Weights.form2Weight` | Weight for Form 2 (Second Form) |
| `Custom NPCs Compatibility.Form Selection Weights.form3Weight` | Weight for Form 3 (Third Form) |
| `Custom NPCs Compatibility.Form Selection Weights.form4PlusWeight` | Weight for Form 4+ (Fourth Form and higher)<br>This weight is divided equally among all remaining forms |

### CustomProgressionConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/CustomProgressionConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/custom_progression.toml`

| Option path | Description |
| --- | --- |
| `custom_progression.demon_slayer_initiation.disable_base_mod_demon_slayer_initiation` | Disable the base mod's demon slayer initiation rewards<br>When enabled, this will prevent the following from happening when a player<br>earns the 'demon_slayer_corps' advancement:<br>- Automatic granting of uniform_chestplate, uniform_leggings, uniform_boots<br>- Automatic granting of nichirinsword<br>- Automatic spawning and taming of kasugai_crow<br>- Automatic granting of mizunoto advancement<br>- Retention of base rank advancements (mizunoto through strongest)<br>This is useful if:<br>- The base mod is bugging out and giving items multiple times<br>- You want to create custom progression via datapacks/commands<br>- You want to use your own rewards system<br>NOTE: This blocks the base mod's SupplyProcedure, AdvancementRewardProcedure,<br>CheckAdvancementDemonProcedure, Advanvement1Procedure, and ColorChangeProcedure<br>from granting these rewards and progression advancements.<br>It also enables a managed datapack that hides related base advancement chat/toast. |
| `custom_progression.demon_slayer_initiation.grant_training_sword` | Grant a training sword when initiation is blocked<br>When enabled (and disable_base_mod_demon_slayer_initiation is also enabled),<br>the player will receive a nichirinsword that has been converted to a training sword.<br>Training swords can only use the 1st Form and have reduced damage.<br>This is useful for servers that want new demon slayers to start with a training sword<br>and earn a real sword through gameplay. |
| `custom_progression.demon_progression.custom_demon_initiation` | Enable the custom demon initiation flow<br>When enabled, consuming kimetsunoyaibamultiplayer:blood_of_muzan starts a<br>custom demon transformation phase instead of immediately turning the player<br>into a demon.<br>During this phase:<br>- The player receives the Demon Transformation effect<br>- The player is tagged with oni_transform<br>- Demon slayers and demons stay neutral unless provoked<br>- Slowness, weakness, and hunger are continuously applied<br>When the effect expires naturally, the player is converted using the base<br>mod's blood_of_muzan logic. If the effect is removed early or the player dies,<br>the transformation is cancelled.<br>This also replaces base mod blood_of_muzan items in player inventories with<br>kimetsunoyaibamultiplayer:blood_of_muzan. |
| `custom_progression.demon_progression.persistent_demonhood` | Persistent Demonhood<br>When enabled, demon players keep their demon progression through death.<br>On respawn the mod replays the remembered Muzan blood consumption so the<br>base mod restores demon state, then reapplies the tracked custom demon data.<br>Default: true |
| `custom_progression.demon_progression.disable_sun_breathing_sunlight_immunity` | Disable Sun Breathing sunlight immunity<br>When enabled, the base mod's Sun Breathing and Hinokami Kagura procedures<br>cannot award kimetsunoyaiba:overcome_sunlight.<br>Because the base mod treats that advancement as sunlight immunity, this<br>also prevents Sun Breathing from making demon players immune to sunlight.<br>Solar Ascension Cure and 100 days of Blue Spider Lily Tea still grant<br>sunlight immunity through the KnYMpSolarAscension NBT tag.<br>Default: true |
| `custom_progression.demon_progression.replace_muzan_blood_ore` | Replace base mod muzan blood ore with hemolith ore<br>When enabled, any player who starts mining kimetsunoyaiba:muzanblood_ore<br>will cause that block to be replaced with<br>kimetsunoyaibamultiplayer:hemolith_ore.<br>If a player somehow finishes breaking the original base ore before the swap<br>fully resolves, the break is intercepted and hemolith drops are forced so<br>the original muzan blood ore drop never appears. |
| `custom_progression.demon_progression.replace_scarlet_ore` | Replace base mod scarlet ore drops with nichirin ore<br>When enabled, any dropped kimetsunoyaiba:scarlet_ore item is rewritten into<br>a nichirin ore stack for a random breathing style.<br>This uses the same item-replacement flow as hemolith dust so the base item<br>never reaches the player inventory unchanged. |
| `custom_progression.demon_progression.human_flesh_per_effective_muzan_blood` | How many consumed human flesh items count as one effective Muzan blood for demon progression.<br>This does not change the raw Muzan Blood Consumed display. It only affects<br>progression systems that scale from the demon player's blood count, such as<br>passive skill points and custom Blood Demon Art slot unlocks. |
| `custom_progression.sword_transformation.replace_color_changing_procedure` | Replace the base mod's color changing procedure<br>When enabled, this completely overrides the base mod's ColorChangeProcedure<br>which transforms the basic nichirinsword into a colored breathing sword<br>when the player holds it for a certain amount of time.<br>Enable this if you want to implement custom sword transformation logic,<br>or to prevent the automatic sword transformation entirely. |
| `custom_progression.infinity_castle.enable_enhanced_infinity_castle` | Enable enhanced infinity castle<br>When enabled, any non-command teleporter in this mod that enters the infinity castle<br>will use the KnY-Worlds dimension if KnY-Worlds v1.0.1 or newer is installed.<br>If KnY-Worlds is missing or older than v1.0.1, the mod falls back to the base<br>KimetsunoYaiba infinity castle dimension.<br>Default: true |
| `custom_progression.infinity_castle.demon_spawn_rate` | Demon spawn rate multiplier inside the Infinity Castle.<br>0.0 prevents natural/timed demon spawning in the Infinity Castle.<br>0.5 is half the old timed spawn attempt rate.<br>1.0 matches the old timed spawn attempt rate.<br>Values above 1.0 increase timed spawn attempts.<br>Commands, spawn eggs, mugen doors, and other manual spawn paths are unaffected.<br>Default: 0.5 |
| `custom_progression.infinity_castle.demon_spawn_cap_per_player` | Maximum loaded demon entities allowed per player in the Infinity Castle.<br>If the loaded demon count is at or above players * this value, natural/timed<br>Infinity Castle demon spawning stops until the loaded count falls below the cap.<br>Existing demons are not removed. Commands, spawn eggs, mugen doors, and other<br>manual spawn paths are unaffected.<br>Default: 70 |
| `custom_progression.infinity_castle.unique_demon_horizontal_radius` | Horizontal radius used to prevent duplicate unique demon spawns in the Infinity Castle.<br>This ignores Y level. Twelve Kizuki check for another demon of the same type.<br>Muzan entities check for any other Muzan entity in this horizontal radius.<br>Default: 800 |
| `custom_progression.toril_gate.guarantee_toril_gate_within_1000_of_origin` | Guarantee at least one toril gate exists between 800 and 1600 blocks of (0, 0) in the overworld.<br>On server start, if no existing toril gate is found in that distance band,<br>the mod will choose a random non-ocean location in that band, paint a wisteria biome patch,<br>and place a toril gate structure there. |
| `custom_progression.debug.enable_debug_logging` | Enable debug logging for progression overrides<br>Logs when advancements are blocked, items are removed, or crows are prevented from spawning |

### DemonRankingConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/DemonRankingConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/demon_ranking.toml`

| Option path | Description |
| --- | --- |
| `demon_ranking.enable_demon_ranking` | Master switch for the Demon Ranking (Twelve Kizuki) system.<br>When disabled, kills never change anyone's rank, no rank buffs are applied,<br>and the Bloody Battle entry is hidden from the Meditation Menu Navigation tab. |
| `demon_ranking.offline_takeover_minutes` | How many minutes a ranked player must be offline before their rank<br>can be taken by killing that rank's fallback entity instead of the player.<br>Use /freerank to bypass this timer for a specific rank immediately.<br>Default: 4320 (3 days) |

### DemonSlayerConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/DemonSlayerConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/demon_slayer.toml`

| Option path | Description |
| --- | --- |
| `demon_slayer.femaleSpawnChance` | Chance (0.0-1.0) that a demon slayer spawns as female instead of male.<br>0.0 = always male, 1.0 = always female, 0.3 = 30% female (default) |

### EnhancedBlocksConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedBlocksConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/enhanced_blocks.toml`

| Option path | Description |
| --- | --- |
| `enhancedChestOfDrawers` | Enable enhanced chest of drawers replacement<br>When true, base mod chest of drawers items/blocks are replaced with the multiplayer version:<br>- kimetsunoyaiba:chest_of_drawer -> kimetsunoyaibamultiplayer:chest_of_drawers<br>Right-clicking a base mod chest of drawers will convert it and immediately pass the click through.<br>Default: true |
| `enhancedVialRack` | Enable enhanced vial rack replacement<br>When true, base mod medicine holder items/blocks are replaced with the multiplayer vial rack:<br>- kimetsunoyaiba:medicine_holder -> kimetsunoyaibamultiplayer:vial_rack<br>Nearby base mod medicine holder blocks are periodically converted with randomized rack contents.<br>Default: true |

### EnhancedBreathingConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedBreathingConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/enhanced_breathing.toml`

| Option path | Description |
| --- | --- |
| `enhanced_breathing.enhancedMistBreathing` | Enable enhanced Mist Breathing forms<br>When true, automatically replaces base mod mist swords with enhanced versions:<br>- kimetsunoyaiba:nichirinsword_mist -> kimetsunoyaibamultiplayer:nichirinsword_mist<br>- kimetsunoyaiba:nichirinsword_tokito -> kimetsunoyaibamultiplayer:nichirinsword_muichiro<br>Enhanced features:<br>- 7th Form: Obscuring Clouds (Muichiro's original technique)<br>- Improved particle effects and animations<br>- Custom sword slash models<br>Default: true |
| `enhanced_breathing.enhancedFlowerBreathing` | Enable enhanced Flower Breathing sword replacement<br>When true, automatically replaces base mod flower swords with enhanced versions:<br>- kimetsunoyaiba:nichirinsword_kanawo -> kimetsunoyaibamultiplayer:nichirinsword_kanawo<br>- kimetsunoyaiba:nichirinsword_kanae -> kimetsunoyaibamultiplayer:nichirinsword_kanae<br>Enhanced features:<br>- Hashira-exclusive forms (7th, 8th, 9th for Kanae's sword)<br>- Final Form: Equinoctial Vermillion Eye<br>- Improved particle effects and animations<br>Default: true |
| `enhanced_breathing.enhancedBeastBreathing` | Enable enhanced Beast Breathing forms<br>When true, automatically replaces base mod Inosuke swords with enhanced versions:<br>- kimetsunoyaiba:nichirinsword_inosuke -> kimetsunoyaibamultiplayer:nichirinsword_inosuke<br>Enhanced features:<br>- Uses Enhanced Beast Breathing technique implementation<br>- Integrates with dual-wield behavior when paired in offhand<br>Default: true |
| `enhanced_breathing.enhancedLoveBreathing` | Enable enhanced Love Breathing forms<br>When true, automatically replaces base mod love swords with enhanced versions:<br>- kimetsunoyaiba:nichirinsword_kanroji -> kimetsunoyaibamultiplayer:nichirinsword_kanroji<br>Enhanced features:<br>- Flexible whip-like sword rendering<br>- 6 Love Breathing forms<br>- Dual-layer emissive rendering with heart particle trails<br>- Physics-based whip motion with keyframe animations<br>Default: true |
| `enhanced_breathing.enhancedBlackSword` | Enable enhanced Black Sword replacement<br>When true, automatically replaces base mod black sword with enhanced version:<br>- kimetsunoyaiba:nichirinsword_black -> kimetsunoyaibamultiplayer:nichirinsword_black<br>Enhanced features:<br>- Uses enhanced Black Sword technique implementation<br>- Improved particle effects and animations<br>Default: true |
| `enhanced_breathing.enhancedCombustibleBlood` | Enable enhanced Combustible Blood replacement<br>When true, automatically replaces base mod Nezuko Blood Demon Art with the enhanced version:<br>- kimetsunoyaiba:blooddemonart_nezuko -> kimetsunoyaibamultiplayer:combustible_blood<br>Enhanced features:<br>- Uses the Combustible Blood technique implementation<br>- Expanded Nezuko-inspired forms and effects<br>Default: true |
| `love_whip_physics.disableLoveM1TrailParticles` | Whether or not to disable the love sword swing particles<br>Default: false |
| `love_whip_physics.segmentCount` | Number of whip segments (higher = smoother but more expensive)<br>Default: 12 |
| `love_whip_physics.segmentLength` | Rest length of each whip segment in blocks<br>Default: 0.3 |
| `love_whip_physics.stiffness` | Spring stiffness (higher = stiffer, less floppy)<br>Default: 0.4 |
| `love_whip_physics.damping` | Damping factor (higher = less bouncy)<br>Default: 0.85 |
| `love_whip_physics.gravity` | Gravity effect on whip (blocks/tickÂ²)<br>Default: 0.02 |
| `love_whip_physics.idleLength` | Whip length when idle (in blocks)<br>Default: 2.5 |
| `love_whip_physics.extendedLength` | Whip length when fully extended during attacks (in blocks)<br>Default: 8.0 |
| `love_whip_physics.extensionTicks` | Ticks to transition between idle and extended length<br>Default: 10 |
| `love_whip_physics.curveResolution` | Number of interpolation samples along curve (higher = smoother)<br>Default: 24 |
| `love_whip_physics.width` | Whip ribbon width<br>Default: 0.08 |
| `love_whip_physics.emissive` | Enable emissive (glowing) rendering for whip<br>Default: true |
| `love_whip_physics.emissiveBrightness` | Emissive layer brightness (0.0 - 1.0)<br>Default: 0.9 |
| `love_whip_physics.particleDensity` | Particle spawn density (particles per block of whip length per tick)<br>0 = disabled, higher = more particles<br>Default: 2 |
| `love_whip_physics.particleIdle` | Spawn particles when whip is idle (not attacking)<br>Default: false |
| `love_whip_physics.keyframeBlend` | Blend factor between keyframe animation and physics (0.0 = pure physics, 1.0 = pure keyframe)<br>Default: 0.7 |

### EnhancedSpawnConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EnhancedSpawnConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/enhanced_spawning.toml`

| Option path | Description |
| --- | --- |
| `enhanced_spawning.enhanced_spawning_rules` | Master switch for enhanced spawning rules.<br>If true, uses complex biome/structure/dimension rules.<br>If false, falls back to simple spawn priority system. |
| `enhanced_spawning.replace_base_generic_demon_slayers` | Replace base mod generic demon slayers with kimetsunoyaibamultiplayer demon slayers.<br>Replaces:<br>- kimetsunoyaiba:demon_slayer -> multiplayer demon slayer (level 0-3)<br>- kimetsunoyaiba:dice_steak_senior -> multiplayer demon slayer (level 4)<br>- kimetsunoyaiba:dice_steak_senior_super -> multiplayer demon slayer (level 5) |
| `enhanced_spawning.replace_base_nezuko` | Replace base mod Nezuko with kimetsunoyaibamultiplayer Nezuko.<br>Replaces:<br>- kimetsunoyaiba:nezuko -> kimetsunoyaibamultiplayer:nezuko |
| `enhanced_spawning.prevent_yorichi_type_0_natural_spawns` | Prevent Yorichi Type 0 from spawning naturally.<br>When enabled, kimetsunoyaiba:yorichi_0 is blocked from natural spawning anywhere.<br>Commands, spawn eggs, and other manual spawn paths are unaffected. |
| `enhanced_spawning.generic_spawn_rates.generic_demon_spawn_rate` | Spawn rate multiplier for generic demons outside designated areas (0.0-1.0).<br>0.4 = 40% of normal spawn rate, 1.0 = 100% normal rate, 0.0 = never spawn |
| `enhanced_spawning.generic_spawn_rates.generic_demon_slayer_spawn_rate` | Spawn rate multiplier for demon slayers outside designated areas (0.0-1.0).<br>0.1 = 10% of normal spawn rate |
| `enhanced_spawning.max_entity_tracking.max_entity_check_radius` | Radius in blocks to check for maximum entity limits.<br>Used to prevent multiple unique entities from spawning too close together. |
| `enhanced_spawning.protective_spawning.enable_protective_spawning` | Enable protective spawning mechanics |
| `enhanced_spawning.protective_spawning.hashira_spawn_chance` | Chance for a hashira to spawn when a civilian sees/is attacked by a twelve kizuki (0.0-1.0) |
| `enhanced_spawning.protective_spawning.kamaboko_spawn_chance` | Chance for a kamaboko member to spawn when a civilian sees/is attacked by a twelve kizuki (0.0-1.0) |
| `enhanced_spawning.protective_spawning.demon_slayer_spawn_chance` | Chance for demon slayers to spawn when a civilian sees/is attacked by a demon (0.0-1.0) |
| `enhanced_spawning.protective_spawning.demon_slayer_group_size_min` | Minimum number of demon slayers to spawn in a protective spawn |
| `enhanced_spawning.protective_spawning.demon_slayer_group_size_max` | Maximum number of demon slayers to spawn in a protective spawn |
| `enhanced_spawning.entity_replacer.enable_kimetsu_replacer` | Enable replacement of kimetsunoyaiba entities with kimetsu mod entities<br>Requires kimetsu mod to be installed<br>Currently only replaces: nakime -> nakime_spawn_egg |
| `enhanced_spawning.entity_replacer.enable_kimetsu_movie_replacer` | Enable replacement with kimetsu mod movie versions<br>Requires kimetsu mod to be installed<br>Replaces: nakime, daki, gyutaro, kaigaku, gyokko, zohakuten, akaza, doma, kokushibo, himejima, kocho |
| `enhanced_spawning.mt_fujikasane_dimension.demon_spawn_center_x` | X coordinate of demon spawn center in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.demon_spawn_center_y` | Y coordinate of demon spawn center in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.demon_spawn_center_z` | Z coordinate of demon spawn center in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.demon_spawn_radius` | Radius around demon spawn center where demons can spawn |
| `enhanced_spawning.mt_fujikasane_dimension.kamaboko_spawn_x` | X coordinate of kamaboko spawn area in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.kamaboko_spawn_y` | Y coordinate of kamaboko spawn area in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.kamaboko_spawn_z` | Z coordinate of kamaboko spawn area in Mt Fujikasane |
| `enhanced_spawning.mt_fujikasane_dimension.kamaboko_spawn_radius` | Radius around kamaboko spawn point where kamaboko can spawn |
| `enhanced_spawning.entity_replacer.replace_nakime` | Enable replacement for nakime |
| `enhanced_spawning.entity_replacer.replace_daki` | Enable replacement for daki |
| `enhanced_spawning.entity_replacer.replace_gyutaro` | Enable replacement for gyutaro |
| `enhanced_spawning.entity_replacer.replace_kaigaku` | Enable replacement for kaigaku |
| `enhanced_spawning.entity_replacer.replace_gyokko` | Enable replacement for gyokko |
| `enhanced_spawning.entity_replacer.replace_zohakuten` | Enable replacement for zohakuten |
| `enhanced_spawning.entity_replacer.replace_akaza` | Enable replacement for akaza |
| `enhanced_spawning.entity_replacer.replace_doma` | Enable replacement for doma |
| `enhanced_spawning.entity_replacer.replace_kokushibo` | Enable replacement for kokushibo |
| `enhanced_spawning.entity_replacer.replace_himejima` | Enable replacement for himejima |
| `enhanced_spawning.entity_replacer.replace_kocho` | Enable replacement for kocho |

### EntityConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/EntityConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/entities.toml`

| Option path | Description |
| --- | --- |
| `entities.kasugai_crow.enhancements-enabled` | Enable all kasugai crow enhancements (master toggle) |
| `entities.kasugai_crow.flying-dodge-enabled` | Enable the flying dodge mechanic where tamed crows fly away from danger |
| `entities.kasugai_crow.flight-height` | Height in blocks the crow flies to when dodging (above its current position) |
| `entities.kasugai_crow.flight-duration` | How long the crow stays flying in ticks (20 ticks = 1 second) |
| `entities.kasugai_crow.circle-radius` | Radius in blocks of the circular flying pattern |
| `entities.kasugai_crow.quest-arrow-enabled` | Enable particle arrow pointing to quest locations when crow gives a quest |
| `entities.kasugai_crow.waypoint-enabled` | Enable waypoint marker at quest target locations |
| `entities.kasugai_crow.arrow-update-interval` | How often to update the quest arrow particles in ticks (lower = smoother but more particles) |
| `entities.kasugai_crow.arrow-length` | Length of the quest arrow in blocks |
| `entities.kasugai_crow.waypoint-duration` | How long waypoint markers last in ticks (20 ticks = 1 second) |
| `entities.kasugai_crow.waypoint-complete-distance` | Distance in blocks (X/Z only, Y is ignored) to complete waypoint |
| `entities.kasugai_crow.auto-detect-quests` | Automatically detect crow quest messages in chat and set waypoints |
| `entities.kasugai_crow.quest_familiars.kasugai-crow-damage-immune` | Make kasugai crows immune to damage |
| `entities.kasugai_crow.quest_familiars.orochi-damage-immune` | Make Orochi familiars immune to damage |
| `entities.kasugai_crow.quest_familiars.eye-familiar-damage-immune` | Make Eye Familiar entities immune to damage |
| `entities.kizuki_fear.enabled` | Enable Kokushibo and Akaza fear auras and aggro fear |

### FinalSelectionRaidConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/FinalSelectionRaidConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/final_selection_raids.toml`

| Option path | Description |
| --- | --- |
| `final_selection_raids.enable_final_selection_raid` | Enable Final Selection raid logic |
| `final_selection_raids.raid_radius` | Bossbar participation radius around raid center |
| `final_selection_raids.night_duration_seconds` | Duration of one Final Selection night for non-boss waves |
| `final_selection_raids.day_duration_seconds` | Duration of daytime break between nights |
| `final_selection_raids.sunrise_acceleration_seconds` | Duration for accelerated midnight->sunrise after boss death |
| `final_selection_raids.entity_spawn_near_player_radius` | Maximum distance from a candidate for non-boss spawns |
| `final_selection_raids.enable_boss_arrow` | Enable temporary boss direction arrow |
| `final_selection_raids.boss_glow_duration_ticks` | Boss glow duration on spawn (ticks) |
| `final_selection_raids.boss_arrow_duration_ticks` | Boss arrow duration on spawn (ticks) |
| `final_selection_raids.boss_escort_respawn_interval_seconds` | While boss alive, how often the escort cycle repeats |
| `final_selection_raids.boss_escort_spawn_step_seconds` | Delay between each escort spawn step in a cycle; if 0, uses interval/4 |
| `final_selection_raids.max_demons_per_player` | Maximum non-boss demons allowed per player in Mt Fujikasane during Final Selection |
| `final_selection_raids.allow_vanilla_monsters_in_final_selection` | Allow vanilla non-demon hostile mobs to spawn naturally during Final Selection when they are not replaced by demons |
| `final_selection_raids.enable_daytime_passive_animals` | Spawn passive animals during daytime breaks between nights |
| `final_selection_raids.daytime_passive_animals_per_player_max` | Maximum passive animals spawned per player during each daytime break |
| `final_selection_raids.daytime_passive_animal_spawn_radius` | Radius around each player for daytime passive animal spawns |

### ParticleConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/ParticleConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/particles.toml`

| Option path | Description |
| --- | --- |
| `particles.sword-particles-enabled` | Enable sword particle effects during kimetsunoyaiba animations |
| `particles.sword-particles-for-other-entities` | Enable sword particle effects for other biped-based entities that perform animations (only if they are close enough to the player for particles to render). |
| `particles.particle-trigger-mode` | When to spawn sword particles: ATTACK_ONLY = only during normal attacks (left click), ALL_ANIMATIONS = during any kimetsunoyaiba animation including special moves |
| `particles.animation.angle-increment` | Angle increment in degrees per step during particle arc animation |
| `particles.animation.steps-per-tick` | Number of angle increments to process per tick (higher = faster animation) |
| `particles.animation.arc-degrees` | Total arc length in degrees for sword swing animations |
| `particles.appearance.radial-layers` | Number of radial layers in the particle ribbon (more = thicker) |
| `particles.appearance.base-radius` | Base radius distance from player center |
| `particles.appearance.radius-increment` | Distance between each radial layer |
| `particles.appearance.particles-per-position` | Number of particles to spawn at each calculated position |
| `particles.appearance.max-particles-per-tick` | Maximum total particles to spawn per tick (0 = unlimited) |
| `particles.mappings.particle-mappings` | Particle mappings in format 'item_id:particle_type[:size:red:green:blue]'<br>For dust and energy particles, add size (0.1-2.0) and RGB values (0.0-1.0)<br>Energy particles drift in random direction and shrink before despawning<br>Examples:<br>'kimetsunoyaiba:nichirinsword_thunder:minecraft:dust:1.2:1.0:1.0:0.2'<br>'kimetsunoyaiba:nichirinsword_kanroji:kimetsunoyaibamultiplayer:energy:0.9:1.0:0.9:0.9'<br>'kimetsunoyaiba:nichirinsword_water:kimetsunoyaiba:particle_blue_smoke'<br>'minecraft:diamond_sword:minecraft:enchanted_hit' |

### RaidConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/RaidConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/raids.toml`

| Option path | Description |
| --- | --- |
| `raids.switches.enable_raids` | Enable raid system (fallback if gamerule not available) |
| `raids.switches.enable_debug_logging` | Enable debug logging for raid events |
| `raids.spawn_radii.small_structure_spawn_radius` | Spawn radius for small structures (houses) |
| `raids.spawn_radii.medium_structure_spawn_radius` | Spawn radius for medium structures (temples, trains) |
| `raids.spawn_radii.large_structure_spawn_radius` | Spawn radius for large structures (villages) |
| `raids.timing.wave_preparation_time` | Time between waves (preparation phase) |
| `raids.timing.entity_spawn_interval` | Time between individual entity spawns (prevents lag) |
| `raids.timing.raid_timeout` | Maximum raid duration before automatic defeat (30 minutes) |
| `raids.timing.raid_abandonment_timeout` | Time before raid is cancelled when all players leave area (2 minutes) |
| `raids.timing.raid_abandonment_warning_interval` | Time between abandonment warnings (30 seconds) |
| `raids.participation.raid_participation_radius` | Radius for raid participation and boss bar display |
| `raids.rewards.enable_omen_potion_rewards` | Enable omen potion rewards for completing raids |
| `raids.rewards.omen_potion_same_level_chance` | Chance to receive same level omen potion (vs +1 level) |
| `raids.entity_restrictions.disable_yoriichi` | Disable Yoriichi from spawning in demon slayer raids |
| `raids.entity_restrictions.disable_yoriichi_old` | Disable Old Yoriichi from spawning in demon slayer raids |
| `raids.entity_restrictions.enable_kimetsu_addon_entities` | Enable kimetsu addon mod entities in raids<br>When true, entities from the 'kimetsu' mod can spawn in raids<br>This includes additional upper moons, hashira, and other powerful entities<br>Only works if the kimetsu addon mod is installed |
| `raids.mugen_door.enable_mugen_door_teleportation` | Enable mugen door teleportation to Mugen Castle<br>When enabled, players within 2 blocks of a mugen door will be teleported to the Mugen Castle dimension |
| `raids.containment.small_structure_containment_radius` | Containment radius for small structures (houses) |
| `raids.containment.medium_structure_containment_radius` | Containment radius for medium structures (temples, trains) |
| `raids.containment.large_structure_containment_radius` | Containment radius for large structures (villages) |
| `raids.containment.containment_push_back_distance` | Distance inside boundary to push entities back to |
| `raids.containment.containment_teleport_threshold` | Distance beyond boundary that triggers teleportation instead of push-back |
| `raids.containment.containment_check_interval` | Ticks between containment boundary checks (20 ticks = 1 second) |

### SpawnRateConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SpawnRateConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/spawn_rates.toml`

| Option path | Description |
| --- | --- |
| `spawn_priority.kimetsunoyaiba_akaza_priority` | Spawn priority for kimetsunoyaiba:akaza (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_boar_priority` | Spawn priority for kimetsunoyaiba:boar (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_boar_golden_priority` | Spawn priority for kimetsunoyaiba:boar_golden (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_daki_priority` | Spawn priority for kimetsunoyaiba:daki (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon2_priority` | Spawn priority for kimetsunoyaiba:demon2 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon3_priority` | Spawn priority for kimetsunoyaiba:demon3 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon4_priority` | Spawn priority for kimetsunoyaiba:demon4 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon5_priority` | Spawn priority for kimetsunoyaiba:demon5 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon6_priority` | Spawn priority for kimetsunoyaiba:demon6 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon7_priority` | Spawn priority for kimetsunoyaiba:demon7 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon8_priority` | Spawn priority for kimetsunoyaiba:demon8 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon9_priority` | Spawn priority for kimetsunoyaiba:demon9 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon10_priority` | Spawn priority for kimetsunoyaiba:demon10 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_demon_priority` | Spawn priority for kimetsunoyaiba:demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_dice_steak_senior_priority` | Spawn priority for kimetsunoyaiba:dice_steak_senior (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_dice_steak_senior_demon_priority` | Spawn priority for kimetsunoyaiba:dice_steak_senior_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_dice_steak_senior_golden_priority` | Spawn priority for kimetsunoyaiba:dice_steak_senior_golden (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_dice_steak_senior_super_priority` | Spawn priority for kimetsunoyaiba:dice_steak_senior_super (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_doctor_priority` | Spawn priority for kimetsunoyaiba:doctor (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_doma_priority` | Spawn priority for kimetsunoyaiba:doma (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_enko_priority` | Spawn priority for kimetsunoyaiba:enko (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_enmu_priority` | Spawn priority for kimetsunoyaiba:enmu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_genya_priority` | Spawn priority for kimetsunoyaiba:genya (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_goldfishbig_priority` | Spawn priority for kimetsunoyaiba:goldfishbig (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_grandmother_priority` | Spawn priority for kimetsunoyaiba:grandmother (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_gyokko_priority` | Spawn priority for kimetsunoyaiba:gyokko (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_gyutaro_priority` | Spawn priority for kimetsunoyaiba:gyutaro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_haganeduka_priority` | Spawn priority for kimetsunoyaiba:haganeduka (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_hairo_priority` | Spawn priority for kimetsunoyaiba:hairo (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_hakuji_priority` | Spawn priority for kimetsunoyaiba:hakuji (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_hand_demon_priority` | Spawn priority for kimetsunoyaiba:hand_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_hantengu_priority` | Spawn priority for kimetsunoyaiba:hantengu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_hasu_priority` | Spawn priority for kimetsunoyaiba:hasu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_himejima_priority` | Spawn priority for kimetsunoyaiba:himejima (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_honoikaduchi_priority` | Spawn priority for kimetsunoyaiba:honoikaduchi (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_iguro_priority` | Spawn priority for kimetsunoyaiba:iguro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_inosuke_priority` | Spawn priority for kimetsunoyaiba:inosuke (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kaigaku_priority` | Spawn priority for kimetsunoyaiba:kaigaku (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kaigaku_human_priority` | Spawn priority for kimetsunoyaiba:kaigaku_human (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kakushi_priority` | Spawn priority for kimetsunoyaiba:kakushi (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kamanue_priority` | Spawn priority for kimetsunoyaiba:kamanue (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kanae_priority` | Spawn priority for kimetsunoyaiba:kanae (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kanawo_priority` | Spawn priority for kimetsunoyaiba:kanawo (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kanroji_priority` | Spawn priority for kimetsunoyaiba:kanroji (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kasugai_crow_priority` | Spawn priority for kimetsunoyaiba:kasugai_crow (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kocho_priority` | Spawn priority for kimetsunoyaiba:kocho (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kokushibo_priority` | Spawn priority for kimetsunoyaiba:kokushibo (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kokushirinten_priority` | Spawn priority for kimetsunoyaiba:kokushirinten (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kotetsu_priority` | Spawn priority for kimetsunoyaiba:kotetsu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kuwajima_priority` | Spawn priority for kimetsunoyaiba:kuwajima (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_kyogai_priority` | Spawn priority for kimetsunoyaiba:kyogai (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_makomo_priority` | Spawn priority for kimetsunoyaiba:makomo (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_masachika_priority` | Spawn priority for kimetsunoyaiba:masachika (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_michikatsu_priority` | Spawn priority for kimetsunoyaiba:michikatsu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_mob_slayer_priority` | Spawn priority for kimetsunoyaiba:mob_slayer (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_muichirou_priority` | Spawn priority for kimetsunoyaiba:muichirou (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_mukago_priority` | Spawn priority for kimetsunoyaiba:mukago (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_murata_priority` | Spawn priority for kimetsunoyaiba:murata (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_muscular_mouse_priority` | Spawn priority for kimetsunoyaiba:muscular_mouse (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_muzan_priority` | Spawn priority for kimetsunoyaiba:muzan (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_nakime_priority` | Spawn priority for kimetsunoyaiba:nakime (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_nezuko_priority` | Spawn priority for kimetsunoyaiba:nezuko (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_pandan_priority` | Spawn priority for kimetsunoyaiba:pandan (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rengoku_priority` | Spawn priority for kimetsunoyaiba:rengoku (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rokuro_priority` | Spawn priority for kimetsunoyaiba:rokuro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_brother_priority` | Spawn priority for kimetsunoyaiba:rui_brother (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_priority` | Spawn priority for kimetsunoyaiba:rui (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_father_priority` | Spawn priority for kimetsunoyaiba:rui_father (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_human_priority` | Spawn priority for kimetsunoyaiba:rui_human (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_mother_priority` | Spawn priority for kimetsunoyaiba:rui_mother (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_rui_sister_priority` | Spawn priority for kimetsunoyaiba:rui_sister (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_sabito_priority` | Spawn priority for kimetsunoyaiba:sabito (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_inosuke_priority` | Spawn priority for kimetsunoyaiba:school_inosuke (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_kocho_priority` | Spawn priority for kimetsunoyaiba:school_kocho (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_nezuko_priority` | Spawn priority for kimetsunoyaiba:school_nezuko (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_tanjiro_priority` | Spawn priority for kimetsunoyaiba:school_tanjiro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_tomioka_priority` | Spawn priority for kimetsunoyaiba:school_tomioka (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_school_zenitsu_priority` | Spawn priority for kimetsunoyaiba:school_zenitsu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_shinazugawa_priority` | Spawn priority for kimetsunoyaiba:shinazugawa (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_spider_demon_priority` | Spawn priority for kimetsunoyaiba:spider_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_suigokubati_priority` | Spawn priority for kimetsunoyaiba:suigokubati (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_suirenbosatsu_priority` | Spawn priority for kimetsunoyaiba:suirenbosatsu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_susamaru_priority` | Spawn priority for kimetsunoyaiba:susamaru (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_swamp_demon_priority` | Spawn priority for kimetsunoyaiba:swamp_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_tamayo_priority` | Spawn priority for kimetsunoyaiba:tamayo (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_tanjiro_demon_priority` | Spawn priority for kimetsunoyaiba:tanjiro_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_tanjiro_priority` | Spawn priority for kimetsunoyaiba:tanjiro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_temple_demon_priority` | Spawn priority for kimetsunoyaiba:temple_demon (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_tomioka_priority` | Spawn priority for kimetsunoyaiba:tomioka (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_toyosan_priority` | Spawn priority for kimetsunoyaiba:toyosan (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_ubuyashiki_priority` | Spawn priority for kimetsunoyaiba:ubuyashiki (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_urokodaki_priority` | Spawn priority for kimetsunoyaiba:urokodaki (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_uzui_priority` | Spawn priority for kimetsunoyaiba:uzui (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_wakuraba_priority` | Spawn priority for kimetsunoyaiba:wakuraba (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yachan_brother_priority` | Spawn priority for kimetsunoyaiba:yachan_brother (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yachan_priority` | Spawn priority for kimetsunoyaiba:yachan (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yahaba_priority` | Spawn priority for kimetsunoyaiba:yahaba (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yorichi_0_priority` | Spawn priority for kimetsunoyaiba:yorichi_0 (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yoriichi_priority` | Spawn priority for kimetsunoyaiba:yoriichi (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yoriichi_old_priority` | Spawn priority for kimetsunoyaiba:yoriichi_old (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_yushiro_priority` | Spawn priority for kimetsunoyaiba:yushiro (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_zennitsu_priority` | Spawn priority for kimetsunoyaiba:zennitsu (0 = never, 50 = 50% chance, 100 = always) |
| `spawn_priority.kimetsunoyaiba_zohakuten_priority` | Spawn priority for kimetsunoyaiba:zohakuten (0 = never, 50 = 50% chance, 100 = always) |

### SurvivalRaidConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SurvivalRaidConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/survival_raids.toml`

| Option path | Description |
| --- | --- |
| `survival_raids.enable_survival_raids` | Enable survival raid system |
| `survival_raids.wave_preparation_time` | Preparation time before each wave starts (seconds) |
| `survival_raids.entity_spawn_interval` | Delay between each entity spawn (seconds) |
| `survival_raids.wave_interval` | Base interval between waves/reinforcements (seconds) |
| `survival_raids.default_radius` | Default raid radius used by command |
| `survival_raids.boss_spawn_radius` | Boss spawn radius around raid center |
| `survival_raids.entity_spawn_near_player_radius` | Maximum distance from raid players for non-boss spawns |
| `survival_raids.enable_boss_arrow` | Enable temporary boss direction arrow effect |
| `survival_raids.boss_glow_duration` | Boss glow duration on spawn (ticks) |
| `survival_raids.boss_arrow_duration` | Boss arrow duration on spawn (ticks) |

### SwordDisplayConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordDisplayConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/sword_display.toml`

| Option path | Description |
| --- | --- |
| `sword_display.enabled` | Enable displaying swords on player model when not actively held |
| `sword_display.default_position` | Default position for swords on the player model (HIP or BACK). Per-sword overrides below take precedence. |
| `sword_display.sword_position_overrides` | Per-sword position overrides. Format: 'modid:itemname=POSITION' (e.g., 'kimetsunoyaiba:nichirinsword_uzui=BACK') |
| `sword_display.scale` | Scale of displayed swords (0.5 = half size, 1.0 = normal size, 2.0 = double size) |
| `sword_display.sheath_scale` | Additional scale multiplier for sheaths (applied on top of sword scale)<br>Example: sword scale=0.5, sheath scale=1.2 -> sheath displays at 0.6 (0.5 * 1.2) |
| `sword_display.render_sheaths` | Enable rendering sword sheaths on the hip/back |
| `sword_display.draw_sheath_animations` | Enable draw and sheath animations when swords are drawn from or returned to sheaths |
| `sword_display.hip_position.left_translate_x` | Left hip X translation |
| `sword_display.hip_position.left_translate_y` | Left hip Y translation |
| `sword_display.hip_position.left_translate_z` | Left hip Z translation |
| `sword_display.hip_position.left_rotate_z` | Left hip Z rotation (degrees) |
| `sword_display.hip_position.left_rotate_y` | Left hip Y rotation (degrees) |
| `sword_display.hip_position.left_rotate_x` | Left hip X rotation (degrees) |
| `sword_display.hip_position.right_translate_x` | Right hip X translation |
| `sword_display.hip_position.right_translate_y` | Right hip Y translation |
| `sword_display.hip_position.right_translate_z` | Right hip Z translation |
| `sword_display.hip_position.right_rotate_z` | Right hip Z rotation (degrees) |
| `sword_display.hip_position.right_rotate_y` | Right hip Y rotation (degrees) |
| `sword_display.hip_position.right_rotate_x` | Right hip Z rotation (degrees) |
| `sword_display.back_position.left_translate_x` | Left back X translation |
| `sword_display.back_position.left_translate_y` | Left back Y translation |
| `sword_display.back_position.left_translate_z` | Left back Z translation |
| `sword_display.back_position.left_rotate_z` | Left back Z rotation (degrees) |
| `sword_display.back_position.left_rotate_y` | Left back Y rotation (degrees) |
| `sword_display.back_position.left_rotate_x` | Left back X rotation (degrees) |
| `sword_display.back_position.right_translate_x` | Right back X translation |
| `sword_display.back_position.right_translate_y` | Right back Y translation |
| `sword_display.back_position.right_translate_z` | Right back Z translation |
| `sword_display.back_position.right_rotate_z` | Right back Z rotation (degrees) |
| `sword_display.back_position.right_rotate_y` | Right back Y rotation (degrees) |
| `sword_display.back_position.right_rotate_x` | Right back X rotation (degrees) |
| `sword_display.entity_display.back.translation.offset_x` | Entity translation X offset (added after optional flip) |
| `sword_display.entity_display.back.translation.offset_y` | Entity translation Y offset (added after optional flip) |
| `sword_display.entity_display.back.translation.offset_z` | Entity translation Z offset (added after optional flip) |
| `sword_display.entity_display.back.translation.flip_x` | Flip entity translation X (multiply base by -1) |
| `sword_display.entity_display.back.translation.flip_y` | Flip entity translation Y (multiply base by -1) |
| `sword_display.entity_display.back.translation.flip_z` | Flip entity translation Z (multiply base by -1) |
| `sword_display.entity_display.back.rotation.offset_x` | Entity rotation X offset in degrees (added after optional flip) |
| `sword_display.entity_display.back.rotation.offset_y` | Entity rotation Y offset in degrees (added after optional flip) |
| `sword_display.entity_display.back.rotation.offset_z` | Entity rotation Z offset in degrees (added after optional flip) |
| `sword_display.entity_display.back.rotation.flip_x` | Flip entity rotation X (multiply base by -1) |
| `sword_display.entity_display.back.rotation.flip_y` | Flip entity rotation Y (multiply base by -1) |
| `sword_display.entity_display.back.rotation.flip_z` | Flip entity rotation Z (multiply base by -1) |
| `sword_display.entity_display.hip.translation.offset_x` | Entity translation X offset (added after optional flip) |
| `sword_display.entity_display.hip.translation.offset_y` | Entity translation Y offset (added after optional flip) |
| `sword_display.entity_display.hip.translation.offset_z` | Entity translation Z offset (added after optional flip) |
| `sword_display.entity_display.hip.translation.flip_x` | Flip entity translation X (multiply base by -1) |
| `sword_display.entity_display.hip.translation.flip_y` | Flip entity translation Y (multiply base by -1) |
| `sword_display.entity_display.hip.translation.flip_z` | Flip entity translation Z (multiply base by -1) |
| `sword_display.entity_display.hip.rotation.offset_x` | Entity rotation X offset in degrees (added after optional flip) |
| `sword_display.entity_display.hip.rotation.offset_y` | Entity rotation Y offset in degrees (added after optional flip) |
| `sword_display.entity_display.hip.rotation.offset_z` | Entity rotation Z offset in degrees (added after optional flip) |
| `sword_display.entity_display.hip.rotation.flip_x` | Flip entity rotation X (multiply base by -1) |
| `sword_display.entity_display.hip.rotation.flip_y` | Flip entity rotation Y (multiply base by -1) |
| `sword_display.entity_display.hip.rotation.flip_z` | Flip entity rotation Z (multiply base by -1) |

### SwordRackConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordRackConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/sword_rack.toml`

| Option path | Description |
| --- | --- |
| `sword_rack.floor.slot1.translate_x` | Local X translation in block space |
| `sword_rack.floor.slot1.translate_y` | Local Y translation in block space |
| `sword_rack.floor.slot1.translate_z` | Local Z translation in block space |
| `sword_rack.floor.slot1.rotate_x` | Rotation around X in degrees |
| `sword_rack.floor.slot1.rotate_y` | Rotation around Y in degrees |
| `sword_rack.floor.slot1.rotate_z` | Rotation around Z in degrees |
| `sword_rack.floor.slot2.translate_x` | Local X translation in block space |
| `sword_rack.floor.slot2.translate_y` | Local Y translation in block space |
| `sword_rack.floor.slot2.translate_z` | Local Z translation in block space |
| `sword_rack.floor.slot2.rotate_x` | Rotation around X in degrees |
| `sword_rack.floor.slot2.rotate_y` | Rotation around Y in degrees |
| `sword_rack.floor.slot2.rotate_z` | Rotation around Z in degrees |
| `sword_rack.floor.slot3.translate_x` | Local X translation in block space |
| `sword_rack.floor.slot3.translate_y` | Local Y translation in block space |
| `sword_rack.floor.slot3.translate_z` | Local Z translation in block space |
| `sword_rack.floor.slot3.rotate_x` | Rotation around X in degrees |
| `sword_rack.floor.slot3.rotate_y` | Rotation around Y in degrees |
| `sword_rack.floor.slot3.rotate_z` | Rotation around Z in degrees |
| `sword_rack.wall.slot1.translate_x` | Local X translation in block space |
| `sword_rack.wall.slot1.translate_y` | Local Y translation in block space |
| `sword_rack.wall.slot1.translate_z` | Local Z translation in block space |
| `sword_rack.wall.slot1.rotate_x` | Rotation around X in degrees |
| `sword_rack.wall.slot1.rotate_y` | Rotation around Y in degrees |
| `sword_rack.wall.slot1.rotate_z` | Rotation around Z in degrees |
| `sword_rack.wall.slot2.translate_x` | Local X translation in block space |
| `sword_rack.wall.slot2.translate_y` | Local Y translation in block space |
| `sword_rack.wall.slot2.translate_z` | Local Z translation in block space |
| `sword_rack.wall.slot2.rotate_x` | Rotation around X in degrees |
| `sword_rack.wall.slot2.rotate_y` | Rotation around Y in degrees |
| `sword_rack.wall.slot2.rotate_z` | Rotation around Z in degrees |
| `sword_rack.wall.slot3.translate_x` | Local X translation in block space |
| `sword_rack.wall.slot3.translate_y` | Local Y translation in block space |
| `sword_rack.wall.slot3.translate_z` | Local Z translation in block space |
| `sword_rack.wall.slot3.rotate_x` | Rotation around X in degrees |
| `sword_rack.wall.slot3.rotate_y` | Rotation around Y in degrees |
| `sword_rack.wall.slot3.rotate_z` | Rotation around Z in degrees |

### SwordSwingConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordSwingConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/sword_slash.toml`

| Option path | Description |
| --- | --- |
| `general.use_sword_swing_model` | Enable 3D sword slash models instead of particles<br>Set to true for animated 3D slashes, false for particle effects<br>Default: true |
| `general.model_scale` | Scale/size of the sword slash models<br>Higher values = larger slashes<br>Range: 0.1 to 5.0, Default: 0.5 |
| `general.animation_duration_ms` | Duration of slash animation in milliseconds<br>Lower values = faster slashes<br>Range: 50 to 1000, Default: 150 (3 ticks) |
| `general.brightness_multiplier` | Brightness multiplier for sword slash models<br>Higher values = brighter glow (values >1.0 create bloom with shaders)<br>Range: 1.0 to 10.0, Default: 5.0 |
| `advanced.global.yaw_offset` | Global yaw rotation offset (degrees)<br>Range: -180 to 180, Default: 0 |
| `advanced.global.pitch_offset` | Global pitch rotation offset (degrees)<br>Range: -180 to 180, Default: 0 |
| `advanced.global.roll_offset` | Global roll rotation offset (degrees)<br>Range: -180 to 180, Default: 0 |
| `advanced.global.radius_mult` | Global radius multiplier<br>Default: 0.7 |
| `advanced.sword_to_right.yaw_offset` | Yaw offset for right slash (degrees)<br>Default: 90 |
| `advanced.sword_to_right.pitch` | Pitch angle for right slash (degrees)<br>Default: 0 |
| `advanced.sword_to_right.roll` | Roll angle for right slash (degrees)<br>Default: -20 |
| `advanced.sword_to_right.arc_offset` | Arc angle offset for right slash (degrees)<br>Default: -50 |
| `advanced.sword_to_right.right_radius_mult` | Radius multiplier for right slash<br>Default: 1 |
| `advanced.sword_to_right.yaw_offset_offhand` | Yaw offset for right slash (degrees)<br>Default: 90 |
| `advanced.sword_to_right.pitch_offhand` | Pitch angle for right slash (degrees)<br>Default: 0 |
| `advanced.sword_to_right.roll_offhand` | Roll angle for right slash (degrees)<br>Default: -20 |
| `advanced.sword_to_right.arc_offset_offhand` | Arc angle offset for right slash (degrees)<br>Default: -50 |
| `advanced.sword_to_right.right_radius_mult_offhand` | Radius multiplier for right slash<br>Default: 1 |
| `advanced.sword_to_left.yaw_offset` | Yaw offset for left slash (degrees)<br>Default: -90 |
| `advanced.sword_to_left.pitch` | Pitch angle for left slash (degrees)<br>Default: 0 |
| `advanced.sword_to_left.roll` | Roll angle for left slash (degrees)<br>Default: -20 |
| `advanced.sword_to_left.arc_offset` | Arc angle offset for left slash (degrees)<br>Default: -50 |
| `advanced.sword_to_left.left_radius_mult` | Radius multiplier for left slash<br>Default: 1 |
| `advanced.sword_to_left.yaw_offset_offhand` | Yaw offset for left slash (degrees)<br>Default: -90 |
| `advanced.sword_to_left.pitch_offhand` | Pitch angle for left slash (degrees)<br>Default: 0 |
| `advanced.sword_to_left.roll_offhand` | Roll angle for left slash (degrees)<br>Default: -20 |
| `advanced.sword_to_left.arc_offset_offhand` | Arc angle offset for left slash (degrees)<br>Default: -50 |
| `advanced.sword_to_left.left_radius_mult_offhand` | Radius multiplier for left slash<br>Default: 1 |
| `advanced.sword_overhead.yaw_offset` | Yaw offset for overhead slash (degrees)<br>Default: 0 |
| `advanced.sword_overhead.pitch` | Pitch angle for overhead slash (degrees)<br>Default: -90 |
| `advanced.sword_overhead.roll` | Roll angle for overhead slash (degrees)<br>Default: 110 |
| `advanced.sword_overhead.arc_offset` | Arc angle offset for overhead slash (degrees)<br>Default: -20 |
| `advanced.sword_overhead.overhead_radius_mult` | Radius multiplier for overhead slash<br>Default: 1 |
| `advanced.sword_overhead.yaw_offset_offhand` | Yaw offset for overhead slash (degrees)<br>Default: 0 |
| `advanced.sword_overhead.pitch_offhand` | Pitch angle for overhead slash (degrees)<br>Default: -90 |
| `advanced.sword_overhead.roll_offhand` | Roll angle for overhead slash (degrees)<br>Default: 110 |
| `advanced.sword_overhead.arc_offset_offhand` | Arc angle offset for overhead slash (degrees)<br>Default: -20 |
| `advanced.sword_overhead.overhead_radius_mult_offhand` | Radius multiplier for overhead slash<br>Default: 1 |
| `advanced.sword_to_upper.yaw_offset` | Yaw offset for upward slash (degrees)<br>Default: 0 |
| `advanced.sword_to_upper.pitch` | Pitch angle for upward slash (degrees)<br>Default: 90 |
| `advanced.sword_to_upper.roll` | Roll angle for upward slash (degrees)<br>Default: 90 |
| `advanced.sword_to_upper.arc_offset` | Arc angle offset for upward slash (degrees)<br>Default: 0 |
| `advanced.sword_to_upper.upper_radius_mult` | Radius multiplier for upper slash<br>Default: 1 |
| `advanced.sword_rotate.yaw_offset` | Yaw offset for spin attack (degrees)<br>Default: -90 |
| `advanced.sword_rotate.pitch` | Pitch angle for spin attack (degrees)<br>Default: 0 |
| `advanced.sword_rotate.roll` | Roll angle for spin attack (degrees)<br>Default: 0 |
| `advanced.sword_rotate.arc_offset` | Arc angle offset for spin attack (degrees)<br>Default: 50 |
| `advanced.sword_rotate.rotate_radius_mult` | Radius multiplier for spin attack<br>Default: 1 |

### SwordsmithVillageConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordsmithVillageConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/swordsmith_village.toml`

| Option path | Description |
| --- | --- |
| `maxPopulation` | Maximum stored population target for the Swordsmith Village dimension. |
| `noonRecoveryChance` | Chance at each noon to recover 1 stored population if below the configured maximum. |

### VariationConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/VariationConfig.java`
- Config file: `config/kimetsunoyaibamultiplayer/variations.toml`

| Option path | Description |
| --- | --- |
| `variations.variationBlacklist` | Blacklist of disabled variation IDs<br>Variations are ENABLED by default. Add IDs here to DISABLE specific variations.<br>Variation ID format: "styleId:formIndex:variationIndex"<br>Examples:<br>- "mist_breathing:0:1" = First variation of Mist Breathing First Form<br>- "mist_breathing:1:2" = Second variation of Mist Breathing Second Form<br>To disable all variations for a form, blacklist all indices:<br>["mist_breathing:0:1", "mist_breathing:0:2", "mist_breathing:0:3"]<br>Default: [] (all variations enabled) |

### GravityConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/GravityConfig.java`
- Config file: Not registered as a Forge config file in `KimetsunoyaibaMultiplayer.java`

| Option path | Description |
| --- | --- |
| `enable-gravity-changing` | Master switch for KNY gravity changing. When false, public gravity helpers return vanilla-safe values and field sources are inert. |
| `rotation-time-ms` | Default visual rotation time for gravity changes, in milliseconds. |
| `gravity-strength-multiplier` | Multiplier for custom gravity acceleration. |
| `reset-gravity-on-respawn` | Reset player base gravity to DOWN after death respawn. |
| `adjust-position-after-changing-gravity` | Reserved for the full movement/collision mixin layer. |
| `max-field-range` | Maximum gravity projector field range. |
| `field-debug-render` | Enable client-side gravity field debug rendering when implemented. |

### MitsuriSwordConfig

- Source: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/MitsuriSwordConfig.java`
- Config file: Not registered as a Forge config file in `KimetsunoyaibaMultiplayer.java`

| Option path | Description |
| --- | --- |
| `mitsuri_sword.curveResolution` | Curve resolution (segments) |
