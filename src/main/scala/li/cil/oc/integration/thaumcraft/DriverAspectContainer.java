package li.cil.oc.integration.thaumcraft;

import com.google.common.base.Preconditions;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

public class DriverAspectContainer extends DriverTileEntity {
    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((IAspectContainer) world.getTileEntity(pos));
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
