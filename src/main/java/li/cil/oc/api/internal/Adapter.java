package li.cil.oc.api.internal;

import li.cil.oc.api.network.Environment;
import net.minecraft.inventory.IInventory;

/**
 * This interface is implemented as a marker by adapters.
 * <p/>
 * This is implemented by adapter tile entities, which also serve as its
 * components' environment. That means you can use this to check for
 * adapters by using either:
 * <pre>
 *     if (tileEntity instanceof Adapter) {
 * </pre>
 * or
 * <pre>
 *     if (node.host() instanceof Adapter) {
 * </pre>
 * <p/>
 * This can also be used by {@link li.cil.oc.api.driver.item.HostAware} item
 * drivers to check if the provided environment class is an adapter by checking
 * for assignability, which allows for items that make no sense in adapters to
 * deny being placed into them, for example.
 * <p/>
 * The only purpose is to allow identifying tile entities as adapters
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Adapter extends Environment, IInventory {
}
