package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.tile.IBloodAltar;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class DriverBloodAltar extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IBloodAltar.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final EnumFacing side) {
        return new Environment((IBloodAltar) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IBloodAltar> implements NamedBlock {
        public Environment(final IBloodAltar tileEntity) {
            super(tileEntity, "blood_altar");
        }

        @Override
        public String preferredName() {
            return "blood_altar";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the capacity.")
        public Object[] getCapacity(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getCapacity()};
        }

        @Callback(doc = "function():number -- Get the amount of blood currently contained by this altar.")
        public Object[] getCurrentBlood(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getCurrentBlood()};
        }

        @Callback(doc = "function():number -- Get the current tier.")
        public Object[] getTier(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getTier()};
        }

        @Callback(doc = "function():number -- Get the progress.")
        public Object[] getProgress(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getProgress()};
        }

        @Callback(doc = "function():number -- Get the sacrifice multiplier.")
        public Object[] getSacrificeMultiplier(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getCapacity()};
        }

        @Callback(doc = "function():number -- Get the self sacrifice multiplier.")
        public Object[] getSelfSacrificeMultiplier(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getSelfSacrificeMultiplier()};
        }

        @Callback(doc = "function():number -- Get the orb multiplier.")
        public Object[] getOrbMultiplier(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getOrbMultiplier()};
        }

        @Callback(doc = "function():number -- Get the dislocation multiplier.")
        public Object[] getDislocationMultiplier(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getDislocationMultiplier()};
        }
    }
}
