package buildcraft.api.gates;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public interface ITriggerParameter {

	public abstract ItemStack getItemStack();

	public abstract void set(ItemStack stack);

	public abstract void writeToNBT(NBTTagCompound compound);

	public abstract void readFromNBT(NBTTagCompound compound);

	@Deprecated
	public abstract ItemStack getItem();

}
