package li.cil.oc.server

import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits._
import li.cil.oc.common.{CompressedPacketBuilder, PacketBuilder, PacketType}
import li.cil.oc.util.PackedColor
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraftforge.common.ForgeDirection
import net.minecraft.world.World

object PacketSender {
  def sendAbstractBusState(t: AbstractBusAware) {
    val pb = new PacketBuilder(PacketType.AbstractBusState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isAbstractBusAvailable)

    pb.sendToNearbyPlayers(t)
  }

  def sendAnalyze(address: String, player: EntityPlayerMP) {
    val pb = new PacketBuilder(PacketType.Analyze)

    pb.writeUTF(address)

    pb.sendToPlayer(player)
  }

  def sendChargerState(t: tileentity.Charger) {
    val pb = new PacketBuilder(PacketType.ChargerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.chargeSpeed)
    pb.writeBoolean(t.hasPower)

    pb.sendToNearbyPlayers(t)
  }

  def sendColorChange(t: Colored) {
    val pb = new PacketBuilder(PacketType.ColorChange)

    pb.writeTileEntity(t)
    pb.writeInt(t.color)

    pb.sendToNearbyPlayers(t)
  }

  def sendComputerState(t: Computer) {
    val pb = new PacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isRunning)

    pb.sendToNearbyPlayers(t)
  }

  def sendComputerUserList(t: Computer, list: Array[String]) {
    val pb = new PacketBuilder(PacketType.ComputerUserList)

    pb.writeTileEntity(t)
    pb.writeInt(list.length)
    list.foreach(pb.writeUTF)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramClear(t: tileentity.Hologram) {
    val pb = new PacketBuilder(PacketType.HologramClear)

    pb.writeTileEntity(t)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramPowerChange(t: tileentity.Hologram) {
    val pb = new PacketBuilder(PacketType.HologramPowerChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.hasPower)

    pb.sendToNearbyPlayers(t)
  }

  def sendHologramScale(t: tileentity.Hologram) {
    val pb = new PacketBuilder(PacketType.HologramScale)

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
      }
    }

    pb.sendToNearbyPlayers(t)
  }

  def sendPowerState(t: PowerInformation) {
    val pb = new PacketBuilder(PacketType.PowerState)

    pb.writeTileEntity(t)
    pb.writeDouble(t.globalBuffer)
    pb.writeDouble(t.globalBufferSize)

    pb.sendToNearbyPlayers(t)
  }

  def sendRedstoneState(t: RedstoneAware) {
    val pb = new PacketBuilder(PacketType.RedstoneState)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isOutputEnabled)
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      pb.writeByte(t.output(d))
    }

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotMove(t: tileentity.Robot, ox: Int, oy: Int, oz: Int, direction: ForgeDirection) {
    val pb = new PacketBuilder(PacketType.RobotMove)

    // Custom pb.writeTileEntity() with fake coordinates (valid for the client).
    pb.writeInt(t.proxy.world.provider.dimensionId)
    pb.writeInt(ox)
    pb.writeInt(oy)
    pb.writeInt(oz)
    pb.writeDirection(direction)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotAnimateSwing(t: tileentity.Robot) {
    val pb = new PacketBuilder(PacketType.RobotAnimateSwing)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendRobotAnimateTurn(t: tileentity.Robot) {
    val pb = new PacketBuilder(PacketType.RobotAnimateTurn)

    pb.writeTileEntity(t.proxy)
    pb.writeByte(t.turnAxis)
    pb.writeInt(t.animationTicksTotal)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendRobotEquippedItemChange(t: tileentity.Robot, stack: ItemStack) {
    val pb = new PacketBuilder(PacketType.RobotEquippedItemChange)

    pb.writeTileEntity(t.proxy)
    pb.writeItemStack(stack)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotEquippedUpgradeChange(t: tileentity.Robot, stack: ItemStack) {
    val pb = new PacketBuilder(PacketType.RobotEquippedUpgradeChange)

    pb.writeTileEntity(t.proxy)
    pb.writeItemStack(stack)

    pb.sendToNearbyPlayers(t)
  }

  def sendRobotSelectedSlotChange(t: tileentity.Robot) {
    val pb = new PacketBuilder(PacketType.RobotSelectedSlotChange)

    pb.writeTileEntity(t.proxy)
    pb.writeInt(t.selectedSlot)

    pb.sendToNearbyPlayers(t, 16)
  }

  def sendRobotXp(t: tileentity.Robot) {
    val pb = new PacketBuilder(PacketType.RobotXp)

    pb.writeTileEntity(t)
    pb.writeDouble(t.xp)

    pb.sendToNearbyPlayers(t)
  }

  def sendRotatableState(t: Rotatable) {
    val pb = new PacketBuilder(PacketType.RotatableState)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    pb.sendToNearbyPlayers(t)
  }

  def sendRouterActivity(t: tileentity.Router) {
    val pb = new PacketBuilder(PacketType.RouterActivity)

    pb.writeTileEntity(t)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendScreenColorChange(b: common.component.Buffer, foreground: Int, background: Int) {
    val pb = new PacketBuilder(PacketType.ScreenColorChange)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(foreground)
    pb.writeInt(background)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenCopy(b: common.component.Buffer, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    val pb = new PacketBuilder(PacketType.ScreenCopy)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeInt(tx)
    pb.writeInt(ty)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenDepthChange(b: common.component.Buffer, value: PackedColor.Depth.Value) {
    val pb = new PacketBuilder(PacketType.ScreenDepthChange)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(value.id)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenFill(b: common.component.Buffer, col: Int, row: Int, w: Int, h: Int, c: Char) {
    val pb = new PacketBuilder(PacketType.ScreenFill)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeChar(c)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenPowerChange(t: TextBuffer, hasPower: Boolean) {
    val pb = new PacketBuilder(PacketType.ScreenPowerChange)

    pb.writeTileEntity(t)
    pb.writeBoolean(hasPower)

    pb.sendToNearbyPlayers(t, 64)
  }

  def sendScreenResolutionChange(b: common.component.Buffer, w: Int, h: Int) {
    val pb = new PacketBuilder(PacketType.ScreenResolutionChange)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(w)
    pb.writeInt(h)

    pb.sendToNearbyPlayers(t)
  }

  def sendScreenSet(b: common.component.Buffer, col: Int, row: Int, s: String) {
    val pb = new PacketBuilder(PacketType.ScreenSet)

    val t = b.owner match {
      case t: TextBuffer =>
        pb.writeTileEntity(t)
        t
      case t: common.component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
        t.rack
      case _ => return
    }
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeUTF(s)

    pb.sendToNearbyPlayers(t)
  }

  def sendServerPresence(t: tileentity.Rack) {
    val pb = new PacketBuilder(PacketType.ServerPresence)

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

  def sendServerState(t: tileentity.Rack) {
    val pb = new PacketBuilder(PacketType.ComputerState)

    pb.writeTileEntity(t)
    pb.writeInt(-1)
    pb.writeInt(t.range)

    pb.sendToNearbyPlayers(t)
  }

  def sendServerState(t: tileentity.Rack, number: Int, player: Option[EntityPlayerMP] = None) {
    val pb = new PacketBuilder(PacketType.ComputerState)

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
    val pb = new PacketBuilder(PacketType.Sound)

    pb.writeInt(world.provider.dimensionId)
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeInt(z)
    pb.writeShort(frequency.toShort)
    pb.writeShort(duration.toShort)

    pb.sendToNearbyPlayers(world, x, y, z, 16)
  }
}