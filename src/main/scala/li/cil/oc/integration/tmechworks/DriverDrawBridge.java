package li.cil.oc.integration.tmechworks;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import tmechworks.lib.blocks.IDrawbridgeLogicBase;

public class DriverDrawBridge extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IDrawbridgeLogicBase.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IDrawbridgeLogicBase) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IDrawbridgeLogicBase> {
        public Environment(final IDrawbridgeLogicBase tileEntity) {
            super(tileEntity, "drawbridge");
        }

        @Callback(doc = "function():boolean -- Whether the draw bridge is currently in its extended state or not.")
        public Object[] hasExtended(final Context context, final Arguments args) {
            return new Object[]{tileEntity.hasExtended()};
        }
    }
}
