package forestry.api.farming;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * IGermling describes a crop or other harvestable object and can be used to inspect item stacks and blocks for matches.
 */
public interface IFarmable {

	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the block at the given location is a "sapling" for this type, i.e. a non-harvestable immature version of the crop.
	 */
	boolean isSaplingAt(World world, int x, int y, int z);

	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return {@link ICrop} if the block at the given location is a harvestable and mature crop, null otherwise.
	 */
	ICrop getCropAt(World world, int x, int y, int z);

	/**
	 * @param itemstack
	 * @return true if the item is a valid germling (plantable sapling, seed, etc.) for this type.
	 */
	boolean isGermling(ItemStack itemstack);

	/**
	 * @param itemstack
	 * @return true if the item is something that can drop from this type without actually being harvested as a crop. (Apples or sapling from decaying leaves.)
	 */
	boolean isWindfall(ItemStack itemstack);

	/**
	 * Plants a sapling by manipulating the world. The {@link IFarmLogic} should have verified the given location as valid. Called by the {@link IFarmHousing}
	 * which handles resources.
	 * 
	 * @param germling
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return true on success, false otherwise.
	 */
	boolean plantSaplingAt(ItemStack germling, World world, int x, int y, int z);

}
