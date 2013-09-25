package li.cil.oc.common.tileentity

import li.cil.oc.client.gui.GuiScreen
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.components.IScreenEnvironment
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound

class TileEntityScreen extends TileEntityRotatable with IScreenEnvironment {
  var gui: Option[GuiScreen] = None

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    screen.readFromNBT(nbt.getCompoundTag("screen"))
    load(nbt.getCompoundTag("data"))
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val screenNbt = new NBTTagCompound
    screen.writeToNBT(screenNbt)
    nbt.setCompoundTag("screen", screenNbt)

    val dataNbt = new NBTTagCompound
    save(dataNbt)
    nbt.setCompoundTag("data", dataNbt)
  }

  override def validate() = {
    super.validate()
    if (worldObj.isRemote)
      ClientPacketSender.sendScreenBufferRequest(this)
  }

  // ----------------------------------------------------------------------- //
  // IScreenEnvironment
  // ----------------------------------------------------------------------- //

  def onScreenResolutionChange(w: Int, h: Int) =
    if (worldObj.isRemote) {
      gui.foreach(_.setSize(w, h))
    }
    else {
      markAsChanged()
      ServerPacketSender.sendScreenResolutionChange(this, w, h)
    }

  def onScreenSet(col: Int, row: Int, s: String) =
    if (worldObj.isRemote) {
      gui.foreach(_.updateText())
    }
    else {
      markAsChanged()
      ServerPacketSender.sendScreenSet(this, col, row, s)
    }

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    if (worldObj.isRemote) {
      gui.foreach(_.updateText())
    }
    else {
      markAsChanged()
      ServerPacketSender.sendScreenFill(this, col, row, w, h, c)
    }

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    if (worldObj.isRemote) {
      gui.foreach(_.updateText())
    }
    else {
      markAsChanged()
      ServerPacketSender.sendScreenCopy(this, col, row, w, h, tx, ty)
    }

  private def markAsChanged(): Unit =
    worldObj.updateTileEntityChunkAndDoNothing(
      xCoord, yCoord, zCoord, this)
}