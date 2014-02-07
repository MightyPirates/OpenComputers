package li.cil.oc.driver.appeng;

import appeng.api.me.tiles.IGridTileEntity;
import cpw.mods.fml.relauncher.ReflectionHelper;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.Registry;
import li.cil.oc.driver.TileEntityDriver;
import li.cil.oc.util.Reflection;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lordjoda on 07.02.14.
 */
public class DriverTileController extends TileEntityDriver {


    private static final Class<?> TILECONTROLLER = Reflection.getClass("appeng.me.tile.TileController");


    @Override
    public Class<?> getFilterClass() {
        return TILECONTROLLER;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment {
        public Environment(TileEntity tileEntity) {
            super(tileEntity, "gridtileEntity");
        }

        @Callback
        public Object[] getJobList(final Context context, final Arguments args) {
            try {
                ArrayList<Map> maps = new ArrayList<Map>();
                for (ItemStack stack : (List<ItemStack>) Reflection.invoke(tileEntity, "getJobList")) {
                    maps.add(Registry.toMap(stack));
                }
                return maps.toArray();
            } catch (Throwable ex) {

            }
            return new Object[]{null, "Unknown Error"};
        }
    }
}
