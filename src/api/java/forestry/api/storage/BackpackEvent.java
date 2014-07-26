/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import cpw.mods.fml.common.eventhandler.Event;


public abstract class BackpackEvent extends Event {

	public final EntityPlayer player;
	public final IBackpackDefinition backpackDefinition;
	public final IInventory backpackInventory;

	public BackpackEvent(EntityPlayer player, IBackpackDefinition backpackDefinition, IInventory backpackInventory) {
		this.player = player;
		this.backpackDefinition = backpackDefinition;
		this.backpackInventory = backpackInventory;
	}
}
