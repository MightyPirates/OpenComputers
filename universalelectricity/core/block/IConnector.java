package universalelectricity.core.block;

import net.minecraftforge.common.ForgeDirection;

/**
 * Applied to TileEntities that can connect to an electrical network.
 * 
 * @author Calclavia
 * 
 */
public interface IConnector
{

	/**
	 * @return If the connection is possible.
	 */
	public boolean canConnect(ForgeDirection direction);
}
