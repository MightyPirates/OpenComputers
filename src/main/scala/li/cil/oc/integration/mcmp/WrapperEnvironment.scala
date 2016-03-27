package li.cil.oc.integration.mcmp

import java.util

import li.cil.oc.api.network.Environment
import li.cil.oc.common.capabilities.Capabilities
import mcmultipart.capabilities.ICapabilityWrapper
import net.minecraftforge.common.capabilities.Capability

object WrapperEnvironment extends ICapabilityWrapper[Environment] {
  override def getCapability: Capability[Environment] = Capabilities.EnvironmentCapability

  override def wrapImplementations(implementations: util.Collection[Environment]): Environment = implementations.iterator().next()
}
