package stargatetech2.api.stargate;

import net.minecraft.world.World;

public interface IStargatePlacer {
	/**
	 * Attempts to place a Stargate in the givel location.
	 * 
	 * @param w Our world.
	 * @param x The stargate base's (bottom center block) X coord.
	 * @param y The stargate base's (bottom center block) Y coord.
	 * @param z The stargate base's (bottom center block) Z coord.
	 * @param facing The direction the stargate should be facing. Should be a value in [0 - 3].
	 * @return Whether the Stargate was placed or not.
	 */
	public boolean placeStargate(World w, int x, int y, int z, int facing);
}
