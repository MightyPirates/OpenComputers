package li.cil.oc.common

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.network.internal.FMLProxyPacket
import io.netty.buffer.Unpooled
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import li.cil.oc.common.tileentity.TileEntity
import li.cil.oc.OpenComputers
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import scala.collection.convert.WrapAsScala._

class PacketBuilder(packetType: PacketType.Value, private val stream: ByteArrayOutputStream = new ByteArrayOutputStream) extends DataOutputStream(stream) {
  writeByte(packetType.id)

  def writeTileEntity(t: TileEntity) = {
    writeInt(t.world.provider.dimensionId)
    writeInt(t.x)
    writeInt(t.y)
    writeInt(t.z)
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

  def sendToAllPlayers() = OpenComputers.channel.sendToAll(packet)

  def sendToNearbyPlayers(t: TileEntity, range: Double = 1024): Unit = sendToNearbyPlayers(t.world, t.x + 0.5, t.y + 0.5, t.z + 0.5, range)

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

  def sendToPlayer(player: EntityPlayerMP) = OpenComputers.channel.sendTo(packet, player)

  def sendToServer() = OpenComputers.channel.sendToServer(packet)

  private def packet = new FMLProxyPacket(Unpooled.wrappedBuffer(stream.toByteArray), "OpenComputers")
}
