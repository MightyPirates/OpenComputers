package li.cil.oc.common.component

import java.io.InvalidObjectException
import java.security.InvalidParameterException

import li.cil.oc.api.network.{Environment, Message, Node}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.api.internal.TextBuffer.ColorDepth
import li.cil.oc.api
import li.cil.oc.common.component.traits.{TextBufferProxy, VideoRamDevice, VideoRamRasterizer}

class GpuTextBuffer(val owner: String, val id: Int, val data: li.cil.oc.util.TextBuffer) extends traits.TextBufferProxy {

  // the gpu ram does not join nor is searchable to the network
  // this field is required because the api TextBuffer is an Environment
  override def node(): Node = {
    throw new InvalidObjectException("GpuTextBuffers do not have nodes")
  }


  override def getMaximumWidth: Int = data.width
  override def getMaximumHeight: Int = data.height
  override def getViewportWidth: Int = data.height
  override def getViewportHeight: Int = data.width

  var dirty: Boolean = true
  override def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = dirty = true
  override def onBufferColorChange(): Unit = dirty = true
  override def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = dirty = true
  override def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Char): Unit = dirty = true

  override def load(nbt: NBTTagCompound): Unit = {
    // the data is initially dirty because other devices don't know about it yet
    data.load(nbt)
    dirty = true
  }

  override def save(nbt: NBTTagCompound): Unit = {
    data.save(nbt)
    dirty = false
  }

  override def setEnergyCostPerTick(value: Double): Unit = {}
  override def getEnergyCostPerTick: Double = 0
  override def setPowerState(value: Boolean): Unit = {}
  override def getPowerState: Boolean = false
  override def setMaximumResolution(width: Int, height: Int): Unit = {}
  override def setAspectRatio(width: Double, height: Double): Unit = {}
  override def getAspectRatio: Double = 1
  override def setResolution(width: Int, height: Int): Boolean = false
  override def setViewport(width: Int, height: Int): Boolean = false
  override def setMaximumColorDepth(depth: ColorDepth): Unit = {}
  override def getMaximumColorDepth: ColorDepth = data.format.depth
  override def renderText: Boolean = false
  override def renderWidth: Int = 0
  override def renderHeight: Int = 0
  override def setRenderingEnabled(enabled: Boolean): Unit = {}
  override def isRenderingEnabled: Boolean = false
  override def keyDown(character: Char, code: Int, player: EntityPlayer): Unit = {}
  override def keyUp(character: Char, code: Int, player: EntityPlayer): Unit = {}
  override def clipboard(value: String, player: EntityPlayer): Unit = {}
  override def mouseDown(x: Double, y: Double, button: Int, player: EntityPlayer): Unit = {}
  override def mouseDrag(x: Double, y: Double, button: Int, player: EntityPlayer): Unit = {}
  override def mouseUp(x: Double, y: Double, button: Int, player: EntityPlayer): Unit = {}
  override def mouseScroll(x: Double, y: Double, delta: Int, player: EntityPlayer): Unit = {}
  override def canUpdate: Boolean = false
  override def update(): Unit = {}
  override def onConnect(node: Node): Unit = {}
  override def onDisconnect(node: Node): Unit = {}
  override def onMessage(message: Message): Unit = {}
}

object ClientGpuTextBufferHandler {
  def bitblt(dst: api.internal.TextBuffer, col: Int, row: Int, w: Int, h: Int, owner: String, srcId: Int, fromCol: Int, fromRow: Int): Unit = {
    dst match {
      case videoDevice: VideoRamRasterizer => videoDevice.getBuffer(owner, srcId) match {
        case Some(buffer: GpuTextBuffer) => {
          GpuTextBuffer.bitblt(dst, col, row, w, h, buffer, fromCol, fromRow)
        }
        case _ => // ignore - got a bitblt for a missing buffer
      }
      case _ => // ignore - weird packet handler called this, should only happen for video ram aware devices
    }
  }

