package universalelectricity.api.net;

import java.util.Set;

/**
 * A network of with connectors and individual nodes.
 * 
 * @author Calclavia
 * 
 * @param <N> - the class/interface Type value in which you implement this
 * @param <C> - the class/interface Type which makes up the network's connector set
 * @param <A> - the class/interface Type which makes up the network's node set
 */
public interface INodeNetwork<N extends INodeNetwork, C extends IConnector, A> extends INetwork<N, C>
{
	/**
	 * The nodes in a network are the objects that interact with the connectors.
	 * 
	 * @return The list of nodes in the network.
	 */
	public Set<A> getNodes();
}
