package stargatetech2.api.shields;

/**
 * Implemented by the shield emitter TileEntities.
 * 
 * @author LordFokas
 */
public interface ITileShieldEmitter {
	/**
	 * Given the way shield emitters work together, you cannot
	 * directly access their ShieldPermissions object.
	 * This means you cannot use this method to change
	 * permissions on a shield.
	 * 
	 * @return A deep clone of  theShieldPermissions object that
	 * defines this emitter's shield behavior.
	 */
	public ShieldPermissions getPermissions();
	
	/**
	 * @return True if the shield is activated, false otherwise.
	 */
	public boolean isShieldOn();
	
	/**
	 * Update the permissions on this emitter.
	 * It will propagate to the whole shield.
	 * 
	 * @param isAllow true if allowing this flag, false if disallowing.
	 * @param flag The flag we're (dis)allowing.
	 * @see stargatetech2.api.ShieldPermissions
	 */
	public void updatePermissions(boolean isAllow, int flag);
	
	/**
	 * Update the exceptions on this emitter.
	 * It will propagate to the whole shield.
	 * 
	 * @param isAdding true if we're adding a player to the exceptions, false if removing.
	 * @param player The name of the player we're adding / removing.
	 * @see stargatetech2.api.ShieldPermissions
	 */
	public void updateExceptions(boolean isAdding, String player);
	
	/**
	 * Sets the owner of this Shield Emitter.
	 * An owner has previleges no other players have.
	 * 
	 * @param owner The owner's player name.
	 */
	public void setOwner(String owner);
	
	/**
	 * @return The player name of this machine's owner.
	 */
	public String getOwner();
	
	/**
	 * Checks if a player can access this device.
	 * 
	 * @param player The player's name.
	 * @return Whether or not this player can access this device.
	 */
	public boolean canAccess(String player);
}