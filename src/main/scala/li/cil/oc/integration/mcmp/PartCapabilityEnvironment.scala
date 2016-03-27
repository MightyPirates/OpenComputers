package li.cil.oc.integration.mcmp

import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.integration.Mods
import mcmultipart.multipart.IMultipart
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object PartCapabilityEnvironment {
  final val PartProviderEnvironment = new ResourceLocation(Mods.IDs.OpenComputers, "part_environment")

  class Provider(val part: IMultipart with Environment) extends ICapabilityProvider with Environment {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.EnvironmentCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def node = part.node

    override def onMessage(message: Message) = part.onMessage(message)

    override def onConnect(node: Node) = part.onConnect(node)

    override def onDisconnect(node: Node) = part.onDisconnect(node)
  }

}