  def removeBuffer(buffer: api.internal.TextBuffer, owner: String, id: Int): Boolean = {
    buffer match {
      case screen: VideoRamRasterizer => screen.removeBuffer(owner, id)
      case _ => false // ignore, not compatible with bitblts
    }
  }

  def loadBuffer(buffer: api.internal.TextBuffer, owner: String, id: Int, nbt: NBTTagCompound): Boolean = {
    buffer match {
      case screen: VideoRamRasterizer => screen.loadBuffer(owner, id, nbt)
      case _ => false // ignore, not compatible with bitblts
    }
  }
}

object GpuTextBuffer {
  def wrap(owner: String, id: Int, data: li.cil.oc.util.TextBuffer): GpuTextBuffer = new GpuTextBuffer(owner, id, data)

  def bitblt(dst: api.internal.TextBuffer, col: Int, row: Int, w: Int, h: Int, src: api.internal.TextBuffer, fromCol: Int, fromRow: Int): Unit = {
    val x = col - 1
    val y = row - 1
    val fx = fromCol - 1
    val fy = fromRow - 1
    var adjustedDstX = x
    var adjustedDstY = y
    var adjustedWidth = w
    var adjustedHeight = h
    var adjustedSourceX = fx
    var adjustedSourceY = fy

    if (x < 0) {
      adjustedWidth += x
      adjustedSourceX -= x
      adjustedDstX = 0
    }

    if (y < 0) {
      adjustedHeight += y
      adjustedSourceY -= y
      adjustedDstY = 0
    }

    if (adjustedSourceX < 0) {
      adjustedWidth += adjustedSourceX
      adjustedDstX -= adjustedSourceX
      adjustedSourceX = 0
    }

    if (adjustedSourceY < 0) {
      adjustedHeight += adjustedSourceY
      adjustedDstY -= adjustedSourceY
      adjustedSourceY = 0
    }

    adjustedWidth -= ((adjustedDstX + adjustedWidth) - dst.getWidth) max 0
    adjustedWidth -= ((adjustedSourceX + adjustedWidth) - src.getWidth) max 0

    adjustedHeight -= ((adjustedDstY + adjustedHeight) - dst.getHeight) max 0
    adjustedHeight -= ((adjustedSourceY + adjustedHeight) - src.getHeight) max 0

    // anything left?
    if (adjustedWidth <= 0 || adjustedHeight <= 0) {
      return
    }

    dst match {
      case dstScreen: TextBuffer => src match {
        case srcGpu: GpuTextBuffer => write_vram_to_screen(dstScreen, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcGpu, adjustedSourceX, adjustedSourceY)
        case _ => throw new UnsupportedOperationException("Source buffer does not support bitblt operations to a screen")
      }
      case dstGpu: GpuTextBuffer => src match {
        case srcProxy: TextBufferProxy => write_to_vram(dstGpu, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcProxy, adjustedSourceX, adjustedSourceY)
        case _ => throw new UnsupportedOperationException("Source buffer does not support bitblt operations")
      }
      case _ => throw new UnsupportedOperationException("Destination buffer does not support bitblt operations")
    }
  }

  def write_vram_to_screen(dstScreen: TextBuffer, x: Int, y: Int, w: Int, h: Int, srcRam: GpuTextBuffer, fx: Int, fy: Int): Boolean = {
    if (dstScreen.data.rawcopy(x + 1, y + 1, w, h, srcRam.data, fx + 1, fy + 1)) {
      // rawcopy returns true only if data was modified
      dstScreen.addBuffer(srcRam)
      dstScreen.onBufferBitBlt(x + 1, y + 1, w, h, srcRam, fx + 1, fy + 1)
      true
    } else false
  }

  def write_to_vram(dstRam: GpuTextBuffer, x: Int, y: Int, w: Int, h: Int, src: TextBufferProxy, fx: Int, fy: Int): Boolean = {
    dstRam.data.rawcopy(x + 1, y + 1, w, h, src.data, fx + 1, fy + 1)
  }
}
