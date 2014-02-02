package universalelectricity.api.electricity;

import net.minecraftforge.common.ForgeDirection;

/**
 * Applied to electrical machines that are designed to act as sources of power in an electrical
 * network. Mainly used to calculate the over all voltage of a network correctly.
 * 
 * @author DarkGuardsman
 */
public interface IVoltageOutput
{
	/**
	 * Can this machine emit voltage on the given side.
	 * 
	 * @param side - side that the voltage will be emitted on
	 * @return the voltage emitted
	 */
	public long getVoltageOutput(ForgeDirection side);
}
