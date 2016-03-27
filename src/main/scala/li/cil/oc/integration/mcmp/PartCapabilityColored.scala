package li.cil.oc.integration.mcmp

import li.cil.oc.api.internal.Colored
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.integration.Mods
import mcmultipart.multipart.IMultipart
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object PartCapabilityColored {
  final val PartProviderColored = new ResourceLocation(Mods.IDs.OpenComputers, "part_colored")

  class Provider(val part: IMultipart with Colored) extends ICapabilityProvider with Colored {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.ColoredCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def getColor = part.getColor

    override def setColor(value: Int) = part.setColor(value)

    override def controlsConnectivity = part.controlsConnectivity
  }

}
