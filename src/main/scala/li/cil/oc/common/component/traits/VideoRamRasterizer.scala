package li.cil.oc.common.component.traits

import li.cil.oc.common.component
import li.cil.oc.common.component.GpuTextBuffer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.village.VillageDoorInfo

import scala.collection.mutable

trait VideoRamRasterizer {
  class VirtualRamDevice(val owner: String) extends VideoRamDevice {}
  private val internalBuffers = new mutable.HashMap[String, VideoRamDevice]

  def onBufferRamInit(ram: component.GpuTextBuffer): Unit
  def onBufferBitBlt(col: Int, row: Int, w: Int, h: Int, ram: component.GpuTextBuffer, fromCol: Int, fromRow: Int): Unit
  def onBufferRamDestroy(ram: component.GpuTextBuffer): Unit

  def addBuffer(ram: GpuTextBuffer): Boolean = {
    var gpu = internalBuffers.get(ram.owner)
    if (gpu.isEmpty) {
      gpu = Option(new VirtualRamDevice(ram.owner))
      internalBuffers += ram.owner -> gpu.get
    }
    val preexists: Boolean = gpu.get.addBuffer(ram)
    if (!preexists || ram.dirty) {
      onBufferRamInit(ram)
    }
    preexists
  }

  def removeBuffer(owner: String, id: Int): Boolean = {
    internalBuffers.get(owner) match {
      case Some(gpu: VideoRamDevice) => {
        gpu.getBuffer(id) match {
          case Some(ram: component.GpuTextBuffer) => {
            onBufferRamDestroy(ram)
            gpu.removeBuffers(Array(id)) == 1
          }
          case _ => false
        }
      }
      case _ => false
    }
  }

  def removeAllBuffers(owner: String): Int = {
    var count = 0
    internalBuffers.get(owner) match {
      case Some(gpu: VideoRamDevice) => {
        val ids = gpu.bufferIndexes()
        for (id <- ids) {
          if (removeBuffer(owner, id)) {
            count += 1
          }
        }
      }
      case _ => Unit
    }
    count
  }

  def removeAllBuffers(): Int = {
    var count = 0
    for ((owner: String, _: Any) <- internalBuffers) {
      count += removeAllBuffers(owner)
    }
    count
  }

  def loadBuffer(owner: String, id: Int, nbt: NBTTagCompound): Boolean = {
    val src = new li.cil.oc.util.TextBuffer(width = 1, height = 1, li.cil.oc.util.PackedColor.SingleBitFormat)
    src.load(nbt)
    addBuffer(component.GpuTextBuffer.wrap(owner, id, src))
  }

  def getBuffer(owner: String, id: Int): Option[component.GpuTextBuffer] = {
    internalBuffers.get(owner) match {
      case Some(gpu: VideoRamDevice) => gpu.getBuffer(id)
      case _ => None
    }
  }
}
