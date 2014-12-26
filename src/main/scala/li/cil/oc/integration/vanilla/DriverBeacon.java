package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentAware;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class DriverBeacon extends DriverTileEntity implements EnvironmentAware {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBeacon.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos) {
        return new Environment((TileEntityBeacon) world.getTileEntity(pos));
    }

    @Override
    public Class<? extends li.cil.oc.api.network.Environment> providedEnvironment(ItemStack stack) {
        if (stack != null && Block.getBlockFromItem(stack.getItem()) == Blocks.beacon)
            return Environment.class;
        return null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityBeacon> implements NamedBlock {
        public Environment(final TileEntityBeacon tileEntity) {
            super(tileEntity, "beacon");
        }

        @Override
        public String preferredName() {
            return "beacon";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the number of levels for this beacon.")
        public Object[] getLevels(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getField(0)};
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(final Context context, final Arguments args) {
            return new Object[]{getEffectName(tileEntity.getField(1))};
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(final Context context, final Arguments args) {
            return new Object[]{getEffectName(tileEntity.getField(2))};
        }
    }

    private static String getEffectName(final int id) {
        return (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null)
                ? Potion.potionTypes[id].getName()
                : null;
    }
}
