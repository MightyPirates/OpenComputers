package li.cil.oc.driver.appeng;

import appeng.api.IAEItemStack;
import appeng.api.Util;
import appeng.api.exceptions.AppEngTileMissingException;
import appeng.api.me.tiles.ICellProvider;
import appeng.api.me.tiles.IGridTileEntity;
import appeng.api.me.util.IGridInterface;
import appeng.api.me.util.IMEInventoryHandler;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by lordjoda on 06.02.14.
 */
public class DriverGridTileEntity extends TileEntityDriver {
            @Override
            public Class<?> getFilterClass() {
                return IGridTileEntity.class;
            }

            @Override
            public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
                return new Environment((IGridTileEntity) world.getBlockTileEntity(x, y, z));
            }

            public static final class Environment extends ManagedTileEntityEnvironment<IGridTileEntity> {
                public Environment(IGridTileEntity tileEntity) {
                    super(tileEntity, "gridtileEntity");
                }

        @Callback
        public Object[] requestCrafting(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                try {

                    tileEntity.getGrid().craftingRequest(new ItemStack(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)));
                    return new Object[]{true};
                } catch (Throwable e) {

                }
                return new Object[]{false};
            }
            return new Object[]{null, "Grid Null"};
        }

//        @Callback
//        public int extractItem(final Context context, final Arguments args) {
//
//        }

//        @Callback
//        public int insertItem(final Context context, final Arguments args) {
//         //not used
//        }

        @Callback
        public Object[] getTotalItemTypes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.getTotalItemTypes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getPriority(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.getPriority()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] canHoldNewItem(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.canHoldNewItem()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getFreeBytes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.freeBytes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getAvailableItems(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    ArrayList<Map> list = new ArrayList<Map>();
                    for (IAEItemStack stack : cell.getAvailableItems()) {
                        Map m = HandlerAppEng.toMap(stack);
                        list.add(m);
                    }
                    return list.toArray();
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] containsItemType(final Context context, final Arguments args) {
            return new Object[]{((Integer) countOfItemType(context, args)[0]) > 0};
        }

        @Callback
        public Object[] countOfItemType(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid == null) {
                return new Object[]{null, "Grid Null"};
            }
            IMEInventoryHandler cell = grid.getCellArray();
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
        public Object[] getPreformattedItems(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    ArrayList<Map> list = new ArrayList<Map>();
                    for (ItemStack stack : cell.getPreformattedItems()) {
                        Map m = Registry.toMap(stack);
                        list.add(m);
                    }
                    return list.toArray();
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] isFuzzyPreformatted(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.isFuzzyPreformatted()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] isPreformatted(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.isPreformatted()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getRemainingItemCount(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.remainingItemCount()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getRemainingItemTypes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.remainingItemTypes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getStoredItemCount(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.storedItemCount()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getStoredItemTypes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.storedItemTypes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getTotalBytes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.totalBytes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getUnusedItemCount(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.unusedItemCount()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }

        @Callback
        public Object[] getUnusedBytes(final Context context, final Arguments args) {
            IGridInterface grid = tileEntity.getGrid();
            if (grid != null) {
                IMEInventoryHandler cell = grid.getCellArray();
                if (cell != null) {
                    return new Object[]{cell.usedBytes()};
                }
            }
            return new Object[]{null, "Grid Null"};
        }
    }
}
