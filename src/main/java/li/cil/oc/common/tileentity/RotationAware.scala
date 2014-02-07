package li.cil.oc.common.tileentity

import net.minecraftforge.common.util.ForgeDirection

trait RotationAware extends TileEntity {
  def toLocal(value: ForgeDirection) = value

  def toGlobal(value: ForgeDirection) = value
}
