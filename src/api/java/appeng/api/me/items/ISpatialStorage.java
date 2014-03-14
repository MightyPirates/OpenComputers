package appeng.api.me.items;

import appeng.api.WorldCoord;
import appeng.api.me.util.TransitionResult;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ISpatialStorage {
	
	boolean isSpatialStorage( ItemStack is );
	
	int getMaxStoredDim( ItemStack is );
	
	World getWorld( ItemStack is );
	
	WorldCoord getStoredSize( ItemStack is );
	
	WorldCoord getMin( ItemStack is );
	
	WorldCoord getMax( ItemStack is );
	
	TransitionResult doSpatialTransition( ItemStack is, World w, WorldCoord min, WorldCoord max, boolean doTransition );

	World createNewWorld(ItemStack is);
	
}
