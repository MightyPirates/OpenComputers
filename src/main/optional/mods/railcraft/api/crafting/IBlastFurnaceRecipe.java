package mods.railcraft.api.crafting;

import net.minecraft.item.ItemStack;

/**
 *
 * @author CovertJaguar <http://www.ModTMechworks.info>
 */
public interface IBlastFurnaceRecipe
{

    public int getCookTime();

    public ItemStack getInput();

    public ItemStack getOutput();

    int getOutputStackSize();

    boolean isRoomForOutput(ItemStack outputSlot);
}
