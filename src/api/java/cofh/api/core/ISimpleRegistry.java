package cofh.api.core;

/**
 * Basic interface for the Cape and Skin Registries.
 * 
 * @author Zeldo Kavira
 * 
 */
public interface ISimpleRegistry {

	/**
	 * Register a new entry.
	 * 
	 * @param playerName
	 *            The player to register.
	 * @param URL
	 *            Location of the cape/skin.
	 * @return True if registration was successful.
	 */
	public boolean register(String playerName, String URL);

}
