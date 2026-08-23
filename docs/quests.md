A quest can have multiple parts to it, completing a part of a quest will grant rewards (such as XP, items, or unlocking abilities). And each part can have multiple steps.

“Quest Entity” refers to the entity that the player uses for quests, such as a Kasugai Crow, could be Princess (the poodle dog entity), the Eye Familiar…

Whenever “Player” is referenced for a dialog, it should use the player’s username

# Main story quests

## Cruel

The main story quest for Demon Slayers, focuses on destroying demons

### Mission No.1 - Kidnapper's Bog

The player obtains this quest after completing final selection

Step 1: travel to a swamp village structure
Quest Entity: “A demon lurks in the swamp! Several villagers have gone missing!”

Step 2: talk to Kazumi
Kazumi will talk about how his fiancée Satoko disappeared last night, and he will lead you to the spot that she disappeared
Kazumi: "Y-you’re a Demon Slayer… right?"
Kazumi: "Please… you have to help me!"
Kazumi: "My fiancée, Satoko… she disappeared last night… right before my eyes!"
Kazumi: "I’ll take you to where it happened… maybe you can find something I missed."
Kazumi: "This is it… this is where she was taken…"
Kazumi: "Please… bring her back…"

Step 3: encounter the swamp demon
You must wait until night time at the location that Kazumi brought you to
Numa: "Heh… another one has come to be devoured…"
Numa: "You Demon Slayers are always so predictable…"

Step 4: enter the swamp domain and look for Satoko
The swamp demon can use its blood demon art to summon a portal to the swamp domain
You must enter the swamp domain and search for Satoko
Inside the swamp domain, there will be a swamp demon wearing Satoko’s Bow on its head, killing that swamp demon will give you the bow
In the spawnSwampDomainDemons() function in SwampDemonArt.java, we should modify it such that if the LivingEntity target is a player that is currently doing the kidnapper’s bog quest, then the first swamp demon that gets spawned in the spawnSwampDomainDemons() function will be wearing Satoko’s bow item on its head
Player: “Hey! You’re wearing Satoko’s bow in your hair!”
Numa: “A trophy from my last meal!”
Player: “Where is Satotko?”
Numa: “You’re too late! I’ve already eaten her!”
Numa: “I keep artifacts from all my victims! I think I’ll keep your sword!”
Player: “Not if I kill you first!”
Numa: “"You think you can defeat me in MY domain?!"
Numa: "I’ll drag you into the swamp just like the others!"

Step 5: kill the swamp demon
When the original Swamp Demon is low health (less than 20% HP):
Numa: "Impossible… how can a human… be this strong…?!"

Step 6: return the hairpin to Kazumi
Kazumi: "You’re back…! Did you find her?!"
The player is prompted to give the Satoko’s Bow
Kazumi: "...That’s… Satoko’s…"
Kazumi: “...She’s gone… isn’t she…?"
Kazumi: "Thank you… for finding the truth…"

Reward: 50 XP + 10 yen

### Mission No.2 - Asakusa

The player travels to Tamayo’s House to better understand demons and to find out if violence is the only solution, and then Susumaru and Yahaba attack

"Your Kasugai Crow has received new orders."
"A demon doctor named Tamayo wishes to speak with Demon Slayers."

Step 1: travel to Tamayo's House

- The waypoint will point to the `kimetsunoyaiba:house_tamayo` structure
- If the location of the house_tamayo structure is at the positive X positive Z corner
  - Save those structure coordinates as pos_x pos_y pos_z
    - To get the pos_y coordinate, we'll need to find the grass_block on the surface at that x and z coordinate
- But the house_tamayo structure could be rotated in any orientation (in increments of 90 degrees)
  - The following coordinate points are calculated assuming that pos_x pos_y pos_z are at the positive X positive Z corner
  - For example, that first quest point is at x = (pos_x - 16), y = (pos_x + 1), z = (pos_z - 16)
  - But if the pos_x pos_y pos_z is at the negative X negative Z corner, then the first quest point is at x = (pos_x + 16), y = (pos_x + 1), z = (pos_z + 16)
  - So we need to take the rotation into consideration when calculating all these quest points

Step 2: talk to Tamayo and Yushiro

- `kimetsunoyaiba:tamayo` and `kimetsunoyaiba:yushiro` could be anywhere in the structure
  - At this point they will pathfind to x = (pos_x - 16), y = (pos_x + 1), z = (pos_z - 16)
  - If it takes more than 30 seconds for both of them to pathfind to that position, then those entities will be teleported there
  - If tamayo and/or yushiro don't exist within 100 blocks of the structure, then they will be spawned at that point
