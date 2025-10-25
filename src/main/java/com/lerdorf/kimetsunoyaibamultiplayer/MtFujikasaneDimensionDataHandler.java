package com.lerdorf.kimetsunoyaibamultiplayer;

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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Automatically downloads and deploys Mt Fujikasane region files from GitHub
 * Also sets up the vanilla world border for the dimension
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
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/archive/refs/heads/main.zip";

    // Set to false to disable automatic downloading (useful for testing)
    private static final boolean ENABLE_AUTO_DOWNLOAD = true;

    /**
     * Called when server has started - downloads region files if dimension is empty
     * Uses ServerStartedEvent instead of ServerAboutToStartEvent because we need
     * the overworld level to be fully loaded to access the world save directory
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ENABLE_AUTO_DOWNLOAD) {
            return;
        }

        MinecraftServer server = event.getServer();

        try {
            // Get the overworld to access the save directory
            ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld == null) {
                System.err.println("[Mt Fujikasane] Could not access overworld level");
                return;
            }

            // Build path to Mt Fujikasane dimension folder
            Path worldSaveDir = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            Path dimensionDir = worldSaveDir.resolve("dimensions")
                .resolve("kimetsunoyaibamultiplayer")
                .resolve("mt_fujikasane");
            Path regionDir = dimensionDir.resolve("region");

            // Check if region directory exists and has files
            boolean needsDownload = !Files.exists(regionDir) || isDirectoryEmpty(regionDir);

            if (!needsDownload) {
                System.out.println("[Mt Fujikasane] Region files already exist, skipping download");
                return;
            }

            // Create directories if they don't exist
            Files.createDirectories(regionDir);

            // Download and extract region files from GitHub
            System.out.println("[Mt Fujikasane] Downloading region files from GitHub...");
            System.out.println("[Mt Fujikasane] URL: " + GITHUB_DOWNLOAD_URL);
            System.out.println("[Mt Fujikasane] This may take a minute depending on your connection...");

            boolean success = downloadAndExtractRegionFiles(regionDir);

            if (success) {
                System.out.println("[Mt Fujikasane] Successfully downloaded and installed region files!");
                System.out.println("[Mt Fujikasane] Mt Fujikasane dimension is ready to explore!");
            } else {
                System.err.println("[Mt Fujikasane] Failed to download region files");
                System.err.println("[Mt Fujikasane] Please check the GitHub URL in MtFujikasaneDimensionDataHandler.java");
                System.err.println("[Mt Fujikasane] Dimension will use default terrain generation");
            }

        } catch (Exception e) {
            System.err.println("[Mt Fujikasane] Error downloading dimension data:");
            e.printStackTrace();
        }
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

        System.out.println("[Mt Fujikasane] World border configured: " +
            (int)WORLD_BORDER_SIZE + "x" + (int)WORLD_BORDER_SIZE +
            " blocks centered at (" + (int)WORLD_BORDER_CENTER_X + ", " + (int)WORLD_BORDER_CENTER_Z + ")");
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
                            System.out.println("[Mt Fujikasane] Downloaded " + (totalBytesRead / (1024 * 1024)) + " MB...");
                        }
                    }

                    System.out.println("[Mt Fujikasane] Download complete (" + (totalBytesRead / (1024 * 1024)) + " MB)");
                }

                // Extract zip file
                System.out.println("[Mt Fujikasane] Extracting region files...");
                int filesExtracted = extractZipFile(tempZipFile, targetDir);

                System.out.println("[Mt Fujikasane] Extracted " + filesExtracted + " region files");

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

                // Only extract .mca files
                if (fileName.endsWith(".mca")) {
                    // Get just the filename (remove any directory structure from the zip)
                    String mcaFileName = Paths.get(fileName).getFileName().toString();
                    Path targetFile = targetDir.resolve(mcaFileName);

                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }

                    filesExtracted++;
                    System.out.println("[Mt Fujikasane] Extracted: " + mcaFileName);
                }

                zis.closeEntry();
            }
        }

        return filesExtracted;
    }
}
