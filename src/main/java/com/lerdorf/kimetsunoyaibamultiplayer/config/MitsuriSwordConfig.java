package com.lerdorf.kimetsunoyaibamultiplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MitsuriSwordConfig {
	/*
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

	
    public static int CURVE_RESOLUTION = 24;  // number of samples along curve
    public static float WIDTH = 0.06f;
    public static boolean EMISSIVE = true;
	
	static {
		BUILDER.comment("Mitsuri Sword visual settings").push("mitsuri_sword");
		CURVE_RESOLUTION = BUILDER.comment("Curve resolution (segments)")
		        .defineInRange("curveResolution", 24, 6, 64).get();
		BUILDER.pop();
	}
	
	public static int mitsuriSwordCurveResolution;
    
    /**
     * Event handler for config loading/reloading
     */
	/*
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
    	 mitsuriSwordCurveResolution = CURVE_RESOLUTION.get();
    }
    */
}
