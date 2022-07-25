package li.cil.oc.common

import java.util.function.Supplier

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
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.world.World
import net.minecraftforge.fml.network.PacketDistributor
import net.minecraftforge.fml.server.ServerLifecycleHooks
import net.minecraftforge.registries._

import scala.collection.convert.WrapAsScala._

abstract class PacketBuilder(stream: OutputStream) extends DataOutputStream(stream) {
  def writeRegistryEntry[T <: IForgeRegistryEntry[T]](registry: IForgeRegistry[T], value: T): Unit =
    writeInt(registry.asInstanceOf[ForgeRegistry[T]].getID(value))

  def writeTileEntity(t: TileEntity) {
    writeUTF(t.getLevel.dimension.location.toString)
    writeInt(t.getBlockPos.getX)
    writeInt(t.getBlockPos.getY)
    writeInt(t.getBlockPos.getZ)
  }

  def writeEntity(e: Entity) {
    writeUTF(e.level.dimension.location.toString)
    writeInt(e.getId)
  }

  def writeDirection(d: Option[Direction]) = d match {
    case Some(side) => writeByte(side.ordinal.toByte)
    case _ => writeByte(-1: Byte)
  }

  def writeItemStack(stack: ItemStack) = {
    val haveStack = !stack.isEmpty && stack.getCount > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.save(new CompoundNBT()))
    }
  }

  def writeNBT(nbt: CompoundNBT) = {
    val haveNbt = nbt != null
    writeBoolean(haveNbt)
    if (haveNbt) {
      CompressedStreamTools.write(nbt, this)
    }
  }

  def writePacketType(pt: PacketType.Value) = writeByte(pt.id)

  def sendToAllPlayers() = OpenComputers.channel.send(PacketDistributor.ALL.noArg(), packet)

  def sendToPlayersNearEntity(e: Entity, range: Option[Double] = None): Unit = sendToNearbyPlayers(e.level, e.getX, e.getY, e.getZ, range)

  def sendToPlayersNearTileEntity(t: TileEntity, range: Option[Double] = None): Unit = sendToNearbyPlayers(t.getLevel, t.getBlockPos.getX + 0.5, t.getBlockPos.getY + 0.5, t.getBlockPos.getZ + 0.5, range)

  def sendToPlayersNearHost(host: EnvironmentHost, range: Option[Double] = None): Unit = sendToNearbyPlayers(host.world, host.xPosition, host.yPosition, host.zPosition, range)

  def sendToNearbyPlayers(world: World, x: Double, y: Double, z: Double, range: Option[Double]) {
    val server = ServerLifecycleHooks.getCurrentServer
    val manager = server.getPlayerList
    for (player <- manager.getPlayers if player.level == world) {
      val playerRenderDistance = 16 // ObfuscationReflectionHelper.getPrivateValue(classOf[ServerPlayerEntity], player, "renderDistance").asInstanceOf[Integer]
      val playerSpecificRange = range.getOrElse((manager.getViewDistance min playerRenderDistance) * 16.0)
      if (player.distanceToSqr(x, y, z) < playerSpecificRange * playerSpecificRange) {
        sendToPlayer(player)
      }
    }
  }

  def sendToPlayer(player: ServerPlayerEntity) = OpenComputers.channel.send(PacketDistributor.PLAYER.`with`(new Supplier[ServerPlayerEntity] {
    override def get = player
  }), packet)

  def sendToServer() = OpenComputers.channel.sendToServer(packet)

  protected def packet: Array[Byte]
}

// Necessary to keep track of the GZIP stream.
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends PacketBuilder(new BufferedOutputStream(stream))

class SimplePacketBuilder(val packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.toByteArray
  }
}

class CompressedPacketBuilder(val packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED))) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.finish()
    data.toByteArray
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}
