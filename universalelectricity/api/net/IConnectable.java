package universalelectricity.api.net;

import net.minecraftforge.common.ForgeDirection;

/**
 * @author Calclavia
 * 
 */
public interface IConnectable
{
	/**
	 * Can this TileEntity connect with another?
	 * 
	 * @return Return true, if the connection is possible.
	 */
	public boolean canConnect(ForgeDirection direction);
}
