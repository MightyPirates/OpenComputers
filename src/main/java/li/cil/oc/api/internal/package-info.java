/**
 * This package provides interfaces that are implemented by OC internal
 * classes so that they can be checked for and used by type checking and
 * casting to these interfaces.
 * <p/>
 * For example, to determine whether a tile entity is a robot, you can
 * do an <tt>instanceof</tt> with the {@link li.cil.oc.api.internal.Robot}
 * interface - and cast to it if you wish to access some of the provided
 * functionality.
 * <p/>
 * The other main use-case is in {@link li.cil.oc.api.driver.item.HostAware}
 * drivers, where these interfaces can be used to check if the item can be
 * used inside the specified environment (where the environment class may
 * be assignable to one of the interfaces in this package).
 */
@cpw.mods.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|Internal",
        apiVersion = API.VERSION)
package li.cil.oc.api.internal;

import li.cil.oc.api.API;