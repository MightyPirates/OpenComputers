package li.cil.oc.common.inventory

import net.minecraftforge.fluids.IFluidTank

// TODO Move to api.internal in 1.5, make internal.Robot extend this.
trait MultiTank {
  def tankCount: Int

  def getFluidTank(index: Int): IFluidTank
}
