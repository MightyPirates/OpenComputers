package appeng.api.me.tiles;

import net.minecraftforge.common.ForgeDirection;

/**
 * Please note that none of these methods will cause a block update,
 * if you use set ori/spin/cycle, must mark the block for update if it was already sent to the client.
 */
public interface IOrientableTile {
	
	// returns the main direction for rotation.
	ForgeDirection getPrimaryOrientation();
	
	// used for up or down orientation to spin the tile around, 0 - 3 clockwise ( probobly )
	int getSpin();
	
	// returns the main direction for rotation.
	void setPrimaryOrientation( ForgeDirection s );
	
	// used for up or down orientation.
	void setSpin( int spin );
	
	// acts like a wrench was used on it, if a wrench does nothing, this does nothing.
	void cycleOrientation();
	
}
