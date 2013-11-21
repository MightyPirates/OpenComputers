package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.Node
import li.cil.oc.client.gui
import li.cil.oc.common.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.{PackedColor, Persistable}
import net.minecraft.nbt.NBTTagCompound

trait Buffer extends Environment with Persistable {
  protected val _buffer = new component.Buffer(this)

  protected var _bufferIsDirty = false

  protected var _currentGui = None: Option[gui.Buffer]

  def buffer = _buffer

  def bufferIsDirty = _bufferIsDirty

  def bufferIsDirty_=(value: Boolean) = _bufferIsDirty = value

  def currentGui = _currentGui

  def currentGui_=(value: Option[gui.Buffer]) = _currentGui = value

  def node: Node = buffer.node

  def tier: Int

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    buffer.load(nbt)
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    buffer.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  def onScreenColorChange(foreground: Int, background: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenColorChange(this, foreground, background)
    }
  }

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenCopy(this, col, row, w, h, tx, ty)
    }
    else markForRenderUpdate()
  }

  def onScreenDepthChange(depth: PackedColor.Depth.Value) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenDepthChange(this, depth)
    }
    else markForRenderUpdate()
  }

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenFill(this, col, row, w, h, c)
    }
    else markForRenderUpdate()
  }

  def onScreenResolutionChange(w: Int, h: Int) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenResolutionChange(this, w, h)
    }
    else markForRenderUpdate()
  }

  def onScreenSet(col: Int, row: Int, s: String) {
    if (isServer) {
      world.markTileEntityChunkModified(x, y, z, this.asInstanceOf[net.minecraft.tileentity.TileEntity])
      ServerPacketSender.sendScreenSet(this, col, row, s)
    }
    else markForRenderUpdate()
  }

  @SideOnly(Side.CLIENT)
  protected def markForRenderUpdate() {
    bufferIsDirty = true
  }
}
