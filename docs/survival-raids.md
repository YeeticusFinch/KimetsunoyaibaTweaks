Survival Raid System - Implementation Plan   

 Context  

 This is a new "Survival" raid type that is completely independent from the existing omen-triggered raids.     
 Survival raids feature unlimited waves cycling in a 3-wave pattern (easy → mixed → boss → repeat) for level 1, 4-wave pattern (easy → mixed → mixed → boss → repeat) for level 2, up to a 7-wave pattern (mixed medium+hard → mixed medium+hard → mixed medium+easy (but large swarms) → hard → medium boss → hard boss → demon king boss → repeat) for level 5, end at sunrise, scale per player count, and are triggered via commands or programmatically. The boss bar shows a "Sunrise Countdown" instead of combined health. Victory requires defeating at least 1 boss before dawn.

 New Files (6 files)

 1. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SurvivalRaidConfig.java

 - ForgeConfigSpec with: enableSurvivalRaids, wavePreparationTime (8s default), entitySpawnInterval (1s),   
 defaultRadius (200), bossSpawnRadius (100), entitySpawnNearPlayerRadius (200), enableBossArrow (true),     
 bossGlowDuration (100 ticks), bossArrowDuration (100 ticks)      

 2. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/SurvivalWaveGenerator.java    

 - generateWave(WaveType, difficulty, playerCount) returns List<ResourceLocation> 
 For difficulty level 1:
 - EASY wave: 6 easy demons per player (at diff 4+, some upgrade to medium)       
 - MIXED wave: 3 easy + 3 medium per player (tiers upgrade at higher diff)        
 - BOSS wave: 1 boss entity (NOT per player) + 2 medium + 2 easy per player       

 For difficulty level 2:
 - EASY wave: 6 easy demons per player
 - MIXED wave: 5 easy demons and 5 medium demons per player
 - MIXED wave: 6 easy demons and 6 medium demons per player
 - BOSS wave: 2 hard demons / easy boss demons as the bossfight (two bosses, not per player), with 4 medium demons and 4 easy demons per player

The amount of waves before a repeat should be equal to the difficulty level + 2.

- Boss tier scales: diff 1→HARD_DEMON, diff 2-3→MEDIUM_BOSS_DEMON (lower kizuki), diff 4-5→HARD_BOSS_DEMON (upper  
 kizuki)  
