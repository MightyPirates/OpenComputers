package li.cil.oc.integration.vanilla

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModVanilla extends ModProxy {
  def getMod = Mods.Minecraft

  def initialize() {
    Driver.add(new DriverBeacon)
    Driver.add(new DriverBrewingStand)
    Driver.add(new DriverCommandBlock)
    Driver.add(new DriverComparator)
    Driver.add(new DriverFluidHandler)
    Driver.add(new DriverFluidTank)
    Driver.add(new DriverFurnace)
    Driver.add(new DriverInventory)
    Driver.add(new DriverMobSpawner)
    Driver.add(new DriverNoteBlock)
    Driver.add(new DriverRecordPlayer)
    Driver.add(new DriverSign)

    Driver.add(ConverterFluidStack)
    Driver.add(ConverterFluidTankInfo)
    Driver.add(ConverterItemStack)
  }
}
