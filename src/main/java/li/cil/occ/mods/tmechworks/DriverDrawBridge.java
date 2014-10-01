package li.cil.occ.mods.tmechworks;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import tmechworks.lib.blocks.IDrawbridgeLogicBase;

public class DriverDrawBridge extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IDrawbridgeLogicBase.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IDrawbridgeLogicBase) world.getBlockTileEntity(x, y, z));
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
