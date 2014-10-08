package cofh.api.core;

import net.minecraft.item.ItemStack;

/**
 * Interface to allow a Container to interact with a secondary inventory.
 * 
 * @author King Lemming
 * 
 */
public interface ICustomInventory {

	ItemStack[] getInventorySlots(int inventoryIndex);

	int getSlotStackLimit(int slotIndex);

	void onSlotUpdate();

}
