package li.cil.oc.driver.appeng;

import appeng.api.IAEItemStack;
import appeng.api.me.tiles.ICellProvider;
import appeng.api.me.util.IMEInventoryHandler;
import buildcraft.core.IMachine;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.Registry;
import li.cil.oc.driver.TileEntityDriver;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class DriverCellProvider extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return ICellProvider.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((ICellProvider) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ICellProvider> {
        public Environment(ICellProvider tileEntity) {
            super(tileEntity, "celltileEntity");
        }

        @Callback
        public Object[] getTotalItemTypes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.getTotalItemTypes()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getPriority(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.getPriority()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] canHoldNewItem(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.canHoldNewItem()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getFreeBytes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.freeBytes()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getAvailableItems(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                ArrayList<Map> list = new ArrayList<Map>();
                for (IAEItemStack stack : cell.getAvailableItems()) {
                    Map m = HandlerAppEng.toMap(stack);
                    list.add(m);
                }
                return list.toArray();
            }
            return new Object[]{null, "Cell Null"};
        }



        @Callback
        public Object[] containsItemType(final Context context, final Arguments args) {
            try {
                return new Object[]{((Integer) countOfItemType(context, args)[0]) > 0};
            } catch (Throwable e) {
                return new Object[]{null, "Cell Null"};
            }

        }

        @Callback
        public Object[] countOfItemType(final Context context, final Arguments args) {

            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell == null) {
                return new Object[]{null, "Cell Null"};
            }
            Iterator<IAEItemStack> iterator = cell.getAvailableItems().iterator();
            long c = 0;
            while (iterator.hasNext()) {
                IAEItemStack next = iterator.next();
                if (next.getItemID() == args.checkInteger(0) && next.getItemDamage() == args.checkInteger(1)) {
                    c += next.getStackSize();
                }
            }
            return new Object[]{c};
        }

        @Callback
        public Object[] getName(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.getName()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getPreformattedItems(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                ArrayList<Map> list = new ArrayList<Map>();
                for (ItemStack stack : cell.getPreformattedItems()) {
                    Map m = Registry.toMap(stack);
                    list.add(m);
                }
                return list.toArray();
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] isFuzzyPreformatted(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.isFuzzyPreformatted()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] isPreformatted(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.isPreformatted()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getRemainingItemCount(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.remainingItemCount()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getRemainingItemTypes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.remainingItemTypes()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getStoredItemCount(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.storedItemCount()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getStoredItemTypes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.storedItemTypes()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getTotalBytes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.totalBytes()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getUnusedItemCount(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.unusedItemCount()};
            }
            return new Object[]{null, "Cell Null"};
        }

        @Callback
        public Object[] getUnusedBytes(final Context context, final Arguments args) {
            IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell != null) {
                return new Object[]{cell.usedBytes()};
            }
            return new Object[]{null, "Cell Null"};
        }
    }
}
