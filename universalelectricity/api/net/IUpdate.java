package universalelectricity.api.net;

public interface IUpdate
{
	/**
	 * Updates the network. Called by the {NetworkTickHandler}.
	 */
	public void update();

	/**
	 * Can the network update?
	 * 
	 * @return True if the network can update, otherwise the network tick handler will remove the
	 * network from the tick list.
	 */
	public boolean canUpdate();

	/**
	 * @return True to leave the network in the ticker. False to remove the network from the ticker.
	 */
	public boolean continueUpdate();
}
