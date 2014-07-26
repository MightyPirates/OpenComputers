package cofh.api.transport;

/**
 * This interface is implemented on Ender Attuned objects which can receive Energy (Redstone Flux).
 * 
 * @author King Lemming
 * 
 */
public interface IEnderEnergyHandler extends IEnderAttuned {

	/**
	 * Return whether or not the Ender Attuned object can currently send energy (Redstone Flux).
	 */
	boolean canSendEnergy();

	/**
	 * This should be checked to see if the Ender Attuned object can currently receive energy (Redstone Flux).
	 * 
	 * Note: In practice, this can (and should) be used to ensure that something does not send to itself.
	 */
	boolean canReceiveEnergy();

	/**
	 * This tells the Ender Attuned object to receive energy. This returns the amount remaining, *not* the amount received - a return of 0 means that all energy
	 * was received!
	 * 
	 * @param energy
	 *            Amount of energy to be received.
	 * @param simulate
	 *            If TRUE, the result will only be simulated.
	 * @return Amount of energy that is remaining (or would be remaining, if simulated).
	 */
	int receiveEnergy(int energy, boolean simulate);

}
