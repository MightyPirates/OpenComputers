package li.cil.oc.common.tileentity.traits

import net.minecraftforge.common.ForgeDirection

trait RotationAware extends TileEntity {
  def toLocal(value: ForgeDirection) = value

  def toGlobal(value: ForgeDirection) = value
}
