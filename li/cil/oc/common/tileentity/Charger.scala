package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraftforge.common.ForgeDirection

class Charger extends Environment with Redstone {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  val robots = Array.fill(6)(None: Option[RobotProxy])

  var chargeSpeed = 0.0

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      updateRedstoneInput()

      val charge = Settings.get.chargeRate * chargeSpeed
      robots.collect {
        case Some(proxy) => node.changeBuffer(proxy.robot.battery.changeBuffer(charge + node.changeBuffer(-charge)))
      }
    }
    else if (chargeSpeed > 0 && world.getWorldInfo.getWorldTotalTime % 10 == 0) {
      ForgeDirection.VALID_DIRECTIONS.map(side => world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ)).collect {
        case proxy: RobotProxy if proxy.globalBuffer / proxy.globalBufferSize < 0.95 =>
          val theta = world.rand.nextDouble * Math.PI
          val phi = world.rand.nextDouble * Math.PI * 2
          val dx = 0.45 * Math.sin(theta) * Math.cos(phi)
          val dy = 0.45 * Math.sin(theta) * Math.sin(phi)
          val dz = 0.45 * Math.cos(theta)
          world.spawnParticle("happyVillager", proxy.x + 0.5 + dx, proxy.y + 0.5 + dz, proxy.z + 0.5 + dy, 0, 0, 0)
      }
    }
  }

  override def validate() {
    super.validate()
    chargeSpeed = 0.0 max (ForgeDirection.VALID_DIRECTIONS.map(input).max min 15) / 15.0
    if (isClient) {
      ClientPacketSender.sendChargerStateRequest(this)
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
    if (isServer) {
      ServerPacketSender.sendChargerState(this)
    }
  }

  def onNeighborChanged() {
    checkRedstoneInputChanged()
    ForgeDirection.VALID_DIRECTIONS.map(side => (side.ordinal(), world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ))).collect {
      case (side, proxy: RobotProxy) => robots(side) = Some(proxy)
    }
  }
}
