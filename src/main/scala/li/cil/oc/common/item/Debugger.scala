package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.FakePlayer

class Debugger(val parent: Delegator) extends traits.Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    player match {
      case _: FakePlayer => false // Nope
      case realPlayer: EntityPlayerMP =>
        world.getTileEntity(position) match {
          case host: SidedEnvironment =>
            if (!world.isRemote) {
              Debugger.reconnect(Array(host.sidedNode(side)))
            }
            true
          case host: Environment =>
            if (!world.isRemote) {
              Debugger.reconnect(Array(host.getNode))
            }
            true
          case _ =>
            if (!world.isRemote) {
              Debugger.getNode.remove()
            }
            true
        }
      case _ => false
    }
  }
}

object Debugger extends Environment {
  var getNode = api.Network.newNode(this, Visibility.NETWORK).create()

  override def onConnect(node: Node) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] New node in network: ${nodeInfo(node)}")
  }

  override def onDisconnect(node: Node) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] Node removed from network: ${nodeInfo(node)}")
  }

  override def onMessage(message: Message) {
    OpenComputers.log.info(s"[NETWORK DEBUGGER] Received message: ${messageInfo(message)}.")
  }

  def reconnect(nodes: Array[Node]) {
    getNode.remove()
    api.Network.joinNewNetwork(getNode)
    for (node <- nodes if node != null) {
      this.getNode.connect(node)
    }
  }

  private def nodeInfo(node: Node) = s"{address = ${node.getAddress}, reachability = ${node.getReachability.name}" + (node match {
    case componentConnector: ComponentConnector => componentInfo(componentConnector) + connectorInfo(componentConnector)
    case component: Component => componentInfo(component)
    case connector: Connector => connectorInfo(connector)
    case _ =>
  }) + "}"

  private def componentInfo(component: Component) = s", type = component, name = ${component.getName}, visibility = ${component.getVisibility.name}"

  private def connectorInfo(connector: Connector) = s", type = connector, buffer = ${connector.getLocalBuffer}, bufferSize = ${connector.getLocalBufferSize}"

  private def messageInfo(message: Message) = s"{name = ${message.getName()}, source = ${nodeInfo(message.getSource)}, data = [${message.getData.mkString(", ")}]}"
}
