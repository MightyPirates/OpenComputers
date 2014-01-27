package universalelectricity.api.net;

import net.minecraftforge.common.ForgeDirection;

/**
 * Applied to TileEntities that has an instance of an electricity network.
 * 
 * @author Calclavia
 * 
 */
public interface IConnector<N> extends INetworkProvider<N>, IConnectable
{
	/**
	 * Gets an array of all the connected IConnectors that this conductor is connected to. This
	 * should correspond to the ForgeDirection index.
	 * 
	 * @return An array of length "6".
	 */
	public Object[] getConnections();

	/**
	 * Gets this connector instance. Used specially for MultiPart connections.
	 * 
	 * @return The instance, in most cases, just return "this".
	 */
	public IConnector<N> getInstance(ForgeDirection dir);
}
