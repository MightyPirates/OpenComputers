package li.cil.oc.api;

import li.cil.oc.server.computer.Drivers;

public final class OpenComputersAPI {
    public static void addDriver(IBlockDriver driver) {
        // TODO Use reflection to allow distributing the API.
        Drivers.add(driver);
    }

    public static void addDriver(IItemDriver driver) {
        // TODO Use reflection to allow distributing the API.
        Drivers.add(driver);
    }
}