package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.Optional.Interface
import stargatetech2.api.bus.IBusDevice

@Optional(new Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2"))
trait AbstractBusAware extends IBusDevice {
  def getInterfaces(side: Int) = if (hasAbstractBusCard) Array(null) else null

  protected def hasAbstractBusCard = false
}
