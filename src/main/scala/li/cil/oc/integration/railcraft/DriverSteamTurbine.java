package li.cil.oc.integration.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import mods.railcraft.common.blocks.machine.alpha.TileSteamTurbine;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverSteamTurbine extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileSteamTurbine.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((TileSteamTurbine) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileSteamTurbine> implements NamedBlock {
        public Environment(final TileSteamTurbine tileEntity) {
            super(tileEntity, "steam_turbine");
        }

        @Override
        public String preferredName() {
            return "steam_turbine";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number --  Returns the output of the steam turbine")
        public Object[] getTurbineOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutput()};
        }

        @Callback(doc = "function():number -- Returns the durability of the rotor in percent.")
        public Object[] getTurbineRotorStatus(final Context context, final Arguments args) {
            final IInventory inventory = tileEntity.getInventory();
            if (inventory != null && inventory.getSizeInventory() > 0) {
                final ItemStack itemStack = inventory.getStackInSlot(0);
                if (itemStack != null) {
                    return new Object[]{100 - (int) (itemStack.getItemDamage() * 100.0 / itemStack.getMaxDamage())};
                }
            }
            return new Object[]{0};
        }
    }
}
