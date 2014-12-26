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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class DriverMobSpawner extends DriverTileEntity implements EnvironmentAware {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMobSpawner.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos) {
        return new Environment((TileEntityMobSpawner) world.getTileEntity(pos));
    }

    @Override
    public Class<? extends li.cil.oc.api.network.Environment> providedEnvironment(ItemStack stack) {
        if (stack != null && Block.getBlockFromItem(stack.getItem()) == Blocks.mob_spawner)
            return Environment.class;
        return null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityMobSpawner> implements NamedBlock {
        public Environment(final TileEntityMobSpawner tileEntity) {
            super(tileEntity, "mob_spawner");
        }

        @Override
        public String preferredName() {
            return "mob_spawner";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(final Context context, final Arguments args) {
            final NBTTagCompound tag = new NBTTagCompound();
            tileEntity.writeToNBT(tag);
            return new Object[]{tag.getString("EntityId")};
        }
    }
}
