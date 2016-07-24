package li.cil.oc.integration.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class DriverBoilerFirebox extends DriverSidedTileEntity {
    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final EnumFacing side) {
        return new Environment((TileBoilerFirebox) world.getTileEntity(x, y, z));
    }

    @Override
    public Class<?> getTileEntityClass() {
        return TileBoilerFirebox.class;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileBoilerFirebox> implements NamedBlock {
        public Environment(final TileBoilerFirebox tileEntity) {
            super(tileEntity, "boiler_firebox");
        }

        @Override
        public String preferredName() {
            return "boiler_firebox";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Get whether the boiler is active or not.")
        public Object[] isBurning(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isBurning()};
        }

        @Callback(doc = "function():number -- Get the temperature of the boiler.")
        public Object[] getTemperature(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getTemperature()};
        }

        @Callback(doc = "function():number -- Get the maximum temperature of the boiler.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxHeat()};
        }
    }
}
