package forestry.api.core;

import java.util.ArrayList;

/**
 *  Many things Forestry use temperature and humidity of a biome to determine whether they can or how they can work or spawn at a given location.
 * 
 *  This enum concerns humidity. 
 */
public enum EnumHumidity {
	ARID("Arid"), NORMAL("Normal"), DAMP("Damp");

	/**
	 * Populated by Forestry with vanilla biomes. Add additional arid biomes here. (ex. desert)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> aridBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional damp biomes here. (ex. jungle)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> dampBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional normal biomes here.
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> normalBiomeIds = new ArrayList<Integer>();

	public final String name;

	private EnumHumidity(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Deprecated
	public static ArrayList<Integer> getBiomeIds(EnumHumidity humidity) {
		switch (humidity) {
		case ARID:
			return aridBiomeIds;
		case DAMP:
			return dampBiomeIds;
		case NORMAL:
		default:
			return normalBiomeIds;
		}
	}
	
	/**
	 * Determines the EnumHumidity given a floating point representation of Minecraft Rainfall
	 * @param rawHumidity raw rainfall value
	 * @return EnumHumidity corresponding to rainfall value
	 */
	public static EnumHumidity getFromValue(float rawHumidity) {
		EnumHumidity value = EnumHumidity.ARID;
		
		if (rawHumidity >= 0.9f) {
			value = EnumHumidity.DAMP;
		}
		else if (rawHumidity >= 0.3f) {
			value = EnumHumidity.NORMAL;
		}

		return value;
	}
}
