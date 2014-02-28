/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.fuels;

import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class IronEngineFuel {

	public static Map<String, Fuel> fuels = new HashMap<String, Fuel>();

	public static Fuel getFuelForFluid(Fluid liquid) {
		return liquid == null ? null : fuels.get(liquid.getName());
	}

	private IronEngineFuel() {
	}

	public static class Fuel {

		public final Fluid liquid;
		public final float powerPerCycle;
		public final int totalBurningTime;

		private Fuel(String fluidName, float powerPerCycle, int totalBurningTime) {
			this(FluidRegistry.getFluid(fluidName), powerPerCycle, totalBurningTime);
		}

		private Fuel(Fluid liquid, float powerPerCycle, int totalBurningTime) {
			this.liquid = liquid;
			this.powerPerCycle = powerPerCycle;
			this.totalBurningTime = totalBurningTime;
		}
	}

	public static void addFuel(Fluid fluid, float powerPerCycle, int totalBurningTime) {
		fuels.put(fluid.getName(), new Fuel(fluid, powerPerCycle, totalBurningTime));
	}

	public static void addFuel(String fluidName, float powerPerCycle, int totalBurningTime) {
		fuels.put(fluidName, new Fuel(fluidName, powerPerCycle, totalBurningTime));
	}
}
