package li.cil.oc.integration.ic2;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import ic2.api.crops.ICropTile;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverCrop extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ICropTile.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z, ForgeDirection side) {
        ICropTile tile = (ICropTile) world.getTileEntity(x, y, z);
        if (tile.getScanLevel() < 4)
            return new DriverCrop.DummyEnvironment((ICropTile) world.getTileEntity(x, y, z));
        else
            return new DriverCrop.Environment((ICropTile) world.getTileEntity(x, y, z));
    }
    public static final class DummyEnvironment extends ManagedTileEntityEnvironment<ICropTile> {
        public DummyEnvironment(final ICropTile tileEntity) {
            super(tileEntity, "crop");
        }

        @Callback
        public Object[] getScanLevel(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getScanLevel()};
        }
    }
    public static final class Environment extends ManagedTileEntityEnvironment<ICropTile> {
        public Environment(final ICropTile tileEntity) {
            super(tileEntity, "crop");
        }

        @Callback
        public Object[] getSize(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getSize()};
        }
        @Callback
        public Object[] getGrowth(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getGrowth()};
        }
        @Callback
        public Object[] getGain(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getGain()};
        }
        @Callback
        public Object[] getResistance(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getResistance()};
        }
        @Callback
        public Object[] getNutrientStorage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getNutrientStorage()};
        }
        @Callback
        public Object[] getHydrationStorage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getHydrationStorage()};
        }
        @Callback
        public Object[] getWeedExStorage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getWeedExStorage()};
        }
        @Callback
        public Object[] getHumidity(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getHumidity()};
        }
        @Callback
        public Object[] getNutrients(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getNutrients()};
        }
        @Callback
        public Object[] getAirQuality(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getAirQuality()};
        }
        @Callback
        public Object[] getName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().name()};
        }
        @Callback
        public Object[] getRootsLength(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().getrootslength(tileEntity)};
        }
        @Callback
        public Object[] getTier(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().tier()};
        }
        @Callback
        public Object[] maxSize(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().maxSize()};
        }
        @Callback
        public Object[] canGrow(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().canGrow(tileEntity)};
        }
        @Callback
        public Object[] getOptimalHavestSize(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCrop().getOptimalHavestSize(tileEntity)};
        }
    }
}