- Reuses EntityCategorization.getEntitiesForScale() + random selection (same pattern as WaveGenerator.pick()) 

 3. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/SurvivalRaid.java (~700 lines)

 Core raid class. Key design: 

 State machine: PREPARING → ACTIVE → VICTORY/DEFEAT     

 Fields:  
 - UUID raidId, ServerLevel level, BlockPos center, int radius, int difficultyLevel   
 - int currentWave (increments indefinitely), Deque<ResourceLocation> pendingSpawns   
 - Set<UUID> aliveEntities, Set<UUID> allRaidEntities, Set<UUID> aliveBosses      
 - int bossesDefeated (victory requires >= 1) 
 - ServerBossEvent bossBar titled "Sunrise Countdown"   
 - int bossPendingCount - tracks how many leading entries in pendingSpawns are bosses 
 - UUID currentBossForArrow, long bossArrowEndTime - arrow tracking     

 Constructor: Creates boss bar (PURPLE color), plays raid horn, broadcasts start message, calls scheduleNextWave()    

 tick() method:     
 1. Update boss bar players (add/remove based on distance from center)  
 2. Update sunrise progress on boss bar (full at sunset/13000, empty at sunrise/23000)
 3. Send boss arrow packets if within arrow window (every 5 ticks)
 4. Check sunrise → victory if bossesDefeated > 0, else defeat    
 5. Check if no alive non-demon players in area → defeat
 6. Delegate to tickPreparing() or tickActive()   

 updateSunriseProgress():     
 - timeOfDay = level.getDayTime() % 24000     
 - Night is 13000→23000 (10000 ticks). Progress = 1.0 - ((timeOfDay - 13000) / 10000.0)     
 - Boss bar title always shows "Sunrise Countdown"

 scheduleNextWave():
 - Determines wave type: (currentWave - 1) % 3 → 0=EASY, 1=MIXED, 2=BOSS
 - Counts non-demon players in area (min 1)   
 - Generates entities via SurvivalWaveGenerator.generateWave()    
 - For BOSS waves: sets bossPendingCount = 1, changes bossbar color to RED temporarily
 - Preparation countdown before spawning begins   
 - NOTE: the next wave will start based on a timer (not based on how many demons are still alive)
   - This timer can be configured in the config (like in the survival raids config), but it defaults to one wave every 90 seconds
   - Boss waves are the only ones that aren't timer based, the boss wave only concludes once the boss is defeated
   - If the 90 second timer (or whatever is defined in the config) elapses before the boss is defeated, then the non-boss demons associated with this wave will be spawned again, so every 90 seconds that the boss is alive, the non-boss demons will spawn

 spawnNextEntity(): 
 - Pops from pendingSpawns, checks bossPendingCount > 0 to identify boss
 - Boss spawns: within 100 blocks of center, gets applyBossEffects()    
 - Non-boss spawns: within 200 blocks of a random player in the area    
 - All entities get setPersistenceRequired(), tagged with SurvivalRaidId NBT      
 - Uses heightmap-based surface spawning (reuses pattern from KnYRaid)  
 - Kizuki bosses spawn with mugen door animation (same delayed task pattern)      

 applyBossEffects(Mob boss):  
 1. Play SoundEvents.WITHER_SPAWN at boss position (volume 64.0)  
 2. Apply MobEffects.GLOWING for 100 ticks (5 seconds)  
 3. Set currentBossForArrow = boss.getUUID(), bossArrowEndTime = gameTime + 100   
 4. Send BossArrowPacket to all players in area   
 5. Call setTargetNearestNonDemonPlayer(boss) 

 setTargetNearestNonDemonPlayer(Mob mob):     
 - Iterates level.players(), skips players with getPersistentData().getBoolean("oni") (demon players) 
 - Skips players outside raid radius
 - Sets mob.setTarget() and mob.setLastHurtByMob() on nearest     

 Wave completion: When aliveEntities.isEmpty() && pendingSpawns.isEmpty() → scheduleNextWave()        

 Victory: Sunrise + bossesDefeated > 0 → title screen, XP reward (waves * 50 + bossesDefeated * 200), despawn         
 entities 
 Defeat: Sunrise + 0 bosses, or no alive players → defeat message, despawn        

 No NBT persistence - raids last one night at most, no need to survive server restarts

 4. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/SurvivalRaidRegistry.java     

 - In-memory Map<ResourceKey<Level>, SurvivalRaid> (one raid per dimension)       
 - createRaid(), getRaid(), tickAll(), stopRaid(), onEntityKilled(), isRaidEntity()   

 5. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/network/packets/BossArrowPacket.java

 - Server→client packet with double bossX, bossY, bossZ and int remainingTicks    
 - Client handler: calls CrowQuestMarkerHandler.drawQuestArrow(player, bossPos, level) to draw END_ROD particle arrow 
  from player toward boss position  
 - Sent every 5 ticks during the 5-second arrow window  

 6. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/SurvivalRaidCommand.java   

 - /survivalraid start <level> [radius] - requires permission level 2, must be nighttime    
 - /survivalraid stop - force-stops current survival raid         
 - /survivalraid status - shows wave count, bosses defeated, alive entities, player count   
 - Registered via RegisterCommandsEvent       

 Modified Files (3 files)     

 7. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/raids/RaidTriggerHandler.java 

 - In onServerTick(): add SurvivalRaidRegistry.tickAll(level) after existing RaidRegistry.tickAll(level)    
 - In onLivingDeath(): add SurvivalRaidRegistry.onEntityKilled(level, entity.getUUID())     

 8. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/network/ModNetworking.java

 - Register BossArrowPacket using same messageBuilder pattern (at end of existing registrations)      

 9. src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/KimetsunoyaibaMultiplayer.java

 - Register SurvivalRaidConfig.SPEC as common config    
 - Register SurvivalRaidCommand in the command registration event 

 Key Design Decisions         

 1. Completely independent - no modifications to KnYRaid.java, WaveGenerator.java, or RaidConfig.java 
 2. Demon player detection via player.getPersistentData().getBoolean("oni") (confirmed pattern used in 6+ files)      
 3. Boss arrow reuses existing CrowQuestMarkerHandler.drawQuestArrow() via network packet   
 4. One survival raid per dimension - simple constraint via registry    
 5. No persistence - raid lasts one night max, simplifies implementation

 Implementation Order         

 1. SurvivalRaidConfig.java (config must exist first)   
 2. SurvivalWaveGenerator.java (standalone)   
 3. BossArrowPacket.java (standalone packet)  
 4. SurvivalRaid.java (depends on 1, 2, 3)    
 5. SurvivalRaidRegistry.java (depends on 4)  
 6. SurvivalRaidCommand.java (depends on 5)   
 7. Modify ModNetworking.java (register packet)   
 8. Modify RaidTriggerHandler.java (tick + death forwarding)      
 9. Modify KimetsunoyaibaMultiplayer.java (register config + command)   

 Verification 

 1. Build with ./gradlew build to verify compilation    
 2. In-game test: Set time to night (/time set night), run /survivalraid start 1  
 3. Verify boss bar shows "Sunrise Countdown" with decreasing progress  
 4. Verify waves cycle: easy (6 demons) → mixed (3+3 per player) → boss (1 boss + escorts)  
 5. Verify boss wave: wither spawn sound, 5s glow, particle arrow 
 6. Verify sunrise ends raid with appropriate victory/defeat      
 7. Verify /survivalraid stop and /survivalraid status work       
 8. Verify existing omen raids are completely unaffected  