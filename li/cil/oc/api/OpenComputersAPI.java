package li.cil.oc.api;

import li.cil.oc.server.computer.Drivers;

public class OpenComputersAPI {
  static void addDriver(IBlockDriver driver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.add(driver);
  }

  static void addDriver(IItemDriver driver) {
    // TODO Use reflection to allow distributing the API.
    Drivers.add(driver);
  }
}