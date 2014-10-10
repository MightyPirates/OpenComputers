/**
 * This package contains interfaces used by the file system implementation.
 * <p/>
 * This allows it to add custom file systems that will behave the same as the
 * existing ones, particularly that can be used the same from a machine as any
 * other. In the case of Lua, for example, this means it can be mounted like
 * any other file system, and interacted with without further special handling.
 * <p/>
 * <em>You will usually not need to implement these interfaces!</em>
 * <p/>
 * Consider using the factory methods in {@link li.cil.oc.api.FileSystem} to
 * create file systems and wrapper nodes for these file systems (i.e. nodes
 * that can be added as component nodes to the network, so they can be used
 * from computers).
 */
@cpw.mods.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|FileSystem",
        apiVersion = API.VERSION)
package li.cil.oc.api.fs;

import li.cil.oc.api.API;