package cofh.api.tileentity;

import net.minecraft.util.Icon;

/**
 * Implement this interface on Tile Entities which can change their block's texture based on the current render pass. The block must defer the call to its Tile
 * Entity.
 * 
 * @author Zeldo Kavira
 * 
 */
public interface ISidedBlockTexture {

	public Icon getBlockTexture(int side, int pass);

}
