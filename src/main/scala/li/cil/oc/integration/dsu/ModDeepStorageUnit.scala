package li.cil.oc.integration.dsu

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

// This is it's own "mod" instead of part of MFR, because some mods like JABBA
// use this interface, too, and ship that part of the MFR API.
object ModDeepStorageUnit extends ModProxy {
  override def getMod = Mods.DeepStorageUnit

  override def initialize() {
    Driver.add(DriverDeepStorageUnit)
  }
}
