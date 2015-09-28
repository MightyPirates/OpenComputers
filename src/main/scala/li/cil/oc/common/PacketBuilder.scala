package li.cil.oc.common

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.network.internal.FMLProxyPacket
import io.netty.buffer.Unpooled
import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.EnvironmentHost
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._

abstract class PacketBuilder(stream: OutputStream) extends DataOutputStream(stream) {
  def writeTileEntity(t: TileEntity) {
    writeInt(t.getWorldObj.provider.dimensionId)
    writeInt(t.xCoord)
    writeInt(t.yCoord)
    writeInt(t.zCoord)
  }

  def writeEntity(e: Entity) {
    writeInt(e.worldObj.provider.dimensionId)
    writeInt(e.getEntityId)
  }

  def writeDirection(d: Option[ForgeDirection]) = d match {
    case Some(side) => writeByte(side.ordinal.toByte)
    case _ => writeByte(-1: Byte)
  }

  def writeItemStack(stack: ItemStack) = {
    val haveStack = stack != null && stack.stackSize > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.writeToNBT(new NBTTagCompound()))
    }
  }

  def writeNBT(nbt: NBTTagCompound) = CompressedStreamTools.writeCompressed(nbt, this)

  def writePacketType(pt: PacketType.Value) = writeByte(pt.id)

  def sendToAllPlayers() = OpenComputers.channel.sendToAll(packet)

  def sendToPlayersNearEntity(e: Entity, range: Option[Double] = None): Unit = sendToNearbyPlayers(e.worldObj, e.posX, e.posY, e.posZ, range)

  def sendToPlayersNearTileEntity(t: TileEntity, range: Option[Double] = None): Unit = sendToNearbyPlayers(t.getWorldObj, t.xCoord + 0.5, t.yCoord + 0.5, t.zCoord + 0.5, range)

  def sendToPlayersNearHost(host: EnvironmentHost, range: Option[Double] = None): Unit = sendToNearbyPlayers(host.world, host.xPosition, host.yPosition, host.zPosition, range)

  def sendToNearbyPlayers(world: World, x: Double, y: Double, z: Double, range: Option[Double]) {
    val dimension = world.provider.dimensionId
    val server = FMLCommonHandler.instance.getMinecraftServerInstance
    val manager = server.getConfigurationManager
    for (player <- manager.playerEntityList.map(_.asInstanceOf[EntityPlayerMP]) if player.dimension == dimension) {
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
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends PacketBuilder(stream)

class SimplePacketBuilder(packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    new FMLProxyPacket(Unpooled.wrappedBuffer(stream.toByteArray), "OpenComputers")
  }
}

class CompressedPacketBuilder(packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new GZIPOutputStream(data)) {
  writeByte(packetType.id)

  override protected def packet = {
    stream.finish()
    new FMLProxyPacket(Unpooled.wrappedBuffer(data.toByteArray), "OpenComputers")
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}