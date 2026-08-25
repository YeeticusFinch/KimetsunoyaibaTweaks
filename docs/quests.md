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
The inside of Tsuzumi Mansion is built like a maze.
Kiyoshi is hiding inside a closet in the mansion.
Inside the Tsuzumi Mansion there are regular demons, tongue demons, and horned demons.

Step 4: Defeat Kyogai 
If Kiyoshi dies then the quest fails and must be restarted.

Step 5: Exit the Tsuzumi Mansion
Exit the mansion and bring Kiyoshi back to his siblings (Teruko and Shoichi).


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

# Stage No.4 - The Spider Family

A spider familiar will find the player (a Spider Manifestation will spawn and be tamed by the player), and the Spider Manifestation is a quest entity used for quest waypoints.

This spider familiar will tell the player that a demon child is in peril, and will ask the player to go rescue that demon child from the demon slayers.

Step 1: Find the demon child

The demon child is named Ryoko (this is actually Spider Demon Daughter in her original form), and she is surrounded by demon slayers that aren’t currently attacking, but will attack as soon as the player approached (ie. once step 2 starts)

Step 2: Kill the demon slayers before they kill the demon child

If Ryoko dies, then the quest fails and must be restarted.

Step 3: Talk to the demon child, she will thank you for saving her, and she will ask if she can travel with you, introducing herself as Ryoko

Step 4: Take Ryoko to Mount Natagumo.

The spider familiar will tell the player to bring Ryoko back to her home in Mount Natagumo, where her mother is waiting and is very worried. If Ryoko dies, then the quest fails and must be restarted

Step 5: Talk to Ryoko’s mother

At this point, both Ryoko and Spider Demon Mother are invulnerable for quest purposes
This is Spider Demon Mother. As soon as you approach Mother, Ryoko will say “No! Please don’t bring me back here!”, and then spider webs will grab her and trap her here. Ryoko’s mother will thank the player for bringing back her Daughter, and then she will reprimand Daughter for running away, and also say “you know He doesn’t like when you don’t keep up appearances”, and then Ryoko will transform back into Spider Demon Daughter form.
As a reward, Spider Demon Mother will offer the player a place in the Spider Family. 

Step 6: Join the Spider Family

The waypoint directs the player to the Spider Family’s house, where all the Spider Family demons await. For quest purposes, all Spider Demon Family members are invulnerable right now

Rui will welcome you to the family as a sibling (thanking you for saving his sister), and offer you Spider Demon Blood. Upon consuming the Spider Demon Blood, it’s the equivalent of consuming 1 Muzan blood, plus it grants 1 spider demon transformation for the player to choose from (this is an armor to equip).

Rewards: 1 Spider Demon Blood (given from Rui), 400 xp

# Stage No.5 - Demonweb Prince

At any point in this quest, if the player tries attacking Rui, Rui won’t take any damage from the player, Rui will be invulnerable for most steps of this quest stage

Step 1: Witness Daughter’s punishment

The player will be directed to the courtyard where all of the Spider Family has been gathered. Rui will reprimand Daughter for risking the lives of the family by running away, and then Rui will chop her up into many pieces using his sharp webs, and leave her as a bloody mess on the ground as she heals herself.

For quest purposes, all spider demon family members are invulnerable, but Daughter will be brought down to 2 HP when Rui attacks her.

Step 2: Talk to Daughter

After Rui and the rest of the family leaves, the player will talk to Daughter, and she will say that this is a violent and abusive family, and that Rui must be destroyed. Daughter will say that she is too weak to run away again, and that no demon is strong enough to defeat Rui, so you must find the demon slayers.

Step 3: Talk to Rui 

Rui will say that the Spider Family is built on trust, and they protect each other from the horrors of this cruel world (such as the demon slayers), and that he is Muzan’s favorite. He will then tell the player that Daughter trusts the player, and that she has broken the family’s trust and brings danger to the family, so she must die. He will then tell the player to kill Daughter, to prove loyalty to the family, otherwise Rui will kill both the player and Daughter

At this point there are two paths the player can follow: 
- Daughter Path (kill Rui)
- Rui Path (kill Daughter)

The player will immediately have to pick one from a UI (or from the chat), and that will determine the next quest steps the player is given. If there are multiple players playing this quest together, one of the players will be given the choice, and that will make the choice for all players (so you can’t have two different players simultaneously doing different choices for this same quest at the same time in the same Mount Natagumo biome).

