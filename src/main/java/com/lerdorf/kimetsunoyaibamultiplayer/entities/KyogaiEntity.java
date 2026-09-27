package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.KyogaiDrumsItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Custom Kyogai entity with the drums permanently attached to its equipment. */
public class KyogaiEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KyogaiEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);

        ItemStack drums = new ItemStack(ModItems.KYOGAI_DRUMS.get());
        this.setItemSlot(EquipmentSlot.MAINHAND, drums);
        // The chest slot is render-only; KyogaiDrumsItem is not wearable armor.
        this.setItemSlot(EquipmentSlot.CHEST, drums.copy());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.ARMOR, 8.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        // Kyogai is a stationary entity. Future drum abilities can provide his attacks.
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        ensureDrumsEquipped();
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
    }

    private void ensureDrumsEquipped() {
        if (!(this.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof KyogaiDrumsItem)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KYOGAI_DRUMS.get()));
        }
        if (!(this.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof KyogaiDrumsItem)) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.KYOGAI_DRUMS.get()));
        }
    }

    /** Prevent natural and chunk-generation spawning; commands and spawn eggs remain valid. */
    @Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class SpawnHandler {
        @SubscribeEvent
        public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
            if (event.getEntity().getType() != ModEntities.KYOGAI.get()) {
                return;
            }
            MobSpawnType spawnType = event.getSpawnType();
            if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
