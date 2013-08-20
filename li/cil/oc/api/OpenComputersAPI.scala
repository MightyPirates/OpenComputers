package li.cil.oc.api

import li.cil.oc.server.computer.Drivers

object OpenComputersAPI {
  def addDriver(driver: IBlockDriver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.addDriver(driver)
  }

  def addDriver(driver: IItemDriver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.addDriver(driver)
  }
}