## Daughter Path

Step 3 (Daughter path): Terrorize a village as a spider demon

Wearing a spider demon transformation item, the player must attack a village until demon slayers arrive.

Step 4 (Daughter path): Return to Mount Natagumo

Once the player returns to Mt. Natagumo, the player will overhear a conversation between Father and Rui

Father: “Rui, you owe Daughter an apology. Daughter is not the traitor.”

Rui: “Is that so?

Daughter: “Yes, the traitor is <player>.”

Father: “Daughter never ran away, <player> kidnapped her.”

Daughter: “And now <player> is bringing the demon slayers to kill us all!”

Rui: “They can't hurt us anymore, I'll make sure of it. And <player> must be destroyed.”

At this point the first wave of demon slayers will arrive and target Rui and Father. Just some regular demon slayers. The player doesn’t have to fight, the player is directed to run away and go find Mother.

The spider family is still invulnerable (just for quest purposes so that they don't die early).

New Objective: Survive Rui’s Wrath

Step 5 (Daughter path): Gather allies, talk to Mother

During this step, any demon slayers that come near Mother will instantly turn into puppets and get pulled up into the trees

Player: “Help! Daughter has betrayed me, and now Rui is going to kill me!”

Mother: “You expect me to be grateful you rescued Daughter? I resent you for it!”

Mother: “If you want to survive as a demon you must eliminate all traces of humanity within you. Daughter is not a helpless child, she’s a century-old demon that will kill all of us if given the chance.”

Player: “The demon slayers are here, Rui’s time will soon come to an end”

Mother: “Rui has killed hundreds of demon slayers, and if I let you live then he will kill me too”

Step 6 (Daughter path): Circus of Horrors

Mother will get pulled up into the trees by her own webs, and then 10 demon slayer puppets will descend onto the Player and attack. These demon slayer puppets will all have the puppetry potion effect (with no particles) for 15 minute duration.

The player must destroy all demon slayer puppets

After the demon slayer puppets have been killed, Tanjiro will spawn at Mother’s location and kill her.

Step 7 (Daughter path): Confront Daughter

Daughter: “Now’s our chance to run away! Mother is dead, Rui is distracted…”

Player: “You betrayed me, you will die with the rest of your wretched family”

*Shinobu spawns*

Shinobu: “You will all die”

Shinobu will chase Daughter. Giyu will spawn and attack the player (Giyu will be invulnerable for quest purposes)

Step 8 (Daughter path): Finish what you started

Giyu will spawn and target the player. The player must run from Giyu and find Rui. Rui is no longer invulnerable. Giyu will target Rui as soon as Rui is found and Giyu will kill Rui.

Step 9 (Daughter path): Flee Mount Natagumo

The player must exit the Mount Natagumo biome without getting killed. All the demon slayers from this quest will despawn as soon as the player exits Mount Natagumo.

## Rui Path

Step 3 (Rui path): Confront Daughter

Find Daughter. Once the player finds Daughter, they will discover that Daughter has been fully healed, and Daughter will trap player in a ball of silk and then escape.

Daughter: “You aim to betray me? Fine, I do not need your help, I will get the demon slayers myself and burn this whole family to the ground!”

*Father spawns*

Father: “<player> you will be punished for your insolence!"

Step 4 (Rui path): Defeat Father

Father will attack the player, and the player must defeat Father.

Once Father dies, Rui will tell the player "Stop wasting your time and go kill daughter!"

Step 5 (Rui path): Find Daughter at the village

The quest marker will point to the nearest village. Once the player arrives at the village, the player will notice that all the villagers are in daughter's cocoons.

Daughter will be in the center of the village in Ryoko form (resembling a human).

Ryoko: Help! The demon is here!

*Zenitsu spawns*

Zenitsu: I'll save you Ryoko-chan! 

Zenitsu picks up Ryoko (Daughter) and they both disappear in a flash of thunder.

A group of demon slayers spawns and immediately ambush the player.

Step 6 (Rui path): Defeat the Demon Slayers

The player will need to defeat the demon slayers in the village.

Step 7 (Rui path): Warn the Spider Family

The player must return to Mount Natagumo to warn the Spider Family about the incoming Demon Slayer attack. The player must find Mother and warn her.

Player: Daughter betrayed us, the Demon Slayers are coming to exterminate our family!

Mother: Lure the demon slayers into my traps, we will build an army of puppets to defend our family!

Step 8 (Rui path): Build the army of puppets

Spider Manifestations will be hidden throughout the forest (Mount Natagumo is a forest), the player must bring 15 demon slayers into range of Spider Manifestations. Each time a demon slayer is brought within 5 blocks of a Spider Manifestation, the demon slayer will become a puppet (they will gain the Puppetry potion effect with no particles for 20 minute duration, with the owner tag being set to Mother).

Demon slayers will spawn throughout the forest, and the player must lure them over to the spider manifestations, this step will be completed once 15 slayers have been given the puppetry effect.

_The puppetry potion effect is a new potion effect. When an entity is under the puppetry potion effect, each of their bones will have a thin straight white bezier line connecting to the bone, and extending out in a random direction. For each of those thin straight white bezier lines, it should store the location (in the global space) that the line extends out towards, so each line should be drawn from the entity with the puppetry potion effect, to a random location within 30 blocks, and that location should be stored such that it only chooses a random location once per new line, such that these lines aren't constantly changing directions, they should always be pointed towards that same location. Also, the line should gradually fade out the further it gets from the entity with the puppetry potion effect, like it should get more and more transparent the further from the entity, so the lines just fade out into full transparency towards the end, and these lines should be 20 blocks long. When an entity gets the puppetry potion effect, it should immediately store a position in the entity's NBT (the position being the location that entity was at when they received the puppetry effect), and the entity cannot move more than 20 blocks away from that location, whenever the entity tries moving further than 20 blocks from that location, that entity will jut get pulled back towards the location. Also, when the puppetry potion effect is applied, that entity will get a puppet owner in the NBT, the puppet owner just pointing to a LivingEntity. If the puppet owner dies or despawns or becomes null or invalid, then the puppetry potion effect will immediately end. While the puppetry effect is active, the entity will be entirely controlled by a different entity controller, even players, so like if a player has the puppetry effect, the player will have no control of their character for the duration. For the duration of the puppetry effect, the entity/player will stay still if they are not aggro on anything, they won't walk around randomly, they won't even look around randomly, they will just stay still, also they can hover, they don't fall, they should pathfind like a parrot where they can fly but also still pathfind towards enemies to attack them. Entities/players with the puppetry effect will automatically aggro on and attack any entities that the puppet owner is aggro on, or any entities that have attacked the puppet owner. If the puppet owner is a demon, then the entity will automatically aggro on and attack demon slayers and humans and non-demon players. Every time an entity with the puppetry effect takes damage, it subtracts 40 seconds from the effect duration. Once the puppetry effect is over, it should clear all the puppetry effect stuff for that entity, it should remove the thin white lines that are rendered, it should make them no longer floating, and it should give them control of themselves again, essentially reverting the entity back to its former self._

After the army of puppets is created, Rui will arrive (teleport over, or spawn if there is no Rui in a loaded chunk within 500 blocks).

Rui: “<player> you are proving to be a valuable member of the family, now find Daughter and kill her!”

Step 9 (Rui path): Finish what you started

The player must find Daughter. Daughter now resembles a helpless little human child (she is still in her Ryoko form), and she is being protected by Zenitsu and some demon slayers

Zenitsu: “You monster! Leave this girl alone!”

Player: "She's a demon, just like me"

Zenitsu: "Ryoko-chan is not a monster like you, I won't let you murder her!"

The player must kill Daughter without getting killed. The step will complete itself as soon as Daughter dies. The player does not need to kill Zenitsu or any of the other demon slayers for this step.

Daughter will be using her blood demon art to fight the player. This is a boss battle, with Daughter having the bossbar for her health.

Step 10 (Rui path): Report back to Rui

While still evading Zenitsu (or not, if Zenitsu is dead), the player must return back to Rui. Upon returning to Rui, the player will witness Rui getting instantly killed by Giyu.

There will be more demon slayers spawning, and there will also be one Tanjiro, the same Zenitsu from earlier (if not already killed), one Inosuke, one Giyu, and one Shinobu entity.

Step 11 (Rui path): Flee Mount Natagumo

The player must exit the Mount Natagumo biome without getting killed. All the demon slayers and demons from this quest will despawn as soon as the player exits Mount Natagumo.

Rewards: 4 Muzan blood, 800 xp
