package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.Node
import li.cil.oc.common.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.PackedColor
import net.minecraft.nbt.NBTTagCompound

trait Buffer extends Environment with component.Buffer.Owner {
  protected val _buffer = new component.Buffer(this)

  protected var _bufferIsDirty = false

  lazy val buffer = _buffer

  def bufferIsDirty = _bufferIsDirty

  def bufferIsDirty_=(value: Boolean) = _bufferIsDirty = value

  override def node: Node = buffer.node

  override def tier: Int

  def hasKeyboard = true

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    buffer.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    buffer.save(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    buffer.buffer.load(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    buffer.buffer.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def onScreenColorChange(foreground: Int, background: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenColorChange(buffer, foreground, background)
    }
  }

  override def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenCopy(buffer, col, row, w, h, tx, ty)
    }
    else markForRenderUpdate()
  }

  override def onScreenDepthChange(depth: PackedColor.Depth.Value) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenDepthChange(buffer, depth)
    }
    else markForRenderUpdate()
  }

  override def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenFill(buffer, col, row, w, h, c)
    }
    else markForRenderUpdate()
  }

  override def onScreenResolutionChange(w: Int, h: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenResolutionChange(buffer, w, h)
    }
    else markForRenderUpdate()
  }

  override def onScreenSet(col: Int, row: Int, s: String) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this)
      ServerPacketSender.sendScreenSet(buffer, col, row, s)
    }
    else markForRenderUpdate()
  }

  @SideOnly(Side.CLIENT)
  protected def markForRenderUpdate() {
    bufferIsDirty = true
  }
}
