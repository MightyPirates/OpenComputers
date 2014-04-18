package appeng.api.parts;

import net.minecraftforge.common.util.ForgeDirection;

public interface IPartCollsionHelper
{

	/**
	 * add a collision box, expects 0.0 - 16.0 block coords.
	 * 
	 * No complaints about the size, I like using pixels :P
	 * 
	 * @param minX
	 * @param minY
	 * @param minZ
	 * @param maxX
	 * @param maxY
	 * @param maxZ
	 */
	void addBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

	/**
	 * @return east in world space.
	 */
	ForgeDirection getWorldX();

	/**
	 * @return up in world space.
	 */
	ForgeDirection getWorldY();

	/**
	 * @return forward in world space.
	 */
	ForgeDirection getWorldZ();

	/**
	 * @return true if this test is to get the BB Collision information.
	 */
	boolean isBBCollision();

}
