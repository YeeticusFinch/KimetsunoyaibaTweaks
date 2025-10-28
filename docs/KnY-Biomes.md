# KimetsunoYaiba Biomes

## Orca's KimetsunoYaiba Mod Biomes

Orca's KimetsunoYaiba Mod adds the following biomes:

- mt_natagumo
- mt_yoko
- mt_sagiri
- mugen_biome (spawns in mugen dimension)
- biome_enmu_dream (spawns in enmu dream dimension)

**NOTE: As of KimetsunoYaiba ver3, all biomes now spawn properly!** The base mod has been updated to fix the climate parameter issues that prevented mt_natagumo and mt_yoko from spawning in earlier versions.

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

## Optional: Increase KnY Biome Spawn Frequency

If you want mt_yoko, mt_natagumo, or mugen_biome to spawn more frequently than the base mod provides, you can enable vanilla biome replacement in the config:

1. Edit `config/kimetsunoyaibamultiplayer-biome.toml`
2. Set `enableVanillaReplacement = true`
3. Adjust replacement chances:
   - `mtYokoReplacementChance` (default: 0.15 = 15% of taiga biomes)
   - `mtNatagumoReplacementChance` (default: 0.10 = 10% of dark forests)
   - `mugenReplacementChance` (default: 0.15 = 15% of savanna biomes)

This is seed-deterministic, so the same world seed will always produce the same biome layout.