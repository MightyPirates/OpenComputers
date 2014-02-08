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

        @Callback
        public Object[] storedItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] storedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.storedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] remainingItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] remainingItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.remainingItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] containsItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.containsItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] getTotalItemTypes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getTotalItemTypes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] countOfItemType(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = (args.count() > 1) ? args.checkInteger(1) : 0;
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.countOfItemType(Util.createItemStack(new ItemStack(itemId, 1, itemDamage)))}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
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

        @Callback
        public Object[] getPriority(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getPriority()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] totalBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.totalBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] freeBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.freeBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] usedBytes(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.usedBytes()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] unusedItemCount(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.unusedItemCount()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] canHoldNewItem(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.canHoldNewItem()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
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

        @Callback
        public Object[] isPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] isFuzzyPreformatted(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.isFuzzyPreformatted()}
                    : new Object[]{null, "no storage cell"};
        }

        @Callback
        public Object[] getName(final Context context, final Arguments args) {
            final IMEInventoryHandler cell = tileEntity.provideCell();
            return cell != null
                    ? new Object[]{cell.getName()}
                    : new Object[]{null, "no storage cell"};
        }
    }
}
