package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.raids.FinalSelectionProcedure;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.MtFujikasaneDaylightController;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Automatically downloads and deploys Mt Fujikasane region files from GitHub
 * Also sets up the vanilla world border for the dimension
 *
 * CACHING SYSTEM:
 * - Downloads region files ONCE during mod initialization (not per-world)
 * - Caches files in .minecraft/kimetsunoyaibamultiplayer/mt_fujikasane_cache/
 * - Copies cached files to new worlds on creation
 * - Never re-downloads unless cache is deleted
 *
 * SETUP INSTRUCTIONS:
 * 1. Create a repository on GitHub (e.g., YourUsername/mt-fujikasane-world)
 * 2. Upload your region files to a folder (e.g., /region/)
 * 3. Create a release and attach a zip file containing the region files
 * 4. Update GITHUB_DOWNLOAD_URL below to point to your release's zip file
 *
 * The URL should look like:
 * https://github.com/YourUsername/YourRepo/releases/download/v1.0/mt_fujikasane_region.zip
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class MtFujikasaneDimensionDataHandler {
    private static final String MT_FUJIKASANE_SUN_BURN_TICKS_TAG = "KnYMtFujikasaneSunBurnTicks";

    private static final ResourceLocation MT_FUJIKASANE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mt_fujikasane");

    // World border settings - 1000x1000 blocks centered at 0,0
    private static final double WORLD_BORDER_SIZE = 1000.0;
    private static final double WORLD_BORDER_CENTER_X = 0.0;
    private static final double WORLD_BORDER_CENTER_Z = 0.0;
    private static final java.util.List<ResourceLocation> FINAL_SELECTION_EASY_DEMON_REPLACEMENTS = java.util.List.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_2"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_3"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "spider_demon")
    );

    // GitHub repository download URL
    // Downloads the entire repository as a zip and extracts .mca files from the region/ folder
    private static final String GITHUB_DOWNLOAD_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.43/region.zip";

    // Version tracking: a small text file hosted alongside the region files
    // Prefer the release asset version file; fallback to raw main branch
    private static final String VERSION_FILE_NAME = "mt_fujikasane.version";
    // Primary version URL (release asset)
    private static final String RELEASE_VERSION_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.43/mt_fujikasane.version";
    // Fallback raw URL to fetch the version text quickly without downloading the whole zip
    private static final String RAW_VERSION_URL =
        "https://raw.githubusercontent.com/YeeticusFinch/KimetsunoyaibaTweaks/main/region/" + VERSION_FILE_NAME;

    // Set to false to disable automatic downloading (useful for testing)
    private static final boolean ENABLE_AUTO_DOWNLOAD = true;

    // Cache directory in .minecraft folder
    private static Path CACHE_DIR = null;
    private static volatile boolean cacheInitialized = false;

    // Async executor for network and IO so we never block the main thread
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MtFujikasane-IO");
        t.setDaemon(true);
        return t;
    });

    // Prevent duplicate concurrent tasks
    private static final AtomicBoolean cacheInitInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean worldCopyInProgress = new AtomicBoolean(false);

    /**
     * Initialize cache directory on first server start
     * Downloads region files ONCE and caches them for all future worlds
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MtFujikasaneDaylightController.resetRuntimeState(event.getServer());
        FinalSelectionProcedure.resetRuntimeState();

        if (!ENABLE_AUTO_DOWNLOAD) {
            return;
        }

        final MinecraftServer server = event.getServer();

        // Run cache init and world preparation asynchronously to avoid blocking startup
        IO_EXECUTOR.execute(() -> {
            try {
                ensureCacheUpToDate();
                ensureWorldPrepared(server, false);
            } catch (Exception e) {
                System.err.println("[Mt Fujikasane] Error scheduling startup tasks:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Initialize cache directory and download region files if not cached
     */
    private static void ensureCacheUpToDate() {
        if (cacheInitialized) return;
        if (!cacheInitInProgress.compareAndSet(false, true)) {
            return; // another thread is already doing this
        }

        try {
            // Get game directory (.minecraft)
            Path gameDir = FMLPaths.GAMEDIR.get();
            CACHE_DIR = gameDir.resolve("kimetsunoyaibamultiplayer").resolve("mt_fujikasane_cache");
            Files.createDirectories(CACHE_DIR);

            boolean cacheHasMca = cacheHasAnyMca();
            String localVersion = readLocalCacheVersion();
            String remoteVersion = fetchRemoteVersion();

            if (!cacheHasMca) {
                Log.debug("[Mt Fujikasane] Cache empty, downloading region files...");
                Log.debug("[Mt Fujikasane] Cache location: " + CACHE_DIR);
                Log.debug("[Mt Fujikasane] URL: " + GITHUB_DOWNLOAD_URL);
                downloadFreshCache();
            } else if (localVersion == null) {
                // Cache has MCA files but no version file — force a refresh
                Log.debug("[Mt Fujikasane] Cache missing version file — refreshing cache...");
                // Try to refresh without clearing first; extraction will overwrite/update files
                downloadFreshCache();
            } else if (remoteVersion != null && !remoteVersion.trim().equals(localVersion.trim())) {
                Log.debug("[Mt Fujikasane] Remote region version differs (local: " + (localVersion == null ? "none" : localVersion) + ", remote: " + remoteVersion + ") — updating cache...");
                clearDirectory(CACHE_DIR);
                downloadFreshCache();
            } else {
                Log.debug("[Mt Fujikasane] Cache found at: " + CACHE_DIR);
                Log.debug("[Mt Fujikasane] Using cached region files" + (localVersion != null ? (" (version " + localVersion + ")") : ""));
            }

            cacheInitialized = true;
        } catch (Exception e) {
            System.err.println("[Mt Fujikasane] Error ensuring cache is up to date:");
            e.printStackTrace();
        } finally {
            cacheInitInProgress.set(false);
        }
    }

    private static void downloadFreshCache() throws IOException {
        boolean success = downloadAndExtractRegionFiles(CACHE_DIR);
        if (success) {
            Log.debug("[Mt Fujikasane] Successfully downloaded and cached region files!");
            Log.debug("[Mt Fujikasane] Cache will be reused for all future worlds");
        } else {
            System.err.println("[Mt Fujikasane] Failed to download region files");
            System.err.println("[Mt Fujikasane] Please check the GitHub URL in MtFujikasaneDimensionDataHandler.java");
            System.err.println("[Mt Fujikasane] Dimensions will use default terrain generation");
        }
    }

    /**
     * Copy cached region files to a world's dimension directory
     */
    private static void copyCachedRegionFiles(Path targetDir) throws IOException {
        if (CACHE_DIR == null || !Files.exists(CACHE_DIR)) {
            throw new IOException("Cache directory not initialized or doesn't exist");
        }

        int filesCopied = 0;

        try (Stream<Path> files = Files.list(CACHE_DIR)) {
            for (Path cachedFile : files.toList()) {
                String name = cachedFile.getFileName().toString();
                if (name.endsWith(".mca") || name.equals(VERSION_FILE_NAME)) {
                    Path targetFile = targetDir.resolve(name);
                    Files.copy(cachedFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    if (name.endsWith(".mca")) filesCopied++;
                }
            }
        }

        Log.debug("[Mt Fujikasane] Copied " + filesCopied + " region files from cache");
    }

    /**
     * Called when a dimension/level loads - sets up vanilla world border
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        // Only run on server side
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Check if this is the Mt Fujikasane dimension
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ResourceKey<Level> dimensionKey = serverLevel.dimension();
        if (!dimensionKey.location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }

        // Set up vanilla world border
        WorldBorder border = serverLevel.getWorldBorder();

        // Set center and size
        border.setCenter(WORLD_BORDER_CENTER_X, WORLD_BORDER_CENTER_Z);
        border.setSize(WORLD_BORDER_SIZE);

        // Configure border behavior
        border.setWarningBlocks(50); // Warning when within 50 blocks
        border.setWarningTime(15); // Warning time in seconds
        border.setDamagePerBlock(0.2); // Damage when outside border

        Log.debug("[Mt Fujikasane] World border configured: " +
            (int)WORLD_BORDER_SIZE + "x" + (int)WORLD_BORDER_SIZE +
            " blocks centered at (" + (int)WORLD_BORDER_CENTER_X + ", " + (int)WORLD_BORDER_CENTER_Z + ")");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MtFujikasaneDaylightController.resetRuntimeState(event.getServer());
        FinalSelectionProcedure.resetRuntimeState();
    }

    /**
     * Check if a directory is empty
     */
    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }

        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }

    /**
     * Download and extract region files from GitHub
     * Returns true if successful, false otherwise
     */
    private static boolean downloadAndExtractRegionFiles(Path targetDir) {
        Path tempZipFile = null;

        try {
            // Create temp file for download
            tempZipFile = Files.createTempFile("mt_fujikasane_", ".zip");

            // Download zip file from GitHub
            URL url = new URL(GITHUB_DOWNLOAD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000); // 60 seconds

            // Follow redirects (GitHub releases redirect to actual file)
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Download file
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(tempZipFile.toFile())) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        // Log progress every 5MB
                        if (totalBytesRead % (5 * 1024 * 1024) == 0) {
                            Log.debug("[Mt Fujikasane] Downloaded " + (totalBytesRead / (1024 * 1024)) + " MB...");
                        }
                    }

                    Log.debug("[Mt Fujikasane] Download complete (" + (totalBytesRead / (1024 * 1024)) + " MB)");
                }

                // Extract zip file
                Log.debug("[Mt Fujikasane] Extracting region files...");
                int filesExtracted = extractZipFile(tempZipFile, targetDir);

                Log.debug("[Mt Fujikasane] Extracted " + filesExtracted + " region files");

                return filesExtracted > 0;

            } else {
                System.err.println("[Mt Fujikasane] HTTP error " + responseCode + ": " + connection.getResponseMessage());
                return false;
            }

        } catch (Exception e) {
            System.err.println("[Mt Fujikasane] Error downloading from GitHub:");
            e.printStackTrace();
            return false;

        } finally {
            // Clean up temp file
            if (tempZipFile != null) {
                try {
                    Files.deleteIfExists(tempZipFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Extract .mca files from zip to target directory
     * Returns the number of files extracted
     */
    private static int extractZipFile(Path zipFile, Path targetDir) throws IOException {
        int filesExtracted = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                // Only extract .mca files and the version file
                if (fileName.endsWith(".mca") || fileName.endsWith("/" + VERSION_FILE_NAME) || fileName.endsWith("\\" + VERSION_FILE_NAME) || fileName.equals(VERSION_FILE_NAME)) {
                    String simpleName = Paths.get(fileName).getFileName().toString();
                    Path targetFile = targetDir.resolve(simpleName);

                    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }

                    if (simpleName.endsWith(".mca")) {
                        filesExtracted++;
                        Log.debug("[Mt Fujikasane] Extracted: " + simpleName);
                    } else if (simpleName.equals(VERSION_FILE_NAME)) {
                        Log.debug("[Mt Fujikasane] Extracted version file: " + simpleName);
                    }
                }

                zis.closeEntry();
            }
        }

        return filesExtracted;
    }

    // ===== Helper methods for versioning and world prep =====

    private static boolean cacheHasAnyMca() throws IOException {
        if (CACHE_DIR == null || !Files.exists(CACHE_DIR)) return false;
        try (Stream<Path> s = Files.list(CACHE_DIR)) {
            return s.anyMatch(p -> p.getFileName().toString().endsWith(".mca"));
        }
    }

    private static String readLocalCacheVersion() {
        if (CACHE_DIR == null) return null;
        Path v = CACHE_DIR.resolve(VERSION_FILE_NAME);
        if (!Files.exists(v)) return null;
        try {
            return Files.readString(v, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static String fetchRemoteVersion() {
        HttpURLConnection connection = null;
        try {
            // Try release asset first (kept in lockstep with region.zip)
            URL url = new URL(RELEASE_VERSION_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
        // Fallback to raw URL
        try {
            URL url = new URL(RAW_VERSION_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private static void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(p -> {
                    try { if (!p.equals(dir)) Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        }
    }

    private static boolean worldHasVersionFile(Path regionDir) {
        Path versionInRegion = regionDir.resolve(VERSION_FILE_NAME);
        return Files.exists(versionInRegion);
    }

    private static void ensureWorldPrepared(MinecraftServer server, boolean logNotReady) throws IOException {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        Path worldSaveDir = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path dimensionDir = worldSaveDir.resolve("dimensions")
            .resolve("kimetsunoyaibamultiplayer")
            .resolve("mt_fujikasane");
        Path regionDir = dimensionDir.resolve("region");

        boolean needsCopy = !Files.exists(regionDir) || isDirectoryEmpty(regionDir);

        if (!needsCopy) {
            return;
        }

        if (logNotReady) {
            Log.debug("[Mt Fujikasane] Dimension not ready yet; preparing files in background...");
        }

        if (!worldCopyInProgress.compareAndSet(false, true)) {
            return; // already copying
        }

        try {
            Files.createDirectories(regionDir);

            // If cache is missing MCA files, try to download them first
            if (!cacheHasAnyMca()) {
                Log.debug("[Mt Fujikasane] Cache missing MCA files; downloading before copy...");
                ensureCacheUpToDate();
            }

            copyCachedRegionFiles(regionDir);

            // Notify players on main thread
            server.execute(() -> {
                try {
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§a[Mt Fujikasane] Dimension is ready."), false);
                } catch (Throwable t) {
                    Log.debug("[Mt Fujikasane] Dimension is ready.");
                }
            });
        } finally {
            worldCopyInProgress.set(false);
        }
    }

    // ===== Mt Fujikasane dimension-specific event handlers =====

    /**
     * Deny all natural mob spawn placement checks in Mt Fujikasane.
     * Fires before the entity is even created — most efficient prevention.
     * Programmatic spawns via level.addFreshEntity() bypass this event entirely.
     */
    @SubscribeEvent
    public static void onMtFujikasaneSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (!serverLevel.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
                return;
            }

            if (!isNaturalLikeSpawn(event.getSpawnType())) {
                return;
            }

            if (event.getPos() != null
                && shouldAllowFinalSelectionNaturalHostileSpawn(serverLevel, event.getEntityType(), event.getPos().getX(), event.getPos().getZ(), true)) {
                return;
            }

            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    /**
     * Prevent all natural mob spawning in the Mt Fujikasane dimension (late-stage safety net).
     * Only entities spawned programmatically (e.g., by raids or commands) are allowed.
     */
    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (!serverLevel.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
                return;
            }

            if (!isNaturalLikeSpawn(event.getSpawnType())) {
                return;
            }

            if (tryReplaceVanillaHostileWithEasyDemon(serverLevel, event)) {
                // Cancel original vanilla hostile spawn after successful replacement.
                event.setSpawnCancelled(true);
                return;
            }

            if (shouldAllowFinalSelectionNaturalHostileSpawn(
                serverLevel,
                event.getEntity() != null ? event.getEntity().getType() : null,
                event.getEntity() != null ? event.getEntity().getX() : 0.0D,
                event.getEntity() != null ? event.getEntity().getZ() : 0.0D,
                true
            )) {
                return;
            }

            // Deny all other natural spawns in Mt Fujikasane.
            event.setSpawnCancelled(true);
        }
    }

    private static boolean tryReplaceVanillaHostileWithEasyDemon(ServerLevel level, MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null) {
            return false;
        }

        EntityType<?> originalType = event.getEntity().getType();
        if (!isHostileEntityType(originalType) || isDemonEntityType(originalType)) {
            return false;
        }

        ResourceLocation originalId = BuiltInRegistries.ENTITY_TYPE.getKey(originalType);
        if (originalId == null || !"minecraft".equals(originalId.getNamespace())) {
            return false; // Only replace vanilla hostile mobs.
        }

        double x = event.getEntity().getX();
        double z = event.getEntity().getZ();
        if (!FinalSelectionProcedure.isInsideActiveRaidArea(level, x, z)) {
            return false;
        }

        int night = FinalSelectionProcedure.getActiveRaidNight(level);
        double replaceChance = getVanillaHostileReplacementChanceForNight(night);
        if (replaceChance <= 0.0D || level.random.nextDouble() >= replaceChance) {
            return false;
        }

        // Cap non-boss demon population during Final Selection.
        // Returning true here still cancels the original hostile spawn.
        if (!FinalSelectionProcedure.canSpawnAdditionalNonBossDemon(level)) {
            return true;
        }

        ResourceLocation demonId = FINAL_SELECTION_EASY_DEMON_REPLACEMENTS.get(level.random.nextInt(FINAL_SELECTION_EASY_DEMON_REPLACEMENTS.size()));
        EntityType<?> demonType = BuiltInRegistries.ENTITY_TYPE.get(demonId);
        if (demonType == null) {
            return false;
        }

        net.minecraft.world.entity.Entity created = demonType.create(level);
        if (!(created instanceof Mob demonMob)) {
            return false;
        }

        demonMob.moveTo(
            event.getEntity().getX(),
            event.getEntity().getY(),
            event.getEntity().getZ(),
            level.random.nextFloat() * 360.0F,
            0.0F
        );
        demonMob.setPersistenceRequired();
        level.addFreshEntity(demonMob);

        return true;
    }

    private static boolean shouldAllowFinalSelectionNaturalHostileSpawn(ServerLevel level, EntityType<?> entityType, double x, double z, boolean applyNightReduction) {
        if (!isHostileEntityType(entityType)) {
            return false;
        }
        if (!FinalSelectionProcedure.isInsideActiveRaidArea(level, x, z)) {
            return false;
        }

        // During Final Selection, reduce natural non-demon hostile spawning by night.
        if (!applyNightReduction) {
            return true;
        }
        if (isDemonEntityType(entityType)) {
            return FinalSelectionProcedure.canSpawnAdditionalNonBossDemon(level);
        }

        int night = FinalSelectionProcedure.getActiveRaidNight(level);
        double allowChance = getNonDemonHostileAllowChanceForNight(night);
        return level.random.nextDouble() < allowChance;
    }

    private static boolean isHostileEntityType(EntityType<?> entityType) {
        return entityType != null && entityType.getCategory() == MobCategory.MONSTER;
    }

    private static boolean isDemonEntityType(EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }
        ResourceLocation entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityId != null && com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization.isDemon(entityId);
    }

    private static double getNonDemonHostileAllowChanceForNight(int night) {
        return switch (night) {
            case 1 -> 0.10D; // 90% reduction
            case 2 -> 0.40D; // 60% reduction
            case 3 -> 0.60D; // 40% reduction
            case 4 -> 0.70D; // 30% reduction
            case 5 -> 0.80D; // 20% reduction
            case 6 -> 0.90D; // 10% reduction
            default -> 1.00D; // Night 7+ no reduction
        };
    }

    private static double getVanillaHostileReplacementChanceForNight(int night) {
        return switch (night) {
            case 3 -> 0.20D;
            case 4 -> 0.30D;
            case 5 -> 0.40D;
            case 6, 7 -> 0.50D;
            default -> 0.0D;
        };
    }

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    /**
     * If a chunk unloads during an active Final Selection raid, preserve trainee movement by
     * relocating some DemonSlayerEntity mobs to a nearby loaded surface chunk.
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().location().equals(MT_FUJIKASANE_DIM_ID)) {
            return;
        }
        if (!FinalSelectionProcedure.shouldRelocateDemonSlayersOnChunkUnload(level)) {
            return;
        }
        if (level.players().isEmpty()) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        ChunkPos unloadingChunkPos = chunk.getPos();
        AABB bounds = new AABB(
            unloadingChunkPos.getMinBlockX(),
            level.getMinBuildHeight(),
            unloadingChunkPos.getMinBlockZ(),
            unloadingChunkPos.getMaxBlockX() + 1,
            level.getMaxBuildHeight(),
            unloadingChunkPos.getMaxBlockZ() + 1
        );

        for (DemonSlayerEntity slayer : level.getEntitiesOfClass(DemonSlayerEntity.class, bounds, EntitySelector.ENTITY_STILL_ALIVE)) {
            // 50% chance to relocate.
            if (ThreadLocalRandom.current().nextBoolean()) {
                continue;
            }

            BlockPos safePos = findNearestLoadedSafeSurface(level, unloadingChunkPos, slayer.blockPosition());
            if (safePos == null) {
                // Safeguard: if there are no suitable loaded chunks, skip safely.
                continue;
            }

            slayer.teleportTo(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
            slayer.setDeltaMovement(0.0D, 0.0D, 0.0D);
            PathNavigation navigation = slayer.getNavigation();
            if (navigation != null) {
                navigation.stop();
            }
        }
    }

    private static BlockPos findNearestLoadedSafeSurface(ServerLevel level, ChunkPos sourceChunk, BlockPos origin) {
        final int maxSearchRadiusChunks = 16;
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int radius = 1; radius <= maxSearchRadiusChunks; radius++) {
            boolean foundAnyLoadedAtRadius = false;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int chunkX = sourceChunk.x + dx;
                    int chunkZ = sourceChunk.z + dz;

                    if (chunkX == sourceChunk.x && chunkZ == sourceChunk.z) {
                        continue;
                    }
                    if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    foundAnyLoadedAtRadius = true;
                    BlockPos candidate = findSafeSurfaceInChunk(level, chunkX, chunkZ, origin.getY());
                    if (candidate == null) {
                        continue;
                    }

                    double distSq = candidate.distSqr(origin);
                    if (distSq < nearestDistSq) {
                        nearest = candidate;
                        nearestDistSq = distSq;
                    }
                }
            }

            // Since this is a ring search, first ring with a valid position is the nearest loaded area.
            if (nearest != null) {
                return nearest;
            }

            // Continue searching outer rings even if no loaded chunks in this ring.
            if (!foundAnyLoadedAtRadius) {
                continue;
            }
        }

        return nearest;
    }

    private static BlockPos findSafeSurfaceInChunk(ServerLevel level, int chunkX, int chunkZ, int anchorY) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        // Probe center + random points inside chunk for a valid standing location.
        BlockPos centerCandidate = level.getHeightmapPos(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            new BlockPos(minX + 8, anchorY, minZ + 8)
        );
        if (isSafeSurface(level, centerCandidate)) {
            return centerCandidate;
        }

        for (int i = 0; i < 6; i++) {
            int x = minX + ThreadLocalRandom.current().nextInt(16);
            int z = minZ + ThreadLocalRandom.current().nextInt(16);
            BlockPos candidate = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, anchorY, z)
            );
            if (isSafeSurface(level, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isSafeSurface(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        return !level.getFluidState(pos).isSource();
    }

    private static void tickMtFujikasaneSunlightBurn(ServerLevel level) {
        if (level == null || !level.isDay()) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (mob instanceof AbstractDemonEntity) {
                continue;
            }
            if (!isSunlightVulnerableDemon(mob)) {
                continue;
            }

            if (isInBurningSunlight(level, mob)) {
                int burnTicks = mob.getPersistentData().getInt(MT_FUJIKASANE_SUN_BURN_TICKS_TAG) + 1;
                mob.getPersistentData().putInt(MT_FUJIKASANE_SUN_BURN_TICKS_TAG, burnTicks);
                mob.setSecondsOnFire(2);

                level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY(0.5D), mob.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.01D);
                level.sendParticles(ParticleTypes.LAVA, mob.getX(), mob.getY(0.2D), mob.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);

                if (burnTicks % 10 == 0 && burnTicks <= 40) {
                    mob.hurt(mob.damageSources().onFire(), 10.0F);
                }

                if (burnTicks >= 40) {
                    level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY(0.6D), mob.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.02D);
                    mob.playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.1F);
                    mob.discard();
                }
            } else if (mob.getPersistentData().contains(MT_FUJIKASANE_SUN_BURN_TICKS_TAG)) {
                mob.getPersistentData().remove(MT_FUJIKASANE_SUN_BURN_TICKS_TAG);
            }
        }
    }

    private static boolean isSunlightVulnerableDemon(Mob mob) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (entityId != null && com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry.isSunlightImmune(entityId)) {
            return false;
        }
        return (entityId != null && EntityCategorization.isDemon(entityId)) || mob.getPersistentData().getBoolean("oni");
    }

    private static boolean isInBurningSunlight(ServerLevel level, Mob mob) {
        if (mob.isInWaterRainOrBubble() || mob.isUnderWater()) {
            return false;
        }

        BlockPos pos = mob.blockPosition();
        return level.canSeeSky(pos) && !level.isRainingAt(pos);
    }

    /**
     * Tick the Mt Fujikasane daylight controller and final selection procedure.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // Get the Mt Fujikasane dimension if loaded
        ResourceKey<Level> mtFujikasaneKey = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            MT_FUJIKASANE_DIM_ID
        );
        ServerLevel mtFujikasane = event.getServer().getLevel(mtFujikasaneKey);
        if (mtFujikasane == null) return;

        // Tick daylight controller
        MtFujikasaneDaylightController.tick(mtFujikasane);

        // Apply sunlight death to all demons in Mt Fujikasane, including base-mod demons.
        tickMtFujikasaneSunlightBurn(mtFujikasane);

        // Tick final selection procedure
        FinalSelectionProcedure.tickActive(mtFujikasane);
    }
}
