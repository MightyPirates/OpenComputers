package li.cil.oc.client

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent
import li.cil.oc.Localization
import li.cil.oc.api.component
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.common.{PacketType, PacketHandler => CommonPacketHandler}
import li.cil.oc.util.Audio
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
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
      case PacketType.DisassemblerActiveChange => onDisassemblerActiveChange(p)
      case PacketType.FloppyChange => onFloppyChange(p)
      case PacketType.HologramClear => onHologramClear(p)
      case PacketType.HologramColor => onHologramColor(p)
      case PacketType.HologramPowerChange => onHologramPowerChange(p)
      case PacketType.HologramScale => onHologramScale(p)
      case PacketType.HologramSet => onHologramSet(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.PowerState => onPowerState(p)
      case PacketType.RedstoneState => onRedstoneState(p)
      case PacketType.RobotAnimateSwing => onRobotAnimateSwing(p)
      case PacketType.RobotAnimateTurn => onRobotAnimateTurn(p)
      case PacketType.RobotAssemblingState => onRobotAssemblingState(p)
      case PacketType.RobotInventoryChange => onRobotInventoryChange(p)
      case PacketType.RobotMove => onRobotMove(p)
      case PacketType.RobotSelectedSlotChange => onRobotSelectedSlotChange(p)
      case PacketType.RotatableState => onRotatableState(p)
      case PacketType.SwitchActivity => onSwitchActivity(p)
      case PacketType.TextBufferColorChange => onTextBufferColorChange(p)
      case PacketType.TextBufferCopy => onTextBufferCopy(p)
      case PacketType.TextBufferDepthChange => onTextBufferDepthChange(p)
      case PacketType.TextBufferFill => onTextBufferFill(p)
      case PacketType.TextBufferInit => onTextBufferInit(p)
      case PacketType.TextBufferPaletteChange => onTextBufferPaletteChange(p)
      case PacketType.TextBufferPowerChange => onTextBufferPowerChange(p)
      case PacketType.TextBufferResolutionChange => onTextBufferResolutionChange(p)
      case PacketType.TextBufferSet => onTextBufferSet(p)
      case PacketType.ScreenTouchMode => onScreenTouchMode(p)
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
      p.player.addChatMessage(Localization.Analyzer.AddressCopied)
    }
  }

  def onChargerState(p: PacketParser) =
    p.readTileEntity[Charger]() match {
      case Some(t) =>
        t.chargeSpeed = p.readDouble()
        t.hasPower = p.readBoolean()
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
      case Some(t: ServerRack) =>
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

  def onDisassemblerActiveChange(p: PacketParser) =
    p.readTileEntity[Disassembler]() match {
      case Some(t) => t.isActive = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onFloppyChange(p: PacketParser) =
    p.readTileEntity[DiskDrive]() match {
      case Some(t) => t.setInventorySlotContents(0, p.readItemStack())
      case _ => // Invalid packet.
    }

  def onHologramClear(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        for (i <- 0 until t.volume.length) t.volume(i) = 0
        t.dirty = true
      case _ => // Invalid packet.
    }

  def onHologramColor(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        val index = p.readInt()
        val value = p.readInt()
        t.colors(index) = value & 0xFFFFFF
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
            t.volume(x + z * t.width + t.width * t.width) = p.readInt()
          }
        }
        t.dirty = true
      case _ => // Invalid packet.
    }

  def onPetVisibility(p: PacketParser) {
    val count = p.readInt()
    for (i <- 0 until count) {
      val name = p.readUTF()
      if (p.readBoolean()) {
        PetRenderer.hidden -= name
      }
      else {
        PetRenderer.hidden += name
      }
    }
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

  def onRobotAssemblingState(p: PacketParser) =
    p.readTileEntity[RobotAssembler]() match {
      case Some(t) =>
        if (p.readBoolean()) t.requiredEnergy = 9001
        else t.requiredEnergy = 0
      case _ => // Invalid packet.
    }

  def onRobotInventoryChange(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) =>
        val robot = t.robot
        val slot = p.readInt()
        val stack = p.readItemStack()
        if (slot >= robot.getSizeInventory - robot.componentCount) {
          robot.info.components(slot - (robot.getSizeInventory - robot.componentCount)) = stack
        }
        else t.robot.setInventorySlotContents(slot, stack)
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

  def onRotatableState(p: PacketParser) =
    p.readTileEntity[Rotatable]() match {
      case Some(t) =>
        t.pitch = p.readDirection()
        t.yaw = p.readDirection()
      case _ => // Invalid packet.
    }

  def onSwitchActivity(p: PacketParser) =
    p.readTileEntity[Switch]() match {
      case Some(t) => t.lastMessage = System.currentTimeMillis()
      case _ => // Invalid packet.
    }

  def onTextBufferColorChange(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val foreground = p.readInt()
        val foregroundIsPalette = p.readBoolean()
        buffer.setForegroundColor(foreground, foregroundIsPalette)
        val background = p.readInt()
        val backgroundIsPalette = p.readBoolean()
        buffer.setBackgroundColor(background, backgroundIsPalette)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferCopy(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val tx = p.readInt()
        val ty = p.readInt()
        buffer.copy(col, row, w, h, tx, ty)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferDepthChange(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        buffer.setColorDepth(component.TextBuffer.ColorDepth.values.apply(p.readInt()))
      case _ => // Invalid packet.
    }
  }

  def onTextBufferFill(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val c = p.readChar()
        buffer.fill(col, row, w, h, c)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferInit(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: li.cil.oc.common.component.TextBuffer) => buffer.data.load(p.readNBT())
      case _ => // Invalid packet.
    }
  }

  def onTextBufferPaletteChange(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val index = p.readInt()
        val color = p.readInt()
        buffer.setPaletteColor(index, color)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferPowerChange(p: PacketParser) =
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        buffer.setRenderingEnabled(p.readBoolean())
      case _ => // Invalid packet.
    }

  def onTextBufferResolutionChange(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val w = p.readInt()
        val h = p.readInt()
        buffer.setResolution(w, h)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferSet(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        val col = p.readInt()
        val row = p.readInt()
        val s = p.readUTF()
        val vertical = p.readBoolean()
        buffer.set(col, row, s, vertical)
      case _ => // Invalid packet.
    }
  }

  def onScreenTouchMode(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => t.invertTouchMode = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onServerPresence(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
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