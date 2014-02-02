package universalelectricity.api.net;

import java.util.Set;

/**
 * A network with connectors only.
 */
public interface INetwork<N extends INetwork, C extends IConnector>
{
	public void addConnector(C connector);

	public void removeConnector(C connector);

	/**
	 * Gets the set of conductors that make up this network.
	 * 
	 * @return conductor set
	 */
	public Set<C> getConnectors();

	/**
	 * Reconstructs the network and all objects within it.
	 */
	public void reconstruct();

	/**
	 * Creates a new network that makes up the current network and the network defined in the
	 * parameters. Be sure to refresh the new network inside this method.
	 * 
	 * @param network - network to merge
	 * @return The new network instance.
	 */
	public N merge(N network);

	/**
	 * Splits a network by removing a conductor referenced in the parameter. It will then create and
	 * refresh the new independent networks possibly created by this operation.
	 * 
	 * @param connection
	 */
	public void split(C connection);

	/**
	 * Splits the network between 2 connectors, separating their networks.
	 * 
	 * @param connectorA
	 * @param connectorB
	 */
	public void split(C connectorA, C connectorB);
}
