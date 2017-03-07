package li.cil.oc.api.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.INBTSerializable;

public interface RedstoneAware extends INBTSerializable<NBTTagCompound> {
    int getInput(final EnumFacing side);

    int getOutput(final EnumFacing side);

    void setOutput(final EnumFacing side, final int value);

    boolean isOutputEnabled();

    boolean isOutputStrong();

    void setOutputEnabled(final boolean value);

    int getMaxInput();

    void scheduleInputUpdate();
}
