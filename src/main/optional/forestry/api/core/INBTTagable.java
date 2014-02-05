package forestry.api.core;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTTagable {
	void readFromNBT(NBTTagCompound nbttagcompound);

	void writeToNBT(NBTTagCompound nbttagcompound);
}
