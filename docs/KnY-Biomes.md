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

## Enhanced Mount Biomes

Kimetsunoyaiba Tweaks replaces the base mod's broad Mount Natagumo and Mount Yoko
climate entries with a seed-deterministic biome source when enabled. The source:

- Samples Minecraft's existing climate values, so only mountainous inland terrain qualifies.
- Uses smooth, low-frequency selector noise at an 800-2000 block scale.
- Selects approximately 10% of qualifying mountainous terrain by default.
- Leaves the base mod biome definitions intact, including Mount Natagumo trees and its biome-specific spawns.
- Limits each Mount Natagumo structure (`house_rui` and `house_rui_brother`) to one deterministic candidate per enhanced region.

Settings are in `config/kimetsunoyaibamultiplayer/enhanced_mount_biomes.toml`.
