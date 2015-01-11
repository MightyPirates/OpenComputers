package li.cil.oc.client

import java.io.EOFException

import li.cil.oc.Localization
import li.cil.oc.api.component
import li.cil.oc.api.event.FileSystemAccessEvent
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.Audio
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent
import org.lwjgl.input.Keyboard

object PacketHandler extends CommonPacketHandler {
  @SubscribeEvent
  def onPacket(e: ClientCustomPacketEvent) =
    onPacketData(e.packet.payload, Minecraft.getMinecraft.thePlayer)

  protected override def world(player: EntityPlayer, dimension: Int) = {
    val world = player.worldObj
    if (world.provider.getDimensionId == dimension) Some(world)
    else None
  }

  override def dispatch(p: PacketParser) {
    p.packetType match {
      case PacketType.Analyze => onAnalyze(p)
      case PacketType.ChargerState => onChargerState(p)
      case PacketType.ColorChange => onColorChange(p)
      case PacketType.ComputerState => onComputerState(p)
      case PacketType.ComputerUserList => onComputerUserList(p)
      case PacketType.DisassemblerActiveChange => onDisassemblerActiveChange(p)
      case PacketType.FileSystemActivity => onFileSystemActivity(p)
      case PacketType.FloppyChange => onFloppyChange(p)
      case PacketType.HologramClear => onHologramClear(p)
      case PacketType.HologramColor => onHologramColor(p)
      case PacketType.HologramPowerChange => onHologramPowerChange(p)
      case PacketType.HologramScale => onHologramScale(p)
      case PacketType.HologramSet => onHologramSet(p)
      case PacketType.HologramTranslation => onHologramPositionOffsetY(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.PowerState => onPowerState(p)
      case PacketType.RaidStateChange => onRaidStateChange(p)
      case PacketType.RedstoneState => onRedstoneState(p)
      case PacketType.RobotAnimateSwing => onRobotAnimateSwing(p)
      case PacketType.RobotAnimateTurn => onRobotAnimateTurn(p)
      case PacketType.RobotAssemblingState => onRobotAssemblingState(p)
      case PacketType.RobotInventoryChange => onRobotInventoryChange(p)
      case PacketType.RobotLightChange => onRobotLightChange(p)
      case PacketType.RobotMove => onRobotMove(p)
      case PacketType.RobotSelectedSlotChange => onRobotSelectedSlotChange(p)
      case PacketType.RotatableState => onRotatableState(p)
      case PacketType.SwitchActivity => onSwitchActivity(p)
      case PacketType.TextBufferInit => onTextBufferInit(p)
      case PacketType.TextBufferPowerChange => onTextBufferPowerChange(p)
      case PacketType.TextBufferMulti => onTextBufferMulti(p)
      case PacketType.ScreenTouchMode => onScreenTouchMode(p)
      case PacketType.ServerPresence => onServerPresence(p)
      case PacketType.Sound => onSound(p)
      case PacketType.SoundPattern => onSoundPattern(p)
      case _ => // Invalid packet.
    }
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
        t.world.markBlockForUpdate(t.position)
      case _ => // Invalid packet.
    }

  def onColorChange(p: PacketParser) =
    p.readTileEntity[Colored]() match {
      case Some(t) =>
        t.color = EnumDyeColor.byMetadata(p.readInt())
        t.world.markBlockForUpdate(t.position)
      case _ => // Invalid packet.
    }

  def onComputerState(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => t.setRunning(p.readBoolean())
      case Some(t: ServerRack) =>
        val number = p.readInt()
        t.setRunning(number, p.readBoolean())
        t.sides(number) = p.readDirection()
      case _ => // Invalid packet.
    }

  def onComputerUserList(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) =>
        val count = p.readInt()
        t.setUsers((0 until count).map(_ => p.readUTF()))
      case _ => // Invalid packet.
    }

