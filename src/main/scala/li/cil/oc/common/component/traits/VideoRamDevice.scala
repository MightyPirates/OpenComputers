package li.cil.oc.common.component.traits

import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

trait VideoRamDevice {
  private val internalBuffers = new mutable.HashMap[Int, component.GpuTextBuffer]
  val RESERVED_SCREEN_INDEX: Int = 0

  def isEmpty: Boolean = internalBuffers.isEmpty

  def onBufferRamDestroy(id: Int): Unit = {}

  def bufferIndexes(): Array[Int] = internalBuffers.collect {
    case (index: Int, _: Any) => index
  }.toArray

  def addBuffer(ram: component.GpuTextBuffer): Boolean = {
    val preexists = internalBuffers.contains(ram.id)
    internalBuffers += ram.id -> ram
    preexists
  }

  def removeBuffers(ids: Array[Int]): Int = {
    var count = 0
    if (ids.nonEmpty) {
      for (id <- ids) {
        if (internalBuffers.remove(id).nonEmpty) {
          onBufferRamDestroy(id)
          count += 1
        }
      }
    }
    count
  }

  def removeAllBuffers(): Int = removeBuffers(bufferIndexes())

  def loadBuffer(address: String, id: Int, nbt: NBTTagCompound): Unit = {
    val src = new li.cil.oc.util.TextBuffer(width = 1, height = 1, li.cil.oc.util.PackedColor.SingleBitFormat)
    src.load(nbt)
    addBuffer(component.GpuTextBuffer.wrap(address, id, src))
  }

  def getBuffer(id: Int): Option[component.GpuTextBuffer] = {
    if (internalBuffers.contains(id))
      Option(internalBuffers(id))
    else
      None
  }

  def nextAvailableBufferIndex: Int = {
    var index = RESERVED_SCREEN_INDEX + 1
    while (internalBuffers.contains(index)) {
      index += 1;
    }
    index
  }

  def calculateUsedMemory(): Int = {
    var sum: Int = 0
    for ((_, buffer: component.GpuTextBuffer) <- internalBuffers) {
      sum += buffer.data.width * buffer.data.height
    }
    sum
  }
}
