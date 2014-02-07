package stargatetech2.api;

import stargatetech2.api.bus.IBusDevice;
import stargatetech2.api.bus.IBusDriver;
import stargatetech2.api.bus.IBusInterface;

/**
 * A factory for private classes that implement
 * interfaces from the public API.
 * 
 * @author LordFokas
 */
public interface IFactory {
	public IBusInterface getIBusInterface(IBusDevice device, IBusDriver driver);
}
