package li.cil.oc.api.internal;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.machine.Machine;
import net.minecraft.entity.player.EntityPlayer;

/**
 * This interface is implemented as a marker by tablets.
 * <p/>
 * This is implemented by the class containing tablets' logic implementation,
 * which is <em>not the tablet item</em>! The tablet class serves as its
 * computer components' environment. That means you can use this to check for
 * tablets by using
 * <pre>
 *     if (node.host() instanceof Tablet) {
 * </pre>
 * <p/>
 * This can also be used by {@link li.cil.oc.api.driver.item.HostAware} item
 * drivers to check if the provided environment class is a tablet by checking
 * for assignability, which allows for items that make no sense in tablets to
 * deny being placed into them in the assembler, for example.
 * <p/>
 * The only purpose is to allow identifying tile entities as tablets
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Tablet extends EnvironmentHost, Rotatable {
    /**
     * The machine currently hosted by this tablet.
     */
    Machine machine();

    /**
     * Returns the player last holding the tablet.
     * <p/>
     * Note that this value may change over the lifetime of a tablet instance.
     * The player may also already have dropped the tablet - this value will
     * <em>not</em> be set to <tt>null</tt> in that case!
     *
     * @return the player last holding the tablet.
     */
    EntityPlayer player();
}
