/**
 * This package provides component networking related functionality.
 * <p/>
 * This mainly involves the (purely server-side!) network that is spanned over
 * all of OpenComputers' components, including blocks and items alike.
 */
@net.minecraftforge.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|Nanomachines",
        apiVersion = API.VERSION)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package li.cil.oc.api.nanomachines;

import li.cil.oc.api.API;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;