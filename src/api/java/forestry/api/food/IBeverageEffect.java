/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.food;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface IBeverageEffect {
	int getId();

	void doEffect(World world, EntityPlayer player);

	String getDescription();
}
