package stargatetech2.api.shields;

/**
 * Implemented by the Shield Controller TileEntities.
 * 
 * @author LordFokas
 */
public interface IShieldController {
	/**
	 * Given the way shield emitters work together, you cannot
	 * directly access their ShieldPermissions object.
	 * This means you cannot use this method to change
	 * permissions on a shield.
	 * 
	 * @return A deep clone of the ShieldPermissions object that
	 * defines this controller's shield behavior.
	 */
	public ShieldPermissions getPermissions();
	
	/**
	 * @return True if the shield is activated, false otherwise.
	 */
	public boolean isShieldOn();
	
	/**
	 * @return The name of the player who owns this Shield Controller.
	 */
	public String getOwner();
	
	/**
	 * Checks if a player can access this device.
	 * 
	 * @param player The player's name.
	 * @return Whether or not this player can access this device.
	 */
	public boolean hasAccess(String player);
}