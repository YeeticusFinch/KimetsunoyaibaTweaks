package com.lerdorf.kimetsunoyaibamultiplayer.api;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Main API entry point for the Kimetsu no Yaiba Multiplayer mod.
 * Use this class to:
 * - Register custom breathing styles
 * - Create and register nichirin swords
 * - Access helper utilities for creating breathing forms
 *
 * This API is designed to be used by other mods that want to add
 * their own breathing techniques and swords to the KnY Multiplayer system.
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class KnYAPI {

    private KnYAPI() {
        // Utility class, no instantiation
    }

    // ==================== Style Metadata Registration ====================

    /**
     * Register metadata for a breathing style.
     * This is separate from registerBreathingStyle() and tracks parent relationships
     * and eligibility flags for color change and ore selection features.
     *
     * @param styleId Unique identifier for the style (e.g., "water_breathing")
     * @param parentStyleId Parent style ID, or null for root styles
     * @param colorChangeEligible Whether swords of this style can be obtained via color change
     * @param oreSelectionEligible Whether this style appears in ore selection (future feature)
     * @return The registered style metadata
     */
    public static StyleMetadataRegistry.StyleMetadata registerStyleMetadata(
            String styleId,
            String parentStyleId,
            boolean colorChangeEligible,
            boolean oreSelectionEligible) {
        return StyleMetadataRegistry.register(styleId, parentStyleId, colorChangeEligible, oreSelectionEligible);
    }

    /**
     * Get the metadata for a registered style.
     *
     * @param styleId The style identifier
     * @return The style metadata, or null if not found
     */
    public static StyleMetadataRegistry.StyleMetadata getStyleMetadata(String styleId) {
        return StyleMetadataRegistry.getMetadata(styleId);
    }

    /**
     * Get all styles that are eligible for color change transformation.
     *
     * @return List of color-change-eligible style metadata
     */
    public static List<StyleMetadataRegistry.StyleMetadata> getColorChangeEligibleStyles() {
        return StyleMetadataRegistry.getColorChangeEligibleStyles();
    }

    /**
     * Get all styles that are eligible for ore selection (future feature).
     *
     * @return List of ore-selection-eligible style metadata
     */
    public static List<StyleMetadataRegistry.StyleMetadata> getOreSelectionEligibleStyles() {
        return StyleMetadataRegistry.getOreSelectionEligibleStyles();
    }

    // ==================== Breathing Style Registration ====================

    /**
     * Register a new breathing style.
     *
     * @param styleId Unique identifier for this style (e.g., "frost_breathing")
     * @param styleName Display name (e.g., "Frost Breathing")
     * @param technique The BreathingTechnique with all forms
     * @param styleRange Numeric range (must be multiple of 100, e.g., 1600)
     * @param defaultParticle Default particle for sword swings
     * @return The registered style object
     */
    public static BreathingStyleRegistry.RegisteredBreathingStyle registerBreathingStyle(
            String styleId,
            String styleName,
            BreathingTechnique technique,
            int styleRange,
            ParticleOptions defaultParticle,
            Map<String, String> replaceAnimations) {
        return BreathingStyleRegistry.register(styleId, styleName, technique, styleRange, defaultParticle, replaceAnimations);
    }

    /**
     * Get a registered breathing style by ID.
     *
     * @param styleId The unique identifier
     * @return The registered style, or null if not found
     */
    public static BreathingStyleRegistry.RegisteredBreathingStyle getBreathingStyle(String styleId) {
        return BreathingStyleRegistry.getStyle(styleId);
    }

    /**
     * Get all registered breathing styles.
     *
     * @return Collection of all registered styles
     */
    public static Collection<BreathingStyleRegistry.RegisteredBreathingStyle> getAllBreathingStyles() {
        return BreathingStyleRegistry.getAllStyles();
    }

    // ==================== Sword Registration ====================

    /**
     * Start building a new nichirin sword.
     *
     * @param swordId Unique identifier for the sword (e.g., "nichirinsword_frost")
     * @return A builder for configuring and registering the sword
     */
    public static NichirinSwordBuilder createSword(String swordId) {
        return NichirinSwordBuilder.create(swordId);
    }

    /**
     * Get a registered sword by ID.
     *
     * @param swordId The unique identifier
     * @return The registered sword, or null if not found
     */
    public static SwordRegistry.RegisteredSword getSword(String swordId) {
        return SwordRegistry.getSword(swordId);
    }

    /**
     * Get a registered sword by item instance.
     *
     * @param item The sword item
     * @return The registered sword, or null if not found
     */
    public static SwordRegistry.RegisteredSword getSword(Item item) {
        return SwordRegistry.getSword(item);
    }

    /**
     * Get all registered nichirin swords (non-special).
     *
     * @return List of nichirin swords
     */
    public static List<SwordRegistry.RegisteredSword> getNichirinSwords() {
        return SwordRegistry.getNichirinSwords();
    }

    /**
     * Get all special swords.
     *
     * @return List of special swords
     */
    public static List<SwordRegistry.RegisteredSword> getSpecialSwords() {
        return SwordRegistry.getSpecialSwords();
    }

    /**
     * Get all registered swords.
     *
     * @return Collection of all registered swords
     */
    public static Collection<SwordRegistry.RegisteredSword> getAllSwords() {
        return SwordRegistry.getAllSwords();
    }

    // ==================== Helper Utilities ====================

    /**
     * Create a new BreathingForm.
     * This is a convenience method for creating breathing forms.
     *
     * @param formId Unique form ID (choose a unique range >= 20000 for your mod)
     * @param name Form name (e.g., "First Form: Glacial Slash")
     * @param description Form description
     * @param cooldownSeconds Cooldown in seconds
     * @param effect The effect to execute (receives entity, level, formId automatically)
     * @return A new BreathingForm
     */
    public static BreathingForm createForm(
            int formId,
            String name,
            String description,
            int cooldownSeconds,
            BreathingForm.FormEffect effect) {
        return new BreathingForm(formId, name, description, cooldownSeconds, effect);
    }

    /**
     * Create a new BreathingTechnique from a list of forms.
     *
     * @param name Technique name (e.g., "Frost Breathing")
     * @param forms List of forms in this technique
     * @return A new BreathingTechnique
     */
    public static BreathingTechnique createTechnique(String name, List<BreathingForm> forms) {
        return new BreathingTechnique(name, forms);
    }

    /**
     * Create a new BreathingTechnique from a list of forms with custom colors.
     *
     * @param name Technique name (e.g., "Frost Breathing")
     * @param forms List of forms in this technique
     * @param techniqueColor Minecraft color code for technique name (e.g., "§6" for gold, "" for no color)
     * @param formColor Minecraft color code for form name (e.g., "§b" for aqua, "" for no color)
     * @return A new BreathingTechnique with custom colors
     */
    public static BreathingTechnique createTechnique(String name, List<BreathingForm> forms,
                                                     String techniqueColor, String formColor) {
        return new BreathingTechnique(name, forms, techniqueColor, formColor);
    }

    // ==================== Animation Helpers ====================

    /**
     * Play an animation on a player.
     * Automatically handles client-side playback and network synchronization.
     *
     * @param player The player to animate
     * @param animationName The animation name (e.g., "sword_to_left")
     */
    public static void playAnimation(Player player, String animationName) {
        AnimationHelper.playAnimation(player, animationName);
    }

    /**
     * Play a timed animation on a player.
     *
     * @param player The player to animate
     * @param animationName The animation name
     * @param maxTicks Maximum duration in ticks
     */
    public static void playAnimation(Player player, String animationName, int maxTicks) {
        AnimationHelper.playAnimation(player, animationName, maxTicks);
    }

    /**
     * Play an animation on a specific layer with speed control.
     *
     * @param player The player to animate
     * @param animationName The animation name
     * @param maxTicks Maximum duration in ticks
     * @param speed Animation speed multiplier (1.0 = normal, 2.0 = double speed)
     * @param layer Animation layer (3000 = base, 4000 = overlay)
     */
    public static void playAnimationOnLayer(
            Player player,
            String animationName,
            int maxTicks,
            float speed,
            int layer) {
        AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
    }

    // ==================== Scheduling Helpers ====================

    /**
     * Schedule a one-time delayed action for a player.
     *
     * @param player The player
     * @param action The action to execute
     * @param delayTicks Delay in ticks
     */
    public static void scheduleOnce(Player player, Runnable action, int delayTicks) {
        AbilityScheduler.scheduleOnce(player, action, delayTicks);
    }

    /**
     * Schedule a repeating action for a player.
     *
     * @param player The player
     * @param action The action to execute
     * @param intervalTicks Interval between executions in ticks
     * @param durationTicks Total duration in ticks
     */
    public static void scheduleRepeating(
            Player player,
            Runnable action,
            int intervalTicks,
            int durationTicks) {
        AbilityScheduler.scheduleRepeating(player, action, intervalTicks, durationTicks);
    }

    // ==================== Damage Calculation ====================

    /**
     * Calculate scaled damage based on player's attack damage attribute.
     *
     * @param player The attacking player
     * @param baseDamage The base damage amount
     * @return Scaled damage value
     */
    public static float calculateScaledDamage(Player player, float baseDamage) {
        return DamageCalculator.calculateScaledDamage(player, baseDamage);
    }

    // ==================== Sword Clashing / Guard System ====================

    /**
     * Set an entity to guard/defensive state with specified defensive power.
     * This enables the sword clashing system where attacks can be deflected or mitigated.
     * Call this when a breathing technique or blood demon art starts.
     *
     * @param entity The entity using the ability
     * @param defensivePower Amount of incoming damage that can be negated (e.g., 10.0 blocks 10 damage)
     * @param abilityId Unique ID for the ability (e.g., 320.0 for Flame Breathing, 802.0 for Temari)
     */
    public static void setGuardState(net.minecraft.world.entity.LivingEntity entity, double defensivePower, double abilityId) {
        GuardStateHelper.setGuardState(entity, defensivePower, abilityId);
    }

    /**
     * Set an entity to guard/defensive state with specified defensive power.
     * Simplified version without ability ID.
     *
     * @param entity The entity using the ability
     * @param defensivePower Amount of incoming damage that can be negated
     */
    public static void setGuardState(net.minecraft.world.entity.LivingEntity entity, double defensivePower) {
        GuardStateHelper.setGuardState(entity, defensivePower);
    }

    /**
     * Set an entity to attack state with specified offensive damage.
     * Call this when performing an attack within a breathing technique or blood demon art.
     * The offensive damage value also acts as defensive power during the attack.
     *
     * @param entity The entity attacking
     * @param offensiveDamage Amount of damage to deal (also acts as defensive power)
     */
    public static void setAttackState(net.minecraft.world.entity.LivingEntity entity, double offensiveDamage) {
        GuardStateHelper.setAttackState(entity, offensiveDamage);
    }

    /**
     * Clear all guard/attack state from an entity.
     * Call this when a breathing technique or blood demon art ends.
     *
     * @param entity The entity to clear state from
     */
    public static void clearGuardState(net.minecraft.world.entity.LivingEntity entity) {
        GuardStateHelper.clearGuardState(entity);
    }

    /**
     * Enable continuous defensive state without setting a specific damage value.
     * Useful for maintaining defense between attacks in a technique sequence.
     * This allows the entity to still benefit from defensive mechanics even when
     * not actively dealing damage.
     *
     * @param entity The entity to enable defense for
     */
    public static void enableContinuousDefense(net.minecraft.world.entity.LivingEntity entity) {
        GuardStateHelper.enableContinuousDefense(entity);
    }

    /**
     * Check if an entity is currently in a guard state (can defend against attacks).
     *
     * @param entity The entity to check
     * @param attacker The entity attacking (to prevent self-damage)
     * @return true if entity can successfully guard
     */
    public static boolean isGuarding(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.LivingEntity attacker) {
        return GuardStateHelper.isGuarding(entity, attacker);
    }

    /**
     * Get the defensive power of an entity (amount of damage they can negate).
     *
     * @param entity The entity to check
     * @return The defensive power value
     */
    public static double getDefensivePower(net.minecraft.world.entity.LivingEntity entity) {
        return GuardStateHelper.getDefensivePower(entity);
    }
    
    /**
     * Enable or disable the sword clashing system globally.
     * When disabled, no sword clashing mechanics will be active.
     *
     * @param enabled true to enable sword clashing, false to disable
     */
    public static void setSwordClashingEnabled(boolean enabled) {
        Config.enableSwordClashing = enabled;
    }
    
    /**
     * Check if the sword clashing system is currently enabled.
     *
     * @return true if sword clashing is enabled, false otherwise
     */
    public static boolean isSwordClashingEnabled() {
        return Config.enableSwordClashing;
    }

    // ==================== Movement Helpers ====================

    /**
     * Access movement helper utilities.
     *
     * @return The MovementHelper class for static method access
     */
    public static Class<MovementHelper> getMovementHelper() {
        return MovementHelper.class;
    }

    /**
     * Access particle helper utilities.
     *
     * @return The ParticleHelper class for static method access
     */
    public static Class<ParticleHelper> getParticleHelper() {
        return ParticleHelper.class;
    }

    // ==================== Sword Slash Model Registration ====================

    /**
     * Register a sword to use a specific slash model.
     * This maps a sword item path to a model key for visual slash effects.
     * Client-side only - safe to call from common code but only executes on client.
     *
     * @param swordItemPath The item path (e.g., "nichirinsword_frost")
     * @param modelKey The model key (e.g., "frost") - must have corresponding geo/texture files
     */
    public static void registerSlashModel(String swordItemPath, String modelKey) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry.registerModel(swordItemPath, modelKey);
        }
    }

    /**
     * Register an animated slash texture for a model key.
     * Animated textures cycle through frames sequentially during the slash effect.
     * Client-side only - safe to call from common code but only executes on client.
     *
     * @param modelKey The model key (e.g., "water")
     * @param frameCount Number of texture frames (must be > 1)
     * @param ticksPerFrame Ticks to wait before changing to next frame (default: 2)
     */
    public static void registerAnimatedSlashTexture(String modelKey, int frameCount, int ticksPerFrame) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry.registerAnimatedTexture(modelKey, frameCount, ticksPerFrame);
        }
    }

    /**
     * Register a slash model with random texture selection.
     * Instead of animating through frames, a random texture is chosen at spawn time
     * and remains fixed for the duration of the slash effect.
     * Client-side only - safe to call from common code but only executes on client.
     *
     * @param modelKey The model key (e.g., "wind")
     * @param textureCount Number of texture variants available
     */
    public static void registerRandomSlashTexture(String modelKey, int textureCount) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry.registerRandomTexture(modelKey, textureCount);
        }
    }

    /**
     * Set whether a model key uses random texture selection instead of animation.
     * When true, a random frame is chosen at spawn time and remains fixed.
     * When false (default), textures animate sequentially through frames.
     * Client-side only - safe to call from common code but only executes on client.
     *
     * @param modelKey The model key
     * @param useRandom true for random selection, false for animation (default)
     */
    public static void setSlashTextureRandomSelection(String modelKey, boolean useRandom) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry.setRandomTextureSelection(modelKey, useRandom);
        }
    }

    /**
     * Register a custom namespace for a model key's resources.
     * Use this when your slash model files are in your own mod's assets folder.
     * Client-side only - safe to call from common code but only executes on client.
     *
     * @param modelKey The model key (e.g., "frost")
     * @param namespace The resource namespace where model/texture files live (e.g., "mymod")
     */
    public static void registerSlashModelNamespace(String modelKey, String namespace) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.lerdorf.kimetsunoyaibamultiplayer.client.models.SwordSlashModelRegistry.registerModelNamespace(modelKey, namespace);
        }
    }

    // ==================== Version Info ====================

    /**
     * Get the API version.
     *
     * @return The API version string
     */
    public static String getAPIVersion() {
        return "1.0.0";
    }

    /**
     * Check if the API version is compatible with a required version.
     *
     * @param requiredVersion The minimum required version
     * @return true if compatible
     */
    public static boolean isCompatible(String requiredVersion) {
        // Simple version comparison (can be enhanced later)
        String currentVersion = getAPIVersion();
        return currentVersion.compareTo(requiredVersion) >= 0;
    }
}
