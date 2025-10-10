package com.lerdorf.kimetsunoyaibamultiplayer.capability;

import net.minecraft.resources.ResourceLocation;

public class SwordWielderData implements ISwordWielderData {
	private boolean cancellingAttackSwing = false;
	private ResourceLocation swordModelOverride = null;

	@Override
	public boolean cancelAttackSwing() {
		return cancellingAttackSwing;
	}

	@Override
	public void setCancelAttackSwing(boolean value) {
		cancellingAttackSwing = value;
	}

	@Override
	public ResourceLocation getSwordModelOverride() {
		return swordModelOverride;
	}

	@Override
	public void setSwordModelOverride(ResourceLocation model) {
		swordModelOverride = model;
	}
}
