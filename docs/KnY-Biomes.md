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
climate entries with a seed-deterministic biome source when enabled. Mount Natagumo uses
the same ring region mask for biome and terrain generation. The source:

- Places one deterministic candidate center on each ring at 3000, 6000, 9000, and later blocks from spawn.
- Uses a continuous concentric 800-block terrain region with a smooth radial falloff from a Y=75 base toward a Y=280 summit target.
- Shapes terrain through one final-density vertical sampling pass with only small-scale detail variation; the surface is hard-capped at Y=280.
- Can optionally replace ocean positions inside selected regions; the setting defaults to allowing ocean overwrite.
- Adds one deterministic, gently meandering river from the south side of each peak toward its southern foothills during the completed surface pass.
- The river leaves the mountain density untouched and follows the completed local surface with a 3-block water corridor; its bed is exactly one block below the water and uses gravel.
- Adds common 15-25 block dark oak trees and 10-20 block dark oak wall trees with narrow conical dark oak crowns, alongside the existing mega spruce generation. Trees require grass blocks.
- Generates two persistent decorative silk cocoons per newly loaded Sister-region chunk, preferring positions beside detected trees and marking each entity with `Decoration: true`. Chunk claims are saved so reloads do not duplicate them.
- Persists discovered peak centers in `kimetsunoyaibamultiplayer_natagumo_peaks` world data for later navigation or structure logic.
- Leaves the base mod biome definitions intact, including Mount Natagumo trees and its biome-specific spawns.
- Limits each Mount Natagumo structure (`house_rui` and `house_rui_brother`) to one deterministic candidate per enhanced region.

Settings are in `serverconfig/kimetsunoyaibamultiplayer/enhanced_mount_biomes.toml`. Use `/natagumoregion` to report the imaginary ring or Boss Minions sub-region at the player's position.
