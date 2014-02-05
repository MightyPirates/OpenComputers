package forestry.api.mail;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

/**
 *  Postal Carriers are systems which can be hooked into Forestry's mail system to handle mail delivery.
 *  
 *  The two available carriers in vanilla Forestry are
 *       "player" - Delivers mail to individual players.
 *       "trader" - Handles mail addressed to trade stations.
 */
public interface IPostalCarrier {

	/**
	 * @return A lower-case identifier without spaces.
	 */
	String getUID();
	
	/**
	 * @return A human-readable name for this carrier.
	 */
	String getName();
	
	@SideOnly(Side.CLIENT)
	Icon getIcon();

	/**
	 * Handle delivery of a letter addressed to this carrier. 
	 * @param world The world the {@link IPostOffice} handles.
	 * @param office {link @IPostOffice} which received this letter and handed it to the carrier. 
	 * @param recipient An identifier for the recipient as typed by the player into the address field.
	 * @param letterstack ItemStack representing the letter. See {@link IPostRegistry} for helper functions to validate and extract it.
	 * @param doDeliver Whether or not the letter is supposed to actually be delivered or if delivery is only to be simulated.
	 * @return {link IPostalState} holding information on success or failure for delivery.
	 */
	IPostalState deliverLetter(World world, IPostOffice office, String recipient, ItemStack letterstack, boolean doDeliver);

}
