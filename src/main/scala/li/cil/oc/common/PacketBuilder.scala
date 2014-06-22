package li.cil.oc.common

import java.io.{ByteArrayOutputStream, DataOutputStream, OutputStream}
import java.util.zip.GZIPOutputStream

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.network.{PacketDispatcher, Player}
import li.cil.oc.api.driver.Container
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

import scala.collection.convert.WrapAsScala._

// Necessary to keep track of the GZIP stream.
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends DataOutputStream(stream) {
  def writeTileEntity(t: TileEntity) = {
    writeInt(t.getWorldObj.provider.dimensionId)
    writeInt(t.xCoord)
    writeInt(t.yCoord)
    writeInt(t.zCoord)
  }

  def writeDirection(d: ForgeDirection) = writeInt(d.ordinal)

  def writeItemStack(stack: ItemStack) = {
    val haveStack = stack != null && stack.stackSize > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.writeToNBT(new NBTTagCompound()))
    }
  }

  def writeNBT(nbt: NBTTagCompound) = CompressedStreamTools.writeCompressed(nbt, this)

  def sendToAllPlayers() = PacketDispatcher.sendPacketToAllPlayers(packet)

  def sendToNearbyPlayers(t: TileEntity, range: Double = 1024): Unit = sendToNearbyPlayers(t.getWorldObj, t.xCoord + 0.5, t.yCoord + 0.5, t.zCoord + 0.5, range)

  def sendToNearbyPlayers(c: Container): Unit = sendToNearbyPlayers(c.world, c.xPosition, c.yPosition, c.zPosition, 1024)

  def sendToNearbyPlayers(world: World, x: Double, y: Double, z: Double, range: Double) {
    val dimension = world.provider.dimensionId
    val server = FMLCommonHandler.instance.getMinecraftServerInstance
    val manager = server.getConfigurationManager
    for (player <- manager.playerEntityList.map(_.asInstanceOf[EntityPlayerMP]) if player.dimension == dimension) {
      val playerRenderDistance = Int.MaxValue // ObfuscationReflectionHelper.getPrivateValue(classOf[EntityPlayerMP], player, "renderDistance").asInstanceOf[Integer]
      val playerSpecificRange = math.min(range, (manager.getViewDistance min playerRenderDistance) * 16)
      if (player.getDistanceSq(x, y, z) < playerSpecificRange * playerSpecificRange) {
        sendToPlayer(player)
      }
    }
  }

  def sendToPlayer(player: EntityPlayerMP) = PacketDispatcher.sendPacketToPlayer(packet, player.asInstanceOf[Player])

  def sendToServer() = PacketDispatcher.sendPacketToServer(packet)

  protected def packet: Packet250CustomPayload
}

class PacketBuilder(packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    val p = new Packet250CustomPayload
    p.channel = "OpenComp"
    p.data = stream.toByteArray
    p.length = stream.size
    p
  }
}

class CompressedPacketBuilder(packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new GZIPOutputStream(data)) {
  writeByte(packetType.id)

  override protected def packet = {
    stream.finish()
    val p = new Packet250CustomPayload
    p.channel = "OpenComp"
    p.data = data.toByteArray
    p.length = data.size
    p
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}