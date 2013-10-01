/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.power;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.api.core.SafeTimeTracker;

public final class PowerHandler
{

	public static enum Type
	{

		ENGINE, GATE, MACHINE, PIPE, STORAGE;

		public boolean canReceiveFromPipes()
		{
			switch (this)
			{
				case MACHINE:
				case STORAGE:
					return true;
				default:
					return false;
			}
		}

		public boolean eatsEngineExcess()
		{
			switch (this)
			{
				case MACHINE:
				case STORAGE:
					return true;
				default:
					return false;
			}
		}
	}

	public static class PerditionCalculator
	{

		public static final float DEFAULT_POWERLOSS = 1F;
		public static final float MIN_POWERLOSS = 0.01F;
		private final float powerLoss;

		public PerditionCalculator()
		{
			powerLoss = DEFAULT_POWERLOSS;
		}

		public PerditionCalculator(float powerLoss)
		{
			if (powerLoss < MIN_POWERLOSS)
			{
				powerLoss = MIN_POWERLOSS;
			}
			this.powerLoss = powerLoss;
		}

		/**
		 * Apply the perdition algorithm to the current stored energy. This function can only be
		 * called once per tick, but it might not be called every tick. It is triggered by any
		 * manipulation of the stored energy.
		 * 
		 * @param powerHandler the PowerHandler requesting the perdition update
		 * @param current the current stored energy
		 * @param ticksPassed ticks since the last time this function was called
		 * @return
		 */
		public float applyPerdition(PowerHandler powerHandler, float current, long ticksPassed)
		{
			current -= powerLoss * ticksPassed;
			if (current < 0)
			{
				current = 0;
			}
			return current;
		}
	}

	public static final PerditionCalculator DEFAULT_PERDITION = new PerditionCalculator();
	private float minEnergyReceived;
	private float maxEnergyReceived;
	private float maxEnergyStored;
	private float activationEnergy;
	private float energyStored = 0;
	private final SafeTimeTracker doWorkTracker = new SafeTimeTracker();
	private final SafeTimeTracker sourcesTracker = new SafeTimeTracker();
	private final SafeTimeTracker perditionTracker = new SafeTimeTracker();
	public final int[] powerSources = new int[6];
	public final IPowerReceptor receptor;
	private PerditionCalculator perdition;
	private final PowerReceiver receiver;
	private final Type type;

	public PowerHandler(IPowerReceptor receptor, Type type)
	{
		this.receptor = receptor;
		this.type = type;
		this.receiver = new PowerReceiver();
		this.perdition = DEFAULT_PERDITION;
	}

	public PowerReceiver getPowerReceiver()
	{
		return receiver;
	}

	public float getMinEnergyReceived()
	{
		return minEnergyReceived;
	}

	public float getMaxEnergyReceived()
	{
		return maxEnergyReceived;
	}

	public float getMaxEnergyStored()
	{
		return maxEnergyStored;
	}

	public float getActivationEnergy()
	{
		return activationEnergy;
	}

	public float getEnergyStored()
	{
		return energyStored;
	}

	/**
	 * Setup your PowerHandler's settings.
	 * 
	 * @param minEnergyReceived This is the minimum about of power that will be accepted by the
	 * PowerHandler. This should generally be greater than the activationEnergy if you plan to use
	 * the doWork() callback. Anything greater than 1 will prevent Redstone Engines from powering
	 * this Provider.
	 * @param maxEnergyReceived The maximum amount of power accepted by the PowerHandler. This
	 * should generally be less than 500. Too low and larger engines will overheat while trying to
	 * power the machine. Too high, and the engines will never warm up. Greater values also place
	 * greater strain on the power net.
	 * @param activationEnergy If the stored energy is greater than this value, the doWork()
	 * callback is called (once per tick).
	 * @param maxStoredEnergy The maximum amount of power this PowerHandler can store. Values tend
	 * to range between 100 and 5000. With 1000 and 1500 being common.
	 */
	public void configure(float minEnergyReceived, float maxEnergyReceived, float activationEnergy, float maxStoredEnergy)
	{
		if (minEnergyReceived > maxEnergyReceived)
		{
			maxEnergyReceived = minEnergyReceived;
		}
		this.minEnergyReceived = minEnergyReceived;
		this.maxEnergyReceived = maxEnergyReceived;
		this.maxEnergyStored = maxStoredEnergy;
		this.activationEnergy = activationEnergy;
	}

	public void configurePowerPerdition(int powerLoss, int powerLossRegularity)
	{
		if (powerLoss == 0 || powerLossRegularity == 0)
		{
			perdition = new PerditionCalculator(0);
			return;
		}
		perdition = new PerditionCalculator((float) powerLoss / (float) powerLossRegularity);
	}

	/**
	 * Allows you to define a new PerditionCalculator class to handler perdition calculations.
	 * 
	 * For example if you want exponentially increasing loss based on amount stored.
	 * 
	 * @param perdition
	 */
	public void setPerdition(PerditionCalculator perdition)
	{
		if (perdition == null)
			perdition = DEFAULT_PERDITION;
		this.perdition = perdition;
	}

	public PerditionCalculator getPerdition()
	{
		if (perdition == null)
			return DEFAULT_PERDITION;
		return perdition;
	}

