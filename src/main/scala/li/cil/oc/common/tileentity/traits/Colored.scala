package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.server.PacketSender
import net.minecraft.nbt.NBTTagCompound

trait Colored extends TileEntity {
  private var _color = 0

  def color = _color

  def color_=(value: Int) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  protected def onColorChanged() {
    if (world != null && isServer) {
      PacketSender.sendColorChange(this)
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "renderColor")) {
      _color = nbt.getInteger(Settings.namespace + "renderColor")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setInteger(Settings.namespace + "renderColor", _color)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    _color = nbt.getInteger("renderColor")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("renderColor", _color)
  }
}
