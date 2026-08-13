# Meditation Menu

Developer implementation notes now live in [meditation-quest-system.md](/mnt/c/Users/carlf/eclipse-workspace-2/Kimetsunoyaiba-Multiplayer/docs/meditation-quest-system.md).

Accessed by sitting on a cushion (any color) for 2 seconds

- After 40 ticks of sitting on a cushion, a dialog in the chat will pop up asking if you wish to meditate
- There is an option for YES and an option for NO
- If the player clicks the option for YES, then the meditation menu will open

## Menu Layout

### Info Tab

At the top it shows your role (Civilian, Demon Slayer in Training, Demon Slayer, Kakushi, Swordsmith, or Demon)

- If you don't have a role, it will just show Civilian
- If you have a training sword but haven't done final selection yet, it will say Demon Slayer in Training
- If you have done final selection, it will say Demon Slayer
- If you dropped out of final selection and opted to become a Kakushi instead, it will say Kakushi
- If you are a Swordsmith, it will say swordsmith
- If you have the Demon tag (oni tag) at all, then it will say Demon

Shows current rank (either in the demon slayer corps, like Mizunoto and Hashira..., if you are a hashira it will show your hashira title (like Wind Hashira)...)

If you are a demon, it shows how much Muzan blood you have (which indicates your demon strength)

If you are a kizuki, it shows your kizuki rank

Depending on your role, it will show different stats

If you are a Demon Slayer or a Demon Slayer in Training, it shows the total number of demons you have killed, and then underneath it will show you how many kizuki demons you have killed

If you are a Demon, it will show you the total number of humans you have killed (this includes, villagers, and entities with one of the following tags: demon_slayer, tag_hashira, former_hashira, kamboko, sword_smith, civilian, woman)

- And then underneath it will show you the total number of kills you have of each kind of entity, so how many demon slayers you have killed, how many swordsmiths, how many kambokos, how many hashiras...

There might be a lot of information on this page, so we will probably need a scrollbar

### Navigation Tab

There will be two sections here, one will be quests, and the other will be locations

The player can have one thing from this page selected (either a quest or a location)

The quests are represented by the scroll.png item texture (we should make a quest item, not available in the creative inventory, it only shows up in the meditation menu on the Navigation page)

- Each quest item will have it's name be the name of the quest
  - The main quest for demon slayers is called "Cruel"
  - The main quest for demons is called "Permanence"
  - The main quest for kakushi is called "Veil"
  - The main quest for swordsmiths is called "Temper"
- For the main quests, the first line of the item's lore should be in gold and say "Main Story Quest"
- For the sidequests, the first line of the item's lore should be aqua and say "Side Quest"
- Then the next few lines should have the quest description
- Then the last line should show the quest reward(s)
  - Quest rewards can include a list of items, xp, and achievements
- We probably want a quest class such that we can create instances of this quest class
- Each quest also has some completion criteria

Cruel Quest:

Mission No.1 - Kidnapper's Bog

- Investigate the kidnappings in Northwest Town (kimestunoyaiba:village_swamp structure)
- Talk to Kazumi (this is a new entity, it uses the kazumi.png texture with the biped_civilian.geo.json geckolib model, using all the regular biped animations, and it counts as a civilian)
  - Kazumi will tell you about his fiancée Satoko that disappeared last night
  - Kazumi will bring you to the place that Satoko disappeared, telling you to follow him
  - He will lead you to a random spot in the village
  - You will wait for night time, and then a swamp demon will spawn somewhere and attack you, and you must defend Kazumi

Mission No.2 - Asakusa

Mission No.3 - Tsuzumi Mansion

Mission No.3 - Mount Natagumo

All of those missions above should be different steps of the Cruel quest

The cruel quest is only available to demon slayers

### Skills Tab

#### Passive Skills

##### Passive Skills for Demon Players

Nightvision

- when the demon player is in darkness (block light level below 5, if exposed to the sky then it must be nighttime, otherwise ignore the time of day), give the player ambient nightvision effect with no particles with a duration of 10 seconds
- Check every 20 ticks for the conditions
- If the conditions are no longer met, don't remove the nightvision, just only reapply it if the conditions are met
- Max level of 1

Regeneration

- When the demon player is not full health, but the following conditions are met, briefly grant regeneration of a level equal to the level of this skill
- Conditions for regeneration:
  - Player doesn't have full health
  - It's been X amount of seconds since the last time the player took damage
    - X = 21 subtracted by the product of this skill's level multiplied by 1.5
  - Player has more than 6 hunger bars
- Every time regeneration is applied, it will consume some hunger
- Max level of 10

Martial Arts

- When the demon player attacks with left click with their custom BDA item (BDA is shorthand for blood demon art), it will do one of the punch or kick animations, and it will deal AOE damage, just like left clicking a nichirin sword
- At level 1 this AOE does half the damage, at level 2 it does 60% of the damage, at level 3 it does 70% of the damage, and at level 5 this AOE does full damage
- This AOE also deals knockback, which will scale with the level
- Each time the demon player left clicks with their custom BDA item and does a punch or kick animation, it should spawn an impact particle 2 blocks in front of the player's head, in the direction they are looking
  - That impact particle should be given the same color as the text color of that player's custom BDA (BDA is shorthand for blood demon art)
- It should also do a sound effect, the player attack strong sound effect
- If the player has both Martial Arts and claws, each time the player left clicks it will randomly pick between a punch/kick, or a claw, it won't do both
- Max level of 5

Claws

- When the demon player attacks with left click with their custom BDA item (BDA is shorthand for blood demon art), it will do one of the following animations: sword_to_left, sword_to_right, left_sword_to_left, left_sword_to_right, sword_overhead, left_sword_overhead, and it will deal AOE damage, just like left clicking a nichirin sword
- At level 1 this AOE does half the damage, at level 2 it does 60% of the damage, at level 3 it does 70% of the damage, and at level 5 this AOE does full damage
- There is a chance that entities hit by this AOE get one of the player's target effects from their custom blood demon art (check if either of that player's blood demon art effects are target effects, and give one of them)
  - This chance starts off as 5%, and it increases by 5% each level of this claw skill
- Each time the demon player left clicks with their custom BDA item and does a sword animation, it should spawn a claw slash (claw.geo.json) exactly like spawning a sword slash for these animations for players left clicking nichirinswords (see BonePositionTracker)
  - That claw slash should be given the same color as the text color of that player's custom BDA (BDA is shorthand for blood demon art)
  - The claw slash has the claw.png texture, which is greyscale, so the color can be applied
  - Spawning this claw entity should be identical to when a player swings a nichirin sword and it spawns a sword slash entity, don't duplicate the flow, just hook in to that exact same flow, and it should already have handlers for all those animations
- It should also do a sound effect, the sweep attack sound effect
- If the player has both Martial Arts and claws, each time the player left clicks it will randomly pick between a punch/kick, or a claw, it won't do both
- Max level of 5

##### Passive Skills for Demon Slayer Players

##### Passive Skills for Kakushi Players

##### Passive Skills for Swordsmith Players
