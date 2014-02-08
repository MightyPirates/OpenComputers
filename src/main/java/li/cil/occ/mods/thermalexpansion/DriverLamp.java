package li.cil.occ.mods.thermalexpansion;


import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class DriverLamp extends DriverTileEntity {
    private static final Class<?> TileLamp = Reflection.getClass("thermalexpansion.block.lamp.TileLamp");

    @Override
    public Class<?> getTileEntityClass() {
        return TileLamp;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "Lamp");
        }

        @Callback
        public Object[] setColor(final Context context, final Arguments args) {
            try {
                return new Object[]{Reflection.invoke(tileEntity, "setColor", args.checkInteger(0))};
            } catch (Throwable t) {
                return new Object[]{null, "Error"};
            }
        }

    }
}
