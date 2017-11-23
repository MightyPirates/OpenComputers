package li.cil.oc.integration.ec

import appeng.api.AEApi
import appeng.api.storage.data.IAEFluidStack
import extracells.api.ECApi
import extracells.api.gas.{IAEGasStack, IGasStorageChannel}

object ECUtil {
  val isGasSystemEnabled = ECApi.instance.isGasSystemEnabled
  val gasStorageChannel = if (isGasSystemEnabled) AEApi.instance.storage.getStorageChannel[IAEGasStack, IGasStorageChannel](classOf[IGasStorageChannel]) else null

  def canSeeFluidInNetwork(fluid: IAEFluidStack) = fluid != null && ECApi.instance.canFluidSeeInTerminal(fluid.getFluid)
}
