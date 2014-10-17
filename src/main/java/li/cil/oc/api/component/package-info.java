/**
 * This package provides interfaces to allow interacting with some components.
 * <p/>
 * These interfaces allow more specific interaction with some of OpenComputers'
 * components, which would otherwise require reflection or linking against the
 * mod itself.
 */
@cpw.mods.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|Component",
        apiVersion = API.VERSION)
package li.cil.oc.api.component;

import li.cil.oc.api.API;