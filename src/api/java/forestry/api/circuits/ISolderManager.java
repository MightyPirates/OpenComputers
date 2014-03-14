package forestry.api.circuits;

import net.minecraft.item.ItemStack;

public interface ISolderManager {

	void addRecipe(ICircuitLayout layout, ItemStack resource, ICircuit circuit);

}
