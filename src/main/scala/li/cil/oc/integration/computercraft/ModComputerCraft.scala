package li.cil.oc.integration.computercraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModComputerCraft extends IMod {
  override def getMod = Mods.ComputerCraft

  override def initialize() {
    Driver.add(DriverComputerCraftMedia)

    try {
      val driver: DriverPeripheral = new DriverPeripheral
      if (driver.isValid) {
        Driver.add(new ConverterLuaObject)
        Driver.add(driver)
      }
    }
    catch {
      case ignored: Throwable =>
    }
  }
}