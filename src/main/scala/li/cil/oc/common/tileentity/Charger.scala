package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Analyzable, Node, Visibility}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Localization, Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Charger extends traits.Environment with traits.PowerAcceptor with traits.RedstoneAware with traits.Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  val robots = Array.fill(6)(None: Option[RobotProxy])

  var chargeSpeed = 0.0

  var hasPower = false

  var invertSignal = false

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) node else null)

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.addChatMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed))
    null
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = true

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      val charge = Settings.get.chargeRate * chargeSpeed
      val canCharge = charge > 0 && node.globalBuffer >= charge
      if (hasPower && !canCharge) {
        hasPower = false
        ServerPacketSender.sendChargerState(this)
      }
      if (!hasPower && canCharge) {
        hasPower = true
        ServerPacketSender.sendChargerState(this)
      }
      if (canCharge) robots.collect {
        case Some(proxy) => node.changeBuffer(proxy.robot.bot.node.changeBuffer(charge + node.changeBuffer(-charge)))
      }
    }
    else if (chargeSpeed > 0 && hasPower && world.getWorldInfo.getWorldTotalTime % 10 == 0) {
      ForgeDirection.VALID_DIRECTIONS.map(side => world.getTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ)).collect {
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

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed") max 0 min 1
    hasPower = nbt.getBoolean("hasPower")
    invertSignal = nbt.getBoolean("invertSignal")
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
    nbt.setBoolean("hasPower", hasPower)
    nbt.setBoolean("invertSignal", invertSignal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed")
    hasPower = nbt.getBoolean("hasPower")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
    nbt.setBoolean("hasPower", hasPower)
  }

  // ----------------------------------------------------------------------- //

  override protected def updateRedstoneInput(side: ForgeDirection) {
    super.updateRedstoneInput(side)
    val signal = math.max(0, math.min(15, ForgeDirection.VALID_DIRECTIONS.map(input).max))

    if (invertSignal) chargeSpeed = (15 - signal) / 15.0
    else chargeSpeed = signal / 15.0
    if (isServer) {
      ServerPacketSender.sendChargerState(this)
    }
  }

  def onNeighborChanged() {
    checkRedstoneInputChanged()
    ForgeDirection.VALID_DIRECTIONS.map(side => (side.ordinal(), world.getTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ))).collect {
      case (side, proxy: RobotProxy) => robots(side) = Some(proxy)
      case (side, _) => robots(side) = None
    }
  }
}
