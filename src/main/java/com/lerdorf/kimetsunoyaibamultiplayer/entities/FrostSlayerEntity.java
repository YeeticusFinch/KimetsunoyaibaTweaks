package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Demon Slayer - Generic slayer that randomly selects a nichirin sword from the registry
 * Wields any registered NICHIRIN sword (including those added via API)
 * Uses the breathing technique associated with the selected sword
 * Wears kimetsunoyaiba:uniform armor
 */
public class FrostSlayerEntity extends BreathingSlayerEntity {

    // Store the selected sword and technique for this entity
    private BreathingSwordItem selectedSword;
    private BreathingTechnique selectedTechnique;
    private String selectedSwordId; // For NBT persistence

    public FrostSlayerEntity(EntityType<? extends BreathingSlayerEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                       @Nullable CompoundTag dataTag) {
        // Select random nichirin sword before calling super (which equips the sword)
        selectRandomSword();

        // Call super to handle power level and equipment
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    /**
     * Selects a random nichirin sword from the registry and caches its technique.
     */
    private void selectRandomSword() {
        List<SwordRegistry.RegisteredSword> nichirinSwords = SwordRegistry.getNichirinSwords();

        if (nichirinSwords.isEmpty()) {
            // Fallback: no swords registered, shouldn't happen
            System.err.println("[FrostSlayerEntity] No nichirin swords found in registry!");
            return;
        }

        // Select random sword
        int randomIndex = this.random.nextInt(nichirinSwords.size());
        SwordRegistry.RegisteredSword selected = nichirinSwords.get(randomIndex);

        // Cache the sword and technique
        this.selectedSword = selected.getSwordItem();
        this.selectedTechnique = this.selectedSword.getBreathingTechnique();
        this.selectedSwordId = selected.getSwordId();

        if (!this.level().isClientSide) {
            System.out.println("[FrostSlayerEntity] Selected sword: " + this.selectedSwordId +
                " with technique: " + this.selectedTechnique.getName());
        }
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        // If technique hasn't been selected yet (e.g., during construction), select now
        if (selectedTechnique == null) {
            selectRandomSword();
        }
        return selectedTechnique;
    }

    @Override
    public ItemStack getEquippedSword() {
        // If sword hasn't been selected yet, select now
        if (selectedSword == null) {
            selectRandomSword();
        }
        return selectedSword != null ? new ItemStack(selectedSword) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack[] getArmorEquipment() {
        // Load kimetsunoyaiba mod armor
        Item uniformChest = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_chestplate"));
        Item uniformLegs = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_leggings"));
        Item uniformBoots = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("kimetsunoyaiba", "uniform_boots"));

        return new ItemStack[]{
            ItemStack.EMPTY, // No helmet
            uniformChest != null ? new ItemStack(uniformChest) : ItemStack.EMPTY,
            uniformLegs != null ? new ItemStack(uniformLegs) : ItemStack.EMPTY,
            uniformBoots != null ? new ItemStack(uniformBoots) : ItemStack.EMPTY
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (selectedSwordId != null) {
            tag.putString("SelectedSwordId", selectedSwordId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SelectedSwordId")) {
            String swordId = tag.getString("SelectedSwordId");
            // Restore sword and technique from registry
            SwordRegistry.RegisteredSword registered = SwordRegistry.getSword(swordId);
            if (registered != null) {
                this.selectedSword = registered.getSwordItem();
                this.selectedTechnique = this.selectedSword.getBreathingTechnique();
                this.selectedSwordId = swordId;
            } else {
                // Sword no longer exists in registry, select a new one
                selectRandomSword();
            }
        } else {
            // Old save data or first load, select a sword
            selectRandomSword();
        }
    }
}
