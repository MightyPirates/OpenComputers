package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.internal
import li.cil.oc.server.PacketSender
import li.cil.oc.util.Color
import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait Colored extends TileEntity with internal.Colored {
  private var _color = 0

  def consumesDye = false

  override def getColor: Int = _color

  override def setColor(value: Int) = if (value != _color) {
    _color = value
    onColorChanged()
  }

  override def controlsConnectivity = false

  protected def onColorChanged() {
    if (world != null && isServer) {
      PacketSender.sendColorChange(this)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val RenderColorTag = Settings.namespace + "renderColorRGB"
  private final val RenderColorTagCompat = Settings.namespace + "renderColor"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(RenderColorTagCompat)) {
      _color = Color.rgbValues(EnumDyeColor.byMetadata(nbt.getInteger(RenderColorTagCompat)))
    }
    if (nbt.hasKey(RenderColorTag)) {
      _color = nbt.getInteger(RenderColorTag)
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(RenderColorTag, _color)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    _color = nbt.getInteger(RenderColorTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger(RenderColorTag, _color)
  }
}
