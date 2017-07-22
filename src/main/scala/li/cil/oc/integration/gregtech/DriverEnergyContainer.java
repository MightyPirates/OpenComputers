package li.cil.oc.integration.gregtech;

import gregtech.api.interfaces.tileentity.IBasicEnergyContainer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnergyContainer extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IBasicEnergyContainer.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IBasicEnergyContainer) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IBasicEnergyContainer> {
        public Environment(final IBasicEnergyContainer tileEntity) {
            super(tileEntity, "gt_energyContainer");
        }

        @Callback(doc = "function():number --  Returns the amount of electricity contained in this Block, in EU units!")
        public Object[] getStoredEU(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getStoredEU()};
        }

        @Callback(doc = "function():number --  Returns the amount of electricity containable in this Block, in EU units!")
        public Object[] getEUCapacity(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getEUCapacity()};
        }

        @Callback(doc = "function():number --  Returns the amount of Steam containable in this Block, in EU units!")
        public Object[] getSteamCapacity(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getSteamCapacity()};
        }

        @Callback(doc = "function():number --  Returns the amount of Steam contained in this Block, in EU units!")
        public Object[] getStoredSteam(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getStoredSteam()};
        }

        @Callback(doc = "function():number --  Gets the Output in EU/p.")
        public Object[] getOutputVoltage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputVoltage()};
        }

        @Callback(doc = "function():number -- Gets the amount of Energy Packets per tick.")
        public Object[] getOutputAmperage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputAmperage()};
        }

        @Callback(doc = "function():number -- Gets the maximum Input in EU/p.")
        public Object[] getInputVoltage(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInputVoltage()};
        }

        @Callback(doc = "function():number -- Returns the amount of Electricity, accepted by this Block the last 5 ticks as Average.")
        public Object[] getAverageElectricInput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getAverageElectricInput()};
        }

        @Callback(doc = "function():number -- Returns the amount of Electricity, outputted by this Block the last 5 ticks as Average.")
        public Object[] getAverageElectricOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getAverageElectricOutput()};
        }
    }
}
