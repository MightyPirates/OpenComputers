package li.cil.oc.common.capabilities

import li.cil.oc.api.network.Node
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.integration.Mods
import net.minecraft.nbt.NBTBase
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object CapabilitySidedEnvironment {
  final val ProviderSidedEnvironment = new ResourceLocation(Mods.IDs.OpenComputers, "sided_environment")

  class Provider(val tileEntity: TileEntity with SidedEnvironment) extends ICapabilityProvider with SidedEnvironment {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.SidedEnvironmentCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def sidedNode(side: EnumFacing) = tileEntity.sidedNode(side)

    override def canConnect(side: EnumFacing) = tileEntity.canConnect(side)
  }

  class DefaultImpl extends SidedEnvironment {
    override def sidedNode(side: EnumFacing): Node = null

    override def canConnect(side: EnumFacing): Boolean = false
  }

  class DefaultStorage extends Capability.IStorage[SidedEnvironment] {
    override def writeNBT(capability: Capability[SidedEnvironment], t: SidedEnvironment, enumFacing: EnumFacing): NBTBase = null

    override def readNBT(capability: Capability[SidedEnvironment], t: SidedEnvironment, enumFacing: EnumFacing, nbtBase: NBTBase): Unit = {}
  }

}
