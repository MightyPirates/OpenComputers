package li.cil.oc.common.tileentity.traits;

import net.minecraft.item.ItemStack;

public interface ItemStackSerializable {
    ItemStack writeItemStack();

    void readItemStack(final ItemStack stack);
}
