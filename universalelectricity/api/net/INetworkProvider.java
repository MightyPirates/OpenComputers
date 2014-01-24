package universalelectricity.api.net;

import net.minecraftforge.common.ForgeDirection;

/**
 * Applied to TileEntities that has an instance of an electricity network.
 * 
 * @author Calclavia
 * 
 */
public interface INetworkProvider<N>
{
	public N getNetwork();

	public void setNetwork(N network);
}
