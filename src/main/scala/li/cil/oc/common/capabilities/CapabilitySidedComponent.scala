package li.cil.oc.common.capabilities

import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.integration.Mods
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier

object CapabilitySidedComponent {
  final val SidedComponent = new ResourceLocation(Mods.IDs.OpenComputers, "sided_component")

  class Provider(val tileEntity: TileEntity with Environment with SidedComponent) extends ICapabilityProvider with NonNullSupplier[Provider] with SidedEnvironment {
    private val wrapper = LazyOptional.of(this)

    def get = this

    def invalidate() = wrapper.invalidate

    override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
      if (capability == Capabilities.SidedEnvironmentCapability) wrapper.cast[T]
      else LazyOptional.empty[T]
    }

    override def sidedNode(side: Direction) = if (tileEntity.canConnectNode(side)) tileEntity.node else null

    override def canConnect(side: Direction) = tileEntity.canConnectNode(side)
  }

}
