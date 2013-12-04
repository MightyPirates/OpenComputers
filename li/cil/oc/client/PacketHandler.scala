package li.cil.oc.client

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.PackedColor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagString, NBTBase, NBTTagCompound}
import net.minecraft.util.StatCollector
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.input.Keyboard
import scala.Some
import scala.collection.convert.WrapAsScala._

class PacketHandler extends CommonPacketHandler {
  protected override def world(player: Player, dimension: Int) = {
    val world = player.asInstanceOf[EntityPlayer].worldObj
    if (world.provider.dimensionId == dimension) Some(world)
    else None
  }

  override def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.Analyze => onAnalyze(p)
      case PacketType.ChargerState => onChargerState(p)
      case PacketType.ComputerState => onComputerState(p)
      case PacketType.ComputerUserList => onComputerUserList(p)
      case PacketType.PowerState => onPowerState(p)
      case PacketType.RedstoneState => onRedstoneState(p)
      case PacketType.RobotAnimateSwing => onRobotAnimateSwing(p)
      case PacketType.RobotAnimateTurn => onRobotAnimateTurn(p)
      case PacketType.RobotEquippedItemChange => onRobotEquippedItemChange(p)
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
      case _ => // Invalid packet.
    }

  def onAnalyze(p: PacketParser) {
    val player = p.player.asInstanceOf[EntityPlayer]
    val stats = p.readNBT().asInstanceOf[NBTTagCompound]
    stats.getTags.map(_.asInstanceOf[NBTBase]).map(tag => {
      ("§6" + StatCollector.translateToLocal(tag.getName) + "§f: " + (tag match {
        case value: NBTTagString => value.data
        case _ => "ERROR: invalid value type. Stat values must be strings."
      })).trim
    }).toArray.sorted.foreach(player.addChatMessage)
    val address = p.readUTF()
    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
      GuiScreen.setClipboardString(address)
      player.addChatMessage("Address copied to clipboard.")
    }
  }

  def onChargerState(p: PacketParser) =
    p.readTileEntity[Charger]() match {
      case Some(t) => t.chargeSpeed = p.readDouble()
      case _ => // Invalid packet.
    }

  def onComputerState(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) => t.isRunning = p.readBoolean()
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
    p.readTileEntity[Redstone]() match {
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
      case Some(t) => t.robot.xp = p.readDouble()
      case _ => // Invalid packet.
    }

  def onRotatableState(p: PacketParser) =
    p.readTileEntity[Rotatable]() match {
      case Some(t) =>
        t.pitch = p.readDirection()
        t.yaw = p.readDirection()
      case _ => // Invalid packet.
    }

  def onScreenColorChange(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) =>
        t.buffer.foreground = p.readInt()
        t.buffer.background = p.readInt()
      case _ => // Invalid packet.
    }

  def onScreenCopy(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) =>
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val tx = p.readInt()
        val ty = p.readInt()
        t.buffer.copy(col, row, w, h, tx, ty)
      case _ => // Invalid packet.
    }

  def onScreenDepthChange(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) => t.buffer.depth = PackedColor.Depth(p.readInt())
      case _ => // Invalid packet.
    }

  def onScreenFill(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) =>
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val c = p.readChar()
        t.buffer.fill(col, row, w, h, c)
      case _ => // Invalid packet.
    }

  def onScreenPowerChange(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => t.hasPower = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onScreenResolutionChange(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) =>
        val w = p.readInt()
        val h = p.readInt()
        t.buffer.resolution = (w, h)
      case _ => // Invalid packet.
    }

  def onScreenSet(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) =>
        val col = p.readInt()
        val row = p.readInt()
        val s = p.readUTF()
        t.buffer.set(col, row, s)
      case _ => // Invalid packet.
    }
}