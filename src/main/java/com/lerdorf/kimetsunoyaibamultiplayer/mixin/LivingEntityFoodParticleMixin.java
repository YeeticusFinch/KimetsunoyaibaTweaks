package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFoodParticleMixin {
    private static final TagKey<Item> HUMAN_FLESH_SOURCES = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "human_flesh_sources")
    );

    @Redirect(
        method = "triggerItemUseEffects",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;spawnItemParticles(Lnet/minecraft/world/item/ItemStack;I)V"
        ),
        require = 0
    )
    private void kimetsu$replaceHumanFleshEatParticles(LivingEntity self, ItemStack stack, int count) {
        if (!stack.is(HUMAN_FLESH_SOURCES)) {
            kimetsu$spawnVanillaEatParticles(self, stack, count);
            return;
        }

        kimetsu$spawnBloodEatParticles(self, stack, count);
    }

    // @Redirect(
    //     method = "m_21137_(Lnet/minecraft/world/item/ItemStack;I)V",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Lnet/minecraft/world/entity/LivingEntity;spawnItemParticles(Lnet/minecraft/world/item/ItemStack;I)V"
    //     ),
    //     require = 0,
    //     remap = false
    // )
    // private void kimetsu$replaceHumanFleshEatParticlesSrg(LivingEntity self, ItemStack stack, int count) {
    //     kimetsu$replaceHumanFleshEatParticles(self, stack, count);
    // }

    private void kimetsu$spawnVanillaEatParticles(LivingEntity self, ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) {
            return;
        }

        RandomSource random = self.getRandom();
        for (int i = 0; i < count; ++i) {
            Vec3 velocity = new Vec3(((double)random.nextFloat() - 0.5D) * 0.1D, random.nextDouble() * 0.1D + 0.1D, 0.0D);
            velocity = velocity.xRot(-self.getXRot() * ((float)Math.PI / 180F));
            velocity = velocity.yRot(-self.getYRot() * ((float)Math.PI / 180F));
            double yOffset = (double)(-random.nextFloat()) * 0.6D - 0.3D;
            Vec3 spawnPos = new Vec3(((double)random.nextFloat() - 0.5D) * 0.3D, yOffset, 0.6D);
            spawnPos = spawnPos.xRot(-self.getXRot() * ((float)Math.PI / 180F));
            spawnPos = spawnPos.yRot(-self.getYRot() * ((float)Math.PI / 180F));
            spawnPos = spawnPos.add(self.getX(), self.getEyeY(), self.getZ());

            if (self.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, stack),
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    1,
                    velocity.x,
                    velocity.y + 0.05D,
                    velocity.z,
                    0.0D
                );
            } else {
                self.level().addParticle(
                    new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, stack),
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    velocity.x,
                    velocity.y + 0.05D,
                    velocity.z
                );
            }
        }
    }

    private void kimetsu$spawnBloodEatParticles(LivingEntity self, ItemStack stack, int count) {
        if (count <= 0 || stack.isEmpty()) {
            return;
        }

        RandomSource random = self.getRandom();
        for (int i = 0; i < count; ++i) {
            Vec3 velocity = new Vec3(((double)random.nextFloat() - 0.5D) * 0.1D, random.nextDouble() * 0.1D + 0.1D, 0.0D);
            velocity = velocity.xRot(-self.getXRot() * ((float)Math.PI / 180F));
            velocity = velocity.yRot(-self.getYRot() * ((float)Math.PI / 180F));

            double yOffset = (double)(-random.nextFloat()) * 0.6D - 0.3D;
            Vec3 spawnPos = new Vec3(((double)random.nextFloat() - 0.5D) * 0.3D, yOffset, 0.6D);
            spawnPos = spawnPos.xRot(-self.getXRot() * ((float)Math.PI / 180F));
            spawnPos = spawnPos.yRot(-self.getYRot() * ((float)Math.PI / 180F));
            spawnPos = spawnPos.add(self.getX(), self.getEyeY(), self.getZ());

            if (self.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ModParticles.BLOOD.get(),
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    1,
                    velocity.x,
                    velocity.y + 0.05D,
                    velocity.z,
                    0.0D
                );
            } else {
                self.level().addParticle(
                    ModParticles.BLOOD.get(),
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    velocity.x,
                    velocity.y + 0.05D,
                    velocity.z
                );
            }
        }
    }
}
