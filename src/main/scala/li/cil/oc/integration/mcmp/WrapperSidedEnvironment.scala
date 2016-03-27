package li.cil.oc.integration.mcmp

import java.util

import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.capabilities.Capabilities
import mcmultipart.capabilities.ICapabilityWrapper
import net.minecraftforge.common.capabilities.Capability

object WrapperSidedEnvironment extends ICapabilityWrapper[SidedEnvironment] {
  override def getCapability: Capability[SidedEnvironment] = Capabilities.SidedEnvironmentCapability

  override def wrapImplementations(implementations: util.Collection[SidedEnvironment]): SidedEnvironment = implementations.iterator().next()
}
