package com.lerdorf.kimetsunoyaibamultiplayer.commands;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.DemonSlayerConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Command to spawn a demon slayer entity with a specific breathing style and power level.
 *
 * Usage: /spawndemonslayer <breathing_style> <power_level> [male|female|random] [skin_number] [demonized]
 *
 * Examples:
 * - /spawndemonslayer water_breathing 0    (training sword, no armor)
 * - /spawndemonslayer mist_breathing 3     (mist sword, full uniform, high stats)
 */
public class SpawnDemonSlayerCommand {

    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(collectSpawnableStyleIds(), builder);

    private static final SuggestionProvider<CommandSourceStack> GENDER_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(List.of("random", "male", "female"), builder);

    private static final SuggestionProvider<CommandSourceStack> SKIN_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(List.of("1", "2", "3", "4", "5", "6"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("spawndemonslayer")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .then(Commands.argument("style", StringArgumentType.string())
                .suggests(STYLE_SUGGESTIONS)
                .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                    .executes(SpawnDemonSlayerCommand::execute)
                    .then(Commands.literal("demonized")
                        .executes(SpawnDemonSlayerCommand::execute)
                    )
                    .then(Commands.argument("gender", StringArgumentType.word())
                        .suggests(GENDER_SUGGESTIONS)
                        .executes(SpawnDemonSlayerCommand::execute)
                        .then(Commands.literal("demonized")
                            .executes(SpawnDemonSlayerCommand::execute)
                        )
                        .then(Commands.argument("skin", IntegerArgumentType.integer(1, 6))
                            .suggests(SKIN_SUGGESTIONS)
                            .executes(SpawnDemonSlayerCommand::execute)
                            .then(Commands.literal("demonized")
                                .executes(SpawnDemonSlayerCommand::execute)
                            )
                        )
                    )
                )
            );

        dispatcher.register(command);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String styleId = StringArgumentType.getString(context, "style");
        int powerLevel = IntegerArgumentType.getInteger(context, "level");
        String genderArg = getOptionalGender(context);
        Integer skinArg = getOptionalSkin(context);
        boolean demonized = hasDemonizedLiteral(context);
        if (!genderArg.equals("male") && !genderArg.equals("female") && !genderArg.equals("random")) {
            source.sendFailure(Component.literal("\u00A7cInvalid gender. Use: male, female, or random"));
            return 0;
        }

        // Validate the breathing style is spawnable
        if (!collectSpawnableStyleIds().contains(styleId)) {
            source.sendFailure(Component.literal("\u00A7cUnknown breathing style: " + styleId));
            return 0;
        }

        // Find a level-0 sword for this style
        ServerLevel level = source.getLevel();
        String swordId = findSwordForStyle(styleId, level.getRandom());
        if (swordId == null) {
            source.sendFailure(Component.literal("\u00A7cNo level-0 sword found for style: " + styleId));
            return 0;
        }

        // Determine male/female from optional command arg
        boolean female;
        if ("female".equals(genderArg)) {
            female = true;
        } else if ("male".equals(genderArg)) {
            female = false;
        } else {
            female = level.getRandom().nextDouble() < DemonSlayerConfig.getFemaleSpawnChance();
        }

        EntityType<DemonSlayerEntity> entityType = female
            ? ModEntities.DEMON_SLAYER_FEMALE.get()
            : ModEntities.DEMON_SLAYER.get();

        // Spawn the entity at the command source's position
        Vec3 pos = source.getPosition();
        DemonSlayerEntity entity = entityType.create(level);
        if (entity == null) {
            source.sendFailure(Component.literal("\u00A7cFailed to create demon slayer entity."));
            return 0;
        }

        entity.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

        // Set sword ID before power level (so getArmorEquipment can reference it)
        entity.setSwordId(swordId);

        // Set texture variant. Command uses 1-based skin numbers, entity stores 0-based index.
        int maxTextures = 6;
        int textureIndex;
        if (skinArg != null) {
            int requestedIndex = skinArg - 1;
            if (requestedIndex < 0 || requestedIndex >= maxTextures) {
                source.sendFailure(Component.literal(
                    "\u00A7cInvalid skin number for " + (female ? "female" : "male") +
                    ". Valid range: 1-" + maxTextures
                ));
                return 0;
            }
            textureIndex = requestedIndex;
        } else {
            textureIndex = level.getRandom().nextInt(maxTextures);
        }
        entity.setTextureIndex(textureIndex);

        // Set power level/loadout (our override allows 0-5) and apply stat bonuses
        entity.configurePowerLevelLoadout(powerLevel);
        entity.applyAttackSpeedBonus();
        if (demonized) {
            entity.demonize();
        }

        // Add to world
        level.addFreshEntityWithPassengers(entity);

        // Get the style name for the feedback message
        BreathingStyleRegistry.RegisteredBreathingStyle style = BreathingStyleRegistry.getStyle(styleId);
        String styleName = style != null ? style.getStyleName() : formatStyleName(styleId);

        source.sendSuccess(() -> Component.literal(
            "\u00A7aSpawned " + (demonized ? "demonized " : "") + (female ? "female" : "male") + " demon slayer" +
            " [\u00A7e" + styleName + "\u00A7a]" +
            " power level \u00A7e" + powerLevel +
            "\u00A7a with sword \u00A7e" + swordId +
            "\u00A7a skin \u00A7e" + (textureIndex + 1)
        ), true);

        Log.debug("[SpawnDemonSlayer] Spawned {}{} demon slayer, style={}, power={}, sword={}",
            demonized ? "demonized " : "", female ? "female" : "male", styleId, powerLevel, swordId);

        return 1;
    }

