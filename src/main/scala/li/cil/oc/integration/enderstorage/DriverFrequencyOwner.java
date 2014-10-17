package li.cil.oc.integration.enderstorage;

import codechicken.enderstorage.common.TileFrequencyOwner;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverFrequencyOwner extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileFrequencyOwner.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileFrequencyOwner) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileFrequencyOwner> {
        public Environment(final TileFrequencyOwner tileEntity) {
            super(tileEntity, "ender_storage");
        }

        @Callback(doc = "function():number -- Get the currently set frequency.")
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.freq};
        }

        @Callback(doc = "function(value:number) -- Set the frequency. Who would have thought?!")
        public Object[] setFrequency(final Context context, final Arguments args) {
            final int frequency = args.checkInteger(0);
            if ((frequency & 0xFFF) != frequency) {
                throw new IllegalArgumentException("invalid frequency");
            }
            final String owner = tileEntity.owner;
            if (owner == null || owner.isEmpty() || "global".equals(owner)) {
                tileEntity.setFreq(frequency);
            } else {
                return new Object[]{null, "cannot change frequency of owned storage"};
            }
            return null;
        }

        @Callback(doc = "function():string -- Get the name of the owner, which is usually a player's name or 'global'.")
        public Object[] getOwner(final Context context, final Arguments args) {
            return new Object[]{tileEntity.owner};
        }
    }
}
