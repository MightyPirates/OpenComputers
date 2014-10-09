package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.block.BlockJukebox;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class DriverRecordPlayer extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return BlockJukebox.TileEntityJukebox.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((BlockJukebox.TileEntityJukebox) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BlockJukebox.TileEntityJukebox> implements NamedBlock {
        public Environment(final BlockJukebox.TileEntityJukebox tileEntity) {
            super(tileEntity, "jukebox");
        }

        @Override
        public String preferredName() {
            return "jukebox";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the title of the record currently in the juke box.")
        public Object[] getRecord(final Context context, final Arguments args) {
            final ItemStack record = tileEntity.func_145856_a();
            if (record == null || !(record.getItem() instanceof ItemRecord)) {
                return null;
            }
            return new Object[]{((ItemRecord) record.getItem()).getRecordNameLocal()};
        }
    }
}
