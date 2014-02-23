package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Node, Message, Visibility, SidedEnvironment}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{api, Settings}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

trait Hub extends Environment with SidedEnvironment {
  protected val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => new Plug(side))

  // ----------------------------------------------------------------------- //

  override def node = null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = true

  override def sidedNode(side: ForgeDirection) = plugs(side.ordinal()).node

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "plugs", NBT.TAG_COMPOUND).foreach {
      case (list, index) => plugs(index).node.load(list.getCompoundTagAt(index))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "plugs", plugs.map(plug => {
      val plugNbt = new NBTTagCompound()
      plug.node.save(plugNbt)
      plugNbt
    }))
  }

  // ----------------------------------------------------------------------- //

  protected class Plug(val side: ForgeDirection) extends api.network.Environment {
    val node = createNode(this)

    override def onMessage(message: Message) {
      if (isPrimary) {
        onPlugMessage(this, message)
      }
    }

    override def onConnect(node: Node) = onPlugConnect(this, node)

    override def onDisconnect(node: Node) = onPlugDisconnect(this, node)

    def isPrimary = plugs(plugs.indexWhere(_.node.network == node.network)) == this

    def plugsInOtherNetworks = plugs.filter(_.node.network != node.network)
  }

  protected def onPlugConnect(plug: Plug, node: Node) {}

  protected def onPlugDisconnect(plug: Plug, node: Node) {}

  protected def onPlugMessage(plug: Plug, message: Message) {
    if (message.name == "network.message") {
      plug.plugsInOtherNetworks.foreach(_.node.sendToReachable(message.name, message.data: _*))
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
