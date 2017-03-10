package li.cil.oc.server.component

import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.server.component

object Redstone {

  class Vanilla(val redstone: EnvironmentHost with RedstoneAwareImpl)
    extends component.RedstoneVanilla

}
