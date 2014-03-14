package ic2.api.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidStack;

public class RecipeInputFluidContainer implements IRecipeInput {
	public RecipeInputFluidContainer(Fluid fluid) {
		this(fluid, FluidContainerRegistry.BUCKET_VOLUME);
	}

	public RecipeInputFluidContainer(Fluid fluid, int amount) {
		this.fluid = fluid;
		this.amount = amount;
	}

	@Override
	public boolean matches(ItemStack subject) {
		FluidStack fs = FluidContainerRegistry.getFluidForFilledItem(subject);
		if (fs == null) return false;

		return fs.getFluid() == fluid;
	}

	@Override
	public int getAmount() {
		return amount;
	}

	@Override
	public List<ItemStack> getInputs() {
		List<ItemStack> ret = new ArrayList<ItemStack>();

		for (FluidContainerData data : FluidContainerRegistry.getRegisteredFluidContainerData()) {
			if (data.fluid.getFluid() == fluid) ret.add(data.filledContainer);
		}

		return ret;
	}

	public final Fluid fluid;
	public final int amount;
}
