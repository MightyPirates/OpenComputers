package li.cil.oc.api

import li.cil.oc.server.computer.Drivers

object OpenComputersAPI {
  def addDriver(driver: IBlockDriver) {
    Drivers.add(driver)
  }

  def addDriver(driver: IItemDriver) {
    Drivers.add(driver)
  }
}