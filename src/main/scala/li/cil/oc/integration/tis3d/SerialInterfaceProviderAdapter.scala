package li.cil.oc.integration.tis3d

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
import li.cil.tis3d.api.ManualAPI
import li.cil.tis3d.api.SerialAPI
import li.cil.tis3d.api.prefab.manual.ResourceContentProvider
import li.cil.tis3d.api.serial.SerialInterface
import li.cil.tis3d.api.serial.SerialInterfaceProvider
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

import scala.collection.mutable

object SerialInterfaceProviderAdapter extends SerialInterfaceProvider {
  def init(): Unit = {
    ManualAPI.addProvider(new ResourceContentProvider(Settings.resourceDomain, "doc/tis3d/"))
    SerialAPI.addProvider(this)
  }

  override def getDocumentationReference = new SerialProtocolDocumentationReference("OpenComputers Adapter", "protocols/opencomputersAdapter.md")

  override def worksWith(world: World, x: Int, y: Int, z: Int, side: EnumFacing): Boolean = world.getTileEntity(x, y, z).isInstanceOf[Adapter]

  override def interfaceFor(world: World, x: Int, y: Int, z: Int, side: EnumFacing): SerialInterface = new SerialInterfaceAdapter(world.getTileEntity(x, y, z).asInstanceOf[Adapter])

  override def isValid(world: World, x: Int, y: Int, z: Int, side: EnumFacing, serialInterface: SerialInterface): Boolean = serialInterface match {
    case adapter: SerialInterfaceAdapter => adapter.tileEntity.asInstanceOf[TileEntity] == world.getTileEntity(x, y, z)
    case _ => false
  }

  class SerialInterfaceAdapter(val tileEntity: Adapter) extends Environment with SerialInterface {
    final val BufferCapacity = 128
    final val readBuffer = mutable.Queue.empty[Short]
    final val writeBuffer = mutable.Queue.empty[Short]
    var isReading = false

    // ----------------------------------------------------------------------- //

    val node: Node = api.Network.newNode(this, Visibility.Network).withComponent("serial_port").create()

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

    override def readFromNBT(nbt: NBTTagCompound): Unit = {
      node.load(nbt)

      writeBuffer.clear()
      writeBuffer ++= nbt.getIntArray("writeBuffer").map(_.toShort)
      readBuffer.clear()
      readBuffer ++= nbt.getIntArray("readBuffer").map(_.toShort)
      isReading = nbt.getBoolean("isReading")
    }

    override def writeToNBT(nbt: NBTTagCompound): Unit = {
      node.save(nbt)

      nbt.setIntArray("writeBuffer", writeBuffer.toArray.map(_.toInt))
      nbt.setIntArray("readBuffer", readBuffer.toArray.map(_.toInt))
      nbt.setBoolean("isReading", isReading)
    }

    private def ensureConnected(): Unit = {
      if (tileEntity.node.network != node.network) {
        tileEntity.node.connect(node)
      }
    }
  }

}
