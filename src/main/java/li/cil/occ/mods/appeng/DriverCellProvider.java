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

        @Callback(doc = "function():number -- Get the number of stored item types.")
        public Object[] storedItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- Get the number of stored items total, regardless of type.")
        public Object[] storedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- Get the estimated number of additional items this inventory can hold, regardless of type.")
        public Object[] remainingItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- The estimated number of additional types the inventory could hold.")
        public Object[] remainingItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function(itemId:number[, itemDamage:number]):boolean -- Whether this inventory contains such an item.")
        public Object[] containsItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.containsItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- The total number of types this inventory can hold.")
        public Object[] getTotalItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getTotalItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function(itemId:number[, itemDamage:number]):number -- Returns how many of this item are in the inventory, regardless of a how many stacks / cells or anything else.")
        public Object[] countOfItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.countOfItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():table -- Returns a list of all available items, with stackSize set to the real amount, without stack limits.")
        public Object[] getAvailableItems(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell == null) {
                return new Object[]{null, "no storage cell"};
            }
            final ArrayList<Map> list = new ArrayList<Map>();
            for (IAEItemStack stack : cell.getAvailableItems()) {
                list.add(ModAppEng.toMap(stack));
            }
            return new Object[]{list.toArray()};
        }

        // ----------------------------------------------------------------- //
        // IMEInventoryHandler

        @Callback(doc = "function():number -- Returns the estimated number of total bytes represented by the inventory.")
        public Object[] totalBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.totalBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- Returns the estimated number of free bytes represented by inventory.")
        public Object[] freeBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.freeBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- Returns the number of used bytes represented by the inventory.")
        public Object[] usedBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.usedBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():number -- The number of items tha can be added before freeBytes() decreases.")
        public Object[] unusedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.unusedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():boolean -- Whether the specified item type can be added to the inventory.")
        public Object[] canHoldNewItem(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.canHoldNewItem()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():table -- Get the list of pre-formatted items")
        public Object[] getPreformattedItems(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            if (cell == null) {
                return new Object[]{null, "no storage cell"};
            }
            final ArrayList<Map> list = new ArrayList<Map>();
            for (ItemStack stack : cell.getPreformattedItems()) {
                list.add(Registry.toMap(stack));
            }
            return new Object[]{list.toArray()};
        }

        @Callback(doc = "function():boolean -- Returns whether the cell is pre-formatted")
        public Object[] isPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():boolean -- Returns whether the cell is fuzzy pre-formatted")
        public Object[] isFuzzyPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isFuzzyPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback(doc = "function():string -- Get the name of the inventory / storage cell.")
        public Object[] getName(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getName()}
                    : new Object[]{null, "no storage cell"};
        }
    }
}
