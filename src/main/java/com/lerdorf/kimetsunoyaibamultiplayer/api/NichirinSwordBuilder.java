package com.lerdorf.kimetsunoyaibamultiplayer.api;

import java.util.Map;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Builder class for creating and registering nichirin swords.
 * Provides a fluent API for sword creation with automatic registration
 * into the appropriate systems (items, breathing styles, particles, etc.).
 *
 * Example usage:
 * <pre>
 * NichirinSwordBuilder.create("mysword_frost")
 *     .breathingStyle("frost_breathing", FrostBreathingForms.createFrostBreathing())
 *     .styleRange(1600)
 *     .defaultParticle(ParticleTypes.SNOWFLAKE)
 *     .category(SwordRegistry.SwordCategory.NICHIRIN)
 *     .durability(2000)
 *     .build(ITEMS);
 * </pre>
 */
public class NichirinSwordBuilder {

    private final String swordId;
    private String breathingStyleId;
    private BreathingTechnique breathingTechnique;
    private int styleRange = -1;
    private ParticleOptions defaultParticle;
    private ParticleOptions swordParticle;
    private Map<String, String> replaceAnimations;
    private SoundEvent swingSound;
    private SwordRegistry.SwordCategory category = SwordRegistry.SwordCategory.NICHIRIN;
    private int durability = 2000;
    private boolean registerToCreativeTab = true;

    private NichirinSwordBuilder(String swordId) {
        this.swordId = swordId;
    }

    /**
     * Start building a new nichirin sword.
     *
     * @param swordId Unique identifier for the sword (e.g., "nichirinsword_frost")
     * @return A new builder instance
     */
    public static NichirinSwordBuilder create(String swordId) {
        return new NichirinSwordBuilder(swordId);
    }

    /**
     * Set the breathing style for this sword.
     * This will automatically register the breathing style if it hasn't been registered yet.
     *
     * @param styleId Unique identifier for the breathing style
     * @param technique The BreathingTechnique containing all forms
     * @return This builder for chaining
     */
    public NichirinSwordBuilder breathingStyle(String styleId, BreathingTechnique technique) {
        this.breathingStyleId = styleId;
        this.breathingTechnique = technique;
        return this;
    }

    /**
     * Set the style range number (e.g., 1000 for Ice, 1600 for Frost).
     * Must be a multiple of 100.
     *
     * @param styleRange The numeric range for this breathing style
     * @return This builder for chaining
     */
    public NichirinSwordBuilder styleRange(int styleRange) {
        if (styleRange % 100 != 0) {
            throw new IllegalArgumentException("Style range must be a multiple of 100, got: " + styleRange);
        }
        this.styleRange = styleRange;
        return this;
    }

    /**
     * Set the default particle for the breathing style.
     * This particle will be used for all swords of this style unless overridden.
     *
     * @param particle The particle to use
     * @return This builder for chaining
     */
    public NichirinSwordBuilder defaultParticle(ParticleOptions particle) {
        this.defaultParticle = particle;
        return this;
    }

    public NichirinSwordBuilder replaceAnimations(Map<String, String> replaceAnimations) {
    	this.replaceAnimations = replaceAnimations;
    	return this;
    }
    
    /**
     * Set a custom particle specifically for this sword.
     * This overrides the style's default particle.
     *
     * @param particle The particle to use for this sword
     * @return This builder for chaining
     */
    public NichirinSwordBuilder swordParticle(ParticleOptions particle) {
        this.swordParticle = particle;
        return this;
    }

    /**
     * Set a custom swing sound effect for this sword.
     * This sound plays in addition to the default sweep attack sound when the sword is swung.
     *
     * @param sound The sound event to play on sword swing
     * @return This builder for chaining
     */
    public NichirinSwordBuilder swingSound(SoundEvent sound) {
        this.swingSound = sound;
        return this;
    }

    /**
     * Set the sword category (NICHIRIN or SPECIAL).
     *
     * @param category The category for this sword
     * @return This builder for chaining
     */
    public NichirinSwordBuilder category(SwordRegistry.SwordCategory category) {
        this.category = category;
        return this;
    }

    /**
     * Set the sword durability.
     *
     * @param durability The durability value (default: 2000)
     * @return This builder for chaining
     */
    public NichirinSwordBuilder durability(int durability) {
        this.durability = durability;
        return this;
    }

