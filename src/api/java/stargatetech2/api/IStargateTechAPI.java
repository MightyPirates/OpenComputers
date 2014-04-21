package stargatetech2.api;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fluids.Fluid;
import stargatetech2.api.stargate.IStargateNetwork;

public interface IStargateTechAPI {
	/**
	 * @return The Fluid instance corresponding to Ionized Particles.
	 */
	public Fluid getIonizedParticlesFluidInstance();
	
	/**
	 * @return The creative inventory tab used by StargateTech 2.
	 */
	public CreativeTabs getStargateTab();
	
	/**
	 * @return The IStargateNetwork singleton instance.
	 */
	public IStargateNetwork getStargateNetwork();
	
	/**
	 * @return The current IFactory instance.
	 */
	public IFactory getFactory();
	
	/**
	 * @return The current IStackManager instance.
	 */
	public IStackManager getStackManager();
}