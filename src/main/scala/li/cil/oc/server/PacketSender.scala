package li.cil.oc.server

import li.cil.oc.api.component.TextBuffer.ColorDepth
import li.cil.oc.api.driver.Container
import li.cil.oc.common._
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.util.PackedColor
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object PacketSender {
  def sendAbstractBusState(t: AbstractBusAware) {
    val pb = new SimplePacketBuilder(PacketType.AbstractBusState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isAbstractBusAvailable)

    pb.sendToNearbyPlayers(t)
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

    pb.sendToNearbyPlayers(t)
  }

  def sendColorChange(t: Colored) {
    val pb = new SimplePacketBuilder(PacketType.ColorChange)

    pb.writeTileEntity(t)
    pb.writeInt(t.color)

    pb.sendToNearbyPlayers(t)
  }

  def sendComputerState(t: Computer) {
    val pb = new SimplePacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isRunning)

    pb.sendToNearbyPlayers(t)
  }

  def sendComputerUserList(t: Computer, list: Array[String]) {
    val pb = new SimplePacketBuilder(PacketType.ComputerUserList)

    pb.writeTileEntity(t)
    pb.writeInt(list.length)
    list.foreach(pb.writeUTF)

    pb.sendToNearbyPlayers(t)
  }

  def sendDisassemblerActive(t: tileentity.Disassembler, active: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.DisassemblerActiveChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(active)

    pb.sendToNearbyPlayers(t)
  }

  def sendFloppyChange(t: tileentity.DiskDrive, stack: ItemStack = null) {
    val pb = new SimplePacketBuilder(PacketType.FloppyChange)

    pb.writeTileEntity(t)
    pb.writeItemStack(stack)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramClear(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramClear)

    pb.writeTileEntity(t)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramColor(t: tileentity.Hologram, index: Int, value: Int) {
    val pb = new SimplePacketBuilder(PacketType.HologramColor)

    pb.writeTileEntity(t)
    pb.writeInt(index)
    pb.writeInt(value)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramPowerChange(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramPowerChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.hasPower)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramScale(t: tileentity.Hologram) {
    val pb = new SimplePacketBuilder(PacketType.HologramScale)

    pb.writeTileEntity(t)
    pb.writeDouble(t.scale)

    pb.sendToNearbyPlayers(t)
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

    pb.sendToNearbyPlayers(t)
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

    pb.sendToNearbyPlayers(t)
  }

  def sendRedstoneState(t: RedstoneAware) {
    val pb = new SimplePacketBuilder(PacketType.RedstoneState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isOutputEnabled)
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      pb.writeByte(t.output(d))
    }

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotAssembling(t: tileentity.RobotAssembler, assembling: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.RobotAssemblingState)

    pb.writeTileEntity(t)
    pb.writeBoolean(assembling)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotMove(t: tileentity.Robot, ox: Int, oy: Int, oz: Int, direction: ForgeDirection) {
    val pb = new SimplePacketBuilder(PacketType.RobotMove)

    // Custom pb.writeTileEntity() with fake coordinates (valid for the client).
    pb.writeInt(t.proxy.world.provider.dimensionId)
    pb.writeInt(ox)
    pb.writeInt(oy)
    pb.writeInt(oz)
    pb.writeDirection(direction)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotAnimateSwing(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotAnimateSwing)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendRobotAnimateTurn(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotAnimateTurn)

    pb.writeTileEntity(t.proxy)
    pb.writeByte(t.turnAxis)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendRobotInventory(t: tileentity.Robot, slot: Int, stack: ItemStack) {
    val pb = new SimplePacketBuilder(PacketType.RobotInventoryChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(slot)
    pb.writeItemStack(stack)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotSelectedSlotChange(t: tileentity.Robot) {
    val pb = new SimplePacketBuilder(PacketType.RobotSelectedSlotChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.selectedSlot)

    pb.sendToNearbyPlayers(t, 16)
  }

  def sendRotatableState(t: Rotatable) {
    val pb = new SimplePacketBuilder(PacketType.RotatableState)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    pb.sendToNearbyPlayers(t)
  }

  def sendSwitchActivity(t: tileentity.Switch) {
    val pb = new SimplePacketBuilder(PacketType.SwitchActivity)

    pb.writeTileEntity(t)

    pb.sendToNearbyPlayers(t, 64)
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

  def sendTextBufferPowerChange(address: String, hasPower: Boolean, container: Container) {
    val pb = new SimplePacketBuilder(PacketType.TextBufferPowerChange)

    pb.writeUTF(address)
    pb.writeBoolean(hasPower)

    pb.sendToNearbyPlayers(container)
  }

  def sendScreenTouchMode(t: tileentity.Screen, value: Boolean) {
    val pb = new SimplePacketBuilder(PacketType.ScreenTouchMode)

    pb.writeTileEntity(t)
    pb.writeBoolean(value)

    pb.sendToNearbyPlayers(t)
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

    pb.sendToNearbyPlayers(t)
  }

  def sendServerState(t: tileentity.ServerRack) {
    val pb = new SimplePacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeInt(-1)
    pb.writeInt(t.range)

    pb.sendToNearbyPlayers(t)
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
      case _ => pb.sendToNearbyPlayers(t)
    }
  }

  def sendSound(world: World, x: Int, y: Int, z: Int, frequency: Int, duration: Int) {
    val pb = new SimplePacketBuilder(PacketType.Sound)

    pb.writeInt(world.provider.dimensionId)
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeInt(z)
    pb.writeShort(frequency.toShort)
    pb.writeShort(duration.toShort)

    pb.sendToNearbyPlayers(world, x, y, z, 16)
  }
}