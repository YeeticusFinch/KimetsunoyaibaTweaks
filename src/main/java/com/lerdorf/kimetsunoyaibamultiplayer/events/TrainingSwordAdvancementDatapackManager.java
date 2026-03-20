package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordAdvancementHelper;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class TrainingSwordAdvancementDatapackManager {
    private static final String PACK_FOLDER_NAME = "knymp_training_sword_advancements";
    private static boolean reloadAttemptedThisRun = false;

    private TrainingSwordAdvancementDatapackManager() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path datapacksDir = event.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
        Path packDir = datapacksDir.resolve(PACK_FOLDER_NAME);

        try {
            installPack(packDir);
            reloadAttemptedThisRun = false;
        } catch (Exception e) {
            System.err.println("[KnY-MP TrainingAdvancements] Failed to generate training sword datapack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (reloadAttemptedThisRun) {
            return;
        }

        PackRepository packRepository = event.getServer().getPackRepository();
        packRepository.reload();

        Optional<String> packIdOpt = findPackId(packRepository);
        if (packIdOpt.isEmpty()) {
            System.err.println("[KnY-MP TrainingAdvancements] Training sword datapack was not discovered by pack repository.");
            return;
        }

        String packId = packIdOpt.get();
        List<String> selected = new ArrayList<>(packRepository.getSelectedIds());
        if (selected.contains(packId)) {
            return;
        }

        selected.add(packId);
        reloadAttemptedThisRun = true;
        event.getServer().reloadResources(selected).exceptionally(ex -> {
            System.err.println("[KnY-MP TrainingAdvancements] Failed to reload datapacks after training sword generation: " + ex.getMessage());
            return null;
        });
    }

    private static void installPack(Path packDir) throws IOException {
        Path parent = packDir.getParent();
        if (parent == null) {
            throw new IOException("Invalid datapack destination path: " + packDir);
        }

        Path stagedDir = parent.resolve(PACK_FOLDER_NAME + ".tmp");
        removePack(stagedDir);
        Files.createDirectories(stagedDir);

        writeFile(stagedDir.resolve("pack.mcmeta"), TrainingSwordAdvancementHelper.buildPackMetadataJson());
        for (TrainingSwordAdvancementHelper.TrainingSwordAdvancementDefinition definition : TrainingSwordAdvancementHelper.getDefinitions()) {
            Path destination = stagedDir.resolve(definition.relativePath());
            writeFile(destination, TrainingSwordAdvancementHelper.buildAdvancementJson(definition));
        }

        removePack(packDir);
        Files.move(stagedDir, packDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeFile(Path destination, String contents) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(destination, contents, StandardCharsets.UTF_8);
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
