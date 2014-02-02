package universalelectricity.api.energy;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Can be used internally for IEnergyInterface blocks. This is optional and should be used for
 * ease of use purposes.
 * 
 * @author Calclavia, Based on Thermal Expansion
 * 
 */
public class EnergyStorageHandler
{
	protected long energy;
	protected long capacity;
	protected long maxReceive;
	protected long maxExtract;

	/**
	 * A cache of the last energy stored through extract and receive.
	 */
	protected long lastEnergy;

	public EnergyStorageHandler()
	{
		this(0);
	}

	public EnergyStorageHandler(long capacity)
	{
		this(capacity, capacity, capacity);
	}

	public EnergyStorageHandler(long capacity, long maxTransfer)
	{
		this(capacity, maxTransfer, maxTransfer);
	}

	public EnergyStorageHandler(long capacity, long maxReceive, long maxExtract)
	{
		this.capacity = capacity;
		this.maxReceive = maxReceive;
		this.maxExtract = maxExtract;
	}

	public EnergyStorageHandler readFromNBT(NBTTagCompound nbt)
	{
		this.energy = nbt.getLong("energy");
		return this;
	}

	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setLong("energy", energy);
		return nbt;
	}

	public void setCapacity(long capacity)
	{
		this.capacity = capacity;

		if (energy > capacity)
		{
			energy = capacity;
		}
	}

	public void setMaxTransfer(long maxTransfer)
	{
		setMaxReceive(maxTransfer);
		setMaxExtract(maxTransfer);
	}

	public void setMaxReceive(long maxReceive)
	{
		this.maxReceive = maxReceive;
	}

	public void setMaxExtract(long maxExtract)
	{
		this.maxExtract = maxExtract;
	}

	public long getMaxReceive()
	{
		return maxReceive;
	}

	public long getMaxExtract()
	{
		return maxExtract;
	}

	/**
	 * This function is included to allow for server -> client sync. Do not call this externally to
	 * the containing Tile Entity, as not all IEnergyHandlers are
	 * guaranteed to have it.
	 * 
	 * @param energy
	 */
	public void setEnergy(long energy)
	{
		this.energy = energy;

		if (this.energy > capacity)
		{
			this.energy = capacity;
		}
		else if (this.energy < 0)
		{
			this.energy = 0;
		}
	}

	/**
	 * This function is included to allow the containing tile to directly and efficiently modify the
	 * energy contained in the EnergyStorage. Do not rely on this
	 * externally, as not all IEnergyHandlers are guaranteed to have it.
	 * 
	 * @param energy
	 */
	public void modifyEnergyStored(long energy)
	{
		this.energy += energy;

		if (this.energy > this.capacity)
		{
			this.energy = this.capacity;
		}
		else if (this.energy < 0)
		{
			this.energy = 0;
		}
	}

	public long receiveEnergy(long receive, boolean doReceive)
	{
		long energyReceived = Math.min(this.capacity - this.energy, Math.min(this.maxReceive, receive));

		if (doReceive)
		{
			this.lastEnergy = this.energy;
			this.energy += energyReceived;
		}
		return energyReceived;
	}

	public long receiveEnergy(boolean doReceive)
	{
		return this.receiveEnergy(this.getMaxReceive(), doReceive);
	}

	public long receiveEnergy()
	{
		return this.receiveEnergy(true);
	}

	public long extractEnergy(long extract, boolean doExtract)
	{
		long energyExtracted = Math.min(this.energy, Math.min(this.maxExtract, extract));

		if (doExtract)
		{
			this.lastEnergy = this.energy;
			this.energy -= energyExtracted;
		}
		return energyExtracted;
	}

	public long extractEnergy(boolean doExtract)
	{
		return this.extractEnergy(this.getMaxExtract(), doExtract);
	}

	public long extractEnergy()
	{
		return this.extractEnergy(true);
	}

	public boolean checkReceive(long receive)
	{
		return this.receiveEnergy(receive, false) >= receive;
	}

	public boolean checkReceive()
	{
		return this.checkReceive(this.getMaxReceive());
	}

	public boolean checkExtract(long extract)
	{
		return this.extractEnergy(extract, false) >= extract;
	}

	public boolean checkExtract()
	{
		return this.checkExtract(this.getMaxExtract());
	}

	public boolean isFull()
	{
		return this.getEnergy() >= this.getEnergyCapacity();
	}

	public boolean isEmpty()
	{
		return this.getEnergy() == 0;
	}

	public long getLastEnergy()
	{
		return this.lastEnergy;
	}

	/**
	 * @return True if the last energy state and the current one are either in an
	 * "empty or not empty" change state.
	 */
	public boolean didEnergyStateChange()
	{
		return (this.lastEnergy == 0 && this.energy > 0) || (this.lastEnergy > 0 && this.energy == 0);
	}

	/**
	 * Returns the amount of energy this storage can further store.
	 */
	public long getEmptySpace()
	{
		return this.getEnergyCapacity() - this.getEnergy();
	}

	public long getEnergy()
	{
		return this.energy;
	}

	public long getEnergyCapacity()
	{
		return this.capacity;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "[" + this.getEnergy() + "/" + this.getEnergyCapacity() + "]";
	}
}
