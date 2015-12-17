package li.cil.oc.client

import java.io.EOFException

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.event.{NetworkActivityEvent, FileSystemAccessEvent}
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.Loot
import li.cil.oc.common.PacketType
import li.cil.oc.common.container
import li.cil.oc.common.nanomachines.ControllerImpl
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.Audio
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.CompressedStreamTools
import net.minecraftforge.common.MinecraftForge
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
      case PacketType.ContainerUpdate => onContainerUpdate(p)
      case PacketType.DisassemblerActiveChange => onDisassemblerActiveChange(p)
      case PacketType.FileSystemActivity => onFileSystemActivity(p)
      case PacketType.FloppyChange => onFloppyChange(p)
      case PacketType.HologramArea => onHologramArea(p)
      case PacketType.HologramClear => onHologramClear(p)
      case PacketType.HologramColor => onHologramColor(p)
      case PacketType.HologramPowerChange => onHologramPowerChange(p)
      case PacketType.HologramRotation => onHologramRotation(p)
      case PacketType.HologramRotationSpeed => onHologramRotationSpeed(p)
      case PacketType.HologramScale => onHologramScale(p)
      case PacketType.HologramTranslation => onHologramPositionOffsetY(p)
      case PacketType.HologramValues => onHologramValues(p)
      case PacketType.LootDisk => onLootDisk(p)
      case PacketType.NanomachinesConfiguration => onNanomachinesConfiguration(p)
      case PacketType.NanomachinesInputs => onNanomachinesInputs(p)
      case PacketType.NanomachinesPower => onNanomachinesPower(p)
      case PacketType.NetSplitterState => onNetSplitterState(p)
      case PacketType.NetworkActivity => onNetworkActivity(p)
      case PacketType.ParticleEffect => onParticleEffect(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.PowerState => onPowerState(p)
      case PacketType.PrinterState => onPrinterState(p)
      case PacketType.RackInventory => onRackInventory(p)
      case PacketType.RackMountableData => onRackMountableData(p)
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
      case PacketType.Sound => onSound(p)
      case PacketType.SoundPattern => onSoundPattern(p)
      case PacketType.TransposerActivity => onTransposerActivity(p)
      case PacketType.WaypointLabel => onWaypointLabel(p)
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
        t.world.markBlockForUpdate(t.position)
      case _ => // Invalid packet.
    }

  def onColorChange(p: PacketParser) =
    p.readTileEntity[Colored]() match {
      case Some(t) =>
        t.color = p.readInt()
        t.world.markBlockForUpdate(t.position)
      case _ => // Invalid packet.
    }

  def onComputerState(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) =>
        t.setRunning(p.readBoolean())
        t.hasErrored = p.readBoolean()
      case _ => // Invalid packet.
    }

  def onComputerUserList(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) =>
        val count = p.readInt()
        t.setUsers((0 until count).map(_ => p.readUTF()))
      case _ => // Invalid packet.
    }

  def onContainerUpdate(p: PacketParser) = {
    val windowId = p.readUnsignedByte()
    if (p.player.openContainer != null && p.player.openContainer.windowId == windowId) {
      p.player.openContainer match {
        case container: container.Player => container.updateCustomData(p.readNBT())
        case _ => // Invalid packet.
      }
    }
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

  def onNetworkActivity(p: PacketParser) = {
    val data = CompressedStreamTools.read(p)
    if (p.readBoolean()) p.readTileEntity[net.minecraft.tileentity.TileEntity]() match {
      case Some(t) =>
        MinecraftForge.EVENT_BUS.post(new NetworkActivityEvent.Client(t, data))
      case _ => // Invalid packet.
    }
    else world(p.player, p.readInt()) match {
      case Some(world) =>
        val x = p.readDouble()
        val y = p.readDouble()
        val z = p.readDouble()
        MinecraftForge.EVENT_BUS.post(new NetworkActivityEvent.Client(world, x, y, z, data))
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
        for (i <- t.volume.indices) t.volume(i) = 0
        t.needsRendering = true
      case _ => // Invalid packet.
    }

  def onHologramColor(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        val index = p.readInt()
        val value = p.readInt()
        t.colors(index) = value & 0xFFFFFF
        t.needsRendering = true
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

  def onHologramArea(p: PacketParser) =
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
        t.needsRendering = true
      case _ => // Invalid packet.
    }

  def onHologramValues(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        val count = p.readInt()
        for (i <- 0 until count) {
          val xz = p.readShort()
          val x = (xz >> 8).toByte
          val z = xz.toByte
          t.volume(x + z * t.width) = p.readInt()
          t.volume(x + z * t.width + t.width * t.width) = p.readInt()
        }
        t.needsRendering = true
      case _ => // Invalid packet.
    }

  def onHologramPositionOffsetY(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        t.translation.xCoord = p.readDouble()
        t.translation.yCoord = p.readDouble()
        t.translation.zCoord = p.readDouble()
      case _ => // Invalid packet.
    }

  def onHologramRotation(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        t.rotationAngle = p.readFloat()
        t.rotationX = p.readFloat()
        t.rotationY = p.readFloat()
        t.rotationZ = p.readFloat()
      case _ => // Invalid packet.
    }

  def onHologramRotationSpeed(p: PacketParser) =
    p.readTileEntity[Hologram]() match {
      case Some(t) =>
        t.rotationSpeed = p.readFloat()
        t.rotationSpeedX = p.readFloat()
        t.rotationSpeedY = p.readFloat()
        t.rotationSpeedZ = p.readFloat()
      case _ => // Invalid packet.
    }

  def onLootDisk(p: PacketParser) = {
    val stack = p.readItemStack()
    if (stack != null) {
      Loot.disksForClient += stack
    }
  }

  def onNanomachinesConfiguration(p: PacketParser) = {
    p.readEntity[EntityPlayer]() match {
      case Some(player) =>
        val hasController = p.readBoolean()
        if (hasController) {
          api.Nanomachines.installController(player) match {
            case controller: ControllerImpl => controller.load(p.readNBT())
            case _ => // Wat.
          }
        }
        else {
          api.Nanomachines.uninstallController(player)
        }
      case _ => // Invalid packet.
    }
  }

  def onNanomachinesInputs(p: PacketParser) = {
    p.readEntity[EntityPlayer]() match {
      case Some(player) => api.Nanomachines.getController(player) match {
        case controller: ControllerImpl =>
          val inputs = new Array[Byte](p.readInt())
          p.read(inputs)
          controller.configuration.synchronized {
            for ((value, index) <- inputs.zipWithIndex if index < controller.configuration.triggers.length) {
              controller.configuration.triggers(index).isActive = value == 1
            }
            controller.activeBehaviorsDirty = true
          }
        case _ => // Wat.
      }
      case _ => // Invalid packet.
    }
  }

  def onNanomachinesPower(p: PacketParser) = {
    p.readEntity[EntityPlayer]() match {
      case Some(player) => api.Nanomachines.getController(player) match {
        case controller: ControllerImpl => controller.storedEnergy = p.readDouble()
        case _ => // Wat.
      }
      case _ => // Invalid packet.
    }
  }

  def onNetSplitterState(p: PacketParser) =
    p.readTileEntity[NetSplitter]() match {
      case Some(t) =>
        t.isInverted = p.readBoolean()
        t.openSides = t.uncompressSides(p.readByte())
        t.world.markBlockForUpdate(t.x, t.y, t.z)
      case _ => // Invalid packet.
    }

  def onParticleEffect(p: PacketParser) = {
    val dimension = p.readInt()
    world(p.player, dimension) match {
      case Some(world) =>
        val x = p.readInt()
        val y = p.readInt()
        val z = p.readInt()
        val velocity = p.readDouble()
        val direction = p.readDirection()
        val name = p.readUTF()
        val count = p.readUnsignedByte()

        for (i <- 0 until count) {
          def rv(f: ForgeDirection => Int) = direction match {
            case Some(d) => world.rand.nextFloat - 0.5 + f(d) * 0.5
            case _ => world.rand.nextFloat * 2.0 - 1
          }
          val vx = rv(_.offsetX)
          val vy = rv(_.offsetY)
          val vz = rv(_.offsetZ)
          if (vx * vx + vy * vy + vz * vz < 1) {
            def rp(x: Int, v: Double, f: ForgeDirection => Int) = direction match {
              case Some(d) => x + 0.5 + v * velocity * 0.5 + f(d) * velocity
              case _ => x + 0.5 + v * velocity
            }
            val px = rp(x, vx, _.offsetX)
            val py = rp(y, vy, _.offsetY)
            val pz = rp(z, vz, _.offsetZ)
            world.spawnParticle(name, px, py, pz, vx, vy + velocity * 0.25, vz)
          }
        }
      case _ => // Invalid packet.
    }
  }

  def onPetVisibility(p: PacketParser) {
    if (!PetRenderer.isInitialized) {
      PetRenderer.isInitialized = true
      if (Settings.get.hideOwnPet) {
        PetRenderer.hidden += Minecraft.getMinecraft.thePlayer.getCommandSenderName
      }
      PacketSender.sendPetVisibility()
    }

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

  def onPrinterState(p: PacketParser) =
    p.readTileEntity[Printer]() match {
      case Some(t) =>
        if (p.readBoolean()) t.requiredEnergy = 9001
        else t.requiredEnergy = 0
      case _ => // Invalid packet.
    }

  def onRackInventory(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) =>
        val count = p.readInt()
        for (_ <- 0 until count) {
          val slot = p.readInt()
          t.setInventorySlotContents(slot, p.readItemStack())
        }
      case _ => // Invalid packet.
    }

  def onRackMountableData(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) =>
        val mountableIndex = p.readInt()
        t.lastData(mountableIndex) = p.readNBT()
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
        PacketSender.sendRobotStateRequest(dimension, x + d.offsetX, y + d.offsetY, z + d.offsetZ)
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
    p.readTileEntity[traits.SwitchLike]() match {
      case Some(t) => t.lastMessage = System.currentTimeMillis()
      case _ => // Invalid packet.
    }

  def onTextBufferPowerChange(p: PacketParser) =
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) =>
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
        buffer.markInitialized()
      case _ => // Invalid packet.
    }
  }

  def onTextBufferMulti(p: PacketParser) =
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) =>
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
            case PacketType.TextBufferMultiRawSetText => onTextBufferMultiRawSetText(p, buffer)
            case PacketType.TextBufferMultiRawSetBackground => onTextBufferMultiRawSetBackground(p, buffer)
            case PacketType.TextBufferMultiRawSetForeground => onTextBufferMultiRawSetForeground(p, buffer)
            case _ => // Invalid packet.
          }
        }
        catch {
          case ignored: EOFException => // No more commands.
        }
      case _ => // Invalid packet.
    }

  def onTextBufferMultiColorChange(p: PacketParser, env: api.internal.TextBuffer) {
    env match {
      case buffer: api.internal.TextBuffer =>
        val foreground = p.readInt()
        val foregroundIsPalette = p.readBoolean()
        buffer.setForegroundColor(foreground, foregroundIsPalette)
        val background = p.readInt()
        val backgroundIsPalette = p.readBoolean()
        buffer.setBackgroundColor(background, backgroundIsPalette)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferMultiCopy(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val tx = p.readInt()
    val ty = p.readInt()
    buffer.copy(col, row, w, h, tx, ty)
  }

  def onTextBufferMultiDepthChange(p: PacketParser, buffer: api.internal.TextBuffer) {
    buffer.setColorDepth(api.internal.TextBuffer.ColorDepth.values.apply(p.readInt()))
  }

  def onTextBufferMultiFill(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val c = p.readChar()
    buffer.fill(col, row, w, h, c)
  }

  def onTextBufferMultiPaletteChange(p: PacketParser, buffer: api.internal.TextBuffer) {
    val index = p.readInt()
    val color = p.readInt()
    buffer.setPaletteColor(index, color)
  }

  def onTextBufferMultiResolutionChange(p: PacketParser, buffer: api.internal.TextBuffer) {
    val w = p.readInt()
    val h = p.readInt()
    buffer.setResolution(w, h)
  }

  def onTextBufferMultiMaxResolutionChange(p: PacketParser, buffer: api.internal.TextBuffer) {
    val w = p.readInt()
    val h = p.readInt()
    buffer.setMaximumResolution(w, h)
  }

  def onTextBufferMultiSet(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()
    val s = p.readUTF()
    val vertical = p.readBoolean()
    buffer.set(col, row, s, vertical)
  }

  def onTextBufferMultiRawSetText(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()

    val rows = p.readShort()
    val text = new Array[Array[Char]](rows)
    for (y <- 0 until rows) {
      val cols = p.readShort()
      val line = new Array[Char](cols)
      for (x <- 0 until cols) {
        line(x) = p.readChar()
      }
      text(y) = line
    }

    buffer.rawSetText(col, row, text)
  }

  def onTextBufferMultiRawSetBackground(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()

    val rows = p.readShort()
    val color = new Array[Array[Int]](rows)
    for (y <- 0 until rows) {
      val cols = p.readShort()
      val line = new Array[Int](cols)
      for (x <- 0 until cols) {
        line(x) = p.readInt()
      }
      color(y) = line
    }

    buffer.rawSetBackground(col, row, color)
  }

  def onTextBufferMultiRawSetForeground(p: PacketParser, buffer: api.internal.TextBuffer) {
    val col = p.readInt()
    val row = p.readInt()

    val rows = p.readShort()
    val color = new Array[Array[Int]](rows)
    for (y <- 0 until rows) {
      val cols = p.readShort()
      val line = new Array[Int](cols)
      for (x <- 0 until cols) {
        line(x) = p.readInt()
      }
      color(y) = line
    }

    buffer.rawSetForeground(col, row, color)
  }

  def onScreenTouchMode(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => t.invertTouchMode = p.readBoolean()
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

  def onTransposerActivity(p: PacketParser) =
    p.readTileEntity[Transposer]() match {
      case Some(transposer) => transposer.lastOperation = System.currentTimeMillis()
      case _ => // Invalid packet.
    }

  def onWaypointLabel(p: PacketParser) =
    p.readTileEntity[Waypoint]() match {
      case Some(waypoint) => waypoint.label = p.readUTF()
      case _ => // Invalid packet.
    }
}