	/**
	 * Ticks the power handler. You should call this if you can, but its not required.
	 * 
	 * If you don't call it, the possibility exists for some weirdness with the perdition algorithm
	 * and work callback as its possible they will not be called on every tick they otherwise would
	 * be. You should be able to design around this though if you are aware of the limitations.
	 */
	public void update()
	{
		applyPerdition();
		applyWork();
		validateEnergy();
	}

	private void applyPerdition()
	{
		if (perditionTracker.markTimeIfDelay(receptor.getWorld(), 1) && energyStored > 0)
		{
			float newEnergy = getPerdition().applyPerdition(this, energyStored, perditionTracker.durationOfLastDelay());
			if (newEnergy == 0 || newEnergy < energyStored)
				energyStored = newEnergy;
			else
				energyStored = DEFAULT_PERDITION.applyPerdition(this, energyStored, perditionTracker.durationOfLastDelay());
			validateEnergy();
		}
	}

	private void applyWork()
	{
		if (energyStored >= activationEnergy)
		{
			if (doWorkTracker.markTimeIfDelay(receptor.getWorld(), 1))
			{
				receptor.doWork(this);
			}
		}
	}

	private void updateSources(ForgeDirection source)
	{
		if (sourcesTracker.markTimeIfDelay(receptor.getWorld(), 1))
		{
			for (int i = 0; i < 6; ++i)
			{
				powerSources[i] -= sourcesTracker.durationOfLastDelay();
				if (powerSources[i] < 0)
				{
					powerSources[i] = 0;
				}
			}
		}

		if (source != null)
			powerSources[source.ordinal()] = 10;
	}

	/**
	 * Extract energy from the PowerHandler. You must call this even if doWork() triggers.
	 * 
	 * @param min
	 * @param max
	 * @param doUse
	 * @return amount used
	 */
	public float useEnergy(float min, float max, boolean doUse)
	{
		applyPerdition();

		float result = 0;

		if (energyStored >= min)
		{
			if (energyStored <= max)
			{
				result = energyStored;
				if (doUse)
				{
					energyStored = 0;
				}
			}
			else
			{
				result = max;
				if (doUse)
				{
					energyStored -= max;
				}
			}
		}

		validateEnergy();

		return result;
	}

	public void readFromNBT(NBTTagCompound data)
	{
		readFromNBT(data, "powerProvider");
	}

	public void readFromNBT(NBTTagCompound data, String tag)
	{
		NBTTagCompound nbt = data.getCompoundTag(tag);
		energyStored = nbt.getFloat("storedEnergy");
	}

	public void writeToNBT(NBTTagCompound data)
	{
		writeToNBT(data, "powerProvider");
	}

	public void writeToNBT(NBTTagCompound data, String tag)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setFloat("storedEnergy", energyStored);
		data.setCompoundTag(tag, nbt);
	}

	public final class PowerReceiver
	{

		private PowerReceiver()
		{
		}

		public float getMinEnergyReceived()
		{
			return minEnergyReceived;
		}

		public float getMaxEnergyReceived()
		{
			return maxEnergyReceived;
		}

		public float getMaxEnergyStored()
		{
			return maxEnergyStored;
		}

		public float getActivationEnergy()
		{
			return activationEnergy;
		}

		public float getEnergyStored()
		{
			return energyStored;
		}

		public Type getType()
		{
			return type;
		}

		public void update()
		{
			PowerHandler.this.update();
		}

		/**
		 * The amount of power that this PowerHandler currently needs.
		 * 
		 * @return
		 */
		public float powerRequest()
		{
			update();
			return Math.min(maxEnergyReceived, maxEnergyStored - energyStored);
		}

		/**
		 * Add power to the PowerReceiver from an external source.
		 * 
		 * @param quantity
		 * @param from
		 * @return the amount of power used
		 */
		public float receiveEnergy(Type source, final float quantity, ForgeDirection from)
		{
			float used = quantity;
			if (source == Type.ENGINE)
			{
				if (used < minEnergyReceived)
				{
					return 0;
				}
				else if (used > maxEnergyReceived)
				{
					used = maxEnergyReceived;
				}
			}

			updateSources(from);

			used = addEnergy(used);

			applyWork();

			if (source == Type.ENGINE && type.eatsEngineExcess())
			{
				return Math.min(quantity, maxEnergyReceived);
			}

			return used;
		}
	}

	/**
	 * 
	 * @return the amount the power changed by
	 */
	public float addEnergy(float quantity)
	{
		energyStored += quantity;

		if (energyStored > maxEnergyStored)
		{
			quantity -= energyStored - maxEnergyStored;
			energyStored = maxEnergyStored;
		}
		else if (energyStored < 0)
		{
			quantity -= energyStored;
			energyStored = 0;
		}

		applyPerdition();

		return quantity;
	}

	public void setEnergy(float quantity)
	{
		this.energyStored = quantity;
		validateEnergy();
	}

	public boolean isPowerSource(ForgeDirection from)
	{
		return powerSources[from.ordinal()] != 0;
	}

	private void validateEnergy()
	{
		if (energyStored < 0)
		{
			energyStored = 0;
		}
		if (energyStored > maxEnergyStored)
		{
			energyStored = maxEnergyStored;
		}
	}
}
