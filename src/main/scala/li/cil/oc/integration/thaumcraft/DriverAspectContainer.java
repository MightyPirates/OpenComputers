package li.cil.oc.integration.thaumcraft;

import com.google.common.base.Preconditions;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

public class DriverAspectContainer extends DriverSidedTileEntity {
    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IAspectContainer) world.getTileEntity(x, y, z));
    }

    @Override
    public Class<?> getTileEntityClass() {
        return IAspectContainer.class;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IAspectContainer> {
        public Environment(final IAspectContainer tileEntity) {
            super(tileEntity, "aspect_container");
        }

        @Callback(doc = "function():table -- Get the Aspects stored in the block")
        public Object[] getAspects(final Context context, final Arguments args) {
            return new Object[]{tileEntity};
        }

        @Callback(doc = "function(aspect:string):number -- Get amount of specific aspect stored in this block")
        public Object[] getAspectCount(final Context context, final Arguments args) {
            final Aspect aspect = Aspect.getAspect(args.checkString(0).toLowerCase());
            Preconditions.checkNotNull(aspect, "Invalid aspect name");
            final AspectList list = tileEntity.getAspects();
            return new Object[]{list.getAmount(aspect)};
        }
    }
}
