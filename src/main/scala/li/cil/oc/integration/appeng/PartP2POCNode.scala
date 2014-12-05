package li.cil.oc.integration.appeng

import appeng.api.config.TunnelType
import appeng.parts.p2p.PartP2PTunnel
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class PartP2POCNode(stack: ItemStack) extends PartP2PTunnel[PartP2POCNode](stack) with Environment with SidedEnvironment {
  val node = api.Network.newNode(this, Visibility.None).create()
  api.Network.joinNewNetwork(node)

  var input: Option[Node] = None

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {}

  override def onMessage(message: Message) {}

  override def onDisconnect(node: Node) {}

  override def sidedNode(side: ForgeDirection) = if (proxy.isActive) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = true

  // ----------------------------------------------------------------------- //

  // TODO Never called, might as well return null. May have to, if enum gets removed.
  override def getTunnelType = TunnelType.COMPUTER_MESSAGE

  @SideOnly(Side.CLIENT)
  override def getTypeTexture = api.Items.get("adapter").block().getIcon(2, 0)

  // ----------------------------------------------------------------------- //

  override def onTunnelNetworkChange() {
    super.onTunnelNetworkChange()
    if (node != null) {
      input.foreach(in => if (in != node) node.disconnect(in))
      input = None
      if (output) {
        Option(getInput) match {
          case Some(part) =>
            input = Option(part.node)
            input.foreach(node.connect)
          case _ =>
        }
      }
    }
  }

  override def addToWorld() {
    super.addToWorld()
    EventHandler.schedule(() => api.Network.joinOrCreateNetwork(getHost.getTile))
  }

  override def removeFromWorld() {
    super.removeFromWorld()
    if (node != null) node.remove()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(data: NBTTagCompound) {
    super.readFromNBT(data)
    node.load(data)
  }

  override def writeToNBT(data: NBTTagCompound) {
    super.writeToNBT(data)
    node.save(data)
  }
}
