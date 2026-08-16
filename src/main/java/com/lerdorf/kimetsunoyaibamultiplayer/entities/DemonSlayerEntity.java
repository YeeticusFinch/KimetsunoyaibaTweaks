package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BreathingStyleRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.client.EntityCombatStateTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SlayerFleshHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Generic Demon Slayer entity that spawns with a random nichirin sword
 * from any registered breathing style (base mod, this mod, or addons).
 *
 * Two entity types share this class:
 * - demon_slayer (male) — uses biped.geo.json with mob_slayer textures
 * - demon_slayer_female — uses biped_female.geo.json with slayer_female textures
 *
 * Power levels 0-5:
 * - Level 0: Training sword, no armor, 20 HP
 * - Level 1-4: Normal sword, standard uniform, scaling stats
 * - Level 5: Super senior with three style swords and periodic sword switching
 */
public class DemonSlayerEntity extends BreathingSlayerEntity {
    private static final int MAX_SWORD_ASSIGN_RETRIES = 12;
    private static final int MAX_POWER_LEVEL = 5;
    private static final int FALL_RECOVERY_TICKS = 30;
    private static final int KICK_COOLDOWN_MIN = 50;
    private static final int KICK_COOLDOWN_MAX = 95;
    private static final int SUPER_SENIOR_SWITCH_MIN_TICKS = 80;
    private static final int SUPER_SENIOR_SWITCH_MAX_TICKS = 180;
    private static final int FLEE_REPATH_INTERVAL_TICKS = 8;
    private static final double FLEE_SPEED_MULTIPLIER = 1.35D;
    private static final int FLEE_AWAY_RANGE = 20;
    private static final int FLEE_AWAY_VERTICAL_RANGE = 8;
    private static final String SLAYERS_BLOOD_CAPTIVE_TAG = "slayers_blood_captive";
    private static final String FINAL_SELECTION_PATHING_TAG = "FinalSelectionPathing";
    private static final double KICK_DAMAGE = 2.5D;
    private static final double KICK_KNOCKBACK = 0.6D;
    private static final String[] DICE_STEAK_DROPS = {
        "dice_steak_arm",
        "dice_steak_body",
        "dice_steak_head_1",
        "dice_steak_head_2",
        "dice_steak_head_3",
        "dice_steak_head_4",
        "dice_steak_leg"
    };
    private static final Set<String> DISALLOWED_RANDOM_SWORD_PATH_KEYS = Set.of(
        "ice_shirasaya"
    );

