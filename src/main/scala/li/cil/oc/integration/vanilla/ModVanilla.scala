package li.cil.oc.integration.vanilla

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModVanilla extends ModProxy {
  def getMod = Mods.Minecraft

  def initialize() {
    Driver.add(new DriverBeacon)
    Driver.add(new DriverBrewingStand)
    Driver.add(new DriverComparator)
    Driver.add(new DriverFurnace)
    Driver.add(new DriverMobSpawner)
    Driver.add(new DriverNoteBlock)
    Driver.add(new DriverRecordPlayer)

    if (Settings.get.enableInventoryDriver) {
      Driver.add(new DriverInventory)
    }
    if (Settings.get.enableTankDriver) {
      Driver.add(new DriverFluidHandler)
      Driver.add(new DriverFluidTank)
    }
    if (Settings.get.enableCommandBlockDriver) {
      Driver.add(new DriverCommandBlock)
    }

    Driver.add(ConverterFluidStack)
    Driver.add(ConverterFluidTankInfo)
    Driver.add(ConverterItemStack)
    Driver.add(ConverterNBT)
    Driver.add(ConverterWorld)
    Driver.add(ConverterWorldProvider)
    RecipeRegistry.init()
  }
}
