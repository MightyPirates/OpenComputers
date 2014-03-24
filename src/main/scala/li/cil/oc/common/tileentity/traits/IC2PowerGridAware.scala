package li.cil.oc.common.tileentity.traits

import ic2.api.energy.tile.IEnergySink
import cpw.mods.fml.common.Optional

@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
trait IC2PowerGridAware extends IEnergySink {
  var addedToPowerGrid = false
}
