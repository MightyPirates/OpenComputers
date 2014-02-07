package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.world.World;

public final class DriverCommandBlock extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityCommandBlock.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityCommandBlock) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityCommandBlock> {
        public Environment(final TileEntityCommandBlock tileEntity) {
            super(tileEntity, "command_block");
        }

        @Callback(direct = true)
        public Object[] getCommand(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCommand()};
        }

        @Callback
        public Object[] setCommand(final Context context, final Arguments args) {
            tileEntity.setCommand(args.checkString(0));
            tileEntity.getWorldObj().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            return new Object[]{true};
        }

        @Callback
        public Object[] executeCommand(final Context context, final Arguments args) {
            context.pause(0.1); // Make sure the command block has time to do its thing.
            return new Object[]{tileEntity.executeCommandOnPowered(tileEntity.getWorldObj())};
        }
    }
}
