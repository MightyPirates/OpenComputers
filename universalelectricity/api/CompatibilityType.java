package universalelectricity.api;

import cpw.mods.fml.common.Loader;

/**
 * ORDER OF MAGNITUDE:
 * A coal in Universal Electricity (based on an estimate in real life) is worth 4MJ.
 * A fission reactor should make around 4-9GW.
 * A fusion reactor would go into the tera-watts.
 * 
 * @author Calclavia
 */
public enum CompatibilityType
{
	THERMAL_EXPANSION("ThermalExpansion", "ThermalExpansion", "Redstone Flux", "RF", 5628),
	INDUSTRIALCRAFT("IC2", "IndustrialCraft", "Electrical Unit", "EU", 140700),
	BUILDCRAFT("BuildCraft|Energy", "BuildCraft", "Minecraft Joule", "MJ", 56280);

	public final String modID;
	public final String moduleName;
	public final String fullUnit;
	public final String unit;

	/**
	 * Multiply UE energy by this ratio to convert it to the forgien ratio.
	 */
	public double ratio;

	/**
	 * Multiply the forgien energy by this ratio to convert it into UE energy.
	 */
	public double reciprocal_ratio;

	/**
	 * The Universal Electricity Loader will change this value to indicate if the module is
	 * loaded or not.
	 */
	public boolean isModuleEnabled;

	/**
	 * @param modID - The Forge mod ID.
	 * @param moduleName - The name of the module, used for config and ASM
	 * @param fullUnit - The unit used
	 * @param unit - The unit short form used
	 * @param ratio - How much UE energy equates to the forgien energy?
	 */
	CompatibilityType(String modID, String moduleName, String fullUnit, String unit, int ratio)
	{
		this.modID = modID;
		this.moduleName = moduleName;
		this.fullUnit = fullUnit;
		this.unit = unit;
		this.ratio = 1 / ratio;
		this.reciprocal_ratio = ratio;
	}

	public boolean isLoaded()
	{
		return Loader.isModLoaded(this.modID);
	}

	public static CompatibilityType get(String moduleName)
	{
		for (CompatibilityType type : values())
		{
			if (moduleName.equals(type.moduleName))
			{
				return type;
			}
		}

		return null;
	}
}