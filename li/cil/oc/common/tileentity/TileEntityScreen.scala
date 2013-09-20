package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher._
import li.cil.oc.client.{ PacketSender => ClientPacketSender }
import li.cil.oc.client.gui.GuiScreen
import li.cil.oc.common.components.IScreenEnvironment
import li.cil.oc.common.components.Screen
import li.cil.oc.server.{ PacketSender => ServerPacketSender }
import net.minecraft.nbt.NBTTagCompound

class TileEntityScreen extends TileEntityRotatable with IScreenEnvironment {
  val component = new Screen(this)

  var gui: Option[GuiScreen] = None

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    component.readFromNBT(nbt.getCompoundTag("component"))
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val componentNbt = new NBTTagCompound
    component.writeToNBT(componentNbt)
    nbt.setCompoundTag("component", componentNbt)
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