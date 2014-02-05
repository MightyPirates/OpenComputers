package ic2.api.reactor;

import net.minecraft.item.ItemStack;

/**
 * Design custom Reactor components by implementing this Interface
 * Items implementing the interface will not be ejected from Reactors in their clean-up
 * and can/will be interacted with by other elements, f.e. Uranium Cells.
 * 
 * All IC2 ReactorComponents implement and use this Interface
 *
 */
public interface IReactorComponent
{
	/**
	 * Called by reactor upon iterating through it's inventory (every cycle).
	 * Perform all necessary calculation/interaction here
	 * 
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @param x X-coordinate of the stack in the grid
	 * @param y Y-coordinate of the stack in the grid
	 * @param heatrun every Stack will cycle 2 time (true,false) first run for heat, sec for Eu calculation
	 */
	public void processChamber(IReactor reactor, ItemStack yourStack, int x, int y,boolean heatrun);

	/**
	 * Can be called by Uranium-Components who attempt to generate energy by pulsing to other components.
	 * Uranium-Uranium interaction (f.e.) uses this method.
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of called ItemStack
	 * @param pulsingStack Reference to the specific instance of pulsing ItemStack
	 * @param youX X-coordinate of your stack in the grid
	 * @param youY Y-coordinate of your stack in the grid
	 * @param pulseX X-coordinate of pulsing stack in the grid
	 * @param pulseY Y-coordinate of pulsing stack in the grid
	 * @param heatrun true for only create heat not EU, false for only EU  not heat
	 * @return true if this component reacts to the pulse (and pulse is therefore meant to produce heat)
	 */
	public boolean acceptUraniumPulse(IReactor reactor, ItemStack yourStack, ItemStack pulsingStack, int youX, int youY, int pulseX, int pulseY,boolean heatrun);

	/**
	 * Called by components to determine whether your component can be heated.
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @param x X-coordinate of the stack in the grid
	 * @param y Y-coordinate of the stack in the grid
	 * @return true if your component can take heat
	 */
	public boolean canStoreHeat(IReactor reactor, ItemStack yourStack, int x, int y);

	/**
	 * Called by heat-switches to determine how much heat to distribute into which direction.
	 * Please return the maximum capacity of your heat-containing component here.
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @param x X-coordinate of the stack in the grid
	 * @param y Y-coordinate of the stack in the grid
	 * @return Maximum heat
	 */
	public int getMaxHeat(IReactor reactor, ItemStack yourStack, int x, int y);

	/**
	 * Called by heat-switches to determine how much heat to distribute into which direction.
	 * Please return the current amount of heat stored in this component
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @param x X-coordinate of the stack in the grid
	 * @param y Y-coordinate of the stack in the grid
	 * @return Current Heat
	 */
	public int getCurrentHeat(IReactor reactor, ItemStack yourStack, int x, int y);

	/**
	 * Called by components to distribute heat to your component.
	 * Perform heating-calculations and increase your heat (dmg) level accordingly.
	 * This method will as well be called to REDUCE heat, by providing a negative amount.
	 * 
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @param x X-coordinate of the stack in the grid
	 * @param y Y-coordinate of the stack in the grid
	 * @param heat Amount of heat to be added (may be negative to subtract heat)
	 * @return 0 if the 'order' was accepted, return >0 to indicate the 'remaining' heat which couldn't be absorbed (and vice versa for <0)
	 */
	public int alterHeat(IReactor reactor, ItemStack yourStack, int x, int y, int heat);

	/**
	 * Called upon reactor explosion
	 * Alter the explosion size.
	 * Returning a float 0 < f < 1 will be counted as multiplier.
	 * Anything else will be counted as a flat addition (in case of <0 = reduction).
	 * 
	 * @param reactor Reference to the Reactor
	 * @param yourStack Reference to the specific instance of iterated ItemStack
	 * @return your explosion modifier
	 */
	public float influenceExplosion(IReactor reactor, ItemStack yourStack);
}
