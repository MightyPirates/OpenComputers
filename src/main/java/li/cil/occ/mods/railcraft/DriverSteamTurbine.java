package li.cil.occ.mods.railcraft;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;


public class DriverSteamTurbine extends DriverTileEntity {
    // See https://bitbucket.org/ChickenBones/enderstorage/
    private static final Class<?> TileSteamTurbine = Reflection.getClass("mods.railcraft.common.blocks.machine.alpha.TileSteamTurbine");

    @Override
    public Class<?> getTileEntityClass() {
        return TileSteamTurbine;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "steam_turbine");
        }

        @Callback
        public Object[] getTurbineOutput(final Context context, final Arguments args) {
            return new Object[]{Reflection.tryInvoke(tileEntity, "getOutput")};
        }

        @Callback
        public Object[] getTurbineRotorStatus(final Context context, final Arguments args) {


            IInventory inventory = (IInventory) Reflection.tryInvoke(tileEntity, "getInventory");

            if (inventory != null && inventory.getSizeInventory() >= 1) {
                ItemStack itemStack = inventory.getStackInSlot(0);
                if (itemStack != null) {
                    Item item = itemStack.getItem();
                    if (item != null) {
                        return new Object[]{100 - (int) (itemStack.getItemDamage() * 100.0 / item.getMaxDamage())};
                    }
                }
            }
            return new Object[]{null, "no Inventory"};

        }
    }
}
