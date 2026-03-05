package com.lerdorf.kimetsunoyaibamultiplayer;

import java.util.Map;
import static java.util.Map.entry;   

public class BaseKnYForms {
	
	
	
	public static Map<Integer, BaseForm> forms = Map.ofEntries(
			
			// Bamboo
			entry(1, new BaseForm("Bamboo Breathing 1st Form", "§a")),
			entry(2, new BaseForm("Bamboo Breathing 2st Form", "§a")),
			entry(3, new BaseForm("Bamboo Breathing 3st Form", "§a")),
			entry(4, new BaseForm("Bamboo Breathing 4st Form", "§a")),
			entry(5, new BaseForm("Bamboo Breathing 5st Form", "§a")),
			entry(6, new BaseForm("Bamboo Breathing 6st Form", "§a")),
			
			// Water
			entry(101, new BaseForm("Water Breathing 1st Form: Water Surface Slash", "§9")),
			entry(102, new BaseForm("Water Breathing 2nd Form: Water Wheel", "§9")),
			entry(103, new BaseForm("Water Breathing 3rd Form: Flowing Dance", "§9")),
			entry(104, new BaseForm("Water Breathing 4th Form: Striking Tide", "§9")),
			entry(105, new BaseForm("Water Breathing 5th Form: Blessed Rain After the Drought", "§9")),
			entry(106, new BaseForm("Water Breathing 6th Form: Whirlpool", "§9")),
			entry(107, new BaseForm("Water Breathing 7th Form: Drop Ripple Thrust", "§9")),
			entry(108, new BaseForm("Water Breathing 8th Form: Waterfall Basin", "§9")),
			entry(109, new BaseForm("Water Breathing 9th Form: Splashing Water Flow", "§9")),
			entry(110, new BaseForm("Water Breathing 10th Form: Constant Flux", "§9")),
			entry(111, new BaseForm("Water Breathing 11th Form: Dead Calm", "§9")),

			// Beast
			entry(201, new BaseForm("Beast Breathing 1st Fang", "§3")),
			entry(202, new BaseForm("Beast Breathing 2nd Fang", "§3")),
			entry(203, new BaseForm("Beast Breathing 3rd Fang", "§3")),
			entry(204, new BaseForm("Beast Breathing 4th Fang", "§3")),
			entry(205, new BaseForm("Beast Breathing 5th Fang", "§3")),
			entry(206, new BaseForm("Beast Breathing 6th Fang", "§3")),
			entry(207, new BaseForm("Beast Breathing 7th Fang", "§3")),
			entry(208, new BaseForm("Beast Breathing 8th Fang", "§3")),
			entry(209, new BaseForm("Beast Breathing 9th Fang", "§3")),
			entry(210, new BaseForm("Beast Breathing 10th Fang", "§3")),

			// Thunder
			entry(301, new BaseForm("Thunder Breathing 1st Form: Thunderclap and Flash", "§e")),
			entry(302, new BaseForm("Thunder Breathing 2nd Form: Rice Spirit", "§e")),
			entry(303, new BaseForm("Thunder Breathing 3rd Form: Thunder Swarm", "§e")),
			entry(304, new BaseForm("Thunder Breathing 4th Form: Distant Thunder", "§e")),
			entry(305, new BaseForm("Thunder Breathing 5th Form: Heat Lightning", "§e")),
			entry(306, new BaseForm("Thunder Breathing 6th Form: Rumble and Flash", "§e")),
			entry(307, new BaseForm("Thunder Breathing 7th Form: Honoikazuchi no Kami", "§e")),
			entry(309, new BaseForm("Thunder Breathing 9th Form", "§e")),

			// Flame
			entry(401, new BaseForm("Flame Breathing 1st Form: Unknowing Fire", "§4")),
			entry(402, new BaseForm("Flame Breathing 2nd Form: Rising Scorching Sun", "§4")),
			entry(403, new BaseForm("Flame Breathing 3rd Form: Blazing Universe", "§4")),
			entry(404, new BaseForm("Flame Breathing 4th Form: Blooming Flame Undulation", "§4")),
			entry(405, new BaseForm("Flame Breathing 5th Form: Flame Tiger", "§4")),
			entry(409, new BaseForm("Flame Breathing 9th Form: Rengoku", "§4")),

			// Wind
			entry(501, new BaseForm("Wind Breathing 1st Form: Dust Whirlwind Cutter", "§2")),
			entry(502, new BaseForm("Wind Breathing 2nd Form: Claws, Purifying Wind", "§2")),
			entry(503, new BaseForm("Wind Breathing 3rd Form: Clean Storm Wind Tree", "§2")),
			entry(504, new BaseForm("Wind Breathing 4th Form: Rising Dust Storm", "§2")),
			entry(505, new BaseForm("Wind Breathing 5th Form: Cold Mountain Wind", "§2")),
			entry(506, new BaseForm("Wind Breathing 6th Form: Black Wind Mountain Mist", "§2")),
			entry(507, new BaseForm("Wind Breathing 7th Form: Gale, Sudden Gusts", "§2")),
			entry(508, new BaseForm("Wind Breathing 8th Form: Primary Gale Slash", "§2")),
			entry(509, new BaseForm("Wind Breathing 9th Form: Idaten Typhoon", "§2")),

			// Stone
			entry(601, new BaseForm("First Form: Serpentinite Bipolar", "§7")),
			entry(602, new BaseForm("Second Form: Upper Smash", "§7")),
			entry(603, new BaseForm("Third Form: Stone Skin", "§7")),
			entry(604, new BaseForm("Fourth Form: Volcanic Rock, Rapid Conquest", "§7")),
			entry(605, new BaseForm("Fifth Form: Arcs of Justice", "§7")),
			entry(606, new BaseForm("Stone Breathing 6th Form", "§7")),
			entry(607, new BaseForm("Stone Breathing 7th Form", "§7")),
			entry(608, new BaseForm("Stone Breathing 8th Form", "§7")),
			entry(609, new BaseForm("Stone Breathing 9th Form", "§7")),
			entry(611, new BaseForm("Stone Breathing 11th Form", "§7")),

			// Mist
			entry(701, new BaseForm("Mist Breathing 1st Form", "§b")),
			entry(702, new BaseForm("Mist Breathing 2nd Form", "§b")),
			entry(703, new BaseForm("Mist Breathing 3rd Form", "§b")),
			entry(704, new BaseForm("Mist Breathing 4th Form", "§b")),
			entry(705, new BaseForm("Mist Breathing 5th Form", "§b")),
			entry(706, new BaseForm("Mist Breathing 6th Form", "§b")),
			entry(707, new BaseForm("Mist Breathing 7th Form", "§b")),

			// Serpent
			entry(801, new BaseForm("First Form: Winding Serpent Slash", "§5")),
			entry(802, new BaseForm("Second Form: Venom Fangs of the Narrow Head", "§5")),
			entry(803, new BaseForm("Third Form: Coil Choke", "§5")),
			entry(804, new BaseForm("Fourth Form: Twin-Headed Reptile", "§5")),
			entry(805, new BaseForm("Fifth Form: Slithering Serpent", "§5")),

			// Sound
			entry(901, new BaseForm("First Form: Roar", "§6")),
			entry(902, new BaseForm("Fourth Form: Constant Resounding Slashes", "§6")),
			entry(903, new BaseForm("Fifth Form: String Performance", "§6")),

			// Moon
			entry(1101, new BaseForm("Moon Breathing 1st Form", "§d")),
			entry(1102, new BaseForm("Moon Breathing 2nd Form", "§d")),
			entry(1103, new BaseForm("Moon Breathing 3rd Form", "§d")),
			entry(1105, new BaseForm("Moon Breathing 5th Form", "§d")),
			entry(1106, new BaseForm("Moon Breathing 6th Form", "§d")),
			entry(1107, new BaseForm("Moon Breathing 7th Form", "§d")),
			entry(1108, new BaseForm("Moon Breathing 8th Form", "§d")),
			entry(1109, new BaseForm("Moon Breathing 9th Form", "§d")),
			entry(1110, new BaseForm("Moon Breathing 10th Form", "§d")),
			entry(1114, new BaseForm("Moon Breathing 14th Form", "§d")),
			entry(1116, new BaseForm("Moon Breathing 16th Form", "§d")),

			// Sun
			entry(1201, new BaseForm("Sun Breathing 1st Form", "§4")),
			entry(1202, new BaseForm("Sun Breathing 2nd Form", "§4")),
			entry(1203, new BaseForm("Sun Breathing 3rd Form", "§4")),
			entry(1204, new BaseForm("Sun Breathing 4th Form", "§4")),
			entry(1205, new BaseForm("Sun Breathing 5th Form", "§4")),
			entry(1206, new BaseForm("Sun Breathing 6th Form", "§4")),
			entry(1207, new BaseForm("Sun Breathing 7th Form", "§4")),
			entry(1208, new BaseForm("Sun Breathing 8th Form", "§4")),
			entry(1209, new BaseForm("Sun Breathing 9th Form", "§4")),
			entry(1210, new BaseForm("Sun Breathing 10th Form", "§4")),
			entry(1211, new BaseForm("Sun Breathing 11th Form", "§4")),
			entry(1212, new BaseForm("Sun Breathing 12th Form", "§4")),

			// Flower
			entry(1301, new BaseForm("Flower Breathing 1st Form: Flowing Tiger Lily Petals", "§5")),
			entry(1302, new BaseForm("Flower Breathing 2nd Form: Honorable Shadow Plum", "§5")),
			entry(1303, new BaseForm("Flower Breathing 3rd Form: Scattering Rose-Peach Thorns", "§5")),
			entry(1304, new BaseForm("Flower Breathing 4th Form: Crimson Hanagoromo", "§5")),
			entry(1305, new BaseForm("Flower Breathing 5th Form: Peonies of Futility", "§5")),
			entry(1306, new BaseForm("Flower Breathing 6th Form: Whirling Peach", "§5")),
			entry(1307, new BaseForm("Flower Breathing 7th Form: Bellowing Peach Shockwave", "§5")),
			entry(1308, new BaseForm("Flower Breathing 8th Form: Flowing Sakura Petal Onslaught", "§5")),
			entry(1309, new BaseForm("Flower Breathing 9th Form: Sudden Bamboo Entrapment", "§5")),
			entry(1310, new BaseForm("Flower Breathing Final Form: Equinoctial Vermillion Eye", "§5")),

			// Insect
			entry(1401, new BaseForm("Butterfly Dance: Caprice", "§5")),
			entry(1402, new BaseForm("Dance of the Bee Sting: True Flutter", "§5")),
			entry(1404, new BaseForm("Dance of the Dragonfly: Compound Eye Hexagon", "§5")),
			entry(1405, new BaseForm("Dance of the Centipede: Hundred-Legged Zigzag", "§5")),

			// Love
			entry(1501, new BaseForm("Love Breathing 1st Form", "§d")),
			entry(1502, new BaseForm("Love Breathing 2nd Form", "§d")),
			entry(1505, new BaseForm("Love Breathing 5th Form", "§d")),
			entry(1506, new BaseForm("Love Breathing 6th Form", "§d")),

			// Sakura
			entry(1801, new BaseForm("Sakura Breathing 1st Form", "§d")),
			entry(1802, new BaseForm("Sakura Breathing 2nd Form", "§d")),
			entry(1805, new BaseForm("Sakura Breathing 5th Form", "§d")),
			entry(1806, new BaseForm("Sakura Breathing 6th Form", "§d"))
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

