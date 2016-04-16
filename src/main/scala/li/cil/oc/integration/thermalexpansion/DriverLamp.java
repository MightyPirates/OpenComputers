package li.cil.oc.integration.thermalexpansion;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import li.cil.oc.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverLamp extends DriverSidedTileEntity {
    private static final Class<?> TileLamp = Reflection.getClass("thermalexpansion.block.lamp.TileLamp");

    @Override
    public Class<?> getTileEntityClass() {
        return TileLamp;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment(world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "lamp");
        }

        @Callback(doc = "function(color:number):boolean --  Sets the color of the lamp to the given color.")
        public Object[] setColor(final Context context, final Arguments args) {
            return new Object[]{Reflection.tryInvoke(tileEntity, "setColor", args.checkInteger(0))};
        }
    }
}
