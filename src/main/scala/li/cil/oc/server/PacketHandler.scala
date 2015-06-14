package li.cil.oc.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Machine
import li.cil.oc.common.Achievement
import li.cil.oc.common.PacketType
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.Computer
import li.cil.oc.common.tileentity.traits.TileEntity
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.integration.fmp.EventHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetHandlerPlayServer
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.ForgeDirection

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
      case PacketType.DronePower => onDronePower(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.MouseUp => onMouseUp(p)
      case PacketType.MultiPartPlace => onMultiPartPlace(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.RobotAssemblerStart => onRobotAssemblerStart(p)
      case PacketType.RobotStateRequest => onRobotStateRequest(p)
      case PacketType.ServerRange => onServerRange(p)
      case PacketType.ServerSide => onServerSide(p)
      case PacketType.ServerSwitchMode => onServerSwitchMode(p)
      case PacketType.TextBufferInit => onTextBufferInit(p)
      case PacketType.WaypointLabel => onWaypointLabel(p)
      case _ => // Invalid packet.
    }
  }

  def onComputerPower(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => p.player match {
        case player: EntityPlayerMP => trySetComputerPower(t.machine, p.readBoolean(), player)
        case _ =>
      }
      case Some(r: ServerRack) => r.servers(p.readInt()) match {
        case Some(server) => p.player match {
          case player: EntityPlayerMP => trySetComputerPower(server.machine, p.readBoolean(), player)
          case _ =>
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
      case Some(buffer: api.component.TextBuffer) => buffer.keyDown(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onKeyUp(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.keyUp(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onClipboard(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.clipboard(p.readUTF(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseClick(p: PacketParser) {
    ComponentTracker.get(p.player.worldObj, p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) =>
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
      case Some(buffer: api.component.TextBuffer) =>
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
      case Some(buffer: api.component.TextBuffer) =>
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

  def onServerRange(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          rack.range = math.min(math.max(0, p.readInt()), Settings.get.maxWirelessRange).toInt
          PacketSender.sendServerState(rack)
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onServerSide(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          val number = p.readInt()
          val side = p.readDirection()
          if (rack.sides(number) != side && side != Option(ForgeDirection.SOUTH) && (!rack.sides.contains(side) || side.isEmpty)) {
            rack.sides(number) = side
            rack.servers(number) match {
              case Some(server) => rack.reconnectServer(number, server)
              case _ =>
            }
            PacketSender.sendServerState(rack, number)
          }
          else PacketSender.sendServerState(rack, number, Some(player))
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onServerSwitchMode(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          rack.internalSwitch = p.readBoolean()
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onTextBufferInit(p: PacketParser) {
    val address = p.readUTF()
    p.player match {
      case entity: EntityPlayerMP =>
        ComponentTracker.get(p.player.worldObj, address) match {
          case Some(buffer: TextBuffer) =>
            val nbt = new NBTTagCompound()
            buffer.data.save(nbt)
            nbt.setInteger("maxWidth", buffer.getMaximumWidth)
            nbt.setInteger("maxHeight", buffer.getMaximumHeight)
            PacketSender.sendTextBufferInit(address, nbt, entity)
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