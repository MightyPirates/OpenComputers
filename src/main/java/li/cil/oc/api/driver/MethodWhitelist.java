package li.cil.oc.api.driver;

/**
 * This interface can be implemented by environments to enforce a method
 * whitelist.
 * <p/>
 * When drivers are collected for a block, they are combined into a compound
 * driver. This compound driver will in turn generate a compound environment
 * that wraps the contributing environments. Which in turn results in the
 * block's component containing the list of methods from all drivers that apply
 * to the block.
 * <p/>
 * In some scenarios you may not want this to happen. Instead, only a select
 * list of methods should be shown for a block - for example, you may want to
 * suppress inventory functionality if your TileEntity implements IInventory.
 * <p/>
 * To do so, implement this interface in the <em>environment</em> that you
 * return from your driver's {@link Block#createEnvironment(net.minecraft.world.World, int, int, int)}
 * method, and provide the names of the allowed methods from {@link #whitelistedMethods()}.
 * <p/>
 * <em>Important</em>: if multiple drivers apply to a single block that each
 * provide a whitelist, the list of allowed methods is the intersection of the
 * different whitelists!
 */
public interface MethodWhitelist {
    /**
     * The list of methods allowed to be exposed for blocks this driver is used
     * for. Note that the names must <em>exactly</em> match the names of the
     * methods they allow.
     *
     * @return the list of allowed methods.
     */
    String[] whitelistedMethods();
}