  def onDisassemblerActiveChange(p: PacketParser) =
    p.readTileEntity[Disassembler]() match {
      case Some(t) => t.isActive = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onFileSystemActivity(p: PacketParser) = {
    val sound = p.readUTF()
    val data = CompressedStreamTools.read(p)
    if (p.readBoolean()) p.readTileEntity[net.minecraft.tileentity.TileEntity]() match {
      case Some(t) =>
        MinecraftForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, t, data))
      case _ => // Invalid packet.
    }
    else world(p.player, p.readInt()) match {
      case Some(world) =>
        val x = p.readDouble()
        val y = p.readDouble()
        val z = p.readDouble()
        MinecraftForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, world, x, y, z, data))
      case _ => // Invalid packet.
    }
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

  def onHologramPositionOffsetY(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        val x = p.readDouble()
        val y = p.readDouble()
        val z = p.readDouble()
        t.translation = new Vec3(x, y, z)
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

  def onRaidStateChange(p: PacketParser) =
    p.readTileEntity[Raid]() match {
      case Some(t) =>
        for (slot <- 0 until t.getSizeInventory) {
          t.presence(slot) = p.readBoolean()
        }
      case _ => // Invalid packet.
    }

  def onRedstoneState(p: PacketParser) =
    p.readTileEntity[RedstoneAware]() match {
      case Some(t) =>
        t.isOutputEnabled = p.readBoolean()
        for (d <- EnumFacing.values) {
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
    p.readTileEntity[Assembler]() match {
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

  def onRobotLightChange(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => t.robot.info.lightColor = p.readInt()
      case _ => // Invalid packet.
    }

  def onRobotMove(p: PacketParser) = {
    val dimension = p.readInt()
    val x = p.readInt()
    val y = p.readInt()
    val z = p.readInt()
    val direction = p.readDirection()
    (p.getTileEntity[RobotProxy](dimension, x, y, z), direction) match {
      case (Some(t), Some(d)) => t.robot.move(d)
      case (_, Some(d)) =>
        // Invalid packet, robot may be coming from outside our loaded area.
        PacketSender.sendRobotStateRequest(dimension, x + d.getFrontOffsetX, y + d.getFrontOffsetY, z + d.getFrontOffsetZ)
      case _ => // Invalid packet.
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
        t.pitch = p.readDirection().get
        t.yaw = p.readDirection().get
      case _ => // Invalid packet.
    }

  def onSwitchActivity(p: PacketParser) =
    p.readTileEntity[Switch]() match {
      case Some(t) => t.lastMessage = System.currentTimeMillis()
      case _ => // Invalid packet.
    }

  def onTextBufferPowerChange(p: PacketParser) =
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        buffer.setRenderingEnabled(p.readBoolean())
      case _ => // Invalid packet.
    }

  def onTextBufferInit(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: li.cil.oc.common.component.TextBuffer) =>
        val nbt = p.readNBT()
        if (nbt.hasKey("maxWidth")) {
          val maxWidth = nbt.getInteger("maxWidth")
          val maxHeight = nbt.getInteger("maxHeight")
          buffer.setMaximumResolution(maxWidth, maxHeight)
        }
        buffer.data.load(nbt)
        buffer.proxy.markDirty()
      case _ => // Invalid packet.
    }
  }

  def onTextBufferMulti(p: PacketParser) =
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: component.TextBuffer) =>
        try while (true) {
          p.readPacketType() match {
            case PacketType.TextBufferMultiColorChange => onTextBufferMultiColorChange(p, buffer)
            case PacketType.TextBufferMultiCopy => onTextBufferMultiCopy(p, buffer)
            case PacketType.TextBufferMultiDepthChange => onTextBufferMultiDepthChange(p, buffer)
            case PacketType.TextBufferMultiFill => onTextBufferMultiFill(p, buffer)
            case PacketType.TextBufferMultiPaletteChange => onTextBufferMultiPaletteChange(p, buffer)
            case PacketType.TextBufferMultiResolutionChange => onTextBufferMultiResolutionChange(p, buffer)
            case PacketType.TextBufferMultiMaxResolutionChange => onTextBufferMultiMaxResolutionChange(p, buffer)
            case PacketType.TextBufferMultiSet => onTextBufferMultiSet(p, buffer)
            case _ => // Invalid packet.
          }
        }
        catch {
          case ignored: EOFException => // No more commands.
        }
      case _ => // Invalid packet.
    }

  def onTextBufferMultiColorChange(p: PacketParser, env: component.TextBuffer) {
    env match {
      case buffer: component.TextBuffer =>
        val foreground = p.readInt()
        val foregroundIsPalette = p.readBoolean()
        buffer.setForegroundColor(foreground, foregroundIsPalette)
        val background = p.readInt()
        val backgroundIsPalette = p.readBoolean()
        buffer.setBackgroundColor(background, backgroundIsPalette)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferMultiCopy(p: PacketParser, buffer: component.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val tx = p.readInt()
    val ty = p.readInt()
    buffer.copy(col, row, w, h, tx, ty)
  }

  def onTextBufferMultiDepthChange(p: PacketParser, buffer: component.TextBuffer) {
    buffer.setColorDepth(component.TextBuffer.ColorDepth.values.apply(p.readInt()))
  }

  def onTextBufferMultiFill(p: PacketParser, buffer: component.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val c = p.readChar()
    buffer.fill(col, row, w, h, c)
  }

  def onTextBufferMultiPaletteChange(p: PacketParser, buffer: component.TextBuffer) {
    val index = p.readInt()
    val color = p.readInt()
    buffer.setPaletteColor(index, color)
  }

  def onTextBufferMultiResolutionChange(p: PacketParser, buffer: component.TextBuffer) {
    val w = p.readInt()
    val h = p.readInt()
    buffer.setResolution(w, h)
  }

  def onTextBufferMultiMaxResolutionChange(p: PacketParser, buffer: component.TextBuffer) {
    val w = p.readInt()
    val h = p.readInt()
    buffer.setMaximumResolution(w, h)
  }

  def onTextBufferMultiSet(p: PacketParser, buffer: component.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val s = p.readUTF()
    val vertical = p.readBoolean()
    buffer.set(col, row, s, vertical)
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

  def onSoundPattern(p: PacketParser) {
    val dimension = p.readInt()
    if (world(p.player, dimension).isDefined) {
      val x = p.readInt()
      val y = p.readInt()
      val z = p.readInt()
      val pattern = p.readUTF()
      Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, pattern)
    }
  }
}