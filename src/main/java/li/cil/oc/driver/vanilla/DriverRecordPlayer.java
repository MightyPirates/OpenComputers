package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverBlock;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.block.Block;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityRecordPlayer;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public final class DriverRecordPlayer extends DriverBlock {
    DriverRecordPlayer() {
        super(new ItemStack(Block.jukebox, 1, OreDictionary.WILDCARD_VALUE));
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityRecordPlayer) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityRecordPlayer> {
        public Environment(final TileEntityRecordPlayer tileEntity) {
            super(tileEntity, "jukebox");
        }

        @Callback
        public Object[] getRecord(final Context context, final Arguments args) {
            final ItemStack record = tileEntity.func_96097_a();
            if (record == null || !(record.getItem() instanceof ItemRecord)) {
                return null;
            }
            return new Object[]{((ItemRecord) record.getItem()).getRecordTitle()};
        }
    }
}