    // Synced data: sword registry ID (e.g., "nichirinsword_water" or "kimetsunoyaiba:nichirinsword_water")
    private static final EntityDataAccessor<String> SWORD_ID =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.STRING);

    // Synced data: texture variant index (0-5 for male/female)
    private static final EntityDataAccessor<Integer> TEXTURE_INDEX =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LOW_LEVEL_FLEEING =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHEATHE_ON_BACK =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PURPLE_UNIFORM_VARIANT =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FINAL_SELECTION_PATHING_ACTIVE =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> FINAL_SELECTION_PATH_SPEED =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.FLOAT);
    // Synced data: level-5 additional sword ids (for extra sheaths/sword switching)
    private static final EntityDataAccessor<String> ALT_SWORD_ID_1 =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ALT_SWORD_ID_2 =
        SynchedEntityData.defineId(DemonSlayerEntity.class, EntityDataSerializers.STRING);

    // Cached breathing technique (resolved from sword ID)
    private BreathingTechnique cachedTechnique = null;

    // UUID for attack speed modifier
    private static final UUID DEMON_SLAYER_ATTACK_SPEED_UUID =
        UUID.fromString("d3a0b514-7e3f-4a00-a77c-5b33d0000001");
    private int lastDamageTick = -1000;
    private int kickCooldownTicks = 0;
    private int fallRecoveryTicks = 0;
    private boolean heavyLandingPending = false;
    private int superSeniorSwordSwitchTicks = 0;
    private int fleeRepathTicks = 0;
    private boolean sheathPreferenceInitialized = false;
    private int movementAnimationGraceTicks = 0;
    private boolean movementAnimationLatched = false;
    private int sprintAnimationGraceTicks = 0;
    private double smoothedHorizontalSpeed = 0.0D;
    private String latchedMovementAnimation = "idle";
    private int movementAnimationSwitchCooldownTicks = 0;
    private int movementStateHoldTicks = 0;
    private int sprintStateHoldTicks = 0;
    private double smoothedAnimationSpeed = 1.0D;
    private static final java.util.Map<String, RawAnimation> LOOPING_MOVEMENT_ANIMATIONS = new java.util.HashMap<>();

    /**
     * Holds info about an eligible sword for random selection.
     */
    private record EligibleSword(String swordId, String styleId, Item item) {}

    public DemonSlayerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Breathing forms
        this.goalSelector.addGoal(0, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.BreathingFormAttackGoal(this));

        // Priority 1: Float
        this.goalSelector.addGoal(1, new FloatGoal(this));

        // Priority 2: Level 3+ front-flip gap closer
        this.goalSelector.addGoal(2, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.DemonSlayerFrontFlipGoal(this));

        // Priority 3: Level 3+ dodge/backstep
        this.goalSelector.addGoal(3, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.DemonSlayerBackstepGoal(this));

        // Priority 4: Level 2+ guard
        this.goalSelector.addGoal(4, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.DemonSlayerGuardGoal(this));

        // Priority 5: Basic melee
        this.goalSelector.addGoal(5, new com.lerdorf.kimetsunoyaibamultiplayer.entities.ai.AnimatedMeleeAttackGoal(this, 1.0D, false));

        // Priority 6+: Navigation/idle
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            target -> !this.isDemonized() && !this.isFinalSelectionPathingActive() && DemonSlayerAggroHandler.isDemonTarget(target)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            monster -> !this.isDemonized()
                && !this.isFinalSelectionPathingActive()
                && !DemonSlayerAggroHandler.isDemonTarget(monster)
                && !EntityTagHelper.isDemonSlayer(monster)
                && !EntityTagHelper.isHashira(monster)
                && !EntityTagHelper.isKamaboko(monster)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
            (player) -> !this.isDemonized() && !this.isFinalSelectionPathingActive() && player.getPersistentData().getBoolean("oni")));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SWORD_ID, "");
        this.entityData.define(TEXTURE_INDEX, 0);
        this.entityData.define(LOW_LEVEL_FLEEING, false);
        this.entityData.define(SHEATHE_ON_BACK, false);
        this.entityData.define(PURPLE_UNIFORM_VARIANT, false);
        this.entityData.define(FINAL_SELECTION_PATHING_ACTIVE, false);
        this.entityData.define(FINAL_SELECTION_PATH_SPEED, 0.0F);
        this.entityData.define(ALT_SWORD_ID_1, "");
        this.entityData.define(ALT_SWORD_ID_2, "");
    }

    // --- Sword ID ---

    public String getSwordId() {
        return this.entityData.get(SWORD_ID);
    }

    public void setSwordId(String swordId) {
        this.entityData.set(SWORD_ID, swordId);
        this.cachedTechnique = null; // Invalidate cache
    }

    // --- Texture Index ---

    public int getTextureIndex() {
        return this.entityData.get(TEXTURE_INDEX);
    }

    public void setTextureIndex(int index) {
        this.entityData.set(TEXTURE_INDEX, index);
    }

    public String getAltSwordId1() {
        return this.entityData.get(ALT_SWORD_ID_1);
    }

    public void setAltSwordId1(String swordId) {
        this.entityData.set(ALT_SWORD_ID_1, swordId == null ? "" : swordId);
    }

    public String getAltSwordId2() {
        return this.entityData.get(ALT_SWORD_ID_2);
    }

    public void setAltSwordId2(String swordId) {
        this.entityData.set(ALT_SWORD_ID_2, swordId == null ? "" : swordId);
    }

    // --- Gender check ---

    public boolean isFemale() {
        return this.getType() == ModEntities.DEMON_SLAYER_FEMALE.get();
    }

    public boolean isActionLocked() {
        return this.fallRecoveryTicks > 0;
    }

    public boolean isLowLevelFleeing() {
        return this.entityData.get(LOW_LEVEL_FLEEING);
    }

    public boolean isDisarmed() {
        return this.getPersistentData().getBoolean(SLAYERS_BLOOD_CAPTIVE_TAG);
    }

    @Override
    protected void refreshPowerStateForDemonizedChange() {
        configurePowerLevelLoadout(getPowerLevel());
    }

    @Override
    protected boolean shouldRandomizeDemonizedEyeHue() {
        return true;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return this.isDisarmed() && super.canBeLeashed(player);
    }

    private void setLowLevelFleeing(boolean fleeing) {
        this.entityData.set(LOW_LEVEL_FLEEING, fleeing);
    }

    public boolean isSheatheOnBack() {
        return this.entityData.get(SHEATHE_ON_BACK);
    }

    private void setSheatheOnBack(boolean value) {
        this.entityData.set(SHEATHE_ON_BACK, value);
        this.sheathPreferenceInitialized = true;
    }

    public boolean isPurpleUniformVariant() {
        return this.entityData.get(PURPLE_UNIFORM_VARIANT);
    }

    private void setPurpleUniformVariant(boolean value) {
        this.entityData.set(PURPLE_UNIFORM_VARIANT, value);
    }

    public void setFinalSelectionPathingActive(boolean active) {
        this.getPersistentData().putBoolean(FINAL_SELECTION_PATHING_TAG, active);
        this.entityData.set(FINAL_SELECTION_PATHING_ACTIVE, active);
    }

    public boolean isFinalSelectionPathingActive() {
        return this.entityData.get(FINAL_SELECTION_PATHING_ACTIVE)
            || this.getPersistentData().getBoolean(FINAL_SELECTION_PATHING_TAG);
    }

    public void setFinalSelectionPathSpeed(double speed) {
        this.entityData.set(FINAL_SELECTION_PATH_SPEED, (float) Math.max(0.0D, speed));
    }

    public double getFinalSelectionPathSpeed() {
        return this.entityData.get(FINAL_SELECTION_PATH_SPEED);
    }

    // --- Power level override to allow 0 ---

    @Override
    public void setPowerLevel(int level) {
        if (level < 0) level = 0;
        if (level > MAX_POWER_LEVEL) level = MAX_POWER_LEVEL;
        // Write directly to entity data (bypass parent's 1-4 clamping)
        this.entityData.set(POWER_LEVEL, level);
        updateSeniorNameForLevel(level);
    }

    // --- Breathing Technique ---

    @Override
    public BreathingTechnique getBreathingTechnique() {
        if (cachedTechnique != null) {
            return cachedTechnique;
        }

        String swordId = getSwordId();
        if (swordId.isEmpty()) {
            return null;
        }

        // Try SwordRegistry first (this mod + addons)
        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordId);
        if (registeredSword != null) {
            BreathingStyleRegistry.RegisteredBreathingStyle style =
                BreathingStyleRegistry.getStyle(registeredSword.getStyleId());
            if (style != null) {
                cachedTechnique = style.getTechnique();
                return cachedTechnique;
            }
        }

        // Try SwordMetadataRegistry (base mod swords)
        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(swordId);
        if (metadata != null) {
            BreathingStyleRegistry.RegisteredBreathingStyle style =
                BreathingStyleRegistry.getStyle(metadata.getStyleId());
            if (style != null) {
                cachedTechnique = style.getTechnique();
                return cachedTechnique;
            }
        }

        return null;
    }

    @Override
    public ItemStack getEquippedSword() {
        return getSwordStackById(getSwordId());
    }

    public ItemStack getSwordStackById(String swordId) {
        if (swordId.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Try SwordRegistry first (returns BreathingSwordItem directly)
        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordId);
        if (registeredSword != null) {
            return new ItemStack(registeredSword.getSwordItem());
        }

        // Try SwordMetadataRegistry (base mod swords - lazy resolution)
        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(swordId);
        if (metadata != null && metadata.getSwordItem() != null) {
            ItemStack stack = new ItemStack(metadata.getSwordItem());
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        // Try direct registry lookup as fallback
        ResourceLocation loc = ResourceLocation.tryParse(swordId);
        if (loc != null) {
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item != null) {
                return new ItemStack(item);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        if (getPowerLevel() == 0) {
        	// Random chance for leather armor for power level 0
            if (Math.random() < 0.1)
            {
                return new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS)};
            }
            if (Math.random() < 0.15)
            {
                return new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.LEATHER_CHESTPLATE), ItemStack.EMPTY, new ItemStack(Items.LEATHER_BOOTS)};
            }
            if (Math.random() < 0.2)
            {
                return new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.LEATHER_CHESTPLATE), ItemStack.EMPTY, ItemStack.EMPTY};
            }
            // Otherwise, no armor for power level 0
            return new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        }

        // Standard demon slayer uniform from base mod
        Item uniformChest = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_chestplate"));
        Item uniformLegs = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_leggings"));
        Item uniformBoots = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        // Male slayers with texture index 0/1 (mob_slayer_1 or mob_slayer_2)
        // wear slayer_uniform_2 set when power level is 1+.
        if (!isFemale() && (getTextureIndex() == 0 || getTextureIndex() == 1)) {
            boolean usePurpleUniform2 = isPurpleUniformVariant();
            return new ItemStack[]{
                ItemStack.EMPTY, // No helmet
                new ItemStack(usePurpleUniform2 ? ModItems.SLAYER_UNIFORM_2_CHESTPLATE_PURPLE.get() : ModItems.SLAYER_UNIFORM_2_CHESTPLATE.get()),
                new ItemStack(usePurpleUniform2 ? ModItems.SLAYER_UNIFORM_2_LEGGINGS_PURPLE.get() : ModItems.SLAYER_UNIFORM_2_LEGGINGS.get()),
                new ItemStack(usePurpleUniform2 ? ModItems.SLAYER_UNIFORM_2_BOOTS_PURPLE.get() : ModItems.SLAYER_UNIFORM_2_BOOTS.get())
            };
        }

        boolean usePurpleUniform = isPurpleUniformVariant();
        ItemStack chestStack = usePurpleUniform
            ? new ItemStack(ModItems.PURPLE_DEMON_SLAYER_UNIFORM_CHESTPLATE.get())
            : (uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY);
        ItemStack bootsStack = usePurpleUniform
            ? new ItemStack(ModItems.PURPLE_DEMON_SLAYER_UNIFORM_BOOTS.get())
            : (uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY);

        // Female slayers with texture index 0 (slayer_female_1), 3 (slayer_female_4), or 5 (slayer_female_6), or 8 (slayer_female_9)
        // wear andon_bakama instead of uniform leggings.
        ItemStack legsStack;
        if (isFemale() && (getTextureIndex() == 0 || getTextureIndex() == 3 || getTextureIndex() == 5 || getTextureIndex() == 8)) {
            legsStack = usePurpleUniform
                ? new ItemStack(ModItems.PURPLE_ANDON_BAKAMA.get())
                : new ItemStack(ModItems.ANDON_BAKAMA.get());
        } else {
            legsStack = usePurpleUniform
                ? new ItemStack(ModItems.PURPLE_DEMON_SLAYER_UNIFORM_LEGGINGS.get())
                : (uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY);
        }

        return new ItemStack[]{
            ItemStack.EMPTY, // No helmet
            chestStack,
            legsStack,
            bootsStack
        };
    }

    // --- Attributes ---

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)       // Default for level 0; overridden in finalizeSpawn
            .add(Attributes.ATTACK_DAMAGE, 5.5D)
            .add(Attributes.MOVEMENT_SPEED, 0.2D)
            .add(Attributes.ARMOR, 0.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D)
            .add(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), 3.0D); // Default; increased for whip swords
    }

    // --- Spawning ---

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag dataTag) {
        // Don't call super.finalizeSpawn() — we handle everything ourselves
        // (parent would assign power 1-4 and call getEquippedSword before sword is chosen)

        // 1. Choose random style first, then a level-0 sword from that style.
        // Retry across styles if a sword id resolves to an empty ItemStack.
        assignValidSwordWithRetries();

        // 2. Choose texture variant
        int maxTextures = 6;
        setTextureIndex(this.random.nextInt(maxTextures));

        // 3. Assign random power level 0-5
        int powerLevel = this.random.nextInt(MAX_POWER_LEVEL + 1); // 0-5
        // Keep equipment set deterministic per-entity and avoid mixing regular/purple pieces.
        setPurpleUniformVariant(this.random.nextFloat() < 0.25F);
        configurePowerLevelLoadout(powerLevel);

        // 4. Apply 2x attack speed
        applyAttackSpeedBonus();

        Log.debug("[DemonSlayer] Spawned " + (isFemale() ? "female" : "male") +
                  " demon slayer, power=" + powerLevel +
                  ", sword=" + getSwordId() +
                  ", texture=" + getTextureIndex());

        return spawnData;
    }

    /**
     * Collect all eligible swords (level 0, obtainable) from both registries.
     */
    private List<EligibleSword> collectEligibleSwordsForStyle(String styleId) {
        List<EligibleSword> eligible = new ArrayList<>();
        Set<String> seenItemIds = new LinkedHashSet<>();

        // From SwordRegistry (this mod + addons)
        for (SwordRegistry.RegisteredSword sword : SwordRegistry.getAllSwords()) {
            if (sword.getSwordLevel() == 0 && sword.isObtainableInSurvival() && styleId.equals(sword.getStyleId())) {
                Item swordItem = sword.getSwordItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(swordItem);
                if (!isUsableSwordItem(swordItem) || itemId == null) {
                    continue;
                }
                String itemIdStr = itemId.toString();
                if (seenItemIds.add(itemIdStr)) {
                    if (sword.getCategory() == SwordRegistry.SwordCategory.NICHIRIN
                        && isAllowedForRandomSpawn(sword.getSwordId(), itemIdStr)) {
                        eligible.add(new EligibleSword(sword.getSwordId(), sword.getStyleId(), swordItem));
                    }
                }
            }
        }

        // From SwordMetadataRegistry (base mod swords)
        for (SwordMetadataRegistry.SwordMetadata meta : SwordMetadataRegistry.getAllSwords()) {
            if (meta.getSwordLevel() == 0 && meta.isObtainableInSurvival() && styleId.equals(meta.getStyleId())) {
                Item item = meta.getSwordItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (isUsableSwordItem(item) && itemId != null) {
                    String itemIdStr = itemId.toString();
                    if (!seenItemIds.add(itemIdStr)) {
                        continue;
                    }
                    if (isAllowedForRandomSpawn(meta.getSwordId(), itemIdStr)) {
                        eligible.add(new EligibleSword(meta.getSwordId(), meta.getStyleId(), item));
                    }
                }
            }
        }

        return eligible;
    }

    private boolean isUsableSwordItem(Item item) {
        return item != null && !new ItemStack(item).isEmpty();
    }

    private boolean isAllowedForRandomSpawn(String swordId, String itemIdStr) {
        String swordKey = swordId == null ? "" : swordId.toLowerCase();
        String itemKey = itemIdStr == null ? "" : itemIdStr.toLowerCase();
        for (String banned : DISALLOWED_RANDOM_SWORD_PATH_KEYS) {
            if (swordKey.contains(banned) || itemKey.contains(banned)) {
                return false;
            }
        }
        return true;
    }

    private EligibleSword chooseRandomLevelZeroSwordByStyle() {
        List<String> stylesToTry = collectSpawnableStyleIds();
        // Fisher-Yates shuffle using Minecraft RandomSource
        for (int i = stylesToTry.size() - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            String tmp = stylesToTry.get(i);
            stylesToTry.set(i, stylesToTry.get(j));
            stylesToTry.set(j, tmp);
        }

        for (String styleId : stylesToTry) {
            List<EligibleSword> swords = collectEligibleSwordsForStyle(styleId);
            if (!swords.isEmpty()) {
                return swords.get(this.random.nextInt(swords.size()));
            }
        }

        return null;
    }

    private void assignValidSwordWithRetries() {
        String previousSword = getSwordId();
        for (int attempt = 0; attempt < MAX_SWORD_ASSIGN_RETRIES; attempt++) {
            EligibleSword chosen = chooseRandomLevelZeroSwordByStyle();
            if (chosen == null) {
                break;
            }

            setSwordId(chosen.swordId());
            ItemStack resolved = getSwordStackById(chosen.swordId());
            if (!resolved.isEmpty()) {
                Log.debug("[DemonSlayer] Chose sword: {} (style: {})", chosen.swordId(), chosen.styleId());
                return;
            }

            Log.warn("[DemonSlayer] Failed to resolve sword for style {} (swordId={}), retrying with a different style",
                chosen.styleId(), chosen.swordId());
        }

        // Restore previous value if retries failed, then clear to avoid stale/invalid sword id usage.
        setSwordId(previousSword);
        if (getSwordStackById(getSwordId()).isEmpty()) {
            setSwordId("");
        }

        Log.warn("[DemonSlayer] Could not assign a valid level-0 sword after {} retries; spawning without sword",
            MAX_SWORD_ASSIGN_RETRIES);
    }

    private List<String> collectSpawnableStyleIds() {
        Set<String> ids = new LinkedHashSet<>();

        for (BreathingStyleRegistry.RegisteredBreathingStyle style : BreathingStyleRegistry.getAllStyles()) {
            ids.add(style.getStyleId());
        }
        for (StyleMetadataRegistry.StyleMetadata style : StyleMetadataRegistry.getAllStyles()) {
            ids.add(style.getStyleId());
        }
        for (SwordRegistry.RegisteredSword sword : SwordRegistry.getAllSwords()) {
            if (sword.getSwordLevel() == 0 && sword.isObtainableInSurvival()) {
                ids.add(sword.getStyleId());
            }
        }
        for (SwordMetadataRegistry.SwordMetadata sword : SwordMetadataRegistry.getAllSwords()) {
            if (sword.getSwordLevel() == 0 && sword.isObtainableInSurvival()) {
                ids.add(sword.getStyleId());
            }
        }

        return new ArrayList<>(ids);
    }

    /**
     * Apply stat bonuses based on power level (0-4).
     * Level 0: 20 HP, Speed I
     * Level 1-4: Same as parent BreathingSlayerEntity
     */
    public void applyDemonSlayerPowerBonuses(int powerLevel) {
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double health = switch (powerLevel) {
                case 0 -> 20.0;
                case 1 -> 40.0;
                case 2 -> 60.0;
                case 3 -> 70.0;
                case 4 -> 80.0;
                case 5 -> 95.0;
                default -> 20.0;
            };
            if (isDemonized()) {
                health *= getDemonizedHealthMultiplier();
            }
            maxHealth.setBaseValue(health);
            this.setHealth((float) health);
        }

        // Speed effect
        int speedLevel = switch (powerLevel) {
            case 0 -> -1;  // No Speed
            case 1 -> 0;  // Speed I
            case 2 -> 1;  // Speed II
            case 3 -> 3;  // Speed IV
            case 4 -> 1;  // Speed II
            case 5 -> 1;  // Speed II
            default -> 0;
        };
        if (speedLevel >= 0) // (level 1+)
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, speedLevel, true, false));

        // Resistance (level 1+)
        if (powerLevel >= 2) {
            int resistanceLevel = powerLevel - 2;
            if (resistanceLevel > 4) resistanceLevel = 4;
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, resistanceLevel, true, false));
        }

        // Strength (level 1+)
        if (powerLevel >= 1) {
            int strengthLevel = ((powerLevel - 1) * 2) + (isDemonized() ? 1 : 0);
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, strengthLevel, true, false));
        } else if (isDemonized()) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, true, false));
        }
    }

    public void configurePowerLevelLoadout(int powerLevel) {
        setPowerLevel(powerLevel);
        applyDemonSlayerPowerBonuses(powerLevel);
        applyAttackSpeedBonus();

        if (powerLevel >= 1 && powerLevel <= 4) {
            if (!this.sheathPreferenceInitialized) {
                setSheatheOnBack(this.random.nextFloat() < 0.20F);
            }
        } else {
            setSheatheOnBack(false);
        }

        if (powerLevel >= 5) {
            boolean hasExistingPool = hasValidSuperSeniorSwordPool();
            if (!hasExistingPool) {
                initializeSuperSeniorSwordSet();
            } else if (this.superSeniorSwordSwitchTicks <= 0) {
                resetSuperSeniorSwitchTimer();
            }
        } else {
            setAltSwordId1("");
            setAltSwordId2("");
        }

        if (getSwordId().isEmpty() || getSwordStackById(getSwordId()).isEmpty()) {
            assignValidSwordWithRetries();
        }

        ItemStack swordStack = getEquippedSword();
        if (powerLevel == 0 && !swordStack.isEmpty()) {
            // Level-0 slayers always use training sword behavior.
            TrainingSwordHelper.makeTrainingSword(swordStack);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, swordStack);
        updateEntityReachForSword(swordStack);

        ItemStack[] armor = getArmorEquipment();
        this.setItemSlot(EquipmentSlot.HEAD, armor[0]);
        this.setItemSlot(EquipmentSlot.CHEST, armor[1]);
        this.setItemSlot(EquipmentSlot.LEGS, armor[2]);
        this.setItemSlot(EquipmentSlot.FEET, armor[3]);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.2F);
        }

        updateSeniorNameForLevel(powerLevel);
    }

    private void initializeSuperSeniorSwordSet() {
        java.util.Map<String, List<EligibleSword>> swordsByStyle = new java.util.HashMap<>();
        for (String styleId : collectSpawnableStyleIds()) {
            List<EligibleSword> swords = collectEligibleSwordsForStyle(styleId);
            if (swords.isEmpty()) {
                continue;
            }
            List<EligibleSword> valid = new ArrayList<>();
            for (EligibleSword sword : swords) {
                if (!getSwordStackById(sword.swordId()).isEmpty()) {
                    valid.add(sword);
                }
            }
            if (!valid.isEmpty()) {
                swordsByStyle.put(styleId, valid);
            }
        }

        List<String> uniqueStyles = new ArrayList<>(swordsByStyle.keySet());
        for (int i = uniqueStyles.size() - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            String tmp = uniqueStyles.get(i);
            uniqueStyles.set(i, uniqueStyles.get(j));
            uniqueStyles.set(j, tmp);
        }

        List<EligibleSword> selected = new ArrayList<>();
        int picks = Math.min(3, uniqueStyles.size());
        for (int i = 0; i < picks; i++) {
            String styleId = uniqueStyles.get(i);
            List<EligibleSword> options = swordsByStyle.get(styleId);
            selected.add(options.get(this.random.nextInt(options.size())));
        }

        if (selected.isEmpty()) {
            assignValidSwordWithRetries();
            setAltSwordId1("");
            setAltSwordId2("");
            return;
        }

        setSwordId(selected.get(0).swordId());
        setAltSwordId1(selected.size() > 1 ? selected.get(1).swordId() : "");
        setAltSwordId2(selected.size() > 2 ? selected.get(2).swordId() : "");

        if (selected.size() < 3) {
            Log.warn("[DemonSlayer] Level-5 super senior could not find 3 distinct styles (found {})", selected.size());
        }

        resetSuperSeniorSwitchTimer();
    }

    private boolean hasValidSuperSeniorSwordPool() {
        if (getPowerLevel() < 5) {
            return false;
        }
        String primary = getSwordId();
        String alt1 = getAltSwordId1();
        String alt2 = getAltSwordId2();
        if (primary.isEmpty() || alt1.isEmpty() || alt2.isEmpty()) {
            return false;
        }
        Set<String> unique = new LinkedHashSet<>();
        unique.add(primary);
        unique.add(alt1);
        unique.add(alt2);
        if (unique.size() < 3) {
            return false;
        }
        for (String swordId : unique) {
            if (getSwordStackById(swordId).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void resetSuperSeniorSwitchTimer() {
        this.superSeniorSwordSwitchTicks = SUPER_SENIOR_SWITCH_MIN_TICKS
            + this.random.nextInt(SUPER_SENIOR_SWITCH_MAX_TICKS - SUPER_SENIOR_SWITCH_MIN_TICKS + 1);
    }

    private void tickSuperSeniorSwordSwitching() {
        if (this.getPowerLevel() < 5 || isActionLocked()) {
            return;
        }
        if (this.superSeniorSwordSwitchTicks > 0) {
            this.superSeniorSwordSwitchTicks--;
            return;
        }

        List<String> options = new ArrayList<>();
        if (!getSwordId().isEmpty()) options.add(getSwordId());
        if (!getAltSwordId1().isEmpty() && !options.contains(getAltSwordId1())) options.add(getAltSwordId1());
        if (!getAltSwordId2().isEmpty() && !options.contains(getAltSwordId2())) options.add(getAltSwordId2());

        if (options.size() <= 1) {
            resetSuperSeniorSwitchTimer();
            return;
        }

        String current = getSwordId();
        String next = current;
        for (int tries = 0; tries < 6 && next.equals(current); tries++) {
            next = options.get(this.random.nextInt(options.size()));
        }

        ItemStack nextSword = getSwordStackById(next);
        if (!nextSword.isEmpty() && !next.equals(current)) {
            setSwordId(next);
            this.setItemSlot(EquipmentSlot.MAINHAND, nextSword);
            updateEntityReachForSword(nextSword);
            Log.debug("[DemonSlayer] Super senior switched sword to {}", next);
        }

        resetSuperSeniorSwitchTimer();
    }

    /**
     * Update ENTITY_REACH attribute based on equipped sword type.
     * Kanroji sword = 9 blocks, Love sword = 7.5 blocks, others = default (3).
     */
    private void updateEntityReachForSword(ItemStack swordStack) {
        AttributeInstance reachAttr = this.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (reachAttr == null) return;

        if (com.lerdorf.kimetsunoyaibamultiplayer.combat.KanrojiSwordAttackHandler.isWhipSword(swordStack)) {
            double reach = com.lerdorf.kimetsunoyaibamultiplayer.combat.KanrojiSwordAttackHandler.getEntityReachForSword(swordStack);
            reachAttr.setBaseValue(reach);
            Log.debug("[DemonSlayer] Set ENTITY_REACH to {} for whip sword", reach);
        } else {
            reachAttr.setBaseValue(3.0D);
        }
    }

    /**
     * Apply power-level-based attack speed bonus.
     */
    public void applyAttackSpeedBonus() {
        AttributeInstance attackSpeed = this.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(DEMON_SLAYER_ATTACK_SPEED_UUID);
            double bonusMultiplier = switch (getPowerLevel()) {
                case 0 -> 0.0D;
                case 1 -> 0.20D;
                case 2 -> 0.40D;
                case 3 -> 0.65D;
                case 4 -> 1.00D;
                case 5 -> 1.25D;
                default -> 0.0D;
            };
            attackSpeed.addPermanentModifier(new AttributeModifier(
                DEMON_SLAYER_ATTACK_SPEED_UUID,
                "Demon slayer attack speed",
                bonusMultiplier,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    // --- NBT Save/Load ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SwordId", getSwordId());
        tag.putInt("TextureIndex", getTextureIndex());
        tag.putBoolean("SheatheOnBack", isSheatheOnBack());
        tag.putInt("KickCooldownTicks", this.kickCooldownTicks);
        tag.putInt("FallRecoveryTicks", this.fallRecoveryTicks);
        tag.putBoolean("HeavyLandingPending", this.heavyLandingPending);
        tag.putString("AltSwordId1", getAltSwordId1());
        tag.putString("AltSwordId2", getAltSwordId2());
        tag.putInt("SuperSeniorSwordSwitchTicks", this.superSeniorSwordSwitchTicks);
        tag.putInt("FleeRepathTicks", this.fleeRepathTicks);
        tag.putBoolean("PurpleUniformVariant", isPurpleUniformVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("SwordId")) {
            setSwordId(tag.getString("SwordId"));
        }
        if (tag.contains("TextureIndex")) {
            setTextureIndex(tag.getInt("TextureIndex"));
        }
        if (tag.contains("SheatheOnBack")) {
            setSheatheOnBack(tag.getBoolean("SheatheOnBack"));
        }
        if (tag.contains("KickCooldownTicks")) {
            this.kickCooldownTicks = Math.max(0, tag.getInt("KickCooldownTicks"));
        }
        if (tag.contains("FallRecoveryTicks")) {
            this.fallRecoveryTicks = Math.max(0, tag.getInt("FallRecoveryTicks"));
        }
        if (tag.contains("HeavyLandingPending")) {
            this.heavyLandingPending = tag.getBoolean("HeavyLandingPending");
        }
        if (tag.contains("AltSwordId1")) {
            setAltSwordId1(tag.getString("AltSwordId1"));
        }
        if (tag.contains("AltSwordId2")) {
            setAltSwordId2(tag.getString("AltSwordId2"));
        }
        if (tag.contains("SuperSeniorSwordSwitchTicks")) {
            this.superSeniorSwordSwitchTicks = Math.max(0, tag.getInt("SuperSeniorSwordSwitchTicks"));
        }
        if (tag.contains("FleeRepathTicks")) {
            this.fleeRepathTicks = Math.max(0, tag.getInt("FleeRepathTicks"));
        }
        if (tag.contains("PurpleUniformVariant")) {
            setPurpleUniformVariant(tag.getBoolean("PurpleUniformVariant"));
        }

        // Re-apply power level bonuses on load
        int powerLevel = getPowerLevel();
        configurePowerLevelLoadout(powerLevel);
        applyAttackSpeedBonus();
        updateSeniorNameForLevel(powerLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.isDisarmed()) {
                this.setSprinting(false);
                this.setLowLevelFleeing(false);
                this.fleeRepathTicks = 0;
                this.setTarget(null);
                this.setLastHurtByMob(null);
                return;
            }
            if (this.isLeashed()) {
                this.dropLeash(true, false);
            }
            if (this.kickCooldownTicks > 0) {
                this.kickCooldownTicks--;
            }
            if (this.getPowerLevel() >= 1 && this.fallDistance > 4.0F && !this.onGround()) {
                this.heavyLandingPending = true;
            }
            if (this.onGround() && this.heavyLandingPending) {
                this.heavyLandingPending = false;
                startFallRecovery();
            }
            if (this.fallRecoveryTicks > 0) {
                this.fallRecoveryTicks--;
                this.setSprinting(false);
                this.getNavigation().stop();
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                return;
            }
            LivingEntity target = this.getTarget();
            boolean shouldFlee = shouldLowLevelFlee(target);
            this.setLowLevelFleeing(shouldFlee);
            if (shouldFlee) {
                this.setSprinting(true);
                tickLowLevelFlee(target);
            } else {
                this.setSprinting(target != null && target.isAlive());
                this.fleeRepathTicks = 0;
            }
            tryKickAttack(target);
            tickSuperSeniorSwordSwitching();
        }
    }

    @Override
    public float getSpeed() {
        float baseSpeed = super.getSpeed();
        if (isActionLocked()) {
            return baseSpeed * 0.05F;
        }
        if (this.isSprinting() && this.getTarget() != null) {
            return baseSpeed * 1.4F;
        }
        return baseSpeed;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (isActionLocked() || isLowLevelFleeing() || isDisarmed()) {
            return false;
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        float appliedMultiplier = damageMultiplier;
        if (getPowerLevel() >= 1 && fallDistance > 4.0F) {
            appliedMultiplier = damageMultiplier * 0.2F;
        }
        return super.causeFallDamage(fallDistance, appliedMultiplier, source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);

        if (!recentlyHit || DICE_STEAK_DROPS.length == 0) {
            return;
        }

        double dropChance = Math.min(0.90D, 0.12D + (0.06D * Math.max(0, lootingLevel)));
        if (this.random.nextDouble() >= dropChance) {
            return;
        }

        String selectedDropId = DICE_STEAK_DROPS[this.random.nextInt(DICE_STEAK_DROPS.length)];
        Item dropItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", selectedDropId));
        if (dropItem != null && dropItem != Items.AIR) {
            ItemStack stack = new ItemStack(dropItem);
            SlayerFleshHelper.applyDemonSlayerMetadata(stack, this);
            this.spawnAtLocation(stack);
        }
    }

    @Override
    public int getExperienceReward() {
        return Math.max(0, getPowerLevel()) * 2;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            this.lastDamageTick = this.tickCount;
        }
        return result;
    }

    public boolean wasRecentlyDamaged(int withinTicks) {
        return this.tickCount - this.lastDamageTick <= withinTicks;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }

            String anim = getCurrentAnimation();
            int animTicks = getAnimationTicks();

            if (animTicks > 0 && !isBaseMovementAnimation(anim)) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            // Draw/sheath transitions should always win over movement locomotion.
            if (anim != null && (anim.startsWith("draw_") || anim.startsWith("sheath_"))) {
                state.getController().setAnimationSpeed(1.0D);
                return state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }

            boolean hasActivePath = !this.getNavigation().isDone();
            double travelSpeed = getHorizontalTravelSpeed();
            double velocitySq = this.getDeltaMovement().horizontalDistanceSqr();
            this.smoothedHorizontalSpeed = (this.smoothedHorizontalSpeed * 0.65D) + (travelSpeed * 0.35D);
            double speedNow = this.smoothedHorizontalSpeed;

            // Movement hysteresis to avoid idle/walk flicker at low speeds.
            boolean movementEnter = speedNow > 0.014D
                || velocitySq > 0.00002D
                || hasActivePath
                || state.isMoving();
            boolean movementExit = speedNow < 0.0025D
                && velocitySq < 0.000002D
                && !hasActivePath;

            if (movementEnter) {
                this.movementAnimationLatched = true;
                this.movementAnimationGraceTicks = 28;
            } else if (this.movementAnimationGraceTicks > 0) {
                this.movementAnimationGraceTicks--;
            } else if (movementExit) {
                this.movementAnimationLatched = false;
            }

            boolean isMoving = this.movementAnimationLatched || this.movementAnimationGraceTicks > 0;
            // Hard guard: if the entity is physically moving or still has a path, never show idle.
            if (!isMoving && (hasActivePath || speedNow > 0.003D || velocitySq > 0.00001D)) {
                isMoving = true;
                this.movementAnimationLatched = true;
                this.movementAnimationGraceTicks = Math.max(this.movementAnimationGraceTicks, 16);
            }
            boolean seniorSwordDrawnInCombat = isSeniorSwordDrawnInCombat();

            String desiredMovementAnim;
            double desiredAnimSpeed;

            if (!isMoving) {
                if (this.movementStateHoldTicks > 0) {
                    this.movementStateHoldTicks--;
                    desiredMovementAnim = resolveWalkAnimation(seniorSwordDrawnInCombat);
                    double walkFactor = speedNow / 0.085D;
                    desiredAnimSpeed = clamp(walkFactor, 1.05D, 2.1D);
                } else {
                desiredMovementAnim = resolveIdleAnimation(seniorSwordDrawnInCombat);
                desiredAnimSpeed = 1.0D;
                }
            } else {
                // Keep walk active briefly to absorb tiny movement/path jitters.
                this.movementStateHoldTicks = 28;

                // Sprint selection is speed-driven with strong hysteresis + hold lock.
                boolean currentlySprintingAnim = isSprintAnimation(this.latchedMovementAnimation);
                double sprintEnterSpeed = 0.155D;
                double sprintExitSpeed = 0.105D;
                boolean speedWantsSprint = speedNow >= (currentlySprintingAnim ? sprintExitSpeed : sprintEnterSpeed);
                boolean mountainPathSprint = this.isFinalSelectionPathingActive()
                    && this.getFinalSelectionPathSpeed() > 2.3D;
                boolean mountainPathSpeedSprint = this.isFinalSelectionPathingActive()
                    && hasActivePath
                    && (this.getFinalSelectionPathSpeed() > 1.8D || speedNow > 0.045D);
                boolean wantsSprint = this.isLowLevelFleeing()
                    || mountainPathSprint
                    || mountainPathSpeedSprint
                    || this.isSprinting()
                    || speedWantsSprint;

                if (wantsSprint) {
                    this.sprintAnimationGraceTicks = 20;
                    this.sprintStateHoldTicks = this.isFinalSelectionPathingActive() ? 45 : 28;
                } else if (this.sprintAnimationGraceTicks > 0) {
                    this.sprintAnimationGraceTicks--;
                }
                if (this.sprintStateHoldTicks > 0) {
                    this.sprintStateHoldTicks--;
                }

                boolean shouldSprint = this.sprintAnimationGraceTicks > 0
                    || this.sprintStateHoldTicks > 0
                    || (mountainPathSprint && speedNow > 0.04D);
                if (shouldSprint) {
                    desiredMovementAnim = resolveSprintAnimation(seniorSwordDrawnInCombat);
                    double sprintFactor = speedNow / 0.11D;
                    desiredAnimSpeed = clamp(sprintFactor, 1.0D, 2.8D);
                } else {
                    desiredMovementAnim = resolveWalkAnimation(seniorSwordDrawnInCombat);
                    double walkFactor = speedNow / 0.085D;
                    desiredAnimSpeed = clamp(walkFactor, 1.05D, 2.2D);
                }
            }

            if (!desiredMovementAnim.equals(this.latchedMovementAnimation)) {
                boolean classChanged = movementAnimClass(desiredMovementAnim) != movementAnimClass(this.latchedMovementAnimation);
                if (this.movementAnimationSwitchCooldownTicks <= 0 || classChanged) {
                    this.latchedMovementAnimation = desiredMovementAnim;
                    this.movementAnimationSwitchCooldownTicks = 6;
                } else {
                    this.movementAnimationSwitchCooldownTicks--;
                }
            } else if (this.movementAnimationSwitchCooldownTicks > 0) {
                this.movementAnimationSwitchCooldownTicks--;
            }

            this.smoothedAnimationSpeed = (this.smoothedAnimationSpeed * 0.70D) + (desiredAnimSpeed * 0.30D);
            state.getController().setAnimationSpeed(clamp(this.smoothedAnimationSpeed, 0.9D, 2.8D));
            String currentLoop = null;
            if (state.getController().getCurrentAnimation() != null) {
                currentLoop = state.getController().getCurrentAnimation().animation().name();
            }
            if (this.latchedMovementAnimation.equals(currentLoop)) {
                return PlayState.CONTINUE;
            }
            RawAnimation loop = LOOPING_MOVEMENT_ANIMATIONS.computeIfAbsent(this.latchedMovementAnimation,
                key -> RawAnimation.begin().thenLoop(key));
            return state.setAndContinue(loop);
        }));
    }

    private double getHorizontalTravelSpeed() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isSprintAnimation(String animation) {
        return "sprint".equals(animation)
            || "sprint_noob".equals(animation)
            || "sprint_senior".equals(animation);
    }

    private static int movementAnimClass(String animation) {
        if (animation == null || animation.isEmpty() || animation.startsWith("idle")) {
            return 0;
        }
        if (animation.startsWith("walk")) {
            return 1;
        }
        if (isSprintAnimation(animation)) {
            return 2;
        }
        return 3;
    }

    private boolean isBaseMovementAnimation(String animation) {
        return "idle".equals(animation)
            || "idle_senior".equals(animation)
            || "walk".equals(animation)
            || "walk_female".equals(animation)
            || "walk_senior".equals(animation)
            || "sprint".equals(animation)
            || "sprint_noob".equals(animation)
            || "sprint_senior".equals(animation);
    }

    private String resolveIdleAnimation(boolean seniorSwordDrawnInCombat) {
        if (seniorSwordDrawnInCombat) {
            return "idle_senior";
        }
        return "idle";
    }

    private String resolveWalkAnimation(boolean seniorSwordDrawnInCombat) {
        if (seniorSwordDrawnInCombat) {
            return "walk_senior";
        }
        return isFemale() ? "walk_female" : "walk";
    }

    private String resolveSprintAnimation(boolean seniorSwordDrawnInCombat) {
        if (isLowLevelFleeing() && getPowerLevel() <= 1) {
            return "sprint_noob";
        }
        if (seniorSwordDrawnInCombat) {
            return "sprint_senior";
        }
        if (getPowerLevel() == 0) {
            return "sprint_noob";
        }
        return "sprint";
    }

    private boolean isSeniorSwordDrawnInCombat() {
        if (getPowerLevel() < 4) {
            return false;
        }
        if (isActionLocked()) {
            return false;
        }
        if (!EntityCombatStateTracker.isInCombat(this)) {
            return false;
        }
        ItemStack mainHand = this.getMainHandItem();
        return !mainHand.isEmpty() && SwordParticleMapping.isKimetsunoyaibaSword(mainHand);
    }

    private void updateSeniorNameForLevel(int level) {
        if (level >= 5) {
            Component baseName = this.getType().getDescription();
            this.setCustomName(baseName.copy().append(Component.literal(" Super Senior")));
            this.setCustomNameVisible(false);
        } else if (level >= 4) {
            Component baseName = this.getType().getDescription();
            this.setCustomName(baseName.copy().append(Component.literal(" Senior")));
            this.setCustomNameVisible(false);
        } else if (this.hasCustomName()) {
            String current = this.getCustomName().getString();
            if (current.endsWith(" Senior") || current.endsWith(" Super Senior")) {
                this.setCustomName(null);
            }
        }
    }

    private void tryKickAttack(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (isDisarmed()) {
            return;
        }
        if (isLowLevelFleeing()) {
            return;
        }
        if (this.getPowerLevel() >= 4 || this.getPowerLevel() < 0) {
            return;
        }
        String currentAnim = getCurrentAnimation();
        if ((this.getAnimationTicks() > 0 && !isBaseMovementAnimation(currentAnim)) || this.kickCooldownTicks > 0) {
            return;
        }
        // Give kicks a bit more practical reach so they can trigger during melee footwork.
        if (this.distanceToSqr(target) > 9.0D) {
            return;
        }
        if (this.random.nextFloat() > 0.06F) {
            return;
        }

        String kickAnim = this.random.nextBoolean() ? "kick_left" : "kick_right";
        this.playGeckoAnimation(kickAnim, 10);
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 1.0F);

        target.hurt(this.damageSources().mobAttack(this), (float) KICK_DAMAGE);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz > 1.0e-4) {
            target.knockback(KICK_KNOCKBACK, dx, dz);
        }

        this.kickCooldownTicks = KICK_COOLDOWN_MIN + this.random.nextInt(KICK_COOLDOWN_MAX - KICK_COOLDOWN_MIN + 1);
    }

    private void startFallRecovery() {
        if (this.getPowerLevel() < 1) {
            return;
        }
        this.fallRecoveryTicks = FALL_RECOVERY_TICKS;
        this.playGeckoAnimation("fall1", FALL_RECOVERY_TICKS);
    }

    private boolean shouldLowLevelFlee(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        int powerLevel = this.getPowerLevel();
        if (powerLevel == 0 && target instanceof Creeper) {
            return true;
        }
        if (powerLevel > 1) {
            return false;
        }
        if (!isActivelyInCombatWith(target)) {
            return false;
        }

        double selfHealth = this.getHealth();
        double targetHealth = target.getHealth();
        if (powerLevel == 0) {
            return selfHealth * 1.5D < targetHealth;
        }
        // Level 1
        return selfHealth * 2.0D < targetHealth;
    }

    private boolean isActivelyInCombatWith(LivingEntity target) {
        // Must be the entity this slayer is currently aggroed on.
        if (this.getTarget() != target) {
            return false;
        }

        // If the opponent is directly aggroed back on this slayer, this is active combat.
        if (target instanceof Mob mobTarget && mobTarget.getTarget() == this) {
            return true;
        }

        // Otherwise require a recent damage exchange to treat it as actual combat.
        boolean slayerWasHitByTarget = this.getLastHurtByMob() == target
            && (this.tickCount - this.getLastHurtByMobTimestamp()) <= 200;
        boolean targetWasHitBySlayer = target.getLastHurtByMob() == this
            && (target.tickCount - target.getLastHurtByMobTimestamp()) <= 200;

        return slayerWasHitByTarget || targetWasHitBySlayer;
    }

    private void tickLowLevelFlee(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.fleeRepathTicks > 0) {
            this.fleeRepathTicks--;
        }

        if (this.fleeRepathTicks > 0 && !this.getNavigation().isDone()) {
            return;
        }

        Vec3 awayPos = LandRandomPos.getPosAway(this, FLEE_AWAY_RANGE, FLEE_AWAY_VERTICAL_RANGE, target.position());
        if (awayPos == null) {
            Vec3 delta = this.position().subtract(target.position());
            if (delta.lengthSqr() < 1.0e-4) {
                delta = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
            }
            Vec3 dir = new Vec3(delta.x, 0.0D, delta.z).normalize();
            awayPos = this.position().add(dir.scale(12.0D));
        }

        this.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, FLEE_SPEED_MULTIPLIER);
        this.fleeRepathTicks = FLEE_REPATH_INTERVAL_TICKS;
    }
}
