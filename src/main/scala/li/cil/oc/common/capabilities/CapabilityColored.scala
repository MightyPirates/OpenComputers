package li.cil.oc.common.capabilities

import li.cil.oc.api.internal.Colored
import li.cil.oc.integration.Mods
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagInt
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object CapabilityColored {
  final val ProviderColored = new ResourceLocation(Mods.IDs.OpenComputers, "colored")

  class Provider(val tileEntity: TileEntity with Colored) extends ICapabilityProvider with Colored {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.ColoredCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def getColor = tileEntity.getColor

    override def setColor(value: Int) = tileEntity.setColor(value)

    override def controlsConnectivity = tileEntity.controlsConnectivity
  }

  class DefaultImpl extends Colored {
    var color = 0

    override def getColor = color

    override def setColor(value: Int): Unit = color = value

    override def controlsConnectivity = false
  }

  class DefaultStorage extends Capability.IStorage[Colored] {
    override def writeNBT(capability: Capability[Colored], t: Colored, enumFacing: EnumFacing): NBTBase = {
      val color = t.getColor
      new NBTTagInt(color)
    }

    override def readNBT(capability: Capability[Colored], t: Colored, enumFacing: EnumFacing, nbtBase: NBTBase): Unit = {
      nbtBase match {
        case nbt: NBTTagInt =>
          t.setColor(nbt.getInt)
        case _ =>
      }
    }
  }

}
