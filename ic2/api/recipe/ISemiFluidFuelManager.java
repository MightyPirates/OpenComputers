package ic2.api.recipe;

import java.util.Map;

import net.minecraftforge.fluids.Fluid;


public interface ISemiFluidFuelManager extends ILiquidAcceptManager {
	/**
	 * Add a new fluid to the semi fluid generator.
	 * 
	 * @param fluidName the fluid to burn
	 * @param amount amount of fluid to consume per tick
	 * @param power amount of energy generated per tick
	 */
	void addFluid(String fluidName, int amount, double power);

	BurnProperty getBurnProperty(Fluid fluid);

	Map<String, BurnProperty> getBurnProperties();


	public static class BurnProperty {
		public BurnProperty(int amount, double power) {
			this.amount = amount;
			this.power = power;
		}

		public final int amount;
		public final double power;
	}
}
