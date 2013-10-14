package li.cil.oc.common.tileentity

import li.cil.oc.api.network.PoweredNode
import li.cil.oc.client.gui
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound

class Screen extends Rotatable with component.Screen.Environment with PoweredNode {
  var guiScreen: Option[gui.Screen] = None

  /**
   * Read and reset to false from the tile entity renderer. This is used to
   * keep rendering a little more efficient by compiling the displayed text
   * into an OpenGL display list, and only re-compiling that list when the
   * text/display has actually changed.
   */
  var hasChanged = false

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    load(nbt.getCompoundTag("node"))
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val nodeNbt = new NBTTagCompound
    save(nodeNbt)
    nbt.setCompoundTag("node", nodeNbt)
  }

  override def validate() = {
    super.validate()
    if (worldObj.isRemote)
      ClientPacketSender.sendScreenBufferRequest(this)
  }

  // ----------------------------------------------------------------------- //

  override def onScreenResolutionChange(w: Int, h: Int) = {
    super.onScreenResolutionChange(w, h)
    if (worldObj.isRemote) {
      guiScreen.foreach(_.setSize(w, h))
      hasChanged = true
    }
    else {
      worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      ServerPacketSender.sendScreenResolutionChange(this, w, h)
    }
  }

  override def onScreenSet(col: Int, row: Int, s: String) = {
    super.onScreenSet(col, row, s)
    if (worldObj.isRemote) {
      guiScreen.foreach(_.updateText())
      hasChanged = true
    }
    else {
      worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      ServerPacketSender.sendScreenSet(this, col, row, s)
    }
  }

  override def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) = {
    super.onScreenFill(col, row, w, h, c)
    if (worldObj.isRemote) {
      guiScreen.foreach(_.updateText())
      hasChanged = true
    }
    else {
      worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      ServerPacketSender.sendScreenFill(this, col, row, w, h, c)
    }
  }

  override def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) = {
    super.onScreenCopy(col, row, w, h, tx, ty)
    if (worldObj.isRemote) {
      guiScreen.foreach(_.updateText())
      hasChanged = true
    }
    else {
      worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      ServerPacketSender.sendScreenCopy(this, col, row, w, h, tx, ty)
    }
  }
}