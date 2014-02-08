package li.cil.occ.mods.enderstorage;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class DriverFrequencyOwner extends DriverTileEntity {
    // See https://bitbucket.org/ChickenBones/enderstorage/
    private static final Class<?> TileFrequencyOwner = Reflection.getClass("codechicken.enderstorage.common.TileFrequencyOwner");

    @Override
    public Class<?> getTileEntityClass() {
        return TileFrequencyOwner;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "ender_storage");
        }

        @Callback
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{Reflection.get(tileEntity, "freq")};
        }

        @Callback
        public Object[] setFrequency(final Context context, final Arguments args) {
            final int frequency = args.checkInteger(0);
            if ((frequency & 0xFFF) != frequency) {
                throw new IllegalArgumentException("invalid frequency");
            }
            final String owner = (String) Reflection.get(tileEntity, "owner");
            if (owner == null || owner.isEmpty() || "global".equals(owner)) {
                Reflection.tryInvoke(tileEntity, "setFreq", frequency);
            } else {
                return new Object[]{false, "cannot change frequency of owned storage"};
            }
            return null;
        }

        @Callback
        public Object[] getOwner(final Context context, final Arguments args) {
            return new Object[]{Reflection.get(tileEntity, "owner")};
        }
    }
}
