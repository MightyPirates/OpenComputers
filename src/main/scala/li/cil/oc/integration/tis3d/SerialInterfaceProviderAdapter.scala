package li.cil.oc.integration.tis3d

import java.util.Optional

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.util.ResultWrapper.result
import li.cil.tis3d.api.serial.SerialInterface
import li.cil.tis3d.api.serial.SerialInterfaceProvider
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference
import li.cil.tis3d.common.provider.SerialInterfaceProviders
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.registries.ForgeRegistryEntry

import scala.collection.mutable

object SerialInterfaceProviderAdapter extends ForgeRegistryEntry[SerialInterfaceProvider] with SerialInterfaceProvider {
  setRegistryName(OpenComputers.ID, "serial_port")

  override def getDocumentationReference = Optional.of(new SerialProtocolDocumentationReference(new StringTextComponent("OpenComputers Adapter"), "protocols/opencomputersadapter.md"))

  override def matches(world: World, pos: BlockPos, side: Direction): Boolean = world.getBlockEntity(pos).isInstanceOf[Adapter]

  override def getInterface(world: World, pos: BlockPos, side: Direction): Optional[SerialInterface] = Optional.of(new SerialInterfaceAdapter(world.getBlockEntity(pos).asInstanceOf[Adapter]))

  override def stillValid(world: World, pos: BlockPos, side: Direction, serialInterface: SerialInterface): Boolean = serialInterface match {
    case adapter: SerialInterfaceAdapter => adapter.tileEntity == world.getBlockEntity(pos)
    case _ => false
  }

  class SerialInterfaceAdapter(val tileEntity: Adapter) extends Environment with SerialInterface {
    final val BufferCapacity = 128
    final val readBuffer = mutable.Queue.empty[Short]
    final val writeBuffer = mutable.Queue.empty[Short]
    var isReading = false

    // ----------------------------------------------------------------------- //

    val node = api.Network.newNode(this, Visibility.Network).withComponent("serial_port").create()

    override def onMessage(message: Message): Unit = {}

    override def onConnect(node: Node): Unit = {}

    override def onDisconnect(node: Node): Unit = {}

    // ----------------------------------------------------------------------- //

    @Callback
    def setReading(context: Context, args: Arguments): Array[AnyRef] = {
      isReading = args.checkBoolean(0)
      null
    }

    @Callback
    def read(context: Context, args: Arguments): Array[AnyRef] = {
      readBuffer.synchronized(if (readBuffer.nonEmpty) {
        result(readBuffer.dequeue())
      } else {
        null
      })
    }

    @Callback
    def write(context: Context, args: Arguments): Array[AnyRef] = {
      writeBuffer.synchronized(if (writeBuffer.length < BufferCapacity) {
        writeBuffer += args.checkInteger(0).toShort
        result(true)
      } else {
        result(false, "buffer full")
      })
    }

    // ----------------------------------------------------------------------- //

    override def canWrite: Boolean = readBuffer.synchronized(isReading && readBuffer.length < BufferCapacity)

    override def write(value: Short): Unit = readBuffer.synchronized(readBuffer += value)

    override def canRead: Boolean = {
      ensureConnected()
      writeBuffer.synchronized(writeBuffer.nonEmpty)
    }

    override def peek(): Short = writeBuffer.synchronized(writeBuffer.front)

    override def skip(): Unit = writeBuffer.synchronized(writeBuffer.dequeue())

    override def reset(): Unit = {
      readBuffer.synchronized(writeBuffer.synchronized {
        readBuffer.clear()
        writeBuffer.clear()
        node.remove()
      })
    }

    override def readFromNBT(nbt: CompoundNBT): Unit = {
      node.loadData(nbt)

      writeBuffer.clear()
      writeBuffer ++= nbt.getIntArray("writeBuffer").map(_.toShort)
      readBuffer.clear()
      readBuffer ++= nbt.getIntArray("readBuffer").map(_.toShort)
      isReading = nbt.getBoolean("isReading")
    }

    override def writeToNBT(nbt: CompoundNBT): Unit = {
      node.saveData(nbt)

      nbt.putIntArray("writeBuffer", writeBuffer.toArray.map(_.toInt))
      nbt.putIntArray("readBuffer", readBuffer.toArray.map(_.toInt))
      nbt.putBoolean("isReading", isReading)
    }

    private def ensureConnected(): Unit = {
      if (tileEntity.node.network != node.network) {
        tileEntity.node.connect(node)
      }
    }
  }

}
