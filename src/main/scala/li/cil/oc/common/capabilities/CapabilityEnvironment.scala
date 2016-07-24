package li.cil.oc.common.capabilities

import li.cil.oc.api
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.integration.Mods
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider

object CapabilityEnvironment {
  final val ProviderEnvironment = new ResourceLocation(Mods.IDs.OpenComputers, "environment")

  class Provider(val tileEntity: TileEntity with Environment) extends ICapabilityProvider with Environment {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == Capabilities.EnvironmentCapability
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    override def node = tileEntity.node

    override def onMessage(message: Message) = tileEntity.onMessage(message)

    override def onConnect(node: Node) = tileEntity.onConnect(node)

    override def onDisconnect(node: Node) = tileEntity.onDisconnect(node)
  }

  class DefaultImpl extends Environment {
    override val node = api.Network.newNode(this, Visibility.None).create()

    override def onMessage(message: Message): Unit = {}

    override def onConnect(node: Node): Unit = {}

    override def onDisconnect(node: Node): Unit = {}
  }

  class DefaultStorage extends Capability.IStorage[Environment] {
    override def writeNBT(capability: Capability[Environment], t: Environment, facing: EnumFacing): NBTBase = {
      val node = t.node
      if (node != null) {
        val nbt = new NBTTagCompound()
        node.save(nbt)
        nbt
      }
      else null
    }

    override def readNBT(capability: Capability[Environment], t: Environment, facing: EnumFacing, nbtBase: NBTBase): Unit = {
      nbtBase match {
        case nbt: NBTTagCompound =>
          val node = t.node
          if (node != null) node.load(nbt)
        case _ =>
      }
    }
  }

}
