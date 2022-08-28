package li.cil.oc.common

import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.common.block.RobotAfterimage
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.INetHandler
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.reflect.ClassTag
import scala.reflect.classTag

abstract class PacketHandler {
  /** Top level dispatcher based on packet type. */
  protected def onPacketData(handler: INetHandler, data: ByteBuf, player: EntityPlayer) {
    val thread = FMLCommonHandler.instance.getWorldThread(handler)
    if (thread.isCallingFromMinecraftThread) {
      process(data, player)
    }
    else {
      data.retain()
      thread.addScheduledTask(new Runnable {
        override def run(): Unit = {
          process(data, player)
          data.release()
        }
      })
    }
  }

  private def process(data: ByteBuf, player: EntityPlayer): Unit = {
    // Don't crash on badly formatted packets (may have been altered by a
    // malicious client, in which case we don't want to allow it to kill the
    // server like this). Just spam the log a bit... ;)
    var stream: InputStream = null
    try {
      stream = new ByteBufInputStream(data)
      if (stream.read() != 0) stream = new InflaterInputStream(stream)
      dispatch(new PacketParser(stream, player))
    } catch {
      case e: Throwable =>
        OpenComputers.log.warn("Received a badly formatted packet.", e)
    } finally {
      if (stream != null) {
        stream.close()
      }
    }

    // Avoid AFK kicks by marking players as non-idle when they send packets.
    // This will usually be stuff like typing while in screen GUIs.
    player match {
      case mp: EntityPlayerMP => mp.markPlayerActive()
      case _ => // Uh... OK?
    }
  }

  /**
    * Gets the world for the specified dimension.
    *
    * For clients this returns the client's world if it is the specified
    * dimension; None otherwise. For the server it returns the world for the
    * specified dimension, if such a dimension exists; None otherwise.
    */
  protected def world(player: EntityPlayer, dimension: Int): Option[World]

  protected def dispatch(p: PacketParser): Unit

  protected class PacketParser(stream: InputStream, val player: EntityPlayer) extends DataInputStream(stream) {
    val packetType = PacketType(readByte())

    def getTileEntity[T: ClassTag](dimension: Int, x: Int, y: Int, z: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) if world.blockExists(BlockPosition(x, y, z)) =>
          val t = world.getTileEntity(BlockPosition(x, y, z))
          if (t != null && classTag[T].runtimeClass.isAssignableFrom(t.getClass)) {
            return Some(t.asInstanceOf[T])
          }
          // In case a robot moved away before the packet arrived. This is
          // mostly used when the robot *starts* moving while the client sends
          // a request to the server.
          api.Items.get(Constants.BlockName.RobotAfterimage).block match {
            case afterimage: RobotAfterimage => afterimage.findMovingRobot(world, new BlockPos(x, y, z)) match {
              case Some(robot) if classTag[T].runtimeClass.isAssignableFrom(robot.proxy.getClass) =>
                return Some(robot.proxy.asInstanceOf[T])
              case _ =>
            }
            case _ =>
          }
        case _ => // Invalid dimension.
      }
      None
    }

    def getEntity[T: ClassTag](dimension: Int, id: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) =>
          val e = world.getEntityByID(id)
          if (e != null && classTag[T].runtimeClass.isAssignableFrom(e.getClass)) {
            return Some(e.asInstanceOf[T])
          }
        case _ =>
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

    def readEntity[T: ClassTag](): Option[T] = {
      val dimension = readInt()
      val id = readInt()
      getEntity[T](dimension, id)
    }

    def readDirection(): Option[EnumFacing] = readByte() match {
      case id if id < 0 => None
      case id => Option(EnumFacing.byIndex(id))
    }

    def readItemStack(): ItemStack = {
      val haveStack = readBoolean()
      if (haveStack) {
        new ItemStack(readNBT())
      }
      else ItemStack.EMPTY
    }

    def readNBT(): NBTTagCompound = {
      val haveNbt = readBoolean()
      if (haveNbt) {
        CompressedStreamTools.read(this)
      }
      else null
    }

    def readMedium(): Int = {
      val c0 = readUnsignedByte()
      val c1 = readUnsignedByte()
      val c2 = readUnsignedByte()
      (c0) | (c1 << 8) | (c2 << 16)
    }

    def readPacketType() = PacketType(readByte())
  }

}
