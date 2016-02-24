package li.cil.oc.integration.thaumicenergistics

import appeng.api.storage.data.IAEFluidStack

object ThaumicEnergisticsUtils {
  def getAspect(fluid: IAEFluidStack) = {
    val aspect = fluid.getFluidStack.copy()
    aspect.amount = (fluid.getStackSize / 128).toInt
    aspect
  }
}
