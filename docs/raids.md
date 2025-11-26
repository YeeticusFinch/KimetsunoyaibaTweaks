# Raid System

## Civilian Structures

The following structures are considered civilian structures for the purpose of raids (and other features in this mod):
 kimetsunoyaiba:village_swam,
 kimetsunoyaiba:village_yukak,
 kimetsunoyaiba:house_tamayo,
 kimetsunoyaiba:house_tanjiro,
 kimetsunoyaiba:house_ubuyashiki,
 kimetsunoyaiba:house_urokodaki,
 kimetsunoyaiba:house_a,
 kimetsunoyaiba:house_kocho,
 kimetsunoyaiba:house_rengoku
 
 All kinds of villages from vanilla minecraft should also be considered as civilian structures
 
## Demon Power Scales

Not all demons are of the same power scale, some demons are stronger / more dangerous / harder to kill than others.

### Easy Demons

kimetsunoyaiba:demon, kimetsunoyaiba:demon_2, kimetsunoyaiba:demon_3, kimetsunoyaiba:spider_demon

### Medium Demons

kimetsunoyaiba:demon_4, kimetsunoyaiba:demon_5, kimetsunoyaiba:demon_9, kimetsunoyaiba:temple_demon, kimetsunoyaiba:swamp_demon

### Hard Demons / Easy Boss Demons

kimetsunoyaiba:demon_6, kimetsunoyaiba:demon_7, kimetsunoyaiba:demon_8, kimetsunoyaiba:demon_10, kimetsunoyaiba:hand_demon, kimetsunoyaiba:dice_steak_senior_demon, kimetsunoyaiba:goldfishbig, kimetsunoyaiba:rui_sister, kimetsunoyaiba:rui_brother, kimetsunoyaiba:rui_mother, kimetsunoyaiba:rui_father, kimetsunoyaiba:susamaru, kimetsunoyaiba:yahaba

### Medium Boss Demons (lower 12 kizuki)

Here are the lower moons in order of least to most powerful

kimetsunoyaiba:kyogai
kimetsunoyaiba:kamanue
kimetsunoyaiba:rui
kimetsunoyaiba:mukago
kimetsunoyaiba:wakuraba
kimetsunoyaiba:rokuro
kimetsunoyaiba:hairo
kimetsunoyaiba:enmu

### Hard Boss Demons (upper 12 kizuki)

Here are the upper moons in order of least to most powerful

