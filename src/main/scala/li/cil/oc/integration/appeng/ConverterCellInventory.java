package li.cil.oc.integration.appeng;

import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.channels.IItemStorageChannel;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;

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
            output.put("getAvailableItems", cell.getAvailableItems(AEUtil.aeApi().get().storage().getStorageChannel(IItemStorageChannel.class).createList()));

            output.put("totalBytes", cell.getTotalBytes());
            output.put("freeBytes", cell.getFreeBytes());
            output.put("usedBytes", cell.getUsedBytes());
            output.put("unusedItemCount", cell.getUnusedItemCount());
            output.put("canHoldNewItem", cell.canHoldNewItem());
            //output.put("getPreformattedItems",cell.getConfigInventory());

            output.put("fuzzyMode", cell.getFuzzyMode().toString());
            output.put("name", cell.getItemStack().getDisplayName());
        } else if (value instanceof ICellInventoryHandler) {
            convert(((ICellInventoryHandler<?>) value).getCellInv(), output);
        } else if ((value instanceof ItemStack) && (((ItemStack)value).getItem() instanceof IStorageCell)) {
            ICellInventoryHandler<?> inventory = AEUtil.aeApi().get().registries().cell().getCellInventory((ItemStack) value, null, AEUtil.aeApi().get().storage().getStorageChannel(IItemStorageChannel.class));
            if (inventory != null)
                convert(((ICellInventoryHandler) inventory).getCellInv(), output);
        }
    }
}
