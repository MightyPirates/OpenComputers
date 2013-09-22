package li.cil.oc.api.scala

import li.cil.oc.server.computer.Drivers

object OpenComputersAPI {
  def addDriver(driver: IBlockDriver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.add(driver)
  }

  def addDriver(driver: IItemDriver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.add(driver)
  }
}