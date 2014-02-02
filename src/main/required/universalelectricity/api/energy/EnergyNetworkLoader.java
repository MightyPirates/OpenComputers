package universalelectricity.api.energy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;

/**
 * A dynamic network loader for injecting energy networks (NOT for other networks such as fuild
 * networks).
 * Example usage would be that ElectricityNetwork replaces EnergyNetwork.
 * 
 * @author Calclavia
 * 
 */
public class EnergyNetworkLoader
{
	/**
	 * The default IElectricityNetwork used for primary energy networks.
	 */
	public static Class<? extends IEnergyNetwork> NETWORK_CLASS;
	public static final Set<Class<? extends IEnergyNetwork>> NETWORK_CLASS_REGISTRY = new HashSet<Class<? extends IEnergyNetwork>>();

	static
	{
		setNetworkClass("universalelectricity.core.net.ElectricalNetwork");
	}

	public static void setNetworkClass(Class<? extends IEnergyNetwork> networkClass)
	{
		NETWORK_CLASS_REGISTRY.add(networkClass);
		NETWORK_CLASS = networkClass;
	}

	public static void setNetworkClass(String className)
	{
		try
		{
			setNetworkClass((Class<? extends IEnergyNetwork>) Class.forName(className));
		}
		catch (Exception e)
		{
			FMLLog.severe("Universal Electricity: Failed to set network class with name " + className);
			e.printStackTrace();
		}
	}

	public static IEnergyNetwork getNewNetwork(IConductor... conductors)
	{
		try
		{
			IEnergyNetwork network = NETWORK_CLASS.newInstance();
			network.getConnectors().addAll(Arrays.asList(conductors));
			return network;
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}

		return null;
	}

}