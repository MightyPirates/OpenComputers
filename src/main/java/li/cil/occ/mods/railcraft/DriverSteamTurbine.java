package li.cil.occ.mods.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class DriverSteamTurbine extends DriverTileEntity implements NamedBlock {
    private static final Class<?> TileSteamTurbine = Reflection.getClass("mods.railcraft.common.blocks.machine.alpha.TileSteamTurbine");

    @Override
    public Class<?> getTileEntityClass() {
        return TileSteamTurbine;
    }

    @Override
    public String preferredName() {
        return "steam_turbine";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "steam_turbine");
        }

        @Callback(doc = "function():number --  Returns the output of the steam turbine")
        public Object[] getTurbineOutput(final Context context, final Arguments args) {
            return new Object[]{Reflection.tryInvoke(tileEntity, "getOutput")};
        }

        @Callback(doc = "function():number --  Returns the durability of the rotor in percent.")
        public Object[] getTurbineRotorStatus(final Context context, final Arguments args) {
            IInventory inventory = Reflection.tryInvoke(tileEntity, "getInventory");
            if (inventory != null && inventory.getSizeInventory() > 0) {
                final ItemStack itemStack = inventory.getStackInSlot(0);
                if (itemStack != null) {
                    return new Object[]{100 - (int) (itemStack.getItemDamage() * 100.0 / itemStack.getMaxDamage())};
                }
            }
            return new Object[]{null, "no Inventory"};
        }
    }
}
