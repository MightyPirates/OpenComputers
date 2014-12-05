package lordfokas.stargatetech2.api;

import lordfokas.stargatetech2.api.bus.IBusDevice;
import lordfokas.stargatetech2.api.bus.IBusDriver;
import lordfokas.stargatetech2.api.bus.IBusInterface;

/**
 * A factory for private classes that implement
 * interfaces from the public API.
 * 
 * @author LordFokas
 */
public interface IFactory {
	public IBusInterface getIBusInterface(IBusDevice device, IBusDriver driver);
}
