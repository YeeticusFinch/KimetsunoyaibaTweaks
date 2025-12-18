package com.lerdorf.kimetsunoyaibamultiplayer;

import java.util.Map;
import static java.util.Map.entry;   

public class BaseKnYForms {
	
	
	
	public static Map<Integer, BaseForm> forms = Map.ofEntries(
			// Water
			entry(101, new BaseForm("Water Breathing 1st Form: Water Surface Slash", "§1")),
			entry(102, new BaseForm("Water Breathing 2nd Form: Water Wheel", "§1")),
			entry(103, new BaseForm("Water Breathing 3rd Form: Flowing Dance", "§1")),
			entry(104, new BaseForm("Water Breathing 4th Form: Striking Tide", "§1")),
			entry(105, new BaseForm("Water Breathing 5th Form: Blessed Rain After the Drought", "§1")),
			entry(106, new BaseForm("Water Breathing 6th Form: Whirlpool", "§1")),
			entry(107, new BaseForm("Water Breathing 7th Form: Drop Ripple Thrust", "§1")),
			entry(108, new BaseForm("Water Breathing 8th Form: Waterfall Basin", "§1")),
			entry(109, new BaseForm("Water Breathing 9th Form: Splashing Water Flow", "§1")),
			entry(110, new BaseForm("Water Breathing 10th Form: Constant Flux", "§1")),
			
			// Thunder
			entry(301, new BaseForm("Thunder Breathing 1st Form: Thunderclap and Flash", "§e")),
			entry(302, new BaseForm("Thunder Breathing 2nd Form: Rice Spirit", "§e")),
			entry(303, new BaseForm("Thunder Breathing 3rd Form: Thunder Swarm", "§e")),
			entry(304, new BaseForm("Thunder Breathing 4th Form: Distant Thunder", "§e")),
			entry(305, new BaseForm("Thunder Breathing 5th Form: Heat Lightning", "§e")),
			entry(306, new BaseForm("Thunder Breathing 6th Form: Rumble and Flash", "§e")),
			entry(307, new BaseForm("Thunder Breathing 7th Form: Honoikazuchi no Kami", "§e")),
			
			// Flame
			entry(401, new BaseForm("Flame Breathing 1st Form: Unknowing Fire", "§4")),
			entry(402, new BaseForm("Flame Breathing 2nd Form: Rising Scorching Sun", "§4")),
			entry(403, new BaseForm("Flame Breathing 3rd Form: Blazing Universe", "§4")),
			entry(404, new BaseForm("Flame Breathing 4th Form: Blooming Flame Undulation", "§4")),
			entry(405, new BaseForm("Flame Breathing 5th Form: Flame Tiger", "§4")),
			entry(409, new BaseForm("Flame Breathing 9th Form: Rengoku", "§4"))
	);
	
	
	
	
	
	public static class BaseForm {
		public String name;
		public String color;
		
		public BaseForm(String name, String color) {
			this.name = name;
			this.color = color;
		}
	}

}

