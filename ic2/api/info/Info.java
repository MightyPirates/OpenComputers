package ic2.api.info;

import net.minecraft.util.DamageSource;

public class Info {
	public static IEnergyValueProvider itemEnergy;
	public static IFuelValueProvider itemFuel;
	public static Object ic2ModInstance;

	/**
	 * Damage Sources used by IC2.
	 * Getting assigned in preload.
	 */
	public static DamageSource DMG_ELECTRIC, DMG_NUKE_EXPLOSION, DMG_RADIATION;
}