kimetsunoyaiba:daki
kimetsunoyaiba:gyutaro (this one spawns automatically while fighting daki, so we shouldn't spawn it)
kimetsunoyaiba:kaigaku
kimetsunoyaiba:gyokko (this one spawns a bunch of goldfish)
kimetsunoyaiba:hantengu (this one will spawn a bunch of different forms during the battle)
kimetsunoyaiba:nakime (this one might get replaced by kimetsu:nakime)
kimetsunoyaiba:akaza
kimetsunoyaiba:doma
kimetsunoyaiba:kokushibo

### Demon Kings (hardest)

kimetsunoyaiba:muzan, kimetsunoyaiba:tanjiro_demon (this one should never spawn if muzan has never been killed in this world)

## Demon Slayer Power Scales

### Generic Demon Slayers

kimetsunoyaiba:demon_slayer (this is the most common one)
kimetsunoyaibamultiplayer:frost_slayer (this one will occasionally replace the demon_slayer, so don't worry about spawning this)
kimetsunoyaibamultiplayer:ice_slayer (this one will occasionally replace the demon_slayer, so don't worry about spawning this)
kimetsunoyaiba:murata

### Named Demon Slayers

Here are the named demon slayers in order of weakest to strongest:

kimetsunoyaiba:genya
kimetsunoyaiba:masachika
kimetsunoyaiba:inosuke
kimetsunoyaiba:tanjiro
kimetsunoyaiba:kaigaku_human
kimetsunoyaiba:sabito
kimetsunoyaiba:zennitsu
kimetsunoyaiba:kanawo

## Hard Demon Slayers

kimetsunoyaiba:dice_steak_senior
kimetsunoyaiba:dice_steak_senior_super

### Hashira

Here are the Hashiras in order of least to most powerful:

kimetsunoyaiba:kocho
kimetsunoyaiba:kanroji
kimetsunoyaiba:kanae
kimetsunoyaiba:shinazugawa
kimetsunoyaiba:rengoku
kimetsunoyaiba:iguro
kimetsunoyaiba:uzui
kimetsunoyaiba:tomioka
kimetsunoyaiba:muichirou
kimetsunoyaiba:himejima

### Super Hashira

Here are some super powerful legendary hashira in order of least powerful to most powerful:

kimetsunoyaiba:michikatsu
kimetsunoyaiba:yoriichi
kimetsunoyaiba:yoriichi_old

## Demon Raids

The `omen_of_muzan` potion effect (from `kimetsunoyaibamultiplayer`) will trigger a demon raid when a player with the omen_of_muzan potion effect enters a civilian structure.

Players with the oni tag (demon players) should be incapable of getting the omen_of_muzan potion effect.

The difficulty of the demon raid should depend on the level of the omen_of_muzan potion effect.

### omen_of_muzan I:

Wave 1: 6 easy demons

Wave 2: 3 easy demons and 3 medium demons

Wave 3: 1 hard demon / easy boss demon as the bossfight, with 2 medium demons and 2 easy demons as support

### omen_of_muzan II:

Wave 1: 6 easy demons

Wave 2: 5 easy demons and 5 medium demons

Wave 3: 6 easy demons and 6 medium demons

Wave 4: 2 hard demons / easy boss demons as the bossfight (two bosses), with 4 medium demons and 4 easy demons as support

... infer the other difficulties accordingly

### omen_of_muzan V:

Wave 1: 2 hard demons, 8 medium demons, 10 easy demons

Wave 2: 5 hard demons, 10 medium demons, 10 easy demons

Wave 3: 20 medium demons, 30 easy demons

Wave 4: 10 hard demons, 10 medium demons

Wave 5: 2 medium boss demons (lower ranks), 5 hard demons, 10 medium demons

Wave 6: 2 hard boss demons (upper ranks) as a bossfight, 5 hard demons, 10 medium demons

Wave 7: 1 demon king, 10 hard demons

When a raid is about to start, there should be a bar at the top of the player's screen (for every player in the general vicinity (within 500 blocks of the raid)) that slowly fills up, indicating that a round is about to start. Once this bar is full, the bar should be filled accordingly to the percentage of the raid demons that are left in this current round. So if round 1 spawns 5 demons, and all 5 demons are still alive, then it will show the bar as 100% full, but if 2 of these demons die and there are only 3/5 demons, then the bar should be only 60% full.

## Demon Slayer Raids

The `omen_of_ubuyashiki` potion effect (from `kimetsunoyaibamultiplayer`) will  trigger demon slayer raids when a player with the omen_of_ubuyashiki potion effect enters a civilian structure. 

The difficulty of the demon raid should depend on the level of the omen_of_ubuyashiki potion effect.

### omen_of_ubuyashiki I:

Wave 1: 3 generic demon slayers

Wave 2: 1 dice_steak_senior and 3 generic demon slayers

Wave 3: 1 named demon slayer (preferably an easier one) as a bossfight

### omen of ubuyashiki II:

Wave 1: 4 generic demon slayers

Wave 2: 1 dice_steak_senior and 4 generic demon slayers

Wave 3: 2 dice_steak_senior entities and 5 generic demon slayers

Wave 4: 1 named demon slayer as a bossfight with 3 generic demon slayers

### omen of ubuyashiki III:

Wave 1: 6 generic demon slayers

Wave 2: 2 dice_steak_senior and 5 generic demon slayers

Wave 3: 3 dice_steak_senior entities and 6 generic demon slayers

Wave 4: 2 named demon slayers as a bossfight with 3 dice_steak_senior entities

Wave 5: 1 dice_steak_senior_super or a hashira as a bossfight with 5 generic demon slayers

### omen of ubuyashiki IV:

Wave 1: 6 generic demon slayers with 3 dice_steak_senior entities

Wave 2: 5 dice_steak_senior and 10 generic demon slayers

Wave 3: 6 dice_steak_senior entities and 10 generic demon slayers

Wave 4: 3 named demon slayers with 3 dice_steak_senior entities and 5 generic demon slayers

Wave 5: 1 hashira as a bossfight with 1 dice_steak_senior_super with 5 dice_steak_senior entities

Wave 6: 1 stronger hashira as a bossfight with 2 named demon slayers and 3 dice_steak_senior entities

### omen of ubuyashiki V:

Wave 1: 8 generic demon slayers with 6 dice_steak_senior entities

Wave 2: 10 dice_steak_senior and 15 generic demon slayers

Wave 3: 6 dice_steak_senior_super entities and 10 dice_steak_senior entities

Wave 4: 3 named demon slayers with 10 dice_steak_senior entities and 10 generic demon slayers

Wave 5: 2 hashira as a bossfight with 6 dice_steak_senior_super with 10 dice_steak_senior entities

Wave 6: 2 stronger hashira as a bossfight with 3 named demon slayers and 8 dice_steak_senior entities

Wave 7: 1 super hashira as a bossfight with 5 dice_steak_senior_super entities

Similar to the demon raids, there should be a bar at the top of the player's screen (for every player in the general vicinity (within 500 blocks of the raid)) that slowly fills up, indicating that a round is about to start. Once this bar is full, the bar should be filled accordingly to the percentage of the raid demon slayers that are left in this current round. So if round 1 spawns 5 demon slayers, and all 5 demon slayers are still alive, then it will show the bar as 100% full, but if 2 of these demon slayers die and there are only 3/5 demon slayers, then the bar should be only 60% full.

