package universalelectricity.api;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;

/** A module to extend for compatibility with other energy systems. */
public abstract class CompatibilityModule
{
	public static final Set<CompatibilityModule> loadedModules = new LinkedHashSet<CompatibilityModule>();

	/** A cache to know which module to use with when facing objects with a specific class. */
	public static final HashMap<Class, CompatibilityModule> energyHandlerCache = new HashMap<Class, CompatibilityModule>();
	public static final HashMap<Class, CompatibilityModule> energyStorageCache = new HashMap<Class, CompatibilityModule>();

	public static void register(CompatibilityModule module)
	{
		loadedModules.add(module);
	}

	/** Can the handler connect to this specific direction? */
	public static boolean canConnect(Object handler, ForgeDirection direction, Object source)
	{
		if (isHandler(handler))
		{
			return energyHandlerCache.get(handler.getClass()).doCanConnect(handler, direction, source);
		}

		return false;
	}

	/**
	 * Make the handler receive energy.
	 * 
	 * @return The actual energy that was used.
	 */
	public static long receiveEnergy(Object handler, ForgeDirection direction, long energy, boolean doReceive)
	{
		if (isHandler(handler))
		{
			return energyHandlerCache.get(handler.getClass()).doReceiveEnergy(handler, direction, energy, doReceive);
		}

		return 0;
	}

	/**
	 * Make the handler extract energy.
	 * 
	 * @return The actual energy that was extract.
	 */
	public static long extractEnergy(Object handler, ForgeDirection direction, long energy, boolean doReceive)
	{
		if (isHandler(handler))
		{
			return energyHandlerCache.get(handler.getClass()).doExtractEnergy(handler, direction, energy, doReceive);
		}

		return 0;
	}

	/**
	 * Gets the energy stored in the handler.
	 */
	public static long getEnergy(Object handler, ForgeDirection direction)
	{
		if (isEnergyContainer(handler))
		{
			return energyStorageCache.get(handler.getClass()).doGetEnergy(handler, direction);
		}

		return 0;
	}

	/**
	 * Charges an item
	 * 
	 * @return The actual energy that was accepted.
	 */
	public static long chargeItem(ItemStack itemStack, long energy, boolean doCharge)
	{
		if (itemStack != null && isHandler(itemStack.getItem()))
		{
			return energyHandlerCache.get(itemStack.getItem().getClass()).doChargeItem(itemStack, energy, doCharge);
		}

		return 0;
	}

	/**
	 * Discharges an item
	 * 
	 * @return The actual energy that was removed.
	 */
	public static long dischargeItem(ItemStack itemStack, long energy, boolean doCharge)
	{
		if (itemStack != null && isHandler(itemStack.getItem()))
		{
			return energyHandlerCache.get(itemStack.getItem().getClass()).doDischargeItem(itemStack, energy, doCharge);
		}

		return 0;
	}

	/**
	 * Gets the itemStack with a specific charge.
	 * 
	 * @return ItemStack of electrical/energy item.
	 */
	public static ItemStack getItemWithCharge(ItemStack itemStack, long energy)
	{
		if (itemStack != null && isHandler(itemStack.getItem()))
		{
			return energyHandlerCache.get(itemStack.getItem().getClass()).doGetItemWithCharge(itemStack, energy);
		}

		return null;
	}

	/**
	 * Is this object a valid energy handler?
	 * 
	 * @param True if the handler can store energy. This can be for items and blocks.
	 */
	public static boolean isHandler(Object handler)
	{
		if (handler != null)
		{
			Class clazz = handler.getClass();

			if (energyHandlerCache.containsKey(clazz))
			{
				return true;
			}

			for (CompatibilityModule module : CompatibilityModule.loadedModules)
			{
				if (module.doIsHandler(handler))
				{
					energyHandlerCache.put(clazz, module);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Is this object able to store energy?
	 * 
	 * @param handler
	 * @return True if the handler can store energy. The handler MUST be a block.
	 */
	public static boolean isEnergyContainer(Object handler)
	{
		if (handler != null)
		{
			Class clazz = handler.getClass();

			if (energyStorageCache.containsKey(clazz))
			{
				return true;
			}

			for (CompatibilityModule module : CompatibilityModule.loadedModules)
			{
				if (module.doIsEnergyContainer(handler))
				{
					energyStorageCache.put(clazz, module);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Blocks only
	 */
	public static long getMaxEnergy(Object handler, ForgeDirection direction)
	{
		if (isEnergyContainer(handler))
		{
			return energyStorageCache.get(handler.getClass()).doGetMaxEnergy(handler, direction);
		}

		return 0;
	}

	public static long getEnergyItem(ItemStack itemStack)
	{
		if (itemStack != null && isHandler(itemStack.getItem()))
		{
			return energyHandlerCache.get(itemStack.getItem().getClass()).doGetEnergyItem(itemStack);
		}

		return 0;
	}

	public static long getMaxEnergyItem(ItemStack itemStack)
	{
		if (itemStack != null && isHandler(itemStack.getItem()))
		{
			return energyHandlerCache.get(itemStack.getItem().getClass()).doGetMaxEnergyItem(itemStack);
		}

		return 0;
	}

	public abstract long doReceiveEnergy(Object handler, ForgeDirection direction, long energy, boolean doReceive);

	public abstract long doExtractEnergy(Object handler, ForgeDirection direction, long energy, boolean doExtract);

	/**
	 * Charges an item with the given energy
	 * 
	 * @param itemStack - item stack that is the item
	 * @param joules - input energy
	 * @param docharge - do the action
	 * @return amount of energy accepted
	 */
	public abstract long doChargeItem(ItemStack itemStack, long joules, boolean docharge);

	/**
	 * discharges an item with the given energy
	 * 
	 * @param itemStack - item stack that is the item
	 * @param joules - input energy
	 * @param docharge - do the action
	 * @return amount of energy that was removed
	 */
	public abstract long doDischargeItem(ItemStack itemStack, long joules, boolean doDischarge);

	public abstract boolean doIsHandler(Object obj);

	public abstract boolean doIsEnergyContainer(Object obj);

	public abstract long doGetEnergy(Object obj, ForgeDirection direction);

	public abstract boolean doCanConnect(Object obj, ForgeDirection direction, Object source);

	public abstract ItemStack doGetItemWithCharge(ItemStack itemStack, long energy);

	public abstract long doGetMaxEnergy(Object handler, ForgeDirection direction);

	public abstract long doGetEnergyItem(ItemStack is);

	public abstract long doGetMaxEnergyItem(ItemStack is);
}
