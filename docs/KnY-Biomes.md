# KimetsunoYaiba Biomes

## Orca's KimetsunoYaiba Mod Biomes

Orca's KimetsunoYaiba Mod adds the following biomes:

- mt_natagumo
- mt_yoko
- mt_sagiri
- mugen_biome (spawns in mugen dimension)
- biome_enmu_dream (spawns in enmu dream dimension)

Out of these, two of the biomes don't work: mt_natagumo and mt_yoko

## Multiplayer Addon Biomes & Dimensions

This multiplayer addon adds the following:

### Wisteria Forest Biome
- **ID**: `kimetsunoyaibamultiplayer:wisteria_forest`
- **Features**:
  - Wisteria trees with different colored leaves (pink, lavender, cyan, cream)
  - Wisteria petals on the ground
  - Protective effect against demons (handled by `WisteriaBiomeHandler.java`)
  - Demons that enter take continuous damage and are pushed away
- **Spawning**: Configured via `BiomeConfig.java` with climate parameters
- **Configuration**: See `config/BiomeConfig.java` for spawn frequency and size multipliers

### Mt Fujikasane Dimension
- **ID**: `kimetsunoyaibamultiplayer:mt_fujikasane`
- **Type**: Separate dimension (not a biome)
- **Size**: 1000×1000 blocks with enforced world border
- **Environment**: Overworld-like with day/night cycle
- **World Source**: Pre-made world created in WorldPainter
- **Features**:
  - Large mountain surrounded by wisteria forests
  - World border prevents players from going beyond ±500 blocks from center
  - Warning messages when approaching border
  - Automatic teleport back if boundary exceeded
- **Documentation**: See [mt-fujikasane-dimension.md](mt-fujikasane-dimension.md) for complete setup guide

## How do fix this?

One way of fixing this is with the [Biome Replacer mod](https://www.curseforge.com/minecraft/mc-mods/biome-replacer)

Biome Replacer let's you replace biomes in your game with a different biome, or remove biomes entirely.

You can use Biome Replacer to swap out some vanilla biomes (or biomes from a different mod, or some custom biomes from a datapack) with `kimetsunoyaiba:mt_natagumo` and `kimetsunoyaiba:mt_yoko`, and now you will see those two biomes in your world, and the crow will properly point towards it.

This is a little tedious, and it means that you have to forfeit two biomes in your world (unless you add more with a datapack or a mod).

## Planned feature

I am currently investigating how to add these missing biomes to the game with my add-on