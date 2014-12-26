package li.cil.oc.common.tileentity.traits

import net.minecraft.item.EnumDyeColor
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.internal
import li.cil.oc.server.PacketSender
import net.minecraft.nbt.NBTTagCompound

trait Colored extends TileEntity with internal.Colored {
  private var _color = EnumDyeColor.SILVER

  def color = _color

  def color_=(value: EnumDyeColor) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  override def getColor: EnumDyeColor = color

  override def setColor(value: EnumDyeColor) = color = value

  protected def onColorChanged() {
    if (world != null && isServer) {
      PacketSender.sendColorChange(this)
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "renderColor")) {
      _color = EnumDyeColor.byMetadata(nbt.getInteger(Settings.namespace + "renderColor"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setInteger(Settings.namespace + "renderColor", _color.getMetadata)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    _color = EnumDyeColor.byMetadata(nbt.getInteger("renderColor"))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("renderColor", _color.getMetadata)
  }
}
