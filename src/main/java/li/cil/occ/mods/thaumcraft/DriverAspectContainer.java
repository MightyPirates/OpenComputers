package li.cil.occ.mods.thaumcraft;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

import com.google.common.base.Preconditions;

public class DriverAspectContainer extends DriverTileEntity{
	
	@Override
	public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
		return new Environment((IAspectContainer) world.getBlockTileEntity(x, y, z));
	}
	
	@Override
	public Class<?> getTileEntityClass() {
		return IAspectContainer.class;
	}
	
	public static final class Environment extends ManagedTileEntityEnvironment<IAspectContainer> {
		public Environment(IAspectContainer tileEntity) {
			super(tileEntity, "aspect_container");
			
		}

		@Callback(doc="function():table -- Get the Aspects stored in the block")
		public Object[] getAspects(final Context context, final Arguments args) {
			return new Object[]{tileEntity};
		}

		@Callback(doc="function(aspect:string):number -- Get amount of specific aspect stored in this block")
		public Object[] getAspectCount(final Context context, final Arguments args) {
			Aspect aspect = Aspect.getAspect(args.checkString(0).toLowerCase());
			Preconditions.checkNotNull(aspect, "Invalid aspect name");
			AspectList list = tileEntity.getAspects();
			return new Object[]{list.getAmount(aspect)};
		}
	}
}
