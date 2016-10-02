package li.cil.oc.integration.enderstorage;

import codechicken.enderstorage.tile.TileFrequencyOwner;
import codechicken.enderstorage.tile.TileEnderTank;
import codechicken.enderstorage.api.Frequency;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public final class DriverFrequencyOwner extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileFrequencyOwner.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((TileFrequencyOwner) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileFrequencyOwner> implements NamedBlock {
        public Environment(final TileFrequencyOwner tileEntity) {
            super(tileEntity, tileEntity instanceof TileEnderTank ? "ender_tank" : "ender_chest");
        }

        @Override
        public String preferredName() {
            return tileEntity instanceof TileEnderTank ? "ender_tank" : "ender_chest";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():table -- Get the currently set frequency. {left, middle, right}")
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.frequency.toArray()};
        }

        @Callback(doc = "function(left:number, middle:number, right:number) -- Set the frequency. Range 0-15 (inclusive).")
        public Object[] setFrequency(final Context context, final Arguments args) {
            final int left;
            final int middle;
            final int right;
            if (args.count() == 1){
                final int freq = args.checkInteger(0);
                if ((freq & 0xFFF) != freq) {
                    throw new IllegalArgumentException("invalid frequency");
                }
                left = (freq >> 8) & 0xF;
                middle = (freq >> 4) & 0xF;
                right = freq & 0xF;
            } else {
                left = args.checkInteger(0);
                middle = args.checkInteger(1);
                right = args.checkInteger(2);
                if ((left & 0xF) != left || (middle & 0xF) != middle || (right & 0xF) != right) {
                    throw new IllegalArgumentException("invalid frequency");
                }
            }
            final String owner = tileEntity.frequency.owner;
            if (owner == null || owner.isEmpty() || "global".equals(owner)) {
                tileEntity.setFreq(new Frequency(left,middle,right));
            } else {
                return new Object[]{null, "cannot change frequency of owned storage"};
            }
            return null;
        }

        @Callback(doc = "function():string -- Get the name of the owner, which is usually a player's name or 'global'.")
        public Object[] getOwner(final Context context, final Arguments args) {
            return new Object[]{tileEntity.frequency.owner};
        }

        @Callback(doc = "function():table -- Get the currently set frequency as a table of color names.")
        public Object[] getFrequencyColors(final Context context, final Arguments args){
            return new Object[]{ tileEntity.frequency.getColours() };
        }

        @Callback(doc = "function():table -- Get a table with the mapping of colours (as Minecraft names) to Frequency numbers. NB: Frequencies are zero based!")
        public Object[] getColors(final Context context, final Arguments args){
            String[] colours = new String[Frequency.colours.length];
            for (int i = 0; i < Frequency.colours.length; i++){
                colours[i] = Frequency.colours[i].getMinecraftName();
            }
            return new Object[]{colours};
        }
    }
}
