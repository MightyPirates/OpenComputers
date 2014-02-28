package li.cil.occ.mods.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.world.World;

public final class DriverCommandBlock extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityCommandBlock.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityCommandBlock) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityCommandBlock> {
        public Environment(final TileEntityCommandBlock tileEntity) {
            super(tileEntity, "command_block");
        }

        @Callback(direct = true)
        public Object[] getCommand(final Context context, final Arguments args) {
            return new Object[]{tileEntity.func_145993_a().func_145753_i()};
        }

        @Callback
        public Object[] setCommand(final Context context, final Arguments args) {
            tileEntity.func_145993_a().func_145752_a(args.checkString(0));
            tileEntity.getWorldObj().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            return new Object[]{true};
        }

        @Callback
        public Object[] executeCommand(final Context context, final Arguments args) {
            context.pause(0.1); // Make sure the command block has time to do its thing.
            tileEntity.func_145993_a().func_145755_a(tileEntity.getWorldObj());
            return new Object[]{true};
        }
    }
}
