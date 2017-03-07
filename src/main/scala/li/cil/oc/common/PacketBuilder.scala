package li.cil.oc.common

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

import io.netty.buffer.Unpooled
import li.cil.oc.OpenComputers
import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket

import scala.collection.convert.WrapAsScala._

abstract class PacketBuilder(stream: OutputStream) extends DataOutputStream(stream) {
  def writeTileEntity(t: TileEntity) {
    writeInt(t.getWorld.provider.getDimension)
    writeInt(t.getPos.getX)
    writeInt(t.getPos.getY)
    writeInt(t.getPos.getZ)
  }

  def writeEntity(e: Entity) {
    writeInt(e.world.provider.getDimension)
    writeInt(e.getEntityId)
  }

  def writeDirection(d: Option[EnumFacing]) = d match {
    case Some(side) => writeByte(side.ordinal.toByte)
    case _ => writeByte(-1: Byte)
  }

  def writeItemStack(stack: ItemStack) = {
    val haveStack = stack != null && stack.getCount > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.writeToNBT(new NBTTagCompound()))
    }
  }

  def writeNBT(nbt: NBTTagCompound) = {
    val haveNbt = nbt != null
    writeBoolean(haveNbt)
    if (haveNbt) {
      CompressedStreamTools.write(nbt, this)
    }
  }

  def writePacketType(pt: PacketType.Value) = writeByte(pt.id)

  def sendToAllPlayers() = OpenComputers.channel.sendToAll(packet)

  def sendToPlayersNearEntity(e: Entity, range: Option[Double] = None): Unit = sendToNearbyPlayers(e.getEntityWorld, e.posX, e.posY, e.posZ, range)

  def sendToPlayersNearTileEntity(t: TileEntity, range: Option[Double] = None): Unit = sendToNearbyPlayers(t.getWorld, t.getPos.getX + 0.5, t.getPos.getY + 0.5, t.getPos.getZ + 0.5, range)

  def sendToPlayersNearHost(host: EnvironmentHost, range: Option[Double] = None): Unit = sendToNearbyPlayers(host.getWorld, host.xPosition, host.yPosition, host.zPosition, range)

  def sendToNearbyPlayers(world: World, x: Double, y: Double, z: Double, range: Option[Double]) {
    val dimension = world.provider.getDimension
    val server = FMLCommonHandler.instance.getMinecraftServerInstance
    val manager = server.getPlayerList
    for (player <- manager.getPlayers if player.dimension == dimension) {
      val playerRenderDistance = 16 // ObfuscationReflectionHelper.getPrivateValue(classOf[EntityPlayerMP], player, "renderDistance").asInstanceOf[Integer]
      val playerSpecificRange = range.getOrElse((manager.getViewDistance min playerRenderDistance) * 16.0)
      if (player.getDistanceSq(x, y, z) < playerSpecificRange * playerSpecificRange) {
        sendToPlayer(player)
      }
    }
  }

  def sendToPlayer(player: EntityPlayerMP) = OpenComputers.channel.sendTo(packet, player)

  def sendToServer() = OpenComputers.channel.sendToServer(packet)

  protected def packet: FMLProxyPacket
}

// Necessary to keep track of the GZIP stream.
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends PacketBuilder(new BufferedOutputStream(stream))

class SimplePacketBuilder(val packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(stream.toByteArray)), "OpenComputers")
  }
}

class CompressedPacketBuilder(val packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED))) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.finish()
    new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data.toByteArray)), "OpenComputers")
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}
