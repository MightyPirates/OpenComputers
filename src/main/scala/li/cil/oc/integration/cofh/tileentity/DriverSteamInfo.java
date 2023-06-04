package li.cil.oc.integration.cofh.tileentity;

import cofh.api.tileentity.IEnergyInfo;
import cofh.api.tileentity.ISteamInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DriverSteamInfo extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ISteamInfo.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((ISteamInfo) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ISteamInfo> {
        public Environment(final ISteamInfo tileEntity) {
            super(tileEntity, "steam_info");
        }

        @Callback(doc = "function():number --  Returns the steam per tick.")
        public Object[] getSteamPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoSteamPerTick()};
        }

        @Callback(doc = "function():number --  Returns the maximum steam per tick.")
        public Object[] getMaxSteamPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoMaxSteamPerTick()};
        }
    }
}
