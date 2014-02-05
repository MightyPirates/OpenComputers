package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.util.TileEntityLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import java.util.HashMap;
import java.util.Map;

public class DriverFluidTank implements li.cil.oc.api.driver.Block {
    @Override
    public boolean worksWith(final World world, final ItemStack stack) {
        final Class clazz = TileEntityLookup.get(world, stack);
        return clazz != null && IFluidTank.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null && tileEntity instanceof IFluidTank;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IFluidTank) world.getBlockTileEntity(x, y, z));
    }

    public class Environment extends ManagedTileEntityEnvironment<IFluidTank> {
        public Environment(final IFluidTank tileEntity) {
            super(tileEntity, "fluid_tank");
        }

        @Callback
        public Object[] getInfo(final Context context, final Arguments args) {
            final FluidTankInfo info = tileEntity.getInfo();
            return new Object[]{convertInfo(info)};
        }
    }

    public static Map convertInfo(final FluidTankInfo info) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("capacity", info.capacity);
        if (info.fluid != null) {
            map.put("amount", info.fluid.amount);
            map.put("id", info.fluid.fluidID);
            final Fluid fluid = info.fluid.getFluid();
            if (fluid != null) {
                map.put("name", fluid.getName());
                map.put("label", fluid.getLocalizedName());
            }
        } else {
            map.put("amount", 0);
        }
        return map;
    }
}