package com.lerdorf.kimetsunoyaibamultiplayer.capability;

import net.minecraft.resources.ResourceLocation;

public interface ISwordWielderData {
	boolean cancelAttackSwing();

	void setCancelAttackSwing(boolean value);

	/**
	 * Get the current sword model override (for special forms like Golden Senses)
	 * @return ResourceLocation of the model to use, or null for no override
	 */
	ResourceLocation getSwordModelOverride();

	/**
	 * Set a sword model override (for special forms like Golden Senses)
	 * @param model ResourceLocation of the model to use, or null to clear override
	 */
	void setSwordModelOverride(ResourceLocation model);
}
