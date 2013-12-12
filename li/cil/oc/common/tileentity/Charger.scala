package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.network.{Analyzable, Node, Visibility}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class Charger extends Environment with RedstoneAware with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  val robots = Array.fill(6)(None: Option[RobotProxy])

  var chargeSpeed = 0.0

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      updateRedstoneInput()

      val charge = Settings.get.chargeRate * chargeSpeed
      robots.collect {
        case Some(proxy) => node.changeBuffer(proxy.robot.computer.node.changeBuffer(charge + node.changeBuffer(-charge)))
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

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      onNeighborChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    chargeSpeed = math.max(0, math.min(ForgeDirection.VALID_DIRECTIONS.map(input).max, 15) / 15.0)
    if (isServer) {
      ServerPacketSender.sendChargerState(this)
    }
  }

  def onNeighborChanged() {
    checkRedstoneInputChanged()
    ForgeDirection.VALID_DIRECTIONS.map(side => (side.ordinal(), world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ))).collect {
      case (side, proxy: RobotProxy) => robots(side) = Some(proxy)
      case (side, _) => robots(side) = None
    }
  }
}
