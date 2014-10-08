package cofh.api.core;

/**
 * Interface which can be put on just about anything to allow for iteration during initialization.
 * 
 * @author King Lemming
 * 
 */
public interface IInitializer {

	boolean preInit();

	boolean initialize();

	boolean postInit();

}
