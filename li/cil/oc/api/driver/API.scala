package li.cil.oc.api.driver

import li.cil.oc.server.computer.Drivers

object API {
  def addDriver(driver: Block) {
    Drivers.add(driver)
  }

  def addDriver(driver: Item) {
    Drivers.add(driver)
  }
}