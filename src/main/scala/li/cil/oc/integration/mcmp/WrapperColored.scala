package li.cil.oc.integration.mcmp

import java.util

import li.cil.oc.api.internal.Colored
import li.cil.oc.common.capabilities.Capabilities
import mcmultipart.capabilities.ICapabilityWrapper
import net.minecraftforge.common.capabilities.Capability

object WrapperColored extends ICapabilityWrapper[Colored] {
  override def getCapability: Capability[Colored] = Capabilities.ColoredCapability

  override def wrapImplementations(implementations: util.Collection[Colored]): Colored = implementations.iterator().next()
}
