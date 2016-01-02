package li.cil.oc.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent
import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.api.internal.Server
import li.cil.oc.api.machine.Machine
import li.cil.oc.common.Achievement
import li.cil.oc.common.PacketType
import li.cil.oc.common.component.TextBuffer
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

object PacketHandler extends CommonPacketHandler {
  @SubscribeEvent
  def onPacket(e: ServerCustomPacketEvent) =
    onPacketData(e.packet.payload, e.handler.asInstanceOf[NetHandlerPlayServer].playerEntity)

  override protected def world(player: EntityPlayer, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  override def dispatch(p: PacketParser) {
    p.packetType match {
      case PacketType.ComputerPower => onComputerPower(p)
      case PacketType.CopyToAnalyzer => onCopyToAnalyzer(p)
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

  def onComputerPower(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) => p.player match {
        case player: EntityPlayerMP => trySetComputerPower(t.machine, p.readBoolean(), player)
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onServerPower(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) =>
        val mountableIndex = p.readInt()
        t.getMountable(mountableIndex) match {
          case server: Server => p.player match {
            case player: EntityPlayerMP => trySetComputerPower(server.machine, p.readBoolean(), player)
            case _ => // Invalid packet.
          }
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }

  def onCopyToAnalyzer(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: TextBuffer) => buffer.copyToAnalyzer(p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onDriveMode(p: PacketParser) = p.player match {
    case player: EntityPlayerMP =>
      Delegator.subItem(player.getHeldItem) match {
        case Some(drive: FileSystemLike) =>
          val data = new DriveData(player.getHeldItem)
          data.isUnmanaged = p.readBoolean()
          data.save(player.getHeldItem)
        case _ => // Invalid packet.
      }
    case _ => // Invalid packet.
  }

  def onDronePower(p: PacketParser) =
    p.readEntity[Drone]() match {
      case Some(drone) => p.player match {
        case player: EntityPlayerMP =>
          val power = p.readBoolean()
          if (power) {
            drone.preparePowerUp()
          }
          trySetComputerPower(drone.machine, power, player)
        case _ =>
      }
      case _ => // Invalid packet.
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

  def onKeyDown(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyDown(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onKeyUp(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyUp(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onClipboard(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.clipboard(p.readUTF(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseClick(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val x = p.readFloat()
        val y = p.readFloat()
        val dragging = p.readBoolean()
        val button = p.readByte()
        val player = p.player.asInstanceOf[EntityPlayer]
        if (dragging) buffer.mouseDrag(x, y, button, player)
        else buffer.mouseDown(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMouseUp(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val x = p.readFloat()
        val y = p.readFloat()
        val button = p.readByte()
        val player = p.player.asInstanceOf[EntityPlayer]
        buffer.mouseUp(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMouseScroll(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val x = p.readFloat()
        val y = p.readFloat()
        val button = p.readByte()
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
    p.player match {
      case player: EntityPlayerMP =>
        if (if (p.readBoolean()) {
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

  def onRackMountableMapping(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) => p.player match {
        case player: EntityPlayerMP if t.isUseableByPlayer(player) =>
          val mountableIndex = p.readInt()
          val nodeIndex = p.readInt()
          val side = p.readDirection()
          t.connect(mountableIndex, nodeIndex, side)
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onRackRelayState(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(t) => p.player match {
        case player: EntityPlayerMP if t.isUseableByPlayer(player) =>
          t.isRelayEnabled = p.readBoolean()
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onRobotAssemblerStart(p: PacketParser) =
    p.readTileEntity[Assembler]() match {
      case Some(assembler) =>
        if (assembler.start(p.player match {
          case player: EntityPlayerMP => player.capabilities.isCreativeMode
          case _ => false
        })) assembler.output.foreach(stack => Achievement.onAssemble(stack, p.player))
      case _ => // Invalid packet.
    }

  def onRobotStateRequest(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(proxy) => proxy.world.markBlockForUpdate(proxy.x, proxy.y, proxy.z)
      case _ => // Invalid packet.
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
              PacketSender.sendTextBufferInit(address, nbt, entity)
            }
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }
  }

  def onWaypointLabel(p: PacketParser) =
    p.readTileEntity[Waypoint]() match {
      case Some(waypoint) => p.player match {
        case player: EntityPlayerMP if player.getDistanceSq(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5) <= 64 =>
          val label = p.readUTF().take(32)
          if (label != waypoint.label) {
            waypoint.label = label
            PacketSender.sendWaypointLabel(waypoint)
          }
        case _ =>
      }
      case _ => // Invalid packet.
    }
}