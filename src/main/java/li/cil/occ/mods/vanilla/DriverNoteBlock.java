package li.cil.occ.mods.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.world.World;

public final class DriverNoteBlock extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityNote.class;
    }

    @Override
    public String preferredName() {
        return "note_block";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityNote) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityNote> {
        public Environment(final TileEntityNote tileEntity) {
            super(tileEntity, "note_block");
        }

        @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
        public Object[] getPitch(final Context context, final Arguments args) {
            return new Object[]{tileEntity.note + 1};
        }

        @Callback(doc = "function(value:number) -- Set the pitch for this note block. Must be in the interval [1, 25].")
        public Object[] setPitch(final Context context, final Arguments args) {
            setPitch(args.checkInteger(0));
            return new Object[]{true};
        }

        @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch for to save a tick.")
        public Object[] trigger(final Context context, final Arguments args) {
            if (args.count() > 0 && args.checkAny(0) != null) {
                setPitch(args.checkInteger(0));
            }

            final World world = tileEntity.getWorldObj();
            final int x = tileEntity.xCoord;
            final int y = tileEntity.yCoord;
            final int z = tileEntity.zCoord;
            final Material material = world.getBlock(x, y + 1, z).getMaterial();
            final boolean canTrigger = material == Material.air;

            tileEntity.triggerNote(world, x, y, z);
            return new Object[]{canTrigger};
        }

        private void setPitch(final int value) {
            if (value < 1 || value > 25) {
                throw new IllegalArgumentException("invalid pitch");
            }
            tileEntity.note = (byte) (value - 1);
            tileEntity.markDirty();
        }
    }
}
