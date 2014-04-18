package appeng.api.parts;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Used Internally.
 * 
 * not intended for implementation.
 */
public interface IFacadeContainer
{

	/**
	 * Attempts to add the {@link IFacadePart} to the given side.
	 * 
	 * @return true if the facade as successfully added.
	 */
	boolean addFacade(IFacadePart a);

	/**
	 * Removed the facade on the given side, or does nothing.
	 */
	void removeFacade(IPartHost host, ForgeDirection side);

	/**
	 * @return the {@link IFacadePart} for a given side, or null.
	 */
	IFacadePart getFacade(ForgeDirection s);

}
