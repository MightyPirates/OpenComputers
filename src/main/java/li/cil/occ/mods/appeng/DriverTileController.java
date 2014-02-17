package li.cil.occ.mods.appeng;

import appeng.api.exceptions.AppEngTileMissingException;
import appeng.api.me.tiles.IGridTileEntity;
import appeng.api.me.util.ICraftRequest;
import appeng.api.me.util.IGridInterface;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.mods.Registry;
import li.cil.occ.util.Reflection;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DriverTileController extends DriverTileEntity {
    private static final Class<?> TileController = Reflection.getClass("appeng.me.tile.TileController");

    @Override
    public Class<?> getTileEntityClass() {
        return TileController;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IGridTileEntity) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IGridTileEntity> {
        public Environment(final IGridTileEntity tileEntity) {
            super(tileEntity, "me_controller");
        }

        @Callback(doc="function(itemID:number[itemDamage:number=0,amount:number=1):boolean -- Requests to craft the specified item, and returns if the request was successful." )
        public Object[] craftingRequest(final Context context, final Arguments args) {
            final int itemId = args.checkInteger(0);
            final int itemDamage = args.count() > 1 ? args.checkInteger(1) : 0;
            final int count = args.count() > 2 ? args.checkInteger(2) : 1;
            final IGridInterface grid = tileEntity.getGrid();
            if (grid == null) {
                return new Object[]{null, "no grid"};
            }
            try {
                final ICraftRequest request = grid.craftingRequest(new ItemStack(itemId, count, itemDamage));
                return new Object[]{request != null};
            } catch (AppEngTileMissingException e) {
                return new Object[]{null, "missing tile"};
            }
        }

        @Callback(doc="function():number --  Reports previous 20 ticks avg of energy usage.")
        public Object[] getPowerUsageAvg(final Context context, final Arguments args) {
            final IGridInterface grid = tileEntity.getGrid();
            return grid != null
                    ? new Object[]{grid.getPowerUsageAvg()}
                    : new Object[]{null, "no grid"};
        }

        //Doesn't seem to do something says the api
//        @Callback(doc="function():number --  Returns the name of the.")
//        public Object[] getName(final Context context, final Arguments args) {
//            final IGridInterface grid = tileEntity.getGrid();
//            return grid != null
//                    ? new Object[]{grid.getName()}
//                    : new Object[]{null, "no grid"};
//        }

        @Callback(doc="function():number --  Returns the total amount of power available.")
        public Object[] getAvailablePower(final Context context, final Arguments args) {
            final IGridInterface grid = tileEntity.getGrid();
            return grid != null
                    ? new Object[]{grid.getAvailablePower()}
                    : new Object[]{null, "no grid"};
        }

        @Callback(doc="function():list --  Returns the list of jobs.")
        public Object[] getJobList(final Context context, final Arguments args) {
            final ArrayList<Map> results = new ArrayList<Map>();
            final List<ItemStack> jobs = Reflection.tryInvoke(tileEntity, "getJobList");
            if (jobs != null) {
                for (ItemStack stack : jobs) {
                    results.add(Registry.toMap(stack));
                }
            }
            return new Object[]{results.toArray()};
        }
    }
}
