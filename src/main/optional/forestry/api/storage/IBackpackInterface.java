package forestry.api.storage;

import net.minecraft.item.Item;

public interface IBackpackInterface {

	/**
	 * Adds a backpack with the given id, definition and type, returning the item.
	 * 
	 * @param itemID
	 *            Item id to use.
	 * @param definition
	 *            Definition of backpack behaviour.
	 * @param type
	 *            Type of backpack. (T1 or T2 (= Woven)
	 * @return Created backpack item.
	 */
	Item addBackpack(int itemID, IBackpackDefinition definition, EnumBackpackType type);
}
