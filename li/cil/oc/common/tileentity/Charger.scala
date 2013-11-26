package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraftforge.common.ForgeDirection

class Charger extends Environment with Redstone {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  private val robots = Array.fill(6)(None: Option[RobotProxy])

  private var chargeSpeed = 0.0

  override def updateEntity() {
    super.updateEntity()
    updateRedstoneInput()

    if (chargeSpeed > 0) {
      val charge = Settings.get.chargeRate * chargeSpeed
      robots.collect {
        case Some(proxy) => node.changeBuffer(proxy.robot.battery.changeBuffer(charge + node.changeBuffer(-charge)))
      }
    }
  }

  override def validate() {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRedstoneStateRequest(this)
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      onNeighborChanged()
    }
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    chargeSpeed = 0.0 max (ForgeDirection.VALID_DIRECTIONS.map(input).max min 15) / 15.0
  }

  def onNeighborChanged() {
    checkRedstoneInputChanged()
    ForgeDirection.VALID_DIRECTIONS.map(side => (side.ordinal(), world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ))).collect {
      case (side, proxy: RobotProxy) => robots(side) = Some(proxy)
    }
  }
}
