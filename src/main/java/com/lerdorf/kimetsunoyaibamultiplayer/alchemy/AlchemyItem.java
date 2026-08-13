package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.PetriDishBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class AlchemyItem extends Item {
    private final boolean specialPresentation;
    private final int tintColor;

    public AlchemyItem(Properties properties, boolean specialPresentation, int tintColor) {
        super(properties);
        this.specialPresentation = specialPresentation;
        this.tintColor = tintColor;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return specialPresentation || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (!specialPresentation) {
            return name;
        }
        return name.copy().withStyle(ChatFormatting.BOLD);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isDrinkable(stack)) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!isDrinkable(stack)) {
            return super.finishUsingItem(stack, level, entity);
        }

        if (!level.isClientSide) {
            applyDrinkEffect(stack, entity);
        }

        ItemStack returnStack = BloodDemonArtAlchemyCatalog.containerReturn(stack);
        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!returnStack.isEmpty() && !player.getAbilities().instabuild) {
                if (stack.isEmpty()) {
                    return returnStack;
                }
                if (!player.getInventory().add(returnStack)) {
                    player.drop(returnStack, false);
                }
            }
            return stack;
        }

        stack.shrink(1);
        return stack.isEmpty() ? returnStack : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return isDrinkable(stack) ? 32 : super.getUseDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isDrinkable(stack) ? UseAnim.DRINK : super.getUseAnimation(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        InteractionResult medicineResult = AlchemyMedicineHandler.applyTargetedMedicine(stack, player, target, hand);
        if (medicineResult != InteractionResult.PASS) {
            return medicineResult;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!BloodDemonArtAlchemyCatalog.isPetriDishDisplayItem(stack)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (level.getBlockState(clickedPos).is(ModAlchemyBlocks.PETRI_DISH.get())) {
            return addToPetriDishBlock(context, clickedPos);
        }

        if (context.getClickedFace() != Direction.UP) {
            return super.useOn(context);
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = placeContext.getClickedPos();
        BlockState placeState = ModAlchemyBlocks.PETRI_DISH.get().defaultBlockState();
        if (!level.getBlockState(placePos).canBeReplaced(placeContext) || !placeState.canSurvive(level, placePos)) {
            return super.useOn(context);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, placeState, 3)) {
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(placePos);
        if (blockEntity instanceof PetriDishBlockEntity petriDish) {
            petriDish.loadFromItem(stack);
        }
        shrinkPlacedStack(context.getPlayer(), stack);
        return InteractionResult.CONSUME;
    }

    private InteractionResult addToPetriDishBlock(UseOnContext context, BlockPos pos) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PetriDishBlockEntity petriDish && petriDish.addDish(stack)) {
            shrinkPlacedStack(context.getPlayer(), stack);
        }
        return InteractionResult.CONSUME;
    }

    private static void shrinkPlacedStack(Player player, ItemStack stack) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:wisteria_infusion")
            || BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:immortal_extract")
            || super.hasCraftingRemainingItem(stack);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:wisteria_infusion")
            || BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:immortal_extract")) {
            return BloodDemonArtAlchemyCatalog.stack("kimetsunoyaibamultiplayer:empty_vial");
        }
        return super.getCraftingRemainingItem(stack);
    }

    public int tintColor() {
        return tintColor;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (BloodDemonArtAlchemyCatalog.isPotencyCulture(stack) && stack.hasTag() && stack.getTag().contains("Potency")) {
            tooltip.add(Component.translatable("item.kimetsunoyaibamultiplayer.potency", BloodDemonArtAlchemyCatalog.potency(stack)));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static boolean isDrinkable(ItemStack stack) {
        return BloodDemonArtAlchemyCatalog.isInfusion(stack)
            || BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:antivenom")
            || BlueSpiderLilyTeaHandler.isTea(stack)
            || AlchemyMedicineHandler.isLegendaryMedicine(stack);
    }

    private static void applyDrinkEffect(ItemStack stack, LivingEntity entity) {
        if (AlchemyMedicineHandler.applyDrunkMedicine(stack, entity)) {
            return;
        }
        if (BlueSpiderLilyTeaHandler.isTea(stack)) {
            BlueSpiderLilyTeaHandler.applyTea(entity);
            return;
        }
        if (BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:antivenom")) {
            entity.removeEffect(MobEffects.POISON);
            entity.removeEffect(MobEffects.WITHER);
            entity.removeEffect(MobEffects.CONFUSION);
            return;
        }

        String effectId = BloodDemonArtAlchemyCatalog.infusionEffectId(stack);
        int durationTicks = BloodDemonArtAlchemyCatalog.infusionDurationSeconds(stack) * 20;
        int amplifier = BloodDemonArtAlchemyCatalog.infusionAmplifier(stack);
        if (BloodDemonArtAlchemyCatalog.isFireInfusionEffectId(effectId)) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), durationTicks));
            return;
        }
        if (BloodDemonArtAlchemyCatalog.isFrozenInfusionEffectId(effectId)) {
            entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), Math.max(entity.getTicksFrozen(), durationTicks)));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, Math.max(0, amplifier - 1), false, true));
            return;
        }

        ResourceLocation effectKey = ResourceLocation.tryParse(effectId);
        MobEffect effect = effectKey == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectKey);
        if (effect != null) {
            entity.addEffect(new MobEffectInstance(
                effect,
                durationTicks,
                amplifier,
                false,
                true
            ));
        }
    }
}
