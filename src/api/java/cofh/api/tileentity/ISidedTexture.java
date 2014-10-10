package cofh.api.tileentity;

import net.minecraft.util.IIcon;

/**
 * Implement this interface on Tile Entities which can change their block's texture based on the current render pass. The block must defer the call to its Tile
 * Entity.
 * 
 * @author Zeldo Kavira
 * 
 */
public interface ISidedTexture {

	/**
	 * Returns the icon to use for a given side and render pass.
	 * 
	 * @param side
	 *            Block side to get the texture for.
	 * @param pass
	 *            Render pass.
	 * @return The icon to use.
	 */
	IIcon getTexture(int side, int pass);

}
