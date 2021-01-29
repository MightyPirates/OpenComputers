package li.cil.oc.integration.ic2;

import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class DriverTeleporter extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityTeleporter.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
        return new DriverTeleporter.Environment((TileEntityTeleporter)world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityTeleporter> {
        public Environment(final TileEntityTeleporter tileEntity) {
            super(tileEntity,"ic2_teleporter");
        }
        @Callback(doc = "function(X:number, Y:number, Z:number)")
        public Object[] setCoords(final Context context, final Arguments args) {
            if (args.isInteger(0) && args.isInteger(1) && args.isInteger(2)) {
                tileEntity.setTarget(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            }
            return new Object[]{};
        }
    }

}
