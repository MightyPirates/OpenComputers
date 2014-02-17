package li.cil.occ.mods.appeng;

import appeng.api.me.tiles.IGridTileEntity;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverGridTileEntity extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IGridTileEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IGridTileEntity) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IGridTileEntity> {
        public Environment(final IGridTileEntity tileEntity) {
            super(tileEntity, "me_grid_tile");
        }

        @Callback(doc="function():boolean -- Returns if the component is powered")
        public Object[] isPowered(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isPowered()};
        }
    }
}
