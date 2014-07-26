package li.cil.occ.mods.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class DriverBoilerFirebox extends DriverTileEntity implements NamedBlock {
    private static final Class<?> TileBoilerFirebox = Reflection.getClass("mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox");

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getTileEntity(x, y, z));
    }

    @Override
    public String preferredName() {
        return "boiler_firebox";
    }

    @Override
    public Class<?> getTileEntityClass() {
        return TileBoilerFirebox;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {

        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "boiler_firebox");
        }

        @Callback(doc = "function():boolean -- Get whether the boiler is active or not")
        public Object[] isBurning(final Context context, final Arguments args) {
            return new Object[]{Reflection.tryInvoke(tileEntity, "isBurning")};
        }

        @Callback(doc = "function():number -- Get the temperature of the boiler")
        public Object[] getTemperature(final Context context, final Arguments args) {
            return new Object[]{Reflection.tryInvoke(tileEntity, "getTemperature")};
        }

    }
}
