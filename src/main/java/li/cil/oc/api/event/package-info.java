/**
 * Events dispatched by OpenComputers to allow other mods to hook into some
 * of its functionality.
 */
@net.minecraftforge.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|Event",
        apiVersion = API.VERSION)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package li.cil.oc.api.event;

import li.cil.oc.api.API;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;