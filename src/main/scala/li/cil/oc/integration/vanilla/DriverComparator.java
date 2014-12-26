package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentAware;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class DriverComparator extends DriverTileEntity implements EnvironmentAware {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityComparator.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos) {
        return new Environment((TileEntityComparator) world.getTileEntity(pos));
    }

    @Override
    public Class<? extends li.cil.oc.api.network.Environment> providedEnvironment(ItemStack stack) {
        if (stack != null && stack.getItem() == Items.comparator)
            return Environment.class;
        return null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityComparator> implements NamedBlock {
        public Environment(final TileEntityComparator tileEntity) {
            super(tileEntity, "comparator");
        }

        @Override
        public String preferredName() {
            return "comparator";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
        public Object[] getOutputSignal(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputSignal()};
        }
    }
}
