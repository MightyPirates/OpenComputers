package thaumcraft.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemRunic extends Item implements IRunicArmor  {
	
	int charge;
	
	public ItemRunic (int charge)
    {
        super();
        this.charge = charge;
    }
			
	@Override
	public int getRunicCharge(ItemStack itemstack) {
		return charge;
	}
	
}
