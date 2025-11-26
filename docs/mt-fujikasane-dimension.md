# Mt Fujikasane Dimension

This document explains the Mt Fujikasane dimension implementation and how to integrate a custom WorldPainter world.

## Overview

Mt Fujikasane is a separate dimension in the mod that features a pre-made 1000x1000 block world created in WorldPainter. The dimension has:

- **Overworld-like environment**: Day/night cycle, normal skybox, weather
- **1000x1000 block size**: World border enforced at ±500 blocks from spawn (0, 0)
- **Custom terrain**: Large mountain surrounded by wisteria forests
- **Quest-based access**: Players access via a quest line (teleport system)

## Dimension Files

The dimension is defined by two JSON files:

### 1. Dimension Type (`dimension_type/mt_fujikasane.json`)

Located at: `src/main/resources/data/kimetsunoyaibamultiplayer/dimension_type/mt_fujikasane.json`

This defines the dimension's properties:
- Overworld-like settings (beds work, respawn anchors don't)
- Normal day/night cycle with skylight
- Monster spawning rules (light level 0-7)
- 384 block height (Y -64 to Y 320)

### 2. Dimension Definition (`dimension/mt_fujikasane.json`)

Located at: `src/main/resources/data/kimetsunoyaibamultiplayer/dimension/mt_fujikasane.json`

This links the dimension type to the world generator:
- Uses `minecraft:noise` generator (standard terrain generation)
- Fixed biome source set to `minecraft:plains` (overridden by imported world)
- Uses overworld noise settings

## WorldPainter World Integration

### Creating the World in WorldPainter

1. **World Size**: Create a world that is exactly **1000x1000 blocks**
   - This ensures it fits within the enforced world border
   - Terrain beyond this size will not be accessible

2. **Mountain Design**:
   - Large central mountain (Mount Fujikasane)
   - Wisteria forest ring surrounding the mountain base
   - Spruce trees on mountain slopes (Y 90-140)
   - Rocky peaks and snow cap at higher elevations (Y 140+)

3. **Height Range**: Use Y levels -64 to 320 (matching dimension height)

4. **Biomes**: The imported world's biomes will override the default plains biome

### Exporting from WorldPainter

1. In WorldPainter, go to **File → Export → Minecraft World**
2. Set the export settings:
   - Platform: Java Edition
   - Version: 1.20.1
   - Game mode: Survival (or your preference)
3. Export to a temporary location
4. Navigate to the exported world folder

### Installing the World

The mod automatically downloads region files from GitHub when a new world is created. This keeps the mod file size small while still providing automatic deployment.

## Automatic Download from GitHub with Caching (Recommended)

The mod uses a smart caching system that downloads region files ONCE and reuses them for all worlds.

**How It Works:**
1. **First server start**: Downloads region files from GitHub to cache (`.minecraft/kimetsunoyaibamultiplayer/mt_fujikasane_cache/`)
2. **First world creation**: Copies cached files to world (instant)
3. **Subsequent worlds**: Copies cached files (instant, no download)
4. **Offline mode**: Works after initial download

**Benefits:**
- ✅ Downloads only once per installation
- ✅ World creation is instant after first download
- ✅ Works offline after initial cache
- ✅ Saves bandwidth
- ✅ All worlds share same cached terrain

**Setup Steps:**

### 1. Export Your WorldPainter World

1. In WorldPainter, export your world (see above)
2. Navigate to the exported world's `region/` folder
3. You should see files like: `r.0.0.mca`, `r.0.-1.mca`, `r.-1.0.mca`, etc.

### 2. Upload to GitHub

**Option A: Upload to Repository Folder (Easiest)**
1. Go to your GitHub repository (or create a new one)
2. Create a `region/` folder in the repository
3. Upload all `.mca` files to this folder
4. Commit the changes
5. Done! The mod will download from the repository automatically

**Current Implementation:**
The mod is already configured to download from:
```
https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/tree/main/region
```

**Option B: Use a Different Repository**
1. Create or use an existing GitHub repository
2. Upload your `.mca` files to a `region/` folder
3. Note your repository URL (e.g., `https://github.com/YourUsername/YourRepo`)
4. Update the download URL in the mod (see step 3 below)

### 3. Update the Mod Code (If Using Different Repository)

If you want to use a different GitHub repository, open `MtFujikasaneDimensionDataHandler.java` and update this line:

```java
private static final String GITHUB_DOWNLOAD_URL =
    "https://github.com/YourUsername/YourRepo/archive/refs/heads/main.zip";
```

Replace `YourUsername/YourRepo` with your repository path. The mod will download the entire repository as a zip and extract `.mca` files from the `region/` folder.

### 4. Rebuild and Done!

```bash
./gradlew.bat build
```

**The mod is already configured!** It will automatically download from the YeeticusFinch/KimetsunoyaibaTweaks repository.

When players install your mod and create a new world, it will automatically:
1. Detect the Mt Fujikasane dimension is empty
2. Download the zip file from GitHub
3. Extract region files to the dimension folder
4. Players can immediately explore Mt Fujikasane!

**How it Works:**

- `MtFujikasaneDimensionDataHandler` runs on first server start
- Checks if cache exists at `.minecraft/kimetsunoyaibamultiplayer/mt_fujikasane_cache/`
- If cache empty, downloads zip from GitHub and extracts to cache
- Cache is reused for all future worlds (just file copy)
- Shows progress in console logs

**Console Output (First Time):**
```
[Mt Fujikasane] Cache not found, downloading region files...
[Mt Fujikasane] Cache location: C:\Users\...\mt_fujikasane_cache
[Mt Fujikasane] Download complete (X MB)
[Mt Fujikasane] Successfully downloaded and cached region files!
[Mt Fujikasane] Cache will be reused for all future worlds
```

**Console Output (Subsequent Worlds):**
```
[Mt Fujikasane] Cache found at: C:\Users\...\mt_fujikasane_cache
[Mt Fujikasane] Using cached region files
[Mt Fujikasane] Copied 16 region files from cache
[Mt Fujikasane] Mt Fujikasane dimension is ready to explore!
```

**Advantages:**
- ✅ Keeps mod file size small (under 5 MB)
- ✅ Easy to update - just upload new release and delete cache
- ✅ Players get terrain automatically
- ✅ Works offline after first download
- ✅ No manual installation needed
- ✅ World creation is instant after first download
- ✅ Saves bandwidth (download once, use forever)

**Disadvantages:**
- ❌ Requires internet connection on first mod launch
- ❌ Small delay on first server start (download time, one-time)
- ❌ Requires GitHub account to host files

## Alternative: Manual Installation (Per-World)

If you don't want to use GitHub or want to test locally, you can manually install region files.

**When to Use:**
- Testing and development
- No GitHub account
- Offline environment
- Custom terrain per world

The world data needs to be placed in the **world save's dimension folder**.

**For a Minecraft Server/World:**

1. Locate your world save folder:
   - Singleplayer: `.minecraft/saves/[WorldName]/`
   - Server: `server-folder/[WorldName]/`

2. Navigate to the dimensions folder:
   ```
   [WorldName]/dimensions/kimetsunoyaibamultiplayer/mt_fujikasane/
   ```

3. Copy the following folders from your WorldPainter export:
   - `region/` - Contains the terrain chunks
   - `data/` - Contains world data (if present)
   - `poi/` - Points of interest (if present)
   - `entities/` - Entity data (if present)

4. Paste them into the `mt_fujikasane/` folder

**Directory Structure Example:**
```
saves/MyWorld/
├── level.dat
├── region/                    (Overworld)
├── DIM-1/                     (Nether)
├── DIM1/                      (End)
└── dimensions/
    └── kimetsunoyaibamultiplayer/
        └── mt_fujikasane/
            ├── region/        ← WorldPainter terrain files here
            ├── data/
            ├── poi/
            └── entities/
```

### Important Notes

1. **First Visit**: The dimension must be visited at least once for Minecraft to create the dimension folders
   - You may need to teleport a player to the dimension first using commands
   - Command: `/execute in kimetsunoyaibamultiplayer:mt_fujikasane run tp @p 0 100 0`

2. **Coordinate Alignment**:
   - The WorldPainter world's spawn (0, 0) will align with the dimension's (0, 0)
   - Ensure Mt Fujikasane is centered in your WorldPainter world

3. **Biome Overrides**:
   - The imported world's biome data will override the default plains biome
   - Make sure your WorldPainter world has appropriate biomes set

4. **World Border**:
   - The `MtFujikasaneWorldBorderHandler` enforces a hard boundary at ±500 blocks
   - Players will be teleported back if they exceed this range
   - Warning messages appear when approaching the border (50 blocks from edge)

## Testing the Dimension

### Using Commands

1. **Teleport to Mt Fujikasane**:
   ```
   /execute in kimetsunoyaibamultiplayer:mt_fujikasane run tp @p 0 100 0
   ```

2. **Return to Overworld**:
   ```
   /execute in minecraft:overworld run tp @p ~ ~ ~
   ```

3. **Check Current Dimension**:
   ```
   /execute store result score @p dummy run data get entity @p Dimension
   ```

### What to Test

- [ ] Dimension loads without errors
- [ ] Terrain from WorldPainter is visible
- [ ] Day/night cycle works
- [ ] Weather effects work (if enabled in WorldPainter)
- [ ] World border is visible at 500 blocks from center (1000×1000 total)
- [ ] Screen turns red when approaching border (within 50 blocks)
- [ ] Red animated particles appear at the border wall
- [ ] Players take damage when crossing the border
- [ ] Wisteria forest demon protection works (if demons spawn)

## Code Reference

### Key Classes

1. **MtFujikasaneDimensionDataHandler.java**
   - Downloads region files from GitHub when dimension is empty
   - Sets up vanilla Minecraft world border (1000×1000 blocks)
   - Checks if dimension is empty on server start
   - Extracts region files from zip to world save
   - Only downloads once per world (never overwrites existing files)
   - Location: `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/MtFujikasaneDimensionDataHandler.java`

2. **Dimension Files**
   - Dimension type: `src/main/resources/data/kimetsunoyaibamultiplayer/dimension_type/mt_fujikasane.json`
   - Dimension: `src/main/resources/data/kimetsunoyaibamultiplayer/dimension/mt_fujikasane.json`

### Configuration

The world border is configured in `MtFujikasaneDimensionDataHandler.java`:

```java
// World border settings - 1000x1000 blocks centered at 0,0
private static final double WORLD_BORDER_SIZE = 1000.0;
private static final double WORLD_BORDER_CENTER_X = 0.0;
private static final double WORLD_BORDER_CENTER_Z = 0.0;
```

The vanilla world border provides:
- **Visual barrier** - Red animated barrier particles when approaching
- **Warning effect** - Screen turns red near the edge
- **Damage** - Players take damage when outside the border (0.2 damage per block)
- **Warning distance** - 50 blocks from edge
- **No teleporting** - Players can still try to go beyond but take damage

To change the world size, modify `WORLD_BORDER_SIZE` (total size in blocks, not radius).

## Future Enhancements

- Quest-based teleportation system (chat message click to teleport)
- Custom dimension skybox or effects
- Spawn point management for first entry
- Integration with mod's quest system
- Custom demon spawn rules specific to Mt Fujikasane
- Wisteria forest generation at dimension borders

## Troubleshooting

### GitHub Download Issues

**Problem**: Dimension loads but shows default terrain (not WorldPainter world)

**Solution**:
- Check server/game logs for `[Mt Fujikasane]` messages during startup
- Look for download/extraction messages or errors
- Verify the GitHub URL in `MtFujikasaneDimensionDataHandler.java` is correct
- Test the URL in a browser - it should download the zip file
- Make sure `ENABLE_AUTO_DOWNLOAD` is set to `true` in the code

**Problem**: Console shows "HTTP error 404" or "Failed to download"

**Solution**:
- The GitHub URL is incorrect or the release doesn't exist
- Make sure you published the release (drafts won't work)
- For private repos, the download might be blocked - use public release
- Copy the exact URL from the release page (right-click zip file → copy link)
- URL format: `https://github.com/User/Repo/releases/download/v1.0/filename.zip`

**Problem**: Download works but "Extracted 0 region files"

**Solution**:
- Open your zip file and verify it contains `.mca` files
- Make sure files have the `.mca` extension (not `.mcc` or other)
- Files can be in the root of the zip or in a folder - both work
- Don't zip the entire world export - only zip the region files themselves

**Problem**: Files downloaded once but I want to update them

**Solution**:
- Upload a new release to GitHub with updated region files
- Update the URL in `MtFujikasaneDimensionDataHandler.java` (if version changed)
- **Delete the cache folder**: `.minecraft/kimetsunoyaibamultiplayer/mt_fujikasane_cache/`
- Restart the server - new files will be downloaded to cache
- Existing worlds keep old terrain, new worlds get new terrain
- To update existing world: delete that world's region folder: `saves/[World]/dimensions/kimetsunoyaibamultiplayer/mt_fujikasane/region/`

**Problem**: "Connection timeout" or download is very slow

**Solution**:
- Your internet connection might be slow or unstable
- GitHub might be experiencing issues
- Try again later
- Or use manual installation method as fallback
- Consider hosting on a faster CDN if GitHub is too slow

### Manual Installation

**Problem**: Dimension loads but shows default terrain (not WorldPainter world)

**Solution**:
- Ensure world files are in correct location: `saves/[World]/dimensions/kimetsunoyaibamultiplayer/mt_fujikasane/region/`
- Delete the dimension folder and re-copy WorldPainter files
- Make sure you're using the correct world save folder

### General Issues

**Problem**: Dimension won't load / crashes on entry

**Solution**:
- Check logs for errors related to dimension loading
- Verify JSON files are valid (use a JSON validator)
- Ensure dimension_type and dimension files match the correct namespace

**Problem**: World border not working

**Solution**:
- Check server logs for `[Mt Fujikasane] World border configured` message
- Verify the dimension ResourceLocation matches: `kimetsunoyaibamultiplayer:mt_fujikasane`
- Try using `/worldborder get` command while in the dimension to check current settings
- Make sure you're in the Mt Fujikasane dimension, not the Overworld

**Problem**: Players fall through world / spawn in void

**Solution**:
- Ensure WorldPainter world has terrain at spawn point (0, 0)
- Set spawn point to safe Y level (e.g., Y 100) when teleporting

## Region Versioning

To make the Mt Fujikasane region cache robust and updatable, the mod supports a simple version file that lives next to the region files.

- File name: `mt_fujikasane.version`
- Location in repo: `region/mt_fujikasane.version`
- Contents: any short text like `v1`, `2025-11-06`, etc. (first line is used)

How it works:
- On server start, the mod fetches the version file from GitHub and compares it to the local cache’s version (if present).
- If the version differs or the cache is empty, the mod re-downloads and refreshes the cache asynchronously.
- The version file is also copied into each world’s Mt Fujikasane dimension folder at:
  `saves/[World]/dimensions/kimetsunoyaibamultiplayer/mt_fujikasane/region/mt_fujikasane.version`
- World creation and dimension loading never block; if the region isn’t ready yet, the mod logs a console message and finalizes setup in the background, then announces readiness via a chat message.

Notes:
- Keep the `mt_fujikasane.version` file in sync with your uploaded `.mca` set. Bumping the version triggers cache refresh on next start.
- Works on dedicated servers and singleplayer (integrated server) alike.
- Check that region files were copied correctly
