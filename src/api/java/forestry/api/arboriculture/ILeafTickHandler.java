package forestry.api.arboriculture;

import net.minecraft.world.World;

public interface ILeafTickHandler {
	boolean onRandomLeafTick(ITree tree, World world, int biomeId, int x, int y, int z, boolean isDestroyed);
}
