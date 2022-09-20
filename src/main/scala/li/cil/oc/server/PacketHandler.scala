package li.cil.oc.server

import java.io.InputStream

import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.api.internal.Server
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.network.Connector
import li.cil.oc.common.Achievement
import li.cil.oc.common.PacketType
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.container
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.entity.DroneInventory
import li.cil.oc.common.item.{Tablet, TabletWrapper}
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.common.item.traits.FileSystemLike
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.Computer
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Hand
import net.minecraft.util.RegistryKey
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.server.ServerLifecycleHooks

object PacketHandler extends CommonPacketHandler {
  override protected def world(player: PlayerEntity, dimension: ResourceLocation): Option[World] =
    Option(ServerLifecycleHooks.getCurrentServer.getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)))

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
      case PacketType.MachineItemStateRequest => onMachineItemStateRequest(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.MouseUp => onMouseUp(p)
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
    val containerId = p.readInt()
    val setPower = p.readBoolean()
    p.player.containerMenu match {
      case computer: container.Case if computer.containerId == containerId => {
        (computer.otherInventory, p.player) match {
          case (te: Computer, player: ServerPlayerEntity) => trySetComputerPower(te.machine, setPower, player)
          case _ =>
        }
      }
      case robot: container.Robot if robot.containerId == containerId => {
        (robot.otherInventory, p.player) match {
          case (te: Computer, player: ServerPlayerEntity) => trySetComputerPower(te.machine, setPower, player)
          case _ =>
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  def onServerPower(p: PacketParser): Unit = {
    val containerId = p.readInt()
    val index = p.readInt()
    val setPower = p.readBoolean()
    p.player.containerMenu match {
      case server: container.Server if server.containerId == containerId => {
        (server.otherInventory, p.player) match {
          case (comp: component.Server, player: ServerPlayerEntity) if comp.rack.getMountable(index) == comp =>
            trySetComputerPower(comp.machine, setPower, player)
          case _ => // Invalid packet.
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  def onCopyToAnalyzer(p: PacketParser) {
    val text = p.readUTF()
    val line = p.readInt()
    ComponentTracker.get(p.player.level, text) match {
      case Some(buffer: TextBuffer) => buffer.copyToAnalyzer(line, p.player.asInstanceOf[PlayerEntity])
      case _ => // Invalid Packet
    }
  }

  def onDriveLock(p: PacketParser): Unit = p.player match {
    case player: ServerPlayerEntity => {
      val heldItem = player.getItemInHand(Hand.MAIN_HAND)
      heldItem.getItem match {
        case drive: FileSystemLike => DriveData.lock(heldItem, player)
        case _ => // Invalid packet
      }
    }
    case _ => // Invalid Packet
  }

  def onDriveMode(p: PacketParser): Unit = {
    val unmanaged = p.readBoolean()
    p.player match {
      case player: ServerPlayerEntity =>
        val heldItem = player.getItemInHand(Hand.MAIN_HAND)
        heldItem.getItem match {
          case drive: FileSystemLike => DriveData.setUnmanaged(heldItem, unmanaged)
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }
  }

  def onDronePower(p: PacketParser): Unit = {
    val containerId = p.readInt()
    val power = p.readBoolean()
    p.player.containerMenu match {
      case drone: container.Drone if drone.containerId == containerId => {
        (drone.otherInventory, p.player) match {
          case (droneInv: DroneInventory, player: ServerPlayerEntity) => trySetComputerPower(droneInv.drone.machine, power, player)
          case _ =>
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  private def trySetComputerPower(computer: Machine, value: Boolean, player: ServerPlayerEntity) {
    if (computer.canInteract(player.getName.getString)) {
      if (value) {
        if (!computer.isPaused) {
          computer.start()
          computer.lastError match {
            case message if message != null => player.sendMessage(Localization.Analyzer.LastError(message), Util.NIL_UUID)
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
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyDown(key, code, p.player.asInstanceOf[PlayerEntity])
      case _ => // Invalid Packet
    }
  }

  def onKeyUp(p: PacketParser): Unit = {
    val address = p.readUTF()
    val key = p.readChar()
    val code = p.readInt()
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.keyUp(key, code, p.player.asInstanceOf[PlayerEntity])
      case _ => // Invalid Packet
    }
  }

  def onClipboard(p: PacketParser): Unit = {
    val address = p.readUTF()
    val copy = p.readUTF()
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) => buffer.clipboard(copy, p.player.asInstanceOf[PlayerEntity])
      case _ => // Invalid Packet
    }
  }

  def onMouseClick(p: PacketParser) {
    val address = p.readUTF()
    val x = p.readFloat()
    val y = p.readFloat()
    val dragging = p.readBoolean()
    val button = p.readByte()
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[PlayerEntity]
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
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[PlayerEntity]
        buffer.mouseUp(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onMouseScroll(p: PacketParser) {
    val address = p.readUTF()
    val x = p.readFloat()
    val y = p.readFloat()
    val button = p.readByte()
    ComponentTracker.get(p.player.level, address) match {
      case Some(buffer: api.internal.TextBuffer) =>
        val player = p.player.asInstanceOf[PlayerEntity]
        buffer.mouseScroll(x, y, button, player)
      case _ => // Invalid Packet
    }
  }

  def onPetVisibility(p: PacketParser) {
    val value = p.readBoolean()
    p.player match {
      case player: ServerPlayerEntity =>
        if (if (value) {
          PetVisibility.hidden.remove(player.getName.getString)
        }
        else {
          PetVisibility.hidden.add(player.getName.getString)
        }) {
          // Something changed.
          PacketSender.sendPetVisibility(Some(player.getName.getString))
        }
      case _ => // Invalid packet.
    }
  }

  def onRackMountableMapping(p: PacketParser): Unit = {
    val containerId = p.readInt()
    val mountableIndex = p.readInt()
    val nodeIndex = p.readInt()
    val side = p.readDirection()
    p.player.containerMenu match {
      case rack: container.Rack if rack.containerId == containerId => {
        (rack.otherInventory, p.player) match {
          case (t: Rack, player: ServerPlayerEntity) if t.stillValid(player) =>
            t.connect(mountableIndex, nodeIndex, side)
          case _ =>
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  def onRackRelayState(p: PacketParser): Unit = {
    val containerId = p.readInt()
    val enabled = p.readBoolean()
    p.player.containerMenu match {
      case rack: container.Rack if rack.containerId == containerId => {
        (rack.otherInventory, p.player) match {
          case (t: Rack, player: ServerPlayerEntity) if t.stillValid(player) =>
          t.isRelayEnabled = enabled
          case _ =>
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  def onRobotAssemblerStart(p: PacketParser): Unit = {
    val containerId = p.readInt()
    p.player.containerMenu match {
      case assembler: container.Assembler if assembler.containerId == containerId => {
        assembler.assembler match {
          case te: Assembler =>
            if (te.start(p.player match {
              case player: ServerPlayerEntity => player.isCreative
              case _ => false
            })) te.output.foreach(stack => Achievement.onAssemble(stack, p.player))
          case _ =>
        }
      }
      case _ => // Invalid packet or container closed early.
    }
  }

  def onRobotStateRequest(p: PacketParser): Unit = {
    p.readBlockEntity[RobotProxy]() match {
      case Some(proxy) => proxy.world.sendBlockUpdated(proxy.getBlockPos, proxy.world.getBlockState(proxy.getBlockPos), proxy.world.getBlockState(proxy.getBlockPos), 3)
      case _ => // Invalid packet.
    }
  }

  def onMachineItemStateRequest(p: PacketParser): Unit = p.player match {
    case player: ServerPlayerEntity => {
      val stack = p.readItemStack()
      PacketSender.sendMachineItemState(player, stack, Tablet.get(stack, p.player).machine.isRunning)
    }
    case _ => // ignore
  }

  def onTextBufferInit(p: PacketParser) {
    val address = p.readUTF()
    p.player match {
      case entity: ServerPlayerEntity =>
        ComponentTracker.get(p.player.level, address) match {
          case Some(buffer: TextBuffer) =>
            if (buffer.host match {
              case screen: Screen if !screen.isOrigin => false
              case _ => true
            }) {
              val nbt = new CompoundNBT()
              buffer.data.saveData(nbt)
              nbt.putInt("maxWidth", buffer.getMaximumWidth)
              nbt.putInt("maxHeight", buffer.getMaximumHeight)
              nbt.putInt("viewportWidth", buffer.getViewportWidth)
              nbt.putInt("viewportHeight", buffer.getViewportHeight)
              PacketSender.sendTextBufferInit(address, nbt, entity)
            }
          case _ => // Invalid packet.
        }
      case _ => // Invalid packet.
    }
  }

  def onWaypointLabel(p: PacketParser): Unit = {
    val entity = p.readBlockEntity[Waypoint]()
    val label = p.readUTF().take(32)
    entity match {
      case Some(waypoint) => p.player match {
        case player: ServerPlayerEntity if player.distanceToSqr(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5) <= 64 =>
          if (label != waypoint.label) {
            waypoint.label = label
            PacketSender.sendWaypointLabel(waypoint)
          }
        case _ =>
      }
      case _ => // Invalid packet.
    }
  }

  protected override def createParser(stream: InputStream, player: PlayerEntity) = new PacketParser(stream, player)
}
