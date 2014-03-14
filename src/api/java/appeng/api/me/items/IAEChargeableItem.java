
package appeng.api.me.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/*
 * A simple AE interface used internally for PowerRelay charging.
 */
public interface IAEChargeableItem {
	
	/*
	 * Return the amount of energy not used.
	 * 
	 * if you return the same amount out, as was passed in, then it will consider charging done.
	 * 
	 */
    public float addEnergy( ItemStack target, float energy );

    /*
     * Must return true for the target item for it to be considered a charageable item.
     */
	public boolean isChargeable( ItemStack it );
    
}
