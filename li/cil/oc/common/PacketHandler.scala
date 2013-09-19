package li.cil.oc.common

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import cpw.mods.fml.common.network.IPacketHandler
import cpw.mods.fml.common.network.Player
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.DimensionManager
import net.minecraft.world.World
import scala.reflect.runtime.universe._

abstract class PacketHandler extends IPacketHandler {
  /** Top level dispatcher based on packet type. */
  def onPacketData(manager: INetworkManager, packet: Packet250CustomPayload, player: Player) {
    dispatch(new PacketParser(packet, player))
  }

  /**
   * Gets the world for the specified dimension.
   *
   * For clients this returns the client's world if it is the specified
   * dimension; None otherwise. For the server it returns the world for the
   * specified dimension, if such a dimension exists; None otherwise.
   */
  protected def world(player: Player, dimension: Int): Option[World]

  /** Handles packets based on their type. */
  protected def dispatch(p: PacketParser)

  /** Utility class for packet parsing. */
  protected class PacketParser(packet: Packet250CustomPayload, val player: Player) extends DataInputStream(new ByteArrayInputStream(packet.data)) {
    val packetType = PacketType(readByte())

    def readTileEntity[T <: TileEntity: TypeTag](): Option[T] = {
      val dimension = readInt()
      val x = readInt()
      val y = readInt()
      val z = readInt()

      world(player, dimension) match {
        case None => // Invalid dimension.
        case Some(world) => {
          val t = world.getBlockTileEntity(x, y, z)
          val m = runtimeMirror(this.getClass.getClassLoader)
          if (t != null && m.classSymbol(t.getClass).toType =:= typeOf[T])
            return Some(t.asInstanceOf[T])
        }
      }
      return None
    }
  }
}