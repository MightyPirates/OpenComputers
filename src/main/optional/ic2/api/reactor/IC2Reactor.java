package ic2.api.reactor;

import java.lang.reflect.Field;

public class IC2Reactor {
	private static Field energyGeneratorNuclear;

	public static int getEUOutput() {
		try {
			if (energyGeneratorNuclear == null) energyGeneratorNuclear = Class.forName(getPackage() + ".core.IC2").getDeclaredField("energyGeneratorNuclear");

			return energyGeneratorNuclear.getInt(null);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the base IC2 package name, used internally.
	 * 
	 * @return IC2 package name, if unable to be determined defaults to ic2
	 */
	private static String getPackage() {
		Package pkg = IC2Reactor.class.getPackage();

		if (pkg != null) {
			String packageName = pkg.getName();

			return packageName.substring(0, packageName.length() - ".api.reactor".length());
		}

		return "ic2";
	}
}
