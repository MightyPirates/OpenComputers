/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.circuits;

import net.minecraft.item.ItemStack;

public interface ISolderManager {

	void addRecipe(ICircuitLayout layout, ItemStack resource, ICircuit circuit);

}
