/**
 * This package provides low level machine access.
 * <p/>
 * Using the {@link li.cil.oc.api.Machine} class, you can create new machine
 * instances, i.e. essentially computer "cores", that will run code. This allows
 * you to implement your own computer blocks. Or robots. Or whatever you come up
 * with.
 * <p/>
 * The interfaces in here also allow you to implement an arbitrary new
 * {@link li.cil.oc.api.machine.Architecture}, which can then be used when
 * creating a new {@link li.cil.oc.api.machine.Machine} using the factory
 * methods in {@link li.cil.oc.api.Machine}. An architecture could be a custom
 * language interpreter, or a full blown hardware emulator for old microchips.
 * <p/>
 * There are also a couple of interfaces in here that are not meant to be
 * implemented, but merely to allow accessing some mod internals in a regulated
 * fashion, such as {@link li.cil.oc.api.internal.Robot}.
 */
@cpw.mods.fml.common.API(
        owner = API.ID_OWNER,
        provides = "OpenComputersAPI|Machine",
        apiVersion = API.VERSION)
package li.cil.oc.api.machine;

import li.cil.oc.api.API;