- The quest waypoint will point to x = (pos_x - 16), y = (pos_x + 1), z = (pos_z - 16)
- Once the quest player and both tamayo and yushiro arrive within 6 blocks of that point, the dialogs will start
- They will tell you about their work in helping demons, and studying the demon blood, and that they need to collect Kizuki blood (it's closely related to Muzan’s blood) in order to make a cure
  This will unlock the Doctor's Request side quest

Yushiro: ...A Demon Slayer?
Yushiro: Why did Lady Tamayo allow someone like you here?
Yushiro: If you try anything suspicious, I’ll kill you myself.
Tamayo: Please forgive Yushiro. He is… overly protective.
Tamayo: Welcome. I am Tamayo.
Tamayo: You are likely confused to see a demon who does not attack humans.
Tamayo: Come see my work in the basement

Step 3: go see Tamayo's basement

- A `kimetsunoyaibamultiplayer:demon_villager` entity will spawn at x = (pos_x - 17), y = (pos_y - 4), z = (pos_z - 17), unless there is already a demon_villager within 5 blocks of that location
  - That demon villager will be given slowness 10 and weakness 10, and those potion effects should have hidden particles
- The quest will now point towards x = (pos_x - 14), y = (pos_y - 4), z = (pos_z - 17)
- Tamayo and Yushiro will pathfind towards that point
  - There will be a trapdoor at x = (pos_x - 11), y = (pos_y + 1), z = (pos_z - 20)
  - Tamayo and Yushiro will need to pathfind to that trapdoor point first, open the trapdoor, then go down the ladder to pathfind to x = (pos_x - 14), y = (pos_y - 4), z = (pos_z - 17)
  - If it takes more than 30 seconds for them to get to within 3 blocks of x = (pos_x - 14), y = (pos_y - 4), z = (pos_z - 17), then they will teleport there
- Once the quest player, Tamayo, and Yushiro arrive within 3 blocks of x = (pos_x - 14), y = (pos_y - 4), z = (pos_z - 17), the dialogs will continue

Tamayo: Not all demons desire violence.
Tamayo: Some of us struggle endlessly against Muzan Kibutsuji’s influence.
Tamayo: I have devoted myself to studying demon blood."
Tamayo: If a cure exists… it may be possible to save both humans and demons alike.
Yushiro: Lady Tamayo has spent centuries researching this.
Yushiro: Unlike the Demon Slayers, she actually seeks a permanent solution.
Tamayo: However… ordinary demon blood is not enough.
Tamayo: The Twelve Kizuki possess blood far closer to Muzan’s own.
Tamayo: If samples from the Kizuki can be obtained… my research may finally progress.

Side Quest Unlocked: Doctor’s Request

Tamayo: ...Wait.
Yushiro: ...Someone’s here.

A sinister presence approaches…

The quest will now point towards x = (pos_x - 16), y = (pos_x + 1), z = (pos_z - 16)
Tamayo and Yushiro will pathfind to x = (pos_x - 16), y = (pos_x + 1), z = (pos_z - 16)

- If they take more 30 seconds to reach within 3 blocks of that location, they will get teleported there
- The next step will start once Tamayo, Yushiro, and the quest player reach within 3 blocks of that point

Step 4: defend Tamayo and Yushiro against Susumaru and Yahaba
`kimetsunoyaiba:susamaru` and `kimetsunoyaiba:yahaba` spawn at x = (pos_x - 34), y = (pos_y), z = (pos_z - 19)

Susamaru: Found yooou!"
Susamaru: Muzan-sama said there was a traitor hiding here!
Susamaru: Can I rip them apart now?!
Tamayo: ...So Muzan has found us after all.

There will be a tnt explosion at x = (pos_x - 22), y = (pos_y + 2), z = (pos_z - 12)

Now the quest objective is tto kill susamaru and yahaba

The quest will fail if tamayo entity dies, or if the player entity dies

After the battle:
Tamayo: Your strength may prove vital in the battles to come.
Yushiro: At least you weren’t completely useless.
Tamayo: Please remember what you learned here today.
Tamayo: And if my research succeeds… perhaps this endless tragedy can finally end.

### Mission No.3 - Tsuzumi Mansion

The player(s) will venture to the nearest Tsuzumi Mansion structure.

"Your Kasugai Crow has received new orders."
"Kids are trapped in a building with demons, they need rescuing before it's too late."

Step 1: Travel to Tsuzumi Mansion
The waypoint will point to the nearest Tsuzumi Mansion structure

Step 2: Talk to Teruko and Shoichi

Teruko: Help! A demon has kidnapped our older brother Kiyoshi!
Shoichi: Kiyoshi is trapped in the mansion! Please help us kind sir!

Step 3: Find Kiyoshi
Kiyoshi is hiding inside a closet in the mansion.

Step 4: Defeat Kyogai 




### Mission No.4 - Mount Natagumo

### Mission No.5 - Rehabilitation Training

### Mission No.6 - Mugen Train

### Mission No.7 - Entertainment District

### Mission No.8 - Swordsmith Village

### Mission No.9 - Hashira Training

### Mission No.10 - Infinity Castle

### Mission No.11 - Sunrise Countdown

## Permanence

The main story quest for demons, focuses on eradicating demon slayers and finding the blue spider lilly

### Stage No.1 - First Taste of Blood

Kill and eat 10 humans of any kind (villagers, demon slayers, sword smiths, non demon players…)

### Stage No.2 - Hunger Unending

Enter a village and eat 10 humans in their sleep without getting detected (no demon slayer or iron golems will detect you)

Step 1: Travel to the nearest village at night

Step 2: Kill and eat 10 villagers in their sleep

### Stage No.3 - Slayer’s Blood

This stage should feel unsettling and educational at the same time. Kamanue is cowardly and curious rather than openly aggressive, treating the player as an apprentice learning about humanity's greatest weapon.

An Orochi entity (black snake with glowing red eyes) will find the demon player (an Orochi will spawn, and be immediately tamed by the player, and the Orochi entity is a quest entity, used for waypoints and stuff)

The Orochi will tell the demon player that the strength they have shown has gotten the attention of a powerful demon.

Step 1: Travel to a dungeon (large cave)

- Objective: Travel to the dungeon where a Lower Moon demon resides.
- "Rumors speak of a powerful demon dwelling deep beneath the earth..."

Step 2: Find Kamanue living there and talk to Kamanue learn about demon slayers, and Kamanue will instruct you to capture a demon slayer and bring it back here

Kamanue's Dialogs (30 tick delay in between lines):

Kamanue: "Ah... another demon..."
Kamanue: "You haven't fought many Demon Slayers yet, have you?"
Kamanue: "Humans are weak. Demon Slayers are different."
Kamanue: "I want you to bring one here."
Player: "Dead?"
Kamanue: "Yes. I wish to study their dying breath"
Kamanue: "Bring me a slayer and I'll teach you something useful."

Quest Update: Capture a Demon Slayer and bring them to Kamanue.

Step 3: Terrorize a village until demon slayers arrive

- Objective: Attack villagers until Demon Slayers arrive.
- The quest will point to the nearest vanilla minecraft village
- Once at least one villager has been killed, a small squad of 4 demon slayers of power levels 0 and 1 will spawn somewhere in the village, and will target the demon player
- "The village is gripped by fear..."
- "Your actions have attracted attention."

Step 4: Kill a Demon Slayer

- Objective: Defeat the Demon Slayer
- When one dies: They drop Demon Slayer Human Flesh (this is already part of the base mod, no need to implement this).

Quest Update: Return the Demon Slayer's remains to Kamanue.

Step 5: Return to Kamanue

- Objective: Bring the Demon Slayer's flesh back to Kamanue.

Upon interacting: Kamanue takes the demon slayer's Human Flesh.

Step 6: Witness the Resurrection

(30 tick delay between dialogue)

Kamanue: "Excellent..."
Kamanue: "This one still has one last breath."
Kamanue: "Watch carefully."
Kamanue: "Muzan's blood reshapes the body..."
Kamanue: "...and it rewrites its instincts."
Kamanue: "Even on death's door, it still has potential."

_Kamanue injects Muzan Blood into the demon slayer's flesh. A demonized demon slayer is created._

Step 7: Defeat the Demonized Demon Slayer

Objective: Defeat the resurrected Demon Slayer.

The resurrected slayer begins at full health.
They retain their Nichirin sword.
They gain demon regeneration and increased physical abilities.
They attack only the quest player(s).
Kamanue does not participate.

When the Demonized Slayer Dies:
Kamanue: "You've learned something important today."
Kamanue: "The greatest weapon of humanity..."
Kamanue: "...can become one of demonkind's greatest assets."
Kamanue: "To defeat your enemy..."
Kamanue: "...you must first understand them."

Rewards: 3 muzan blood, 1 cushion, 200 experience

Kamanue: "Now sit on this cushion and meditate."
