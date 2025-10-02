package com.lerdorf.kimetsunoyaibamultiplayer.client;

public class SwordWielderData implements ISwordWielderData {
	private boolean cancellingAttackSwing = false;

	@Override
	public boolean cancelAttackSwing() {
		return cancellingAttackSwing;
	}

	@Override
	public void setCancelAttackSwing(boolean value) {
		cancellingAttackSwing = value;
	}
}