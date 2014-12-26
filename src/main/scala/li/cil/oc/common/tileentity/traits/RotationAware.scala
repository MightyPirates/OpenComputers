package li.cil.oc.common.tileentity.traits

import net.minecraft.util.EnumFacing

trait RotationAware extends TileEntity {
  def toLocal(value: EnumFacing) = value

  def toGlobal(value: EnumFacing) = value
}
