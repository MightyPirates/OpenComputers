package li.cil.oc.driver.thermalexpansion;

import cofh.api.tileentity.ISecureTile;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import org.apache.commons.lang3.text.WordUtils;

public final class DriverSecureTile extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ISecureTile.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((ISecureTile) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ISecureTile> {
        public Environment(final ISecureTile tileEntity) {
            super(tileEntity, "secure_tile");
        }

        @Callback
        public Object[] canPlayerAccess(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canPlayerAccess(args.checkString(0))};
        }

        @Callback
        public Object[] getAccess(final Context context, final Arguments args) {
            return new Object[]{WordUtils.capitalize(tileEntity.getAccess().name())};
        }

        @Callback
        public Object[] getOwnerName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOwnerName()};
        }
    }
}
