package li.cil.oc.integration.ec

import appeng.api.storage.data.IAEFluidStack
import extracells.api.ECApi


object ECUtil {

  val api = ECApi.instance

  def canSeeFluidInNetwork(fluid: IAEFluidStack) = fluid != null && api.canFluidSeeInTerminal(fluid.getFluid)

}
