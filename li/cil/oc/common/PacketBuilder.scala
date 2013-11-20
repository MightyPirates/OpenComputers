package li.cil.oc.common

import cpw.mods.fml.common.network.PacketDispatcher
import cpw.mods.fml.common.network.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import li.cil.oc.common.tileentity.TileEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTBase}
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraftforge.common.ForgeDirection

/** Utility class for packet creation. */
class PacketBuilder(packetType: PacketType.Value, private val stream: ByteArrayOutputStream = new ByteArrayOutputStream) extends DataOutputStream(stream) {
  writeByte(packetType.id)

  def writeTileEntity(t: TileEntity) = {
    writeInt(t.world.provider.dimensionId)
    writeInt(t.x)
    writeInt(t.y)
    writeInt(t.z)
  }

  def writeDirection(d: ForgeDirection) = writeInt(d.ordinal)

  def writeItemStack(stack: ItemStack) = writeNBT(stack.writeToNBT(new NBTTagCompound()))

  def writeNBT(nbt: NBTBase) = NBTBase.writeNamedTag(nbt, this)

  def sendToAllPlayers() = PacketDispatcher.sendPacketToAllPlayers(packet)

  def sendToNearbyPlayers(t: TileEntity, range: Double = 64) = PacketDispatcher.sendPacketToAllAround(t.x, t.y, t.z, range, t.world.provider.dimensionId, packet)

  def sendToPlayer(player: Player) = PacketDispatcher.sendPacketToPlayer(packet, player)

  def sendToServer() = PacketDispatcher.sendPacketToServer(packet)

  private def packet = {
    val p = new Packet250CustomPayload
    p.channel = "OpenComp"
    p.data = stream.toByteArray
    p.length = stream.size
    p
  }
}