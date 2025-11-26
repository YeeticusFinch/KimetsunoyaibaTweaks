# Kimetsu no Yaiba Mod - Biome Spawning Rules

Complete reference for entity spawning rules in the Kimetsu no Yaiba (Demon Slayer) mod for Minecraft 1.20.1 Forge.

## Table of Contents
1. [Overview](#overview)
2. [Custom Biomes](#custom-biomes)
3. [Global Spawning (All Biomes)](#global-spawning-all-biomes)
4. [Biome-Specific Spawning](#biome-specific-spawning)
5. [Spawn Weights Explained](#spawn-weights-explained)
6. [Configuration](#configuration)

---

## Overview

The Kimetsu no Yaiba mod uses **Forge Biome Modifiers** to add entity spawns to biomes. There are 74 different spawn rules defined.

### Key Points

- **82% of entities** (61/74) spawn **globally** in any biome
- **18% of entities** (13/74) spawn **only in specific biomes**
- Most spawns are **very rare** (weight of 1)
- Generic demons have **higher spawn rates** (weight 10-28)
- Specific entities spawn **1 at a time**, generic entities spawn in **groups**

---

## Custom Biomes

The mod adds 5 custom biomes:

| Biome Name | ID | Description | Features |
|------------|-------|-------------|----------|
| **Mt. Natagumo** | `kimetsunoyaiba:mt_natagumo` | Spider demon forest | Dark foliage, spider spawns, Rui's family |
| **Mt. Yoko** | `kimetsunoyaiba:mt_yoko` | Hashira training mountain | Scarlet ore, no precipitation |
| **Mt. Sagiri** | `kimetsunoyaiba:mt_sagiri` | Training mountain | Unknown features |
| **Mugen Train** | `kimetsunoyaiba:mugen_biome` | Infinity train dimension | Dark sky/fog, no spawns |
| **Enmu's Dream** | `kimetsunoyaiba:biome_enmu_dream` | Dream dimension | Unknown features |

---

## Global Spawning (All Biomes)

These entities can spawn **anywhere in the overworld** using `"biomes": {"type": "forge:any"}`.

### Demons (15 spawns)

| Entity | Weight | Group Size | Rarity |
|--------|--------|------------|--------|
| **Generic Demon** | 28 | 1 | Common |
| **Demon 2-5** | 28 each | 1 each | Common |
| **Demon 9-10** | 28 each | 1 each | Common |
| **Kyogai** (Drum Demon) | 1 | 1 | Very Rare |
| **Hand Demon** | 1 | 1 | Very Rare |
| **Temple Demon** | 1 | 1 | Very Rare |
| **Goldfishbig** | 1 | 1 | Very Rare |
| **Dice Steak Senior** | 1 | 1 | Very Rare |
| **Yahaba** | 1 | 1 | Very Rare |
| **Susamaru** | 1 | 1 | Very Rare |

### Twelve Kizuki - Upper Ranks (9 spawns)

| Entity | Weight | Group Size | Rank |
|--------|--------|------------|------|
| **Kokushibo** | 1 | 1 | Upper 1 |
| **Doma** | 1 | 1 | Upper 2 |
| **Akaza** | 1 | 1 | Upper 3 |
| **Hantengu** | 1 | 1 | Upper 4 |
| **Gyokko** | 1 | 1 | Upper 5 |
| **Daki** | 1 | 1 | Upper 6 |
| **Kaigaku** (Demon) | 1 | 1 | Upper 6 (Replacement) |
| **Muzan** | 1 | 1 | Demon King |
| **Enmu** | 1 | 1 | Lower 1 |

### Twelve Kizuki - Lower Ranks (3 spawns)

| Entity | Weight | Group Size | Rank |
|--------|--------|------------|------|
| **Rokuro** | 1 | 1 | Lower 2 |
| **Hairo** | 1 | 1 | Lower 4 |
| **Kamanue** | 1 | 1 | Lower 6 |

**Note**: Rui (Lower 5) spawns globally as well, but also has special Mt. Natagumo spawns (see below).

### Hashira (10 spawns)

| Entity | Weight | Group Size | Hashira Title |
|--------|--------|------------|---------------|
| **Tomioka** | 1 | 1 | Water Hashira |
| **Rengoku** | 1 | 1 | Flame Hashira |
| **Uzui** | 1 | 1 | Sound Hashira |
| **Muichirou** | 1 | 1 | Mist Hashira |
| **Kanroji** | 1 | 1 | Love Hashira |
| **Iguro** | 1 | 1 | Serpent Hashira |
| **Shinazugawa** | 1 | 1 | Wind Hashira |
| **Himejima** | 1 | 1 | Stone Hashira |
| **Kocho** | 1 | 1 | Insect Hashira |
| **Muichiro** | 1 | 1 | Mist Hashira (alternate spelling) |

### Kamaboko Squad (6 spawns)

| Entity | Weight | Group Size |
|--------|--------|------------|
| **Tanjiro** | 1 | 1 |
| **Nezuko** | 1 | 1 |
| **Inosuke** | 1 | 1 |
| **Zenitsu** / **Zennitsu** | 1 each | 1 each |
| **Genya** | 1 | 1 |
| **Kanawo** | 1 | 1 |

### Demon Slayers (1 spawn)

| Entity | Weight | Group Size |
|--------|--------|------------|
| **Generic Demon Slayer** | 10 | 3-5 |

### Supporting Characters (8 spawns)

| Entity | Weight | Group Size | Role |
|--------|--------|------------|------|
| **Urokodaki** | 1 | 1 | Water Breathing Master |
| **Kuwajima** | 1 | 1 | Thunder Breathing Master |
| **Haganeduka** | 1 | 1 | Swordsmith |
| **Kotetsu** | 1 | 1 | Training Assistant |
| **Kakushi** | 1 | 1 | Corps Support |
| **Murata** | 1 | 1 | Demon Slayer |
| **Toyosan** | 1 | 1 | Unknown |
| **Doctor** | 1 | 1 | Medical NPC |

### Civilians (7 spawns)

| Entity | Weight | Group Size |
|--------|--------|------------|
| **Grandmother** | 1 | 1 |
| **Yachan** | 1 | 1 |
| **Yachan Brother** | 1 | 1 |
| **Kanawo (Buyer)** | 1 | 1 |
| **Yorichi_0** | 1 | 1 |
| **Hakuji** (Human Akaza) | 1 | 1 |
| **Kaigaku (Human)** | 1 | 1 |

### Animals/Mobs (3 spawns)

| Entity | Weight | Group Size |
|--------|--------|------------|
| **Boar** | 10 | 1-3 |
| **Muscular Mouse** | 1 | 1 |
| **Kasugai Crow** | 1 | 1 |

---

## Biome-Specific Spawning

### Mt. Natagumo (`kimetsunoyaiba:mt_natagumo`)

**Theme**: Spider Demon Family territory

**Base Biome Spawns** (defined in biome JSON):
- **Spider**: Weight 20, Group of 4
- **Cave Spider**: Weight 20, Group of 4

**Biome Modifier Spawns**:

| Entity | Weight | Group Size | Notes |
|--------|--------|------------|-------|
| **Spider Demon** | 20 | 3-6 | Common spawn |
| **Rui** (Lower 5) | Weight not shown | 1 | Boss demon |
| **Rui Father** | 1 | 1 | Spider family |
| **Rui Mother** | 1 | 1 | Spider family |
| **Rui Brother** | 1 | 1 | Spider family |
| **Rui Sister** | 1 | 1 | Spider family |
| **Demon 8** | 1 | 1 | Generic demon |

**Total**: 7 unique demon spawns + vanilla spiders

**Special Features**:
- Dark green foliage (color: -10066330)
- Custom Mt. Natagumo trees
- Spider web hazards
- High spider spawn rate

**Notes**:
- This is the **only biome with multiple related spawns**
- Recreates the Mt. Natagumo arc from the anime
- Rui's family members spawn alongside him

---

### Mt. Yoko (`kimetsunoyaiba:mt_yoko`)

**Theme**: Hashira training mountain

**Base Biome Spawns**: None (empty spawners in biome JSON)

**Biome Modifier Features** (not spawns):
- **Scarlet Ore Block** (common)
- **Scarlet Ore Block Rare** (rare variant)

**Spawns**: Uses global spawn pool (Hashira can spawn here)

**Special Features**:
- Custom foliage colors (greenish tint)
- No precipitation
- Scarlet ore generation (mod's special resource)
- Custom Mt. Yoko trees

**Notes**:
- **No exclusive entity spawns**
- Only biome with **custom ore generation**
- Intended as resource/training location

---

### Mt. Sagiri (`kimetsunoyaiba:mt_sagiri`)

**Theme**: Training mountain (likely Urokodaki's location)

**Base Biome Spawns**: Unknown (biome file not fully analyzed)

**Special NPCs** (spawn globally but thematically linked):
- **Sabito** - Water Breathing student
- **Makomo** - Water Breathing student
- **Urokodaki** - Water Breathing master
- **Rui (Human)** - Pre-demon Rui

**Notes**:
- These NPCs spawn **globally**, not specifically in Mt. Sagiri
- Thematically linked to this location

---

### Mugen Train Dimension (`kimetsunoyaiba:mugen_biome`)

**Theme**: Infinity Train / Enmu's Blood Demon Art

**Base Biome Spawns**: **None** (empty spawners)

**Special Features**:
- Dark purple sky (color: -12376832)
- Dark purple fog (color: -12376832)
- No precipitation
- No natural features (empty feature list)

**Notes**:
- **Completely barren** dimension
- No entity spawns defined
- Likely used for special events/bossfights
- Enmu spawns globally, not here specifically

---

### Enmu's Dream (`kimetsunoyaiba:biome_enmu_dream`)

**Details**: Biome file exists but not fully analyzed

---

## Spawn Weights Explained

### What is Spawn Weight?

Spawn weight determines the **relative chance** an entity will be selected for spawning. Higher weight = more common.

### Weight Comparison

| Weight | Frequency | Examples |
|--------|-----------|----------|
| **28** | Very Common | Generic demons (demon, demon_2, etc.) |
| **20** | Common | Spider demon (Mt. Natagumo only) |
| **10** | Uncommon | Generic demon slayer, Boar |
| **1** | Very Rare | All named characters, Hashira, Twelve Kizuki, Muzan |

### Spawn Chance Formula

```
Spawn Chance = (Entity Weight) / (Sum of All Weights in Category)
```

**Example - Mt. Natagumo Spider Spawns**:
```
Total Weight = Spider (20) + Cave Spider (20) = 40
Spider chance = 20/40 = 50%
Cave Spider chance = 20/40 = 50%
```

**Example - Global Demon Spawns** (simplified):
```
Total Weight ≈ 28 (generic demon) + 1 (Muzan) + 1 (Kokushibo) + ... = ~50+
Generic Demon chance = 28/50 ≈ 56%
Muzan chance = 1/50 = 2%
```

### Group Size

- **minCount/maxCount**: Number of entities spawned per spawn attempt
- Most unique characters: 1-1 (single spawn)
- Generic entities:
  - Demon Slayers: 3-5 per group
  - Spider Demons: 3-6 per group
  - Boars: 1-3 per group

---

## Configuration

### Disabling Spawns

To disable specific entity spawns, you can:

1. **Remove biome modifier files** from your modpack:
   ```
   data/kimetsunoyaiba/forge/biome_modifier/[entity]_biome_modifier.json
   ```

2. **Use datapack to override**:
   Create a datapack with empty biome modifiers

3. **Use spawn control mods**:
   - In Control!
   - Mob Filter
   - JER (Just Enough Resources)

### Adjusting Spawn Rates

Create a datapack with modified biome modifiers:

```json
{
  "type": "forge:add_spawns",
  "biomes": {
    "type": "forge:any"
  },
  "spawners": {
    "type": "kimetsunoyaiba:demon",
    "weight": 50,  // Changed from 28 (makes demons more common)
    "minCount": 2,  // Changed from 1 (groups of 2+)
    "maxCount": 4   // Changed from 1
  }
}
```

### Restricting to Specific Biomes

**Example**: Make Rengoku spawn only in deserts:

```json
{
  "type": "forge:add_spawns",
  "biomes": "#minecraft:is_desert",  // Use biome tags
  "spawners": {
    "type": "kimetsunoyaiba:rengoku",
    "weight": 5,  // Higher than normal
    "minCount": 1,
    "maxCount": 1
  }
}
```

**Available biome tags**:
- `#minecraft:is_forest`
- `#minecraft:is_mountain`
- `#minecraft:is_taiga`
- `#minecraft:is_ocean`
- etc.

---

## Summary Tables

### By Spawn Location

| Location | Entity Types | Count | Notes |
|----------|--------------|-------|-------|
| **Any Biome** | Demons, Hashira, Slayers, NPCs | 61 | 82% of all spawns |
| **Mt. Natagumo** | Spider family, Rui | 7 | Only multi-spawn biome |
| **Mt. Yoko** | None (ores only) | 0 | Resource biome |
| **Mugen Biome** | None | 0 | Event dimension |

### By Entity Category

| Category | Spawn Count | Weight Range | Group Size |
|----------|-------------|--------------|------------|
| **Generic Demons** | 15 | 28 | 1 |
| **Twelve Kizuki** | 12 | 1 | 1 |
| **Hashira** | 10 | 1 | 1 |
| **Kamaboko Squad** | 6 | 1 | 1 |
| **Demon Slayers** | 1 | 10 | 3-5 |
| **Supporting Characters** | 8 | 1 | 1 |
| **Civilians** | 7 | 1 | 1 |
| **Animals** | 3 | 1-10 | 1-3 |
| **Mt. Natagumo Only** | 7 | 1-20 | 1-6 |

---

## Notes

1. **Global Spawning Dominates**: 82% of entities spawn everywhere, making any biome potentially dangerous/interesting

2. **Rarity Design**: Named characters are intentionally rare (weight 1) to feel special when encountered

3. **Mt. Natagumo is Special**: Only biome with themed, exclusive spawns recreating anime location

4. **No Dimension-Specific Spawns**: Mugen Train dimension has NO spawns defined

5. **Spawn Categories**: All spawns are categorized as "monster" type in Minecraft's spawn system

6. **Overworld Only**: All spawns target overworld biomes (no Nether/End spawns)

7. **Peaceful Mode**: These spawns respect peaceful mode settings (monsters won't spawn)

8. **Difficulty Scaling**: Spawn rates don't change with difficulty, but entity strength might

---

**Last Updated**: 2025-10-27
**Mod Version**: KimetsunoYaiba ver3-forge-1.20.1
**Source**: Decompiled biome modifier and biome definition files

**NOTE**: This documentation was created for ver2 but remains valid for ver3. The main change in ver3 is that mt_yoko and mt_natagumo biomes now spawn properly without requiring third-party fixes.
