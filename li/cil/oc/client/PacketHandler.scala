package li.cil.oc.client

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.PackedColor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.input.Keyboard
import scala.Some

class PacketHandler extends CommonPacketHandler {
  protected override def world(player: Player, dimension: Int) = {
    val world = player.asInstanceOf[EntityPlayer].worldObj
    if (world.provider.dimensionId == dimension) Some(world)
    else None
  }

  override def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.AbstractBusState => onAbstractBusState(p)
      case PacketType.Analyze => onAnalyze(p)
      case PacketType.ChargerState => onChargerState(p)
      case PacketType.ComputerState => onComputerState(p)
      case PacketType.ComputerUserList => onComputerUserList(p)
      case PacketType.PowerState => onPowerState(p)
      case PacketType.RedstoneState => onRedstoneState(p)
      case PacketType.RobotAnimateSwing => onRobotAnimateSwing(p)
      case PacketType.RobotAnimateTurn => onRobotAnimateTurn(p)
      case PacketType.RobotEquippedItemChange => onRobotEquippedItemChange(p)
      case PacketType.RobotEquippedUpgradeChange => onRobotEquippedUpgradeChange(p)
      case PacketType.RobotMove => onRobotMove(p)
      case PacketType.RobotSelectedSlotChange => onRobotSelectedSlotChange(p)
      case PacketType.RobotXp => onRobotXp(p)
      case PacketType.RotatableState => onRotatableState(p)
      case PacketType.ScreenColorChange => onScreenColorChange(p)
      case PacketType.ScreenCopy => onScreenCopy(p)
      case PacketType.ScreenDepthChange => onScreenDepthChange(p)
      case PacketType.ScreenFill => onScreenFill(p)
      case PacketType.ScreenPowerChange => onScreenPowerChange(p)
      case PacketType.ScreenResolutionChange => onScreenResolutionChange(p)
      case PacketType.ScreenSet => onScreenSet(p)
      case PacketType.ServerPresence => onServerPresence(p)
      case _ => // Invalid packet.
    }

  def onAbstractBusState(p: PacketParser) =
    p.readTileEntity[AbstractBusAware]() match {
      case Some(t) => t.isAbstractBusAvailable = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onAnalyze(p: PacketParser) {
    val player = p.player.asInstanceOf[EntityPlayer]
    val address = p.readUTF()
    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
      GuiScreen.setClipboardString(address)
      player.addChatMessage("Address copied to clipboard.")
    }
  }

  def onChargerState(p: PacketParser) =
    p.readTileEntity[Charger]() match {
      case Some(t) =>
        t.chargeSpeed = p.readDouble()
        t.world.markBlockForRenderUpdate(t.x, t.y, t.z)
      case _ => // Invalid packet.
    }

  def onComputerState(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => t.setRunning(p.readBoolean())
      case Some(t: Rack) =>
        val number = p.readInt()
        if (number == -1) {
          t.range = p.readInt()
        }
        else {
          t.setRunning(number, p.readBoolean())
          t.sides(number) = p.readDirection()
          val key = p.readUTF()
          if (key != "") {
            t.terminals(number).key = Option(key)
          }
        }
      case _ => // Invalid packet.
    }

  def onComputerUserList(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) =>
        val count = p.readInt()
        t.users = (0 until count).map(_ => p.readUTF())
      case _ => // Invalid packet.
    }

  def onPowerState(p: PacketParser) =
    p.readTileEntity[PowerInformation]() match {
      case Some(t) =>
        t.globalBuffer = p.readDouble()
        t.globalBufferSize = p.readDouble()
      case _ => // Invalid packet.
    }

  def onRedstoneState(p: PacketParser) =
    p.readTileEntity[RedstoneAware]() match {
      case Some(t) =>
        t.isOutputEnabled = p.readBoolean()
        for (d <- ForgeDirection.VALID_DIRECTIONS) {
          t.output(d, p.readByte())
        }
      case _ => // Invalid packet.
    }

  def onRobotAnimateSwing(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.setAnimateSwing(p.readInt())
      case _ => // Invalid packet.
    }

  def onRobotAnimateTurn(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.setAnimateTurn(p.readByte(), p.readInt())
      case _ => // Invalid packet.
    }

  def onRobotEquippedItemChange(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.equippedItem = Option(p.readItemStack())
      case _ => // Invalid packet.
    }

  def onRobotEquippedUpgradeChange(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.equippedUpgrade = Option(p.readItemStack())
      case _ => // Invalid packet.
    }

  def onRobotMove(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.move(p.readDirection())
      case _ => // Invalid packet.
    }

  def onRobotSelectedSlotChange(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.selectedSlot = p.readInt()
      case _ => // Invalid packet.
    }

  def onRobotXp(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) =>
        t.robot.xp = p.readDouble()
        t.robot.updateXpInfo()
      case _ => // Invalid packet.
    }

  def onRotatableState(p: PacketParser) =
    p.readTileEntity[Rotatable]() match {
      case Some(t) =>
        t.pitch = p.readDirection()
        t.yaw = p.readDirection()
      case _ => // Invalid packet.
    }

  def onScreenColorChange(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    buffer.foreground = p.readInt()
    buffer.background = p.readInt()
  }

  def onScreenCopy(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val tx = p.readInt()
    val ty = p.readInt()
    buffer.copy(col, row, w, h, tx, ty)
  }

  def onScreenDepthChange(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    buffer.depth = PackedColor.Depth(p.readInt())
  }

  def onScreenFill(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val c = p.readChar()
    buffer.fill(col, row, w, h, c)
  }

  def onScreenPowerChange(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => t.hasPower = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onScreenResolutionChange(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    val w = p.readInt()
    val h = p.readInt()
    buffer.resolution = (w, h)
  }

  def onScreenSet(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: Buffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    val col = p.readInt()
    val row = p.readInt()
    val s = p.readUTF()
    buffer.set(col, row, s)
  }

  def onServerPresence(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) => for (i <- 0 until t.isPresent.length) {
        if (p.readBoolean()) {
          t.isPresent(i) = Some(p.readUTF())
        }
        else t.isPresent(i) = None
      }
      case _ => // Invalid packet.
    }
}