    /**
     * Find a level-0 sword for the given breathing style.
     * Checks SwordRegistry first (this mod + addons), then SwordMetadataRegistry (base mod).
     */
    private static String findSwordForStyle(String styleId, RandomSource random) {
        List<String> candidates = new ArrayList<>();

        // Check SwordRegistry (this mod + addons)
        List<SwordRegistry.RegisteredSword> swords = SwordRegistry.getSwordsByStyleAndLevel(styleId, 0);
        for (SwordRegistry.RegisteredSword sword : swords) {
            if (sword.isObtainableInSurvival()) {
                candidates.add(sword.getSwordId());
            }
        }

        // Check SwordMetadataRegistry (base mod swords)
        List<SwordMetadataRegistry.SwordMetadata> baseSwords = SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0);
        for (SwordMetadataRegistry.SwordMetadata sword : baseSwords) {
            // Only include entries that actually resolve to an item, otherwise
            // downstream loadout code will replace with a random fallback sword.
            if (sword.isObtainableInSurvival() && canResolveSwordItem(sword.getSwordId(), sword.getSwordItem())) {
                candidates.add(sword.getSwordId());
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return null;
    }

    private static boolean canResolveSwordItem(String swordId, Item directItem) {
        if (directItem != null && !new ItemStack(directItem).isEmpty()) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(swordId);
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", swordId);
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item != null && !new ItemStack(item).isEmpty();
    }

    private static String getOptionalGender(CommandContext<CommandSourceStack> context) {
        try {
            return StringArgumentType.getString(context, "gender").toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return "random";
        }
    }

    private static Integer getOptionalSkin(CommandContext<CommandSourceStack> context) {
        try {
            return IntegerArgumentType.getInteger(context, "skin");
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean hasDemonizedLiteral(CommandContext<CommandSourceStack> context) {
        return context.getNodes().stream()
            .anyMatch(node -> "demonized".equals(node.getNode().getName()));
    }

    private static List<String> collectSpawnableStyleIds() {
        Set<String> ids = new LinkedHashSet<>();

        // All explicit breathing-style registrations (our mod + addons)
        for (BreathingStyleRegistry.RegisteredBreathingStyle style : BreathingStyleRegistry.getAllStyles()) {
            ids.add(style.getStyleId());
        }

        // Any style that currently has a valid level-0 sword in SwordRegistry
        for (SwordRegistry.RegisteredSword sword : SwordRegistry.getAllSwords()) {
            if (sword.getSwordLevel() == 0 && sword.isObtainableInSurvival()) {
                ids.add(sword.getStyleId());
            }
        }

        // Any style that currently has a valid level-0 sword in base metadata registry
        for (SwordMetadataRegistry.SwordMetadata sword : SwordMetadataRegistry.getAllSwords()) {
            if (sword.getSwordLevel() == 0 && sword.isObtainableInSurvival()) {
                ids.add(sword.getStyleId());
            }
        }

        // Base style metadata entries (base-mod style IDs)
        for (StyleMetadataRegistry.StyleMetadata style : StyleMetadataRegistry.getAllStyles()) {
            ids.add(style.getStyleId());
        }

        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        return sorted;
    }

    private static String formatStyleName(String styleId) {
        String withSpaces = styleId.replace('_', ' ');
        if (withSpaces.isEmpty()) {
            return styleId;
        }
        return Character.toUpperCase(withSpaces.charAt(0)) + withSpaces.substring(1);
    }
}
