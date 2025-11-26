# Spawning Rules

## Config

There is a master switch for enabling / disabling all of these spawning rules

`enhanced_spawning_rules = true` will enable all of these spawning rules, else we'll just use the spawn priority settings

There are also some other config settings associated with spawning rules in the spawning rules config section

## Maximum Spawning Rules

Sometimes there will be a maximum of one of a specific entity type that can spawn in a specific biome or structure or radius. Before spawning another entity with such a restriction, check if that maximum is already satisfied, and also check for the replacer mobs from the replacer section, so if kimetsunoyaiba:akaza is being replaced by the kimetsu:akaza mob, then check for both the kimetsunoyaiba:akaza and the kimetsu:akaza mobs in the area before spawning a new akaza.

## Spawning Tag Groups

There are multiple tags involved in the spawning rules.

See `/docs/KnY-Entity-Tags.md` for info on the entity tags from the kimetsunoyaiba mod

### demon_slayer
This is a new tag introduced by this mod, this is the kimetsunoyaibamultiplayer:demon_slayer tag. This tag includes all hashiras (entities from the kimetsunoyaiba:tag_hashira group), and all kamabokos (entities from the kimetsunoyaiba:kamaboko group) and all the former hashiras too (kimetsunoyaibamultiplayer:former_hashira), along with some more characters too.
- kimetsunoyaiba:demon_slayer
- kimetsunoyaiba:dice_steak_senior
- kimetsunoyaiba:dice_steak_senior_super
- kimetsunoyaiba:dice_steak_senior_golden (doesn't spawn naturally)
- kimetsunoyaiba:genya
- kimetsunoyaiba:tanjiro
- kimetsunoyaiba:zennitsu
- kimetsunoyaiba:kanawo
- kimetsunoyaiba:inosuke
- kimetsunoyaiba:himejima
- kimetsunoyaiba:muichirou
- kimetsunoyaiba:rengoku
- kimetsunoyaiba:shinazugawa
- kimetsunoyaiba:tomioka
- kimetsunoyaiba:kakushi (technically this isn't a demon slayer, but this one is allied with the demon slayers and is seen with them)
- kimetsunoyaiba:makomo
- kimetsunoyaiba:masachika
- kimetsunoyaiba:michikatsu
- kimetsunoyaiba:murata
- kimetsunoyaiba:nezuko (also technically not a demon slayer, but she's allied with the demon slayers)
- kimetsunoyaiba:sabito
- kimetsunoyaiba:ubuyashiki (doesn't spawn naturally)
- kimetsunoyaiba:kaigaku_human
- kimetsunoyaibamultiplayer:frost_slayer
- kimetsunoyaibamultiplayer:ice_slayer
- ...

### tag_hashira
This is the kimetsunoyaiba:tag_hashira tag, which includes all hashira demon slayers
- kimetsunoyaiba:himejima
- kimetsunoyaiba:muichirou
- kimetsunoyaiba:rengoku
- kimetsunoyaiba:shinazugawa
- kimetsunoyaiba:tomioka
- kimetsunoyaiba:michikatsu
- ...

### former_hashira
This is a new tag, the kimetsunoyaibamultiplayer:former_hashira tag, which includes all of the retired hashira
- kimetsunoyaiba:kuwajima
- kimetsunoyaiba:urokodaki
- kimetsunoyaiba:uzui

### kamaboko
This is the kimemtsunoyaiba:kamaboko tag, which includes the main protagonists from demon slayer. These characters are all demon slayers too
- kimetsunoyaiba:genya
- kimetsunoyaiba:tanjiro
- kimetsunoyaiba:zennitsu
- kimetsunoyaiba:kanawo
- kimetsunoyaiba:inosuke

### demon
This is the kimetsunoyaiba:demon tag, which includes all demons, muzan, and the 12 kizuki
- kimetsunoyaiba:akaza
- kimetsunoyaiba:daki
- kimetsunoyaiba:demon
- kimetsunoyaiba:demon_10
- kimetsunoyaiba:demon_2
- kimetsunoyaiba:demon_3
- kimetsunoyaiba:demon_4
- kimetsunoyaiba:doma
- kimetsunoyaiba:enmu
- kimetsunoyaiba:gyokko
- kimetsunoyaiba:gyokko_goldfish
- kimetsunoyaiba:gyutaro
- kimetsunoyaiba:hairo
- kimetsunoyaiba:hand_demon
- kimetsunoyaiba:nezuko (not allied with demons, but she is technically a demon)
- kimetsunoyaiba:wakuraba
- ...

### twelve_kizuki
This is the kimetsunoyaiba:twelve_kizuki tag, which includes all 12 kizuki (the upper and lower rank demons, and there are more than 12 entities in here).
- kimetsunoyaiba:akaza
- kimetsunoyaiba:daki
- kimetsunoyaiba:gyutaro
- kimetsunoyaiba:hairo
- kimetsunoyaiba:doma
- kimetsunoyaiba:enmu
- kimetsunoyaiba:gyokko
- kimetsunoyaiba:nakime 9
- ...

### sword_smith
This is a custom tag added by this mod, the kimetsunoyaibamultiplayer:sword_smith tag, which includes all the swordsmith
- kimetsunoyaiba:haganeduka
- kimetsunoyaiba:yorichi_0
- kimetsunoyaiba:kotetsu

### civilian
This is a custom tag added by this mod, the kimetsunoyaibamultiplayer:civilian tag, which includes all civilian humanoids
- kimetsunoyaiba:doctor
- kimetsunoyaiba:grandmother
- kimetsunoyaiba:hakuji
- kimetsunoyaiba:rui_human
- kimetsunoyaiba:toyosan
- kimetsunoyaiba:kanawo_buyer
- minecraft:villager

### woman
This is the forge:woman tag, it includes all the female characters (civilians, demons, and demon slayers)
- kimetsunoyaiba:mukago
- kimetsunoyaiba:kocho
- kimetsunoyaiba:kanae
- kimetsunoyaiba:kanawo
- kimetsunoyaiba:rui_mother
- kimetsunoyaiba:tamayo
- kimetsunoyaiba:rui_sister
- kimetsunoyaiba:yachan
- kimetsunoyaiba:yachan_brother
- ...

### animal
This is a custom tag added by this mod, it includes all the animals that are added by the kimetsunoyaiba mod
- kimetsunoyaiba:boar
- kimetsunoyaiba:boar_golden (doesn't spawn naturally)
- kimetsunoyaiba:butterfly
- kimetsunoyaiba:muscular_mouse

## Entity Replacers

Some entities should be replaced immediately upon spawning, no matter how they are spawned (except if they are spawned by commands or by spawn eggs).

This should be entirely configurable too, so players can edit the config file to select which entities are replaced by which

Here are the default replacements that should happen if the client has the kimetsu mod and they have `kimetsu_replacer` enabled in the entity_replacer section of our mod's config (some of these are actual entity names, and some of them are the names of the spawn egg, in which case our mod should detect if it's a spawn egg and then get the exact entity that the spawn egg spawns):

kimetsunoyaiba:nakime -> kimetsu:nakime_spawn_egg

Here are the replacements that should only happen if the client has the kimetsu mod and they have the `kimetsu_movie_replacer` enabled in the entity replacer section of our mod's config

kimetsunoyaiba:nakime -> kimetsu:nakime_real_spawn_egg
kimetsunoyaiba:daki -> kimetsu:daki_spawn_egg
kimetsunoyaiba:gyutaro -> kimetsu:gyutaro_spawn_egg
kimetsunoyaiba:kaigaku -> kimetsu:kaigaku_spawn_egg
kimetsunoyaiba:gyokko -> kimetsu:gyokko_spawn_egg
kimetsunoyaiba:zohakuten -> kimetsu:zohakuten_spawn_egg
kimetsunoyaiba:akaza -> kimetsu:akaza_spawn_egg
kimetsunoyaiba:doma -> kimetsu:doma_spawn_egg
kimetsunoyaiba:kokushibo -> kimetsu:kokushibo_spawn_egg
kimetsunoyaiba:himejima -> kimetsu:himejima_spawn_egg
kimetsunoyaiba:kocho -> kimetsu:slayer_kocho_spawn_egg

The config should be able to enable or disable any of these individually too

## New Entities Added to the tags

The following entities aren't included in the existing tag system, and should be added. A lot of these are from the kimetsu mod, and should only be added if the kimetsu mod is loaded (kimetsu mod is optional):

kimetsu:akaza - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:daki - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:demon_nakime - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:doma - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:gyokko - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:gyutaro - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:himejima - kimetsunoyaibamultiplayer:demon_slayer, kimetsunoyaiba:tag_hashira
kimetsu:kaigaku - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:slayer_kocho - kimetsunoyaibamultiplayer:demon_slayer, kimetsunoyaiba:tag_hashira
kimetsu:slayer_kocho_kakusei - kimetsunoyaibamultiplayer:demon_slayer, kimetsunoyaiba:tag_hashira
kimetsu:kokushibo - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki
kimetsu:nakime_real - kimetsunoyaiba:demon, kimetsunoyaiba:twelve_kizuki

## Generic Spawning Rules

Unless there is another rule from one of the biomes or structures, just apply these generic rules

Reduced demon spawning
- There are way too many demons spawning already, so this should be reduced
- Add a config to reduce generic demon spawning (maybe default it to 40%)

No naturally-spawning twelve_kizuki demons
- Entities with the twelve_kizuki tag should never spawn naturally outside of designated biomes or structures
- twelve_kizuki can spawn during demon raids as like boss-fight rounds

No naturally-spawning tag_hashira demon slayers
- Entities with the tag_hashira tag should never spawn naturally outside of designated biomes or structures
- tag_hashira can spawn during demon slayer raids as like boss-fight rounds
- And hashiras should have a rare chance of spawning if an entity with the kimetsunoyaibamultiplayer:civilian tag sees or gets attacked by a entity with the kimetsunoyaiba:twelve_kizuki tag, this mechanic is like iron golems, where it has a chance at spawning a tag_hashira if this happens

No naturally-spawning kamaboko
- Entities with the kamaboko tag should never spawn naturally outside of designated biomes or structures
- kamaboko can spawn during demon slayer raids as like boss-fight rounds
- And kamaboko should have a rare chance of spawning if an entity with the kimetsunoyaibamultiplayer:civilian tag sees or gets attacked by a entity with the kimetsunoyaiba:twelve_kizuki tag, this mechanic is like iron golems, where it has a chance at spawning a kamaboko if this happens

Naturally spawning demon slayers
- Demon slayers spawning naturally outside of designated biomes/structures should be significantly reduced
- There should be a config for this (default 10%)
- Demon slayers should spawn together in groups, similar to pillagers
- Demon slayers should also spawn in demon slayer raids
- And demon slayers should spawn if an entity with the kimetsunoyaibamultiplayer:civilian tag sees or gets attacked by a entity with the kimetsunoyaiba:demon tag, this mechanic is like iron golems, where it has a chance at spawning a few demon slayers when this happens

## Dimension Specific Spawning Rules

### kimetsunoyaibamultiplayer:mt_fujikasane
This is a special dimension, and demons should spawn in a certain area of this dimension.

The center of the mountain is at 227 319 70, and there should be a good amount of demons spawning within a 400 block radius of that center along the surface (when I say 400 block radius, I mean the X and Z horizontal coordinates should be within 400 blocks). Also, spawning should be prioritized on the surface.

Demons should only spawn here at night of course

Don't spawn any of the twelve_kizuki demons here, just spawn the non-kizuki demons here

Demon slayers (not hashiras) should be spawning 311 76 736, within 20 blocks of that area. No hashiras should spawn, but we should be seeing kamabokos spawning here (maximum of one of each type of kamaboko)

## Biome Specific Spawning Rules

### kimetsunoyaibamultiplayer:wisteria_forest
No demons should spawn here ever. Demons can't pathfind into here either, and if a twelve_kizuku demon ever enters a wisteria forest then it should immediately despawn.

### kimetsunoyaiba_enmu_dreaem
Vanilla mobs can spawn here, and the kimetsunoyaiba:boar can spawn here too

### kimetsunoyaiba:mt_natagumo
This is the spider demon mountain
Here are the demons that can spawn here:
- kimetsunoyaiba:demon_3
- kimetsunoyaiba:spider_demon
- kimetsunoyaiba:dice_steak_senior_demon

There will only be one of each of the rui demons in a specific biome (or within a 500 block radius).
- kimetsunoyaiba:rui
- kimetsunoyaiba:rui_father
- kimetsunoyaiba:rui_mother
- kimetsunoyaiba:rui_brother (if this instance of the biome contains a kimetsunoyaiba:house_rui_brother structure, then rui_brother should be spawned there, else rui_brother can spawn anywhere else in the mt_natagumo biome, and rui_brother already spawns here in the kimetsunoyaiba mod)
- kimetsunoyaiba:rui_sister

If this instance of the biome contains a kimetsunoyaiba:house_rui structure, then the rui demons will have a higher chance of spawning there than the rest of the biome.

Also there will be a good amount of spiders and cave spiders spawning in this biome

The demons in this biome shouldn't spawn during the day

### kimetsunoyaiba:mt_sagiri
The demon slayers that can spawn here naturally are kimetsunoyaiba:sabito and kimetsunoyaiba:makomo, they can spawn here, but there shouldn't be more than one of each of these in the same mt_sagiri biome (or within a 500 block radius).
kimetsunoyaiba:urokodaki can also spawn here, but if this biome has a kimetsunoyaiba:house_urokodaki structure, then urokodaki will spawn in that structure instead of the biome (kimetsunoyaiba mod already spawns him in that structure).

### kimetsunoyaiba:mt_yoko
Regular demons can spawn here (kimetsunoyaiba:demon, kimetsunoyaiba:demon_2, kimetsunoyaiba:demon_3 ...)

### kimetsunoyaiba:mugen_biome
This should be infested with demons, including twelve kizuki demons. Don't spawn more than one of each type of twelve_kizuki within 500 blocks of eachother, for example you can have a kimetsunoyaiba:doma within 500 blocks of a kimetsunoyaiba:akaza, but you can't have two domas spawning within 500 blocks of eachother.

## Structure Specific Spawning Rules

### kimetsunoyaiba:graveyard
In a graveyard structure, there should be a maximum of one of each of these that spawn: kimetsunoyaiba:ubuyashiki and kimetsunoyaiba:himejima

### kimetsunoyaiba:house_a
Entities with the kimetsunoyaibamultiplayer:civilian tag should spawn here

### kimetsunoyaiba:house_kocho
kimetsunoyaiba:kanawo and kimetsunoyaiba:kocho already are spawning here from the kimetsunoyaiba mod, no need to touch this one
There should also be kimetsunoyaiba:kakushi entities spawning here

### kimetsunoyaiba:house_rengoku
kimetsunoyaiba:rengoku should be able to spawn here, maximum of 1 per structure
There should also be kimetsunoyaiba:kakushi entities spawning here

### kimetsunoyaiba:house_rui
Any of the rui demons can spawn here, along with the spider demon too (see the mt_nagatumo section)

### kimetsunoyaiba:house_rui_brother
kimetsunoyaiba:rui_brother can spawn here, and this is already done in the kimetsunoyaiba mod (see the mt_nagatumo section)

### kimetsunoyaiba:house_tamayo
tamayo and yushiro can spawn here, which is already done in the kimetsunoyaiba mod

### kimetsunoyaiba:house_tanjiro
Nothing spawns here

### kimetsunoyaiba:house_ubuyashiki
Any entities with the kimetsunoyaibamultiplayer:demon_slayer tag should be able to spawn here, , and
kimetsunoyaiba:ubuyashiki already spawns here in the kimetsunoyaiba mod
Entities with the kimetsunoyaiba:demon tag should be prevented from spawning here
There should also be kimetsunoyaiba:kakushi entities spawning here

### kimetsunoyaiba:house_urokodaki
kimetsunoyaiba:urokodaki already spawns here in the kimetsunoyaiba mod
There should also be kimetsunoyaiba:kakushi entities spawning here

### kimetsunoyaiba:mugen_castle
See the mugen_biome section for more info
Essentially there should be lots of demons in the mugen castle

### kimetsunoyaiba:mugen_train
kimetsunoyaiba:enmu should spawn in this structure (maximum of one per instance of this structure)
Entities of the kimetsunoyaibamultiplayer:civilian tag should also spawn here, there should be civilians inside this train
And then there should be a maximum of one kimetsunoyaiba:rengoku spawning here, along with a maximum of one kimetsunoyaiba:akaza spawning here too. Akaza should only spawn here after enmu has been killed, so there shouldn't be both an enmu and an akaza

### kimetsunoyaiba:temple
kimetsunoyaiba:temple_demon spawns here, and this is already handled by the kimetsunoyaiba mod

### kimetsunoyaiba:temple_doma
kimetsunoyaiba:doma should spawn here, and there should be a maximum of one doma per temple_doma structurek
### kimetsunoyaiba:village_swampEntities of the tag kimetsunoyaibamultiplayer:civilian should be spawning here, and a lot of them, because this is a large village

### kimetsunoyaiba:village_yukak
This is the entertainment district
Entities of the tag kimetsunoyaibamultiplayer:civilian should be spawning here, and a lot of them, because this is a large village
kimetsunoyaiba:daki should also spawn here (maximum of one per village_yukak structure, and don't spawn one if there is a gyutaro entity of any kind in the village_yukak structure
Don't spawn daki in the daytime