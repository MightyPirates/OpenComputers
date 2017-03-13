package li.cil.oc.common.tileentity.traits;

import li.cil.oc.common.inventory.ItemHandlerHosted;

public interface ItemHandlerHostTileEntityProxy extends ItemHandlerHosted.ItemHandlerHost, TileEntityAccess {
    @Override
    default void markHostChanged() {
        getTileEntity().markDirty();
    }
}
