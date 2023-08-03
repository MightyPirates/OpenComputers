package li.cil.oc.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.internal.Server
import li.cil.oc.api.machine.Machine
import li.cil.oc.common.Achievement
import li.cil.oc.common.PacketType
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.container
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.common.item.traits.FileSystemLike
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.Computer
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.integration.fmp.EventHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetHandlerPlayServer
import net.minecraftforge.common.DimensionManager
import org.apache.logging.log4j.MarkerManager

object PacketHandler extends CommonPacketHandler {
  private val securityMarker = MarkerManager.getMarker("SuspiciousPackets")

  private def logForgedPacket(player: EntityPlayerMP) =
    OpenComputers.log.warn(securityMarker, "Player {} tried to send GUI packets without opening them", player.getGameProfile)

  @SubscribeEvent
  def onPacket(e: ServerCustomPacketEvent) =
    onPacketData(e.packet.payload, e.handler.asInstanceOf[NetHandlerPlayServer].playerEntity)

  override protected def world(player: EntityPlayer, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  override def dispatch(p: PacketParser) {
    p.packetType match {
      case PacketType.ComputerPower => onComputerPower(p)
      case PacketType.CopyToAnalyzer => onCopyToAnalyzer(p)
      case PacketType.DriveLock => onDriveLock(p)
      case PacketType.DriveMode => onDriveMode(p)
      case PacketType.DronePower => onDronePower(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.MouseUp => onMouseUp(p)
      case PacketType.MultiPartPlace => onMultiPartPlace(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.RackMountableMapping => onRackMountableMapping(p)
      case PacketType.RackRelayState => onRackRelayState(p)
      case PacketType.RobotAssemblerStart => onRobotAssemblerStart(p)
      case PacketType.RobotStateRequest => onRobotStateRequest(p)
      case PacketType.ServerPower => onServerPower(p)
      case PacketType.TextBufferInit => onTextBufferInit(p)
      case PacketType.WaypointLabel => onWaypointLabel(p)
      case _ => // Invalid packet.
    }
  }

  def onComputerPower(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Computer]()
    val setPower = p.readBoolean()
    p.player match {
      case player: EntityPlayerMP => player.openContainer match {
        case container: container.Player => (container.otherInventory, entity) match {
          case (computer: Computer, Some(c2)) if c2.position == computer.position =>
            trySetComputerPower(computer.machine, setPower, player)
          case _ => logForgedPacket(player)
        }
        case _ => logForgedPacket(player)
      }
      case _ => // Invalid packet.
    }
  }

  def onServerPower(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Rack]()
    val index = p.readInt()
    val readServer = entity match {
      case Some(t) =>
        t.getMountable(index) match {
          case server: Server => server
          case _ => return  // probably just lag, not invalid packet
        }
      case _ => return
    }
    val setPower = p.readBoolean()
    p.player match {
      case player: EntityPlayerMP => player.openContainer match {
        case container: container.Server => container.server match {
          case Some(server) if server == readServer =>
            trySetComputerPower(server.machine, setPower, player)
          case _ => logForgedPacket(player)
        }
        case _ => logForgedPacket(player)
      }
      case _ => // Invalid packet.
    }
  }

  def onCopyToAnalyzer(p: PacketParser) {
    val text = p.readUTF()
    val line = p.readInt()
    ComponentTracker.get(p.player.worldObj, text) match {
      case Some(buffer: TextBuffer) => buffer.copyToAnalyzer(line, p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onDriveLock(p: PacketParser): Unit = p.player match {
    case player: EntityPlayerMP =>
      val heldItem = player.getHeldItem
      Delegator.subItem(heldItem) match {
        case Some(drive: FileSystemLike) => DriveData.lock(heldItem, player)
        case _ => // Invalid packet
      }
    case _ => // Invalid Packet
  }

  def onDriveMode(p: PacketParser): Unit = {
    val unmanaged = p.readBoolean()
    p.player match {
      case player: EntityPlayerMP =>
        val heldItem = player.getHeldItem
        Delegator.subItem(heldItem) match {
          case Some(drive: FileSystemLike) => DriveData.setUnmanaged(heldItem, unmanaged)
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }
  }

  def onDronePower(p: PacketParser): Unit = {
    val entity = p.readEntity[Drone]()
    val power = p.readBoolean()
    p.player match {
      case player: EntityPlayerMP => (player.openContainer, entity) match {
        case (c: container.Drone, Some(readDrone)) if c.drone == readDrone =>
          val drone = c.drone
          if (power) {
            drone.preparePowerUp()
          }
          trySetComputerPower(drone.machine, power, player)
        case _ => logForgedPacket(player)
      }
      case _ =>
    }
  }

  private def trySetComputerPower(computer: Machine, value: Boolean, player: EntityPlayerMP) {
    if (computer.canInteract(player.getCommandSenderName)) {
      if (value) {
        if (!computer.isPaused) {
          computer.start()
          computer.lastError match {
            case message if message != null => player.addChatMessage(Localization.Analyzer.LastError(message))
            case _ =>
          }
        }
      }
      else computer.stop()
    }
  }

  def onKeyDown(p: PacketParser): Unit = {
    val address = p.readUTF()
    val key = p.readChar()
    val code = p.readInt()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyDown(key, code, p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onKeyUp(p: PacketParser): Unit = {
    val address = p.readUTF()
    val key = p.readChar()
    val code = p.readInt()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyUp(key, code, p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onClipboard(p: PacketParser): Unit = {
    val address = p.readUTF()
    val copy = p.readUTF()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.clipboard(copy, p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseClick(p: PacketParser) {
    val address = p.readUTF()
    val x = p.readFloat()
    val y = p.readFloat()
    val dragging = p.readBoolean()
    val button = p.readByte()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[EntityPlayer]
        if (dragging) buffer.mouseDrag(x, y, button, player)
        else buffer.mouseDown(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMouseUp(p: PacketParser) {
    val address = p.readUTF()
    val x = p.readFloat()
    val y = p.readFloat()
    val button = p.readByte()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[EntityPlayer]
        buffer.mouseUp(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMouseScroll(p: PacketParser) {
    val address = p.readUTF()
    val x = p.readFloat()
    val y = p.readFloat()
    val button = p.readByte()
    ComponentTracker.get(p.player.worldObj, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[EntityPlayer]
        buffer.mouseScroll(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMultiPartPlace(p: PacketParser) {
    p.player match {
      case player: EntityPlayerMP => EventHandler.place(player)
      case _ => // Invalid packet.
    }
  }

  def onPetVisibility(p: PacketParser) {
    val value = p.readBoolean()
    p.player match {
      case player: EntityPlayerMP =>
        if (if (value) {
          PetVisibility.hidden.remove(player.getCommandSenderName)
        }
        else {
          PetVisibility.hidden.add(player.getCommandSenderName)
        }) {
          // Something changed.
          PacketSender.sendPetVisibility(Some(player.getCommandSenderName))
        }
      case _ => // Invalid packet.
    }
  }

  def onRackMountableMapping(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Rack]()
    val mountableIndex = p.readInt()
    val nodeIndex = p.readInt()
    val side = p.readDirection()
    p.player match {
      case player: EntityPlayerMP => (player.openContainer, entity) match {
        case (container: container.Rack, Some(readRack)) if readRack == container.rack  =>
          if (container.rack.isUseableByPlayer(player))
            container.rack.connect(mountableIndex, nodeIndex - 1, side)
        case _ => logForgedPacket(player)
      }
      case _ =>
    }
  }

  def onRackRelayState(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Rack]()
    val enabled = p.readBoolean()
    entity match {
      case Some(t) => p.player match {
        case player: EntityPlayerMP if t.isUseableByPlayer(player) =>
          t.isRelayEnabled = enabled
        case _ =>
      }
      case _ => // Invalid packet.
    }
  }

  def onRobotAssemblerStart(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Assembler]()
    entity match {
      case Some(assembler) =>
        if (assembler.start(p.player match {
          case player: EntityPlayerMP => player.capabilities.isCreativeMode
          case _ => false
        })) assembler.output.foreach(stack => Achievement.onAssemble(stack, p.player))
      case _ => // Invalid packet.
    }
  }

  def onRobotStateRequest(p: PacketParser): Unit = {
    p.readTileEntity[RobotProxy]() match {
      case Some(proxy) => proxy.world.markBlockForUpdate(proxy.x, proxy.y, proxy.z)
      case _ => // Invalid packet.
    }
  }

  def onTextBufferInit(p: PacketParser) {
    val address = p.readUTF()
    p.player match {
      case entity: EntityPlayerMP =>
        ComponentTracker.get(p.player.worldObj, address) match {
          case Some(buffer: TextBuffer) =>
            if (buffer.host match {
              case screen: Screen if !screen.isOrigin => false
              case _ => true
            }) {
              val nbt = new NBTTagCompound()
              buffer.data.save(nbt)
              nbt.setInteger("maxWidth", buffer.getMaximumWidth)
              nbt.setInteger("maxHeight", buffer.getMaximumHeight)
              nbt.setInteger("viewportWidth", buffer.getViewportWidth)
              nbt.setInteger("viewportHeight", buffer.getViewportHeight)
              PacketSender.sendTextBufferInit(address, nbt, entity)
            }
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }
  }

  def onWaypointLabel(p: PacketParser): Unit = {
    val entity = p.readTileEntity[Waypoint]()
    val label = p.readUTF().take(32)
    entity match {
      case Some(waypoint) => p.player match {
        case player: EntityPlayerMP if player.getDistanceSq(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5) <= 64 =>
          if (label != waypoint.label) {
            waypoint.label = label
            PacketSender.sendWaypointLabel(waypoint)
          }
        case _ =>
      }
      case _ => // Invalid packet.
    }
  }
}