package ic2.api.info;

import net.minecraft.util.DamageSource;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;

public class Info {
	public static IEnergyValueProvider itemEnergy;
	public static IFuelValueProvider itemFuel;
	public static Object ic2ModInstance;

	/**
	 * Damage Sources used by IC2.
	 * Getting assigned in preload.
	 */
	public static DamageSource DMG_ELECTRIC, DMG_NUKE_EXPLOSION, DMG_RADIATION;

	public static boolean isIc2Available() {
		if (ic2Available != null) return ic2Available;

		boolean loaded = Loader.isModLoaded("IC2");

		if (Loader.instance().hasReachedState(LoaderState.CONSTRUCTING)) {
			ic2Available = loaded;
		}

		return loaded;
	}

	private static Boolean ic2Available = null;
}