    /**
     * Set whether this sword should be added to the creative tab.
     *
     * @param register true to add to creative tab (default), false to exclude
     * @return This builder for chaining
     */
    public NichirinSwordBuilder registerToCreativeTab(boolean register) {
        this.registerToCreativeTab = register;
        return this;
    }

    /**
     * Build and register the sword.
     * This will:
     * 1. Register the breathing style if not already registered
     * 2. Create the sword item
     * 3. Register the sword with Forge's item registry
     * 4. Register the sword with SwordRegistry
     * 5. Configure particle mappings
     *
     * @param itemRegistry The DeferredRegister for items from your mod
     * @return A RegistryObject containing the created sword
     * @throws IllegalStateException if required fields are not set
     */
    public RegistryObject<Item> build(DeferredRegister<Item> itemRegistry) {
        validate();

        // Register breathing style if not already registered
        if (!BreathingStyleRegistry.isRegistered(breathingStyleId)) {
            String styleName = breathingTechnique.getName();
            BreathingStyleRegistry.register(
                breathingStyleId,
                styleName,
                breathingTechnique,
                styleRange,
                defaultParticle,
                replaceAnimations
            );
        }

        // Store references for the closure
        final String finalSwordId = this.swordId;
        final String finalBreathingStyleId = this.breathingStyleId;
        final SwordRegistry.SwordCategory finalCategory = this.category;
        final ParticleOptions finalSwordParticle = this.swordParticle;
        final ParticleOptions finalDefaultParticle = this.defaultParticle;
        final SoundEvent finalSwingSound = this.swingSound;
        final BreathingTechnique finalTechnique = this.breathingTechnique;
        final int finalDurability = this.durability;
        final Map<String, String> finalReplaceAnimations = this.replaceAnimations;

        // Create the sword item
        RegistryObject<Item> swordRegistry = itemRegistry.register(swordId, () -> {
            BreathingSwordItem sword = new BreathingSwordItem(
                new Item.Properties().stacksTo(1).durability(finalDurability)
            ) {
                @Override
                public BreathingTechnique getBreathingTechnique() {
                    return finalTechnique;
                }
            };

            // Register metadata when the item is created
            // This happens during mod initialization after DeferredRegister processes entries
            if (!SwordRegistry.isRegistered(finalSwordId)) {
                SwordRegistry.register(
                    finalSwordId,
                    sword,
                    finalBreathingStyleId,
                    finalCategory,
                    finalSwordParticle,
                    finalSwingSound,
                    finalReplaceAnimations,
                    registerToCreativeTab
                );

                // Register particle mapping
                ParticleOptions effectiveParticle = finalSwordParticle != null ? finalSwordParticle : finalDefaultParticle;
                if (effectiveParticle != null) {
                    ResourceLocation particleId = getParticleId(effectiveParticle);
                    if (particleId != null) {
                        com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping
                            .registerCustomMapping(finalSwordId, particleId);
                    }
                }
            }

            return sword;
        });

        return swordRegistry;
    }

    /**
     * Validate that all required fields are set.
     */
    private void validate() {
        if (breathingStyleId == null || breathingStyleId.isEmpty()) {
            throw new IllegalStateException("Breathing style ID must be set");
        }
        if (breathingTechnique == null) {
            throw new IllegalStateException("Breathing technique must be set");
        }
        if (styleRange < 0) {
            throw new IllegalStateException("Style range must be set");
        }
        if (defaultParticle == null && swordParticle == null) {
            throw new IllegalStateException("At least one of defaultParticle or swordParticle must be set");
        }
    }

    /**
     * Extract the ResourceLocation ID from a ParticleOptions.
     * This is a best-effort attempt and may not work for all particle types.
     */
    private ResourceLocation getParticleId(ParticleOptions particle) {
        try {
            var particleType = particle.getType();
            return ForgeRegistries.PARTICLE_TYPES.getKey(particleType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get whether this sword should be registered to the creative tab.
     * Used by external systems to decide whether to add the sword to creative tabs.
     *
     * @return true if the sword should be in creative tabs
     */
    public boolean shouldRegisterToCreativeTab() {
        return registerToCreativeTab;
    }

    /**
     * Get the sword ID being built.
     *
     * @return The sword ID
     */
    public String getSwordId() {
        return swordId;
    }

    /**
     * Get the category being built.
     *
     * @return The sword category
     */
    public SwordRegistry.SwordCategory getCategory() {
        return category;
    }
}
