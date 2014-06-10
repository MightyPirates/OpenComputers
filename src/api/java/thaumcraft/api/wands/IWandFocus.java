package thaumcraft.api.wands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import thaumcraft.api.aspects.AspectList;


public interface IWandFocus  {
	
	public enum WandFocusAnimation {
		WAVE, CHARGE;
	}

	/**
	 * @return The color the focus should be changed to.
	 */
	public int getFocusColor();
		
	/**
	 * @return An icon that will be drawn as a block inside the focus "block".
	 */
	IIcon getFocusDepthLayerIcon();
	
	public IIcon getOrnament();
	
	public WandFocusAnimation getAnimation();
	
	/**
	 * Gets the amount of vis used per aspect per click or tick. This cost is actually listed as
	 * a hundredth of a single point of vis, so a cost of 100 will equal one vis per tick/click.
	 * It is returned as an AspectList to allow for multiple vis types in different ratios.
	 */
	public AspectList getVisCost();
	
	public boolean isVisCostPerTick();

	public ItemStack onFocusRightClick(ItemStack itemstack, World world, EntityPlayer player, MovingObjectPosition movingobjectposition);
	
	public void onUsingFocusTick(ItemStack itemstack, EntityPlayer player, int count);
	
	public void onPlayerStoppedUsingFocus(ItemStack itemstack, World world, EntityPlayer player, int count);
		
	/**
	 * Helper method to determine in what order foci should be iterated through when 
	 * the user presses the 'change focus' keybinding.
	 * @return a string of characters that foci will be sorted against. 
	 * For example AA00 will be placed before FG12
	 * <br>As a guide build the sort string from two alphanumeric characters followed by 
	 * two numeric characters based on... whatever. 
	 */
	public String getSortingHelper(ItemStack itemstack);

	boolean onFocusBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player);

	public boolean acceptsEnchant(int id);

	

	

}
