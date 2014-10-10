package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
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

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityCommandBlock> implements NamedBlock {
        public Environment(final TileEntityCommandBlock tileEntity) {
            super(tileEntity, "command_block");
        }

        @Override
        public String preferredName() {
            return "command_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
        public Object[] getCommand(final Context context, final Arguments args) {
            return new Object[]{tileEntity.func_145993_a().func_145753_i()};
        }

        @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
        public Object[] setCommand(final Context context, final Arguments args) {
            tileEntity.func_145993_a().func_145752_a(args.checkString(0));
            tileEntity.getWorldObj().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            return new Object[]{true};
        }

        @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
        public Object[] executeCommand(final Context context, final Arguments args) {
            context.pause(0.1); // Make sure the command block has time to do its thing.
            tileEntity.func_145993_a().func_145755_a(tileEntity.getWorldObj());
            return new Object[]{true};
        }
    }
}
