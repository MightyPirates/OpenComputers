package mods.railcraft.api.carts;

/**
 * This interface is implemented by the Energy Cart
 * and is used by the Energy Loaders to charge/discharge carts.
 * It is roughly equivalent to the IItemTransfer interface
 * and based on ElectricItem and IElectricItem.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see IItemTransfer
 */
public interface IEnergyTransfer
{

    /**
     * Injects the specified amount of EU into the device.
     *
     * The function returns the remainder of the EU after
     * any EU used is subtracted.
     *
     * @param source Object initiating the transfer, should be an Entity or Tile Entity
     * @param amount amount of energy to transfer in EU
     * @param tier tier of the source device, has to be at least as high as the target device
     * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
     * @param simulate don't actually change the item, just determine the return value
     * @return The amount of EU not used
     */
    public int injectEnergy(Object source, int amount, int tier, boolean ignoreTransferLimit, boolean simulate, boolean passAlong);

    /**
     * Requests a certain amount of EU from the device.
     *
     * The is function will subtract EU from the device's store of power
     * and return a portion up to, but not exceeding, the amount of EU requested.
     *
     * @param source Object initiating the transfer, should be an Entity or Tile Entity
     * @param amount amount of energy to transfer in EU
     * @param tier tier of the source device, has to be at least as high as the target device
     * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
     * @param simulate don't actually change the item, just determine the return value
     * @param passAlong whether neighboring carts should be asked to provide any missing power.
     * @return The amount of EU transferred
     */
    public int extractEnergy(Object source, int amount, int tier, boolean ignoreTransferLimit, boolean simulate, boolean passAlong);

    /**
     * Return true if energy can be injected into this device.
     *
     * @return true if can inject energy
     */
    public boolean canInjectEnergy();

    /**
     * Return true if energy can be extracted from this device.
     *
     * @return true if can extract energy
     */
    public boolean canExtractEnergy();

    /**
     * The max capacity of the device.
     *
     * @return max capacity
     */
    public int getCapacity();

    /**
     * Returns the current energy contained in the device.
     *
     * @return current energy
     */
    public int getEnergy();

    public int getTier();

    /**
     * The device's transfer rate in EU/t.
     *
     * @return the transfer rate
     */
    public int getTransferLimit();
}
