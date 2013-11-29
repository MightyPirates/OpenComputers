package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.util.PackedColor
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some

object PacketSender {
  def sendAnalyze(stats: NBTTagCompound, address: String, player: Player) {
    val pb = new PacketBuilder(PacketType.Analyze)

    pb.writeNBT(stats)
    pb.writeUTF(address)

    pb.sendToPlayer(player)
  }

  def sendChargerState(t: Charger, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.ChargerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.chargeSpeed)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendComputerState(t: Computer, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isRunning)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendPowerState(t: PowerInformation, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.PowerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.globalBuffer)
    pb.writeDouble(t.globalBufferSize)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendRedstoneState(t: Redstone, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.RedstoneState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isOutputEnabled)
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      pb.writeByte(t.output(d))
    }

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendRobotMove(t: Robot, ox: Int, oy: Int, oz: Int, direction: ForgeDirection) {
    val pb = new PacketBuilder(PacketType.RobotMove)

    // Custom pb.writeTileEntity() with fake coordinates (valid for the client).
    pb.writeInt(t.proxy.world.provider.dimensionId)
    pb.writeInt(ox)
    pb.writeInt(oy)
    pb.writeInt(oz)
    pb.writeDirection(direction)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotAnimateSwing(t: Robot) {
    val pb = new PacketBuilder(PacketType.RobotAnimateSwing)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotAnimateTurn(t: Robot) {
    val pb = new PacketBuilder(PacketType.RobotAnimateTurn)

    pb.writeTileEntity(t.proxy)
    pb.writeByte(t.turnAxis)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotEquippedItemChange(t: Robot, stack: ItemStack) {
    val pb = new PacketBuilder(PacketType.RobotEquippedItemChange)

    pb.writeTileEntity(t.proxy)
    pb.writeItemStack(stack)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotSelectedSlotChange(t: Robot) {
    val pb = new PacketBuilder(PacketType.RobotSelectedSlotChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.selectedSlot)

    pb.sendToNearbyPlayers(t)
  }

  def sendRotatableState(t: Rotatable, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.RotatableState)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendScreenColorChange(t: Buffer, foreground: Int, background: Int) {
    val pb = new PacketBuilder(PacketType.ScreenColorChange)

    pb.writeTileEntity(t)
    pb.writeInt(foreground)
    pb.writeInt(background)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenCopy(t: Buffer, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    val pb = new PacketBuilder(PacketType.ScreenCopy)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeInt(tx)
    pb.writeInt(ty)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenDepthChange(t: Buffer, value: PackedColor.Depth.Value) {
    val pb = new PacketBuilder(PacketType.ScreenDepthChange)

    pb.writeTileEntity(t)
    pb.writeInt(value.id)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenFill(t: Buffer, col: Int, row: Int, w: Int, h: Int, c: Char) {
    val pb = new PacketBuilder(PacketType.ScreenFill)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeChar(c)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenPowerChange(t: Buffer, hasPower: Boolean) {
    val pb = new PacketBuilder(PacketType.ScreenPowerChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(hasPower)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenResolutionChange(t: Buffer, w: Int, h: Int) {
    val pb = new PacketBuilder(PacketType.ScreenResolutionChange)

    pb.writeTileEntity(t)
    pb.writeInt(w)
    pb.writeInt(h)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenSet(t: Buffer, col: Int, row: Int, s: String) {
    val pb = new PacketBuilder(PacketType.ScreenSet)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeUTF(s)

    pb.sendToNearbyPlayers(t)
  }
}