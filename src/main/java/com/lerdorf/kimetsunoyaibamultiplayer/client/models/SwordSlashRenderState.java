package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Dummy GeoAnimatable for sword slash rendering
 * Used by SwordSlashModel for GeckoLib integration
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashRenderState implements GeoAnimatable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	@Override
	public void registerControllers(ControllerRegistrar controllers) {
        // No animation controllers needed for static slash models
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object object) {
		return 0;
	}

}
