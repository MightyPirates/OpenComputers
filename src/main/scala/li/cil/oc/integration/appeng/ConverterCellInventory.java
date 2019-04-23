package li.cil.oc.integration.appeng;

import appeng.api.AEApi;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import net.minecraft.item.ItemStack;
import li.cil.oc.api.driver.Converter;

import java.util.Map;

public final class ConverterCellInventory implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ICellInventory) {
            final ICellInventory cell = (ICellInventory) value;
            output.put("storedItemTypes", cell.getStoredItemTypes());
            output.put("storedItemCount", cell.getStoredItemCount());
            output.put("remainingItemCount", cell.getRemainingItemCount());
            output.put("remainingItemTypes", cell.getRemainingItemTypes());

            output.put("getTotalItemTypes", cell.getTotalItemTypes());
            output.put("getAvailableItems", cell.getAvailableItems(AEApi.instance().storage().createItemList()));

            output.put("totalBytes", cell.getTotalBytes());
            output.put("freeBytes", cell.getFreeBytes());
            output.put("usedBytes", cell.getUsedBytes());
            output.put("unusedItemCount", cell.getUnusedItemCount());
            output.put("canHoldNewItem", cell.canHoldNewItem());
            //output.put("getPreformattedItems",cell.getConfigInventory());

            output.put("fuzzyMode", cell.getFuzzyMode().toString());
            output.put("name", cell.getItemStack().getDisplayName());
        } else if (value instanceof ICellInventoryHandler) {
            convert(((ICellInventoryHandler) value).getCellInv(), output);
        } else if ((value instanceof ItemStack) && (((ItemStack)value).getItem() instanceof IStorageCell)) {
            IMEInventoryHandler<?> inventory = AEApi.instance().registries().cell().getCellInventory((ItemStack) value, null, StorageChannel.ITEMS);
            if (inventory instanceof ICellInventoryHandler)
                convert(((ICellInventoryHandler) inventory).getCellInv(), output);
        }

    }
}
