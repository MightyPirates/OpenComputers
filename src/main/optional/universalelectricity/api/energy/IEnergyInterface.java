package universalelectricity.api.energy;

import net.minecraftforge.common.util.ForgeDirection;
import universalelectricity.api.net.IConnectable;

/**
 * Applied to all TileEntities that can interact with energy.
 * 
 * @author Calclavia, Inspired by Thermal Expansion
 */
public interface IEnergyInterface extends IConnectable
{
	/**
	 * Adds energy to a block. Returns the quantity of energy that was accepted. This should always
	 * return 0 if the block cannot be externally charged.
	 * 
	 * @param from Orientation the energy is sent in from.
	 * @param receive Maximum amount of energy (joules) to be sent into the block.
	 * @param doReceive If false, the charge will only be simulated.
	 * @return Amount of energy that was accepted by the block.
	 */
	public long onReceiveEnergy(ForgeDirection from, long receive, boolean doReceive);

	/**
	 * Removes energy from a block. Returns the quantity of energy that was extracted. This should
	 * always return 0 if the block cannot be externally discharged.
	 * 
	 * @param from Orientation the energy is requested from. This direction MAY be passed as
	 * "Unknown" if it is wrapped from another energy system that has no clear way to find
	 * direction. (e.g BuildCraft 4)
	 * @param energy Maximum amount of energy to be sent into the block.
	 * @param doExtract If false, the charge will only be simulated.
	 * @return Amount of energy that was given out by the block.
	 */
	public long onExtractEnergy(ForgeDirection from, long extract, boolean doExtract);

}
