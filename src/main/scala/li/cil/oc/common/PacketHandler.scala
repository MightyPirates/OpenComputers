package li.cil.oc.common

import java.io.{ByteArrayInputStream, DataInputStream, InputStream}
import java.util.logging.Level
import java.util.zip.GZIPInputStream

import cpw.mods.fml.common.network.{IPacketHandler, Player}
import li.cil.oc.{Blocks, OpenComputers}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

import scala.reflect.{ClassTag, classTag}

abstract class PacketHandler extends IPacketHandler {
  /** Top level dispatcher based on packet type. */
  def onPacketData(manager: INetworkManager, packet: Packet250CustomPayload, player: Player) {
    // Don't crash on badly formatted packets (may have been altered by a
    // malicious client, in which case we don't want to allow it to kill the
    // server like this). Just spam the log a bit... ;)
    try {
      val stream = new ByteArrayInputStream(packet.data)
      if (stream.read() == 0) dispatch(new PacketParser(stream, player))
      else dispatch(new PacketParser(new GZIPInputStream(stream), player))
    } catch {
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Received a badly formatted packet.", e)
    }
  }

  /**
   * Gets the world for the specified dimension.
   *
   * For clients this returns the client's world if it is the specified
   * dimension; None otherwise. For the server it returns the world for the
   * specified dimension, if such a dimension exists; None otherwise.
   */
  protected def world(player: Player, dimension: Int): Option[World]

  protected def dispatch(p: PacketParser)

  protected class PacketParser(stream: InputStream, val player: Player) extends DataInputStream(stream) {
    val packetType = PacketType(readByte())

    def getTileEntity[T: ClassTag](dimension: Int, x: Int, y: Int, z: Int): Option[T] = {
      world(player, dimension) match {
        case None => // Invalid dimension.
        case Some(world) =>
          val t = world.getBlockTileEntity(x, y, z)
          if (t != null && classTag[T].runtimeClass.isAssignableFrom(t.getClass)) {
            return Some(t.asInstanceOf[T])
          }
          // In case a robot moved away before the packet arrived. This is
          // mostly used when the robot *starts* moving while the client sends
          // a request to the server.
          Blocks.robotAfterimage.findMovingRobot(world, x, y, z) match {
            case Some(robot) if classTag[T].runtimeClass.isAssignableFrom(robot.proxy.getClass) =>
              return Some(robot.proxy.asInstanceOf[T])
            case _ =>
          }
      }
      None
    }

    def readTileEntity[T: ClassTag](): Option[T] = {
      val dimension = readInt()
      val x = readInt()
      val y = readInt()
      val z = readInt()
      getTileEntity(dimension, x, y, z)
    }

    def readDirection() = ForgeDirection.getOrientation(readInt())

    def readItemStack() = {
      val haveStack = readBoolean()
      if (haveStack) {
        ItemStack.loadItemStackFromNBT(readNBT())
      }
      else null
    }

    def readNBT() = CompressedStreamTools.readCompressed(this)

    def readPacketType() = PacketType(readByte())
  }

}