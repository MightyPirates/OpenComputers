package ic2.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Represents the 6 possible directions along the axis of a block.
 */
public enum Direction {
	/**
	 * -X
	 */
	XN(0),
	/**
	 * +X
	 */
	XP(1),

	/**
	 * -Y
	 */
	YN(2), //MC-Code starts with 0 here
	/**
	 * +Y
	 */
	YP(3), // 1...

	/**
	 * -Z
	 */
	ZN(4),
	/**
	 * +Z
	 */
	ZP(5);

	Direction(int dir1) {
		this.dir = dir1;
	}

	/*public CoordinateTuple ApplyToCoordinates(CoordinateTuple coordinates) {
		CoordinateTuple ret = new CoordinateTuple(coordinates);

		ret.coords[dir/2] += GetSign();

		return ret;
	}*/

	/**
	 * Get the tile entity next to a tile entity following this direction.
	 *
	 * @param tileEntity tile entity to check
	 * @return Adjacent tile entity or null if none exists
	 */
	public TileEntity applyToTileEntity(TileEntity tileEntity) {
		int coords[] = { tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord };

		coords[dir/2] += getSign();
		World world = tileEntity.getWorldObj();

		if (world != null && world.blockExists(coords[0], coords[1], coords[2])) {
			try {
				return world.getTileEntity(coords[0], coords[1], coords[2]);
			} catch (Exception e) {
				throw new RuntimeException("error getting TileEntity at dim "+world.provider.dimensionId+" "+coords[0]+"/"+coords[1]+"/"+coords[2]);
			}
		}
		return null;
	}

	/**
	 * Get the inverse of this direction (XN -> XP, XP -> XN, etc.)
	 * 
	 * @return Inverse direction
	 */
	public Direction getInverse() {
		int inverseDir = dir - getSign();

		for (Direction direction : directions) {
			if (direction.dir == inverseDir) return direction;
		}

		return this;
	}

	/**
	 * Convert this direction to a Minecraft side value.
	 * 
	 * @return Minecraft side value
	 */
	public int toSideValue() {
		return (dir + 4) % 6;
	}

	/**
	 * Determine direction sign (N for negative or P for positive).
	 *
	 * @return -1 if the direction is negative, +1 if the direction is positive
	 */
	private int getSign() {
		return (dir % 2) * 2 - 1;
	}

	public ForgeDirection toForgeDirection() {
		return ForgeDirection.getOrientation(toSideValue());
	}

	private int dir;
	public static final Direction[] directions = Direction.values();
}

