package li.cil.oc.client

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.{Audio, PackedColor}
import li.cil.oc.Settings
import li.cil.oc.util.PackedColor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentTranslation
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.input.Keyboard

object PacketHandler extends CommonPacketHandler {
  @SubscribeEvent
  def onPacket(e: ClientCustomPacketEvent) =
    onPacketData(e.packet.payload, Minecraft.getMinecraft.thePlayer)

  protected override def world(player: EntityPlayer, dimension: Int) = {
    val world = player.worldObj
    if (world.provider.dimensionId == dimension) Some(world)
    else None
  }

  override def dispatch(p: PacketParser) {
    p.packetType match {
      case PacketType.AbstractBusState => onAbstractBusState(p)
      case PacketType.Analyze => onAnalyze(p)
      case PacketType.ChargerState => onChargerState(p)
      case PacketType.ColorChange => onColorChange(p)
      case PacketType.ComputerState => onComputerState(p)
      case PacketType.ComputerUserList => onComputerUserList(p)
      case PacketType.HologramClear => onHologramClear(p)
      case PacketType.HologramPowerChange => onHologramPowerChange(p)
      case PacketType.HologramScale => onHologramScale(p)
      case PacketType.HologramSet => onHologramSet(p)
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
      case PacketType.RouterActivity => onRouterActivity(p)
      case PacketType.ScreenColorChange => onScreenColorChange(p)
      case PacketType.ScreenCopy => onScreenCopy(p)
      case PacketType.ScreenDepthChange => onScreenDepthChange(p)
      case PacketType.ScreenFill => onScreenFill(p)
      case PacketType.ScreenPowerChange => onScreenPowerChange(p)
      case PacketType.ScreenResolutionChange => onScreenResolutionChange(p)
      case PacketType.ScreenSet => onScreenSet(p)
      case PacketType.ServerPresence => onServerPresence(p)
      case PacketType.Sound => onSound(p)
      case _ => // Invalid packet.
    }
  }

  def onAbstractBusState(p: PacketParser) =
    p.readTileEntity[AbstractBusAware]() match {
      case Some(t) => t.isAbstractBusAvailable = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onAnalyze(p: PacketParser) {
    val address = p.readUTF()
    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
      GuiScreen.setClipboardString(address)
      p.player.addChatMessage(new ChatComponentTranslation(
        Settings.namespace + "gui.Analyzer.AddressCopied"))
    }
  }

  def onChargerState(p: PacketParser) =
    p.readTileEntity[Charger]() match {
      case Some(t) =>
        t.chargeSpeed = p.readDouble()
        t.world.markBlockForUpdate(t.x, t.y, t.z)
      case _ => // Invalid packet.
    }

  def onColorChange(p: PacketParser) =
    p.readTileEntity[Colored]() match {
      case Some(t) =>
        t.color = p.readInt()
        t.world.markBlockForUpdate(t.x, t.y, t.z)
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
          val keyCount = p.readInt()
          val keys = t.terminals(number).keys
          keys.clear()
          for (i <- 0 until keyCount) {
            keys += p.readUTF()
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

  def onHologramClear(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        for (i <- 0 until t.volume.length) t.volume(i) = 0
        t.dirty = true
      case _ => // Invalid packet.
    }

  def onHologramPowerChange(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) => t.hasPower = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onHologramScale(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        t.scale = p.readDouble()
        t.dirty = true
      case _ => // Invalid packet.
    }

  def onHologramSet(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        val fromX = p.readByte(): Int
        val untilX = p.readByte(): Int
        val fromZ = p.readByte(): Int
        val untilZ = p.readByte(): Int
        for (x <- fromX until untilX) {
          for (z <- fromZ until untilZ) {
            t.volume(x + z * t.width) = p.readInt()
          }
        }
        t.dirty = true
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

  def onRobotMove(p: PacketParser) = {
    val dimension = p.readInt()
    val x = p.readInt()
    val y = p.readInt()
    val z = p.readInt()
    val direction = p.readDirection()
    p.getTileEntity[RobotProxy](dimension, x, y, z) match {
      case Some(t) => t.robot.move(direction)
      case _ =>
        // Invalid packet, robot may be coming from outside our loaded area.
        PacketSender.sendRobotStateRequest(dimension, x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
    }
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

  def onRouterActivity(p: PacketParser) =
    p.readTileEntity[Router]() match {
      case Some(t) => t.lastMessage = System.currentTimeMillis()
      case _ => // Invalid packet.
    }

  def onScreenColorChange(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: TextBuffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    buffer.foreground = p.readInt()
    buffer.background = p.readInt()
  }

  def onScreenCopy(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: TextBuffer) => t.buffer
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
      case Some(t: TextBuffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    buffer.depth = PackedColor.Depth(p.readInt())
  }

  def onScreenFill(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: TextBuffer) => t.buffer
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
      case Some(t: TextBuffer) => t.buffer
      case Some(t: Rack) => t.terminals(p.readInt()).buffer
      case _ => return // Invalid packet.
    }
    val w = p.readInt()
    val h = p.readInt()
    buffer.resolution = (w, h)
  }

  def onScreenSet(p: PacketParser) {
    val buffer = p.readTileEntity[TileEntity]() match {
      case Some(t: TextBuffer) => t.buffer
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

  def onSound(p: PacketParser) {
    val dimension = p.readInt()
    if (world(p.player, dimension).isDefined) {
      val x = p.readInt()
      val y = p.readInt()
      val z = p.readInt()
      val frequency = p.readShort()
      val duration = p.readShort()
      Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, frequency, duration)
    }
  }
}