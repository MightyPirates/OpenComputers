package li.cil.oc.api.driver;

import net.minecraft.world.World;

/**
 * This interface can be implemented by drivers to enforce a method whitelist.
 * <p/>
 * When drivers are collected for a block, they are combined, resulting in the
 * block's component containing the list of methods from all drivers that apply
 * to the block.
 * <p/>
 * In some scenarios you may not want this to happen. Instead, only a select
 * list of methods should be shown for a block - for example, you may want to
 * suppress inventory functionality if your TileEntity implements IInventory.
 * <p/>
 * To do so, implement this interface and provide the names of the allowed
 * methods from {@link #whitelistedMethods(World, int, int, int)}.
 * <p/>
 * <em>Important</em>: if multiple drivers apply to a single block that each
 * provide a whitelist, the list of allowed methods is the intersection of the
 * two whitelists!
 */
public interface MethodWhitelist {
    /**
     * The list of methods allowed to be exposed for blocks this driver is used
     * for. Note that the names must <em>exactly</em> match the names of the
     * methods they allow.
     *
     * @param world the world containing the block to get the whitelist for.
     * @param x     the X coordinate of the block to get the whitelist for.
     * @param y     the Y coordinate of the block to get the whitelist for.
     * @param z     the Z coordinate of the block to get the whitelist for.
     */
    String[] whitelistedMethods(World world, int x, int y, int z);
}
