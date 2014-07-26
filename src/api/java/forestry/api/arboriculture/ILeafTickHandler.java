/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.arboriculture;

import net.minecraft.world.World;

public interface ILeafTickHandler {
	boolean onRandomLeafTick(ITree tree, World world, int biomeId, int x, int y, int z, boolean isDestroyed);
}
