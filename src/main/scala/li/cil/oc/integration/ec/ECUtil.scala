package li.cil.oc.integration.ec

import appeng.api.storage.data.IAEFluidStack
import extracells.api.ECApi

object ECUtil {
  def canSeeFluidInNetwork(fluid: IAEFluidStack) = fluid != null && ECApi.instance.canFluidSeeInTerminal(fluid.getFluid)
}
