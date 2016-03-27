package li.cil.oc.integration.mcmp

import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.integration.Mods
import mcmultipart.multipart.IMultipart
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object PartCapabilitySidedComponent {
  final val PartSidedComponent = new ResourceLocation(Mods.IDs.OpenComputers, "part_sided_component")

  class Provider(val part: IMultipart with Environment with SidedComponent) extends ICapabilityProvider with SidedEnvironment {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.SidedEnvironmentCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def sidedNode(side: EnumFacing) = if (part.canConnectNode(side)) part.node else null

    override def canConnect(side: EnumFacing) = part.canConnectNode(side)
  }

}
