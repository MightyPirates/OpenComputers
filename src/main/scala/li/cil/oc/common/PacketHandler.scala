package li.cil.oc.common

import io.netty.buffer.{ByteBufInputStream, ByteBuf}
import java.io.DataInputStream
import li.cil.oc.Blocks
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import scala.reflect.ClassTag
import scala.reflect.classTag

abstract class PacketHandler {
  /**
   * Gets the world for the specified dimension.
   *
   * For clients this returns the client's world if it is the specified
   * dimension; None otherwise. For the server it returns the world for the
   * specified dimension, if such a dimension exists; None otherwise.
   */
  protected def world(player: EntityPlayer, dimension: Int): Option[World]

  protected class PacketParser(data: ByteBuf, val player: EntityPlayer) extends DataInputStream(new ByteBufInputStream(data)) {
    val packetType = PacketType(readByte())

    def getTileEntity[T: ClassTag](dimension: Int, x: Int, y: Int, z: Int): Option[T] = {
      world(player, dimension) match {
        case None => // Invalid dimension.
        case Some(world) =>
          val t = world.getTileEntity(x, y, z)
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
  }

}