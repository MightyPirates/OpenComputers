package appeng.api.storage;

import appeng.api.storage.data.IAEItemStack;

public interface ICellInventoryHandler extends IMEInventoryHandler<IAEItemStack>
{

	/**
	 * @return get access to the Cell Inventory.
	 */
	ICellInventory getCellInv();

}
