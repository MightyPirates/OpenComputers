/**
 * This package contains driver related interfaces.
 * <p/>
 * Drivers are used to add items and third party blocks to the internal network,
 * which is mostly used to make components wrapping them available to computers.
 */
@net.minecraftforge.fml.common.API(
        owner = API.ID_OWNER,
        provides = "opencomputersapi|driver",
        apiVersion = API.VERSION)
package li.cil.oc.api.driver;

import li.cil.oc.api.API;