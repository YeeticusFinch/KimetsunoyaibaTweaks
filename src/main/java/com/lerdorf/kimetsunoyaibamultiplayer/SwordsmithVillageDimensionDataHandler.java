package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class SwordsmithVillageDimensionDataHandler {

    private static final ResourceLocation SWORDSMITH_VILLAGE_DIM_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "swordsmith_village");

    private static final String DIMENSION_NAME = "Swordsmith Village";
    private static final String VERSION_FILE_NAME = "swordsmith_village.version";
    private static final String GITHUB_DOWNLOAD_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.43/swordsmith_village_region.zip";
    private static final String RELEASE_VERSION_URL =
        "https://github.com/YeeticusFinch/KimetsunoyaibaTweaks/releases/download/v1.6.43/swordsmith_village.version";
    private static final String RAW_VERSION_URL =
        "https://raw.githubusercontent.com/YeeticusFinch/KimetsunoyaibaTweaks/main/swordsmith_village_region/" + VERSION_FILE_NAME;

    private static final double WORLD_BORDER_SIZE = 2048.0D;
    private static final double WORLD_BORDER_CENTER_X = 0.0D;
    private static final double WORLD_BORDER_CENTER_Z = 0.0D;

    private static final double ENTRY_X = 3.0D;
    private static final double ENTRY_Y = 70.0D;
    private static final double ENTRY_Z = 280.0D;
    private static final float ENTRY_YAW = 180.0F;
    private static final float ENTRY_PITCH = 0.0F;

    private static final String RESIDENT_TAG = "SwordsmithVillageResident";
    private static final String RESIDENT_TYPE_TAG = "SwordsmithVillageResidentType";
    private static final int RESIDENT_CHECK_INTERVAL_TICKS = 200;
    private static final int WEATHER_ENFORCEMENT_INTERVAL_TICKS = 20;
    private static final double SPAWN_DENSITY_RADIUS = 15.0D;
    private static final int MAX_ENTITIES_PER_SPAWN_CLUSTER = 2;

    private static final ResourceLocation HYOTTOKO_MASK_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mask_hyottoko_helmet");

    private static final BlockPos CHIEF_LOCATION = new BlockPos(15, 83, 42);
    private static final List<BlockPos> CHIEF_ASSISTANT_LOCATIONS = List.of(
        new BlockPos(16, 83, 44),
                new BlockPos(16, 83, 40)
    );

    private static final List<BlockPos> SPAWN_LOCATIONS = List.of(
        new BlockPos(7, 72, 154),
        new BlockPos(0, 72, 190),
        new BlockPos(16, 73, 214),
        new BlockPos(4, 72, 69),
        new BlockPos(-11, 72, 76),
        new BlockPos(-8, 78, 100),
        new BlockPos(-13, 72, 114),
        new BlockPos(-6, 72, 101),
        new BlockPos(29, 72, 107),
        new BlockPos(27, 72, 120),
        new BlockPos(28, 77, 119),
        new BlockPos(25, 77, 112),
        new BlockPos(28, 77, 105),
        new BlockPos(-7, 72, 115),
        new BlockPos(-7, 72, 100),
        new BlockPos(-14, 78, 100),
        new BlockPos(-6, 78, 115),
        new BlockPos(-17, 74, 138),
        new BlockPos(-16, 74, 145),
        new BlockPos(-18, 79, 161),
        new BlockPos(-19, 73, 161),
        new BlockPos(-19, 73, 176),
        new BlockPos(-11, 73, 176),
        new BlockPos(25, 74, 170),
        new BlockPos(33, 79, 169),
        new BlockPos(32, 79, 140),
        new BlockPos(31, 74, 163),
        new BlockPos(33, 75, 178),
        new BlockPos(24, 79, 176),
        new BlockPos(-13, 74, 199),
        new BlockPos(-13, 74, 206),
        new BlockPos(-19, 74, 193),
        new BlockPos(-12, 80, 199),
        new BlockPos(28, 75, 199),
        new BlockPos(29, 75, 206),
        new BlockPos(-7, 76, 230),
        new BlockPos(-22, 76, 236),
        new BlockPos(-15, 83, 231),
        new BlockPos(-7, 83, 228),
        new BlockPos(21, 76, 231),
        new BlockPos(8, 74, 43),
        new BlockPos(18, 74, 36),
        new BlockPos(20, 74, 50),
        new BlockPos(0, 74, 46),
        new BlockPos(0, 78, 47),
        new BlockPos(9, 78, 38),
        new BlockPos(19, 78, 45),
        new BlockPos(11, 78, 56),
        new BlockPos(17, 77, 81),
        new BlockPos(5, 77, 107),
        new BlockPos(17, 77, 115),
        new BlockPos(-3, 74, 25),
        new BlockPos(21, 74, 25),
        new BlockPos(17, 74, 36),
        new BlockPos(6, 83, 43),
        new BlockPos(111, 155, -241),
        new BlockPos(26, 170, -285),
        new BlockPos(-27, 85, -159),
        new BlockPos(-76, 97, -88),
        new BlockPos(-47, 86, -48),
        new BlockPos(-20, 80, -32),
        new BlockPos(124, 75, 53),
        new BlockPos(224, 176, 19),
        new BlockPos(23, 79, -192)
    );

    private static final List<ResidentSpec> RESIDENT_SPECS = List.of(
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kakushi"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "haganeduka"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kotetsu"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "doctor"), true),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "toyosan"), true),
        //new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "yushiro"), false),
        new ResidentSpec(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kanawo_buyer"), true)
    );

    private static final boolean ENABLE_AUTO_DOWNLOAD = true;

    private static Path cacheDir = null;
    private static volatile boolean cacheInitialized = false;

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SwordsmithVillage-IO");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean cacheInitInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean worldCopyInProgress = new AtomicBoolean(false);
    private static final AtomicBoolean residentMaintenanceInProgress = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ENABLE_AUTO_DOWNLOAD) {
            return;
        }

        MinecraftServer server = event.getServer();
        IO_EXECUTOR.execute(() -> {
            try {
                ensureCacheUpToDate();
                ensureWorldPrepared(server, false);
            } catch (Exception e) {
                System.err.println(prefix() + " Error scheduling startup tasks:");
                e.printStackTrace();
            }
        });
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }

        WorldBorder border = serverLevel.getWorldBorder();
        border.setCenter(WORLD_BORDER_CENTER_X, WORLD_BORDER_CENTER_Z);
        border.setSize(WORLD_BORDER_SIZE);
        border.setWarningBlocks(64);
        border.setWarningTime(15);
        border.setDamagePerBlock(0.2D);

        Log.debug(prefix() + " World border configured: "
            + (int) WORLD_BORDER_SIZE + "x" + (int) WORLD_BORDER_SIZE);

        clearVillageWeather(serverLevel, "level load");
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getTo().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }

        ServerLevel targetLevel = player.getServer().getLevel(ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            SWORDSMITH_VILLAGE_DIM_ID
        ));
        if (targetLevel == null) {
            return;
        }

        player.teleportTo(targetLevel, ENTRY_X, ENTRY_Y, ENTRY_Z, ENTRY_YAW, ENTRY_PITCH);
        clearVillageWeather(targetLevel, "player entered dimension");
    }

    @SubscribeEvent
    public static void onSwordsmithVillageSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!isNaturalLikeSpawn(event.getSpawnType())) {
            return;
        }

        event.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }

        ServerLevel swordsmithVillage = event.getServer().getLevel(ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            SWORDSMITH_VILLAGE_DIM_ID
        ));
        if (swordsmithVillage == null) {
            return;
        }

        if (event.getServer().getTickCount() % WEATHER_ENFORCEMENT_INTERVAL_TICKS == 0) {
            clearVillageWeatherIfNeeded(swordsmithVillage, "server tick");
        }

        if (event.getServer().getTickCount() % RESIDENT_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (!residentMaintenanceInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            ensureResidentsPresent(swordsmithVillage);
        } finally {
            residentMaintenanceInProgress.set(false);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().location().equals(SWORDSMITH_VILLAGE_DIM_ID)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        stripTorilGateMarkers(chunk);
    }

    private static void ensureCacheUpToDate() {
        if (cacheInitialized) {
            return;
        }
        if (!cacheInitInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            Path gameDir = FMLPaths.GAMEDIR.get();
            cacheDir = gameDir.resolve("kimetsunoyaibamultiplayer").resolve("swordsmith_village_cache");
            Files.createDirectories(cacheDir);

            boolean cacheHasMca = cacheHasAnyMca();
            String localVersion = readVersionFile(cacheDir.resolve(VERSION_FILE_NAME));
            String remoteVersion = fetchRemoteVersion();

            if (!cacheHasMca) {
                Log.debug(prefix() + " Cache empty, downloading region files...");
                Log.debug(prefix() + " Cache location: " + cacheDir);
                downloadFreshCache();
            } else if (localVersion == null) {
                Log.debug(prefix() + " Cache missing version file, refreshing cache...");
                clearDirectory(cacheDir);
                downloadFreshCache();
            } else if (remoteVersion != null && !remoteVersion.trim().equals(localVersion.trim())) {
                Log.debug(prefix() + " Remote region version differs (local: " + localVersion + ", remote: " + remoteVersion + "), updating cache...");
                clearDirectory(cacheDir);
                downloadFreshCache();
            } else {
                Log.debug(prefix() + " Using cached region files" + (localVersion != null ? " (version " + localVersion + ")" : ""));
            }

            cacheInitialized = true;
        } catch (Exception e) {
            System.err.println(prefix() + " Error ensuring cache is up to date:");
            e.printStackTrace();
        } finally {
            cacheInitInProgress.set(false);
        }
    }

    private static void downloadFreshCache() throws IOException {
        boolean success = downloadAndExtractRegionFiles(cacheDir);
        if (success) {
            Log.debug(prefix() + " Successfully downloaded and cached region files.");
        } else {
            throw new IOException("Failed to download swordsmith village region files");
        }
    }

    private static boolean downloadAndExtractRegionFiles(Path targetDir) {
        Path tempZipFile = null;

        try {
            tempZipFile = Files.createTempFile("swordsmith_village_", ".zip");
            URL url = new URL(GITHUB_DOWNLOAD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println(prefix() + " HTTP error " + responseCode + ": " + connection.getResponseMessage());
                return false;
            }

            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(tempZipFile.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                Log.debug(prefix() + " Download complete (" + (totalBytesRead / (1024 * 1024)) + " MB)");
            }

            int filesExtracted = extractZipFile(tempZipFile, targetDir);
            Log.debug(prefix() + " Extracted " + filesExtracted + " region files");
            return filesExtracted > 0;
        } catch (Exception e) {
            System.err.println(prefix() + " Error downloading from GitHub:");
            e.printStackTrace();
            return false;
        } finally {
            if (tempZipFile != null) {
                try {
                    Files.deleteIfExists(tempZipFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static int extractZipFile(Path zipFile, Path targetDir) throws IOException {
        int filesExtracted = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                if (fileName.endsWith(".mca")
                    || fileName.endsWith("/" + VERSION_FILE_NAME)
                    || fileName.endsWith("\\" + VERSION_FILE_NAME)
                    || fileName.equals(VERSION_FILE_NAME)) {
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
                    }
                }

                zis.closeEntry();
            }
        }

        return filesExtracted;
    }

    private static void ensureWorldPrepared(MinecraftServer server, boolean logNotReady) throws IOException {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        Path worldSaveDir = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path dimensionDir = worldSaveDir.resolve("dimensions")
            .resolve("kimetsunoyaibamultiplayer")
            .resolve("swordsmith_village");
        Path regionDir = dimensionDir.resolve("region");

        boolean needsCopy = !Files.exists(regionDir) || isDirectoryEmpty(regionDir);

        if (!needsCopy) {
            return;
        }
        if (logNotReady) {
            Log.debug(prefix() + " Dimension not ready yet; preparing files in background...");
        }
        if (!worldCopyInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            Files.createDirectories(regionDir);
            if (!cacheHasAnyMca()) {
                ensureCacheUpToDate();
            }
            copyCachedRegionFiles(regionDir);

            server.execute(() -> {
                try {
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§a[" + DIMENSION_NAME + "] Dimension is ready."), false);
                } catch (Throwable ignored) {
                    Log.debug(prefix() + " Dimension is ready.");
                }
            });
        } finally {
            worldCopyInProgress.set(false);
        }
    }

    private static void ensureResidentsPresent(ServerLevel level) {
        for (ResidentSpec spec : RESIDENT_SPECS) {
            if (findResident(level, spec) != null) {
                continue;
            }

            BlockPos spawnPos = pickSpawnLocation(level);
            if (spawnPos == null) {
                Log.debug(prefix() + " No free spawn location available for " + spec.entityId);
                continue;
            }

            Mob mob = spawnResident(level, spawnPos, spec);
            if (mob != null) {
                Log.debug(prefix() + " Spawned resident " + spec.entityId + " at " + spawnPos);
            }
        }
    }

    private static Mob findResident(ServerLevel level, ResidentSpec spec) {
        for (Entity entity : level.getEntities().getAll()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (!mob.getPersistentData().getBoolean(RESIDENT_TAG)) {
                continue;
            }
            if (!spec.entityId.toString().equals(mob.getPersistentData().getString(RESIDENT_TYPE_TAG))) {
                continue;
            }
            return mob;
        }
        return null;
    }

    private static BlockPos pickSpawnLocation(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>(SPAWN_LOCATIONS);
        java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
        for (BlockPos pos : candidates) {
            if (canSpawnAtLocation(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canSpawnAtLocation(ServerLevel level, BlockPos pos) {
        if (isExactSpawnLocationOccupied(level, pos)) {
            return false;
        }
        return countNearbyLivingEntities(level, pos) < getAllowedEntitiesNearSpawnPoint(pos);
    }

    private static boolean isExactSpawnLocationOccupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(
                pos.getX() - 1.5D, pos.getY() - 2.0D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 3.0D, pos.getZ() + 2.5D
            ),
            living -> living.isAlive()
        ).isEmpty();
    }

    private static int countNearbyLivingEntities(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5D;
        double maxDistanceSqr = SPAWN_DENSITY_RADIUS * SPAWN_DENSITY_RADIUS;
        return (int) level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(pos).inflate(SPAWN_DENSITY_RADIUS),
            living -> living.isAlive()
        ).stream()
            .filter(living -> living.distanceToSqr(centerX, centerY, centerZ) <= maxDistanceSqr)
            .count();
    }

    private static int getAllowedEntitiesNearSpawnPoint(BlockPos pos) {
        int nearbySpawnPoints = 0;
        for (BlockPos otherPos : SPAWN_LOCATIONS) {
            if (otherPos.closerThan(pos, SPAWN_DENSITY_RADIUS + 0.001D)) {
                nearbySpawnPoints++;
            }
        }
        if (nearbySpawnPoints > 1) {
            return MAX_ENTITIES_PER_SPAWN_CLUSTER;
        }
        return 1;
    }

    private static Mob spawnResident(ServerLevel level, BlockPos pos, ResidentSpec spec) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(spec.entityId).orElse(null);
        if (entityType == null) {
            System.err.println(prefix() + " Entity type not found: " + spec.entityId);
            return null;
        }

        Entity created = entityType.create(level);
        if (!(created instanceof Mob mob)) {
            System.err.println(prefix() + " Entity is not a mob: " + spec.entityId);
            return null;
        }

        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
            ThreadLocalRandom.current().nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        mob.getPersistentData().putBoolean(RESIDENT_TAG, true);
        mob.getPersistentData().putString(RESIDENT_TYPE_TAG, spec.entityId.toString());

        if (spec.wearHyottokoMask) {
            Item maskItem = BuiltInRegistries.ITEM.getOptional(HYOTTOKO_MASK_ID).orElse(net.minecraft.world.item.Items.AIR);
            if (maskItem != net.minecraft.world.item.Items.AIR) {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(maskItem));
                mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
            }
        }

        if (level.addFreshEntity(mob)) {
            return mob;
        }
        return null;
    }

    private static void copyCachedRegionFiles(Path targetDir) throws IOException {
        if (cacheDir == null || !Files.exists(cacheDir)) {
            throw new IOException("Cache directory not initialized or missing");
        }

        try (Stream<Path> files = Files.list(cacheDir)) {
            for (Path cachedFile : files.toList()) {
                String name = cachedFile.getFileName().toString();
                if (name.endsWith(".mca") || name.equals(VERSION_FILE_NAME)) {
                    Files.copy(cachedFile, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean cacheHasAnyMca() throws IOException {
        if (cacheDir == null || !Files.exists(cacheDir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(cacheDir)) {
            return stream.anyMatch(path -> path.getFileName().toString().endsWith(".mca"));
        }
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }

    private static String fetchRemoteVersion() {
        String version = fetchVersionFromUrl(RELEASE_VERSION_URL);
        if (version != null) {
            return version;
        }
        return fetchVersionFromUrl(RAW_VERSION_URL);
    }

    private static String fetchVersionFromUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readVersionFile(Path versionFile) {
        if (versionFile == null || !Files.exists(versionFile)) {
            return null;
        }
        try {
            return Files.readString(versionFile, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean cacheHasSameVersion(String cacheVersion, String worldVersion) {
        if (cacheVersion == null) {
            return worldVersion == null;
        }
        return cacheVersion.equals(worldVersion);
    }

    private static void clearVillageWeather(ServerLevel village, String reason) {
        boolean weatherActive = hasActiveVillageWeather(village);
        clearWritableWeatherFlags(village);
        village.setWeatherParameters(12000, 0, false, false);
        if (weatherActive) {
            Log.info(prefix() + " Cleared lingering weather on {}", reason);
        }
    }

    private static void clearVillageWeatherIfNeeded(ServerLevel village, String reason) {
        if (!hasActiveVillageWeather(village)) {
            return;
        }
        clearVillageWeather(village, reason);
    }

    private static boolean hasActiveVillageWeather(ServerLevel village) {
        return village.isRaining() || village.isThundering()
            || village.getRainLevel(1.0F) > 0.0F || village.getThunderLevel(1.0F) > 0.0F;
    }

    private static void clearWritableWeatherFlags(ServerLevel village) {
        if (village.getLevelData() instanceof WritableLevelData levelData) {
            levelData.setRaining(false);
        }

        Object levelData = village.getLevelData();
        invokeBooleanSetter(levelData, "setRaining", false);
        invokeBooleanSetter(levelData, "setThundering", false);
    }

    private static void invokeBooleanSetter(Object target, String methodName, boolean value) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void stripTorilGateMarkers(LevelChunk chunk) {
        long startNanos = System.nanoTime();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int removed = 0;
        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y < maxY; y++) {
                    cursor.set(x, y, z);
                    if (!chunk.getBlockState(cursor).is(ModBlocks.TORIL_GATE_MARKER.get())) {
                        continue;
                    }

                    chunk.setBlockState(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            Log.debug(prefix() + " Removed " + removed + " Toril Gate marker block(s) from swordsmith village chunk "
                + chunk.getPos().x + "," + chunk.getPos().z);
        }
        Log.debugVisibleIfSlow(
            "swordsmith-strip-toril-markers",
            startNanos,
            50L,
            prefix() + " stripTorilGateMarkers took {} ms in chunk {},{} (removed={})",
            (System.nanoTime() - startNanos) / 1_000_000L,
            chunk.getPos().x,
            chunk.getPos().z,
            removed
        );
    }

    private static void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        if (!path.equals(dir)) {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException ignored) {
                    }
                });
        }
    }

    private static boolean isNaturalLikeSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    private static String prefix() {
        return "[" + DIMENSION_NAME + "]";
    }

    private record ResidentSpec(ResourceLocation entityId, boolean wearHyottokoMask) {
    }
}
