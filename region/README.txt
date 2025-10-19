Mt Fujikasane Region Files
===========================

NOTE: This directory is NO LONGER USED for bundling region files.

The mod now uses GitHub-based automatic downloads to keep the mod file size small.

CURRENT APPROACH (GitHub Download):
------------------------------------

Region files are now hosted on GitHub and automatically downloaded when a player
creates a new world. This keeps the mod lightweight (under 5 MB).

See docs/mt-fujikasane-dimension.md for complete setup instructions.

Quick Summary:
1. Export your WorldPainter world
2. Zip the region files (.mca files)
3. Upload zip to a GitHub release
4. Update GITHUB_DOWNLOAD_URL in MtFujikasaneDimensionDataHandler.java
5. Rebuild the mod

When players install your mod and create a world, it will automatically:
- Download the region files from GitHub
- Extract them to the dimension folder
- No manual installation required!

WHY THIS APPROACH:
------------------

✅ Mod file stays small (under 5 MB vs 200+ MB)
✅ Easy to update world without rebuilding mod
✅ Still automatic for players (downloads on first launch)
✅ Works for both singleplayer and servers

For full documentation, see:
docs/mt-fujikasane-dimension.md

ALTERNATIVE (Manual Installation):
-----------------------------------

If you don't want to use GitHub, you can still manually install region files
per-world by copying them to:
saves/[WorldName]/dimensions/kimetsunoyaibamultiplayer/mt_fujikasane/region/

This is useful for testing or offline environments.
