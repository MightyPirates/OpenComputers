package cofh.api.core;

/**
 * Dummy class used to initialize Cape and Skin Registries to prevent accidental NPEs.
 * 
 * @author Zeldo Kavira
 * 
 */
public class NullSimpleRegistry implements ISimpleRegistry {

	@Override
	public boolean register(String playerName, String URL) {

		return false;
	}

}
