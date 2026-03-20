package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Installs/removes the custom progression advancement override datapack
 * in the world datapacks folder based on config.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class ProgressionOverrideDatapackManager {
    private static final String PACK_FOLDER_NAME = "knymp_progression_overrides";
    private static final String TEMPLATE_ROOT = "progression_override_pack";
    private static final List<String> TEMPLATE_FILES = List.of(
        "pack.mcmeta",
        "data/kimetsunoyaiba/advancements/demon_slayer_corps.json",
        "data/kimetsunoyaiba/advancements/mizunoto.json",
        "data/kimetsunoyaiba/advancements/mizunoe.json",
        "data/kimetsunoyaiba/advancements/kanoto.json",
        "data/kimetsunoyaiba/advancements/kanoe.json",
        "data/kimetsunoyaiba/advancements/tsuchinoto.json",
        "data/kimetsunoyaiba/advancements/tsuchinoe.json",
        "data/kimetsunoyaiba/advancements/hinoto.json",
        "data/kimetsunoyaiba/advancements/hinoe.json",
        "data/kimetsunoyaiba/advancements/kinoto.json",
        "data/kimetsunoyaiba/advancements/kinoe.json",
        "data/kimetsunoyaiba/advancements/hashira.json",
        "data/kimetsunoyaiba/advancements/demon_kill_count_10.json",
        "data/kimetsunoyaiba/advancements/demon_kill_count_20.json",
        "data/kimetsunoyaiba/advancements/demon_kill_count_30.json",
        "data/kimetsunoyaiba/advancements/demon_kill_count_40.json",
        "data/kimetsunoyaiba/advancements/demon_kill_count_50.json",
        "data/kimetsunoyaiba/advancements/kill_12_moons.json"
    );
    private static boolean reloadAttemptedThisRun = false;

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path datapacksDir = event.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
        Path packDir = datapacksDir.resolve(PACK_FOLDER_NAME);
        boolean enabled = CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get();

        try {
            if (enabled) {
                installPack(packDir);
            } else {
                removePack(packDir);
            }
            reloadAttemptedThisRun = false;
        } catch (Exception e) {
            System.err.println("[KnY-MP Progression] Failed to manage progression override datapack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (reloadAttemptedThisRun) {
            return;
        }

        boolean shouldEnable = CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get();
        PackRepository packRepository = event.getServer().getPackRepository();
        packRepository.reload();

        Optional<String> packIdOpt = findPackId(packRepository);
        if (packIdOpt.isEmpty()) {
            if (shouldEnable) {
                System.err.println("[KnY-MP Progression] Progression override datapack was not discovered by pack repository.");
            }
            return;
        }

        String packId = packIdOpt.get();
        List<String> selected = new ArrayList<>(packRepository.getSelectedIds());
        boolean currentlyEnabled = selected.contains(packId);

        if (shouldEnable == currentlyEnabled) {
            return;
        }

        if (shouldEnable) {
            selected.add(packId);
        } else {
            selected.remove(packId);
        }

        reloadAttemptedThisRun = true;
        event.getServer().reloadResources(selected).exceptionally(ex -> {
            System.err.println("[KnY-MP Progression] Failed to reload datapacks after progression toggle: " + ex.getMessage());
            return null;
        });
    }

    private static void installPack(Path packDir) throws IOException {
        validateTemplateFilesExist();

        Path parent = packDir.getParent();
        if (parent == null) {
            throw new IOException("Invalid datapack destination path: " + packDir);
        }

        Path stagedDir = parent.resolve(PACK_FOLDER_NAME + ".tmp");
        removePack(stagedDir);
        Files.createDirectories(stagedDir);

        for (String relativePath : TEMPLATE_FILES) {
            Path destination = stagedDir.resolve(relativePath);
            Files.createDirectories(destination.getParent());

            String resourcePath = TEMPLATE_ROOT + "/" + relativePath;
            try (InputStream in = ProgressionOverrideDatapackManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Missing template resource: " + resourcePath);
                }
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Swap staged pack into place only after a fully successful copy.
        removePack(packDir);
        try {
            Files.move(stagedDir, packDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(stagedDir, packDir, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void validateTemplateFilesExist() throws IOException {
        for (String relativePath : TEMPLATE_FILES) {
            String resourcePath = TEMPLATE_ROOT + "/" + relativePath;
            try (InputStream in = ProgressionOverrideDatapackManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Missing template resource: " + resourcePath);
                }
            }
        }
    }

    private static void removePack(Path packDir) throws IOException {
        if (!Files.exists(packDir)) {
            return;
        }

        try (var paths = Files.walk(packDir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        }
    }

    private static Optional<String> findPackId(PackRepository packRepository) {
        for (String id : packRepository.getAvailableIds()) {
            if (id.equals(PACK_FOLDER_NAME) ||
                id.equals("file/" + PACK_FOLDER_NAME) ||
                id.endsWith("/" + PACK_FOLDER_NAME)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
