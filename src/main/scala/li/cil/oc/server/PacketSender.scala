package li.cil.oc.server

import li.cil.oc.api.component.TextBuffer.ColorDepth
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.event.FileSystemAccessEvent
import li.cil.oc.api.network.Node
import li.cil.oc.common._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.util.PackedColor
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

object PacketSender {
  def sendAbstractBusState(t: AbstractBusAware) {
    val pb = new SimplePacketBuilder(PacketType.AbstractBusState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isAbstractBusAvailable)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendAnalyze(address: String, player: EntityPlayerMP) {
    val pb = new SimplePacketBuilder(PacketType.Analyze)

    pb.writeUTF(address)

    pb.sendToPlayer(player)
  }

  def sendChargerState(t: tileentity.Charger) {
    val pb = new SimplePacketBuilder(PacketType.ChargerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.chargeSpeed)
    pb.writeBoolean(t.hasPower)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendColorChange(t: Colored) {
    val pb = new SimplePacketBuilder(PacketType.ColorChange)

    pb.writeTileEntity(t)
    pb.writeInt(t.color)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendComputerState(t: Computer) {
    val pb = new SimplePacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isRunning)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendComputerUserList(t: Computer, list: Array[String]) {
    val pb = new SimplePacketBuilder(PacketType.ComputerUserList)

    pb.writeTileEntity(t)
    pb.writeInt(list.length)
    list.foreach(pb.writeUTF)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendDisassemblerActive(t: tileentity.Disassembler, active: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.DisassemblerActiveChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(active)

    pb.sendToPlayersNearTileEntity(t)
  }

  // Avoid spamming the network with disk activity notices.
  val fileSystemAccessTimeouts = mutable.WeakHashMap.empty[EnvironmentHost, mutable.Map[String, Long]]

  def sendFileSystemActivity(node: Node, host: EnvironmentHost, name: String) = fileSystemAccessTimeouts.synchronized {
    fileSystemAccessTimeouts.get(host) match {
      case Some(hostTimeouts) if hostTimeouts.getOrElse(name, 0L) > System.currentTimeMillis() => // Cooldown.
      case _ =>
        val event = host match {
          case t: net.minecraft.tileentity.TileEntity => new FileSystemAccessEvent.Server(name, t, node)
          case _ => new FileSystemAccessEvent.Server(name, host.world, host.xPosition, host.yPosition, host.zPosition, node)
        }
        MinecraftForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          fileSystemAccessTimeouts.getOrElseUpdate(host, mutable.Map.empty) += name -> (System.currentTimeMillis() + 500)

          val pb = new SimplePacketBuilder(PacketType.FileSystemActivity)

          pb.writeUTF(event.getSound)
          CompressedStreamTools.write(event.getData, pb)
          event.getTileEntity match {
            case t: net.minecraft.tileentity.TileEntity =>
              pb.writeBoolean(true)
              pb.writeTileEntity(t)
            case _ =>
              pb.writeBoolean(false)
              pb.writeInt(event.getWorld.provider.dimensionId)
              pb.writeDouble(event.getX)
              pb.writeDouble(event.getY)
              pb.writeDouble(event.getZ)
          }

          pb.sendToPlayersNearHost(host, 64)
        }
    }
  }

  def sendFloppyChange(t: tileentity.DiskDrive, stack: ItemStack = null) {
    val pb = new SimplePacketBuilder(PacketType.FloppyChange)

    pb.writeTileEntity(t)
    pb.writeItemStack(stack)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramClear(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramClear)

    pb.writeTileEntity(t)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramColor(t: tileentity.Hologram, index: Int, value: Int) {
    val pb = new SimplePacketBuilder(PacketType.HologramColor)

    pb.writeTileEntity(t)
    pb.writeInt(index)
    pb.writeInt(value)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramPowerChange(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramPowerChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.hasPower)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramScale(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramScale)

    pb.writeTileEntity(t)
    pb.writeDouble(t.scale)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramSet(t: tileentity.Hologram) {
    val pb = new CompressedPacketBuilder(PacketType.HologramSet)

    pb.writeTileEntity(t)
    pb.writeByte(t.dirtyFromX)
    pb.writeByte(t.dirtyUntilX)
    pb.writeByte(t.dirtyFromZ)
    pb.writeByte(t.dirtyUntilZ)
    for (x <- t.dirtyFromX until t.dirtyUntilX) {
      for (z <- t.dirtyFromZ until t.dirtyUntilZ) {
        pb.writeInt(t.volume(x + z * t.width))
        pb.writeInt(t.volume(x + z * t.width + t.width * t.width))
      }
    }

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendHologramOffset(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramTranslation)

    pb.writeTileEntity(t)
    pb.writeDouble(t.translation.xCoord)
    pb.writeDouble(t.translation.yCoord)
    pb.writeDouble(t.translation.zCoord)

    pb.sendToPlayersNearTileEntity(t)
  }


  def sendPetVisibility(name: Option[String] = None, player: Option[EntityPlayerMP] = None) {
    val pb = new SimplePacketBuilder(PacketType.PetVisibility)

    name match {
      case Some(n) =>
        pb.writeInt(1)
        pb.writeUTF(n)
        pb.writeBoolean(!PetVisibility.hidden.contains(n))
      case _ =>
        pb.writeInt(PetVisibility.hidden.size)
        for (n <- PetVisibility.hidden) {
          pb.writeUTF(n)
          pb.writeBoolean(false)
        }
    }

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendPowerState(t: PowerInformation) {
    val pb = new SimplePacketBuilder(PacketType.PowerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.globalBuffer)
    pb.writeDouble(t.globalBufferSize)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendRedstoneState(t: RedstoneAware) {
    val pb = new SimplePacketBuilder(PacketType.RedstoneState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isOutputEnabled)
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      pb.writeByte(t.output(d))
    }

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendRobotAssembling(t: tileentity.Assembler, assembling: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.RobotAssemblingState)

    pb.writeTileEntity(t)
    pb.writeBoolean(assembling)

    pb.sendToPlayersNearHost(t)
  }

  def sendRobotMove(t: tileentity.Robot, ox: Int, oy: Int, oz: Int, direction: ForgeDirection) {
    val pb = new SimplePacketBuilder(PacketType.RobotMove)

    // Custom pb.writeTileEntity() with fake coordinates (valid for the client).
    pb.writeInt(t.proxy.world.provider.dimensionId)
    pb.writeInt(ox)
    pb.writeInt(oy)
    pb.writeInt(oz)
    pb.writeDirection(direction)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendRobotAnimateSwing(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotAnimateSwing)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToPlayersNearTileEntity(t, 64)
  }

  def sendRobotAnimateTurn(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotAnimateTurn)

    pb.writeTileEntity(t.proxy)
    pb.writeByte(t.turnAxis)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToPlayersNearTileEntity(t, 64)
  }

  def sendRobotInventory(t: tileentity.Robot, slot: Int, stack: ItemStack) {
    val pb = new SimplePacketBuilder(PacketType.RobotInventoryChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(slot)
    pb.writeItemStack(stack)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendRobotSelectedSlotChange(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotSelectedSlotChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.selectedSlot)

    pb.sendToPlayersNearTileEntity(t, 16)
  }

  def sendRotatableState(t: Rotatable) {
    val pb = new SimplePacketBuilder(PacketType.RotatableState)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendSwitchActivity(t: tileentity.Switch) {
    val pb = new SimplePacketBuilder(PacketType.SwitchActivity)

    pb.writeTileEntity(t)

    pb.sendToPlayersNearTileEntity(t, 64)
  }

  def appendTextBufferColorChange(pb: PacketBuilder, foreground: PackedColor.Color, background: PackedColor.Color) {
    pb.writePacketType(PacketType.TextBufferColorChange)

    pb.writeInt(foreground.value)
    pb.writeBoolean(foreground.isPalette)
    pb.writeInt(background.value)
    pb.writeBoolean(background.isPalette)
  }

  def appendTextBufferCopy(pb: PacketBuilder, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    pb.writePacketType(PacketType.TextBufferCopy)

    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeInt(tx)
    pb.writeInt(ty)
  }

  def appendTextBufferDepthChange(pb: PacketBuilder, value: ColorDepth) {
    pb.writePacketType(PacketType.TextBufferDepthChange)

    pb.writeInt(value.ordinal)
  }

  def appendTextBufferFill(pb: PacketBuilder, col: Int, row: Int, w: Int, h: Int, c: Char) {
    pb.writePacketType(PacketType.TextBufferFill)

    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeChar(c)
  }

  def appendTextBufferPaletteChange(pb: PacketBuilder, index: Int, color: Int) {
    pb.writePacketType(PacketType.TextBufferPaletteChange)

    pb.writeInt(index)
    pb.writeInt(color)
  }

  def appendTextBufferResolutionChange(pb: PacketBuilder, w: Int, h: Int) {
    pb.writePacketType(PacketType.TextBufferResolutionChange)

    pb.writeInt(w)
    pb.writeInt(h)
  }

  def appendTextBufferSet(pb: PacketBuilder, col: Int, row: Int, s: String, vertical: Boolean) {
    pb.writePacketType(PacketType.TextBufferSet)

    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeUTF(s)
    pb.writeBoolean(vertical)
  }

  def sendTextBufferInit(address: String, value: NBTTagCompound, player: EntityPlayerMP) {
    val pb = new CompressedPacketBuilder(PacketType.TextBufferInit)

    pb.writeUTF(address)
    pb.writeNBT(value)

    pb.sendToPlayer(player)
  }

  def sendTextBufferPowerChange(address: String, hasPower: Boolean, host: EnvironmentHost) {
    val pb = new SimplePacketBuilder(PacketType.TextBufferPowerChange)

    pb.writeUTF(address)
    pb.writeBoolean(hasPower)

    pb.sendToPlayersNearHost(host)
  }

  def sendScreenTouchMode(t: tileentity.Screen, value: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.ScreenTouchMode)

    pb.writeTileEntity(t)
    pb.writeBoolean(value)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendServerPresence(t: tileentity.ServerRack) {
    val pb = new SimplePacketBuilder(PacketType.ServerPresence)

    pb.writeTileEntity(t)
    t.servers.foreach {
      case Some(server) =>
        pb.writeBoolean(true)
        pb.writeUTF(server.machine.node.address)
      case _ =>
        pb.writeBoolean(false)
    }

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendServerState(t: tileentity.ServerRack) {
    val pb = new SimplePacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeInt(-1)
    pb.writeInt(t.range)

    pb.sendToPlayersNearTileEntity(t)
  }

  def sendServerState(t: tileentity.ServerRack, number: Int, player: Option[EntityPlayerMP] = None) {
    val pb = new SimplePacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeBoolean(t.isRunning(number))
    pb.writeDirection(t.sides(number))
    val keys = t.terminals(number).keys
    pb.writeInt(keys.length)
    for (key <- keys) {
      pb.writeUTF(key)
    }

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToPlayersNearTileEntity(t)
    }
  }

  def sendSound(world: World, x: Double, y: Double, z: Double, frequency: Int, duration: Int) {
    val pb = new SimplePacketBuilder(PacketType.Sound)

    pb.writeInt(world.provider.dimensionId)
    pb.writeInt(math.floor(x).toInt)
    pb.writeInt(math.floor(y).toInt)
    pb.writeInt(math.floor(z).toInt)
    pb.writeShort(frequency.toShort)
    pb.writeShort(duration.toShort)

    pb.sendToNearbyPlayers(world, x, y, z, 16)
  }
}