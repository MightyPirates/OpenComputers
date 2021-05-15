package li.cil.oc.common.capabilities

import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.integration.Mods
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object CapabilitySidedComponent {
  final val SidedComponent = new ResourceLocation(Mods.IDs.OpenComputers, "sided_component")

  class Provider(val tileEntity: TileEntity with Environment with SidedComponent) extends ICapabilityProvider with SidedEnvironment {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.SidedEnvironmentCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def sidedNode(side: EnumFacing) = if (tileEntity.canConnectNode(side)) tileEntity.node else null

    override def canConnect(side: EnumFacing) = tileEntity.canConnectNode(side)
  }

}
