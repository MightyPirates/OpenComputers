package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.internal
import li.cil.oc.server.PacketSender
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait Colored extends TileEntity with internal.Colored {
  private var _color = 0

  def color = _color

  def color_=(value: Int) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  def consumesDye = false

  override def getColor: Int = color

  override def setColor(value: Int) = color = value

  protected def onColorChanged() {
    if (world != null && isServer) {
      PacketSender.sendColorChange(this)
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "renderColor")) {
      _color = nbt.getInteger(Settings.namespace + "renderColor")
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
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
