package thaumcraft.api.wands;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemFocusBasic extends Item  implements IWandFocus {
	
	public ItemFocusBasic ()
    {
        super();
        maxStackSize = 1;
        canRepair=false;
        this.setMaxDamage(0);
    }
	
	public IIcon icon;
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIconFromDamage(int par1) {
		return icon;
	}
	
	@Override
	public boolean isItemTool(ItemStack par1ItemStack)
    {
        return true;
    }	

	@Override
	public boolean isDamageable() {
		return true;
	}

	@Override
	public void addInformation(ItemStack stack,EntityPlayer player, List list, boolean par4) {
		AspectList al = this.getVisCost();
		if (al!=null && al.size()>0) {
			list.add(StatCollector.translateToLocal(isVisCostPerTick()?"item.Focus.cost2":"item.Focus.cost1"));
			for (Aspect aspect:al.getAspectsSorted()) {
				DecimalFormat myFormatter = new DecimalFormat("#####.##");
				String amount = myFormatter.format(al.getAmount(aspect)/100f);
				list.add(" \u00A7"+aspect.getChatcolor()+aspect.getName()+"\u00A7r x "+ amount);
				
			}
		}
	}
	
	@Override
	public int getItemEnchantability() {
		return 5;
	}

	@Override
	public EnumRarity getRarity(ItemStack itemstack)
    {
        return EnumRarity.rare;
    }
	
	
	@Override
	public int getFocusColor() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public AspectList getVisCost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack onFocusRightClick(ItemStack itemstack, World world,
			EntityPlayer player, MovingObjectPosition movingobjectposition) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onUsingFocusTick(ItemStack itemstack, EntityPlayer player,
			int count) {
		// TODO Auto-generated method stub		
	}
	
	@Override
	public void onPlayerStoppedUsingFocus(ItemStack itemstack, World world,
			EntityPlayer player, int count) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Just insert two alphanumeric characters before this string in your focus item class
	 */
	@Override
	public String getSortingHelper(ItemStack itemstack) {
		Map<Integer,Integer> ench = EnchantmentHelper.getEnchantments(itemstack);
		String out="";
		for (Integer lvl:ench.values()) {
			out = out + lvl + "";
		}
		return out;
	}

	@Override
	public boolean isVisCostPerTick() {
		return false;
	}

	@Override
	public IIcon getOrnament() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onFocusBlockStartBreak(ItemStack itemstack, int x, int y,
			int z, EntityPlayer player) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public WandFocusAnimation getAnimation() {
		return WandFocusAnimation.WAVE;
	}

	@Override
	public IIcon getFocusDepthLayerIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	/** 
	 * @see thaumcraft.api.wands.IWandFocus#acceptsEnchant(int)
	 * By default fortune is off for all wands
	 **/
	@Override
	public boolean acceptsEnchant(int id) {
		if (id==ThaumcraftApi.enchantFrugal||
			id==ThaumcraftApi.enchantPotency) return true;
		return false;
	}

	

	

}
