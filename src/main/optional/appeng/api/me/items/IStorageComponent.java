package appeng.api.me.items;

import net.minecraft.item.ItemStack;

public interface IStorageComponent {

    /**
     * rv11 - This isn't necessarily the same as if you make a storage cell out of it, but all of AE's default cells do it that way, its currently only used for the condenser.
     * @param cellItem
     * @return numberofBytes
     */
    int getBytes( ItemStack is );
    
    /**
     * rv11 - just true or false for the item stack.
     * @param is
     * @return
     */
    boolean isStorageComponent( ItemStack is );
    
}
