package forestry.api.core;

import java.util.ArrayList;

import net.minecraft.util.Icon;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 *  Many things Forestry use temperature and humidity of a biome to determine whether they can or how they can work or spawn at a given location.
 * 
 *  This enum concerns temperature. 
 */
public enum EnumTemperature {
	NONE("None", "habitats/ocean"), ICY("Icy", "habitats/snow"), COLD("Cold", "habitats/taiga"),
	NORMAL("Normal", "habitats/plains"), WARM("Warm", "habitats/jungle"), HOT("Hot", "habitats/desert"), HELLISH("Hellish", "habitats/nether");

	/**
	 * Populated by Forestry with vanilla biomes. Add additional icy/snow biomes here. (ex. snow plains)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> icyBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional cold biomes here. (ex. taiga)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> coldBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional normal biomes here. (ex. forest, plains)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> normalBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional warm biomes here. (ex. jungle)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> warmBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional hot biomes here. (ex. desert)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> hotBiomeIds = new ArrayList<Integer>();
	/**
	 * Populated by Forestry with vanilla biomes. Add additional hellish biomes here. (ex. nether)
	 * @deprecated Biomes will be checked live rather than relying on cached values, so you don't have to register them.
	 */
	@Deprecated
	public static ArrayList<Integer> hellishBiomeIds = new ArrayList<Integer>();

	public final String name;
	public final String iconIndex;

	private EnumTemperature(String name, String iconIndex) {
		this.name = name;
		this.iconIndex = iconIndex;
	}

	public String getName() {
		return this.name;
	}

	@SideOnly(Side.CLIENT)
	public Icon getIcon() {
		return ForestryAPI.textureManager.getDefault(iconIndex);
	}
	/**
	 * @deprecated Switching most internals to use getFromValue to not rely on cached values.
	 */
	public static ArrayList<Integer> getBiomeIds(EnumTemperature temperature) {

		switch (temperature) {
		case ICY:
			return icyBiomeIds;
		case COLD:
			return coldBiomeIds;
		case WARM:
			return warmBiomeIds;
		case HOT:
			return hotBiomeIds;
		case HELLISH:
			return hellishBiomeIds;
		case NORMAL:
		default:
			return normalBiomeIds;
		}

	}
	
	/**
	 * Determines if a given BiomeGenBase is of HELLISH temperature, since it is treated seperatly from actual temperature values.
	 * Uses the BiomeDictionary.
	 * @param biomeGen BiomeGenBase of the biome in question
	 * @return true, if the BiomeGenBase is a Nether-type biome; false otherwise.
	 */
	public static boolean isBiomeHellish(BiomeGenBase biomeGen) {
		return BiomeDictionary.isBiomeOfType(biomeGen, BiomeDictionary.Type.NETHER);
	}

	/**
	 * Determines if a given biomeID is of HELLISH temperature, since it is treated seperatly from actual temperature values.
	 * Uses the BiomeDictionary.
	 * @param biomeID ID of the BiomeGenBase in question
	 * @return true, if the biomeID is a Nether-type biome; false otherwise.
	 */
	public static boolean isBiomeHellish(int biomeID) {
		return BiomeDictionary.isBiomeRegistered(biomeID) && BiomeDictionary.isBiomeOfType(BiomeGenBase.biomeList[biomeID], BiomeDictionary.Type.NETHER);
	}
	
	/**
	 * Determines the EnumTemperature given a floating point representation of
	 * Minecraft temperature. Hellish biomes are handled based on their biome
	 * type - check isBiomeHellish.
	 * @param rawTemp raw temperature value
	 * @return EnumTemperature corresponding to value of rawTemp
	 */
	public static EnumTemperature getFromValue(float rawTemp) {
		EnumTemperature value = EnumTemperature.ICY;
		
		if (rawTemp >= 2.0f) {
			value = EnumTemperature.HOT;
		}
		else if (rawTemp >= 1.2f) {
			value = EnumTemperature.WARM;
		}
		else if (rawTemp >= 0.2f) {
			value = EnumTemperature.NORMAL;
		}
		else if (rawTemp >= 0.05f) {
			value = EnumTemperature.COLD;
		}

		return value;
	}
	
}
