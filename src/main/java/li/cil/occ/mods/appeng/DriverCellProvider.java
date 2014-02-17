package li.cil.occ.mods.appeng;

import appeng.api.IAEItemStack;
import appeng.api.Util;
import appeng.api.me.tiles.ICellProvider;
import appeng.api.me.util.IMEInventoryHandler;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.mods.Registry;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Map;

public final class DriverCellProvider extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ICellProvider.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((ICellProvider) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ICellProvider> {
        public Environment(final ICellProvider tileEntity) {
            super(tileEntity, "me_cell_provider");
        }

        // ----------------------------------------------------------------- //
        // IMEInventory

        @Callback(doc="function():number -- Get the number of stored item types.")
        public Object[] storedItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- Get the number of stored items total, regardless of type.")
        public Object[] storedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- Get the estimated number of additional items this inventory can hold, regardless of type..")
        public Object[] remainingItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- The estimated number of additional types the inventory could hold.")
        public Object[] remainingItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function(itemId:number[,itemDamage:number]):boolean -- True or False if this item is inside this inventory.")
        public Object[] containsItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.containsItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number --  The total number of types holdable in this inventory.")
        public Object[] getTotalItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getTotalItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function(itemId:number[,itemDamage:number]):number -- Returns how many of this item are in the inventory, regardless of a how many stacks / cells or anything else.")
        public Object[] countOfItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.countOfItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():list -- Returns a list of all available items, with stackSize set to the real amount, without stack limits.")
        public Object[] getAvailableItems(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell == null) {
                return new Object[]{null, "no storage cell"};
            }
            final ArrayList<Map> list = new ArrayList<Map>();
            for (IAEItemStack stack : cell.getAvailableItems()) {
                list.add(ModAppEng.toMap(stack));
            }
            return list.toArray();
        }

        // ----------------------------------------------------------------- //
        // IMEInventoryHandler


        @Callback(doc="function():number --  Returns estimated number of total bytes represented by the inventory, used mainly for display.")
        public Object[] totalBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.totalBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- Returns estimated number of free bytes represented by inventory, used mainly for display.")
        public Object[] freeBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.freeBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- Returns number of used bytes represented by the inventory, used mainly for display.")
        public Object[] usedBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.usedBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():number -- The number of items you could add before the freeBytes() decreases.")
        public Object[] unusedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.unusedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():boolean -- True of False, if you could add a new item type.")
        public Object[] canHoldNewItem(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.canHoldNewItem()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():list -- Get the list of preformatted Items")
        public Object[] getPreformattedItems(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell == null) {
                return new Object[]{null, "no storage cell"};
            }
            final ArrayList<Map> list = new ArrayList<Map>();
            for (ItemStack stack : cell.getPreformattedItems()) {
                list.add(Registry.toMap(stack));
            }
            return list.toArray();
        }

        @Callback(doc="function():boolean -- Returns if the cell is preformatted")
        public Object[] isPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():boolean -- Returns if the cell is fuzzy preformatted")
        public Object[] isFuzzyPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isFuzzyPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc="function():string -- Get the name")
        public Object[] getName(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getName()}
                    : new Object[]{null, "no storage cell"};
        }
    }
}
