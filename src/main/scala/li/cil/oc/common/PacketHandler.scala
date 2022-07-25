package li.cil.oc.common

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.common.block.RobotAfterimage
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RotationHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.INetHandler
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.network.NetworkDirection
import net.minecraftforge.fml.server.ServerLifecycleHooks
import net.minecraftforge.registries._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.reflect.classTag

object PacketHandler {
  var clientHandler: PacketHandler = _

  var serverHandler: PacketHandler = _

  private[oc] def handlePacket(side: NetworkDirection, arr: Array[Byte], player: PlayerEntity): Unit = {
    try {
      val handler = side match {
        case NetworkDirection.PLAY_TO_CLIENT => clientHandler
        case NetworkDirection.PLAY_TO_SERVER => serverHandler
        case _ => null
      }
      if (handler != null) {
        val stream = new ByteArrayInputStream(arr)
        if (stream.read() == 0) handler.dispatch(handler.createParser(stream, player))
        else handler.dispatch(handler.createParser(new InflaterInputStream(stream), player))
      }
    } catch {
      // Don't crash on badly formatted packets (may have been altered by a
      // malicious client, in which case we don't want to allow it to kill the
      // server like this). Just spam the log a bit... ;)
      case e: Throwable =>
        OpenComputers.log.warn("Received a badly formatted packet.", e)
    }

    // Avoid AFK kicks by marking players as non-idle when they send packets.
    // This will usually be stuff like typing while in screen GUIs.
    player match {
      case mp: ServerPlayerEntity => mp.resetLastActionTime()
    }
  }
}

abstract class PacketHandler {
  /**
    * Gets the world for the specified dimension.
    *
    * For clients this returns the client's world if it is the specified
    * dimension; None otherwise. For the server it returns the world for the
    * specified dimension, if such a dimension exists; None otherwise.
    */
  protected def world(player: PlayerEntity, dimension: ResourceLocation): Option[World]

  protected def dispatch(p: PacketParser): Unit

  protected def createParser(stream: InputStream, player: PlayerEntity): PacketParser

  private[oc] class PacketParser(stream: InputStream, val player: PlayerEntity) extends DataInputStream(stream) {
    val packetType = PacketType(readByte())

    def readRegistryEntry[T <: IForgeRegistryEntry[T]](registry: IForgeRegistry[T]): T =
      registry.asInstanceOf[ForgeRegistry[T]].getValue(readInt())

    def getBlockEntity[T: ClassTag](dimension: ResourceLocation, x: Int, y: Int, z: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) if world.blockExists(BlockPosition(x, y, z)) =>
          val t = world.getBlockEntity(BlockPosition(x, y, z))
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

    def getEntity[T: ClassTag](dimension: ResourceLocation, id: Int): Option[T] = {
      world(player, dimension) match {
        case Some(world) =>
          val e = world.getEntity(id)
          if (e != null && classTag[T].runtimeClass.isAssignableFrom(e.getClass)) {
            return Some(e.asInstanceOf[T])
          }
        case _ =>
      }
      None
    }

    def readBlockEntity[T: ClassTag](): Option[T] = {
      val dimension = new ResourceLocation(readUTF())
      val x = readInt()
      val y = readInt()
      val z = readInt()
      getBlockEntity(dimension, x, y, z)
    }

    def readEntity[T: ClassTag](): Option[T] = {
      val dimension = new ResourceLocation(readUTF())
      val id = readInt()
      getEntity[T](dimension, id)
    }

    def readDirection(): Option[Direction] = readByte() match {
      case id if id < 0 => None
      case id => Option(Direction.from3DDataValue(id))
    }

    def readItemStack(): ItemStack = {
      val haveStack = readBoolean()
      if (haveStack) {
        ItemStack.of(readNBT())
      }
      else ItemStack.EMPTY
    }

    def readNBT(): CompoundNBT = {
      val haveNbt = readBoolean()
      if (haveNbt) {
        CompressedStreamTools.read(this)
      }
      else null
    }

    def readPacketType() = PacketType(readByte())
  }
}
