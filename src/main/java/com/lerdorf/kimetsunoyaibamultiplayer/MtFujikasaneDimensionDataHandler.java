package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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

    private static final ResourceLocation MT_FUJIKASANE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "mt_fujikasane");

    // World border settings - 1000x1000 blocks centered at 0,0
    private static final double WORLD_BORDER_SIZE = 1000.0;
    private static final double WORLD_BORDER_CENTER_X = 0.0;
    private static final double WORLD_BORDER_CENTER_Z = 0.0;

    // GitHub repository download URL
    // Downloads the entire repository as a zip and extracts .mca files from the region/ folder
    private static final String GITHUB_DOWNLOAD_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.25/region.zip";

    // Version tracking: a small text file hosted alongside the region files
    // Prefer the release asset version file; fallback to raw main branch
    private static final String VERSION_FILE_NAME = "mt_fujikasane.version";
    // Primary version URL (release asset)
    private static final String RELEASE_VERSION_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.25/mt_fujikasane.version";
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

        // Ensure region files exist for this world in the background
        IO_EXECUTOR.execute(() -> {
            try {
                // If cache not ready or missing, attempt to prepare it (non-blocking safety)
                ensureCacheUpToDate();
                ensureWorldPrepared(serverLevel.getServer(), true);
            } catch (Exception e) {
                System.err.println("[Mt Fujikasane] Error ensuring world is prepared:");
                e.printStackTrace();
            }
        });
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

        boolean needsCopy = !Files.exists(regionDir) || isDirectoryEmpty(regionDir) || !worldHasVersionFile(regionDir);

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
}
