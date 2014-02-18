package universalelectricity.api.net;

import net.minecraftforge.common.ForgeDirection;

/**
 * @author Calclavia
 * 
 */
public interface IConnectable
{
	/**
	 * Can this object connect with another?
	 * 
	 * @param from - The direction the connection is coming from.
	 * @param source - The source calling canConnect onto this object.
	 * @return Return true, if the connection is possible.
	 */
	public boolean canConnect(ForgeDirection from, Object source);
}
