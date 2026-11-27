package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.client.renderer.ArachnidMandiblesRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** Animated, unbreakable helmet for the arachnid mandibles attack. */
public class ArachnidMandiblesItem extends ArmorItem implements GeoItem {
    public static final String ANIMATION_TAG = "ArachnidMandiblesAnimation";
    public static final String ANIMATION_UNTIL_TAG = "ArachnidMandiblesAnimationUntil";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArachnidMandiblesItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static boolean isEquipped(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArachnidMandiblesItem;
    }

    public static void triggerBite(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ANIMATION_TAG, "bite");
        tag.putLong(ANIMATION_UNTIL_TAG, gameTime + 15L);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!(entity instanceof LivingEntity living) || !isEquipped(living)) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getLong(ANIMATION_UNTIL_TAG) > level.getGameTime()) {
            return;
        }

        String movementAnimation = living.isSprinting() ? "sprint" : "idle";
        if (!movementAnimation.equals(tag.getString(ANIMATION_TAG))) {
            tag.putString(ANIMATION_TAG, movementAnimation);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "arachnid_mandibles_controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            CompoundTag tag = stack == null ? null : stack.getTag();
            String animation = tag == null ? "idle" : tag.getString(ANIMATION_TAG);
            if (!animation.equals("bite") && !animation.equals("sprint")) {
                animation = "idle";
            }

            AnimationController<?> controller = state.getController();
            if (controller.getCurrentAnimation() != null
                && animation.equals(controller.getCurrentAnimation().animation().name())) {
                return PlayState.CONTINUE;
            }

            return state.setAndContinue(animation.equals("bite")
                ? RawAnimation.begin().thenPlay("bite").thenLoop("idle")
                : RawAnimation.begin().thenLoop(animation));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ArachnidMandiblesRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                           EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = new ArachnidMandiblesRenderer();
                }
                renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return renderer;
            }
        });
    }
}
