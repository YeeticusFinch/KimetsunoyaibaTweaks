package com.lerdorf.kimetsunoyaibamultiplayer.mixin.gravity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.GravityAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.gravity.engine.RotationUtil;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

@Mixin(Mob.class)
public abstract class MobMixin
{
	@WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getYRot()F"))
	private float wrapOperation_tryAttack_getYaw_0(Mob attacker, Operation<Float> original, Entity target)
	{
		Direction gravityDirection = GravityAPI.getGravityDirection(target);
		if(gravityDirection == Direction.DOWN)
		{
			return original.call(attacker);
		}

		return RotationUtil.rotWorldToPlayer(original.call(attacker), attacker.getXRot(), gravityDirection).x;
	}
}
