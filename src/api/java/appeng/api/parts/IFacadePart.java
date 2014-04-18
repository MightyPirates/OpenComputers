package appeng.api.parts;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Used Internally.
 * 
 * not intended for implementation.
 */
public interface IFacadePart
{

	/**
	 * used to save the part.
	 */
	ItemStack getItemStack();

	/**
	 * used to collide, and pick the part
	 * 
	 * @param ch
	 */
	void getBoxes(IPartCollsionHelper ch);

	/**
	 * render the part.
	 * 
	 * @param xCoord
	 * @param yCoord
	 * @param zCoord
	 * @param instance
	 * @param renderer
	 * @param busBounds
	 * @param b
	 */
	@SideOnly(Side.CLIENT)
	void renderStatic(int x, int y, int z, IPartRenderHelper instance, RenderBlocks renderer, IFacadeContainer fc, AxisAlignedBB busBounds, boolean renderStilt);

	/**
	 * render the part in inventory.
	 * 
	 * @param instance
	 * @param renderer
	 */
	@SideOnly(Side.CLIENT)
	void renderInventory(IPartRenderHelper instance, RenderBlocks renderer);

	/**
	 * @return side the facade is in
	 */
	ForgeDirection getSide();

	/**
	 * @return the box for the face of the facade
	 */
	AxisAlignedBB getPrimaryBox();

	Item getItem();

	int getItemDamage();

	boolean isBC();

	void setThinFacades(boolean useThinFacades);

}