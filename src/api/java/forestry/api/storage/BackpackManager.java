/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.storage;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.item.ItemStack;

public class BackpackManager {
	/**
	 * 0 - Miner's Backpack 1 - Digger's Backpack 2 - Forester's Backpack 3 - Hunter's Backpack 4 - Adventurer's Backpack
	 * 
	 * Use IMC messages to achieve the same effect!
	 */
	public static ArrayList<ItemStack>[] backpackItems;

	public static IBackpackInterface backpackInterface;

	/**
	 * Only use this if you know what you are doing. Prefer backpackInterface.
	 */
	public static HashMap<String, IBackpackDefinition> definitions = new HashMap<String, IBackpackDefinition>();
}
