package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverBlock;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public final class DriverBrewingStand extends DriverBlock {
    DriverBrewingStand() {
        super(new ItemStack(Item.brewingStand),
                new ItemStack(Block.brewingStand, 1, OreDictionary.WILDCARD_VALUE));
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityBrewingStand) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityBrewingStand> {
        public Environment(final TileEntityBrewingStand tileEntity) {
            super(tileEntity, "brewing_stand");
        }

        @Callback
        public Object[] getBrewTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getBrewTime()};
        }
    }
}
