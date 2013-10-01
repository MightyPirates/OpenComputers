package universalelectricity.core.block;

import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.electricity.ElectricityPack;

/**
 * Applied to all TileEntities that can interact with electricity.
 * 
 * @author Calclavia, King_Lemming
 * 
 */
public interface IElectrical extends IConnector
{
	/**
	 * Adds electricity to an block. Returns the quantity of electricity that was accepted. This
	 * should always return 0 if the block cannot be externally charged.
	 * 
	 * @param from Orientation the electricity is sent in from.
	 * @param receive Maximum amount of electricity to be sent into the block.
	 * @param doReceive If false, the charge will only be simulated.
	 * @return Amount of energy that was accepted by the block.
	 */
	public float receiveElectricity(ForgeDirection from, ElectricityPack receive, boolean doReceive);

	/**
	 * Adds electricity to an block. Returns the ElectricityPack, the electricity provided. This
	 * should always return null if the block cannot be externally discharged.
	 * 
	 * @param from Orientation the electricity is requested from.
	 * @param energy Maximum amount of energy to be sent into the block.
	 * @param doReceive If false, the charge will only be simulated.
	 * @return Amount of energy that was given out by the block.
	 */
	public ElectricityPack provideElectricity(ForgeDirection from, ElectricityPack request, boolean doProvide);

	/**
	 * @return How much energy does this TileEntity want?
	 */
	public float getRequest(ForgeDirection direction);

	/**
	 * @return How much energy does this TileEntity want to provide?
	 */
	public float getProvide(ForgeDirection direction);

	/**
	 * Gets the voltage of this TileEntity.
	 * 
	 * @return The amount of volts. E.g 120v or 240v
	 */
	public float getVoltage();

}
