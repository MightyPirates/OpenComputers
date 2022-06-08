package li.cil.oc.api.network;

/**
 * This interface can be implemented by ComputerCraft peripherals, to allow
 * dynamically deciding whether OC should wrap the peripheral or not.
 * <br>
 * If you have an OC driver equivalent to your peripheral and the more broad,
 * IMC based method (which works purely on class names) doesn't work for you,
 * use this.
 */
public interface BlacklistedPeripheral {
    boolean isPeripheralBlacklisted();
}
