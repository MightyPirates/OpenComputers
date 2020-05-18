package li.cil.oc.common.component.traits

import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound

trait VideoRamAware {
  private val internalBuffers = new scala.collection.mutable.HashMap[Int, component.GpuTextBuffer]
  val RESERVED_SCREEN_INDEX: Int = 0

  def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, id: Int, fromCol: Int, fromRow: Int): Unit = {}
  def onBufferRamInit(id: Int, ram: TextBufferProxy): Unit = {}
  def onBufferRamDestroy(ids: Array[Int]): Unit = {}

  def bufferIndexes(): Array[Int] = internalBuffers.collect {
    case (index: Int, _: Any) => index
  }.toArray

  def addBuffer(buffer: component.GpuTextBuffer): Boolean = {
    val preexists = internalBuffers.contains(buffer.id)
    if (!preexists) {
      internalBuffers += buffer.id -> buffer
    }
    if (!preexists || buffer.dirty) {
      buffer.onBufferRamInit(buffer.id, buffer)
      onBufferRamInit(buffer.id, buffer)
    }
    preexists
  }

  def removeBuffers(ids: Array[Int]): Boolean = {
    var allRemoved: Boolean = true
    if (ids.nonEmpty) {
      onBufferRamDestroy(ids)
      for (id <- ids) {
        if (internalBuffers.remove(id).isEmpty)
          allRemoved = false
      }
    }
    allRemoved
  }

  def removeAllBuffers(): Boolean = removeBuffers(bufferIndexes())

  def loadBuffer(id: Int, nbt: NBTTagCompound): Unit = {
    val src = new li.cil.oc.util.TextBuffer(width = 1, height = 1, li.cil.oc.util.PackedColor.SingleBitFormat)
    src.load(nbt)
    addBuffer(component.GpuTextBuffer.wrap(id, src))
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
